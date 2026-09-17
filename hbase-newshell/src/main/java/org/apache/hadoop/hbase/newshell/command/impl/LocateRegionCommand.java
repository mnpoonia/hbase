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
import org.apache.hadoop.hbase.newshell.hbase.RegionLocationView;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/locate_region.rb}: table name plus row key,
 * reporting the hosting server and region for that row.
 */
@InterfaceAudience.Private
public final class LocateRegionCommand implements ShellCommand {
  @Override
  public String name() {
    return "locate_region";
  }

  @Override
  public String help() {
    return "locate_region 'table', 'row_key' - locate the region hosting a row key";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().size() < 2) {
      throw new ShellCommandException("locate_region requires a table name and a row key");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    String rowKey = String.valueOf(command.positionalArgs().get(1));
    RegionLocationView location = context.admin().locateRegion(tableName, rowKey);
    return new TabularResult(List.of("HOST", "REGION"),
      List.of(List.of(location.hostnamePort(), location.regionName())));
  }
}
