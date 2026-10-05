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
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
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
    assertInstanceOf(PrefixFilter.class, captor.getValue().getFilter());
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
}
