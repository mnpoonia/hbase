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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.replication.ReplicationPeerConfigUtil;
import org.apache.hadoop.hbase.client.replication.TableCFs;
import org.apache.hadoop.hbase.replication.ReplicationException;
import org.apache.hadoop.hbase.replication.ReplicationPeerConfig;
import org.apache.hadoop.hbase.replication.ReplicationPeerConfigBuilder;
import org.apache.hadoop.hbase.replication.ReplicationPeerDescription;
import org.apache.hadoop.hbase.replication.SyncReplicationState;
import org.apache.hadoop.hbase.util.Bytes;

/** Package-private collaborator used by {@link DefaultShellAdmin}. */
final class ReplicationAdminOps {
  private static final Set<String> ADD_PEER_OPTIONS =
    new java.util.TreeSet<>(Arrays.asList("CLUSTER_KEY", "ENDPOINT_CLASSNAME", "CONFIG", "DATA",
      "TABLE_CFS", "NAMESPACES", "STATE", "REMOTE_WAL_DIR", "SERIAL"));

  private final org.apache.hadoop.hbase.client.Admin admin;

  ReplicationAdminOps(org.apache.hadoop.hbase.client.Admin admin) {
    this.admin = admin;
  }

  void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException {
    for (String key : peerConfigSpec.keySet()) {
      if (!ADD_PEER_OPTIONS.contains(key)) {
        throw new ClientErrorException(
          "add_peer: unknown option '" + key + "'; supported: " + ADD_PEER_OPTIONS);
      }
    }
    Object clusterKey = peerConfigSpec.get("CLUSTER_KEY");
    Object endpointClassname = peerConfigSpec.get("ENDPOINT_CLASSNAME");
    if (clusterKey == null && endpointClassname == null) {
      throw new ClientErrorException("add_peer requires CLUSTER_KEY or ENDPOINT_CLASSNAME");
    }
    ReplicationPeerConfigBuilder builder = ReplicationPeerConfig.newBuilder();
    if (clusterKey != null) {
      builder.setClusterKey(String.valueOf(clusterKey));
    }
    if (endpointClassname != null) {
      builder.setReplicationEndpointImpl(String.valueOf(endpointClassname));
    }
    Object remoteWalDir = peerConfigSpec.get("REMOTE_WAL_DIR");
    if (remoteWalDir != null) {
      builder.setRemoteWALDir(String.valueOf(remoteWalDir));
    }
    Object serial = peerConfigSpec.get("SERIAL");
    if (serial != null) {
      String text = String.valueOf(serial);
      if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
        throw new ClientErrorException("SERIAL must be true or false: " + text);
      }
      builder.setSerial(Boolean.parseBoolean(text));
    }
    Object config = peerConfigSpec.get("CONFIG");
    if (config instanceof Map) {
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) config).entrySet()) {
        builder.putConfiguration(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }
    }
    Object data = peerConfigSpec.get("DATA");
    if (data instanceof Map) {
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
        builder.putPeerData(Bytes.toBytes(String.valueOf(entry.getKey())),
          Bytes.toBytes(String.valueOf(entry.getValue())));
      }
    }
    Object tableCfs = peerConfigSpec.get("TABLE_CFS");
    if (tableCfs instanceof Map) {
      builder.setTableCFsMap(toTableCfsMap((Map<?, ?>) tableCfs));
    }
    Object namespaces = peerConfigSpec.get("NAMESPACES");
    if (namespaces instanceof List) {
      builder.setNamespaces(
        ((List<?>) namespaces).stream().map(String::valueOf).collect(Collectors.toSet()));
    }
    boolean enabled = !"DISABLED".equals(peerConfigSpec.getOrDefault("STATE", "ENABLED"));
    admin.addReplicationPeer(peerId, builder.build(), enabled);
  }

  void removePeer(String peerId) throws IOException {
    admin.removeReplicationPeer(peerId);
  }

  List<PeerDescription> listPeers() throws IOException {
    List<PeerDescription> descriptions = new ArrayList<>();
    for (ReplicationPeerDescription peer : admin.listReplicationPeers()) {
      ReplicationPeerConfig config = peer.getPeerConfig();
      boolean replicateAll = config.replicateAllUserTables();
      String namespaces;
      String tableCfs;
      if (replicateAll) {
        String excludeNamespaces =
          ReplicationPeerConfigUtil.convertToString(config.getExcludeNamespaces());
        namespaces = excludeNamespaces == null ? "" : "!" + excludeNamespaces;
        String excludeTableCfs =
          ReplicationPeerConfigUtil.convertToString(config.getExcludeTableCFsMap());
        tableCfs = excludeTableCfs == null ? "" : "!" + excludeTableCfs;
      } else {
        namespaces = orEmpty(ReplicationPeerConfigUtil.convertToString(config.getNamespaces()));
        tableCfs = orEmpty(ReplicationPeerConfigUtil.convertToString(config.getTableCFsMap()));
      }
      descriptions.add(new PeerDescription(peer.getPeerId(), orNil(config.getClusterKey()),
        orNil(config.getReplicationEndpointImpl()), orNil(config.getRemoteWALDir()),
        peer.getSyncReplicationState().toString(), peer.isEnabled(), replicateAll, namespaces,
        tableCfs, config.getBandwidth(), config.isSerial()));
    }
    return descriptions;
  }

  void enablePeer(String peerId) throws IOException {
    admin.enableReplicationPeer(peerId);
  }

  void disablePeer(String peerId) throws IOException {
    admin.disableReplicationPeer(peerId);
  }

  void setPeerReplicateAll(String peerId, boolean replicateAll) throws IOException {
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setReplicateAllUserTables(replicateAll).build());
  }

  void setPeerSerial(String peerId, boolean serial) throws IOException {
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setSerial(serial).build());
  }

  void setPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc)
        .setNamespaces(namespaces == null ? Collections.emptySet() : new HashSet<>(namespaces))
        .build());
  }

  void appendPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    if (namespaces == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    Set<String> ns =
      rpc.getNamespaces() == null ? new HashSet<>() : new HashSet<>(rpc.getNamespaces());
    ns.addAll(namespaces);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setNamespaces(ns).build());
  }

  void removePeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    if (namespaces == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    if (rpc.getNamespaces() == null) {
      return;
    }
    Set<String> ns = new HashSet<>(rpc.getNamespaces());
    namespaces.forEach(ns::remove);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setNamespaces(ns).build());
  }

  void setPeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException {
    if (namespaces == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    admin.updateReplicationPeerConfig(peerId, ReplicationPeerConfig.newBuilder(rpc)
      .setExcludeNamespaces(new HashSet<>(namespaces)).build());
  }

  void appendPeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException {
    if (namespaces == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    Set<String> ns = rpc.getExcludeNamespaces() == null
      ? new HashSet<>()
      : new HashSet<>(rpc.getExcludeNamespaces());
    ns.addAll(namespaces);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setExcludeNamespaces(ns).build());
  }

  void removePeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException {
    if (namespaces == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    if (rpc.getExcludeNamespaces() == null) {
      return;
    }
    Set<String> ns = new HashSet<>(rpc.getExcludeNamespaces());
    namespaces.forEach(ns::remove);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setExcludeNamespaces(ns).build());
  }

  String showPeerTableCFs(String peerId) throws IOException {
    return orEmpty(ReplicationPeerConfigUtil
      .convertToString(admin.getReplicationPeerConfig(peerId).getTableCFsMap()));
  }

  void setPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    Map<TableName, List<String>> map =
      tableCFs == null ? Collections.emptyMap() : toTableCfsMap(tableCFs);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setTableCFsMap(map).build());
  }

  void appendPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    if (tableCFs == null) {
      return;
    }
    try {
      admin.appendReplicationPeerTableCFs(peerId, toTableCfsMap(tableCFs));
    } catch (ReplicationException e) {
      throw new IOException(e);
    }
  }

  void removePeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    if (tableCFs == null) {
      return;
    }
    try {
      admin.removeReplicationPeerTableCFs(peerId, toTableCfsMap(tableCFs));
    } catch (ReplicationException e) {
      throw new IOException(e);
    }
  }

  void setPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    if (tableCFs == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setExcludeTableCFsMap(toTableCfsMap(tableCFs)).build());
  }

  void appendPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    if (tableCFs == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    try {
      admin.updateReplicationPeerConfig(peerId, ReplicationPeerConfigUtil
        .appendExcludeTableCFsToReplicationPeerConfig(toTableCfsMap(tableCFs), rpc));
    } catch (ReplicationException e) {
      throw new IOException(e);
    }
  }

  void removePeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    if (tableCFs == null) {
      return;
    }
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    try {
      admin.updateReplicationPeerConfig(peerId, ReplicationPeerConfigUtil
        .removeExcludeTableCFsFromReplicationPeerConfig(toTableCfsMap(tableCFs), rpc, peerId));
    } catch (ReplicationException e) {
      throw new IOException(e);
    }
  }

  void setPeerBandwidth(String peerId, long bandwidth) throws IOException {
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    admin.updateReplicationPeerConfig(peerId,
      ReplicationPeerConfig.newBuilder(rpc).setBandwidth(bandwidth).build());
  }

  List<List<String>> listReplicatedTables(String regex) throws IOException {
    Pattern pattern = Pattern.compile(regex == null ? ".*" : regex);
    List<List<String>> rows = new ArrayList<>();
    for (TableCFs tableCFs : admin.listReplicatedTableCFs()) {
      if (!pattern.matcher(tableCFs.getTable().getNameAsString()).find()) {
        continue;
      }
      for (Map.Entry<String, Integer> cf : tableCFs.getColumnFamilyMap().entrySet()) {
        String type;
        int scope = cf.getValue() == null ? -1 : cf.getValue();
        // Serial replication is a peer-level flag; CF scope is only LOCAL or GLOBAL.
        if (scope == HConstants.REPLICATION_SCOPE_LOCAL) {
          type = "LOCAL";
        } else if (scope == HConstants.REPLICATION_SCOPE_GLOBAL) {
          type = "GLOBAL";
        } else {
          type = "UNKNOWN";
        }
        rows.add(Arrays.asList(tableCFs.getTable().getNameAsString() + ":" + cf.getKey(), type));
      }
    }
    return rows;
  }

  void enableTableReplication(String tableName) throws IOException {
    admin.enableTableReplication(TableName.valueOf(tableName));
  }

  void disableTableReplication(String tableName) throws IOException {
    admin.disableTableReplication(TableName.valueOf(tableName));
  }

  List<List<String>> getPeerConfigRows(String peerId) throws IOException {
    return formatPeerConfig(admin.getReplicationPeerConfig(peerId));
  }

  List<List<String>> listPeerConfigRows() throws IOException {
    List<List<String>> rows = new ArrayList<>();
    for (ReplicationPeerDescription peer : admin.listReplicationPeers()) {
      rows.add(Arrays.asList("PeerId", peer.getPeerId()));
      rows.addAll(formatPeerConfig(peer.getPeerConfig()));
      rows.add(Arrays.asList(" "));
    }
    return rows;
  }

  void updatePeerConfig(String peerId, Map<String, Object> args) throws IOException {
    ReplicationPeerConfig rpc = admin.getReplicationPeerConfig(peerId);
    ReplicationPeerConfigBuilder builder = ReplicationPeerConfig.newBuilder(rpc);
    Object config = args == null ? null : args.get("CONFIG");
    if (config instanceof Map) {
      Map<String, String> conf = new HashMap<>();
      for (Map.Entry<?, ?> e : ((Map<?, ?>) config).entrySet()) {
        conf.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
      }
      builder.putAllConfiguration(conf);
    }
    Object data = args == null ? null : args.get("DATA");
    if (data instanceof Map) {
      for (Map.Entry<?, ?> e : ((Map<?, ?>) data).entrySet()) {
        builder.putPeerData(Bytes.toBytes(String.valueOf(e.getKey())),
          Bytes.toBytes(String.valueOf(e.getValue())));
      }
    }
    admin.updateReplicationPeerConfig(peerId, builder.build());
  }

  void transitPeerSyncReplicationState(String peerId, String state) throws IOException {
    SyncReplicationState syncState;
    if ("ACTIVE".equalsIgnoreCase(state)) {
      syncState = SyncReplicationState.ACTIVE;
    } else if ("DOWNGRADE_ACTIVE".equalsIgnoreCase(state)) {
      syncState = SyncReplicationState.DOWNGRADE_ACTIVE;
    } else if ("STANDBY".equalsIgnoreCase(state)) {
      syncState = SyncReplicationState.STANDBY;
    } else {
      throw new ClientErrorException(
        "synchronous replication state must be ACTIVE, DOWNGRADE_ACTIVE or STANDBY");
    }
    admin.transitReplicationPeerSyncReplicationState(peerId, syncState);
  }

  boolean peerModificationSwitch(boolean enabled, boolean drainProcs) throws IOException {
    return admin.replicationPeerModificationSwitch(enabled, drainProcs);
  }

  boolean peerModificationEnabled() throws IOException {
    return admin.isReplicationPeerModificationEnabled();
  }

  private static List<List<String>> formatPeerConfig(ReplicationPeerConfig peerConfig) {
    List<List<String>> rows = new ArrayList<>();
    if (peerConfig.getClusterKey() != null) {
      rows.add(Arrays.asList("Cluster Key", peerConfig.getClusterKey()));
    }
    if (peerConfig.getReplicationEndpointImpl() != null) {
      rows.add(Arrays.asList("Replication Endpoint", peerConfig.getReplicationEndpointImpl()));
    }
    if (peerConfig.getConfiguration() != null) {
      for (Map.Entry<String, String> entry : peerConfig.getConfiguration().entrySet()) {
        rows.add(Arrays.asList(entry.getKey(), entry.getValue()));
      }
    }
    return rows;
  }

  private static Map<TableName, List<String>> toTableCfsMap(Map<?, ?> tableCFs) {
    Map<TableName, List<String>> map = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : tableCFs.entrySet()) {
      List<String> cfs = entry.getValue() == null
        ? Collections.emptyList()
        : ((List<?>) entry.getValue()).stream().map(String::valueOf).collect(Collectors.toList());
      map.put(TableName.valueOf(String.valueOf(entry.getKey())), cfs);
    }
    return map;
  }

  private static String orNil(String value) {
    return value == null ? "nil" : value;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
