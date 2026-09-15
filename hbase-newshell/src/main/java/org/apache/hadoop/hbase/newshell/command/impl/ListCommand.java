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
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/list.rb} and {@code hbase/admin.rb#list}: an
 * optional regular-expression positional argument (defaulting to {@code .*}) filters the table
 * names returned as a one-column {@code TABLE} table.
 */
@InterfaceAudience.Private
public final class ListCommand implements ShellCommand {
  @Override
  public String name() {
    return "list";
  }

  @Override
  public String help() {
    return "list, list 'abc.*' - list all tables, optionally filtered by a regular expression";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    String regex = command.positionalArgs().isEmpty() ? ".*"
      : String.valueOf(command.positionalArgs().get(0));
    List<String> tableNames = context.admin().listTables(regex);
    List<List<String>> rows = new ArrayList<>();
    for (String tableName : tableNames) {
      rows.add(List.of(tableName));
    }
    return new TabularResult(List.of("TABLE"), rows);
  }
}
