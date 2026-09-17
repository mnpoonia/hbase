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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTable;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class DeleteallCommandTest {

  private static final class RecordingShellTable extends StubShellTable {
    private String lastRow;
    private String lastColumn;
    private Long lastTimestamp;
    private Map<String, Object> lastOptions;

    @Override
    public void deleteAll(String row, String column, Long timestamp,
      Map<String, Object> options) {
      this.lastRow = row;
      this.lastColumn = column;
      this.lastTimestamp = timestamp;
      this.lastOptions = options;
    }
  }

  private static final class RecordingShellTableFactory implements ShellTableFactory {
    private final ShellTable table;
    private String lastTableName;

    RecordingShellTableFactory(ShellTable table) {
      this.table = table;
    }

    @Override
    public ShellTable forTable(String tableName) {
      this.lastTableName = tableName;
      return table;
    }
  }

  private final DeleteallCommand command = new DeleteallCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final RecordingShellTableFactory tables = new RecordingShellTableFactory(table);
  private final ExecutionContext context =
    new ExecutionContext(new StubShellAdmin(), tables, new PrintWriter(new StringWriter()));

  @Test
  public void deletesWholeRowWhenColumnOmitted() throws Exception {
    var parsed = ShellLineParser.parse("deleteall 't1', 'r1'");
    command.execute(parsed, context);

    assertEquals("t1", tables.lastTableName);
    assertEquals("r1", table.lastRow);
    assertNull(table.lastColumn);
  }

  @Test
  public void deletesAllVersionsOfAColumn() throws Exception {
    var parsed = ShellLineParser.parse("deleteall 't1', 'r1', 'f1:c1'");
    command.execute(parsed, context);

    assertEquals("f1:c1", table.lastColumn);
  }

  @Test
  public void deletesByRowPrefixFilterWithoutARowArgument() throws Exception {
    var parsed = ShellLineParser.parse("deleteall 't1', {ROWPREFIXFILTER => 'prefix'}, 'f1:c1'");
    command.execute(parsed, context);

    assertNull(table.lastRow);
    assertEquals("f1:c1", table.lastColumn);
    assertEquals("prefix", table.lastOptions.get("ROWPREFIXFILTER"));
  }

  @Test
  public void throwsWhenRowMissingAndNoPrefixFilter() throws Exception {
    var parsed = ShellLineParser.parse("deleteall 't1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
