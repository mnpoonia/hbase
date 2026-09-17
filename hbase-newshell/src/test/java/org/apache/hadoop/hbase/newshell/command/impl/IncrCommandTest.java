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
public class IncrCommandTest {

  private static final class RecordingShellTable extends StubShellTable {
    private long lastAmount;

    @Override
    public Long increment(String row, String column, long amount) {
      this.lastAmount = amount;
      return amount;
    }
  }

  private static final class RecordingShellTableFactory implements ShellTableFactory {
    private final ShellTable table;

    RecordingShellTableFactory(ShellTable table) {
      this.table = table;
    }

    @Override
    public ShellTable forTable(String tableName) {
      return table;
    }
  }

  private final IncrCommand command = new IncrCommand();
  private final RecordingShellTable table = new RecordingShellTable();
  private final ExecutionContext context = new ExecutionContext(new StubShellAdmin(),
    new RecordingShellTableFactory(table), new PrintWriter(new StringWriter()));

  @Test
  public void defaultsToIncrementingByOne() throws Exception {
    var parsed = ShellLineParser.parse("incr 't1', 'r1', 'f1:c1'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(1L, table.lastAmount);
    assertEquals(List.of("COUNTER VALUE = 1"), result.lines());
  }

  @Test
  public void incrementsByExplicitAmount() throws Exception {
    var parsed = ShellLineParser.parse("incr 't1', 'r1', 'f1:c1', 5");
    command.execute(parsed, context);

    assertEquals(5L, table.lastAmount);
  }

  @Test
  public void throwsWhenColumnMissing() throws Exception {
    var parsed = ShellLineParser.parse("incr 't1', 'r1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
