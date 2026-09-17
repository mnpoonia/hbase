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
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.ShellTable;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTable;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class GetSplitsCommandTest {

  private static final class RecordingShellTable extends StubShellTable {
    @Override
    public List<String> getSplits() {
      return List.of("1000", "2000");
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

  private final GetSplitsCommand command = new GetSplitsCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final RecordingShellTableFactory tables = new RecordingShellTableFactory(table);
  private final ExecutionContext context =
    new ExecutionContext(new StubShellAdmin(), tables, new PrintWriter(new StringWriter()));

  @Test
  public void printsTotalSplitsAndPoints() throws Exception {
    var parsed = ShellLineParser.parse("get_splits 't1'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", tables.lastTableName);
    assertEquals(List.of("Total number of splits = 3", "1000", "2000"), result.lines());
  }

  @Test
  public void throwsWhenTableNameMissing() throws Exception {
    var parsed = ShellLineParser.parse("get_splits");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
