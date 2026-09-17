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
 * Ported from hbase-shell's {@code hbase/table.rb#_deleteall_internal}: table name, row key
 * (unless {@code ROWPREFIXFILTER} is given, in which case the parser has already lifted it into
 * the options map and the row positional argument is absent), an optional column (whole row if
 * omitted), and an optional timestamp - deletes all versions up to that timestamp, matching the
 * ruby original's {@code all_version=true}. {@code ROWPREFIXFILTER} plus optional {@code CACHE}
 * (default 100) batches a prefix-scan range delete, per {@code _deleterows_internal}. Meta-table
 * guards, ATTRIBUTES and VISIBILITY are explicitly not ported.
 */
@InterfaceAudience.Private
public final class DeleteallCommand implements ShellCommand {
  @Override
  public String name() {
    return "deleteall";
  }

  @Override
  public String help() {
    return "deleteall 'table', 'row' - delete all versions of a row, or of one column";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("deleteall requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    boolean prefixMode = command.options().containsKey("ROWPREFIXFILTER");
    int nextIndex = 1;
    String row = null;
    if (!prefixMode) {
      if (command.positionalArgs().size() < 2) {
        throw new ShellCommandException("deleteall requires a row key argument");
      }
      row = String.valueOf(command.positionalArgs().get(nextIndex++));
    }
    String column = command.positionalArgs().size() > nextIndex
      ? String.valueOf(command.positionalArgs().get(nextIndex++))
      : null;
    Long timestamp = command.positionalArgs().size() > nextIndex
      ? ((Number) command.positionalArgs().get(nextIndex)).longValue()
      : null;
    context.tables().forTable(tableName).deleteAll(row, column, timestamp, command.options());
    return TextResult.of();
  }
}
