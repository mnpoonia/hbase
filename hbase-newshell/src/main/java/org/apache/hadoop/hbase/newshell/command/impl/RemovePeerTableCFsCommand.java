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

@InterfaceAudience.Private
public final class RemovePeerTableCFsCommand implements ShellCommand {
  @Override public String name() { return "remove_peer_tableCFs"; }
  @Override public String help() {
    return "remove_peer_tableCFs 'peerId', { 't1' => [] }";
  }
  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("remove_peer_tableCFs requires a peer id");
    }
    String peerId = String.valueOf(command.positionalArgs().get(0));
    Map<String, Object> tableCFs = tableCfsFrom(command);
    if (tableCFs == null) {
      throw new ShellCommandException("remove_peer_tableCFs requires a table-cfs map");
    }
    context.admin().removePeerTableCFs(peerId, tableCFs);
    return TextResult.of();
  }

  private static Map<String, Object> tableCfsFrom(ParsedCommand command) throws ShellCommandException {
    if (command.positionalArgs().size() > 1) {
      Object arg = command.positionalArgs().get(1);
      if (arg instanceof Map) {
        return copyMap((Map<?, ?>) arg);
      }
      throw new ShellCommandException("table-cfs argument must be a Hash");
    }
    if (!command.hashLiterals().isEmpty()) {
      return new java.util.LinkedHashMap<>(command.hashLiterals().get(0));
    }
    return null;
  }

  private static Map<String, Object> copyMap(Map<?, ?> src) {
    Map<String, Object> out = new java.util.LinkedHashMap<>();
    for (java.util.Map.Entry<?, ?> e : src.entrySet()) {
      out.put(String.valueOf(e.getKey()), e.getValue());
    }
    return out;
  }
}
