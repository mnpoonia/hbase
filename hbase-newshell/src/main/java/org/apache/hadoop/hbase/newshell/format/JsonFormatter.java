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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ResultVisitor;
import org.apache.hadoop.hbase.newshell.command.StreamingTabularResult;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.util.JsonMapper;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * JSON / NDJSON renderer. {@link TextResult} emits a single envelope object. {@link TabularResult}
 * and {@link StreamingTabularResult} both stream one JSON object per row (NDJSON) using header
 * names as keys, then a final envelope line with {@code status}/{@code command}/{@code rows}.
 * {@link StreamingTabularResult} (used by {@code scan}) is the one that's actually safe for large
 * result sets without buffering a giant array in memory - {@link TabularResult} still requires its
 * full row list to be built by the caller first.
 */
@InterfaceAudience.Private
public final class JsonFormatter implements Formatter {
  @Override
  public void formatError(String message, PrintWriter out) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("status", "error");
    envelope.put("error", message);
    try {
      out.println(JsonMapper.writeObjectAsString(envelope));
    } catch (java.io.IOException e) {
      out.println("ERROR: " + message);
    }
  }

  @Override
  public void format(String commandName, CommandResult result, PrintWriter out) {
    try {
      result.accept(new ResultVisitor() {
        @Override
        public void visit(TextResult text) throws java.io.IOException {
          Map<String, Object> envelope = new LinkedHashMap<>();
          envelope.put("status", "ok");
          if (commandName != null) {
            envelope.put("command", commandName);
          }
          envelope.put("data", Collections.singletonMap("lines", text.lines()));
          out.println(JsonMapper.writeObjectAsString(envelope));
        }

        @Override
        public void visit(TabularResult tabular) throws java.io.IOException {
          List<String> header = tabular.header();
          for (List<String> row : tabular.rows()) {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
              String key = header.get(i);
              String value = i < row.size() ? row.get(i) : "";
              obj.put(key, value);
            }
            out.println(JsonMapper.writeObjectAsString(obj));
          }
          Map<String, Object> trailer = new LinkedHashMap<>();
          trailer.put("status", "ok");
          if (commandName != null) {
            trailer.put("command", commandName);
          }
          trailer.put("rows", tabular.rows().size());
          out.println(JsonMapper.writeObjectAsString(trailer));
        }

        @Override
        public void visit(StreamingTabularResult streaming) throws java.io.IOException {
          List<String> header = streaming.header();
          streaming.forEachRow(row -> {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
              String key = header.get(i);
              String value = i < row.size() ? row.get(i) : "";
              obj.put(key, value);
            }
            try {
              out.println(JsonMapper.writeObjectAsString(obj));
            } catch (java.io.IOException e) {
              throw new java.io.UncheckedIOException(e);
            }
          });
          Map<String, Object> trailer = new LinkedHashMap<>();
          trailer.put("status", "ok");
          if (commandName != null) {
            trailer.put("command", commandName);
          }
          trailer.put("rows", (int) streaming.rowCount());
          if (!streaming.trailer().isEmpty()) {
            trailer.put("metrics", streaming.trailer());
          }
          out.println(JsonMapper.writeObjectAsString(trailer));
        }
      });
    } catch (java.io.IOException | java.io.UncheckedIOException e) {
      throw new IllegalStateException("Failed to serialize JSON output", e);
    }
    out.flush();
  }
}
