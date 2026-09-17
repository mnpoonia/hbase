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
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.AlterStatusView;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/alter_status.rb}/{@code admin.rb#alter_status}:
 * reports how many of a table's regions have picked up the most recent schema change. The Ruby
 * original polls every second until all regions catch up; this port reports a single snapshot,
 * since a one-shot/piped invocation has no interactive loop to poll from.
 */
@InterfaceAudience.Private
public final class AlterStatusCommand implements ShellCommand {
  @Override
  public String name() {
    return "alter_status";
  }

  @Override
  public String help() {
    return "alter_status 'table' - report how many regions have the updated schema";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("alter_status requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    AlterStatusView status = context.admin().alterStatus(tableName);
    if (status.totalRegions() == 0) {
      return TextResult.of("All regions updated.");
    }
    int updated = status.totalRegions() - status.regionsYetToUpdate();
    return TextResult.of(updated + "/" + status.totalRegions() + " regions updated.");
  }
}
