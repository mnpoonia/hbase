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
 * Ported from hbase-shell's {@code shell/commands/revoke.rb}: user (or {@code @group}), and an
 * optional table (or {@code @namespace})/family/qualifier. Unlike {@code grant}, revoke has no
 * actions argument - it always revokes all actions.
 */
@InterfaceAudience.Private
public final class RevokeCommand implements ShellCommand {
  @Override
  public String name() {
    return "revoke";
  }

  @Override
  public String help() {
    return "revoke 'user' [, 'table' [, 'family' [, 'qualifier']]] - revoke access rights; use "
      + "'@group'/'@namespace' for groups/namespaces";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException("revoke requires a user (or group) argument");
    }
    String userOrGroup = String.valueOf(positionals.get(0));
    String tableOrNamespace = positionals.size() > 1 ? String.valueOf(positionals.get(1)) : null;
    String family = positionals.size() > 2 ? String.valueOf(positionals.get(2)) : null;
    String qualifier = positionals.size() > 3 ? String.valueOf(positionals.get(3)) : null;

    String tableName = tableOrNamespace;
    String namespace = null;
    if (tableOrNamespace != null && tableOrNamespace.startsWith("@")) {
      namespace = tableOrNamespace.substring(1);
      tableName = null;
    }
    context.admin().revoke(userOrGroup, tableName, family, qualifier, namespace);
    return TextResult.of();
  }
}
