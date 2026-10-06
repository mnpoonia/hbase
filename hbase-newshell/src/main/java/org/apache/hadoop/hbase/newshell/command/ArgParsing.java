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
package org.apache.hadoop.hbase.newshell.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/** Shared parsing helpers for shell command arguments. */
@InterfaceAudience.Private
public final class ArgParsing {
  private ArgParsing() {
  }

  /** Accepts a Boolean object or a string {@code true}/{@code false} (case-insensitive). */
  public static boolean parseBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  /**
   * Rejects surplus positional arguments so that, e.g., {@code truncate 't1', 't2'} fails instead
   * of silently truncating only {@code t1}.
   */
  public static void requireMaxArgs(ParsedCommand command, String commandName, int max)
    throws ShellCommandException {
    int actual = command.positionalArgs().size();
    if (actual > max) {
      throw new ShellCommandException(commandName + " takes at most " + max
        + " positional argument(s), got " + actual + ": " + command.positionalArgs());
    }
  }

  /** Reads a {@code MAXLENGTH} option value; absent means no limit (-1). */
  public static int maxLength(Object value) throws ShellCommandException {
    if (value == null) {
      return -1;
    }
    if (!(value instanceof Number)) {
      throw new ShellCommandException("MAXLENGTH must be a number");
    }
    return ((Number) value).intValue();
  }

  /** Fails with {@code message} unless at least {@code min} positional arguments were given. */
  public static void requireArgs(ParsedCommand command, int min, String message)
    throws ShellCommandException {
    if (command.positionalArgs().size() < min) {
      throw new ShellCommandException(message);
    }
  }

  /** Positional argument {@code index} rendered as a string; the caller has checked it exists. */
  public static String string(ParsedCommand command, int index) {
    return String.valueOf(command.positionalArgs().get(index));
  }

  /** Positional argument {@code index} as a string, or {@code fallback} when absent. */
  public static String optionalArg(ParsedCommand command, int index, String fallback) {
    return command.positionalArgs().size() > index ? string(command, index) : fallback;
  }

  /** Requires positional argument {@code index} and returns it as a string. */
  public static String requireArg(ParsedCommand command, int index, String message)
    throws ShellCommandException {
    requireArgs(command, index + 1, message);
    return string(command, index);
  }

  /** A list value becomes a list of strings; any other value becomes a singleton list. */
  public static List<String> stringList(Object value) {
    if (value instanceof List) {
      List<String> result = new ArrayList<>();
      for (Object element : (List<?>) value) {
        result.add(String.valueOf(element));
      }
      return result;
    }
    return Collections.singletonList(String.valueOf(value));
  }

  /** Copies a map, stringifying its keys. */
  public static Map<String, Object> stringKeyed(Map<?, ?> source) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<?, ?> e : source.entrySet()) {
      out.put(String.valueOf(e.getKey()), e.getValue());
    }
    return out;
  }

  /**
   * The table-CFs map of the {@code *_peer_tableCFs} commands: positional argument 1 when it is a
   * hash, else the first hash literal, else {@code null}.
   */
  public static Map<String, Object> tableCfs(ParsedCommand command) throws ShellCommandException {
    if (command.positionalArgs().size() > 1) {
      Object arg = command.positionalArgs().get(1);
      if (arg instanceof Map) {
        return stringKeyed((Map<?, ?>) arg);
      }
      throw new ShellCommandException("table-cfs argument must be a Hash");
    }
    if (!command.hashLiterals().isEmpty()) {
      return new LinkedHashMap<>(command.hashLiterals().get(0));
    }
    return null;
  }

  /** True when the {@code YES} option (flag or {@code Y}/{@code true} value) was given. */
  public static boolean isYes(ParsedCommand command) {
    Object yes = command.options().get("YES");
    if (yes == null) {
      return false;
    }
    if (yes instanceof Boolean) {
      return (Boolean) yes;
    }
    return parseBoolean(yes) || "Y".equalsIgnoreCase(String.valueOf(yes));
  }
}
