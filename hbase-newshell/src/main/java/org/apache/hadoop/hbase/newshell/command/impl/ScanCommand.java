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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.hbase.CellView;
import org.apache.hadoop.hbase.newshell.hbase.ScanResult;
import org.apache.hadoop.hbase.newshell.hbase.ScanRow;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/scan.rb}: a table name plus an
 * optional {@code COLUMNS}/{@code LIMIT}/{@code STARTROW}/{@code STOPROW}/{@code VERSIONS} hash
 * literal. TIMERANGE, FILTER, ROWPREFIXFILTER, formatters, metrics, and the other scanner options
 * are explicitly not ported for this pilot slice.
 */
@InterfaceAudience.Private
public final class ScanCommand implements ShellCommand {
  private static final List<String> HEADER = List.of("ROW", "COLUMN+CELL");

  @Override
  public String name() {
    return "scan";
  }

  @Override
  public String help() {
    return "scan 'table', {COLUMNS => ['cf1', 'cf2'], LIMIT => 10} - scan a table";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("scan requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    Map<String, Object> options = command.options();
    ScanResult result = context.tables().forTable(tableName).scan(options);
    List<List<String>> rows = new ArrayList<>();
    for (ScanRow row : result.rows()) {
      for (CellView cell : row.cells()) {
        String column = cell.family() + ":" + cell.qualifier();
        String timestamp =
          LocalDateTime.ofInstant(Instant.ofEpochMilli(cell.timestamp()), ZoneId.systemDefault())
            .toString();
        String cellText = "timestamp=" + timestamp + ", value=" + cell.value();
        rows.add(List.of(row.row(), column + " " + cellText));
      }
    }
    return new TabularResult(HEADER, rows);
  }
}
