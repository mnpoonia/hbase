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
 * Ported from hbase-shell's {@code shell/commands/decommission_regionservers.rb}: a single
 * server-name string, or an array of them, naming the region server(s) to decommission (matched
 * against live servers by hostname/host-port, per {@code hbase/admin.rb#getServerNames}), plus an
 * optional trailing boolean (default {@code false}) for whether to offload their regions.
 */
@InterfaceAudience.Private
public final class DecommissionRegionServersCommand implements ShellCommand {
  @Override
  public String name() {
    return "decommission_regionservers";
  }

  @Override
  public String help() {
    return "decommission_regionservers 'server', decommission_regionservers ['s1', 's2'], true - "
      + "mark region server(s) as decommissioned, optionally offloading their regions";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException(
        "decommission_regionservers requires a server name (or array of server names) argument");
    }
    List<String> hostOrServers = toStringList(positionals.get(0));
    boolean offload = positionals.size() > 1 && parseBoolean(positionals.get(1));
    context.admin().decommissionRegionServers(hostOrServers, offload);
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

  private static boolean parseBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }
}
