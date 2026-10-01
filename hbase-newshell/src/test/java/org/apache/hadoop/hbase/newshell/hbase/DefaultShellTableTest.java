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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class DefaultShellTableTest {

  @Test
  public void scanDoesNotOpenScannerUntilForEachRowIsCalled() throws IOException {
    Table table = mock(Table.class);
    DefaultShellTable shellTable = new DefaultShellTable(table);

    shellTable.scan(Collections.emptyMap());

    verify(table, never()).getScanner(any(Scan.class));
  }

  @Test
  public void scanClosesScannerExactlyOnceWhenRowConsumerThrows() throws IOException {
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
  public void scanClosesScannerExactlyOnceOnNormalCompletion() throws IOException {
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
}
