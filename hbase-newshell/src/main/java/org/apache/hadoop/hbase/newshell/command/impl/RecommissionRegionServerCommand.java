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
 * Ported from hbase-shell's {@code shell/commands/recommission_regionserver.rb}: a single
 * server-name string (matched against live servers by hostname/host-port, per
 * {@code hbase/admin.rb#getServerName}), plus an optional trailing array of encoded region names
 * to load onto it (default none).
 */
@InterfaceAudience.Private
public final class RecommissionRegionServerCommand implements ShellCommand {
  @Override
  public String name() {
    return "recommission_regionserver";
  }

  @Override
  public String help() {
    return "recommission_regionserver 'server', recommission_regionserver 'server', "
      + "['encoded_region_name'] - remove the decommission marker from a region server, "
      + "optionally loading the given regions onto it";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException("recommission_regionserver requires a server name argument");
    }
    String hostOrServer = String.valueOf(positionals.get(0));
    List<String> encodedRegionNames =
      positionals.size() > 1 ? toStringList(positionals.get(1)) : List.of();
    context.admin().recommissionRegionServer(hostOrServer, encodedRegionNames);
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
