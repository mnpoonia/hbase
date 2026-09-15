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
 * Ported, minimal slice, from hbase-shell's {@code hbase/table.rb#_put_internal}: table name,
 * row key, a single required {@code family:qualifier} column, and value, plus an optional
 * {@code TIMESTAMP} hash literal. ATTRIBUTES, VISIBILITY and TTL are explicitly not ported for
 * this pilot slice.
 */
@InterfaceAudience.Private
public final class PutCommand implements ShellCommand {
  @Override
  public String name() {
    return "put";
  }

  @Override
  public String help() {
    return "put 'table', 'row', 'family:qualifier', 'value' - write a single cell";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().size() < 4) {
      throw new ShellCommandException(
        "put requires a table name, row key, column and value argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    String row = String.valueOf(command.positionalArgs().get(1));
    String column = String.valueOf(command.positionalArgs().get(2));
    String value = String.valueOf(command.positionalArgs().get(3));
    context.tables().forTable(tableName).put(row, column, value, command.options());
    return TextResult.of("1 row(s) put");
  }
}
