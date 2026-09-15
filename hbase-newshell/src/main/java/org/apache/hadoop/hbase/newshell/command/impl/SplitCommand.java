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
 * Ported from hbase-shell's {@code shell/commands/split.rb}: a table or region name, plus an
 * optional explicit split point.
 */
@InterfaceAudience.Private
public final class SplitCommand implements ShellCommand {
  @Override
  public String name() {
    return "split";
  }

  @Override
  public String help() {
    return "split 'table_or_region', 'splitKey' - request a split, optionally at an explicit key";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("split requires a table or region name argument");
    }
    String tableOrRegionName = String.valueOf(command.positionalArgs().get(0));
    String splitPoint =
      command.positionalArgs().size() > 1 ? String.valueOf(command.positionalArgs().get(1)) : null;
    context.admin().split(tableOrRegionName, splitPoint);
    return TextResult.of(tableOrRegionName + " split requested");
  }
}
