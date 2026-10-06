/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.newshell.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BalanceRequest;
import org.apache.hadoop.hbase.net.Address;
import org.apache.hadoop.hbase.rsgroup.RSGroupInfo;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link RsGroupAdminContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class RsGroupAdminOps implements RsGroupAdminContract {
  private final Admin admin;

  RsGroupAdminOps(Admin admin) {
    this.admin = admin;
  }

  private NamespaceAdminOps namespaces() {
    return new NamespaceAdminOps(admin);
  }

  @Override
  public RsGroupView getRsGroup(String groupName) throws IOException {
    RSGroupInfo groupInfo = admin.getRSGroup(groupName);
    if (groupInfo == null) {
      throw new IOException("RSGroup '" + groupName + "' does not exist");
    }
    List<String> servers =
      groupInfo.getServers().stream().map(Address::toString).collect(Collectors.toList());
    List<String> tables =
      groupInfo.getTables().stream().map(TableName::getNameAsString).collect(Collectors.toList());
    return new RsGroupView(servers, tables);
  }

  @Override
  public void moveServersToRsGroup(List<String> hostPorts, String groupName) throws IOException {
    Set<Address> addresses =
      hostPorts.stream().map(Address::fromString).collect(Collectors.toSet());
    admin.moveServersToRSGroup(addresses, groupName);
  }

  @Override
  public List<RsGroupSummary> listRsGroups(String regex) throws IOException {
    Pattern pattern = Pattern.compile(regex);
    List<RsGroupSummary> result = new ArrayList<>();
    for (RSGroupInfo group : admin.listRSGroups()) {
      if (!pattern.matcher(group.getName()).matches()) {
        continue;
      }
      List<String> servers =
        group.getServers().stream().map(Address::toString).collect(Collectors.toList());
      List<String> tables =
        group.getTables().stream().map(TableName::getNameAsString).collect(Collectors.toList());
      result.add(new RsGroupSummary(group.getName(), servers, tables));
    }
    return result;
  }

  @Override
  public void addRsGroup(String groupName) throws IOException {
    admin.addRSGroup(groupName);
  }

  @Override
  public void removeRsGroup(String groupName) throws IOException {
    admin.removeRSGroup(groupName);
  }

  @Override
  public BalanceResult balanceRsGroup(String groupName, boolean dryRun,
    boolean ignoreRegionsInTransition) throws IOException {
    return BalanceResult.from(admin.balanceRSGroup(groupName, BalanceRequest.newBuilder()
      .setDryRun(dryRun).setIgnoreRegionsInTransition(ignoreRegionsInTransition).build()));
  }

  @Override
  public void moveTablesToRsGroup(List<String> tables, String groupName) throws IOException {
    Set<TableName> tableNames = tables.stream().map(TableName::valueOf).collect(Collectors.toSet());
    admin.setRSGroup(tableNames, groupName);
  }

  @Override
  public void moveNamespacesToRsGroup(List<String> namespaces, String groupName)
    throws IOException {
    Set<TableName> tables = new HashSet<>();
    for (String ns : namespaces) {
      try {
        admin.getNamespaceDescriptor(ns);
      } catch (org.apache.hadoop.hbase.NamespaceNotFoundException e) {
        throw new IOException("Can't find a namespace: " + ns, e);
      }
      for (TableName table : admin.listTableNamesByNamespace(ns)) {
        tables.add(table);
      }
    }
    if (!tables.isEmpty()) {
      admin.setRSGroup(tables, groupName);
    }
    for (String ns : namespaces) {
      Map<String, Object> props = new HashMap<>();
      props.put("METHOD", "set");
      props.put("hbase.rsgroup.name", groupName);
      namespaces().alterNamespace(ns, props);
    }
  }

  @Override
  public void moveServersAndTablesToRsGroup(List<String> hostPorts, List<String> tables,
    String groupName) throws IOException {
    moveServersToRsGroup(hostPorts, groupName);
    moveTablesToRsGroup(tables, groupName);
  }

  @Override
  public void moveServersAndNamespacesToRsGroup(List<String> hostPorts, List<String> namespaces,
    String groupName) throws IOException {
    moveServersToRsGroup(hostPorts, groupName);
    moveNamespacesToRsGroup(namespaces, groupName);
  }

  @Override
  public String getRsGroupOfServer(String hostPort) throws IOException {
    RSGroupInfo group = admin.getRSGroup(Address.fromString(hostPort));
    if (group == null) {
      throw new IOException("Server has no group: " + hostPort);
    }
    return group.getName();
  }

  @Override
  public String getRsGroupOfTable(String tableName) throws IOException {
    RSGroupInfo group = admin.getRSGroup(TableName.valueOf(tableName));
    if (group == null) {
      throw new IOException("Table has no group: " + tableName);
    }
    return group.getName();
  }

  @Override
  public void removeServersFromRsGroup(List<String> hostPorts) throws IOException {
    Set<Address> addresses =
      hostPorts.stream().map(Address::fromString).collect(Collectors.toSet());
    admin.removeServersFromRSGroup(addresses);
  }

  @Override
  public void renameRsGroup(String oldName, String newName) throws IOException {
    admin.renameRSGroup(oldName, newName);
  }

  @Override
  public void alterRsGroupConfig(String groupName, Map<String, Object> args) throws IOException {
    RSGroupInfo group = admin.getRSGroup(groupName);
    if (group == null) {
      throw new IOException("RSGroup does not exist");
    }
    Map<String, String> configuration = new HashMap<>(group.getConfiguration());
    Object method = args.get("METHOD");
    if ("unset".equals(String.valueOf(method))) {
      configuration.remove(String.valueOf(args.get("NAME")));
    } else {
      for (Map.Entry<String, Object> e : args.entrySet()) {
        if ("METHOD".equals(e.getKey())) {
          continue;
        }
        configuration.put(e.getKey(), String.valueOf(e.getValue()));
      }
    }
    admin.updateRSGroupConfig(groupName, configuration);
  }

  @Override
  public List<List<String>> showRsGroupConfig(String groupName) throws IOException {
    RSGroupInfo group = admin.getRSGroup(groupName);
    if (group == null) {
      throw new IOException("RSGroup does not exist");
    }
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<String, String> e : group.getConfiguration().entrySet()) {
      rows.add(Arrays.asList(e.getKey(), e.getValue()));
    }
    return rows;
  }
}
