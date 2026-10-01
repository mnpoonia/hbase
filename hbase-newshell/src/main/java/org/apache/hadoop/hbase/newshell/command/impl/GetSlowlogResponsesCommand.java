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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/get_slowlog_responses.rb}.
 */
@InterfaceAudience.Private
public final class GetSlowlogResponsesCommand implements ShellCommand {
  @Override
  public String name() {
    return "get_slowlog_responses";
  }

  @Override
  public String help() {
    return "get_slowlog_responses '*', get_slowlog_responses ['s1'], LIMIT => 50 - slow RPC logs";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("get_slowlog_responses requires server name(s) or '*'");
    }
    List<String> servers = toServers(command.positionalArgs().get(0));
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
    lines.add("Retrieved SlowLog Responses from RegionServers");
    lines.addAll(context.admin().getSlowLogResponses(servers, args, false));
    return new TextResult(lines);
  }

  private static List<String> toServers(Object value) {
    if (value instanceof List) {
      List<String> result = new ArrayList<>();
      for (Object element : (List<?>) value) {
        result.add(String.valueOf(element));
      }
      return result;
    }
    return Arrays.asList(String.valueOf(value));
  }
}
