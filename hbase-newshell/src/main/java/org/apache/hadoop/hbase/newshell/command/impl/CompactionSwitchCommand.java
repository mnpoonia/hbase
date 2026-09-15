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
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/compaction_switch.rb}: an enable/disable
 * boolean plus zero or more trailing region server names (all servers when omitted). Returns a
 * SERVER/PREV_STATE row per affected server.
 */
@InterfaceAudience.Private
public final class CompactionSwitchCommand implements ShellCommand {
  @Override
  public String name() {
    return "compaction_switch";
  }

  @Override
  public String help() {
    return "compaction_switch true|false, 'server1', 'server2' - enable/disable compactions on "
      + "the given region servers (or all, if none given), returns previous state per server";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException("compaction_switch requires a true|false argument");
    }
    boolean enabled = parseBoolean(positionals.get(0));
    List<String> serverNames = new ArrayList<>();
    for (Object server : positionals.subList(1, positionals.size())) {
      serverNames.add(String.valueOf(server));
    }
    Map<String, Boolean> previousStates = context.admin().compactionSwitch(enabled, serverNames);
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<String, Boolean> entry : previousStates.entrySet()) {
      rows.add(List.of(entry.getKey(), String.valueOf(entry.getValue())));
    }
    return new TabularResult(List.of("SERVER", "PREV_STATE"), rows);
  }

  private static boolean parseBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }
}
