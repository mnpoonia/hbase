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
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, THROTTLE-only slice, from hbase-shell's {@code shell/commands/set_quota.rb}: a single
 * hash literal, e.g. {@code TYPE => THROTTLE, USER => 'u1', LIMIT => '10req/sec'}. {@code
 * TYPE => SPACE}, {@code SCOPE} customization and {@code GLOBAL_BYPASS} are explicitly not
 * ported.
 */
@InterfaceAudience.Private
public final class SetQuotaCommand implements ShellCommand {
  @Override
  public String name() {
    return "set_quota";
  }

  @Override
  public String help() {
    return "set_quota TYPE => THROTTLE, USER|TABLE|NAMESPACE|REGIONSERVER => '...', "
      + "LIMIT => '10req/sec' - set (or, with LIMIT => NONE, remove) a throttle quota";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    Map<String, Object> args = new LinkedHashMap<>(
      command.hashLiterals().isEmpty() ? command.options() : command.hashLiterals().get(0));
    if (args.isEmpty()) {
      throw new ShellCommandException("set_quota requires a TYPE argument");
    }
    context.admin().setQuota(args);
    return TextResult.of();
  }
}
