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
 * Ported from hbase-shell's {@code shell/commands/clone_table_schema.rb}: creates a new table by
 * copying an existing table's descriptor and (by default) its split keys, with no data copy.
 */
@InterfaceAudience.Private
public final class CloneTableSchemaCommand implements ShellCommand {
  @Override
  public String name() {
    return "clone_table_schema";
  }

  @Override
  public String help() {
    return "clone_table_schema 'table', 'new_table', [preserve_splits] - create a new table by "
      + "cloning an existing table's schema";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().size() < 2) {
      throw new ShellCommandException(
        "clone_table_schema requires a table name and a new table name");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    String newTableName = String.valueOf(command.positionalArgs().get(1));
    boolean preserveSplits = command.positionalArgs().size() < 3
      || !"false".equalsIgnoreCase(String.valueOf(command.positionalArgs().get(2)));
    context.admin().cloneTableSchema(tableName, newTableName, preserveSplits);
    return TextResult.of();
  }
}
