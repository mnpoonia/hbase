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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/get_largelog_responses.rb}.
 */
@InterfaceAudience.Private
public final class GetLargelogResponsesCommand implements ShellCommand {
  @Override
  public String name() {
    return "get_largelog_responses";
  }

  @Override
  public String help() {
    return "get_largelog_responses '*', get_largelog_responses ['s1'], LIMIT => 50 - large RPC logs";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "get_largelog_responses", 2);
    ArgParsing.requireArgs(command, 1, "get_largelog_responses requires server name(s) or '*'");
    List<String> servers = ArgParsing.stringList(command.positionalArgs().get(0));
    Map<String, Object> args = new LinkedHashMap<>(command.options());
    for (Map<String, Object> hash : command.hashLiterals()) {
      args.putAll(hash);
    }
    if (command.positionalArgs().size() > 1) {
      Object second = command.positionalArgs().get(1);
      if (second instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) second;
        args.putAll(map);
      }
    }
    List<String> lines = new ArrayList<>();
    lines.add("Retrieved LargeLog Responses from RegionServers");
    lines.addAll(context.admin().getSlowLogResponses(servers, args, true));
    return new TextResult(lines);
  }

}
