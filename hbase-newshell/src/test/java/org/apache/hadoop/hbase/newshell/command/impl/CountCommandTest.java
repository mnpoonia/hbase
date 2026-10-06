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
import org.apache.hadoop.hbase.newshell.command.SessionOptions;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.format.OutputFormat;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable.CountProgressListener;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTable;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class CountCommandTest {

  private static final class RecordingShellTable extends StubShellTable {
    private Map<String, Object> lastOptions;
    private CountProgressListener lastProgressListener;

    @Override
    public long count(Map<String, Object> options, CountProgressListener progressListener) {
      this.lastOptions = options;
      this.lastProgressListener = progressListener;
      return 42L;
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

  private final CountCommand command = new CountCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final RecordingShellTableFactory tables = new RecordingShellTableFactory(table);
  private final StringWriter outBuffer = new StringWriter();
  private final ExecutionContext context =
    new ExecutionContext(new StubShellAdmin(), tables, new PrintWriter(outBuffer));

  @Test
  public void printsRowCount() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("count 't1', {STARTROW => 'r1'}");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", tables.lastTableName);
    assertEquals("r1", table.lastOptions.get("STARTROW"));
    assertEquals(Arrays.asList("42 row(s)"), result.lines());
  }

  @Test
  public void legacyPositionalIntegerIsTheInterval() throws Exception {
    command.execute(ShellLineParser.parse("count 't1', 500"), context);
    assertEquals(500L, ((Number) table.lastOptions.get("INTERVAL")).longValue());
  }

  @Test
  public void nonNumericSecondArgumentIsRejected() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("count 't1', 'x'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }

  @Test
  public void throwsWhenTableNameMissing() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("count");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }

  @Test
  public void progressListenerPrintsCurrentCountLine() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("count 't1'");
    command.execute(parsed, context);

    table.lastProgressListener.onProgress(2L, "r2");

    assertEquals("Current count: 2, row: r2" + System.lineSeparator(), outBuffer.toString());
  }

  @Test
  public void progressLinesAreSuppressedInJsonMode() throws Exception {
    StringWriter jsonOut = new StringWriter();
    ExecutionContext jsonContext = new ExecutionContext(new StubShellAdmin(), tables,
      new PrintWriter(jsonOut), new SessionOptions(OutputFormat.JSON, false, false, false, false));
    command.execute(ShellLineParser.parse("count 't1'"), jsonContext);

    table.lastProgressListener.onProgress(2L, "r2");

    assertEquals("", jsonOut.toString());
  }
}
