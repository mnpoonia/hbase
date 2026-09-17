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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class EnableAllCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private List<String> tableNames = List.of();
    private final List<String> enabled = new ArrayList<>();

    @Override
    public List<String> listTables(String regex) {
      return tableNames;
    }

    @Override
    public void enableTable(String tableName) {
      enabled.add(tableName);
    }
  }

  private final EnableAllCommand command = new EnableAllCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void enablesEveryMatch() throws Exception {
    admin.tableNames = List.of("t1", "t2");
    var parsed = ShellLineParser.parse("enable_all 't.*'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(List.of("t1", "t2"), admin.enabled);
    assertEquals(List.of("2 tables successfully enabled"), result.lines());
  }
}
