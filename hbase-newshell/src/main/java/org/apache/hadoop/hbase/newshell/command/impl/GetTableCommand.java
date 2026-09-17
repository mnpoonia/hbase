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
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/get_table.rb}: in the Ruby REPL this binds a
 * table object to a variable for later method calls (e.g. {@code t1 = get_table 't1'}) and prints
 * nothing itself. newshell has no persistent variable/table-object binding, so this port only
 * validates the table exists (surfacing the same error a later {@code t1.help}-style use would
 * hit) and, like the original, produces no output of its own.
 */
@InterfaceAudience.Private
public final class GetTableCommand implements ShellCommand {
  @Override
  public String name() {
    return "get_table";
  }

  @Override
  public String help() {
    return "get_table 'table' - validate that a table exists";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("get_table requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    if (!context.admin().tableExists(tableName)) {
      throw new ShellCommandException("Table '" + tableName + "' does not exist");
    }
    return TextResult.of();
  }
}
