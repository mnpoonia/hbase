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
import java.util.List;
import java.util.Map;
import org.apache.yetus.audience.InterfaceAudience;

/** Cluster-wide and per-server administrative switches, chores, and server lifecycle operations. */
@InterfaceAudience.Private
public interface ClusterOpsContract {
  void decommissionRegionServers(List<String> hostOrServers, boolean offload) throws IOException;

  void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames)
    throws IOException;

  List<String> listDecommissionedRegionServers() throws IOException;

  boolean balancerSwitch(boolean enabled) throws IOException;

  boolean normalizerSwitch(boolean enabled) throws IOException;

  boolean catalogJanitorSwitch(boolean enabled) throws IOException;

  Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames)
    throws IOException;

  boolean splitOrMergeSwitch(String switchType, boolean enabled) throws IOException;

  boolean balancerEnabled() throws IOException;

  boolean normalizerEnabled() throws IOException;

  boolean catalogJanitorEnabled() throws IOException;

  boolean splitOrMergeEnabled(String switchType) throws IOException;

  void updateConfig(String serverName) throws IOException;

  void updateAllConfig() throws IOException;

  void updateRsGroupConfig(String groupName) throws IOException;

  BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition) throws IOException;

  boolean normalize(Map<String, Object> filterArgs) throws IOException;

  boolean isInMaintenanceMode() throws IOException;

  void flushMasterStore() throws IOException;

  int catalogJanitorRun() throws IOException;

  boolean hbckChoreRun() throws IOException;

  boolean cleanerChoreRun() throws IOException;

  boolean cleanerChoreSwitch(boolean enabled) throws IOException;

  boolean cleanerChoreEnabled() throws IOException;

  void walRoll(String serverName) throws IOException;

  void walRollAll() throws IOException;

  long refreshMeta() throws IOException;

  void stopMaster() throws IOException;

  void stopRegionServer(String hostPort) throws IOException;

  List<String> listDeadServers() throws IOException;

  List<String> listLiveServers() throws IOException;

  List<String> listUnknownServers() throws IOException;

  List<String> clearDeadServers(List<String> serverNames) throws IOException;

  /** Returns summary message of how many RegionServers were cleared */
  String clearSlowLogResponses(List<String> serverNames) throws IOException;

  List<String> getBalancerDecisions(Map<String, Object> args) throws IOException;

  List<String> getBalancerRejections(Map<String, Object> args) throws IOException;

  /**
   * @param serverNames {@code null} or empty with {@code allServers=true} meaning all live RS;
   *                    otherwise host/port/startcode strings (or a single {@code "*"})
   */
  List<String> getSlowLogResponses(List<String> serverNames, Map<String, Object> args,
    boolean largeLog) throws IOException;

  String zkDump() throws IOException;
}
