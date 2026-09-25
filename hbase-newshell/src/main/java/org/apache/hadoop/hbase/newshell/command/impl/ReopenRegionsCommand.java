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
 * Ported from hbase-shell's {@code shell/commands/reopen_regions.rb}: table name plus optional
 * region-name array.
 */
@InterfaceAudience.Private
public final class ReopenRegionsCommand implements ShellCommand {
  @Override
  public String name() {
    return "reopen_regions";
  }

  @Override
  public String help() {
    return "reopen_regions 'table', reopen_regions 'table', ['r1','r2'] - reopen table regions";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("reopen_regions requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    List<String> regions = new ArrayList<>();
    if (command.positionalArgs().size() > 1) {
      Object second = command.positionalArgs().get(1);
      if (second instanceof List) {
        for (Object element : (List<?>) second) {
          regions.add(String.valueOf(element));
        }
      } else {
        regions.add(String.valueOf(second));
      }
    }
    context.admin().reopenRegions(tableName, regions);
    return TextResult.of();
  }
}
