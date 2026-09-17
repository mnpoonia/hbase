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
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/create_namespace.rb}: a namespace name plus an
 * optional dictionary of namespace configuration properties.
 */
@InterfaceAudience.Private
public final class CreateNamespaceCommand implements ShellCommand {
  @Override
  public String name() {
    return "create_namespace";
  }

  @Override
  public String help() {
    return "create_namespace 'ns1', {'PROPERTY_NAME'=>'PROPERTY_VALUE'} - create a namespace";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("create_namespace requires a namespace name argument");
    }
    String namespace = String.valueOf(command.positionalArgs().get(0));
    Map<String, Object> properties = command.hashLiterals().isEmpty() ? Map.of()
      : command.hashLiterals().get(0);
    context.admin().createNamespace(namespace, properties);
    return TextResult.of();
  }
}
