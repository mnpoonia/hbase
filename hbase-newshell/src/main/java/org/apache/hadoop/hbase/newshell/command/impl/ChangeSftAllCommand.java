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
 * Ported from hbase-shell's {@code shell/commands/change_sft_all.rb}: a table-name regular
 * expression and a StoreFileTracker implementation name. The REPL-only y/n confirmation prompt
 * (which crashes under piped stdin in the legacy shell) is intentionally dropped, mirroring the
 * existing {@code disable_all}/{@code drop_all}/{@code enable_all} precedent.
 */
@InterfaceAudience.Private
public final class ChangeSftAllCommand implements ShellCommand {
  @Override
  public String name() {
    return "change_sft_all";
  }

  @Override
  public String help() {
    return "change_sft_all 'tableRegex', 'FILE' - change the StoreFileTracker implementation "
      + "of every table matching a regular expression";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.size() != 2) {
      throw new ShellCommandException(
        "change_sft_all requires a table regex and an implementation name argument");
    }
    context.admin().changeSftAll(String.valueOf(positionals.get(0)),
      String.valueOf(positionals.get(1)));
    return TextResult.of();
  }
}
