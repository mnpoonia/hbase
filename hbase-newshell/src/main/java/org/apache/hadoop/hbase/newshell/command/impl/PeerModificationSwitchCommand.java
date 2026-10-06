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
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

@InterfaceAudience.Private
public final class PeerModificationSwitchCommand implements ShellCommand {
  @Override
  public String name() {
    return "peer_modification_switch";
  }

  @Override
  public String help() {
    return "peer_modification_switch true|false [, drainProcs]";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "peer_modification_switch", 2);
    ArgParsing.requireArgs(command, 1, "peer_modification_switch requires true|false");
    boolean enabled = ArgParsing.parseBoolean(command.positionalArgs().get(0));
    boolean drain = false;
    if (command.positionalArgs().size() > 1) {
      drain = ArgParsing.parseBoolean(command.positionalArgs().get(1));
    }
    boolean previous = context.admin().peerModificationSwitch(enabled, drain);
    return TextResult.of("Previous peer modification state : " + previous);
  }
}
