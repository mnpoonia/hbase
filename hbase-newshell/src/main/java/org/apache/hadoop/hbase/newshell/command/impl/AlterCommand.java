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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/alter.rb}: one table name plus one or more change
 * specs - column family attribute hashes ({@code alter 't1', {NAME => 'f1', TTL => 100}}, which add
 * the family if missing), {@code METHOD => 'delete'} to drop a family, table-scope attributes
 * (MAX_FILESIZE, etc.), and the {@code table_att_unset}/{@code table_conf_unset}/ coprocessor
 * methods. See {@link #help()} for the full list.
 */
@InterfaceAudience.Private
public final class AlterCommand implements ShellCommand {
  @Override
  public String name() {
    return "alter";
  }

  @Override
  public String help() {
    return "alter 'table', {NAME => 'family', TTL => N} - modify a column family (added if "
      + "missing); 'delete' => 'family' - drop it; MAX_FILESIZE => N etc. - table attributes; "
      + "{METHOD => 'table_att_unset', NAME => 'attr'}, {METHOD => 'table_conf_unset', NAME => "
      + "'key'}, {METHOD => 'table_remove_coprocessor', CLASSNAME => 'cls'}, COPROCESSOR => "
      + "'jar|class|priority|k=v' - other table changes; REOPEN_REGIONS => 'false' skips the "
      + "region reopen";
  }

  /**
   * Collects the change specs: a bareword family name becomes {@code {NAME => name}} (adds the
   * family if missing, otherwise a no-op), followed by every hash literal in order.
   */
  static List<Map<String, Object>> alterSpecs(ParsedCommand command) {
    List<Map<String, Object>> specs = new ArrayList<>();
    for (Object family : command.positionalArgs().subList(1, command.positionalArgs().size())) {
      specs.add(Collections.singletonMap("NAME", String.valueOf(family)));
    }
    specs.addAll(command.hashLiterals());
    return specs;
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    String tableName = ArgParsing.requireArg(command, 0, "alter requires a table name argument");
    List<Map<String, Object>> specs = alterSpecs(command);
    if (specs.isEmpty()) {
      throw new ShellCommandException(
        "alter requires at least one change, e.g. {NAME => 'f1', TTL => 100}");
    }
    context.tableAdmin().alterTable(tableName, specs);
    return TextResult.of("Updating all regions with the new schema...");
  }
}
