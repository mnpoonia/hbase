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
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/flush.rb}: a table, region, encoded-region, or
 * region server name, plus an optional column family.
 */
@InterfaceAudience.Private
public final class FlushCommand implements ShellCommand {
  @Override
  public String name() {
    return "flush";
  }

  @Override
  public String help() {
    return "flush 'tableOrRegionOrServerName' [, 'family'] - flush a table, region, or region "
      + "server, optionally for a single column family";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException(
        "flush requires a table, region, or region server name argument");
    }
    String name = String.valueOf(positionals.get(0));
    String family = positionals.size() > 1 ? String.valueOf(positionals.get(1)) : null;
    context.admin().flush(name, family);
    return TextResult.of();
  }
}
