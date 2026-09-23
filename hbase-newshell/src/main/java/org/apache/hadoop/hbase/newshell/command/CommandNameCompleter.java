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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.newshell.spi.Completer;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Completes the command name (the first whitespace-delimited token of the line) against every
 * name registered in a {@link CommandRegistry}. Does not attempt to complete command arguments
 * (table names, etc.) - only the leading command token.
 */
@InterfaceAudience.Private
public final class CommandNameCompleter implements Completer {
  private final List<String> commandNames;

  public CommandNameCompleter(CommandRegistry registry) {
    this.commandNames = registry.commandNames();
  }

  @Override
  public List<String> complete(String buffer, int cursor) {
    int wordStart = wordStart(buffer, cursor);
    if (wordStart != 0) {
      // Cursor is past the first token - completion is out of scope here.
      return Collections.emptyList();
    }
    String prefix = buffer.substring(wordStart, cursor).toLowerCase(Locale.ROOT);
    return commandNames.stream().filter(name -> name.startsWith(prefix))
      .collect(Collectors.toList());
  }

  private static int wordStart(String buffer, int cursor) {
    int i = cursor;
    while (i > 0 && !Character.isWhitespace(buffer.charAt(i - 1))) {
      i--;
    }
    return i;
  }
}
