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

import java.util.Objects;
import org.apache.yetus.audience.InterfaceAudience;

@InterfaceAudience.Private
public final class PeerDescription {
  private final String peerId;
  private final String clusterKey;
  private final String endpointClassname;
  private final String remoteRootDir;
  private final String syncReplicationState;
  private final boolean enabled;
  private final boolean replicateAllUserTables;
  private final String namespaces;
  private final String tableCfs;
  private final long bandwidth;
  private final boolean serial;

  public PeerDescription(String peerId, String clusterKey, String endpointClassname,
    String remoteRootDir, String syncReplicationState, boolean enabled,
    boolean replicateAllUserTables, String namespaces, String tableCfs, long bandwidth,
    boolean serial) {
    this.peerId = peerId;
    this.clusterKey = clusterKey;
    this.endpointClassname = endpointClassname;
    this.remoteRootDir = remoteRootDir;
    this.syncReplicationState = syncReplicationState;
    this.enabled = enabled;
    this.replicateAllUserTables = replicateAllUserTables;
    this.namespaces = namespaces;
    this.tableCfs = tableCfs;
    this.bandwidth = bandwidth;
    this.serial = serial;
  }

  public String peerId() {
    return peerId;
  }

  public String clusterKey() {
    return clusterKey;
  }

  public String endpointClassname() {
    return endpointClassname;
  }

  public String remoteRootDir() {
    return remoteRootDir;
  }

  public String syncReplicationState() {
    return syncReplicationState;
  }

  public boolean enabled() {
    return enabled;
  }

  public boolean replicateAllUserTables() {
    return replicateAllUserTables;
  }

  public String namespaces() {
    return namespaces;
  }

  public String tableCfs() {
    return tableCfs;
  }

  public long bandwidth() {
    return bandwidth;
  }

  public boolean serial() {
    return serial;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PeerDescription)) {
      return false;
    }
    PeerDescription other = (PeerDescription) o;
    return enabled == other.enabled && replicateAllUserTables == other.replicateAllUserTables
      && bandwidth == other.bandwidth && serial == other.serial
      && Objects.equals(peerId, other.peerId) && Objects.equals(clusterKey, other.clusterKey)
      && Objects.equals(endpointClassname, other.endpointClassname)
      && Objects.equals(remoteRootDir, other.remoteRootDir)
      && Objects.equals(syncReplicationState, other.syncReplicationState)
      && Objects.equals(namespaces, other.namespaces) && Objects.equals(tableCfs, other.tableCfs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(peerId, clusterKey, endpointClassname, remoteRootDir, syncReplicationState,
      enabled, replicateAllUserTables, namespaces, tableCfs, bandwidth, serial);
  }

  @Override
  public String toString() {
    return "PeerDescription[peerId=" + peerId + ", clusterKey=" + clusterKey
      + ", endpointClassname=" + endpointClassname + ", remoteRootDir=" + remoteRootDir
      + ", syncReplicationState=" + syncReplicationState + ", enabled=" + enabled
      + ", replicateAllUserTables=" + replicateAllUserTables + ", namespaces=" + namespaces
      + ", tableCfs=" + tableCfs + ", bandwidth=" + bandwidth + ", serial=" + serial + "]";
  }
}
