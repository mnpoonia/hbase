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
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/clear_deadservers.rb}: optional server-name
 * arguments (all dead servers when omitted); returns true on full success, or the uncleared servers
 * as a SERVERNAME table.
 */
@InterfaceAudience.Private
public final class ClearDeadserversCommand implements ShellCommand {
  @Override
  public String name() {
    return "clear_deadservers";
  }

  @Override
  public String help() {
    return "clear_deadservers, clear_deadservers 'host,port,startcode' - clear dead region "
      + "servers that are never used";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<String> requested = new ArrayList<>();
    for (Object arg : command.positionalArgs()) {
      if (arg instanceof List) {
        for (Object element : (List<?>) arg) {
          requested.add(String.valueOf(element));
        }
      } else {
        requested.add(String.valueOf(arg));
      }
    }
    List<String> uncleared = context.admin().clearDeadServers(requested);
    if (uncleared.isEmpty()) {
      return TextResult.of("true");
    }
    List<List<String>> rows = new ArrayList<>();
    for (String server : uncleared) {
      rows.add(List.of(server));
    }
    return new TabularResult(List.of("SERVERNAME"), rows);
  }
}
