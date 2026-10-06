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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BalanceRequest;
import org.apache.hadoop.hbase.client.Hbck;
import org.apache.hadoop.hbase.client.NormalizeTableFilterParams;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link ClusterOpsContract} operations on a real {@link Admin}, used by {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class ClusterAdminOps implements ClusterOpsContract {
  private final Admin admin;

  ClusterAdminOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public boolean balancerSwitch(boolean enabled) throws IOException {
    return admin.balancerSwitch(enabled, false);
  }

  @Override
  public boolean normalizerSwitch(boolean enabled) throws IOException {
    return admin.normalizerSwitch(enabled);
  }

  @Override
  public boolean catalogJanitorSwitch(boolean enabled) throws IOException {
    return admin.catalogJanitorSwitch(enabled);
  }

  @Override
  public Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames)
    throws IOException {
    Map<String, Boolean> previousStates = new LinkedHashMap<>();
    for (Map.Entry<ServerName, Boolean> entry : admin.compactionSwitch(enabled, serverNames)
      .entrySet()) {
      previousStates.put(entry.getKey().getServerName(), entry.getValue());
    }
    return previousStates;
  }

  @Override
  public boolean splitOrMergeSwitch(String switchType, boolean enabled) throws IOException {
    if ("SPLIT".equals(switchType)) {
      return admin.splitSwitch(enabled, false);
    } else if ("MERGE".equals(switchType)) {
      return admin.mergeSwitch(enabled, false);
    }
    throw new ClientErrorException("only SPLIT or MERGE accepted for type!");
  }

  @Override
  public boolean balancerEnabled() throws IOException {
    return admin.isBalancerEnabled();
  }

  @Override
  public boolean normalizerEnabled() throws IOException {
    return admin.isNormalizerEnabled();
  }

  @Override
  public boolean catalogJanitorEnabled() throws IOException {
    return admin.isCatalogJanitorEnabled();
  }

  @Override
  public boolean splitOrMergeEnabled(String switchType) throws IOException {
    if ("SPLIT".equals(switchType)) {
      return admin.isSplitEnabled();
    } else if ("MERGE".equals(switchType)) {
      return admin.isMergeEnabled();
    }
    throw new ClientErrorException("only SPLIT or MERGE accepted for type!");
  }

  @Override
  public BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition)
    throws IOException {
    return BalanceResult.from(admin.balance(BalanceRequest.newBuilder().setDryRun(dryRun)
      .setIgnoreRegionsInTransition(ignoreRegionsInTransition).build()));
  }

  @Override
  public void updateConfig(String serverName) throws IOException {
    admin.updateConfiguration(ServerName.valueOf(serverName));
  }

  @Override
  public void updateAllConfig() throws IOException {
    admin.updateConfiguration();
  }

  @Override
  public void updateRsGroupConfig(String groupName) throws IOException {
    admin.updateConfiguration(groupName);
  }

  @Override
  public boolean normalize(Map<String, Object> filterArgs) throws IOException {
    NormalizeTableFilterParams.Builder builder = new NormalizeTableFilterParams.Builder();
    if (filterArgs != null && !filterArgs.isEmpty()) {
      if (filterArgs.containsKey("TABLE_NAME")) {
        Object tableName = filterArgs.get("TABLE_NAME");
        builder.tableNames(Arrays.asList(TableName.valueOf(String.valueOf(tableName))));
      } else if (filterArgs.containsKey("TABLE_NAMES")) {
        Object tableNames = filterArgs.get("TABLE_NAMES");
        if (!(tableNames instanceof List)) {
          throw new ClientErrorException("TABLE_NAMES must be of type Array");
        }
        List<TableName> names = new ArrayList<>();
        for (Object tn : (List<?>) tableNames) {
          names.add(TableName.valueOf(String.valueOf(tn)));
        }
        builder.tableNames(names);
      }
      if (filterArgs.containsKey("REGEX")) {
        builder.regex(String.valueOf(filterArgs.get("REGEX")));
      }
      if (filterArgs.containsKey("NAMESPACE")) {
        builder.namespace(String.valueOf(filterArgs.get("NAMESPACE")));
      }
    }
    return admin.normalize(builder.build());
  }

  @Override
  public boolean isInMaintenanceMode() throws IOException {
    return admin.isMasterInMaintenanceMode();
  }

  @Override
  public void flushMasterStore() throws IOException {
    admin.flushMasterStore();
  }

  @Override
  public int catalogJanitorRun() throws IOException {
    return admin.runCatalogJanitor();
  }

  @Override
  public boolean hbckChoreRun() throws IOException {
    try (Hbck hbck = admin.getConnection().getHbck()) {
      return hbck.runHbckChore();
    }
  }

  @Override
  public boolean cleanerChoreRun() throws IOException {
    return admin.runCleanerChore();
  }

  @Override
  public boolean cleanerChoreSwitch(boolean enabled) throws IOException {
    return admin.cleanerChoreSwitch(enabled);
  }

  @Override
  public boolean cleanerChoreEnabled() throws IOException {
    return admin.isCleanerChoreEnabled();
  }

  @Override
  public void walRoll(String serverName) throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    ServerName resolved = ServerNames.resolve(serverName, liveServers).orElseThrow(
      () -> new ClientErrorException("Could not find server with specified name: " + serverName));
    admin.rollWALWriter(resolved);
  }

  @Override
  public void walRollAll() throws IOException {
    admin.rollAllWALWriters();
  }

  @Override
  public long refreshMeta() throws IOException {
    return admin.refreshMeta();
  }
}
