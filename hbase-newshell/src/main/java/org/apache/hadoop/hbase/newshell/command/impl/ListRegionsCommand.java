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
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/list_regions.rb}: table name
 * only, always reporting all seven columns (SERVER_NAME, REGION_NAME, START_KEY, END_KEY, SIZE,
 * REQ, LOCALITY) for every region. The Ruby original's optional server-name filter, locality
 * threshold, and column-projection array are not ported for this slice.
 */
@InterfaceAudience.Private
public final class ListRegionsCommand implements ShellCommand {
  @Override
  public String name() {
    return "list_regions";
  }

  @Override
  public String help() {
    return "list_regions 'table' - list all regions for a table";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("list_regions requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    List<List<String>> rows = context.admin().listRegions(tableName);
    return new TabularResult(
      List.of("SERVER_NAME", "REGION_NAME", "START_KEY", "END_KEY", "SIZE", "REQ", "LOCALITY"),
      rows);
  }
}
