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
import java.util.LinkedHashMap;
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
 * Ported, minimal slice, from hbase-shell's {@code hbase/admin.rb#create}: one table name plus
 * one or more column families, each given either as a bareword string (e.g. {@code 'f1'}) or a
 * hash literal ({@code NAME} required, {@code VERSIONS} optional), plus an optional table-level
 * attribute hash with no {@code NAME} key (e.g. {@code SPLITS => [...]}) - see
 * {@link org.apache.hadoop.hbase.newshell.hbase.ShellAdmin#createTable} for the supported
 * attributes. SPLITALGO, CONFIGURATION and MOB options are explicitly not ported for this pilot
 * slice.
 */
@InterfaceAudience.Private
public final class CreateCommand implements ShellCommand {
  @Override
  public String name() {
    return "create";
  }

  @Override
  public String help() {
    return "create 'table', 'family', {NAME => 'family', VERSIONS => N}, ... - create a table "
      + "with one or more column families";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("create requires a table name argument");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));

    List<Map<String, Object>> familySpecs = new ArrayList<>();
    // Bareword family names, e.g. create 't1', 'f1', 'f2' - each becomes a family spec with
    // just a NAME, mirroring hbase-shell's admin.rb#create treating a String arg as a family.
    for (Object extraPositional : command.positionalArgs().subList(1, command.positionalArgs().size())) {
      familySpecs.add(Map.of("NAME", String.valueOf(extraPositional)));
    }
    // Only hash literals with a NAME are families; a hash literal without one (e.g.
    // SPLITS => [...]) is a table-level attribute (mirrors admin.rb#create's NAME check).
    Map<String, Object> tableAttributes = new LinkedHashMap<>();
    for (Map<String, Object> hashLiteral : command.hashLiterals()) {
      if (hashLiteral.containsKey("NAME")) {
        familySpecs.add(hashLiteral);
      } else {
        tableAttributes.putAll(hashLiteral);
      }
    }
    // Native flag syntax (--name=f1 --versions=3) has no hash literal at all, but still
    // describes exactly one family via the flat options map.
    if (familySpecs.isEmpty() && !command.options().isEmpty()) {
      familySpecs.add(command.options());
    }
    if (familySpecs.isEmpty()) {
      throw new ShellCommandException(
        "create requires a column family spec, e.g. 'f1' or {NAME => 'f1'}");
    }
    context.admin().createTable(tableName, familySpecs, tableAttributes);
    return TextResult.of(tableName + " created");
  }
}
