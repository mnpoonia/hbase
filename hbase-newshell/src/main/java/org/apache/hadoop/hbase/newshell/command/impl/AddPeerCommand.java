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
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/add_peer.rb}: a peer id plus a
 * single args hash supporting {@code CLUSTER_KEY}, {@code ENDPOINT_CLASSNAME}, {@code STATE},
 * {@code TABLE_CFS}, and {@code NAMESPACES} (e.g.
 * {@code add_peer '1', CLUSTER_KEY => 'zk1,zk2:2181:/hbase'}). SERIAL/REMOTE_WAL_DIR/DATA/CONFIG
 * (sync replication) are explicitly not ported for this slice.
 */
@InterfaceAudience.Private
public final class AddPeerCommand implements ShellCommand {
  @Override
  public String name() {
    return "add_peer";
  }

  @Override
  public String help() {
    return "add_peer 'id', CLUSTER_KEY => '...' - add a replication peer";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException("add_peer requires a peer id argument");
    }
    String peerId = String.valueOf(command.positionalArgs().get(0));
    context.admin().addPeer(peerId, command.options());
    return TextResult.of();
  }
}
