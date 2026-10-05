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
import java.io.PrintWriter;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.format.OutputFormat;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code hbase/table.rb#_count_internal}: a table name
 * plus an optional {@code COLUMNS}/{@code STARTROW}/{@code STOPROW}/{@code VERSIONS}/
 * {@code FILTER}/{@code CACHE_BLOCKS}/{@code INTERVAL} hash literal, reusing {@link ScanCommand}'s
 * scan option support. The legacy "second positional arg is an Integer meaning INTERVAL" syntax
 * (e.g. {@code count 't1', 100000}) and table-reference chaining (e.g. {@code t.count}) are
 * explicitly not ported.
 */
@InterfaceAudience.Private
public final class CountCommand implements ShellCommand {
  @Override
  public String name() {
    return "count";
  }

  @Override
  public String help() {
    return "count 'table', {STARTROW => 'r1', FILTER => \"...\", CACHE_BLOCKS => true, "
      + "INTERVAL => 100000} - count the rows in a table, reporting progress every INTERVAL rows "
      + "(default 1000)";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "count", 1);
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("count requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    PrintWriter out = context.out();
    // Progress lines are human-oriented; keep them out of json/csv output so it stays parseable.
    boolean showProgress = context.options().outputFormat() == OutputFormat.TEXT;
    long count = context.tables().forTable(tableName).count(command.options(), (cnt, row) -> {
      if (showProgress) {
        out.println("Current count: " + cnt + ", row: " + row);
        out.flush();
      }
    });
    return TextResult.of(count + " row(s)");
  }
}
