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
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/alter_async.rb}: identical column family spec
 * as {@link AlterCommand}, but does not wait for all regions to receive the schema change - this
 * matches {@link org.apache.hadoop.hbase.newshell.hbase.ShellAdmin#alterTable}, which already
 * does not block on region propagation, so {@code alter} and {@code alter_async} share the same
 * wrapper-layer call. Use {@link AlterStatusCommand} to check propagation progress.
 */
@InterfaceAudience.Private
public final class AlterAsyncCommand implements ShellCommand {
  @Override
  public String name() {
    return "alter_async";
  }

  @Override
  public String help() {
    return "alter_async 'table', {NAME => 'family', TTL => N} - modify a column family's "
      + "attributes without waiting for regions to pick up the change";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("alter_async requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    List<Map<String, Object>> familySpecs = command.hashLiterals();
    if (familySpecs.isEmpty()) {
      throw new ShellCommandException(
        "alter_async requires at least one column family spec, e.g. {NAME => 'f1', TTL => 100}");
    }
    context.admin().alterTable(tableName, familySpecs);
    return TextResult.of();
  }
}
