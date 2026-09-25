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

import java.util.List;
import java.util.Map;

/**
 * Every method throws {@link UnsupportedOperationException} by default. Command-under-test fakes
 * extend this and override only the method(s) their command actually calls, instead of every test
 * re-declaring all of {@link ShellAdmin}'s methods.
 */
public class StubShellAdmin implements ShellAdmin {
  private static UnsupportedOperationException notNeeded() {
    return new UnsupportedOperationException("not needed for this test");
  }

  @Override
  public StatusView status() {
    throw notNeeded();
  }

  @Override
  public void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) {
    throw notNeeded();
  }

  @Override
  public void disableTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void enableTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void dropTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public List<String> listTables(String regex) {
    throw notNeeded();
  }

  @Override
  public TableDescription describeTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void decommissionRegionServers(List<String> hostOrServers, boolean offload) {
    throw notNeeded();
  }

  @Override
  public void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames) {
    throw notNeeded();
  }

  @Override
  public List<String> listDecommissionedRegionServers() {
    throw notNeeded();
  }

  @Override
  public void alterTable(String tableName, List<Map<String, Object>> familySpecs) {
    throw notNeeded();
  }

  @Override
  public boolean tableExists(String tableName) {
    throw notNeeded();
  }

  @Override
  public void compact(String tableOrRegionName, String family, String type) {
    throw notNeeded();
  }

  @Override
  public void majorCompact(String tableOrRegionName, String family, String type) {
    throw notNeeded();
  }

  @Override
  public void split(String tableOrRegionName, String splitPoint) {
    throw notNeeded();
  }

  @Override
  public void addPeer(String peerId, Map<String, Object> peerConfigSpec) {
    throw notNeeded();
  }

  @Override
  public void removePeer(String peerId) {
    throw notNeeded();
  }

  @Override
  public List<PeerDescription> listPeers() {
    throw notNeeded();
  }

  @Override
  public void snapshot(String tableName, String snapshotName) {
    throw notNeeded();
  }

  @Override
  public void deleteSnapshot(String snapshotName) {
    throw notNeeded();
  }

  @Override
  public void deleteAllSnapshots(String regex) {
    throw notNeeded();
  }

  @Override
  public List<SnapshotInfo> listSnapshots(String regex) {
    throw notNeeded();
  }

  @Override
  public List<SnapshotInfo> listTableSnapshots(String tableNameRegex, String snapshotNameRegex) {
    throw notNeeded();
  }

  @Override
  public boolean balancerSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean normalizerSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean catalogJanitorSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames) {
    throw notNeeded();
  }

  @Override
  public boolean splitOrMergeSwitch(String switchType, boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean balancerEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean normalizerEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean catalogJanitorEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean splitOrMergeEnabled(String switchType) {
    throw notNeeded();
  }

  @Override
  public RsGroupView getRsGroup(String groupName) {
    throw notNeeded();
  }

  @Override
  public void moveServersToRsGroup(List<String> hostPorts, String groupName) {
    throw notNeeded();
  }

  @Override
  public void grant(String userOrGroup, String actions, String tableName, String family,
    String qualifier, String namespace) {
    throw notNeeded();
  }

  @Override
  public void truncateTable(String tableName, boolean preserveSplits) {
    throw notNeeded();
  }

  @Override
  public boolean isTableDisabled(String tableName) {
    throw notNeeded();
  }

  @Override
  public boolean isTableEnabled(String tableName) {
    throw notNeeded();
  }

  @Override
  public List<String> listTablesByState(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public AlterStatusView alterStatus(String tableName) {
    throw notNeeded();
  }

  @Override
  public void cloneTableSchema(String tableName, String newTableName, boolean preserveSplits) {
    throw notNeeded();
  }

  @Override
  public RegionLocationView locateRegion(String tableName, String rowKey) {
    throw notNeeded();
  }

  @Override
  public ListRegionsView listRegions(String tableName) {
    throw notNeeded();
  }

  @Override
  public void createNamespace(String namespace, Map<String, Object> properties) {
    throw notNeeded();
  }

  @Override
  public void dropNamespace(String namespace) {
    throw notNeeded();
  }

  @Override
  public void alterNamespace(String namespace, Map<String, Object> properties) {
    throw notNeeded();
  }

  @Override
  public String describeNamespace(String namespace) {
    throw notNeeded();
  }

  @Override
  public List<String> listNamespaces(String regex) {
    throw notNeeded();
  }

  @Override
  public List<String> listNamespaceTables(String namespace) {
    throw notNeeded();
  }

  @Override
  public void flush(String tableOrRegionOrServerName, String family) {
    throw notNeeded();
  }

  @Override
  public void assign(String regionName) {
    throw notNeeded();
  }

  @Override
  public void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl,
    String cloneSft) {
    throw notNeeded();
  }

  @Override
  public void restoreSnapshot(String snapshotName, boolean restoreAcl) {
    throw notNeeded();
  }

  @Override
  public void enablePeer(String peerId) {
    throw notNeeded();
  }

  @Override
  public void disablePeer(String peerId) {
    throw notNeeded();
  }

  @Override
  public void setPeerReplicateAll(String peerId, boolean replicateAll) {
    throw notNeeded();
  }

  @Override
  public void setPeerSerial(String peerId, boolean serial) {
    throw notNeeded();
  }

  @Override
  public void setPeerNamespaces(String peerId, List<String> namespaces) {
    throw notNeeded();
  }

  @Override
  public void appendPeerNamespaces(String peerId, List<String> namespaces) {
    throw notNeeded();
  }

  @Override
  public void removePeerNamespaces(String peerId, List<String> namespaces) {
    throw notNeeded();
  }

  @Override
  public void setPeerExcludeNamespaces(String peerId, List<String> namespaces) {
    throw notNeeded();
  }

  @Override
  public void appendPeerExcludeNamespaces(String peerId, List<String> namespaces) {
    throw notNeeded();
  }

  @Override
  public void removePeerExcludeNamespaces(String peerId, List<String> namespaces) {
    throw notNeeded();
  }

  @Override
  public String showPeerTableCFs(String peerId) {
    throw notNeeded();
  }

  @Override
  public void setPeerTableCFs(String peerId, Map<String, Object> tableCFs) {
    throw notNeeded();
  }

  @Override
  public void appendPeerTableCFs(String peerId, Map<String, Object> tableCFs) {
    throw notNeeded();
  }

  @Override
  public void removePeerTableCFs(String peerId, Map<String, Object> tableCFs) {
    throw notNeeded();
  }

  @Override
  public void setPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) {
    throw notNeeded();
  }

  @Override
  public void appendPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) {
    throw notNeeded();
  }

  @Override
  public void removePeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) {
    throw notNeeded();
  }

  @Override
  public void setPeerBandwidth(String peerId, long bandwidth) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listReplicatedTables(String regex) {
    throw notNeeded();
  }

  @Override
  public void enableTableReplication(String tableName) {
    throw notNeeded();
  }

  @Override
  public void disableTableReplication(String tableName) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> getPeerConfigRows(String peerId) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listPeerConfigRows() {
    throw notNeeded();
  }

  @Override
  public void updatePeerConfig(String peerId, Map<String, Object> args) {
    throw notNeeded();
  }

  @Override
  public void transitPeerSyncReplicationState(String peerId, String state) {
    throw notNeeded();
  }

  @Override
  public boolean peerModificationSwitch(boolean enabled, boolean drainProcs) {
    throw notNeeded();
  }

  @Override
  public boolean peerModificationEnabled() {
    throw notNeeded();
  }

  @Override
  public void updateConfig(String serverName) {
    throw notNeeded();
  }

  @Override
  public void updateAllConfig() {
    throw notNeeded();
  }

  @Override
  public void updateRsGroupConfig(String groupName) {
    throw notNeeded();
  }

  @Override
  public void setQuota(Map<String, Object> args) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listQuotas(Map<String, Object> filterArgs) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listQuotaTableSizes() {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listSnapshotSizes() {
    throw notNeeded();
  }

  @Override
  public boolean switchRpcThrottle(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean isRpcThrottleEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean switchExceedThrottleQuota(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public void revoke(String userOrGroup, String tableName, String family, String qualifier,
    String namespace) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> userPermission(String tableOrNamespaceRegex) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> listProcedures() {
    throw notNeeded();
  }

  @Override
  public List<String> listLocks() {
    throw notNeeded();
  }

  @Override
  public void addLabels(List<String> labels) {
    throw notNeeded();
  }

  @Override
  public List<String> listLabels(String regex) {
    throw notNeeded();
  }

  @Override
  public void setAuths(String user, List<String> labels) {
    throw notNeeded();
  }

  @Override
  public List<String> getAuths(String user) {
    throw notNeeded();
  }

  @Override
  public void clearAuths(String user, List<String> labels) {
    throw notNeeded();
  }

  @Override
  public long setVisibility(String tableName, String visibility, Map<String, Object> scanOptions) {
    throw notNeeded();
  }

  @Override
  public List<String> listSecurityCapabilities() {
    throw notNeeded();
  }

  @Override
  public List<RsGroupSummary> listRsGroups(String regex) {
    throw notNeeded();
  }

  @Override
  public void addRsGroup(String groupName) {
    throw notNeeded();
  }

  @Override
  public void removeRsGroup(String groupName) {
    throw notNeeded();
  }

  @Override
  public BalanceResult balanceRsGroup(String groupName, boolean dryRun,
    boolean ignoreRegionsInTransition) {
    throw notNeeded();
  }

  @Override
  public void moveTablesToRsGroup(List<String> tables, String groupName) {
    throw notNeeded();
  }

  @Override
  public void moveNamespacesToRsGroup(List<String> namespaces, String groupName) {
    throw notNeeded();
  }

  @Override
  public void moveServersAndTablesToRsGroup(List<String> hostPorts, List<String> tables,
    String groupName) {
    throw notNeeded();
  }

  @Override
  public void moveServersAndNamespacesToRsGroup(List<String> hostPorts, List<String> namespaces,
    String groupName) {
    throw notNeeded();
  }

  @Override
  public String getRsGroupOfServer(String hostPort) {
    throw notNeeded();
  }

  @Override
  public String getRsGroupOfTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void removeServersFromRsGroup(List<String> hostPorts) {
    throw notNeeded();
  }

  @Override
  public void renameRsGroup(String oldName, String newName) {
    throw notNeeded();
  }

  @Override
  public void alterRsGroupConfig(String groupName, Map<String, Object> args) {
    throw notNeeded();
  }

  @Override
  public List<List<String>> showRsGroupConfig(String groupName) {
    throw notNeeded();
  }

  @Override
  public String getNamespaceRsGroup(String namespace) {
    throw notNeeded();
  }

  @Override
  public void changeSft(String tableName, String family, String sft) {
    throw notNeeded();
  }

  @Override
  public void changeSftAll(String tableRegex, String sft) {
    throw notNeeded();
  }

  @Override
  public BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition) {
    throw notNeeded();
  }

  @Override
  public void move(String encodedRegionName, String destServerName) {
    throw notNeeded();
  }

  @Override
  public boolean normalize(Map<String, Object> filterArgs) {
    throw notNeeded();
  }

  @Override
  public boolean isInMaintenanceMode() {
    throw notNeeded();
  }

  @Override
  public void unassign(String regionName) {
    throw notNeeded();
  }

  @Override
  public void flushMasterStore() {
    throw notNeeded();
  }

  @Override
  public int catalogJanitorRun() {
    throw notNeeded();
  }

  @Override
  public boolean hbckChoreRun() {
    throw notNeeded();
  }

  @Override
  public boolean cleanerChoreRun() {
    throw notNeeded();
  }

  @Override
  public boolean cleanerChoreSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean cleanerChoreEnabled() {
    throw notNeeded();
  }

  @Override
  public void walRoll(String serverName) {
    throw notNeeded();
  }

  @Override
  public void walRollAll() {
    throw notNeeded();
  }

  @Override
  public boolean snapshotCleanupSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean snapshotCleanupEnabled() {
    throw notNeeded();
  }

  @Override
  public long refreshMeta() {
    throw notNeeded();
  }

  @Override
  public void stopMaster() {
    throw notNeeded();
  }

  @Override
  public void stopRegionServer(String hostPort) {
    throw notNeeded();
  }

  @Override
  public List<String> listDeadServers() {
    throw notNeeded();
  }

  @Override
  public List<String> listLiveServers() {
    throw notNeeded();
  }

  @Override
  public List<String> listUnknownServers() {
    throw notNeeded();
  }

  @Override
  public List<String> clearDeadServers(List<String> serverNames) {
    throw notNeeded();
  }

  @Override
  public String clearSlowLogResponses(List<String> serverNames) {
    throw notNeeded();
  }

  @Override
  public void reopenRegions(String tableName, List<String> regionNames) {
    throw notNeeded();
  }

  @Override
  public List<String> getBalancerDecisions(Map<String, Object> args) {
    throw notNeeded();
  }

  @Override
  public List<String> getBalancerRejections(Map<String, Object> args) {
    throw notNeeded();
  }

  @Override
  public List<String> getSlowLogResponses(List<String> serverNames, Map<String, Object> args,
    boolean largeLog) {
    throw notNeeded();
  }

  @Override
  public void mergeRegion(List<String> regionNames, boolean force) {
    throw notNeeded();
  }

  @Override
  public String zkDump() {
    throw notNeeded();
  }

  @Override
  public void compactRegionServer(String serverName, boolean major) {
    throw notNeeded();
  }

  @Override
  public String getCompactionState(String tableName) {
    throw notNeeded();
  }

  @Override
  public void clearCompactionQueues(String serverName, List<String> queueNames) {
    throw notNeeded();
  }

  @Override
  public String clearBlockCache(String tableName) {
    throw notNeeded();
  }

  @Override
  public String regionInfo(String regionName) {
    throw notNeeded();
  }

  @Override
  public List<String> regionsInTransition() {
    throw notNeeded();
  }

  @Override
  public void truncateRegion(String regionName) {
    throw notNeeded();
  }

  @Override
  public long refreshHFiles(Map<String, Object> args) {
    throw notNeeded();
  }
}
