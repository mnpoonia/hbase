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
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, original form only, from hbase-shell's {@code shell/commands/grant.rb}: user (or
 * {@code @group}), permissions, and an optional table (or {@code @namespace})/family/qualifier.
 * The cell-ACL-update form (a permissions hash plus a scanner spec) is explicitly not ported.
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
    if (positionals.size() < 2) {
      throw new ShellCommandException("grant requires a user (or group) and a permissions "
        + "argument");
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
    context.admin().grant(userOrGroup, actions, tableName, family, qualifier, namespace);
    return TextResult.of();
  }
}
