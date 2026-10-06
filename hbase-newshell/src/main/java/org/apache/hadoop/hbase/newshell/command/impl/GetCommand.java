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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.hbase.CellView;
import org.apache.hadoop.hbase.newshell.hbase.CellViews;
import org.apache.hadoop.hbase.newshell.hbase.GetResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code hbase/table.rb#_get_internal}: table name, row
 * key, and either an optional {@code COLUMN}/{@code COLUMNS}/{@code VERSIONS}/{@code TIMESTAMP}/
 * {@code FILTER}/{@code TIMERANGE}/{@code MAXLENGTH}/{@code FORMATTER}/{@code FORMATTER_CLASS}/{@code ATTRIBUTES}/
 * {@code AUTHORIZATIONS}/{@code CONSISTENCY}/{@code REGION_REPLICA_ID} hash literal or bare column
 * arguments ({@code get 't', 'r', 'c1',
 * 'c2'} / {@code get 't', 'r', ['c1', 'c2']}), as in the legacy shell. ATTRIBUTES, AUTHORIZATIONS
 * and CONSISTENCY are explicitly not ported for this pilot slice.
 */
@InterfaceAudience.Private
public final class GetCommand implements ShellCommand {
  private static final List<String> HEADER = Arrays.asList("COLUMN", "CELL");

  @Override
  public String name() {
    return "get";
  }

  @Override
  public String help() {
    return "get 'table', 'row' [, 'cf:col', ... | {COLUMN => ..., VERSIONS, TIMESTAMP, TIMERANGE, FILTER, "
      + "MAXLENGTH, FORMATTER, FORMATTER_CLASS, ATTRIBUTES, AUTHORIZATIONS, CONSISTENCY, "
      + "REGION_REPLICA_ID}] - fetch a row";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireArgs(command, 2, "get requires a table name and a row key argument");
    String tableName = ArgParsing.string(command, 0);
    String row = ArgParsing.string(command, 1);
    Map<String, Object> options = withColumns(command);
    int maxLength = ArgParsing.maxLength(options.remove("MAXLENGTH"));
    GetResult result = context.tables().forTable(tableName).get(row, options);
    List<List<String>> rows = new ArrayList<>();
    for (CellView cell : result.cells()) {
      rows.add(Arrays.asList(CellViews.column(cell), CellViews.formatCell(cell, maxLength)));
    }
    return new TabularResult(HEADER, rows);
  }

  /**
   * Folds bare column arguments and the {@code COLUMNS} alias into the {@code COLUMN} option.
   * Mixing bare columns with a hash is rejected rather than silently dropping one of them.
   */
  private static Map<String, Object> withColumns(ParsedCommand command)
    throws ShellCommandException {
    Map<String, Object> options = new LinkedHashMap<>(command.options());
    if (options.containsKey("COLUMNS")) {
      if (options.containsKey("COLUMN")) {
        throw new ShellCommandException("get accepts only one of COLUMN or COLUMNS");
      }
      options.put("COLUMN", options.remove("COLUMNS"));
    }
    List<Object> bare = new ArrayList<>();
    for (Object arg : command.positionalArgs().subList(2, command.positionalArgs().size())) {
      if (arg instanceof List) {
        bare.addAll((List<?>) arg);
      } else {
        bare.add(arg);
      }
    }
    if (!bare.isEmpty()) {
      if (!options.isEmpty()) {
        throw new ShellCommandException(
          "get: pass columns either as bare arguments or in a COLUMN option, not both");
      }
      options.put("COLUMN", bare);
    }
    return options;
  }
}
