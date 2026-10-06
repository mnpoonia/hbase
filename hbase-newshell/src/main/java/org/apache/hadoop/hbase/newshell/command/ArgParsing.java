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
}
