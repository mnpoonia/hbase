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

/**
 * Plain-text renderer for the two {@link CommandResult} shapes. Uses {@code instanceof} rather
 * than a pattern-matching {@code switch} because this module's release target is Java 17, where
 * switch pattern matching on sealed types is still a preview feature (stable only from Java 21).
 */
@InterfaceAudience.Private
public final class DefaultFormatter implements Formatter {
  @Override
  public void format(CommandResult result, PrintWriter out) {
    if (result instanceof TextResult textResult) {
      for (String line : textResult.lines()) {
        out.println(line);
      }
    } else if (result instanceof TabularResult tabularResult) {
      formatTabular(tabularResult, out);
    } else {
      throw new IllegalArgumentException("Unknown CommandResult type: " + result.getClass());
    }
    out.flush();
  }

  private void formatTabular(TabularResult result, PrintWriter out) {
    out.println(String.join("\t", result.header()));
    for (List<String> row : result.rows()) {
      out.println(String.join("\t", row));
    }
    out.println(result.rows().size() + " row(s)");
  }
}
