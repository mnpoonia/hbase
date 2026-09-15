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
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.GetResult;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class PutCommandTest {

  /** Captures the put it was called with instead of talking to a real Table. */
  private static final class RecordingShellTable implements ShellTable {
    private String lastRow;
    private String lastColumn;
    private String lastValue;
    private Map<String, Object> lastOptions;

    @Override
    public GetResult get(String row, Map<String, Object> options) {
      throw new UnsupportedOperationException("not needed for this test");
    }

    @Override
    public void put(String row, String column, String value, Map<String, Object> options) {
      this.lastRow = row;
      this.lastColumn = column;
      this.lastValue = value;
      this.lastOptions = options;
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

  private final PutCommand command = new PutCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final RecordingShellTableFactory tables = new RecordingShellTableFactory(table);
  private final ExecutionContext context =
    new ExecutionContext(new StubShellAdmin(), tables, new PrintWriter(new StringWriter()));

  @Test
  public void putsSingleCell() throws Exception {
    var parsed = ShellLineParser.parse("put 't1', 'r1', 'f1:c1', 'v1'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", tables.lastTableName);
    assertEquals("r1", table.lastRow);
    assertEquals("f1:c1", table.lastColumn);
    assertEquals("v1", table.lastValue);
    assertEquals(List.of("1 row(s) put"), result.lines());
  }

  @Test
  public void putsSingleCellWithTimestamp() throws Exception {
    var parsed = ShellLineParser.parse("put 't1', 'r1', 'f1:c1', 'v1', {TIMESTAMP => 123}");
    command.execute(parsed, context);

    assertEquals(123L, table.lastOptions.get("TIMESTAMP"));
  }

  @Test
  public void throwsWhenValueMissing() throws Exception {
    var parsed = ShellLineParser.parse("put 't1', 'r1', 'f1:c1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
