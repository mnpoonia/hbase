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
import java.util.Arrays;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.hbase.CellView;
import org.apache.hadoop.hbase.newshell.hbase.GetResult;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTable;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class GetCommandTest {

  private static final class RecordingShellTable extends StubShellTable {
    private Map<String, Object> lastOptions;
    private String lastRow;

    @Override
    public GetResult get(String row, Map<String, Object> options) {
      this.lastRow = row;
      this.lastOptions = options;
      return new GetResult(Arrays.asList(new CellView("f1", "c1", 123L, "v1")));
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

  private final GetCommand command = new GetCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final RecordingShellTableFactory tables = new RecordingShellTableFactory(table);
  private final ExecutionContext context =
    new ExecutionContext(new StubShellAdmin(), tables, new PrintWriter(new StringWriter()));

  @Test
  public void getsRow() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("get 't1', 'r1', {VERSIONS => 2}");
    TabularResult result = (TabularResult) command.execute(parsed, context);

    assertEquals("t1", tables.lastTableName);
    assertEquals("r1", table.lastRow);
    assertEquals(2L, table.lastOptions.get("VERSIONS"));
    assertEquals(Arrays.asList("COLUMN", "CELL"), result.header());
    assertEquals(1, result.rows().size());
  }

  @Test
  public void passesFilterAndTimerangeOptionsThrough() throws Exception {
    ParsedCommand parsed = ShellLineParser
      .parse("get 't1', 'r1', {FILTER => \"ValueFilter(=, 'binary:abc')\", TIMERANGE => [100, 200]}");
    command.execute(parsed, context);

    assertEquals("ValueFilter(=, 'binary:abc')", table.lastOptions.get("FILTER"));
    assertEquals(Arrays.asList(100L, 200L), table.lastOptions.get("TIMERANGE"));
  }

  @Test
  public void throwsWhenRowKeyMissing() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("get 't1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
