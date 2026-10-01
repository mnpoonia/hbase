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
import java.util.List;
import java.util.Map;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Replication peer and peer-config operations. Mirrors the method set delegated internally by
 * {@link DefaultShellAdmin} to {@code ReplicationAdminOps}.
 */
@InterfaceAudience.Private
public interface ReplicationPeerContract {
  void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException;

  void removePeer(String peerId) throws IOException;

  List<PeerDescription> listPeers() throws IOException;

  void enablePeer(String peerId) throws IOException;

  void disablePeer(String peerId) throws IOException;

  void setPeerReplicateAll(String peerId, boolean replicateAll) throws IOException;

  void setPeerSerial(String peerId, boolean serial) throws IOException;

  void setPeerNamespaces(String peerId, List<String> namespaces) throws IOException;

  void appendPeerNamespaces(String peerId, List<String> namespaces) throws IOException;

  void removePeerNamespaces(String peerId, List<String> namespaces) throws IOException;

  void setPeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException;

  void appendPeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException;

  void removePeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException;

  String showPeerTableCFs(String peerId) throws IOException;

  void setPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException;

  void appendPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException;

  void removePeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException;

  void setPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException;

  void appendPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException;

  void removePeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException;

  void setPeerBandwidth(String peerId, long bandwidth) throws IOException;

  List<List<String>> listReplicatedTables(String regex) throws IOException;

  void enableTableReplication(String tableName) throws IOException;

  void disableTableReplication(String tableName) throws IOException;

  List<List<String>> getPeerConfigRows(String peerId) throws IOException;

  List<List<String>> listPeerConfigRows() throws IOException;

  void updatePeerConfig(String peerId, Map<String, Object> args) throws IOException;

  void transitPeerSyncReplicationState(String peerId, String state) throws IOException;

  boolean peerModificationSwitch(boolean enabled, boolean drainProcs) throws IOException;

  boolean peerModificationEnabled() throws IOException;
}
