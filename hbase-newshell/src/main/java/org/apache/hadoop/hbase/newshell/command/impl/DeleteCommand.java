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
 * Ported from hbase-shell's {@code hbase/table.rb#_delete_internal}: table name, row key, a
 * required {@code family[:qualifier]} column, and an optional timestamp - deletes a single
 * version of that column, matching the ruby original's {@code all_version=false}. ATTRIBUTES and
 * VISIBILITY are explicitly not ported.
 */
@InterfaceAudience.Private
public final class DeleteCommand implements ShellCommand {
  @Override
  public String name() {
    return "delete";
  }

  @Override
  public String help() {
    return "delete 'table', 'row', 'family:qualifier' - delete a single cell version";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().size() < 3) {
      throw new ShellCommandException("delete requires a table name, row key and column argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    String row = String.valueOf(command.positionalArgs().get(1));
    String column = String.valueOf(command.positionalArgs().get(2));
    Long timestamp = command.positionalArgs().size() > 3
      ? ((Number) command.positionalArgs().get(3)).longValue()
      : null;
    context.tables().forTable(tableName).delete(row, column, timestamp);
    return TextResult.of();
  }
}
