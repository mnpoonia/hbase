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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@code help} lists every registered command with its one-line usage; {@code help <command>} shows
 * the usage of a single command.
 */
@InterfaceAudience.Private
public final class HelpCommand implements ShellCommand {
  private CommandRegistry registry;

  public HelpCommand() {
  }

  HelpCommand(CommandRegistry registry) {
    this.registry = registry;
  }

  @Override
  public String name() {
    return "help";
  }

  @Override
  public String help() {
    return "help [command] - list all commands, or show usage of one command";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException {
    ArgParsing.requireMaxArgs(command, "help", 1);
    CommandRegistry commands = registry();
    if (command.positionalArgs().isEmpty()) {
      List<String> lines = new ArrayList<>();
      for (String name : commands.commandNames()) {
        lines.add(commands.lookup(name).get().help());
      }
      return new TextResult(lines);
    }
    String target = ArgParsing.string(command, 0);
    Optional<ShellCommand> found = commands.lookup(target);
    if (!found.isPresent()) {
      throw new ShellCommandException("help: unknown command '" + target + "'");
    }
    return TextResult.of(found.get().help());
  }

  private synchronized CommandRegistry registry() {
    if (registry == null) {
      registry = new CommandRegistry();
    }
    return registry;
  }
}
