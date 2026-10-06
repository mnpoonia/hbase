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
package org.apache.hadoop.hbase.newshell.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link ServerLifecycleContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class ServerLifecycleOps implements ServerLifecycleContract {
  private final Admin admin;

  ServerLifecycleOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public void decommissionRegionServers(List<String> hostOrServers, boolean offload)
    throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    List<ServerName> servers = new ArrayList<>();
    for (String hostOrServer : hostOrServers) {
      ServerNames.resolve(hostOrServer, liveServers).ifPresent(servers::add);
    }
    if (servers.isEmpty()) {
      throw new IOException(
        "Could not find any server(s) with specified name(s): " + hostOrServers);
    }
    admin.decommissionRegionServers(servers, offload);
  }

  @Override
  public void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames)
    throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    ServerName serverName = ServerNames.resolve(hostOrServer, liveServers).orElseThrow(
      () -> new IOException("Could not find any server with specified name: " + hostOrServer));
    List<byte[]> regionNameBytes =
      encodedRegionNames.stream().map(Bytes::toBytes).collect(Collectors.toList());
    admin.recommissionRegionServer(serverName, regionNameBytes);
  }

  @Override
  public List<String> listDecommissionedRegionServers() throws IOException {
    return admin.listDecommissionedRegionServers().stream().map(ServerName::getServerName)
      .collect(Collectors.toList());
  }

  @Override
  public void stopMaster() throws IOException {
    admin.stopMaster();
  }

  @Override
  public void stopRegionServer(String hostPort) throws IOException {
    admin.stopRegionServer(hostPort);
  }

  @Override
  public List<String> listDeadServers() throws IOException {
    return admin.listDeadServers().stream().map(ServerName::getServerName)
      .collect(Collectors.toList());
  }

  @Override
  public List<String> listLiveServers() throws IOException {
    return admin.getClusterMetrics().getLiveServerMetrics().keySet().stream()
      .map(ServerName::getServerName).collect(Collectors.toList());
  }

  @Override
  public List<String> listUnknownServers() throws IOException {
    return admin.listUnknownServers().stream().map(ServerName::getServerName)
      .collect(Collectors.toList());
  }

  @Override
  public List<String> clearDeadServers(List<String> serverNames) throws IOException {
    List<ServerName> servers;
    if (serverNames == null || serverNames.isEmpty()) {
      servers = admin.listDeadServers();
    } else {
      servers = new ArrayList<>();
      for (String serverName : serverNames) {
        servers.add(ServerName.valueOf(serverName));
      }
    }
    return admin.clearDeadServers(servers).stream().map(ServerName::getServerName)
      .collect(Collectors.toList());
  }
}
