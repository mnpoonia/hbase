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
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

@InterfaceAudience.Private
public final class AppendPeerExcludeTableCFsCommand implements ShellCommand {
  @Override
  public String name() {
    return "append_peer_exclude_tableCFs";
  }

  @Override
  public String help() {
    return "append_peer_exclude_tableCFs 'peerId', { 't1' => [] }";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "append_peer_exclude_tableCFs", 1);
    String peerId =
      ArgParsing.requireArg(command, 0, "append_peer_exclude_tableCFs requires a peer id");
    Map<String, Object> tableCFs = ArgParsing.tableCfs(command);
    if (tableCFs == null) {
      throw new ShellCommandException("append_peer_exclude_tableCFs requires a table-cfs map");
    }
    context.admin().appendPeerExcludeTableCFs(peerId, tableCFs);
    return TextResult.of();
  }

}
