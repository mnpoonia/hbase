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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.filter.ValueFilter;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag(SmallTests.TAG)
public class DefaultShellTableTest {

  @Test
  public void scanDoesNotOpenScannerUntilForEachRowIsCalled()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    shellTable.scan(Collections.emptyMap());

    verify(table, never()).getScanner(any(Scan.class));
  }

  @Test
  public void scanClosesScannerExactlyOnceWhenRowConsumerThrows()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    Result result = mock(Result.class);
    when(result.getRow()).thenReturn(Bytes.toBytes("r1"));
    when(result.listCells()).thenReturn(Collections.emptyList());
    when(scanner.iterator()).thenReturn(Collections.singletonList(result).iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);

    DefaultShellTable shellTable = new DefaultShellTable(table);
    ScanResult scanResult = shellTable.scan(Collections.emptyMap());

    RuntimeException boom = new RuntimeException("boom");
    RuntimeException thrown =
      assertThrows(RuntimeException.class, () -> scanResult.forEachRow(row -> {
        throw boom;
      }));

    assertSame(boom, thrown);
    verify(scanner, times(1)).close();
  }

  @Test
  public void scanClosesScannerExactlyOnceOnNormalCompletion()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    Result result = mock(Result.class);
    when(result.getRow()).thenReturn(Bytes.toBytes("r1"));
    when(result.listCells()).thenReturn(Collections.emptyList());
    when(scanner.iterator()).thenReturn(Collections.singletonList(result).iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);

    DefaultShellTable shellTable = new DefaultShellTable(table);
    ScanResult scanResult = shellTable.scan(Collections.emptyMap());

    int[] count = { 0 };
    scanResult.forEachRow(row -> {
      assertEquals("r1", row.row());
      count[0]++;
    });

    assertEquals(1, count[0]);
    verify(scanner, times(1)).close();
  }

  @Test
  public void scanAppliesFilterOption() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("FILTER", "PrefixFilter('row')");
    ScanResult scanResult = shellTable.scan(options);
    scanResult.forEachRow(row -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    assertInstanceOf(PrefixFilter.class, captor.getValue().getFilter());
  }

  @Test
  public void scanAppliesTimerangeOption() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("TIMERANGE", Arrays.asList(100L, 200L));
    ScanResult scanResult = shellTable.scan(options);
    scanResult.forEachRow(row -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    assertEquals(100L, captor.getValue().getTimeRange().getMin());
    assertEquals(200L, captor.getValue().getTimeRange().getMax());
  }

  @Test
  public void scanThrowsShellCommandExceptionForMalformedFilter() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("FILTER", "NotARealFilter(=, 'binary:abc')");
    assertThrows(ShellCommandException.class, () -> shellTable.scan(options));
  }

  @Test
  public void scanThrowsShellCommandExceptionForWrongTimerangeArity() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("TIMERANGE", Collections.singletonList(100L));
    assertThrows(ShellCommandException.class, () -> shellTable.scan(options));
  }

  private static void assertUnsupportedOption(ThrowingCall call, String option) {
    ShellCommandException e = assertThrows(ShellCommandException.class, call::run);
    assertTrue(e.getMessage().contains("Unsupported option '" + option + "'"), e.getMessage());
  }

  private static Map<String, Object> opts(String key, Object value) {
    Map<String, Object> m = new HashMap<>();
    m.put(key, value);
    return m;
  }

  @FunctionalInterface
  private interface ThrowingCall {
    void run() throws Exception;
  }

  @Test
  public void scanRejectsUnsupportedOptions() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    for (String option : new String[] { "BOGUS", "INTERVAL" }) {
      assertUnsupportedOption(() -> shellTable.scan(opts(option, "x")), option);
    }
  }

  @Test
  public void getPutCountAndDeleteallRejectUnsupportedOptions() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    assertUnsupportedOption(() -> shellTable.get("r", opts("RAW", true)), "RAW");
    assertUnsupportedOption(() -> shellTable.put("r", "cf:c", "v", opts("ACL", "x")), "ACL");
    assertUnsupportedOption(() -> shellTable.count(opts("REVERSED", true), (c, r) -> {
    }), "REVERSED");
    assertUnsupportedOption(() -> shellTable.deleteAll("r", null, null, opts("RAW", true)), "RAW");
  }

  @Test
  public void scanRejectsNonStringFilter() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    ShellCommandException e = assertThrows(ShellCommandException.class,
      () -> shellTable.scan(opts("FILTER", Long.valueOf(5))));
    assertTrue(e.getMessage().contains("FILTER must be a filter string"), e.getMessage());
  }

  @Test
  public void scanThrowsShellCommandExceptionForNonNumericLimit() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("LIMIT", "ten");
    assertThrows(ShellCommandException.class, () -> shellTable.scan(options));
  }

  @Test
  public void countThrowsShellCommandExceptionForNonNumericInterval() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("INTERVAL", "many");
    assertThrows(ShellCommandException.class, () -> shellTable.count(options, (count, row) -> {
    }));
  }

  @Test
  public void countHonoursRowPrefixAndColumnLikeScan() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.<Result> emptyList().iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("ROWPREFIXFILTER", "ab");
    options.put("COLUMN", "cf:q");
    shellTable.count(options, (count, row) -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    assertEquals("ab", Bytes.toString(captor.getValue().getStartRow()));
    assertTrue(captor.getValue().getFamilyMap().containsKey(Bytes.toBytes("cf")));
  }

  @Test
  public void countRejectsNonPositiveInterval() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    for (Object interval : new Object[] { 0, -5 }) {
      Map<String, Object> options = new HashMap<>();
      options.put("INTERVAL", interval);
      assertThrows(ShellCommandException.class, () -> shellTable.count(options, (count, row) -> {
      }));
    }
  }

  @Test
  public void countRejectsNonBooleanCacheBlocks() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    Map<String, Object> options = new HashMap<>();
    options.put("CACHE_BLOCKS", "ture");
    assertThrows(ShellCommandException.class, () -> shellTable.count(options, (count, row) -> {
    }));
  }

  @Test
  public void getAppliesFilterOption() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    when(table.get(any(Get.class))).thenReturn(Result.create(Collections.emptyList()));
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("FILTER", "ValueFilter(=, 'binary:abc')");
    shellTable.get("r1", options);

    ArgumentCaptor<Get> captor = ArgumentCaptor.forClass(Get.class);
    verify(table).get(captor.capture());
    assertInstanceOf(ValueFilter.class, captor.getValue().getFilter());
  }

  @Test
  public void getAppliesTimerangeOption() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    when(table.get(any(Get.class))).thenReturn(Result.create(Collections.emptyList()));
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("TIMERANGE", Arrays.asList(100L, 200L));
    shellTable.get("r1", options);

    ArgumentCaptor<Get> captor = ArgumentCaptor.forClass(Get.class);
    verify(table).get(captor.capture());
    assertEquals(100L, captor.getValue().getTimeRange().getMin());
    assertEquals(200L, captor.getValue().getTimeRange().getMax());
  }

  @Test
  public void getThrowsShellCommandExceptionForMalformedFilter() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("FILTER", "NotARealFilter(=, 'binary:abc')");
    assertThrows(ShellCommandException.class, () -> shellTable.get("r1", options));
  }

  @Test
  public void getThrowsShellCommandExceptionForWrongTimerangeArity() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("TIMERANGE", Arrays.asList(100L, 200L, 300L));
    assertThrows(ShellCommandException.class, () -> shellTable.get("r1", options));
  }

  @Test
  public void countAppliesFilterOption() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("FILTER", "PrefixFilter('row')");
    shellTable.count(options, (count, row) -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    FilterList list = assertInstanceOf(FilterList.class, captor.getValue().getFilter());
    assertInstanceOf(PrefixFilter.class, list.getFilters().get(0));
    assertInstanceOf(FirstKeyOnlyFilter.class, list.getFilters().get(1));
    assertInstanceOf(KeyOnlyFilter.class, list.getFilters().get(2));
  }

  @Test
  public void countWithoutFilterStillAddsFirstKeyOnlyAndKeyOnly()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    new DefaultShellTable(table).count(new HashMap<>(), (count, row) -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    FilterList list = assertInstanceOf(FilterList.class, captor.getValue().getFilter());
    assertInstanceOf(FirstKeyOnlyFilter.class, list.getFilters().get(0));
    assertInstanceOf(KeyOnlyFilter.class, list.getFilters().get(1));
  }

  @Test
  public void countDefaultsCacheBlocksToFalse() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    shellTable.count(Collections.emptyMap(), (count, row) -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    assertEquals(false, captor.getValue().getCacheBlocks());
  }

  @Test
  public void countCoercesCacheBlocksBooleanAndStringValues()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> trueBool = new HashMap<>();
    trueBool.put("CACHE_BLOCKS", Boolean.TRUE);
    shellTable.count(trueBool, (count, row) -> {
    });

    Map<String, Object> trueString = new HashMap<>();
    trueString.put("CACHE_BLOCKS", "true");
    shellTable.count(trueString, (count, row) -> {
    });

    Map<String, Object> falseString = new HashMap<>();
    falseString.put("CACHE_BLOCKS", "false");
    shellTable.count(falseString, (count, row) -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table, times(3)).getScanner(captor.capture());
    assertEquals(true, captor.getAllValues().get(0).getCacheBlocks());
    assertEquals(true, captor.getAllValues().get(1).getCacheBlocks());
    assertEquals(false, captor.getAllValues().get(2).getCacheBlocks());
  }

  @Test
  public void countThrowsShellCommandExceptionForInvalidCacheBlocksString() {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("CACHE_BLOCKS", "nonsense");
    assertThrows(ShellCommandException.class, () -> shellTable.count(options, (count, row) -> {
    }));
  }

  @Test
  public void countReportsProgressEveryIntervalRows() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    List<Result> results = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Result result = mock(Result.class);
      when(result.getRow()).thenReturn(Bytes.toBytes("r" + i));
      results.add(result);
    }
    when(scanner.iterator()).thenReturn(results.iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("INTERVAL", 2);
    List<long[]> progress = new ArrayList<>();
    List<String> rows = new ArrayList<>();
    long count = shellTable.count(options, (cnt, row) -> {
      progress.add(new long[] { cnt });
      rows.add(row);
    });

    assertEquals(5L, count);
    assertEquals(2, progress.size());
    assertEquals(2L, progress.get(0)[0]);
    assertEquals("r2", rows.get(0));
    assertEquals(4L, progress.get(1)[0]);
    assertEquals("r4", rows.get(1));
  }

  @Test
  public void countDefaultsIntervalTo1000SoFewerRowsNeverReportProgress()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    Result result = mock(Result.class);
    when(result.getRow()).thenReturn(Bytes.toBytes("r1"));
    when(scanner.iterator()).thenReturn(Collections.singletonList(result).iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    int[] progressCalls = { 0 };
    long count = shellTable.count(Collections.emptyMap(), (cnt, row) -> progressCalls[0]++);

    assertEquals(1L, count);
    assertEquals(0, progressCalls[0]);
  }

  private static Result resultOf(byte[] row, byte[] family, byte[] qualifier, byte[] value) {
    return Result
      .create(new org.apache.hadoop.hbase.Cell[] { org.apache.hadoop.hbase.CellBuilderFactory
        .create(org.apache.hadoop.hbase.CellBuilderType.DEEP_COPY).setRow(row).setFamily(family)
        .setQualifier(qualifier).setTimestamp(1L).setType(org.apache.hadoop.hbase.Cell.Type.Put)
        .setValue(value).build() });
  }

  @Test
  public void getAppliesAttributesAuthorizationsConsistencyAndReplica() throws Exception {
    Table table = mock(Table.class);
    when(table.get(any(org.apache.hadoop.hbase.client.Get.class)))
      .thenReturn(Result.create(new org.apache.hadoop.hbase.Cell[0]));
    Map<String, Object> options = new java.util.LinkedHashMap<>();
    options.put("ATTRIBUTES", opts("mykey", "myvalue"));
    options.put("AUTHORIZATIONS", Arrays.asList("PRIVATE", "SECRET"));
    options.put("CONSISTENCY", "TIMELINE");
    options.put("REGION_REPLICA_ID", 1L);
    new DefaultShellTable(table).get("r1", options);

    ArgumentCaptor<org.apache.hadoop.hbase.client.Get> captor =
      ArgumentCaptor.forClass(org.apache.hadoop.hbase.client.Get.class);
    verify(table).get(captor.capture());
    org.apache.hadoop.hbase.client.Get get = captor.getValue();
    assertEquals("myvalue", Bytes.toString(get.getAttribute("mykey")));
    assertEquals(Arrays.asList("PRIVATE", "SECRET"), get.getAuthorizations().getLabels());
    assertEquals(org.apache.hadoop.hbase.client.Consistency.TIMELINE, get.getConsistency());
    assertEquals(1, get.getReplicaId());
  }

  @Test
  public void getRejectsBadConsistencyAttributesAndAuthorizations() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    assertThrows(ShellCommandException.class,
      () -> shellTable.get("r1", opts("CONSISTENCY", "SOMETIMES")));
    assertThrows(ShellCommandException.class,
      () -> shellTable.get("r1", opts("ATTRIBUTES", "notahash")));
    assertThrows(ShellCommandException.class,
      () -> shellTable.get("r1", opts("AUTHORIZATIONS", "SECRET")));
  }

  @Test
  public void getFormatsValuesWithGlobalAndPerColumnConverters() throws Exception {
    Table table = mock(Table.class);
    byte[] f = Bytes.toBytes("f");
    when(table.get(any(org.apache.hadoop.hbase.client.Get.class)))
      .thenReturn(resultOf(Bytes.toBytes("r1"), f, Bytes.toBytes("n"), Bytes.toBytes(42)));
    DefaultShellTable shellTable = new DefaultShellTable(table);

    // default: toStringBinary escapes the int bytes
    assertEquals("\\x00\\x00\\x00*",
      shellTable.get("r1", Collections.emptyMap()).cells().get(0).value());
    // per-column converter in the column spec
    assertEquals("42", shellTable.get("r1", opts("COLUMN", "f:n:toInt")).cells().get(0).value());
    // custom class form
    assertEquals("42",
      shellTable.get("r1", opts("COLUMN", "f:n:c(org.apache.hadoop.hbase.util.Bytes).toInt"))
        .cells().get(0).value());
    // global FORMATTER applies to value, family and qualifier
    GetResult formatted = shellTable.get("r1", opts("FORMATTER", "toString"));
    assertEquals("f", formatted.cells().get(0).family());
    assertEquals("n", formatted.cells().get(0).qualifier());
    assertThrows(ShellCommandException.class,
      () -> shellTable.get("r1", opts("FORMATTER", "noSuchMethod")));
  }

  @Test
  public void scanAppliesScanOptions() throws Exception {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.<Result> emptyList().iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    Map<String, Object> options = new java.util.LinkedHashMap<>();
    options.put("ROWPREFIXFILTER", "row2");
    options.put("REVERSED", true);
    options.put("RAW", true);
    options.put("CACHE", 50L);
    options.put("CACHE_BLOCKS", false);
    options.put("BATCH", 5L);
    options.put("MAX_RESULT_SIZE", 1234L);
    options.put("TIMESTAMP", 77L);
    options.put("ISOLATION_LEVEL", "READ_UNCOMMITTED");
    options.put("READ_TYPE", "PREAD");
    options.put("ALLOW_PARTIAL_RESULTS", true);
    options.put("CONSISTENCY", "TIMELINE");
    options.put("REGION_REPLICA_ID", 2L);
    options.put("AUTHORIZATIONS", Arrays.asList("A", "B"));
    options.put("ATTRIBUTES", opts("k", "v"));
    new DefaultShellTable(table).scan(options).forEachRow(r -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    Scan scan = captor.getValue();
    assertEquals("row2", Bytes.toString(scan.getStartRow()));
    assertTrue(scan.isReversed() && scan.isRaw() && !scan.getCacheBlocks());
    assertEquals(50, scan.getCaching());
    assertEquals(5, scan.getBatch());
    assertEquals(1234L, scan.getMaxResultSize());
    assertEquals(77L, scan.getTimeRange().getMin());
    assertEquals(org.apache.hadoop.hbase.client.IsolationLevel.READ_UNCOMMITTED,
      scan.getIsolationLevel());
    assertEquals(Scan.ReadType.PREAD, scan.getReadType());
    assertTrue(scan.getAllowPartialResults());
    assertEquals(org.apache.hadoop.hbase.client.Consistency.TIMELINE, scan.getConsistency());
    assertEquals(2, scan.getReplicaId());
    assertEquals(Arrays.asList("A", "B"), scan.getAuthorizations().getLabels());
    assertEquals("v", Bytes.toString(scan.getAttribute("k")));
  }

  @Test
  public void scanRejectsBadBooleanAndEnumValues() {
    DefaultShellTable shellTable = new DefaultShellTable(mock(Table.class));
    assertThrows(ShellCommandException.class, () -> shellTable.scan(opts("REVERSED", "maybe")));
    assertThrows(ShellCommandException.class, () -> shellTable.scan(opts("READ_TYPE", "NOPE")));
    assertThrows(ShellCommandException.class,
      () -> shellTable.scan(opts("ISOLATION_LEVEL", "NOPE")));
  }

  @Test
  public void scanFormatsRowKeysAndColumnsAndShowsDeleteMarkers() throws Exception {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    byte[] f = Bytes.toBytes("f");
    org.apache.hadoop.hbase.Cell put = org.apache.hadoop.hbase.CellBuilderFactory
      .create(org.apache.hadoop.hbase.CellBuilderType.DEEP_COPY).setRow(Bytes.toBytes("r1"))
      .setFamily(f).setQualifier(Bytes.toBytes("n")).setTimestamp(1L)
      .setType(org.apache.hadoop.hbase.Cell.Type.Put).setValue(Bytes.toBytes(42)).build();
    org.apache.hadoop.hbase.Cell del = org.apache.hadoop.hbase.CellBuilderFactory
      .create(org.apache.hadoop.hbase.CellBuilderType.DEEP_COPY).setRow(Bytes.toBytes("r1"))
      .setFamily(f).setQualifier(Bytes.toBytes("m")).setTimestamp(2L)
      .setType(org.apache.hadoop.hbase.Cell.Type.DeleteColumn).build();
    Result result = Result.create(new org.apache.hadoop.hbase.Cell[] { del, put });
    when(scanner.iterator()).thenReturn(Collections.singletonList(result).iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);

    java.util.List<ScanRow> rows = new java.util.ArrayList<>();
    new DefaultShellTable(table).scan(opts("COLUMNS", "f:n:toInt")).forEachRow(rows::add);
    assertEquals("r1", rows.get(0).row());
    assertEquals("DeleteColumn", rows.get(0).cells().get(0).deleteType());
    assertEquals("42", rows.get(0).cells().get(1).value());
  }

  @Test
  public void putAppliesAttributesVisibilityAndTtl() throws Exception {
    Table table = mock(Table.class);
    Map<String, Object> options = new java.util.LinkedHashMap<>();
    options.put("ATTRIBUTES", opts("k", "v"));
    options.put("VISIBILITY", "PRIVATE|SECRET");
    options.put("TTL", 5000L);
    options.put("TIMESTAMP", 9L);
    new DefaultShellTable(table).put("r1", "f:c", "v1", options);

    ArgumentCaptor<Put> captor = ArgumentCaptor.forClass(Put.class);
    verify(table).put(captor.capture());
    Put put = captor.getValue();
    assertEquals("v", Bytes.toString(put.getAttribute("k")));
    assertEquals("PRIVATE|SECRET", put.getCellVisibility().getExpression());
    assertEquals(5000L, put.getTTL());
  }

  @Test
  public void scanMetricsAreCollectedAndFiltered() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    org.apache.hadoop.hbase.client.metrics.ScanMetrics scanMetrics =
      new org.apache.hadoop.hbase.client.metrics.ScanMetrics();
    scanMetrics.countOfRPCcalls.incrementAndGet();
    when(scanner.getScanMetrics()).thenReturn(scanMetrics);
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    Map<String, Object> options = new HashMap<>();
    options.put("METRICS", Arrays.asList("RPC_CALLS"));
    ScanResult result = shellTable.scan(options);
    result.forEachRow(row -> {
    });

    ArgumentCaptor<Scan> captor = ArgumentCaptor.forClass(Scan.class);
    verify(table).getScanner(captor.capture());
    assertTrue(captor.getValue().isScanMetricsEnabled());
    assertEquals(Collections.singletonMap("RPC_CALLS", 1L), result.metrics());
  }

  @Test
  public void scanWithoutMetricOptionsLeavesMetricsOffAndEmpty()
    throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    ResultScanner scanner = mock(ResultScanner.class);
    when(scanner.iterator()).thenReturn(Collections.emptyIterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);
    ScanResult result = new DefaultShellTable(table).scan(new HashMap<>());
    result.forEachRow(row -> {
    });
    assertTrue(result.metrics().isEmpty());
  }

  @Test
  public void metaTableServerStartCodeIsShownAsLong() throws IOException, ShellCommandException {
    Table table = mock(Table.class);
    when(table.getName()).thenReturn(org.apache.hadoop.hbase.TableName.META_TABLE_NAME);
    ResultScanner scanner = mock(ResultScanner.class);
    Result result = Result.create(new org.apache.hadoop.hbase.Cell[] {
      new org.apache.hadoop.hbase.KeyValue(Bytes.toBytes("r"), Bytes.toBytes("info"),
        Bytes.toBytes("serverstartcode"), 1L, Bytes.toBytes(1234L)),
      new org.apache.hadoop.hbase.KeyValue(Bytes.toBytes("r"), Bytes.toBytes("info"),
        Bytes.toBytes("regioninfo"), 1L, Bytes.toBytes("junk")) });
    when(scanner.iterator()).thenReturn(Collections.singletonList(result).iterator());
    when(table.getScanner(any(Scan.class))).thenReturn(scanner);

    List<ScanRow> rows = new java.util.ArrayList<>();
    new DefaultShellTable(table).scan(new HashMap<>()).forEachRow(rows::add);

    assertEquals("1234", rows.get(0).cells().get(0).value());
    assertEquals("", rows.get(0).cells().get(1).value());
  }
}
