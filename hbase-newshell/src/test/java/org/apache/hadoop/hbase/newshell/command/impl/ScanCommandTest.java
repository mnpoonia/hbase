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
package org.apache.hadoop.hbase.newshell.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.hbase.CellView;
import org.apache.hadoop.hbase.newshell.hbase.ScanResult;
import org.apache.hadoop.hbase.newshell.hbase.ScanRow;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTable;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class ScanCommandTest {

  private static final class RecordingShellTable extends StubShellTable {
    private Map<String, Object> lastOptions;

    @Override
    public ScanResult scan(Map<String, Object> options) {
      this.lastOptions = options;
      return new ScanResult(
        List.of(new ScanRow("r1", List.of(new CellView("f1", "c1", 123L, "v1")))));
    }
  }

  private static final class RecordingShellTableFactory implements ShellTableFactory {
    private final RecordingShellTable table;
    private String lastTableName;

    RecordingShellTableFactory(RecordingShellTable table) {
      this.table = table;
    }

    @Override
    public ShellTable forTable(String tableName) {
      this.lastTableName = tableName;
      return table;
    }
  }

  private final ScanCommand command = new ScanCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final RecordingShellTableFactory tables = new RecordingShellTableFactory(table);
  private final ExecutionContext context =
    new ExecutionContext(new StubShellAdmin(), tables, new PrintWriter(new StringWriter()));

  @Test
  public void scansTable() throws Exception {
    var parsed = ShellLineParser.parse("scan 't1', {LIMIT => 10}");
    TabularResult result = (TabularResult) command.execute(parsed, context);

    assertEquals("t1", tables.lastTableName);
    assertEquals(10L, table.lastOptions.get("LIMIT"));
    assertEquals(List.of("ROW", "COLUMN+CELL"), result.header());
    assertEquals(1, result.rows().size());
    assertEquals("r1", result.rows().get(0).get(0));
  }

  @Test
  public void throwsWhenTableNameMissing() throws Exception {
    var parsed = ShellLineParser.parse("scan");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
