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
      if (result instanceof TextResult) {
        TextResult textResult = (TextResult) result;
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "ok");
        if (commandName != null) {
          envelope.put("command", commandName);
        }
        envelope.put("data", Collections.singletonMap("lines", textResult.lines()));
        out.println(JsonMapper.writeObjectAsString(envelope));
      } else if (result instanceof TabularResult) {
        TabularResult tabularResult = (TabularResult) result;
        List<String> header = tabularResult.header();
        for (List<String> row : tabularResult.rows()) {
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
        trailer.put("rows", tabularResult.rows().size());
        out.println(JsonMapper.writeObjectAsString(trailer));
      } else if (result instanceof StreamingTabularResult) {
        StreamingTabularResult streamingResult = (StreamingTabularResult) result;
        List<String> header = streamingResult.header();
        int[] count = { 0 };
        streamingResult.forEachRow(row -> {
          Map<String, Object> obj = new LinkedHashMap<>();
          for (int i = 0; i < header.size(); i++) {
            String key = header.get(i);
            String value = i < row.size() ? row.get(i) : "";
            obj.put(key, value);
          }
          count[0]++;
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
        trailer.put("rows", count[0]);
        if (!streamingResult.trailer().isEmpty()) {
          trailer.put("metrics", streamingResult.trailer());
        }
        out.println(JsonMapper.writeObjectAsString(trailer));
      } else {
        throw new IllegalArgumentException("Unknown CommandResult type: " + result.getClass());
      }
    } catch (java.io.IOException | java.io.UncheckedIOException e) {
      throw new IllegalStateException("Failed to serialize JSON output", e);
    }
    out.flush();
  }
}
