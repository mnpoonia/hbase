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

import java.util.Collection;
import java.util.Optional;
import org.apache.hadoop.hbase.ServerName;

/** Resolves shell server arguments against the live server list. */
final class ServerNames {
  private ServerNames() {
  }

  /**
   * Ports {@code hbase/admin.rb#getServerName}: a full {@code host,port,starttime}-form
   * {@link ServerName} string resolves directly; otherwise the given hostname (optionally with a
   * {@code host,port} pair) is matched against the live servers' hostname/port.
   */
  static Optional<ServerName> resolve(String hostOrServer, Collection<ServerName> liveServers) {
    if (ServerName.isFullServerName(hostOrServer)) {
      return Optional.of(ServerName.valueOf(hostOrServer));
    }
    String[] parts = hostOrServer.split(",");
    return liveServers.stream().filter(sn -> parts[0].equals(sn.getHostname())
      && (parts.length < 2 || parts[1].equals(String.valueOf(sn.getPort())))).findFirst();
  }
}
