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
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.BatchTableOp;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/disable_all.rb}: disables every table matching a
 * regex. Lists matches and requires confirmation: session {@code --yes} / line {@code --yes}, an
 * interactive {@code y/N} prompt, or else aborts with
 * {@link org.apache.hadoop.hbase.newshell.command.UserAbortException}.
 */
@InterfaceAudience.Private
public final class DisableAllCommand implements ShellCommand {
  @Override
  public String name() {
    return "disable_all";
  }

  @Override
  public String help() {
    return "disable_all 't.*' [--yes] - disable all tables matching a regex";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "disable_all", 1);
    return BatchTableOp.run(command, context, "disable_all", "disabled",
      (admin, table) -> admin.disableTable(table));
  }
}
