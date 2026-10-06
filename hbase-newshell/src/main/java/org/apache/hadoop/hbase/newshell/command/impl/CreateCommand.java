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
import java.util.LinkedHashMap;
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
 * Ported, minimal slice, from hbase-shell's {@code hbase/admin.rb#create}: one table name plus one
 * or more column families, each given either as a bareword string (e.g. {@code 'f1'}) or a hash
 * literal ({@code NAME} required, {@code VERSIONS} optional), plus an optional table-level
 * attribute hash with no {@code NAME} key (e.g. {@code SPLITS => [...]}) - see
 * {@link org.apache.hadoop.hbase.newshell.hbase.ShellAdmin#createTable} for the supported
 * attributes. When native {@code --flag} options are combined with multiple bareword families (e.g.
 * {@code create 't1', 'f1', 'f2' --versions=3}, a newshell-only syntax with no hbase-shell
 * equivalent), the flags are applied to exactly one family - the last bareword {@code NAME}-only
 * one - not to every family.
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
    String tableName = ArgParsing.requireArg(command, 0, "create requires a table name argument");

    List<Map<String, Object>> familySpecs = new ArrayList<>();
    // Bareword family names, e.g. create 't1', 'f1', 'f2' - each becomes a family spec with
    // just a NAME, mirroring hbase-shell's admin.rb#create treating a String arg as a family.
    for (Object extraPositional : command.positionalArgs().subList(1,
      command.positionalArgs().size())) {
      familySpecs.add(Collections.singletonMap("NAME", String.valueOf(extraPositional)));
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
    // options() also contains a flattened copy of every hash literal, so strip those keys out
    // first - what remains is native --flag input that hash literals didn't already contribute,
    // which must still be merged in rather than silently dropped.
    Map<String, Object> flagOnlyOptions = new LinkedHashMap<>(command.options());
    for (Map<String, Object> hashLiteral : command.hashLiterals()) {
      flagOnlyOptions.keySet().removeAll(hashLiteral.keySet());
    }
    if (!flagOnlyOptions.isEmpty()) {
      if (familySpecs.isEmpty()) {
        // Native flag syntax (--name=f1 --versions=3) has no hash literal or bareword family at
        // all, but still describes exactly one family via the flat options map.
        familySpecs.add(new LinkedHashMap<>(flagOnlyOptions));
      } else {
        // create 't1', 'f1' --versions=3: merge flags onto exactly one family - the last
        // bareword NAME-only one, if any, else the last family overall - so --flag attributes
        // are not silently dropped, and not silently applied to every family either (there is
        // no hbase-shell syntax to compare against here; --flag is a newshell-only addition, so
        // "apply to the single most-recently-named family" is the least surprising choice).
        int targetIndex = familySpecs.size() - 1;
        for (int i = familySpecs.size() - 1; i >= 0; i--) {
          Map<String, Object> fam = familySpecs.get(i);
          if (fam.size() == 1 && fam.containsKey("NAME")) {
            targetIndex = i;
            break;
          }
        }
        Map<String, Object> enriched = new LinkedHashMap<>(familySpecs.get(targetIndex));
        enriched.putAll(flagOnlyOptions);
        familySpecs.set(targetIndex, enriched);
      }
    }
    if (familySpecs.isEmpty()) {
      throw new ShellCommandException(
        "create requires a column family spec, e.g. 'f1' or {NAME => 'f1'}");
    }
    context.admin().createTable(tableName, familySpecs, tableAttributes);
    return TextResult.of("Created table " + tableName);
  }
}
