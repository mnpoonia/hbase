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
 * Ported from hbase-shell's {@code shell/commands/clear_slowlog_responses.rb}: optional list of
 * RegionServer names (all live RS when omitted).
 */
@InterfaceAudience.Private
public final class ClearSlowlogResponsesCommand implements ShellCommand {
  @Override
  public String name() {
    return "clear_slowlog_responses";
  }

  @Override
  public String help() {
    return "clear_slowlog_responses, clear_slowlog_responses ['s1','s2'] - clear slowlog responses";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<String> servers = new ArrayList<>();
    for (Object arg : command.positionalArgs()) {
      if (arg instanceof List) {
        for (Object element : (List<?>) arg) {
          servers.add(String.valueOf(element));
        }
      } else {
        servers.add(String.valueOf(arg));
      }
    }
    return TextResult.of(context.diagnostics().clearSlowLogResponses(servers));
  }
}
