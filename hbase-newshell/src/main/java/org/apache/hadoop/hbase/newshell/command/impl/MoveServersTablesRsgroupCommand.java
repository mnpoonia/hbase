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
import org.apache.hadoop.hbase.newshell.command.*;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

@InterfaceAudience.Private
public final class MoveServersTablesRsgroupCommand implements ShellCommand {
  @Override
  public String name() {
    return "move_servers_tables_rsgroup";
  }

  @Override
  public String help() {
    return "move_servers_tables_rsgroup 'dest', ['s:p'], ['t1']";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "move_servers_tables_rsgroup", 3);
    ArgParsing.requireArgs(command, 3,
      "move_servers_tables_rsgroup requires dest, servers, tables");
    context.rsGroupAdmin().moveServersAndTablesToRsGroup(
      ArgParsing.stringList(command.positionalArgs().get(1)),
      ArgParsing.stringList(command.positionalArgs().get(2)), ArgParsing.string(command, 0));
    return TextResult.of();
  }

}
