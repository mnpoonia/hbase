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
 * Ported from hbase-shell's {@code shell/commands/change_sft.rb}: a table name and a
 * StoreFileTracker implementation name, or a table name, family, and implementation name.
 */
@InterfaceAudience.Private
public final class ChangeSftCommand implements ShellCommand {
  @Override
  public String name() {
    return "change_sft";
  }

  @Override
  public String help() {
    return "change_sft 'table', 'FILE' | change_sft 'table', 'family', 'FILE' - change a "
      + "table's (or column family's) StoreFileTracker implementation";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.size() == 2) {
      context.admin().changeSft(String.valueOf(positionals.get(0)), null,
        String.valueOf(positionals.get(1)));
    } else if (positionals.size() == 3) {
      context.admin().changeSft(String.valueOf(positionals.get(0)),
        String.valueOf(positionals.get(1)), String.valueOf(positionals.get(2)));
    } else {
      throw new ShellCommandException("change_sft requires two or three arguments");
    }
    return TextResult.of();
  }
}
