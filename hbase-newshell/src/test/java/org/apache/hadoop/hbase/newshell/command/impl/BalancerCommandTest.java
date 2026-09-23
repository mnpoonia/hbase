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
import org.apache.hadoop.hbase.client.BalanceResponse;
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
public class BalancerCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private Boolean lastDryRun;
    private Boolean lastIgnoreRit;
    private BalanceResponse response = BalanceResponse.newBuilder().setBalancerRan(true)
      .setMovesCalculated(3).setMovesExecuted(2).build();

    @Override
    public BalanceResponse balance(boolean dryRun, boolean ignoreRegionsInTransition) {
      this.lastDryRun = dryRun;
      this.lastIgnoreRit = ignoreRegionsInTransition;
      return response;
    }
  }

  private final BalancerCommand command = new BalancerCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void runsBalancerWithNoArguments() throws Exception {
    var parsed = ShellLineParser.parse("balancer");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(false, admin.lastDryRun);
    assertEquals(false, admin.lastIgnoreRit);
    assertEquals(List.of("Balancer ran", "Moves calculated: 3, moves executed: 2"),
      result.lines());
  }

  @Test
  public void parsesDryRunAndIgnoreRitArguments() throws Exception {
    var parsed = ShellLineParser.parse("balancer 'dry_run', 'ignore_rit'");
    command.execute(parsed, context);

    assertEquals(true, admin.lastDryRun);
    assertEquals(true, admin.lastIgnoreRit);
  }

  @Test
  public void reportsWhenBalancerDidNotRun() throws Exception {
    admin.response = BalanceResponse.newBuilder().setBalancerRan(false).build();
    var parsed = ShellLineParser.parse("balancer");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals(List.of("Balancer did not run. See logs for details."), result.lines());
  }

  @Test
  public void throwsOnUnknownArgument() throws Exception {
    var parsed = ShellLineParser.parse("balancer 'bogus'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
