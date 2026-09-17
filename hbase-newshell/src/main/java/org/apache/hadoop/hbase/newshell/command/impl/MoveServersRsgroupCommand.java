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
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/move_servers_rsgroup.rb}: a destination group
 * name plus a server name (or array of server names) in {@code host:port} form.
 */
@InterfaceAudience.Private
public final class MoveServersRsgroupCommand implements ShellCommand {
  @Override
  public String name() {
    return "move_servers_rsgroup";
  }

  @Override
  public String help() {
    return "move_servers_rsgroup 'groupName', ['server1:port', 'server2:port'] - move servers "
      + "to a RegionServer group";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.size() < 2) {
      throw new ShellCommandException(
        "move_servers_rsgroup requires a group name and a server (or array of servers) "
          + "argument");
    }
    String groupName = String.valueOf(positionals.get(0));
    List<String> hostPorts = toStringList(positionals.get(1));
    context.admin().moveServersToRsGroup(hostPorts, groupName);
    return TextResult.of();
  }

  @SuppressWarnings("unchecked")
  private static List<String> toStringList(Object value) {
    if (value instanceof List) {
      List<String> result = new ArrayList<>();
      for (Object element : (List<Object>) value) {
        result.add(String.valueOf(element));
      }
      return result;
    }
    return List.of(String.valueOf(value));
  }
}
