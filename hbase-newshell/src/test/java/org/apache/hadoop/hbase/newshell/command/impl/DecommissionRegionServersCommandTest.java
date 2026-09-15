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
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class DecommissionRegionServersCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private List<String> lastHostOrServers;
    private boolean lastOffload;

    @Override
    public void decommissionRegionServers(List<String> hostOrServers, boolean offload) {
      this.lastHostOrServers = hostOrServers;
      this.lastOffload = offload;
    }
  }

  private final DecommissionRegionServersCommand command = new DecommissionRegionServersCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void decommissionsSingleServerWithDefaultOffload() throws Exception {
    var parsed = ShellLineParser.parse("decommission_regionservers 'host1,60020,123'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(List.of("host1,60020,123"), admin.lastHostOrServers);
    assertEquals(false, admin.lastOffload);
    assertEquals(List.of(), result.lines());
  }

  @Test
  public void decommissionsMultipleServersWithOffload() throws Exception {
    var parsed = ShellLineParser.parse("decommission_regionservers ['host1', 'host2'], true");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(List.of("host1", "host2"), admin.lastHostOrServers);
    assertEquals(true, admin.lastOffload);
    assertEquals(List.of(), result.lines());
  }

  @Test
  public void throwsWhenServerNameMissing() throws Exception {
    var parsed = ShellLineParser.parse("decommission_regionservers");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
