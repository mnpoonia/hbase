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
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.ShellAdmin;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/disable_all.rb}: disables every table matching
 * a regex. The Ruby original lists the matches and prompts {@code y/n} on stdin before acting;
 * that has no equivalent in newshell's one-shot/piped invocation model (the triggering command
 * itself already consumed stdin), so this port disables the matches directly, without a
 * confirmation step, and reports the same success/failure summary the Ruby version prints after
 * a {@code y} answer.
 */
@InterfaceAudience.Private
public final class DisableAllCommand implements ShellCommand {
  @Override
  public String name() {
    return "disable_all";
  }

  @Override
  public String help() {
    return "disable_all 't.*' - disable all tables matching a regex";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("disable_all requires a regex argument");
    }
    String regex = String.valueOf(command.positionalArgs().get(0));
    ShellAdmin admin = context.admin();
    List<String> tables = admin.listTables(regex);
    if (tables.isEmpty()) {
      return TextResult.of("No tables matched the regex " + regex);
    }
    List<String> failed = new ArrayList<>();
    for (String table : tables) {
      try {
        admin.disableTable(table);
      } catch (IOException e) {
        failed.add(table);
      }
    }
    List<String> lines = new ArrayList<>();
    lines.add((tables.size() - failed.size()) + " tables successfully disabled");
    if (!failed.isEmpty()) {
      lines.add(failed.size() + " tables not disabled due to an exception: "
        + String.join(",", failed));
    }
    return new TextResult(lines);
  }
}
