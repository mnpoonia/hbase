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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.util.JsonMapper;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * JSON / NDJSON renderer. {@link TextResult} emits a single envelope object. {@link TabularResult}
 * streams one JSON object per row (NDJSON) using header names as keys, then a final envelope line
 * with {@code status}/{@code command}/{@code rows} — suitable for large {@code scan} results
 * without buffering a giant array.
 */
@InterfaceAudience.Private
public final class JsonFormatter implements Formatter {
  @Override
  public void format(String commandName, CommandResult result, PrintWriter out) {
    try {
      if (result instanceof TextResult textResult) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "ok");
        if (commandName != null) {
          envelope.put("command", commandName);
        }
        envelope.put("data", Map.of("lines", textResult.lines()));
        out.println(JsonMapper.writeObjectAsString(envelope));
      } else if (result instanceof TabularResult tabularResult) {
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
      } else {
        throw new IllegalArgumentException("Unknown CommandResult type: " + result.getClass());
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Failed to serialize JSON output", e);
    }
    out.flush();
  }
}
