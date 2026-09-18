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
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/add_labels.rb}: one label, or an array of
 * labels.
 */
@InterfaceAudience.Private
public final class AddLabelsCommand implements ShellCommand {
  @Override
  public String name() {
    return "add_labels";
  }

  @Override
  public String help() {
    return "add_labels ['label1', 'label2'] - add visibility labels";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException(
        "add_labels requires a label (or array of labels) argument");
    }
    List<String> labels = new ArrayList<>();
    for (Object positional : positionals) {
      labels.addAll(toStringList(positional));
    }
    context.admin().addLabels(labels);
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
    return List.of(String.valueOf(value));
  }
}
