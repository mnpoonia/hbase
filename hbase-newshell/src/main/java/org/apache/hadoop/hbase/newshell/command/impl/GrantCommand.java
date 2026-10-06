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
 * Ported, original form only, from hbase-shell's {@code shell/commands/grant.rb}: user (or
 * {@code @group}), permissions, and an optional table (or {@code @namespace})/family/qualifier; or
 * the cell-ACL form: table, a user-to-permissions hash, and a scanner spec hash.
 */
@InterfaceAudience.Private
public final class GrantCommand implements ShellCommand {
  @Override
  public String name() {
    return "grant";
  }

  @Override
  public String help() {
    return "grant 'user', 'RWXCA' [, 'table' [, 'family' [, 'qualifier']]] - grant permissions; "
      + "use '@group'/'@namespace' for groups/namespaces";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.size() == 1 && !command.hashLiterals().isEmpty()) {
      return grantCellAcl(command, context);
    }
    if (positionals.size() < 2) {
      throw new ShellCommandException(
        "grant requires a user (or group) and a permissions " + "argument");
    }
    if (!(positionals.get(1) instanceof String)) {
      throw new ShellCommandException("grant: second argument should be a String or Hash");
    }
    String userOrGroup = String.valueOf(positionals.get(0));
    String actions = String.valueOf(positionals.get(1));
    String tableOrNamespace = positionals.size() > 2 ? String.valueOf(positionals.get(2)) : null;
    String family = positionals.size() > 3 ? String.valueOf(positionals.get(3)) : null;
    String qualifier = positionals.size() > 4 ? String.valueOf(positionals.get(4)) : null;

    String tableName = tableOrNamespace;
    String namespace = null;
    if (tableOrNamespace != null && tableOrNamespace.startsWith("@")) {
      namespace = tableOrNamespace.substring(1);
      tableName = null;
    }
    context.securityAdmin().grant(userOrGroup, actions, tableName, family, qualifier, namespace);
    return TextResult.of();
  }

  /** {@code grant 'table', {'user' => 'RW'}, {scan spec}}: update the ACL of every matched cell. */
  private static CommandResult grantCellAcl(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Map<String, Object>> hashes = command.hashLiterals();
    if (hashes.size() != 2) {
      throw new ShellCommandException(
        "grant: cell ACL form is grant 'table', {'user' => 'perms'}, {scan spec}");
    }
    Map<String, String> permissions = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : hashes.get(0).entrySet()) {
      permissions.put(entry.getKey(), String.valueOf(entry.getValue()));
    }
    long rows = context.tables().forTable(String.valueOf(command.positionalArgs().get(0)))
      .setCellPermissions(permissions, hashes.get(1));
    return new TextResult(java.util.Collections.singletonList(rows + " row(s)"));
  }
}
