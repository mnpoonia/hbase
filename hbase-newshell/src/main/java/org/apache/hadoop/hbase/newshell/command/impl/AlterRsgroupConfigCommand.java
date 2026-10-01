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
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.*;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

@InterfaceAudience.Private
public final class AlterRsgroupConfigCommand implements ShellCommand {
  @Override
  public String name() {
    return "alter_rsgroup_config";
  }

  @Override
  public String help() {
    return "alter_rsgroup_config 'grp', {METHOD=>'set', 'k'=>'v'}";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty())
      throw new ShellCommandException("alter_rsgroup_config requires a group name");
    String group = String.valueOf(command.positionalArgs().get(0));
    Map<String, Object> args = new LinkedHashMap<>();
    if (command.positionalArgs().size() > 1 && command.positionalArgs().get(1) instanceof Map) {
      for (Map.Entry<?, ?> e : ((Map<?, ?>) command.positionalArgs().get(1)).entrySet()) {
        args.put(String.valueOf(e.getKey()), e.getValue());
      }
    } else if (!command.hashLiterals().isEmpty()) {
      args.putAll(command.hashLiterals().get(0));
    } else {
      throw new ShellCommandException("alter_rsgroup_config requires a config hash");
    }
    context.admin().alterRsGroupConfig(group, args);
    return TextResult.of();
  }
}
