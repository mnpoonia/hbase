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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.ListRegionsView;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/list_regions.rb}: table name
 * only, always reporting all seven columns. Output uses the Ruby command's pipe-aligned
 * {@code printf} layout (not the shared shell formatter), including the trailing {@code N rows}
 * footer.
 */
@InterfaceAudience.Private
public final class ListRegionsCommand implements ShellCommand {
  private static final List<String> COLUMNS =
    Arrays.asList("SERVER_NAME", "REGION_NAME", "START_KEY", "END_KEY", "SIZE", "REQ", "LOCALITY");
  private static final int[] DEFAULT_WIDTHS = { 12, 12, 10, 10, 5, 5, 10 };

  @Override
  public String name() {
    return "list_regions";
  }

  @Override
  public String help() {
    return "list_regions 'table' - list all regions for a table";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "list_regions", 1);
    String tableName =
      ArgParsing.requireArg(command, 0, "list_regions requires a table name argument");
    ListRegionsView view = context.tableAdmin().listRegions(tableName);
    return new TextResult(format(view));
  }

  /** Mirrors {@code list_regions.rb}'s size_hash + printf layout. */
  static List<String> format(ListRegionsView view) {
    Map<String, Integer> widths = new LinkedHashMap<>();
    for (int i = 0; i < COLUMNS.size(); i++) {
      widths.put(COLUMNS.get(i), DEFAULT_WIDTHS[i]);
    }
    for (List<String> row : view.rows()) {
      for (int i = 0; i < COLUMNS.size(); i++) {
        String value = i < row.size() && row.get(i) != null ? row.get(i) : "";
        widths.put(COLUMNS.get(i), Math.max(widths.get(COLUMNS.get(i)), value.length()));
      }
    }

    List<String> lines = new ArrayList<>(view.warnings());
    lines.add(formatPipeRow(
      COLUMNS.stream().map(col -> pad(col, widths.get(col))).collect(Collectors.toList())));
    lines.add(formatPipeRow(
      COLUMNS.stream().map(col -> String.join("", Collections.nCopies(widths.get(col), "-")))
        .collect(Collectors.toList())));
    for (List<String> row : view.rows()) {
      List<String> cells = new ArrayList<>(COLUMNS.size());
      for (int i = 0; i < COLUMNS.size(); i++) {
        String value = i < row.size() && row.get(i) != null ? row.get(i) : "";
        cells.add(pad(value, widths.get(COLUMNS.get(i))));
      }
      lines.add(formatPipeRow(cells));
    }
    lines.add(" " + view.rows().size() + " rows");
    return lines;
  }

  private static String formatPipeRow(List<String> cells) {
    StringBuilder sb = new StringBuilder();
    for (String cell : cells) {
      sb.append(' ').append(cell).append(" |");
    }
    return sb.toString();
  }

  /** Right-pad like Ruby {@code printf(" %#{length}s |", value)}. */
  private static String pad(String value, int width) {
    if (value.length() >= width) {
      return value;
    }
    return String.join("", Collections.nCopies(width - value.length(), " ")) + value;
  }
}
