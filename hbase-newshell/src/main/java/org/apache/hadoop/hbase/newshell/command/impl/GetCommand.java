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
import org.apache.hadoop.hbase.newshell.hbase.CellView;
import org.apache.hadoop.hbase.newshell.hbase.GetResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code hbase/table.rb#_get_internal}: table name,
 * row key, and an optional {@code COLUMN}/{@code VERSIONS}/{@code TIMESTAMP} hash literal.
 * FILTER, ATTRIBUTES, AUTHORIZATIONS, CONSISTENCY and TIMERANGE are explicitly not ported for
 * this pilot slice.
 */
@InterfaceAudience.Private
public final class GetCommand implements ShellCommand {
  private static final List<String> HEADER = List.of("COLUMN", "CELL");

  @Override
  public String name() {
    return "get";
  }

  @Override
  public String help() {
    return "get 'table', 'row', {COLUMN => 'cf:col'} - fetch a row";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().size() < 2) {
      throw new ShellCommandException("get requires a table name and a row key argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    String row = String.valueOf(command.positionalArgs().get(1));
    GetResult result = context.tables().forTable(tableName).get(row, command.options());
    List<List<String>> rows = new ArrayList<>();
    for (CellView cell : result.cells()) {
      String column = cell.family() + ":" + cell.qualifier();
      String cellText = "timestamp=" + cell.timestamp() + ", value=" + cell.value();
      rows.add(List.of(column, cellText));
    }
    return new TabularResult(HEADER, rows);
  }
}
