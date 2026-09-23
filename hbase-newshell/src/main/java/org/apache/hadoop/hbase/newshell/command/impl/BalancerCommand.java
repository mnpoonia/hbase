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

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hbase.client.BalanceResponse;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/balancer.rb}: triggers one balancer run,
 * accepting an optional {@code dry_run} and/or {@code ignore_rit} argument, in either order.
 */
@InterfaceAudience.Private
public final class BalancerCommand implements ShellCommand {
  @Override
  public String name() {
    return "balancer";
  }

  @Override
  public String help() {
    return "balancer ['dry_run'][, 'ignore_rit'] - trigger the balancer, returns true if it ran";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    boolean dryRun = false;
    boolean ignoreRit = false;
    for (Object arg : command.positionalArgs()) {
      String value = String.valueOf(arg);
      if (value.equalsIgnoreCase("dry_run")) {
        dryRun = true;
      } else if (value.equalsIgnoreCase("ignore_rit")) {
        ignoreRit = true;
      } else {
        throw new ShellCommandException("balancer accepts only 'dry_run' and/or 'ignore_rit'");
      }
    }
    BalanceResponse response = context.admin().balance(dryRun, ignoreRit);
    if (response.isBalancerRan()) {
      return new TextResult(List.of("Balancer ran", "Moves calculated: "
        + response.getMovesCalculated() + ", moves executed: " + response.getMovesExecuted()));
    }
    return TextResult.of("Balancer did not run. See logs for details.");
  }
}
