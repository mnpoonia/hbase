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
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/normalize.rb}: optional TABLE_NAME / TABLE_NAMES
 * / REGEX / NAMESPACE hash filters; returns whether the normalize request was submitted.
 */
@InterfaceAudience.Private
public final class NormalizeCommand implements ShellCommand {
  @Override
  public String name() {
    return "normalize";
  }

  @Override
  public String help() {
    return "normalize, normalize TABLE_NAME => 't', normalize REGEX => '.*' - trigger region "
      + "normalization, optionally filtered by table name(s), regex, or namespace";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "normalize", 0);
    Map<String, Object> filters = new LinkedHashMap<>(command.options());
    for (Map<String, Object> hash : command.hashLiterals()) {
      filters.putAll(hash);
    }
    boolean submitted = context.clusterOps().normalize(filters);
    return TextResult.of(String.valueOf(submitted));
  }
}
