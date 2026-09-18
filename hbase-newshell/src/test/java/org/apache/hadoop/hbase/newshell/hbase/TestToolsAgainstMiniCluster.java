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

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertDoesNotThrow(() -> admin.flush(tableName, null));
  }

  @Test
  public void assignReassignsARegionByEncodedName() throws Exception {
    String tableName = "newshell_assign_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    Admin realAdmin = connection.getAdmin();
    byte[] regionName = realAdmin.getRegions(TableName.valueOf(tableName)).get(0).getRegionName();
    String encodedName =
      realAdmin.getRegions(TableName.valueOf(tableName)).get(0).getEncodedName();
    realAdmin.unassign(regionName, false);

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
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

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.cloneSnapshot(snapshotName, cloneName, false, null);
    assertTrue(realAdmin.tableExists(TableName.valueOf(cloneName)));

    realAdmin.disableTable(TableName.valueOf(tableName));
    admin.restoreSnapshot(snapshotName, false);
    assertTrue(realAdmin.tableExists(TableName.valueOf(tableName)));
    realAdmin.enableTable(TableName.valueOf(tableName));
    assertTrue(realAdmin.isTableEnabled(TableName.valueOf(tableName)));
  }

  @Test
  public void updateAllConfigDoesNotThrow() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertDoesNotThrow(admin::updateAllConfig);
  }

  @Test
  public void updateConfigDoesNotThrowForALiveServer() throws Exception {
    Admin realAdmin = connection.getAdmin();
    ServerName liveServer =
      realAdmin.getClusterMetrics().getLiveServerMetrics().keySet().iterator().next();

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    assertDoesNotThrow(() -> admin.updateConfig(liveServer.getServerName()));
  }

  @Test
  public void listProceduresReturnsAtLeastOneRow() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    String tableName = "newshell_list_procedures_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    List<List<String>> rows = admin.listProcedures();
    assertFalse(rows.isEmpty());
    assertEquals(6, rows.get(0).size());
  }

  @Test
  public void listLocksDoesNotThrow() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertDoesNotThrow(admin::listLocks);
  }

  @Test
  public void changeSftChangesTableAndFamilyLevelTracker() throws Exception {
    String tableName = "newshell_change_sft_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.changeSft(tableName, null, "FILE");
    admin.changeSft(tableName, "f1", "FILE");

    TableDescriptor descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertEquals("FILE", descriptor.getValue(StoreFileTrackerFactory.TRACKER_IMPL));
  }

  @Test
  public void changeSftAllChangesEveryMatchingTable() throws Exception {
    String tableName = "newshell_change_sft_all_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.changeSftAll(tableName, "FILE");

    TableDescriptor descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertEquals("FILE", descriptor.getValue(StoreFileTrackerFactory.TRACKER_IMPL));
  }

  @Test
  public void enablePeerThenDisablePeerRoundTrips() throws Exception {
    String peerId = "newshell_toggle_peer_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    admin.addPeer(peerId, Map.of("CLUSTER_KEY", TEST_UTIL.getClusterKey(), "ENDPOINT_CLASSNAME",
      SelfReplicationEndpointForTest.class.getName()));

    admin.disablePeer(peerId);
    assertTrue(admin.listPeers().stream()
      .anyMatch(peer -> peer.peerId().equals(peerId) && !peer.enabled()));

    admin.enablePeer(peerId);
    assertTrue(admin.listPeers().stream()
      .anyMatch(peer -> peer.peerId().equals(peerId) && peer.enabled()));

    admin.removePeer(peerId);
  }
}
