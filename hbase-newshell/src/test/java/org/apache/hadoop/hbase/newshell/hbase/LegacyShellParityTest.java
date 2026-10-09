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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Consistency;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.quotas.QuotaSettings;
import org.apache.hadoop.hbase.quotas.ThrottleSettings;
import org.apache.hadoop.hbase.quotas.ThrottleType;
import org.apache.hadoop.hbase.replication.ReplicationPeerConfig;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavior-parity tests against the legacy JRuby shell: delete semantics, add_peer, quotas,
 * alter_async, enable/disable and binary region names.
 */
@Tag(SmallTests.TAG)
public class LegacyShellParityTest {

  private Admin admin;
  private ShellAdmin shellAdmin;

  @BeforeEach
  public void setUp() {
    admin = mock(Admin.class);
    Configuration conf = HBaseConfiguration.create();
    when(admin.getConfiguration()).thenReturn(conf);
    shellAdmin = DefaultShellAdmin.create(admin);
  }

  private static Map<String, Object> map(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }

  // ---- add_peer ----

  private ReplicationPeerConfig addPeerAndCapture(Map<String, Object> spec) throws IOException {
    shellAdmin.addPeer("1", spec);
    ArgumentCaptor<ReplicationPeerConfig> captor =
      ArgumentCaptor.forClass(ReplicationPeerConfig.class);
    verify(admin).addReplicationPeer(any(String.class), captor.capture(), anyBoolean());
    return captor.getValue();
  }

  @Test
  public void addPeerWithTableCfsDisablesReplicateAll() throws IOException {
    ReplicationPeerConfig config = addPeerAndCapture(
      map("CLUSTER_KEY", "zk:2181:/hbase", "TABLE_CFS", map("t1", Collections.emptyList())));
    assertFalse(config.replicateAllUserTables());
  }

  @Test
  public void addPeerWithNamespacesDisablesReplicateAll() throws IOException {
    ReplicationPeerConfig config =
      addPeerAndCapture(map("CLUSTER_KEY", "zk:2181:/hbase", "NAMESPACES", Arrays.asList("ns1")));
    assertFalse(config.replicateAllUserTables());
  }

  @Test
  public void addPeerRejectsWrongTypedValues() {
    assertThrows(ClientErrorException.class,
      () -> shellAdmin.addPeer("1", map("CLUSTER_KEY", "zk:2181:/hbase", "NAMESPACES", "ns1")));
    assertThrows(ClientErrorException.class,
      () -> shellAdmin.addPeer("1", map("CLUSTER_KEY", "zk:2181:/hbase", "TABLE_CFS", "t1:cf")));
    assertThrows(ClientErrorException.class,
      () -> shellAdmin.addPeer("1", map("CLUSTER_KEY", "zk:2181:/hbase", "CONFIG", "x")));
    assertThrows(ClientErrorException.class,
      () -> shellAdmin.addPeer("1", map("CLUSTER_KEY", "zk:2181:/hbase", "DATA", "x")));
  }

  @Test
  public void addPeerEndpointWithoutClusterKeyUsesLocalClusterKey() throws IOException {
    ReplicationPeerConfig config = addPeerAndCapture(map("ENDPOINT_CLASSNAME", "com.example.Ep"));
    assertEquals(
      org.apache.hadoop.hbase.zookeeper.ZKConfig.getZooKeeperClusterKey(admin.getConfiguration()),
      config.getClusterKey());
  }

  // ---- enable / disable ----

  @Test
  public void disableAndEnableAreIdempotent() throws IOException {
    TableName t = TableName.valueOf("t");
    when(admin.tableExists(t)).thenReturn(true);
    when(admin.isTableDisabled(t)).thenReturn(true);
    when(admin.isTableEnabled(t)).thenReturn(true);
    shellAdmin.disableTable("t");
    shellAdmin.enableTable("t");
    verify(admin, never()).disableTable(any(TableName.class));
    verify(admin, never()).enableTable(any(TableName.class));
  }

  // ---- alter_async ----

  @Test
  @SuppressWarnings("unchecked")
  public void alterNoWaitDoesNotBlockOnFuture() throws Exception {
    TableName t = TableName.valueOf("t");
    TableDescriptor desc = TableDescriptorBuilder.newBuilder(t)
      .setColumnFamily(ColumnFamilyDescriptorBuilder.of("f1")).build();
    when(admin.tableExists(t)).thenReturn(true);
    when(admin.getDescriptor(t)).thenReturn(desc);
    Future<Void> future = mock(Future.class);
    when(admin.modifyTableAsync(any(TableDescriptor.class), anyBoolean())).thenReturn(future);

    shellAdmin.alterTableNoWait("t", Collections.singletonList(map("NAME", "f1", "TTL", 100)));

    verify(admin).modifyTableAsync(any(TableDescriptor.class), anyBoolean());
    verify(future, never()).get();
  }

  // ---- region names are binary-decoded ----

  @Test
  public void splitDecodesHexEscapes() throws IOException {
    @SuppressWarnings("unchecked")
    Future<Void> future = mock(Future.class);
    when(admin.splitRegionAsync(any(byte[].class), any(byte[].class))).thenReturn(future);
    shellAdmin.split("region", "\\x00\\x01");
    ArgumentCaptor<byte[]> split = ArgumentCaptor.forClass(byte[].class);
    verify(admin).splitRegionAsync(any(byte[].class), split.capture());
    assertArrayEquals(new byte[] { 0, 1 }, split.getValue());
  }

  // ---- quotas ----

  private QuotaSettings setQuotaAndCapture(Map<String, Object> args) throws IOException {
    shellAdmin.setQuota(args);
    ArgumentCaptor<QuotaSettings> captor = ArgumentCaptor.forClass(QuotaSettings.class);
    verify(admin).setQuota(captor.capture());
    return captor.getValue();
  }

  @Test
  public void unthrottleHonorsThrottleType() throws IOException {
    QuotaSettings settings = setQuotaAndCapture(
      map("TYPE", "THROTTLE", "USER", "u1", "THROTTLE_TYPE", "WRITE_NUMBER", "LIMIT", "NONE"));
    assertEquals(ThrottleType.WRITE_NUMBER, ((ThrottleSettings) settings).getThrottleType());
  }

  @Test
  public void throttleAndUnthrottleRejectUnknownKeys() {
    assertThrows(ClientErrorException.class, () -> shellAdmin
      .setQuota(map("TYPE", "THROTTLE", "USER", "u1", "TABEL", "t1", "LIMIT", "10req/sec")));
    assertThrows(ClientErrorException.class, () -> shellAdmin
      .setQuota(map("TYPE", "THROTTLE", "USER", "u1", "TABEL", "t1", "LIMIT", "NONE")));
  }

  @Test
  public void throttleRejectsZeroLimit() {
    assertThrows(ClientErrorException.class,
      () -> shellAdmin.setQuota(map("TYPE", "THROTTLE", "USER", "u1", "LIMIT", "0req/sec")));
  }

  // ---- items from the review report ----

  @Test
  public void countAppliesQueryOptions() throws Exception {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.<Result> emptyList().iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    new DefaultShellTable(table).count(map("CONSISTENCY", "TIMELINE"), (c, r) -> {
    });
    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    assertEquals(Consistency.TIMELINE, captor.getValue().getConsistency());
  }

  @Test
  public void putAcceptsEmptyQualifier() throws Exception {
    Table table = mock(Table.class);
    new DefaultShellTable(table).put("r", "cf:", "v", Collections.emptyMap());
    ArgumentCaptor<Put> captor = ArgumentCaptor.forClass(Put.class);
    verify(table).put(captor.capture());
    Cell cell = captor.getValue().getFamilyCellMap().values().iterator().next().get(0);
    assertEquals(0, cell.getQualifierLength());
  }

  @Test
  public void compactDecodesNonAsciiRegionNameAsUtf8() throws Exception {
    shellAdmin.compact("é", null, null);
    verify(admin).compactRegion(Bytes.toBytes("é"));
  }

  @Test
  public void throttleScopeIsCaseInsensitive() throws IOException {
    QuotaSettings settings = setQuotaAndCapture(
      map("TYPE", "THROTTLE", "USER", "u1", "LIMIT", "10req/sec", "SCOPE", "cluster"));
    assertTrue(settings instanceof ThrottleSettings);
  }

  @Test
  public void throttleRejectsTableAndNamespaceTogether() {
    assertThrows(ClientErrorException.class, () -> shellAdmin
      .setQuota(map("TYPE", "THROTTLE", "TABLE", "t1", "NAMESPACE", "ns", "LIMIT", "10req/sec")));
    assertThrows(ClientErrorException.class, () -> shellAdmin
      .setQuota(map("TYPE", "THROTTLE", "TABLE", "t1", "NAMESPACE", "ns", "LIMIT", "NONE")));
  }

  @Test
  public void throttleRejectsInvalidThrottleType() {
    assertThrows(ClientErrorException.class, () -> shellAdmin.setQuota(
      map("TYPE", "THROTTLE", "USER", "u1", "THROTTLE_TYPE", "BOGUS", "LIMIT", "10req/sec")));
  }

  // ---- delete semantics ----

  private static Delete captureDelete(Table table) throws IOException {
    ArgumentCaptor<Delete> captor = ArgumentCaptor.forClass(Delete.class);
    verify(table).delete(captor.capture());
    return captor.getValue();
  }

  private static Cell onlyCell(Delete delete) {
    List<Cell> cells = delete.getFamilyCellMap().values().iterator().next();
    assertEquals(1, cells.size());
    return cells.get(0);
  }

  @Test
  public void singleVersionDeleteOfFamilyUsesFamilyVersion() throws Exception {
    Table table = mock(Table.class);
    new DefaultShellTable(table).delete("r", "cf", 5L, Collections.emptyMap());
    Cell cell = onlyCell(captureDelete(table));
    assertEquals(Cell.Type.DeleteFamilyVersion, cell.getType());
    assertEquals(5L, cell.getTimestamp());
  }

  @Test
  public void deleteAllOfEmptyQualifierDoesNotWipeFamily() throws Exception {
    Table table = mock(Table.class);
    new DefaultShellTable(table).deleteAll("r", "cf:", null, Collections.emptyMap());
    Cell cell = onlyCell(captureDelete(table));
    assertEquals(Cell.Type.DeleteColumn, cell.getType());
    assertEquals(0, cell.getQualifierLength());
  }

  @Test
  public void deleteAllOfBareFamilyStillWipesFamily() throws Exception {
    Table table = mock(Table.class);
    new DefaultShellTable(table).deleteAll("r", "cf", null, Collections.emptyMap());
    assertEquals(Cell.Type.DeleteFamily, onlyCell(captureDelete(table)).getType());
  }

  @Test
  public void metaDeletesAreGuarded() throws Exception {
    Table table = mock(Table.class);
    when(table.getName()).thenReturn(TableName.META_TABLE_NAME);
    when(table.get(any(Get.class))).thenReturn(Result.EMPTY_RESULT);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Exception prefix = assertThrows(Exception.class,
      () -> shellTable.deleteAll(null, null, null, map("ROWPREFIXFILTER", "t1")));
    assertTrue(prefix.getMessage().contains("ROWPREFIXFILTER in hbase:meta"));
    Exception missing = assertThrows(Exception.class,
      () -> shellTable.deleteAll("nope", null, null, Collections.emptyMap()));
    assertEquals("Row Not Found", missing.getMessage());
    verify(table, never()).delete(any(Delete.class));
  }
}
