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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.LogEntry;
import org.apache.hadoop.hbase.client.ServerType;
import org.apache.hadoop.hbase.zookeeper.ZKDump;
import org.apache.hadoop.hbase.zookeeper.ZKWatcher;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link DiagnosticsContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class DiagnosticsOps implements DiagnosticsContract {
  private final Admin admin;

  DiagnosticsOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public String clearSlowLogResponses(List<String> serverNames) throws IOException {
    Set<ServerName> servers = resolveLogServers(serverNames, true);
    List<Boolean> responses = admin.clearSlowLogResponses(servers);
    int success = 0;
    for (Boolean response : responses) {
      if (Boolean.TRUE.equals(response)) {
        success++;
      }
    }
    return "Cleared Slowlog responses from " + success + "/" + responses.size() + " RegionServers";
  }

  @Override
  public List<String> getBalancerDecisions(Map<String, Object> args) throws IOException {
    int limit = intArg(args, "LIMIT", 250);
    return logEntriesToJson(
      admin.getLogEntries(null, "BALANCER_DECISION", ServerType.MASTER, limit, null));
  }

  @Override
  public List<String> getBalancerRejections(Map<String, Object> args) throws IOException {
    int limit = intArg(args, "LIMIT", 250);
    return logEntriesToJson(
      admin.getLogEntries(null, "BALANCER_REJECTION", ServerType.MASTER, limit, null));
  }

  @Override
  public List<String> getSlowLogResponses(List<String> serverNames, Map<String, Object> args,
    boolean largeLog) throws IOException {
    Set<ServerName> servers;
    if (serverNames != null && serverNames.size() == 1 && "*".equals(serverNames.get(0))) {
      servers = resolveLogServers(Collections.emptyList(), true);
    } else {
      servers = resolveLogServers(serverNames, false);
    }
    int limit = intArg(args, "LIMIT", 10);
    Map<String, Object> filterParams = slowLogFilterParams(args);
    String logType = largeLog ? "LARGE_LOG" : "SLOW_LOG";
    return logEntriesToJson(
      admin.getLogEntries(servers, logType, ServerType.REGION_SERVER, limit, filterParams));
  }

  @Override
  public String zkDump() throws IOException {
    try (ZKWatcher watcher = new ZKWatcher(admin.getConfiguration(), "admin", null)) {
      return ZKDump.dump(watcher);
    }
  }

  private Set<ServerName> resolveLogServers(List<String> serverNames, boolean allIfEmpty)
    throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    if (serverNames == null || serverNames.isEmpty()) {
      if (allIfEmpty) {
        return new HashSet<>(liveServers);
      }
      return Collections.emptySet();
    }
    Set<ServerName> resolved = new HashSet<>();
    for (String hostOrServer : serverNames) {
      ServerNames.resolve(hostOrServer, liveServers).ifPresent(resolved::add);
    }
    return resolved;
  }

  private static int intArg(Map<String, Object> args, String key, int defaultValue) {
    if (args == null || !args.containsKey(key)) {
      return defaultValue;
    }
    Object value = args.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return Integer.parseInt(String.valueOf(value));
  }

  private static Map<String, Object> slowLogFilterParams(Map<String, Object> args) {
    Map<String, Object> filterParams = new HashMap<>();
    if (args == null) {
      return filterParams;
    }
    if (args.containsKey("REGION_NAME")) {
      filterParams.put("regionName", String.valueOf(args.get("REGION_NAME")));
    }
    if (args.containsKey("TABLE_NAME")) {
      filterParams.put("tableName", String.valueOf(args.get("TABLE_NAME")));
    }
    if (args.containsKey("CLIENT_IP")) {
      filterParams.put("clientAddress", String.valueOf(args.get("CLIENT_IP")));
    }
    if (args.containsKey("USER")) {
      filterParams.put("userName", String.valueOf(args.get("USER")));
    }
    if (args.containsKey("FILTER_BY_OP")) {
      String op = String.valueOf(args.get("FILTER_BY_OP"));
      if (!"OR".equals(op) && !"AND".equals(op)) {
        throw new IllegalArgumentException("FILTER_BY_OP should be either OR / AND");
      }
      if ("AND".equals(op)) {
        filterParams.put("filterByOperator", "AND");
      }
    }
    return filterParams;
  }

  private static List<String> logEntriesToJson(List<LogEntry> entries) {
    List<String> result = new ArrayList<>();
    if (entries == null) {
      return result;
    }
    for (LogEntry entry : entries) {
      result.add(entry.toJsonPrettyPrint());
    }
    return result;
  }
}
