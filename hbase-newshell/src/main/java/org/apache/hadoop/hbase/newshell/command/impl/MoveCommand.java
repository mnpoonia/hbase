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
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/move.rb}: moves a single region, given its
 * encoded name, to a specific regionserver or to one chosen at random.
 */
@InterfaceAudience.Private
public final class MoveCommand implements ShellCommand {
  @Override
  public String name() {
    return "move";
  }

  @Override
  public String help() {
    return "move 'ENCODED_REGIONNAME'[, 'SERVER_NAME'] - move a region, to a specific server or "
      + "one chosen at random";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    var positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException("move requires an encoded region name argument");
    }
    String encodedRegionName = String.valueOf(positionals.get(0));
    String destServerName = positionals.size() > 1 ? String.valueOf(positionals.get(1)) : null;
    context.admin().move(encodedRegionName, destServerName);
    return TextResult
      .of("Moved region " + encodedRegionName + (destServerName != null ? " to " + destServerName
        : " to a randomly chosen server"));
  }
}
