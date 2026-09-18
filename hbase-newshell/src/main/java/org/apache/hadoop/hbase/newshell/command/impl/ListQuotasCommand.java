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
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/list_quotas.rb}: an optional {@code {USER=>...,
 * TABLE=>..., NAMESPACE=>...}} filter hash.
 */
@InterfaceAudience.Private
public final class ListQuotasCommand implements ShellCommand {
  @Override
  public String name() {
    return "list_quotas";
  }

  @Override
  public String help() {
    return "list_quotas [USER => 'bob.*'] [, TABLE => 't1'] [, NAMESPACE => 'ns.*'] - list quota "
      + "settings, optionally filtered";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    Map<String, Object> filterArgs =
      command.hashLiterals().isEmpty() ? command.options() : command.hashLiterals().get(0);
    List<List<String>> rows = context.admin().listQuotas(filterArgs);
    return new TabularResult(List.of("OWNER", "QUOTAS"), rows);
  }
}
