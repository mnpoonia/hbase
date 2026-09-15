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
import org.apache.hadoop.hbase.newshell.hbase.TableDescription;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/describe.rb}: a single table-name positional
 * argument, printing enabled/disabled status, table attributes, and one line per column family.
 * The QUOTAS section is explicitly not ported for this pilot slice - "Quota is disabled" is always
 * printed instead of checking whether the {@code hbase:quota} table actually exists.
 */
@InterfaceAudience.Private
public final class DescribeCommand implements ShellCommand {
  @Override
  public String name() {
    return "describe";
  }

  @Override
  public String help() {
    return "describe 'table' - show the table's column families and attributes";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("describe requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    TableDescription description = context.admin().describeTable(tableName);

    List<String> lines = new ArrayList<>();
    lines.add("Table " + tableName + " is " + (description.enabled() ? "ENABLED" : "DISABLED"));
    lines.add(tableName + description.tableAttributes());
    lines.add("COLUMN FAMILIES DESCRIPTION");
    lines.addAll(description.columnFamilies());
    lines.add("");
    lines.add(description.columnFamilies().size() + " row(s)");
    lines.add("Quota is disabled");
    return new TextResult(lines);
  }
}
