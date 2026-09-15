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
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/alter.rb}: one table name plus
 * one or more column family attribute hash literals, each requiring {@code NAME} to identify an
 * existing column family on the table (e.g. {@code alter 't1', {NAME => 'f1', TTL => 100}}).
 * Adding/deleting column families, table-scope attributes (MAX_FILESIZE, etc.), and coprocessors
 * are explicitly not ported for this slice.
 */
@InterfaceAudience.Private
public final class AlterCommand implements ShellCommand {
  @Override
  public String name() {
    return "alter";
  }

  @Override
  public String help() {
    return "alter 'table', {NAME => 'family', TTL => N} - modify an existing column family's "
      + "attributes";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("alter requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    List<Map<String, Object>> familySpecs = command.hashLiterals();
    if (familySpecs.isEmpty()) {
      throw new ShellCommandException(
        "alter requires at least one column family spec, e.g. {NAME => 'f1', TTL => 100}");
    }
    context.admin().alterTable(tableName, familySpecs);
    return TextResult.of(tableName + " altered");
  }
}
