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
 * Ported from hbase-shell's {@code hbase/table.rb#_get_splits_internal}: the start key of every
 * default-replica region, excluding the first (empty) start key. Total split count is reported as
 * region count minus one.
 */
@InterfaceAudience.Private
public final class GetSplitsCommand implements ShellCommand {
  @Override
  public String name() {
    return "get_splits";
  }

  @Override
  public String help() {
    return "get_splits 'table' - print the split points of a table";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("get_splits requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    List<String> splits = context.tables().forTable(tableName).getSplits();
    List<String> lines = new ArrayList<>();
    lines.add("Total number of splits = " + (splits.size() + 1));
    lines.addAll(splits);
    return new TextResult(lines);
  }
}
