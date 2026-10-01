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

import java.io.PrintWriter;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.yetus.audience.InterfaceAudience;

/** RFC 4180-ish CSV renderer (quotes fields that need it). */
@InterfaceAudience.Private
public final class CsvFormatter implements Formatter {
  @Override
  public void format(String commandName, CommandResult result, PrintWriter out) {
    if (result instanceof TextResult) {
      TextResult textResult = (TextResult) result;
      out.println(csvEscape("line"));
      for (String line : textResult.lines()) {
        out.println(csvEscape(line));
      }
    } else if (result instanceof TabularResult) {
      TabularResult tabularResult = (TabularResult) result;
      out.println(joinCsv(tabularResult.header()));
      for (List<String> row : tabularResult.rows()) {
        out.println(joinCsv(row));
      }
    } else {
      throw new IllegalArgumentException("Unknown CommandResult type: " + result.getClass());
    }
    out.flush();
  }

  private static String joinCsv(List<String> columns) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(csvEscape(columns.get(i)));
    }
    return sb.toString();
  }

  static String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
      || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    if (!needsQuotes) {
      return value;
    }
    return '"' + value.replace("\"", "\"\"") + '"';
  }
}
