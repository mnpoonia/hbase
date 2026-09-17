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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of the pilot commands' Java API wrapper layer against a real
 * minicluster - {@link DefaultShellAdmin} for {@code status}/{@code create}/{@code disable}/
 * {@code enable}/{@code drop}/{@code list}/{@code describe} and {@link DefaultShellTable} for
 * {@code get}/{@code put}.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestPilotCommandsAgainstMiniCluster {
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
  public void statusReturnsNonNullClusterMetrics() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertNotNull(admin.status());
  }

  @Test
  public void createBuildsTableWithNamedFamily() throws Exception {
    String tableName = "newshell_create_test";
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createTable(tableName, List.of(Map.of("NAME", "f1")), Map.of());

    Admin realAdmin = connection.getAdmin();
    assertTrue(realAdmin.tableExists(TableName.valueOf(tableName)));
    assertTrue(
      realAdmin.getDescriptor(TableName.valueOf(tableName)).hasColumnFamily(Bytes.toBytes("f1")));
  }

  @Test
  public void createBuildsTableWithMultipleNamedFamilies() throws Exception {
    String tableName = "newshell_create_multi_family_test";
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createTable(tableName,
      List.of(Map.of("NAME", "f1"), Map.of("NAME", "f2", "VERSIONS", 5L)), Map.of());

    Admin realAdmin = connection.getAdmin();
    assertTrue(realAdmin.tableExists(TableName.valueOf(tableName)));
    TableDescriptor descriptor = realAdmin.getDescriptor(TableName.valueOf(tableName));
    assertTrue(descriptor.hasColumnFamily(Bytes.toBytes("f1")));
    assertTrue(descriptor.hasColumnFamily(Bytes.toBytes("f2")));
    assertEquals(5, descriptor.getColumnFamily(Bytes.toBytes("f2")).getMaxVersions());
  }

  @Test
  public void createAppliesTableLevelSplitsAndAttributes() throws Exception {
    String tableName = "newshell_create_table_attrs_test";
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createTable(tableName, List.of(Map.of("NAME", "f1")),
      Map.of("SPLITS", List.of("1000", "2000"), "REGION_REPLICATION", 2L));

    Admin realAdmin = connection.getAdmin();
    TableDescriptor descriptor = realAdmin.getDescriptor(TableName.valueOf(tableName));
    assertEquals(2, descriptor.getRegionReplication());
    // 2 splits -> 3 regions, x2 replicas each -> 6 RegionInfo entries; count primaries only.
    long primaryRegionCount = realAdmin.getRegions(TableName.valueOf(tableName)).stream()
      .filter(region -> region.getReplicaId() == 0).count();
    assertEquals(3, primaryRegionCount);
  }

  @Test
  public void disableGuardsAgainstAlreadyDisabledTable() throws Exception {
    String tableName = "newshell_disable_test";
    Admin realAdmin = connection.getAdmin();
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.disableTable(tableName);
    assertTrue(realAdmin.isTableDisabled(TableName.valueOf(tableName)));
  }

  @Test
  public void getReturnsPutCellsWithColumnFilter() throws Exception {
    String tableName = "newshell_get_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      table.put(put);
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    GetResult result = factory.forTable(tableName).get("r1", Map.of("COLUMN", "f1:c1"));

    List<CellView> cells = result.cells();
    assertEquals(1, cells.size());
    assertEquals("f1", cells.get(0).family());
    assertEquals("c1", cells.get(0).qualifier());
    assertEquals("v1", cells.get(0).value());
  }

  @Test
  public void getReturnsCellsAcrossMultipleFamiliesAndQualifiers() throws Exception {
    String tableName = "newshell_get_multi_family_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), new byte[][] { Bytes.toBytes("f1"),
      Bytes.toBytes("f2") });
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c2"), Bytes.toBytes("v2"));
      put.addColumn(Bytes.toBytes("f2"), Bytes.toBytes("c1"), Bytes.toBytes("v3"));
      table.put(put);
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    GetResult result = factory.forTable(tableName)
      .get("r1", Map.of("COLUMN", List.of("f1:c1", "f1:c2", "f2:c1")));

    List<CellView> cells = result.cells();
    assertEquals(3, cells.size());
  }

  @Test
  public void getReturnsEmptyForMissingRow() throws Exception {
    String tableName = "newshell_get_missing_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    GetResult result = factory.forTable(tableName).get("missing-row", Map.of());
    assertTrue(result.cells().isEmpty());
  }

  @Test
  public void enableReversesDisable() throws Exception {
    String tableName = "newshell_enable_test";
    Admin realAdmin = connection.getAdmin();
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    realAdmin.disableTable(TableName.valueOf(tableName));

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.enableTable(tableName);
    assertTrue(realAdmin.isTableEnabled(TableName.valueOf(tableName)));
  }

  @Test
  public void dropRemovesDisabledTable() throws Exception {
    String tableName = "newshell_drop_test";
    Admin realAdmin = connection.getAdmin();
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    realAdmin.disableTable(TableName.valueOf(tableName));

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.dropTable(tableName);
    assertTrue(!realAdmin.tableExists(TableName.valueOf(tableName)));
  }

  @Test
  public void putWritesACellReadableViaGet() throws Exception {
    String tableName = "newshell_put_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    factory.forTable(tableName).put("r1", "f1:c1", "v1", Map.of());

    GetResult result = factory.forTable(tableName).get("r1", Map.of());
    assertEquals(1, result.cells().size());
    assertEquals("v1", result.cells().get(0).value());
  }

  @Test
  public void listFiltersTableNamesByRegex() throws Exception {
    String tableName = "newshell_list_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertTrue(admin.listTables(tableName).contains(tableName));
    assertTrue(!admin.listTables("no_such_table_.*").contains(tableName));
  }

  @Test
  public void describeReturnsEnabledStatusAndColumnFamily() throws Exception {
    String tableName = "newshell_describe_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    TableDescription description = admin.describeTable(tableName);
    assertTrue(description.enabled());
    assertTrue(description.columnFamilies().get(0).contains("f1"));
    // tableAttributes() must be the bare ", {TABLE_ATTRIBUTES => ...}" suffix hbase-shell's
    // describe.rb prepends the table name to - not toStringCustomizedValues(), which redundantly
    // includes its own leading table name and trailing column-family descriptors.
    assertTrue(
      description.tableAttributes().isEmpty() || description.tableAttributes().startsWith(", "));
    assertTrue(!description.tableAttributes().contains(tableName));
  }

  @Test
  public void decommissionAndRecommissionRoundTripByHostname() throws Exception {
    Admin realAdmin = connection.getAdmin();
    ServerName liveServer =
      realAdmin.getClusterMetrics().getLiveServerMetrics().keySet().iterator().next();

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.decommissionRegionServers(List.of(liveServer.getHostname()), false);
    assertTrue(admin.listDecommissionedRegionServers().contains(liveServer.getServerName()));

    admin.recommissionRegionServer(liveServer.getHostname(), List.of());
    assertTrue(!admin.listDecommissionedRegionServers().contains(liveServer.getServerName()));
  }

  @Test
  public void alterChangesColumnFamilyTtl() throws Exception {
    String tableName = "newshell_alter_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.alterTable(tableName, List.of(Map.of("NAME", "f1", "TTL", 100)));

    TableDescriptor descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertEquals(100, descriptor.getColumnFamily(Bytes.toBytes("f1")).getTimeToLive());
  }

  @Test
  public void existsReflectsTablePresence() throws Exception {
    String tableName = "newshell_exists_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertTrue(admin.tableExists(tableName));
    assertFalse(admin.tableExists("newshell_exists_missing_test"));
  }

  @Test
  public void compactFallsBackToTableLevelWhenGivenATableName() throws Exception {
    String tableName = "newshell_compact_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertDoesNotThrow(() -> admin.compact(tableName, null, null));
  }

  @Test
  public void majorCompactFallsBackToTableLevelWhenGivenATableName() throws Exception {
    String tableName = "newshell_major_compact_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertDoesNotThrow(() -> admin.majorCompact(tableName, null, null));
  }

  @Test
  public void splitFallsBackToTableLevelWhenGivenATableName() throws Exception {
    String tableName = "newshell_split_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    assertDoesNotThrow(() -> admin.split(tableName, "m"));
  }

  @Test
  public void addPeerThenListPeersThenRemovePeerRoundTrips() throws Exception {
    String peerId = "newshell_peer_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    admin.addPeer(peerId, Map.of("CLUSTER_KEY", TEST_UTIL.getClusterKey(), "ENDPOINT_CLASSNAME",
      SelfReplicationEndpointForTest.class.getName()));
    assertTrue(admin.listPeers().stream().anyMatch(peer -> peer.peerId().equals(peerId)));

    admin.removePeer(peerId);
    assertTrue(admin.listPeers().stream().noneMatch(peer -> peer.peerId().equals(peerId)));
  }

  @Test
  public void snapshotThenListSnapshotsThenDeleteSnapshotRoundTrips() throws Exception {
    String tableName = "newshell_snapshot_test";
    String snapshotName = "newshell_snapshot_test_snap";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.snapshot(tableName, snapshotName);
    assertTrue(
      admin.listSnapshots(".*").stream().anyMatch(snap -> snap.name().equals(snapshotName)));

    admin.deleteSnapshot(snapshotName);
    assertTrue(
      admin.listSnapshots(".*").stream().noneMatch(snap -> snap.name().equals(snapshotName)));
  }

  @Test
  public void balanceSwitchNormalizerSwitchAndCatalogJanitorSwitchToggleAndRestore()
    throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    Admin realAdmin = connection.getAdmin();

    boolean previousBalancer = admin.balancerSwitch(false);
    assertFalse(realAdmin.isBalancerEnabled());
    assertFalse(admin.balancerEnabled());
    admin.balancerSwitch(previousBalancer);
    assertEquals(previousBalancer, admin.balancerEnabled());

    boolean previousNormalizer = admin.normalizerSwitch(false);
    assertFalse(realAdmin.isNormalizerEnabled());
    assertFalse(admin.normalizerEnabled());
    admin.normalizerSwitch(previousNormalizer);
    assertEquals(previousNormalizer, admin.normalizerEnabled());

    boolean previousCatalogJanitor = admin.catalogJanitorSwitch(false);
    assertFalse(realAdmin.isCatalogJanitorEnabled());
    assertFalse(admin.catalogJanitorEnabled());
    admin.catalogJanitorSwitch(previousCatalogJanitor);
    assertEquals(previousCatalogJanitor, admin.catalogJanitorEnabled());
  }

  @Test
  public void compactionSwitchTogglesAllRegionServers() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    Map<String, Boolean> previousStates = admin.compactionSwitch(false, List.of());
    assertFalse(previousStates.isEmpty());

    admin.compactionSwitch(true, List.of());
  }

  @Test
  public void splitOrMergeSwitchTogglesSplitAndMerge() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    Admin realAdmin = connection.getAdmin();

    boolean previousSplit = admin.splitOrMergeSwitch("SPLIT", false);
    assertFalse(realAdmin.isSplitEnabled());
    assertFalse(admin.splitOrMergeEnabled("SPLIT"));
    admin.splitOrMergeSwitch("SPLIT", previousSplit);
    assertEquals(previousSplit, admin.splitOrMergeEnabled("SPLIT"));

    boolean previousMerge = admin.splitOrMergeSwitch("MERGE", false);
    assertFalse(realAdmin.isMergeEnabled());
    assertFalse(admin.splitOrMergeEnabled("MERGE"));
    admin.splitOrMergeSwitch("MERGE", previousMerge);
    assertEquals(previousMerge, admin.splitOrMergeEnabled("MERGE"));
  }

  @Test
  public void scanReturnsAllRowsAcrossFamilies() throws Exception {
    String tableName = "newshell_scan_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put1 = new Put(Bytes.toBytes("r1"));
      put1.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      Put put2 = new Put(Bytes.toBytes("r2"));
      put2.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v2"));
      table.put(List.of(put1, put2));
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    ScanResult result = factory.forTable(tableName).scan(Map.of());

    assertEquals(2, result.rows().size());
    assertEquals("r1", result.rows().get(0).row());
    assertEquals("v1", result.rows().get(0).cells().get(0).value());
    assertEquals("r2", result.rows().get(1).row());
  }

  @Test
  public void scanRespectsLimitStartRowAndStopRow() throws Exception {
    String tableName = "newshell_scan_range_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      for (String row : List.of("r1", "r2", "r3", "r4")) {
        Put put = new Put(Bytes.toBytes(row));
        put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v"));
        table.put(put);
      }
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    ScanResult limited = factory.forTable(tableName).scan(Map.of("LIMIT", 1L));
    assertEquals(1, limited.rows().size());
    assertEquals("r1", limited.rows().get(0).row());

    ScanResult ranged =
      factory.forTable(tableName).scan(Map.of("STARTROW", "r2", "STOPROW", "r4"));
    assertEquals(List.of("r2", "r3"),
      ranged.rows().stream().map(ScanRow::row).toList());
  }

  @Test
  public void scanFiltersByColumn() throws Exception {
    String tableName = "newshell_scan_column_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName),
      new byte[][] { Bytes.toBytes("f1"), Bytes.toBytes("f2") });
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      put.addColumn(Bytes.toBytes("f2"), Bytes.toBytes("c1"), Bytes.toBytes("v2"));
      table.put(put);
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    ScanResult result = factory.forTable(tableName).scan(Map.of("COLUMNS", "f1"));

    assertEquals(1, result.rows().size());
    assertEquals(1, result.rows().get(0).cells().size());
    assertEquals("f1", result.rows().get(0).cells().get(0).family());
  }

  @Test
  public void countReturnsNumberOfRows() throws Exception {
    String tableName = "newshell_count_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      for (String row : List.of("r1", "r2", "r3")) {
        Put put = new Put(Bytes.toBytes(row));
        put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v"));
        table.put(put);
      }
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    assertEquals(3L, factory.forTable(tableName).count(Map.of()));
  }

  @Test
  public void deleteRemovesASingleColumn() throws Exception {
    String tableName = "newshell_delete_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c2"), Bytes.toBytes("v2"));
      table.put(put);
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    factory.forTable(tableName).delete("r1", "f1:c1", null);

    GetResult result = factory.forTable(tableName).get("r1", Map.of());
    assertEquals(1, result.cells().size());
    assertEquals("c2", result.cells().get(0).qualifier());
  }

  @Test
  public void deleteallRemovesWholeRow() throws Exception {
    String tableName = "newshell_deleteall_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      table.put(put);
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    factory.forTable(tableName).deleteAll("r1", null, null, Map.of());

    GetResult result = factory.forTable(tableName).get("r1", Map.of());
    assertTrue(result.cells().isEmpty());
  }

  @Test
  public void deleteallWithRowPrefixFilterBatchDeletesMatchingRows() throws Exception {
    String tableName = "newshell_deleteall_prefix_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      for (String row : List.of("prefix-1", "prefix-2", "other-1")) {
        Put put = new Put(Bytes.toBytes(row));
        put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v"));
        table.put(put);
      }
    }

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    factory.forTable(tableName).deleteAll(null, null, null,
      Map.of("ROWPREFIXFILTER", "prefix-", "CACHE", 1L));

    ScanResult remaining = factory.forTable(tableName).scan(Map.of());
    assertEquals(List.of("other-1"), remaining.rows().stream().map(ScanRow::row).toList());
  }

  @Test
  public void getCounterReadsAnIncrementedValue() throws Exception {
    String tableName = "newshell_get_counter_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    assertEquals(3L, factory.forTable(tableName).increment("r1", "f1:c1", 3L));
    assertEquals(3L, factory.forTable(tableName).getCounter("r1", "f1:c1"));
  }

  @Test
  public void getCounterReturnsNullWhenMissing() throws Exception {
    String tableName = "newshell_get_counter_missing_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    assertEquals(null, factory.forTable(tableName).getCounter("r1", "f1:c1"));
  }

  @Test
  public void incrementAccumulatesAcrossCalls() throws Exception {
    String tableName = "newshell_incr_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    factory.forTable(tableName).increment("r1", "f1:c1", 5L);
    assertEquals(8L, factory.forTable(tableName).increment("r1", "f1:c1", 3L));
  }

  @Test
  public void appendConcatenatesAndReturnsCurrentValue() throws Exception {
    String tableName = "newshell_append_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    factory.forTable(tableName).append("r1", "f1:c1", "a");
    assertEquals("ab", factory.forTable(tableName).append("r1", "f1:c1", "b"));
  }

  @Test
  public void getSplitsExcludesTheFirstEmptyStartKey() throws Exception {
    String tableName = "newshell_get_splits_test";
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createTable(tableName, List.of(Map.of("NAME", "f1")),
      Map.of("SPLITS", List.of("1000", "2000")));

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    assertEquals(List.of("1000", "2000"), factory.forTable(tableName).getSplits());
  }

  @Test
  public void truncateRecreatesAnEmptyTable() throws Exception {
    String tableName = "newshell_truncate_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      table.put(put);
    }

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.truncateTable(tableName, false);

    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    assertTrue(factory.forTable(tableName).scan(Map.of()).rows().isEmpty());
  }

  @Test
  public void truncatePreserveKeepsSplitsAfterTruncate() throws Exception {
    String tableName = "newshell_truncate_preserve_test";
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createTable(tableName, List.of(Map.of("NAME", "f1")),
      Map.of("SPLITS", List.of("1000", "2000")));

    admin.truncateTable(tableName, true);

    Admin realAdmin = connection.getAdmin();
    long primaryRegionCount = realAdmin.getRegions(TableName.valueOf(tableName)).stream()
      .filter(region -> region.getReplicaId() == 0).count();
    assertEquals(3, primaryRegionCount);
  }

  @Test
  public void isTableDisabledAndIsTableEnabledReflectTableState() throws Exception {
    String tableName = "newshell_is_disabled_test";
    Admin realAdmin = connection.getAdmin();
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    assertTrue(admin.isTableEnabled(tableName));
    assertFalse(admin.isTableDisabled(tableName));

    realAdmin.disableTable(TableName.valueOf(tableName));
    assertTrue(admin.isTableDisabled(tableName));
    assertFalse(admin.isTableEnabled(tableName));
  }

  @Test
  public void listTablesByStateFiltersOnEnabledFlag() throws Exception {
    String enabledTable = "newshell_list_by_state_enabled_test";
    String disabledTable = "newshell_list_by_state_disabled_test";
    Admin realAdmin = connection.getAdmin();
    TEST_UTIL.createTable(TableName.valueOf(enabledTable), Bytes.toBytes("f1"));
    TEST_UTIL.createTable(TableName.valueOf(disabledTable), Bytes.toBytes("f1"));
    realAdmin.disableTable(TableName.valueOf(disabledTable));

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    assertTrue(admin.listTablesByState(true).contains(enabledTable));
    assertFalse(admin.listTablesByState(true).contains(disabledTable));
    assertTrue(admin.listTablesByState(false).contains(disabledTable));
    assertFalse(admin.listTablesByState(false).contains(enabledTable));
  }

  @Test
  public void alterStatusReportsAllRegionsUpdatedAfterAlter() throws Exception {
    String tableName = "newshell_alter_status_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.alterTable(tableName, List.of(Map.of("NAME", "f1", "TTL", 100)));

    AlterStatusView status = admin.alterStatus(tableName);
    assertEquals(status.totalRegions(), status.totalRegions() - status.regionsYetToUpdate());
  }

  @Test
  public void cloneTableSchemaCopiesFamiliesWithoutData() throws Exception {
    String tableName = "newshell_clone_schema_src_test";
    String newTableName = "newshell_clone_schema_dst_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));
    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      table.put(put);
    }

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.cloneTableSchema(tableName, newTableName, true);

    Admin realAdmin = connection.getAdmin();
    assertTrue(realAdmin.tableExists(TableName.valueOf(newTableName)));
    assertTrue(
      realAdmin.getDescriptor(TableName.valueOf(newTableName)).hasColumnFamily(Bytes.toBytes("f1")));
    ShellTableFactory factory = new DefaultShellTableFactory(connection);
    assertTrue(factory.forTable(newTableName).scan(Map.of()).rows().isEmpty());
  }

  @Test
  public void locateRegionReturnsHostAndRegionForRowKey() throws Exception {
    String tableName = "newshell_locate_region_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    RegionLocationView location = admin.locateRegion(tableName, "r1");
    assertNotNull(location.hostnamePort());
    assertTrue(location.regionName().startsWith(tableName));
  }

  @Test
  public void listRegionsReturnsOneRowPerRegion() throws Exception {
    String tableName = "newshell_list_regions_test";
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createTable(tableName, List.of(Map.of("NAME", "f1")),
      Map.of("SPLITS", List.of("1000", "2000")));

    List<List<String>> rows = admin.listRegions(tableName);
    assertEquals(3, rows.size());
    assertTrue(rows.get(0).get(1).startsWith(tableName));
  }
}
