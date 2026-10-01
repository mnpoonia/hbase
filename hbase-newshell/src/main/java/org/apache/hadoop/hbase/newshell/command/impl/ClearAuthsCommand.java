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
import java.util.Arrays;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/clear_auths.rb}.
 */
@InterfaceAudience.Private
public final class ClearAuthsCommand implements ShellCommand {
  @Override
  public String name() {
    return "clear_auths";
  }

  @Override
  public String help() {
    return "clear_auths 'user', ['label1', 'label2'] - clear visibility labels from a user or "
      + "group";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.size() < 2) {
      throw new ShellCommandException("clear_auths requires a user and at least one label");
    }
    String user = String.valueOf(positionals.get(0));
    List<String> labels = new ArrayList<>();
    for (int i = 1; i < positionals.size(); i++) {
      labels.addAll(toStringList(positionals.get(i)));
    }
    context.admin().clearAuths(user, labels);
    return TextResult.of();
  }

  @SuppressWarnings("unchecked")
  private static List<String> toStringList(Object value) {
    if (value instanceof List) {
      List<String> result = new ArrayList<>();
      for (Object element : (List<Object>) value) {
        result.add(String.valueOf(element));
      }
      return result;
    }
    return Arrays.asList(String.valueOf(value));
  }
}
