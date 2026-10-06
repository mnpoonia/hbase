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
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Wraps a real {@link Admin}. The full {@link ShellAdmin} implementation, covering all twelve
 * contract domains it is composed from - {@link TableAdminContract} (including
 * {@link ColumnFamilyAttributes} for per-family attributes and {@link TableAttributes} for
 * table-level attributes such as SPLITS/SPLITS_FILE/NUMREGIONS+SPLITALGO/CONFIGURATION/METADATA),
 * {@link NamespaceAdminContract}, {@link ClusterOpsContract}, {@link ServerLifecycleContract},
 * {@link SnapshotAdminContract}, {@link ReplicationPeerContract}, {@link SecurityAdminContract},
 * {@link VisibilityLabelContract}, {@link QuotaAdminContract}, {@link ProcedureAdminContract},
 * {@link RsGroupAdminContract}, and {@link DiagnosticsContract} - ported from hbase-shell's
 * {@code hbase/admin.rb} and the corresponding {@code shell/commands/*.rb} files.
 */
@InterfaceAudience.Private
public final class DefaultShellAdmin implements ShellAdmin {
  private final Admin admin;
  private final ReplicationAdminOps replication;
  private final QuotaAdminOps quotas;
  private final ProcedureAdminOps procedures;
  private final TableAdminOps tableAdminOps;
  private final ClusterAdminOps clusterAdminOps;
  private final RsGroupAdminOps rsGroupAdminOps;
  private final ServerLifecycleOps serverLifecycleOps;
  private final SnapshotAdminOps snapshotAdminOps;
  private final NamespaceAdminOps namespaceAdminOps;
  private final VisibilityLabelOps visibilityLabelOps;
  private final SecurityAdminOps securityAdminOps;
  private final DiagnosticsOps diagnosticsOps;

  public DefaultShellAdmin(Admin admin) {
    this.admin = admin;
    this.replication = new ReplicationAdminOps(admin);
    this.quotas = new QuotaAdminOps(admin);
    this.procedures = new ProcedureAdminOps(admin);
    this.tableAdminOps = new TableAdminOps(admin);
    this.clusterAdminOps = new ClusterAdminOps(admin);
    this.rsGroupAdminOps = new RsGroupAdminOps(admin);
    this.serverLifecycleOps = new ServerLifecycleOps(admin);
    this.snapshotAdminOps = new SnapshotAdminOps(admin);
    this.namespaceAdminOps = new NamespaceAdminOps(admin);
    this.visibilityLabelOps = new VisibilityLabelOps(admin);
    this.securityAdminOps = new SecurityAdminOps(admin);
    this.diagnosticsOps = new DiagnosticsOps(admin);
  }

  @Override
  public void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException {
    replication.addPeer(peerId, peerConfigSpec);
  }

  @Override
  public void removePeer(String peerId) throws IOException {
    replication.removePeer(peerId);
  }

  @Override
  public List<PeerDescription> listPeers() throws IOException {
    return replication.listPeers();
  }

  @Override
  public void enablePeer(String peerId) throws IOException {
    replication.enablePeer(peerId);
  }

  @Override
  public void disablePeer(String peerId) throws IOException {
    replication.disablePeer(peerId);
  }

  @Override
  public void setPeerReplicateAll(String peerId, boolean replicateAll) throws IOException {
    replication.setPeerReplicateAll(peerId, replicateAll);
  }

  @Override
  public void setPeerSerial(String peerId, boolean serial) throws IOException {
    replication.setPeerSerial(peerId, serial);
  }

  @Override
  public void setPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.setPeerNamespaces(peerId, namespaces);
  }

  @Override
  public void appendPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.appendPeerNamespaces(peerId, namespaces);
  }

  @Override
  public void removePeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.removePeerNamespaces(peerId, namespaces);
  }

  @Override
  public void setPeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.setPeerExcludeNamespaces(peerId, namespaces);
  }

  @Override
  public void appendPeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    replication.appendPeerExcludeNamespaces(peerId, namespaces);
  }

  @Override
  public void removePeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    replication.removePeerExcludeNamespaces(peerId, namespaces);
  }

  @Override
  public String showPeerTableCFs(String peerId) throws IOException {
    return replication.showPeerTableCFs(peerId);
  }

  @Override
  public void setPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    replication.setPeerTableCFs(peerId, tableCFs);
  }

  @Override
  public void appendPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    replication.appendPeerTableCFs(peerId, tableCFs);
  }

  @Override
  public void removePeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    replication.removePeerTableCFs(peerId, tableCFs);
  }

  @Override
  public void setPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    replication.setPeerExcludeTableCFs(peerId, tableCFs);
  }

  @Override
  public void appendPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    replication.appendPeerExcludeTableCFs(peerId, tableCFs);
  }

  @Override
  public void removePeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    replication.removePeerExcludeTableCFs(peerId, tableCFs);
  }

  @Override
  public void setPeerBandwidth(String peerId, long bandwidth) throws IOException {
    replication.setPeerBandwidth(peerId, bandwidth);
  }

  @Override
  public List<List<String>> listReplicatedTables(String regex) throws IOException {
    return replication.listReplicatedTables(regex);
  }

  @Override
  public void enableTableReplication(String tableName) throws IOException {
    replication.enableTableReplication(tableName);
  }

  @Override
  public void disableTableReplication(String tableName) throws IOException {
    replication.disableTableReplication(tableName);
  }

  @Override
  public List<List<String>> getPeerConfigRows(String peerId) throws IOException {
    return replication.getPeerConfigRows(peerId);
  }

  @Override
  public List<List<String>> listPeerConfigRows() throws IOException {
    return replication.listPeerConfigRows();
  }

  @Override
  public void updatePeerConfig(String peerId, Map<String, Object> args) throws IOException {
    replication.updatePeerConfig(peerId, args);
  }

  @Override
  public void transitPeerSyncReplicationState(String peerId, String state) throws IOException {
    replication.transitPeerSyncReplicationState(peerId, state);
  }

  @Override
  public boolean peerModificationSwitch(boolean enabled, boolean drainProcs) throws IOException {
    return replication.peerModificationSwitch(enabled, drainProcs);
  }

  @Override
  public boolean peerModificationEnabled() throws IOException {
    return replication.peerModificationEnabled();
  }

  @Override
  public void setQuota(Map<String, Object> args) throws IOException {
    quotas.setQuota(args);
  }

  @Override
  public List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException {
    return quotas.listQuotas(filterArgs);
  }

  @Override
  public List<List<String>> listQuotaTableSizes() throws IOException {
    return quotas.listQuotaTableSizes();
  }

  @Override
  public List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) throws IOException {
    return quotas.listQuotaSnapshots(filterArgs);
  }

  @Override
  public List<List<String>> listSnapshotSizes() throws IOException {
    return quotas.listSnapshotSizes();
  }

  @Override
  public boolean switchRpcThrottle(boolean enabled) throws IOException {
    return quotas.switchRpcThrottle(enabled);
  }

  @Override
  public boolean isRpcThrottleEnabled() throws IOException {
    return quotas.isRpcThrottleEnabled();
  }

  @Override
  public boolean switchExceedThrottleQuota(boolean enabled) throws IOException {
    return quotas.switchExceedThrottleQuota(enabled);
  }

  @Override
  public List<List<String>> listProcedures() throws IOException {
    return procedures.listProcedures();
  }

  @Override
  public List<String> listLocks() throws IOException {
    return procedures.listLocks();
  }

  @Override
  public StatusView status() throws IOException {
    return tableAdminOps.status();
  }

  @Override
  public void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) throws IOException {
    tableAdminOps.createTable(tableName, familySpecs, tableAttributes);
  }

  @Override
  public void disableTable(String tableName) throws IOException {
    tableAdminOps.disableTable(tableName);
  }

  @Override
  public void enableTable(String tableName) throws IOException {
    tableAdminOps.enableTable(tableName);
  }

  @Override
  public void dropTable(String tableName) throws IOException {
    tableAdminOps.dropTable(tableName);
  }

  @Override
  public List<String> listTables(String regex) throws IOException {
    return tableAdminOps.listTables(regex);
  }

  @Override
  public TableDescription describeTable(String tableName) throws IOException {
    return tableAdminOps.describeTable(tableName);
  }

  @Override
  public void decommissionRegionServers(List<String> hostOrServers, boolean offload)
    throws IOException {
    serverLifecycleOps.decommissionRegionServers(hostOrServers, offload);
  }

  @Override
  public void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames)
    throws IOException {
    serverLifecycleOps.recommissionRegionServer(hostOrServer, encodedRegionNames);
  }

  @Override
  public List<String> listDecommissionedRegionServers() throws IOException {
    return serverLifecycleOps.listDecommissionedRegionServers();
  }

  @Override
  public void alterTable(String tableName, List<Map<String, Object>> specs) throws IOException {
    tableAdminOps.alterTable(tableName, specs);
  }

  @Override
  public boolean tableExists(String tableName) throws IOException {
    return tableAdminOps.tableExists(tableName);
  }

  @Override
  public void compact(String tableOrRegionName, String family, String type) throws IOException {
    tableAdminOps.compact(tableOrRegionName, family, type);
  }

  @Override
  public void majorCompact(String tableOrRegionName, String family, String type)
    throws IOException {
    tableAdminOps.majorCompact(tableOrRegionName, family, type);
  }

  @Override
  public void split(String tableOrRegionName, String splitPoint) throws IOException {
    tableAdminOps.split(tableOrRegionName, splitPoint);
  }

  @Override
  public void snapshot(String tableName, String snapshotName) throws IOException {
    snapshotAdminOps.snapshot(tableName, snapshotName);
  }

  @Override
  public void snapshot(String tableName, String snapshotName, Map<String, Object> options)
    throws ShellCommandException, IOException {
    snapshotAdminOps.snapshot(tableName, snapshotName, options);
  }

  @Override
  public void deleteSnapshot(String snapshotName) throws IOException {
    snapshotAdminOps.deleteSnapshot(snapshotName);
  }

  @Override
  public void deleteAllSnapshots(String regex) throws IOException {
    snapshotAdminOps.deleteAllSnapshots(regex);
  }

  @Override
  public List<SnapshotInfo> listSnapshots(String regex) throws IOException {
    return snapshotAdminOps.listSnapshots(regex);
  }

  @Override
  public List<SnapshotInfo> listTableSnapshots(String tableNameRegex, String snapshotNameRegex)
    throws IOException {
    return snapshotAdminOps.listTableSnapshots(tableNameRegex, snapshotNameRegex);
  }

  @Override
  public boolean balancerSwitch(boolean enabled) throws IOException {
    return clusterAdminOps.balancerSwitch(enabled);
  }

  @Override
  public boolean normalizerSwitch(boolean enabled) throws IOException {
    return clusterAdminOps.normalizerSwitch(enabled);
  }

  @Override
  public boolean catalogJanitorSwitch(boolean enabled) throws IOException {
    return clusterAdminOps.catalogJanitorSwitch(enabled);
  }

  @Override
  public Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames)
    throws IOException {
    return clusterAdminOps.compactionSwitch(enabled, serverNames);
  }

  @Override
  public boolean splitOrMergeSwitch(String switchType, boolean enabled) throws IOException {
    return clusterAdminOps.splitOrMergeSwitch(switchType, enabled);
  }

  @Override
  public boolean balancerEnabled() throws IOException {
    return clusterAdminOps.balancerEnabled();
  }

  @Override
  public boolean normalizerEnabled() throws IOException {
    return clusterAdminOps.normalizerEnabled();
  }

  @Override
  public boolean catalogJanitorEnabled() throws IOException {
    return clusterAdminOps.catalogJanitorEnabled();
  }

  @Override
  public boolean splitOrMergeEnabled(String switchType) throws IOException {
    return clusterAdminOps.splitOrMergeEnabled(switchType);
  }

  @Override
  public RsGroupView getRsGroup(String groupName) throws IOException {
    return rsGroupAdminOps.getRsGroup(groupName);
  }

  @Override
  public void moveServersToRsGroup(List<String> hostPorts, String groupName) throws IOException {
    rsGroupAdminOps.moveServersToRsGroup(hostPorts, groupName);
  }

  @Override
  public void grant(String userOrGroup, String actions, String tableName, String family,
    String qualifier, String namespace) throws IOException {
    securityAdminOps.grant(userOrGroup, actions, tableName, family, qualifier, namespace);
  }

  @Override
  public void truncateTable(String tableName, boolean preserveSplits) throws IOException {
    tableAdminOps.truncateTable(tableName, preserveSplits);
  }

  @Override
  public boolean isTableDisabled(String tableName) throws IOException {
    return tableAdminOps.isTableDisabled(tableName);
  }

  @Override
  public boolean isTableEnabled(String tableName) throws IOException {
    return tableAdminOps.isTableEnabled(tableName);
  }

  @Override
  public List<String> listTablesByState(boolean enabled) throws IOException {
    return tableAdminOps.listTablesByState(enabled);
  }

  @Override
  public AlterStatusView alterStatus(String tableName) throws IOException {
    return tableAdminOps.alterStatus(tableName);
  }

  @Override
  public void cloneTableSchema(String tableName, String newTableName, boolean preserveSplits)
    throws IOException {
    tableAdminOps.cloneTableSchema(tableName, newTableName, preserveSplits);
  }

  @Override
  public RegionLocationView locateRegion(String tableName, String rowKey) throws IOException {
    return tableAdminOps.locateRegion(tableName, rowKey);
  }

  @Override
  public ListRegionsView listRegions(String tableName) throws IOException {
    return tableAdminOps.listRegions(tableName);
  }

  @Override
  public void createNamespace(String namespace, Map<String, Object> properties) throws IOException {
    namespaceAdminOps.createNamespace(namespace, properties);
  }

  @Override
  public void dropNamespace(String namespace) throws IOException {
    namespaceAdminOps.dropNamespace(namespace);
  }

  @Override
  public void alterNamespace(String namespace, Map<String, Object> properties) throws IOException {
    namespaceAdminOps.alterNamespace(namespace, properties);
  }

  @Override
  public String describeNamespace(String namespace) throws IOException {
    return namespaceAdminOps.describeNamespace(namespace);
  }

  @Override
  public List<String> listNamespaces(String regex) throws IOException {
    return namespaceAdminOps.listNamespaces(regex);
  }

  @Override
  public List<String> listNamespaceTables(String namespace) throws IOException {
    return namespaceAdminOps.listNamespaceTables(namespace);
  }

  @Override
  public void flush(String tableOrRegionOrServerName, String family) throws IOException {
    tableAdminOps.flush(tableOrRegionOrServerName, family);
  }

  @Override
  public void assign(String regionName) throws IOException {
    tableAdminOps.assign(regionName);
  }

  @Override
  public BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition)
    throws IOException {
    return clusterAdminOps.balance(dryRun, ignoreRegionsInTransition);
  }

  @Override
  public void move(String encodedRegionName, String destServerName) throws IOException {
    tableAdminOps.move(encodedRegionName, destServerName);
  }

  @Override
  public void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl,
    String cloneSft) throws IOException {
    snapshotAdminOps.cloneSnapshot(snapshotName, tableName, restoreAcl, cloneSft);
  }

  @Override
  public void restoreSnapshot(String snapshotName, boolean restoreAcl) throws IOException {
    snapshotAdminOps.restoreSnapshot(snapshotName, restoreAcl);
  }

  @Override
  public void updateConfig(String serverName) throws IOException {
    clusterAdminOps.updateConfig(serverName);
  }

  @Override
  public void updateAllConfig() throws IOException {
    clusterAdminOps.updateAllConfig();
  }

  @Override
  public void updateRsGroupConfig(String groupName) throws IOException {
    clusterAdminOps.updateRsGroupConfig(groupName);
  }

  @Override
  public void revoke(String userOrGroup, String tableName, String family, String qualifier,
    String namespace) throws IOException {
    securityAdminOps.revoke(userOrGroup, tableName, family, qualifier, namespace);
  }

  @Override
  public List<List<String>> userPermission(String tableOrNamespaceRegex) throws IOException {
    return securityAdminOps.userPermission(tableOrNamespaceRegex);
  }

  @Override
  public void addLabels(List<String> labels) throws IOException {
    visibilityLabelOps.addLabels(labels);
  }

  @Override
  public List<String> listLabels(String regex) throws IOException {
    return visibilityLabelOps.listLabels(regex);
  }

  @Override
  public void setAuths(String user, List<String> labels) throws IOException {
    visibilityLabelOps.setAuths(user, labels);
  }

  @Override
  public List<String> getAuths(String user) throws IOException {
    return visibilityLabelOps.getAuths(user);
  }

  @Override
  public void clearAuths(String user, List<String> labels) throws IOException {
    visibilityLabelOps.clearAuths(user, labels);
  }

  @Override
  public long setVisibility(String tableName, String visibility, Map<String, Object> scanOptions)
    throws IOException {
    return visibilityLabelOps.setVisibility(tableName, visibility, scanOptions);
  }

  @Override
  public List<String> listSecurityCapabilities() throws IOException {
    return securityAdminOps.listSecurityCapabilities();
  }

  @Override
  public List<RsGroupSummary> listRsGroups(String regex) throws IOException {
    return rsGroupAdminOps.listRsGroups(regex);
  }

  @Override
  public void addRsGroup(String groupName) throws IOException {
    rsGroupAdminOps.addRsGroup(groupName);
  }

  @Override
  public void removeRsGroup(String groupName) throws IOException {
    rsGroupAdminOps.removeRsGroup(groupName);
  }

  @Override
  public BalanceResult balanceRsGroup(String groupName, boolean dryRun,
    boolean ignoreRegionsInTransition) throws IOException {
    return rsGroupAdminOps.balanceRsGroup(groupName, dryRun, ignoreRegionsInTransition);
  }

  @Override
  public void moveTablesToRsGroup(List<String> tables, String groupName) throws IOException {
    rsGroupAdminOps.moveTablesToRsGroup(tables, groupName);
  }

  @Override
  public void moveNamespacesToRsGroup(List<String> namespaces, String groupName)
    throws IOException {
    rsGroupAdminOps.moveNamespacesToRsGroup(namespaces, groupName);
  }

  @Override
  public void moveServersAndTablesToRsGroup(List<String> hostPorts, List<String> tables,
    String groupName) throws IOException {
    rsGroupAdminOps.moveServersAndTablesToRsGroup(hostPorts, tables, groupName);
  }

  @Override
  public void moveServersAndNamespacesToRsGroup(List<String> hostPorts, List<String> namespaces,
    String groupName) throws IOException {
    rsGroupAdminOps.moveServersAndNamespacesToRsGroup(hostPorts, namespaces, groupName);
  }

  @Override
  public String getRsGroupOfServer(String hostPort) throws IOException {
    return rsGroupAdminOps.getRsGroupOfServer(hostPort);
  }

  @Override
  public String getRsGroupOfTable(String tableName) throws IOException {
    return rsGroupAdminOps.getRsGroupOfTable(tableName);
  }

  @Override
  public void removeServersFromRsGroup(List<String> hostPorts) throws IOException {
    rsGroupAdminOps.removeServersFromRsGroup(hostPorts);
  }

  @Override
  public void renameRsGroup(String oldName, String newName) throws IOException {
    rsGroupAdminOps.renameRsGroup(oldName, newName);
  }

  @Override
  public void alterRsGroupConfig(String groupName, Map<String, Object> args) throws IOException {
    rsGroupAdminOps.alterRsGroupConfig(groupName, args);
  }

  @Override
  public List<List<String>> showRsGroupConfig(String groupName) throws IOException {
    return rsGroupAdminOps.showRsGroupConfig(groupName);
  }

  @Override
  public String getNamespaceRsGroup(String namespace) throws IOException {
    return namespaceAdminOps.getNamespaceRsGroup(namespace);
  }

  @Override
  public void changeSft(String tableName, String family, String sft) throws IOException {
    tableAdminOps.changeSft(tableName, family, sft);
  }

  @Override
  public void changeSftAll(String tableRegex, String sft) throws IOException {
    tableAdminOps.changeSftAll(tableRegex, sft);
  }

  @Override
  public boolean normalize(Map<String, Object> filterArgs) throws IOException {
    return clusterAdminOps.normalize(filterArgs);
  }

  @Override
  public boolean isInMaintenanceMode() throws IOException {
    return clusterAdminOps.isInMaintenanceMode();
  }

  @Override
  public void unassign(String regionName) throws IOException {
    tableAdminOps.unassign(regionName);
  }

  @Override
  public void flushMasterStore() throws IOException {
    clusterAdminOps.flushMasterStore();
  }

  @Override
  public int catalogJanitorRun() throws IOException {
    return clusterAdminOps.catalogJanitorRun();
  }

  @Override
  public boolean hbckChoreRun() throws IOException {
    return clusterAdminOps.hbckChoreRun();
  }

  @Override
  public boolean cleanerChoreRun() throws IOException {
    return clusterAdminOps.cleanerChoreRun();
  }

  @Override
  public boolean cleanerChoreSwitch(boolean enabled) throws IOException {
    return clusterAdminOps.cleanerChoreSwitch(enabled);
  }

  @Override
  public boolean cleanerChoreEnabled() throws IOException {
    return clusterAdminOps.cleanerChoreEnabled();
  }

  @Override
  public void walRoll(String serverName) throws IOException {
    clusterAdminOps.walRoll(serverName);
  }

  @Override
  public void walRollAll() throws IOException {
    clusterAdminOps.walRollAll();
  }

  @Override
  public boolean snapshotCleanupSwitch(boolean enabled) throws IOException {
    return snapshotAdminOps.snapshotCleanupSwitch(enabled);
  }

  @Override
  public boolean snapshotCleanupEnabled() throws IOException {
    return snapshotAdminOps.snapshotCleanupEnabled();
  }

  @Override
  public long refreshMeta() throws IOException {
    return clusterAdminOps.refreshMeta();
  }

  @Override
  public void stopMaster() throws IOException {
    serverLifecycleOps.stopMaster();
  }

  @Override
  public void stopRegionServer(String hostPort) throws IOException {
    serverLifecycleOps.stopRegionServer(hostPort);
  }

  @Override
  public List<String> listDeadServers() throws IOException {
    return serverLifecycleOps.listDeadServers();
  }

  @Override
  public List<String> listLiveServers() throws IOException {
    return serverLifecycleOps.listLiveServers();
  }

  @Override
  public List<String> listUnknownServers() throws IOException {
    return serverLifecycleOps.listUnknownServers();
  }

  @Override
  public List<String> clearDeadServers(List<String> serverNames) throws IOException {
    return serverLifecycleOps.clearDeadServers(serverNames);
  }

  @Override
  public String clearSlowLogResponses(List<String> serverNames) throws IOException {
    return diagnosticsOps.clearSlowLogResponses(serverNames);
  }

  @Override
  public void reopenRegions(String tableName, List<String> regionNames) throws IOException {
    tableAdminOps.reopenRegions(tableName, regionNames);
  }

  @Override
  public List<String> getBalancerDecisions(Map<String, Object> args) throws IOException {
    return diagnosticsOps.getBalancerDecisions(args);
  }

  @Override
  public List<String> getBalancerRejections(Map<String, Object> args) throws IOException {
    return diagnosticsOps.getBalancerRejections(args);
  }

  @Override
  public List<String> getSlowLogResponses(List<String> serverNames, Map<String, Object> args,
    boolean largeLog) throws IOException {
    return diagnosticsOps.getSlowLogResponses(serverNames, args, largeLog);
  }

  @Override
  public void mergeRegion(List<String> regionNames, boolean force) throws IOException {
    tableAdminOps.mergeRegion(regionNames, force);
  }

  @Override
  public String zkDump() throws IOException {
    return diagnosticsOps.zkDump();
  }

  @Override
  public void compactRegionServer(String serverName, boolean major) throws IOException {
    tableAdminOps.compactRegionServer(serverName, major);
  }

  @Override
  public String getCompactionState(String tableName) throws IOException {
    return tableAdminOps.getCompactionState(tableName);
  }

  @Override
  public void clearCompactionQueues(String serverName, List<String> queueNames) throws IOException {
    tableAdminOps.clearCompactionQueues(serverName, queueNames);
  }

  @Override
  public String clearBlockCache(String tableName) throws IOException {
    return tableAdminOps.clearBlockCache(tableName);
  }

  @Override
  public String regionInfo(String regionName) throws IOException {
    return tableAdminOps.regionInfo(regionName);
  }

  @Override
  public List<String> regionsInTransition() throws IOException {
    return tableAdminOps.regionsInTransition();
  }

  @Override
  public void truncateRegion(String regionName) throws IOException {
    tableAdminOps.truncateRegion(regionName);
  }

  @Override
  public long refreshHFiles(Map<String, Object> args) throws IOException {
    return tableAdminOps.refreshHFiles(args);
  }
}
