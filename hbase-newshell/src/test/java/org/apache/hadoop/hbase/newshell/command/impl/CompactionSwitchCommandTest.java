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
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class CompactionSwitchCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private Boolean lastEnabled;
    private List<String> lastServerNames;

    @Override
    public Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames) {
      this.lastEnabled = enabled;
      this.lastServerNames = serverNames;
      return Map.of("server1,60020,1000", true);
    }
  }

  private final CompactionSwitchCommand command = new CompactionSwitchCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void switchesCompactionOnSpecifiedServers() throws Exception {
    var parsed = ShellLineParser.parse("compaction_switch true, 'server1', 'server2'");
    TabularResult result = (TabularResult) command.execute(parsed, context);

    assertEquals(true, admin.lastEnabled);
    assertEquals(List.of("server1", "server2"), admin.lastServerNames);
    assertEquals(List.of("SERVER", "PREV_STATE"), result.header());
    assertEquals(List.of(List.of("server1,60020,1000", "true")), result.rows());
  }

  @Test
  public void switchesCompactionOnAllServersWhenNoneGiven() throws Exception {
    var parsed = ShellLineParser.parse("compaction_switch false");
    command.execute(parsed, context);

    assertEquals(false, admin.lastEnabled);
    assertEquals(List.of(), admin.lastServerNames);
  }

  @Test
  public void throwsWhenArgumentMissing() throws Exception {
    var parsed = ShellLineParser.parse("compaction_switch");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
