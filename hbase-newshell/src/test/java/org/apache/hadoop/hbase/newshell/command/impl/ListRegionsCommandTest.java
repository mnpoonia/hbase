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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.ListRegionsView;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class ListRegionsCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastTableName;
    private ListRegionsView view =
      new ListRegionsView(Collections.emptyList(), Collections.emptyList());

    @Override
    public ListRegionsView listRegions(String tableName) {
      this.lastTableName = tableName;
      return view;
    }
  }

  private final ListRegionsCommand command = new ListRegionsCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void reportsPipeAlignedRegionRows() throws Exception {
    admin.view = new ListRegionsView(Collections.emptyList(),
      Arrays.asList(Arrays.asList("host1:1234", "t1,,123.abc.", "", "", "0", "0", "1.0")));
    ParsedCommand parsed = ShellLineParser.parse("list_regions 't1'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    List<String> lines = result.lines();
    assertTrue(lines.get(0).contains("SERVER_NAME"));
    assertTrue(lines.get(0).contains("|"));
    assertTrue(lines.get(1).contains("---"));
    assertTrue(lines.get(2).contains("host1:1234"));
    assertTrue(lines.get(2).contains("t1,,123.abc."));
    assertEquals(" 1 rows", lines.get(3));
  }

  @Test
  public void prependsMissingMetricsWarnings() throws Exception {
    admin.view = new ListRegionsView(
      Arrays.asList(
        "Can not find all details for region: t1,,123.abc. , it may be disabled or in transition"),
      Arrays.asList(Arrays.asList("host1:1234", "t1,,123.abc.", "", "", "", "", "")));
    ParsedCommand parsed = ShellLineParser.parse("list_regions 't1'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertTrue(result.lines().get(0).startsWith("Can not find all details"));
    assertEquals(" 1 rows", result.lines().get(result.lines().size() - 1));
  }
}
