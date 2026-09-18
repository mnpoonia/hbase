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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.UnknownRegionException;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.CompactType;
import org.apache.hadoop.hbase.client.SnapshotDescription;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.client.replication.ReplicationPeerConfigUtil;
import org.apache.hadoop.hbase.net.Address;
import org.apache.hadoop.hbase.quotas.QuotaFilter;
import org.apache.hadoop.hbase.quotas.QuotaScope;
import org.apache.hadoop.hbase.quotas.QuotaSettings;
import org.apache.hadoop.hbase.quotas.QuotaSettingsFactory;
import org.apache.hadoop.hbase.quotas.ThrottleType;
import org.apache.hadoop.hbase.replication.ReplicationPeerConfig;
import org.apache.hadoop.hbase.replication.ReplicationPeerConfigBuilder;
import org.apache.hadoop.hbase.replication.ReplicationPeerDescription;
import org.apache.hadoop.hbase.rsgroup.RSGroupInfo;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.Permission;
import org.apache.hadoop.hbase.security.access.UserPermission;
import org.apache.hadoop.hbase.security.visibility.VisibilityClient;
import org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.ListLabelsResponse;
import org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.VisibilityLabelsResponse;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.FutureUtils;
import org.apache.hbase.thirdparty.com.google.gson.Gson;
import org.apache.hbase.thirdparty.com.google.gson.JsonArray;
import org.apache.hbase.thirdparty.com.google.gson.JsonObject;
import org.apache.hbase.thirdparty.com.google.gson.JsonParser;
import org.apache.yetus.audience.InterfaceAudience;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * Wraps a real {@link Admin}. Ported, for the pilot commands only, from hbase-shell's
 * {@code hbase/admin.rb} - {@code status} (summary branch only), {@code create} (one or more
 * column families - see {@link ColumnFamilyAttributes} for the supported per-family attributes,
 * and {@link TableAttributes} for the supported table-level attributes including SPLITS - no
 * SPLITALGO/CONFIGURATION/MOB at the table level), {@code disable}, {@code enable} (mirrors
 * {@code disable}'s exists/already-in-that-state guards), {@code drop} (requires the table be
 * disabled first, per {@code admin.rb#drop}), {@code list} (regex-filtered table names, per
 * {@code admin.rb#list}), and {@code describe} (enabled/disabled status, table attributes, and
 * column family descriptions, per {@code shell/commands/describe.rb} - the QUOTAS section is not
 * ported), {@code decommission_regionservers}/{@code recommission_regionserver}/
 * {@code list_decommissioned_regionservers} (per {@code hbase/admin.rb}'s
 * {@code getServerName}/{@code getServerNames} hostname-resolution logic).
 */
@InterfaceAudience.Private
public final class DefaultShellAdmin implements ShellAdmin {
  private final Admin admin;

  public DefaultShellAdmin(Admin admin) {
    this.admin = admin;
  }

  @Override
  public ClusterMetrics status() throws IOException {
    return admin.getClusterMetrics();
  }

  @Override
  public void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) throws IOException {
    TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName));
    for (Map<String, Object> familySpec : familySpecs) {
      tableBuilder.setColumnFamily(ColumnFamilyAttributes.build(familySpec));
    }
    byte[][] splits = TableAttributes.apply(tableBuilder, tableAttributes);
    if (splits == null) {
      admin.createTable(tableBuilder.build());
    } else {
      admin.createTable(tableBuilder.build(), splits);
    }
  }

  @Override
  public void disableTable(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    if (admin.isTableDisabled(table)) {
      throw new IOException("Table '" + tableName + "' is already disabled");
    }
    admin.disableTable(table);
  }

  @Override
  public void enableTable(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    if (admin.isTableEnabled(table)) {
      throw new IOException("Table '" + tableName + "' is already enabled");
    }
    admin.enableTable(table);
  }

  @Override
  public void dropTable(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    if (admin.isTableEnabled(table)) {
      throw new IOException("Table '" + tableName + "' is enabled. Disable it first.");
    }
    admin.deleteTable(table);
  }

  @Override
  public List<String> listTables(String regex) throws IOException {
    return Arrays.stream(admin.listTableNames(Pattern.compile(regex)))
      .map(TableName::getNameAsString).collect(Collectors.toList());
  }

  @Override
  public TableDescription describeTable(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    TableDescriptor descriptor = admin.getDescriptor(table);
    List<String> columnFamilies = Arrays.stream(descriptor.getColumnFamilies())
      .map(ColumnFamilyDescriptor::toString).collect(Collectors.toList());
    return new TableDescription(admin.isTableEnabled(table), tableAttributesString(descriptor),
      columnFamilies);
  }

  /**
   * {@code toStringTableAttributes()} is a public method, but it is defined on the private
   * {@code ModifiableTableDescriptor} class, so it needs reflection to access - same approach as
   * hbase-shell's {@code admin.rb#get_table_attributes}.
   */
  private static String tableAttributesString(TableDescriptor descriptor) {
    try {
      var method = descriptor.getClass().getMethod("toStringTableAttributes");
      method.setAccessible(true);
      return (String) method.invoke(descriptor);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void decommissionRegionServers(List<String> hostOrServers, boolean offload)
    throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    List<ServerName> servers = new ArrayList<>();
    for (String hostOrServer : hostOrServers) {
      resolveServerName(hostOrServer, liveServers)
        .ifPresent(servers::add);
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
    ServerName serverName = resolveServerName(hostOrServer, liveServers)
      .orElseThrow(() -> new IOException(
        "Could not find any server with specified name: " + hostOrServer));
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
  public void alterTable(String tableName, List<Map<String, Object>> familySpecs)
    throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    TableDescriptor existing = admin.getDescriptor(table);
    TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(existing);
    for (Map<String, Object> familySpec : familySpecs) {
      Object name = familySpec.get("NAME");
      if (name == null) {
        throw new IOException("Column family spec requires a NAME");
      }
      byte[] familyName = Bytes.toBytes(name.toString());
      ColumnFamilyDescriptor existingFamily = existing.getColumnFamily(familyName);
      if (existingFamily == null) {
        throw new IOException(
          "Column family '" + name + "' does not exist on table '" + tableName + "'");
      }
      ColumnFamilyDescriptorBuilder familyBuilder =
        ColumnFamilyDescriptorBuilder.newBuilder(existingFamily);
      ColumnFamilyAttributes.applyAttributes(familyBuilder, familySpec);
      tableBuilder.modifyColumnFamily(familyBuilder.build());
    }
    admin.modifyTable(tableBuilder.build());
  }

  @Override
  public boolean tableExists(String tableName) throws IOException {
    return admin.tableExists(TableName.valueOf(tableName));
  }

  @Override
  public void compact(String tableOrRegionName, String family, String type) throws IOException {
    CompactType compactType = parseCompactType(type);
    byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
    try {
      if (familyBytes == null) {
        admin.compactRegion(Bytes.toBytes(tableOrRegionName));
      } else {
        admin.compactRegion(Bytes.toBytes(tableOrRegionName), familyBytes);
      }
    } catch (IllegalArgumentException | UnknownRegionException e) {
      TableName table = TableName.valueOf(tableOrRegionName);
      try {
        if (familyBytes == null) {
          admin.compact(table, compactType);
        } else {
          admin.compact(table, familyBytes, compactType);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new IOException(ie);
      }
    }
  }

  @Override
  public void majorCompact(String tableOrRegionName, String family, String type)
    throws IOException {
    CompactType compactType = parseCompactType(type);
    byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
    try {
      if (familyBytes == null) {
        admin.majorCompactRegion(Bytes.toBytes(tableOrRegionName));
      } else {
        admin.majorCompactRegion(Bytes.toBytes(tableOrRegionName), familyBytes);
      }
    } catch (IllegalArgumentException | UnknownRegionException e) {
      TableName table = TableName.valueOf(tableOrRegionName);
      try {
        if (familyBytes == null) {
          admin.majorCompact(table, compactType);
        } else {
          admin.majorCompact(table, familyBytes, compactType);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new IOException(ie);
      }
    }
  }

  @Override
  public void split(String tableOrRegionName, String splitPoint) throws IOException {
    byte[] splitPointBytes = splitPoint == null ? null : Bytes.toBytes(splitPoint);
    try {
      if (splitPointBytes == null) {
        FutureUtils.get(admin.splitRegionAsync(Bytes.toBytes(tableOrRegionName)));
      } else {
        FutureUtils.get(admin.splitRegionAsync(Bytes.toBytes(tableOrRegionName), splitPointBytes));
      }
    } catch (IllegalArgumentException | UnknownRegionException e) {
      TableName table = TableName.valueOf(tableOrRegionName);
      if (splitPointBytes == null) {
        admin.split(table);
      } else {
        admin.split(table, splitPointBytes);
      }
    }
  }

  @Override
  public void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException {
    Object clusterKey = peerConfigSpec.get("CLUSTER_KEY");
    Object endpointClassname = peerConfigSpec.get("ENDPOINT_CLASSNAME");
    if (clusterKey == null && endpointClassname == null) {
      throw new IOException("add_peer requires CLUSTER_KEY or ENDPOINT_CLASSNAME");
    }
    ReplicationPeerConfigBuilder builder = ReplicationPeerConfig.newBuilder();
    if (clusterKey != null) {
      builder.setClusterKey(String.valueOf(clusterKey));
    }
    if (endpointClassname != null) {
      builder.setReplicationEndpointImpl(String.valueOf(endpointClassname));
    }
    Object tableCfs = peerConfigSpec.get("TABLE_CFS");
    if (tableCfs instanceof Map) {
      Map<TableName, List<String>> tableCfsMap = new HashMap<>();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) tableCfs).entrySet()) {
        List<String> cfs = entry.getValue() == null ? List.of()
          : ((List<?>) entry.getValue()).stream().map(String::valueOf)
            .collect(Collectors.toList());
        tableCfsMap.put(TableName.valueOf(String.valueOf(entry.getKey())), cfs);
      }
      builder.setTableCFsMap(tableCfsMap);
    }
    Object namespaces = peerConfigSpec.get("NAMESPACES");
    if (namespaces instanceof List) {
      builder.setNamespaces(((List<?>) namespaces).stream().map(String::valueOf)
        .collect(Collectors.toSet()));
    }
    boolean enabled = !"DISABLED".equals(peerConfigSpec.getOrDefault("STATE", "ENABLED"));
    admin.addReplicationPeer(peerId, builder.build(), enabled);
  }

  @Override
  public void removePeer(String peerId) throws IOException {
    admin.removeReplicationPeer(peerId);
  }

  @Override
  public List<PeerDescription> listPeers() throws IOException {
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

  private static String orNil(String value) {
    return value == null ? "nil" : value;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  @Override
  public void snapshot(String tableName, String snapshotName) throws IOException {
    admin.snapshot(snapshotName, TableName.valueOf(tableName));
  }

  @Override
  public void deleteSnapshot(String snapshotName) throws IOException {
    admin.deleteSnapshot(snapshotName);
  }

  @Override
  public List<SnapshotInfo> listSnapshots(String regex) throws IOException {
    List<SnapshotInfo> snapshots = new ArrayList<>();
    for (SnapshotDescription snapshot : admin.listSnapshots(Pattern.compile(regex))) {
      snapshots.add(new SnapshotInfo(snapshot.getName(), snapshot.getTableNameAsString(),
        snapshot.getCreationTime(), snapshot.getTtl()));
    }
    return snapshots;
  }

  @Override
  public boolean balancerSwitch(boolean enabled) throws IOException {
    return admin.balancerSwitch(enabled, false);
  }

  @Override
  public boolean normalizerSwitch(boolean enabled) throws IOException {
    return admin.normalizerSwitch(enabled);
  }

  @Override
  public boolean catalogJanitorSwitch(boolean enabled) throws IOException {
    return admin.catalogJanitorSwitch(enabled);
  }

  @Override
  public Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames)
    throws IOException {
    Map<String, Boolean> previousStates = new LinkedHashMap<>();
    for (Map.Entry<ServerName, Boolean> entry : admin.compactionSwitch(enabled, serverNames)
      .entrySet()) {
      previousStates.put(entry.getKey().getServerName(), entry.getValue());
    }
    return previousStates;
  }

  @Override
  public boolean splitOrMergeSwitch(String switchType, boolean enabled) throws IOException {
    if ("SPLIT".equals(switchType)) {
      return admin.splitSwitch(enabled, false);
    } else if ("MERGE".equals(switchType)) {
      return admin.mergeSwitch(enabled, false);
    }
    throw new IOException("only SPLIT or MERGE accepted for type!");
  }

  @Override
  public boolean balancerEnabled() throws IOException {
    return admin.isBalancerEnabled();
  }

  @Override
  public boolean normalizerEnabled() throws IOException {
    return admin.isNormalizerEnabled();
  }

  @Override
  public boolean catalogJanitorEnabled() throws IOException {
    return admin.isCatalogJanitorEnabled();
  }

  @Override
  public boolean splitOrMergeEnabled(String switchType) throws IOException {
    if ("SPLIT".equals(switchType)) {
      return admin.isSplitEnabled();
    } else if ("MERGE".equals(switchType)) {
      return admin.isMergeEnabled();
    }
    throw new IOException("only SPLIT or MERGE accepted for type!");
  }

  private static CompactType parseCompactType(String type) throws IOException {
    if (type == null || type.equalsIgnoreCase("NORMAL")) {
      return CompactType.NORMAL;
    }
    if (type.equalsIgnoreCase("MOB")) {
      return CompactType.MOB;
    }
    throw new IOException("only NORMAL or MOB accepted for type!");
  }

  /**
   * Ports {@code hbase/admin.rb#getServerName}: a full {@code host,port,starttime}-form
   * {@link ServerName} string resolves directly; otherwise the given hostname (optionally with a
   * {@code host,port} pair) is matched against the live servers' hostname/port.
   */
  private static Optional<ServerName> resolveServerName(String hostOrServer,
    Collection<ServerName> liveServers) {
    if (ServerName.isFullServerName(hostOrServer)) {
      return Optional.of(ServerName.valueOf(hostOrServer));
    }
    String[] parts = hostOrServer.split(",");
    return liveServers.stream()
      .filter(sn -> parts[0].equals(sn.getHostname())
        && (parts.length < 2 || parts[1].equals(String.valueOf(sn.getPort()))))
      .findFirst();
  }

  @Override
  public RsGroupView getRsGroup(String groupName) throws IOException {
    RSGroupInfo groupInfo = admin.getRSGroup(groupName);
    if (groupInfo == null) {
      throw new IOException("RSGroup '" + groupName + "' does not exist");
    }
    List<String> servers =
      groupInfo.getServers().stream().map(Address::toString).collect(Collectors.toList());
    List<String> tables =
      groupInfo.getTables().stream().map(TableName::getNameAsString).collect(Collectors.toList());
    return new RsGroupView(servers, tables);
  }

  @Override
  public void moveServersToRsGroup(List<String> hostPorts, String groupName) throws IOException {
    Set<Address> addresses =
      hostPorts.stream().map(Address::fromString).collect(Collectors.toSet());
    admin.moveServersToRSGroup(addresses, groupName);
  }

  @Override
  public void grant(String userOrGroup, String actions, String tableName, String family,
    String qualifier, String namespace) throws IOException {
    Permission.Action[] permActions = parseActions(actions);
    try {
      if (namespace != null) {
        AccessControlClient.grant(admin.getConnection(), namespace, userOrGroup, permActions);
      } else if (tableName != null) {
        byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
        byte[] qualifierBytes = qualifier == null ? null : Bytes.toBytes(qualifier);
        AccessControlClient.grant(admin.getConnection(), TableName.valueOf(tableName),
          userOrGroup, familyBytes, qualifierBytes, permActions);
      } else {
        AccessControlClient.grant(admin.getConnection(), userOrGroup, permActions);
      }
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
  }

  private static Permission.Action[] parseActions(String actions) throws IOException {
    Permission.Action[] result = new Permission.Action[actions.length()];
    for (int i = 0; i < actions.length(); i++) {
      result[i] = charToAction(actions.charAt(i));
    }
    return result;
  }

  @Override
  public void truncateTable(String tableName, boolean preserveSplits) throws IOException {
    TableName name = TableName.valueOf(tableName);
    if (admin.isTableEnabled(name)) {
      admin.disableTable(name);
    }
    admin.truncateTable(name, preserveSplits);
  }

  @Override
  public boolean isTableDisabled(String tableName) throws IOException {
    return admin.isTableDisabled(TableName.valueOf(tableName));
  }

  @Override
  public boolean isTableEnabled(String tableName) throws IOException {
    return admin.isTableEnabled(TableName.valueOf(tableName));
  }

  @Override
  public List<String> listTablesByState(boolean enabled) throws IOException {
    List<String> result = new ArrayList<>();
    for (TableName table : admin.listTableNames()) {
      if (admin.isTableEnabled(table) == enabled) {
        result.add(table.getNameAsString());
      }
    }
    return result;
  }

  @Override
  public AlterStatusView alterStatus(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    var regionStatus = admin.getClusterMetrics().getTableRegionStatesCount().get(table);
    if (regionStatus == null || regionStatus.getTotalRegions() == 0) {
      return new AlterStatusView(0, 0);
    }
    int updated = regionStatus.getTotalRegions() - regionStatus.getRegionsInTransition()
      - regionStatus.getClosedRegions();
    return new AlterStatusView(regionStatus.getTotalRegions() - updated,
      regionStatus.getTotalRegions());
  }

  @Override
  public void cloneTableSchema(String tableName, String newTableName, boolean preserveSplits)
    throws IOException {
    admin.cloneTableSchema(TableName.valueOf(tableName), TableName.valueOf(newTableName),
      preserveSplits);
  }

  @Override
  public RegionLocationView locateRegion(String tableName, String rowKey) throws IOException {
    var location = admin.getConnection().getRegionLocator(TableName.valueOf(tableName))
      .getRegionLocation(Bytes.toBytes(rowKey));
    return new RegionLocationView(location.getHostnamePort(),
      location.getRegion().getRegionNameAsString());
  }

  @Override
  public List<List<String>> listRegions(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.isTableEnabled(table)) {
      throw new IOException("Table " + tableName + " must be enabled.");
    }
    ClusterMetrics clusterMetrics = admin.getClusterMetrics();
    List<List<String>> rows = new ArrayList<>();
    for (var location : admin.getConnection().getRegionLocator(table).getAllRegionLocations()) {
      var regionInfo = location.getRegion();
      ServerName serverName = location.getServerName();
      var serverMetrics = clusterMetrics.getLiveServerMetrics().get(serverName);
      var regionMetrics =
        serverMetrics == null ? null : serverMetrics.getRegionMetrics().get(regionInfo.getRegionName());
      String size = regionMetrics == null ? "" : String.valueOf(regionMetrics.getStoreFileSize());
      String req = regionMetrics == null ? "" : String.valueOf(regionMetrics.getRequestCount());
      String locality = regionMetrics == null ? "" : String.valueOf(regionMetrics.getDataLocality());
      rows.add(List.of(serverName == null ? "" : serverName.toString(),
        regionInfo.getRegionNameAsString(), Bytes.toStringBinary(regionInfo.getStartKey()),
        Bytes.toStringBinary(regionInfo.getEndKey()), size, req, locality));
    }
    return rows;
  }

  @Override
  public void createNamespace(String namespace, Map<String, Object> properties)
    throws IOException {
    var builder = org.apache.hadoop.hbase.NamespaceDescriptor.create(namespace);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      builder.addConfiguration(entry.getKey(), String.valueOf(entry.getValue()));
    }
    admin.createNamespace(builder.build());
  }

  @Override
  public void dropNamespace(String namespace) throws IOException {
    admin.deleteNamespace(namespace);
  }

  @Override
  public void alterNamespace(String namespace, Map<String, Object> properties)
    throws IOException {
    var existing = admin.getNamespaceDescriptor(namespace);
    var builder = org.apache.hadoop.hbase.NamespaceDescriptor.create(existing);
    String method = String.valueOf(properties.get("METHOD"));
    if ("unset".equalsIgnoreCase(method)) {
      Object name = properties.get("NAME");
      if (name == null) {
        throw new IOException("alter_namespace unset requires NAME");
      }
      builder.removeConfiguration(String.valueOf(name));
    } else {
      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        if ("METHOD".equals(entry.getKey())) {
          continue;
        }
        builder.addConfiguration(entry.getKey(), String.valueOf(entry.getValue()));
      }
    }
    admin.modifyNamespace(builder.build());
  }

  @Override
  public String describeNamespace(String namespace) throws IOException {
    return admin.getNamespaceDescriptor(namespace).toString();
  }

  @Override
  public List<String> listNamespaces(String regex) throws IOException {
    Pattern pattern = Pattern.compile(regex);
    List<String> result = new ArrayList<>();
    for (var descriptor : admin.listNamespaceDescriptors()) {
      if (pattern.matcher(descriptor.getName()).matches()) {
        result.add(descriptor.getName());
      }
    }
    return result;
  }

  @Override
  public List<String> listNamespaceTables(String namespace) throws IOException {
    return Arrays.stream(admin.listTableNamesByNamespace(namespace))
      .map(TableName::getQualifierAsString).collect(Collectors.toList());
  }

  @Override
  public void flush(String tableOrRegionOrServerName, String family) throws IOException {
    byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
    try {
      if (familyBytes == null) {
        admin.flushRegion(Bytes.toBytes(tableOrRegionOrServerName));
      } else {
        admin.flushRegion(Bytes.toBytes(tableOrRegionOrServerName), familyBytes);
      }
    } catch (IllegalArgumentException | UnknownRegionException e) {
      try {
        TableName table = TableName.valueOf(tableOrRegionOrServerName);
        if (familyBytes == null) {
          admin.flush(table);
        } else {
          admin.flush(table, familyBytes);
        }
      } catch (IllegalArgumentException iae) {
        admin.flushRegionServer(ServerName.valueOf(tableOrRegionOrServerName));
      }
    }
  }

  @Override
  public void assign(String regionName) throws IOException {
    admin.assign(Bytes.toBytes(regionName));
  }

  @Override
  public void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl,
    String cloneSft) throws IOException {
    admin.cloneSnapshot(snapshotName, TableName.valueOf(tableName), restoreAcl, cloneSft);
  }

  @Override
  public void restoreSnapshot(String snapshotName, boolean restoreAcl) throws IOException {
    admin.restoreSnapshot(snapshotName, false, restoreAcl);
  }

  @Override
  public void enablePeer(String peerId) throws IOException {
    admin.enableReplicationPeer(peerId);
  }

  @Override
  public void disablePeer(String peerId) throws IOException {
    admin.disableReplicationPeer(peerId);
  }

  @Override
  public void updateConfig(String serverName) throws IOException {
    admin.updateConfiguration(ServerName.valueOf(serverName));
  }

  @Override
  public void updateAllConfig() throws IOException {
    admin.updateConfiguration();
  }

  @Override
  public void setQuota(Map<String, Object> args) throws IOException {
    Map<String, Object> spec = new LinkedHashMap<>(args);
    Object type = spec.remove("TYPE");
    if (!"THROTTLE".equals(type)) {
      throw new IOException("Only TYPE => THROTTLE is supported by this newshell port; "
        + "SPACE quotas and GLOBAL_BYPASS are not yet ported");
    }
    Object limit = spec.remove("LIMIT");
    QuotaSettings settings;
    if ("NONE".equals(limit)) {
      settings = buildUnthrottle(spec);
    } else {
      if (limit == null) {
        throw new IOException("set_quota requires a LIMIT");
      }
      settings = buildThrottle(spec, String.valueOf(limit));
    }
    admin.setQuota(settings);
  }

  private static QuotaSettings buildThrottle(Map<String, Object> spec, String limitSpec)
    throws IOException {
    String throttleTypeName = String.valueOf(spec.remove("THROTTLE_TYPE"));
    Object[] parsed = parseThrottleLimit(limitSpec, "null".equals(throttleTypeName) ? "REQUEST"
      : throttleTypeName);
    ThrottleType throttleType = (ThrottleType) parsed[0];
    long limit = (Long) parsed[1];
    TimeUnit timeUnit = (TimeUnit) parsed[2];
    QuotaScope scope = QuotaScope.valueOf(String.valueOf(spec.getOrDefault("SCOPE", "MACHINE")));
    if (spec.containsKey("USER")) {
      String user = String.valueOf(spec.get("USER"));
      if (spec.containsKey("TABLE")) {
        return QuotaSettingsFactory.throttleUser(user, TableName.valueOf(String.valueOf(spec.get("TABLE"))),
          throttleType, limit, timeUnit, scope);
      } else if (spec.containsKey("NAMESPACE")) {
        return QuotaSettingsFactory.throttleUser(user, String.valueOf(spec.get("NAMESPACE")),
          throttleType, limit, timeUnit, scope);
      }
      return QuotaSettingsFactory.throttleUser(user, throttleType, limit, timeUnit, scope);
    } else if (spec.containsKey("TABLE")) {
      return QuotaSettingsFactory.throttleTable(TableName.valueOf(String.valueOf(spec.get("TABLE"))),
        throttleType, limit, timeUnit, scope);
    } else if (spec.containsKey("NAMESPACE")) {
      return QuotaSettingsFactory.throttleNamespace(String.valueOf(spec.get("NAMESPACE")),
        throttleType, limit, timeUnit, scope);
    } else if (spec.containsKey("REGIONSERVER")) {
      if (scope == QuotaScope.CLUSTER) {
        throw new IOException("Invalid region server throttle scope, must be MACHINE");
      }
      return QuotaSettingsFactory.throttleRegionServer("all", throttleType, limit, timeUnit);
    }
    throw new IOException("One of USER, TABLE, NAMESPACE or REGIONSERVER must be specified");
  }

  private static QuotaSettings buildUnthrottle(Map<String, Object> spec) throws IOException {
    if (spec.containsKey("USER")) {
      String user = String.valueOf(spec.get("USER"));
      if (spec.containsKey("TABLE")) {
        return QuotaSettingsFactory.unthrottleUser(user,
          TableName.valueOf(String.valueOf(spec.get("TABLE"))));
      } else if (spec.containsKey("NAMESPACE")) {
        return QuotaSettingsFactory.unthrottleUser(user, String.valueOf(spec.get("NAMESPACE")));
      }
      return QuotaSettingsFactory.unthrottleUser(user);
    } else if (spec.containsKey("TABLE")) {
      return QuotaSettingsFactory
        .unthrottleTable(TableName.valueOf(String.valueOf(spec.get("TABLE"))));
    } else if (spec.containsKey("NAMESPACE")) {
      return QuotaSettingsFactory.unthrottleNamespace(String.valueOf(spec.get("NAMESPACE")));
    } else if (spec.containsKey("REGIONSERVER")) {
      return QuotaSettingsFactory.unthrottleRegionServer("all");
    }
    throw new IOException("One of USER, TABLE, NAMESPACE or REGIONSERVER must be specified");
  }

  private static final java.util.regex.Pattern LIMIT_PATTERN =
    java.util.regex.Pattern.compile("^(\\d+)(req|cu|[bkmgtp])/(sec|min|hour|day)$");

  /**
   * Ports {@code hbase/quotas.rb#_parse_limit}'s request/data-size/capacity-unit LIMIT syntax
   * only; the raw-byte-size SPACE-quota syntax is not needed here since SPACE quotas are not
   * ported.
   */
  private static Object[] parseThrottleLimit(String limitSpec, String throttleTypePrefix)
    throws IOException {
    Matcher matcher = LIMIT_PATTERN.matcher(limitSpec.toLowerCase(java.util.Locale.ROOT));
    if (!matcher.matches()) {
      throw new IOException("Invalid limit syntax: " + limitSpec);
    }
    long limit = Long.parseLong(matcher.group(1));
    String unit = matcher.group(2);
    ThrottleType type;
    if ("req".equals(unit)) {
      type = ThrottleType.valueOf(throttleTypePrefix + "_NUMBER");
    } else if ("cu".equals(unit)) {
      type = ThrottleType.valueOf(throttleTypePrefix + "_CAPACITY_UNIT");
      limit = limit; // capacity units are unit-less counts
    } else {
      type = ThrottleType.valueOf(throttleTypePrefix + "_SIZE");
      limit = sizeFromUnit(limit, unit);
    }
    TimeUnit timeUnit;
    switch (matcher.group(3)) {
      case "sec":
        timeUnit = TimeUnit.SECONDS;
        break;
      case "min":
        timeUnit = TimeUnit.MINUTES;
        break;
      case "hour":
        timeUnit = TimeUnit.HOURS;
        break;
      case "day":
        timeUnit = TimeUnit.DAYS;
        break;
      default:
        throw new IOException("Invalid time unit in limit: " + limitSpec);
    }
    return new Object[] { type, limit, timeUnit };
  }

  private static long sizeFromUnit(long value, String unit) throws IOException {
    switch (unit) {
      case "b":
        return value;
      case "k":
        return value * 1024L;
      case "m":
        return value * 1024L * 1024L;
      case "g":
        return value * 1024L * 1024L * 1024L;
      case "t":
        return value * 1024L * 1024L * 1024L * 1024L;
      case "p":
        return value * 1024L * 1024L * 1024L * 1024L * 1024L;
      default:
        throw new IOException("Invalid size unit: " + unit);
    }
  }

  @Override
  public List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException {
    QuotaFilter filter = new QuotaFilter();
    if (filterArgs.containsKey("USER")) {
      filter.setUserFilter(String.valueOf(filterArgs.get("USER")));
    }
    if (filterArgs.containsKey("TABLE")) {
      filter.setTableFilter(String.valueOf(filterArgs.get("TABLE")));
    }
    if (filterArgs.containsKey("NAMESPACE")) {
      filter.setNamespaceFilter(String.valueOf(filterArgs.get("NAMESPACE")));
    }
    List<List<String>> rows = new ArrayList<>();
    for (QuotaSettings settings : admin.getQuota(filter)) {
      Map<String, String> owner = new LinkedHashMap<>();
      if (settings.getUserName() != null) {
        owner.put("USER", settings.getUserName());
      }
      if (settings.getTableName() != null) {
        owner.put("TABLE", settings.getTableName().getNameAsString());
      }
      if (settings.getNamespace() != null) {
        owner.put("NAMESPACE", settings.getNamespace());
      }
      if (settings.getRegionServer() != null) {
        owner.put("REGIONSERVER", settings.getRegionServer());
      }
      String ownerString = owner.entrySet().stream()
        .map(entry -> entry.getKey() + " => " + entry.getValue())
        .collect(Collectors.joining(", "));
      rows.add(List.of(ownerString, settings.toString()));
    }
    return rows;
  }

  @Override
  public void revoke(String userOrGroup, String tableName, String family, String qualifier,
    String namespace) throws IOException {
    try {
      if (namespace != null) {
        AccessControlClient.revoke(admin.getConnection(), namespace, userOrGroup);
      } else if (tableName != null) {
        byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
        byte[] qualifierBytes = qualifier == null ? null : Bytes.toBytes(qualifier);
        AccessControlClient.revoke(admin.getConnection(), TableName.valueOf(tableName),
          userOrGroup, familyBytes, qualifierBytes);
      } else {
        AccessControlClient.revoke(admin.getConnection(), userOrGroup, new Permission.Action[0]);
      }
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
  }

  @Override
  public List<List<String>> userPermission(String tableOrNamespaceRegex) throws IOException {
    try {
      List<UserPermission> permissions =
        AccessControlClient.getUserPermissions(admin.getConnection(), tableOrNamespaceRegex);
      List<List<String>> rows = new ArrayList<>();
      for (UserPermission permission : permissions) {
        rows.add(List.of(permission.getUser(), permission.getPermission().toString()));
      }
      return rows;
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
  }

  private static final Gson GSON = new Gson();

  @Override
  public List<List<String>> listProcedures() throws IOException {
    JsonArray procedures = JsonParser.parseString(admin.getProcedures()).getAsJsonArray();
    List<List<String>> rows = new ArrayList<>();
    for (var element : procedures) {
      JsonObject proc = element.getAsJsonObject();
      rows.add(List.of(getAsString(proc, "procId"), getAsString(proc, "className"),
        getAsString(proc, "state"), getAsString(proc, "submittedTime"),
        getAsString(proc, "lastUpdate"), getAsString(proc, "stateMessage")));
    }
    return rows;
  }

  private static String getAsString(JsonObject object, String member) {
    if (!object.has(member) || object.get(member).isJsonNull()) {
      return "";
    }
    var element = object.get(member);
    return element.isJsonPrimitive() ? element.getAsString() : element.toString();
  }

  @Override
  public List<String> listLocks() throws IOException {
    JsonArray locks = JsonParser.parseString(admin.getLocks()).getAsJsonArray();
    List<String> lines = new ArrayList<>();
    for (var element : locks) {
      JsonObject lock = element.getAsJsonObject();
      lines.add(getAsString(lock, "resourceType") + "(" + getAsString(lock, "resourceName") + ")");
      String lockType = getAsString(lock, "lockType");
      if ("EXCLUSIVE".equals(lockType)) {
        lines.add("Lock type: " + lockType + ", procedure: "
          + getAsString(lock, "exclusiveLockOwnerProcedure"));
      } else if ("SHARED".equals(lockType)) {
        lines.add(
          "Lock type: " + lockType + ", count: " + getAsString(lock, "sharedLockCount"));
      }
      if (lock.has("waitingProcedures") && lock.get("waitingProcedures").isJsonArray()) {
        for (var waiting : lock.getAsJsonArray("waitingProcedures")) {
          lines.add("    " + waiting.getAsString());
        }
      }
      lines.add("");
    }
    return lines;
  }

  @Override
  public void addLabels(List<String> labels) throws IOException {
    VisibilityLabelsResponse response;
    try {
      response = VisibilityClient.addLabels(admin.getConnection(), labels.toArray(new String[0]));
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    if (response == null) {
      throw new IOException("DISABLED: Visibility labels feature is not available");
    }
    StringBuilder failures = new StringBuilder();
    for (var result : response.getResultList()) {
      if (result.hasException()) {
        failures.append(result.getException().getValue().toStringUtf8());
      }
    }
    if (failures.length() > 0) {
      throw new IOException(failures.toString());
    }
  }

  @Override
  public List<String> listLabels(String regex) throws IOException {
    ListLabelsResponse response;
    try {
      response = VisibilityClient.listLabels(admin.getConnection(), regex);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    if (response == null) {
      throw new IOException("DISABLED: Visibility labels feature is not available");
    }
    List<String> labels = new ArrayList<>();
    for (var label : response.getLabelList()) {
      labels.add(Bytes.toStringBinary(label.toByteArray()));
    }
    return labels;
  }

  @Override
  public List<RsGroupSummary> listRsGroups(String regex) throws IOException {
    Pattern pattern = Pattern.compile(regex);
    List<RsGroupSummary> result = new ArrayList<>();
    for (RSGroupInfo group : admin.listRSGroups()) {
      if (!pattern.matcher(group.getName()).matches()) {
        continue;
      }
      List<String> servers =
        group.getServers().stream().map(Address::toString).collect(Collectors.toList());
      List<String> tables =
        group.getTables().stream().map(TableName::getNameAsString).collect(Collectors.toList());
      result.add(new RsGroupSummary(group.getName(), servers, tables));
    }
    return result;
  }

  @Override
  public void addRsGroup(String groupName) throws IOException {
    admin.addRSGroup(groupName);
  }

  @Override
  public void changeSft(String tableName, String family, String sft) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (family == null) {
      admin.modifyTableStoreFileTracker(table, sft);
    } else {
      admin.modifyColumnFamilyStoreFileTracker(table, Bytes.toBytes(family), sft);
    }
  }

  @Override
  public void changeSftAll(String tableRegex, String sft) throws IOException {
    for (TableName table : admin.listTableNames(Pattern.compile(tableRegex))) {
      admin.modifyTableStoreFileTracker(table, sft);
    }
  }

  private static Permission.Action charToAction(char c) throws IOException {
    switch (Character.toUpperCase(c)) {
      case 'R':
        return Permission.Action.READ;
      case 'W':
        return Permission.Action.WRITE;
      case 'X':
        return Permission.Action.EXEC;
      case 'C':
        return Permission.Action.CREATE;
      case 'A':
        return Permission.Action.ADMIN;
      default:
        throw new IOException("Unknown permission action: " + c);
    }
  }
}
