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
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/user_permission.rb}: an optional table or
 * {@code @namespace} regular expression, defaulting to all.
 */
@InterfaceAudience.Private
public final class UserPermissionCommand implements ShellCommand {
  @Override
  public String name() {
    return "user_permission";
  }

  @Override
  public String help() {
    return "user_permission ['tableOrNamespaceRegex'] - show all permissions for a user; use "
      + "'@namespace' for namespaces";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    String regex = positionals.isEmpty() ? null : String.valueOf(positionals.get(0));
    List<List<String>> rows = context.admin().userPermission(regex);
    return new TabularResult(List.of("User", "Namespace,Table,Family,Qualifier:Permission"),
      rows);
  }
}
