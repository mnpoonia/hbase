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
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/clear_compaction_queues.rb}.
 */
@InterfaceAudience.Private
public final class ClearCompactionQueuesCommand implements ShellCommand {
  @Override
  public String name() {
    return "clear_compaction_queues";
  }

  @Override
  public String help() {
    return "clear_compaction_queues 'server', clear_compaction_queues 'server', 'long' - clear queues";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "clear_compaction_queues", 2);
    String server =
      ArgParsing.requireArg(command, 0, "clear_compaction_queues requires a server name argument");
    List<String> queues = new ArrayList<>();
    if (command.positionalArgs().size() > 1) {
      Object second = command.positionalArgs().get(1);
      if (second instanceof List) {
        for (Object element : (List<?>) second) {
          queues.add(String.valueOf(element));
        }
      } else {
        queues.add(String.valueOf(second));
      }
    }
    context.admin().clearCompactionQueues(server, queues);
    return TextResult.of();
  }
}
