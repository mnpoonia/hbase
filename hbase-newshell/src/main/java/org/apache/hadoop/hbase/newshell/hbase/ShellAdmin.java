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
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Narrow, newshell-specific facade over the admin-plane operations the pilot commands need.
 * Command implementations depend on this interface, never on
 * {@code org.apache.hadoop.hbase.client.Admin} directly - {@link DefaultShellAdmin} is the only
 * class that does.
 */
@InterfaceAudience.Private
public interface ShellAdmin {
  ClusterMetrics status() throws IOException;

  void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) throws IOException;

  void disableTable(String tableName) throws IOException;

  void enableTable(String tableName) throws IOException;

  void dropTable(String tableName) throws IOException;

  List<String> listTables(String regex) throws IOException;

  TableDescription describeTable(String tableName) throws IOException;

  void decommissionRegionServers(List<String> hostOrServers, boolean offload) throws IOException;

  void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames)
    throws IOException;

  List<String> listDecommissionedRegionServers() throws IOException;

  void alterTable(String tableName, List<Map<String, Object>> familySpecs) throws IOException;

  boolean tableExists(String tableName) throws IOException;

  void compact(String tableOrRegionName, String family, String type) throws IOException;

  void majorCompact(String tableOrRegionName, String family, String type) throws IOException;

  void split(String tableOrRegionName, String splitPoint) throws IOException;

  void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException;

  void removePeer(String peerId) throws IOException;

  List<PeerDescription> listPeers() throws IOException;

  void snapshot(String tableName, String snapshotName) throws IOException;

  void deleteSnapshot(String snapshotName) throws IOException;

  List<SnapshotInfo> listSnapshots(String regex) throws IOException;

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

  RsGroupView getRsGroup(String groupName) throws IOException;

  void moveServersToRsGroup(List<String> hostPorts, String groupName) throws IOException;

  void grant(String userOrGroup, String actions, String tableName, String family,
    String qualifier, String namespace) throws IOException;

  void truncateTable(String tableName, boolean preserveSplits) throws IOException;

  boolean isTableDisabled(String tableName) throws IOException;

  boolean isTableEnabled(String tableName) throws IOException;

  List<String> listTablesByState(boolean enabled) throws IOException;

  AlterStatusView alterStatus(String tableName) throws IOException;

  void cloneTableSchema(String tableName, String newTableName, boolean preserveSplits)
    throws IOException;

  RegionLocationView locateRegion(String tableName, String rowKey) throws IOException;

  List<List<String>> listRegions(String tableName) throws IOException;

  void createNamespace(String namespace, Map<String, Object> properties) throws IOException;

  void dropNamespace(String namespace) throws IOException;

  void alterNamespace(String namespace, Map<String, Object> properties) throws IOException;

  String describeNamespace(String namespace) throws IOException;

  List<String> listNamespaces(String regex) throws IOException;

  List<String> listNamespaceTables(String namespace) throws IOException;

  void flush(String tableOrRegionOrServerName, String family) throws IOException;

  void assign(String regionName) throws IOException;

  void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl, String cloneSft)
    throws IOException;

  void restoreSnapshot(String snapshotName, boolean restoreAcl) throws IOException;

  void enablePeer(String peerId) throws IOException;

  void disablePeer(String peerId) throws IOException;

  void updateConfig(String serverName) throws IOException;

  void updateAllConfig() throws IOException;

  void setQuota(Map<String, Object> args) throws IOException;

  List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException;

  void revoke(String userOrGroup, String tableName, String family, String qualifier,
    String namespace) throws IOException;

  List<List<String>> userPermission(String tableOrNamespaceRegex) throws IOException;

  List<List<String>> listProcedures() throws IOException;

  List<String> listLocks() throws IOException;

  void addLabels(List<String> labels) throws IOException;

  List<String> listLabels(String regex) throws IOException;

  List<RsGroupSummary> listRsGroups(String regex) throws IOException;

  void addRsGroup(String groupName) throws IOException;

  void changeSft(String tableName, String family, String sft) throws IOException;

  void changeSftAll(String tableRegex, String sft) throws IOException;
}
