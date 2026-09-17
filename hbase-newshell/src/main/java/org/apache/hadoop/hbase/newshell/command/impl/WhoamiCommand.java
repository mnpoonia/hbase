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
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.security.User;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/whoami.rb}: {@link User#getCurrent()}'s name
 * plus group names, no admin/table interaction.
 */
@InterfaceAudience.Private
public final class WhoamiCommand implements ShellCommand {
  @Override
  public String name() {
    return "whoami";
  }

  @Override
  public String help() {
    return "whoami - print the current user and their groups";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws IOException {
    User user = User.getCurrent();
    List<String> lines = new ArrayList<>();
    lines.add(user.toString());
    String[] groups = user.getGroupNames();
    if (groups != null && groups.length > 0) {
      lines.add("    groups: " + String.join(", ", groups));
    }
    return new TextResult(lines);
  }
}
