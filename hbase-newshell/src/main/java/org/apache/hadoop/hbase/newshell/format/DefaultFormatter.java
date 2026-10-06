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
package org.apache.hadoop.hbase.newshell.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ResultVisitor;
import org.apache.hadoop.hbase.newshell.command.StreamingTabularResult;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Plain-text renderer for the {@link CommandResult} shapes, dispatched through a
 * {@link ResultVisitor}.
 */
@InterfaceAudience.Private
public final class DefaultFormatter implements Formatter {
  @Override
  public void format(String commandName, CommandResult result, PrintWriter out) {
    try {
      result.accept(new ResultVisitor() {
        @Override
        public void visit(TextResult text) {
          for (String line : text.lines()) {
            out.println(line);
          }
        }

        @Override
        public void visit(TabularResult tabular) {
          formatTabular(tabular, out);
        }

        @Override
        public void visit(StreamingTabularResult streaming) {
          formatStreamingTabular(streaming, out);
        }
      });
    } catch (IOException e) {
      throw new IllegalStateException("Failed to format result", e);
    }
    out.flush();
  }

  private void formatStreamingTabular(StreamingTabularResult result, PrintWriter out) {
    out.println(formatRow(result.header(), true));
    long[] count = { 0 };
    try {
      result.forEachRow(row -> {
        out.println(formatRow(row, false));
        count[0]++;
      });
    } catch (IOException e) {
      throw new IllegalStateException("Failed to stream scan result", e);
    }
    out.println(count[0] + " row(s)");
    Map<String, String> trailer = result.trailer();
    if (!trailer.isEmpty()) {
      out.println();
      out.println(formatRow(Arrays.asList("METRIC", "VALUE"), true));
      for (Map.Entry<String, String> entry : trailer.entrySet()) {
        out.println(formatRow(Arrays.asList(entry.getKey(), entry.getValue()), false));
      }
    }
  }

  /**
   * Mirrors hbase-shell's {@code shell/formatter.rb#row} column layout for a non-tty output stream
   * (where {@code @max_width} is 0 and no column padding is applied): a single-column result prints
   * its value bare; a two-column result separates the header with two spaces and each row with a
   * leading space plus one separating space; three-or-more columns lead every line (header and rows
   * alike) with a space and join the remaining columns with single spaces.
   */
  private void formatTabular(TabularResult result, PrintWriter out) {
    out.println(formatRow(result.header(), true));
    for (List<String> row : result.rows()) {
      out.println(formatRow(row, false));
    }
    out.println(result.rows().size() + " row(s)");
  }

  private String formatRow(List<String> columns, boolean isHeader) {
    if (columns.size() == 1) {
      return columns.get(0);
    }
    if (columns.size() == 2) {
      return isHeader
        ? columns.get(0) + "  " + columns.get(1)
        : " " + columns.get(0) + " " + columns.get(1);
    }
    return " " + String.join(" ", columns);
  }
}
