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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.SessionOptions;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.command.UserAbortException;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class DisableAllCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private List<String> tableNames = Collections.emptyList();
    private final List<String> disabled = new ArrayList<>();
    private String failingTable;

    @Override
    public List<String> listTables(String regex) {
      return tableNames;
    }

    @Override
    public void disableTable(String tableName) throws java.io.IOException {
      if (tableName.equals(failingTable)) {
        throw new java.io.IOException("boom on " + tableName);
      }
      disabled.add(tableName);
    }
  }

  private final DisableAllCommand command = new DisableAllCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context = new ExecutionContext(admin, new StubShellTableFactory(),
    new PrintWriter(new StringWriter()), SessionOptions.defaults().withForceYes(true));

  @Test
  public void disablesEveryMatch() throws Exception {
    admin.tableNames = Arrays.asList("t1", "t2");
    ParsedCommand parsed = ShellLineParser.parse("disable_all 't.*'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(Arrays.asList("t1", "t2"), admin.disabled);
    assertEquals(Arrays.asList("2 tables successfully disabled"), result.lines());
  }

  @Test
  public void reportsNoMatches() throws Exception {
    admin.tableNames = Collections.emptyList();
    ParsedCommand parsed = ShellLineParser.parse("disable_all 'nope.*'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(Arrays.asList("No tables matched the regex nope.*"), result.lines());
  }

  @Test
  public void abortsWithoutYesWhenNonInteractive() throws Exception {
    admin.tableNames = Arrays.asList("t1");
    ExecutionContext noYes =
      new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));
    ParsedCommand parsed = ShellLineParser.parse("disable_all 't.*'");
    assertThrows(UserAbortException.class, () -> command.execute(parsed, noYes));
    assertEquals(Collections.emptyList(), admin.disabled);
  }

  @Test
  public void acceptsInlineYesFlag() throws Exception {
    admin.tableNames = Arrays.asList("t1");
    ExecutionContext noYes =
      new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));
    ParsedCommand parsed = ShellLineParser.parse("disable_all 't.*' --YES");
    TextResult result = (TextResult) command.execute(parsed, noYes);
    assertEquals(Arrays.asList("t1"), admin.disabled);
    assertEquals(Arrays.asList("1 tables successfully disabled"), result.lines());
  }

  @Test
  public void partialFailureIsReportedAsErrorWithCause() throws Exception {
    admin.tableNames = Arrays.asList("t1", "t2", "t3");
    admin.failingTable = "t2";
    ParsedCommand parsed = ShellLineParser.parse("disable_all 't.*'");
    ShellCommandException e =
      assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
    assertEquals(Arrays.asList("t1", "t3"), admin.disabled);
    assertTrue(e.getMessage().contains("2 tables successfully disabled"), e.getMessage());
    assertTrue(e.getMessage().contains("t2 (boom on t2)"), e.getMessage());
    assertEquals("boom on t2", e.getCause().getMessage());
  }
}
