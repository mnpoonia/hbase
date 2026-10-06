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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.regionserver.storefiletracker.StoreFileTrackerFactory;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of the tools/replication/procedure/storefiletracker commands' Java API
 * wrapper layer against a real minicluster - split out from
 * {@link TestPilotCommandsAgainstMiniCluster} to stay within the {@code LargeTests} class-wide
 * timeout budget.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestToolsAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    TEST_UTIL.startMiniCluster(1);
    connection = TEST_UTIL.getConnection();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    connection.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test
  public void flushFlushesATableByName() throws Exception {
    String tableName = "newshell_flush_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    assertDoesNotThrow(() -> admin.flush(tableName, null));
  }

  @Test
  public void assignReassignsARegionByEncodedName() throws Exception {
    String tableName = "newshell_assign_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    Admin realAdmin = connection.getAdmin();
    byte[] regionName = realAdmin.getRegions(TableName.valueOf(tableName)).get(0).getRegionName();
    String encodedName = realAdmin.getRegions(TableName.valueOf(tableName)).get(0).getEncodedName();
    realAdmin.unassign(regionName, false);

    ShellAdmin admin = DefaultShellAdmin.create(realAdmin);
    assertDoesNotThrow(() -> admin.assign(encodedName));
    TEST_UTIL.waitUntilNoRegionsInTransition(10000);
  }

  @Test
  public void cloneSnapshotThenRestoreSnapshotRoundTrip() throws Exception {
    String tableName = "newshell_clone_snapshot_src_test";
    String snapshotName = "newshell_clone_snapshot_test_snap";
    String cloneName = "newshell_clone_snapshot_dst_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    Admin realAdmin = connection.getAdmin();
    realAdmin.snapshot(snapshotName, TableName.valueOf(tableName));

    ShellAdmin admin = DefaultShellAdmin.create(realAdmin);
    admin.cloneSnapshot(snapshotName, cloneName, false, null);
    assertTrue(realAdmin.tableExists(TableName.valueOf(cloneName)));

    realAdmin.disableTable(TableName.valueOf(tableName));
    admin.restoreSnapshot(snapshotName, false);
    assertTrue(realAdmin.tableExists(TableName.valueOf(tableName)));
    realAdmin.enableTable(TableName.valueOf(tableName));
    assertTrue(realAdmin.isTableEnabled(TableName.valueOf(tableName)));
  }

  @Test
  public void listTableSnapshotsThenDeleteAllSnapshotsRoundTrips() throws Exception {
    String tableName = "newshell_table_snap_list_test";
    String snap1 = "newshell_table_snap_list_s1";
    String snap2 = "newshell_table_snap_list_s2";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    admin.snapshot(tableName, snap1);
    admin.snapshot(tableName, snap2);

    assertEquals(2, admin.listTableSnapshots(tableName, ".*").size());
    assertEquals(1, admin.listTableSnapshots(tableName, ".*_s1").size());
    assertEquals(snap1, admin.listTableSnapshots(tableName, ".*_s1").get(0).name());

    admin.deleteAllSnapshots("newshell_table_snap_list_s.*");
    assertTrue(admin.listTableSnapshots(tableName, ".*").isEmpty());
  }

  @Test
  public void deleteTableSnapshotsDeletesMatchingSnapshotsOnly() throws Exception {
    String tableName = "newshell_delete_table_snaps_test";
    String keep = "newshell_delete_table_snaps_keep";
    String drop = "newshell_delete_table_snaps_drop";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    admin.snapshot(tableName, keep);
    admin.snapshot(tableName, drop);

    // Mirror delete_table_snapshots.rb: list then deleteSnapshot each name.
    for (SnapshotInfo snapshot : admin.listTableSnapshots(tableName, ".*_drop")) {
      admin.deleteSnapshot(snapshot.name());
    }

    List<SnapshotInfo> remaining = admin.listTableSnapshots(tableName, ".*");
    assertEquals(1, remaining.size());
    assertEquals(keep, remaining.get(0).name());
    admin.deleteSnapshot(keep);
  }

  @Test
  public void listSecurityCapabilitiesDoesNotThrow() throws Exception {
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    List<String> caps = admin.listSecurityCapabilities();
    assertFalse(caps.isEmpty());
  }

  @Test
  public void updateAllConfigDoesNotThrow() throws Exception {
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    assertDoesNotThrow(admin::updateAllConfig);
  }

  @Test
  public void updateConfigDoesNotThrowForALiveServer() throws Exception {
    Admin realAdmin = connection.getAdmin();
    ServerName liveServer =
      realAdmin.getClusterMetrics().getLiveServerMetrics().keySet().iterator().next();

    ShellAdmin admin = DefaultShellAdmin.create(realAdmin);
    assertDoesNotThrow(() -> admin.updateConfig(liveServer.getServerName()));
  }

  @Test
  public void listProceduresReturnsAtLeastOneRow() throws Exception {
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());

    String tableName = "newshell_list_procedures_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    List<List<String>> rows = admin.listProcedures();
    assertFalse(rows.isEmpty());
    assertEquals(6, rows.get(0).size());
  }

  @Test
  public void listLocksDoesNotThrow() throws Exception {
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    assertDoesNotThrow(admin::listLocks);
  }

  @Test
  public void changeSftChangesTableAndFamilyLevelTracker() throws Exception {
    String tableName = "newshell_change_sft_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    admin.changeSft(tableName, null, "FILE");
    admin.changeSft(tableName, "f1", "FILE");

    TableDescriptor descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertEquals("FILE", descriptor.getValue(StoreFileTrackerFactory.TRACKER_IMPL));
  }

  @Test
  public void changeSftAllChangesEveryMatchingTable() throws Exception {
    String tableName = "newshell_change_sft_all_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    admin.changeSftAll(tableName, "FILE");

    TableDescriptor descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertEquals("FILE", descriptor.getValue(StoreFileTrackerFactory.TRACKER_IMPL));
  }

  @Test
  public void enablePeerThenDisablePeerRoundTrips() throws Exception {
    String peerId = "newshell_toggle_peer_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());

    admin.addPeer(peerId, mapOf("CLUSTER_KEY", TEST_UTIL.getClusterKey(), "ENDPOINT_CLASSNAME",
      SelfReplicationEndpointForTest.class.getName()));

    admin.disablePeer(peerId);
    assertTrue(
      admin.listPeers().stream().anyMatch(peer -> peer.peerId().equals(peerId) && !peer.enabled()));

    admin.enablePeer(peerId);
    assertTrue(
      admin.listPeers().stream().anyMatch(peer -> peer.peerId().equals(peerId) && peer.enabled()));

    admin.removePeer(peerId);
  }

  @Test
  public void toolsChoresAndSwitchesDoNotThrow() throws Exception {
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    assertDoesNotThrow(admin::isInMaintenanceMode);
    assertDoesNotThrow(admin::cleanerChoreEnabled);
    assertDoesNotThrow(admin::snapshotCleanupEnabled);
    assertDoesNotThrow(admin::peerModificationEnabled);
    assertDoesNotThrow(admin::listDeadServers);
    assertDoesNotThrow(admin::listLiveServers);
    assertDoesNotThrow(admin::listUnknownServers);
    assertDoesNotThrow(admin::regionsInTransition);
    assertDoesNotThrow(admin::catalogJanitorRun);
    assertDoesNotThrow(admin::cleanerChoreRun);
    assertDoesNotThrow(admin::hbckChoreRun);
    assertDoesNotThrow(admin::flushMasterStore);
    assertDoesNotThrow(() -> admin.normalize(Collections.emptyMap()));
    assertDoesNotThrow(admin::zkDump);
    assertDoesNotThrow(admin::walRollAll);
    assertDoesNotThrow(admin::refreshMeta);

    boolean previousCleaner = admin.cleanerChoreSwitch(false);
    assertFalse(admin.cleanerChoreEnabled());
    admin.cleanerChoreSwitch(previousCleaner);

    boolean previousSnap = admin.snapshotCleanupSwitch(false);
    admin.snapshotCleanupSwitch(previousSnap);
  }

  @Test
  public void compactionStateAndClearBlockCacheRoundTrip() throws Exception {
    String tableName = "newshell_compaction_state_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    assertEquals("NONE", admin.getCompactionState(tableName));
    assertDoesNotThrow(() -> admin.clearBlockCache(tableName));
  }

  @Test
  public void peerConfigMutatorsRoundTrip() throws Exception {
    String peerId = "newshell_peer_cfg_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    admin.addPeer(peerId, mapOf("CLUSTER_KEY", TEST_UTIL.getClusterKey(), "ENDPOINT_CLASSNAME",
      SelfReplicationEndpointForTest.class.getName()));

    admin.setPeerReplicateAll(peerId, false);
    admin.setPeerSerial(peerId, true);
    admin.setPeerNamespaces(peerId, Arrays.asList("default"));
    admin.appendPeerNamespaces(peerId, Arrays.asList("hbase"));
    admin.removePeerNamespaces(peerId, Arrays.asList("hbase"));
    admin.setPeerBandwidth(peerId, 1024L * 1024L);
    assertDoesNotThrow(() -> admin.showPeerTableCFs(peerId));
    assertFalse(admin.getPeerConfigRows(peerId).isEmpty());
    assertDoesNotThrow(admin::listPeerConfigRows);
    assertDoesNotThrow(() -> admin.listReplicatedTables(".*"));

    admin.removePeer(peerId);
  }

  @Test
  public void enableThenDisableTableReplicationRoundTrip() throws Exception {
    String tableName = "newshell_table_rep_test";
    String peerId = "newshell_table_rep_peer_" + UUID.randomUUID().toString().replace("-", "");
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    ShellAdmin admin = DefaultShellAdmin.create(connection.getAdmin());
    // enableTableReplication requires at least one peer to sync CF scopes against.
    admin.addPeer(peerId, mapOf("CLUSTER_KEY", TEST_UTIL.getClusterKey(), "ENDPOINT_CLASSNAME",
      SelfReplicationEndpointForTest.class.getName()));
    try {
      assertDoesNotThrow(() -> admin.enableTableReplication(tableName));
      assertDoesNotThrow(() -> admin.disableTableReplication(tableName));
    } finally {
      admin.removePeer(peerId);
    }
  }

  private static Map<String, Object> mapOf(Object... keyValuePairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }
}
