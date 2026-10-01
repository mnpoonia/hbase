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
import java.util.Arrays;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.*;
import org.apache.hadoop.hbase.newshell.hbase.BalanceResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;
@InterfaceAudience.Private
public final class BalanceRsgroupCommand implements ShellCommand {
  @Override public String name() { return "balance_rsgroup"; }
  @Override public String help() { return "balance_rsgroup 'group' [, 'dry_run'] [, 'ignore_rit']"; }
  @Override public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) throw new ShellCommandException("balance_rsgroup requires a group name");
    String group = String.valueOf(command.positionalArgs().get(0));
    boolean dryRun=false, ignoreRit=false;
    for (int i=1;i<command.positionalArgs().size();i++) {
      String v=String.valueOf(command.positionalArgs().get(i));
      if (v.equalsIgnoreCase("dry_run")) dryRun=true;
      else if (v.equalsIgnoreCase("ignore_rit")) ignoreRit=true;
      else throw new ShellCommandException("balance_rsgroup accepts only 'dry_run' and/or 'ignore_rit'");
    }
    BalanceResult response = context.admin().balanceRsGroup(group, dryRun, ignoreRit);
    if (response.ran()) {
      return new TextResult(Arrays.asList("Balancer ran",
        "Moves calculated: "+response.movesCalculated()+", moves executed: "+response.movesExecuted()));
    }
    return TextResult.of("Balancer did not run. See logs for details.");
  }
}
