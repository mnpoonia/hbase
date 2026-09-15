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
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.hbase.PeerDescription;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/list_peers.rb}: no arguments,
 * one row per configured peer with PEER_ID/CLUSTER_KEY/ENDPOINT_CLASSNAME/STATE/TABLE_CFS/
 * NAMESPACES columns. REMOTE_ROOT_DIR/SYNC_REPLICATION_STATE/REPLICATE_ALL/BANDWIDTH/SERIAL are
 * explicitly not ported for this slice.
 */
@InterfaceAudience.Private
public final class ListPeersCommand implements ShellCommand {
  @Override
  public String name() {
    return "list_peers";
  }

  @Override
  public String help() {
    return "list_peers - list all replication peers";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<PeerDescription> peers = context.admin().listPeers();
    List<List<String>> rows = new ArrayList<>();
    for (PeerDescription peer : peers) {
      rows.add(List.of(peer.peerId(), String.valueOf(peer.clusterKey()),
        String.valueOf(peer.endpointClassname()), peer.enabled() ? "ENABLED" : "DISABLED",
        peer.tableCfs(), peer.namespaces()));
    }
    return new TabularResult(
      List.of("PEER_ID", "CLUSTER_KEY", "ENDPOINT_CLASSNAME", "STATE", "TABLE_CFS", "NAMESPACES"),
      rows);
  }
}
