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

/**
 * {@link ReplicationPeerContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or
 * via {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubReplicationPeerContract extends ReplicationPeerContract {
  @Override
  default void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removePeer(String peerId) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<PeerDescription> listPeers() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void enablePeer(String peerId) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void disablePeer(String peerId) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerReplicateAll(String peerId, boolean replicateAll) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerSerial(String peerId, boolean serial) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void appendPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removePeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void appendPeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removePeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String showPeerTableCFs(String peerId) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void appendPeerTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removePeerTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void appendPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removePeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void setPeerBandwidth(String peerId, long bandwidth) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> listReplicatedTables(String regex) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void enableTableReplication(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void disableTableReplication(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> getPeerConfigRows(String peerId) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> listPeerConfigRows() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void updatePeerConfig(String peerId, Map<String, Object> args) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void transitPeerSyncReplicationState(String peerId, String state) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean peerModificationSwitch(boolean enabled, boolean drainProcs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean peerModificationEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
