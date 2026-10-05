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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.MetaTableAccessor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.RegionMetrics;
import org.apache.hadoop.hbase.ServerMetrics;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.UnknownRegionException;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BalanceRequest;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.CompactType;
import org.apache.hadoop.hbase.client.Hbck;
import org.apache.hadoop.hbase.client.LogEntry;
import org.apache.hadoop.hbase.client.NormalizeTableFilterParams;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.client.RegionStatesCount;
import org.apache.hadoop.hbase.client.ServerType;
import org.apache.hadoop.hbase.client.SnapshotDescription;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.master.RegionState;
import org.apache.hadoop.hbase.net.Address;
import org.apache.hadoop.hbase.rsgroup.RSGroupInfo;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.Permission;
import org.apache.hadoop.hbase.security.access.UserPermission;
import org.apache.hadoop.hbase.security.visibility.VisibilityClient;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.FutureUtils;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.hadoop.hbase.zookeeper.ZKDump;
import org.apache.hadoop.hbase.zookeeper.ZKWatcher;
import org.apache.yetus.audience.InterfaceAudience;

import org.apache.hbase.thirdparty.com.google.protobuf.ByteString;

import org.apache.hadoop.hbase.shaded.protobuf.generated.ClientProtos.RegionActionResult;
import org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.ListLabelsResponse;
import org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.VisibilityLabelsResponse;

/**
 * Wraps a real {@link Admin}. The full {@link ShellAdmin} implementation, covering all twelve
 * contract domains it is composed from - {@link TableAdminContract} (including
 * {@link ColumnFamilyAttributes} for per-family attributes and {@link TableAttributes} for
 * table-level attributes such as SPLITS/SPLITS_FILE/NUMREGIONS+SPLITALGO/CONFIGURATION/METADATA),
 * {@link NamespaceAdminContract}, {@link ClusterOpsContract}, {@link ServerLifecycleContract},
 * {@link SnapshotAdminContract}, {@link ReplicationPeerContract}, {@link SecurityAdminContract},
 * {@link VisibilityLabelContract}, {@link QuotaAdminContract}, {@link ProcedureAdminContract},
 * {@link RsGroupAdminContract}, and {@link DiagnosticsContract} - ported from hbase-shell's
 * {@code hbase/admin.rb} and the corresponding {@code shell/commands/*.rb} files.
 */
@InterfaceAudience.Private
public final class DefaultShellAdmin implements ShellAdmin {
  private final Admin admin;
  private final ReplicationAdminOps replication;
  private final QuotaAdminOps quotas;
  private final ProcedureAdminOps procedures;

  public DefaultShellAdmin(Admin admin) {
    this.admin = admin;
    this.replication = new ReplicationAdminOps(admin);
    this.quotas = new QuotaAdminOps(admin);
    this.procedures = new ProcedureAdminOps(admin);
  }

  @Override
  public StatusView status() throws IOException {
    return StatusView.from(admin.getClusterMetrics());
  }

  @Override
  public void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) throws IOException {
    TableName table;
    try {
      table = TableName.valueOf(tableName);
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid table name '" + tableName + "': " + e.getMessage(), e);
    }
    TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(table);
    for (Map<String, Object> familySpec : familySpecs) {
      tableBuilder.setColumnFamily(ColumnFamilyAttributes.build(familySpec));
    }
    byte[][] splits =
      TableAttributes.apply(tableBuilder, tableAttributes, admin.getConfiguration());
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
      Method method = descriptor.getClass().getMethod("toStringTableAttributes");
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
      resolveServerName(hostOrServer, liveServers).ifPresent(servers::add);
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
    ServerName serverName = resolveServerName(hostOrServer, liveServers).orElseThrow(
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
      Object method = familySpec.get("METHOD");
      if (method != null) {
        if (!"delete".equalsIgnoreCase(method.toString())) {
          throw new IOException("Unsupported METHOD '" + method + "' (only 'delete' is supported)");
        }
        if (familySpec.size() > 2) {
          throw new IOException("METHOD => 'delete' takes only NAME, got: " + familySpec.keySet());
        }
        tableBuilder.removeColumnFamily(familyName);
        continue;
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
  public void snapshot(String tableName, String snapshotName) throws IOException {
    admin.snapshot(snapshotName, TableName.valueOf(tableName));
  }

  @Override
  public void deleteSnapshot(String snapshotName) throws IOException {
    admin.deleteSnapshot(snapshotName);
  }

  @Override
  public void deleteAllSnapshots(String regex) throws IOException {
    admin.deleteSnapshots(Pattern.compile(regex));
  }

  @Override
  public List<SnapshotInfo> listSnapshots(String regex) throws IOException {
    List<SnapshotInfo> snapshots = new ArrayList<>();
    for (SnapshotDescription snapshot : admin.listSnapshots(Pattern.compile(regex))) {
      snapshots.add(toSnapshotInfo(snapshot));
    }
    return snapshots;
  }

  @Override
  public List<SnapshotInfo> listTableSnapshots(String tableNameRegex, String snapshotNameRegex)
    throws IOException {
    List<SnapshotInfo> snapshots = new ArrayList<>();
    for (SnapshotDescription snapshot : admin.listTableSnapshots(Pattern.compile(tableNameRegex),
      Pattern.compile(snapshotNameRegex))) {
      snapshots.add(toSnapshotInfo(snapshot));
    }
    return snapshots;
  }

  private static SnapshotInfo toSnapshotInfo(SnapshotDescription snapshot) {
    return new SnapshotInfo(snapshot.getName(), snapshot.getTableNameAsString(),
      snapshot.getCreationTime(), snapshot.getTtl());
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
    return liveServers.stream().filter(sn -> parts[0].equals(sn.getHostname())
      && (parts.length < 2 || parts[1].equals(String.valueOf(sn.getPort())))).findFirst();
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
        AccessControlClient.grant(admin.getConnection(), TableName.valueOf(tableName), userOrGroup,
          familyBytes, qualifierBytes, permActions);
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
    for (TableName table : admin.listTableNamesByState(enabled)) {
      result.add(table.getNameAsString());
    }
    return result;
  }

  @Override
  public AlterStatusView alterStatus(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new IOException("Table '" + tableName + "' does not exist");
    }
    RegionStatesCount regionStatus =
      admin.getClusterMetrics().getTableRegionStatesCount().get(table);
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
    HRegionLocation location = admin.getConnection().getRegionLocator(TableName.valueOf(tableName))
      .getRegionLocation(Bytes.toBytes(rowKey));
    // Ruby locate_region prints RegionInfo#toString (ENCODED/NAME/STARTKEY/ENDKEY dict).
    return new RegionLocationView(location.getHostnamePort(), location.getRegion().toString());
  }

  @Override
  public ListRegionsView listRegions(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.isTableEnabled(table)) {
      throw new IOException("Table " + tableName + " must be enabled.");
    }
    ClusterMetrics clusterMetrics = admin.getClusterMetrics();
    List<String> warnings = new ArrayList<>();
    List<List<String>> rows = new ArrayList<>();
    for (HRegionLocation location : admin.getConnection().getRegionLocator(table)
      .getAllRegionLocations()) {
      RegionInfo regionInfo = location.getRegion();
      ServerName serverName = location.getServerName();
      ServerMetrics serverMetrics = clusterMetrics.getLiveServerMetrics().get(serverName);
      RegionMetrics regionMetrics = serverMetrics == null
        ? null
        : serverMetrics.getRegionMetrics().get(regionInfo.getRegionName());
      String regionName = regionInfo.getRegionNameAsString().trim();
      if (regionMetrics == null) {
        warnings.add("Can not find all details for region: " + regionName
          + " , it may be disabled or in transition");
      }
      String size = regionMetrics == null ? "" : String.valueOf(regionMetrics.getStoreFileSize());
      String req = regionMetrics == null ? "" : String.valueOf(regionMetrics.getRequestCount());
      String locality =
        regionMetrics == null ? "" : String.valueOf(regionMetrics.getDataLocality());
      rows.add(Arrays.asList(serverName == null ? "" : serverName.toString().trim(), regionName,
        Bytes.toStringBinary(regionInfo.getStartKey()).trim(),
        Bytes.toStringBinary(regionInfo.getEndKey()).trim(), size.trim(), req.trim(),
        locality.trim()));
    }
    return new ListRegionsView(warnings, rows);
  }

  @Override
  public void createNamespace(String namespace, Map<String, Object> properties) throws IOException {
    NamespaceDescriptor.Builder builder = NamespaceDescriptor.create(namespace);
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
  public void alterNamespace(String namespace, Map<String, Object> properties) throws IOException {
    NamespaceDescriptor existing = admin.getNamespaceDescriptor(namespace);
    NamespaceDescriptor.Builder builder = NamespaceDescriptor.create(existing);
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
    for (NamespaceDescriptor descriptor : admin.listNamespaceDescriptors()) {
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
  public BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition)
    throws IOException {
    return BalanceResult.from(admin.balance(BalanceRequest.newBuilder().setDryRun(dryRun)
      .setIgnoreRegionsInTransition(ignoreRegionsInTransition).build()));
  }

  @Override
  public void move(String encodedRegionName, String destServerName) throws IOException {
    byte[] encoded = Bytes.toBytes(encodedRegionName);
    if (destServerName == null) {
      admin.move(encoded);
    } else {
      admin.move(encoded, ServerName.valueOf(destServerName));
    }
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
  public void updateConfig(String serverName) throws IOException {
    admin.updateConfiguration(ServerName.valueOf(serverName));
  }

  @Override
  public void updateAllConfig() throws IOException {
    admin.updateConfiguration();
  }

  @Override
  public void updateRsGroupConfig(String groupName) throws IOException {
    admin.updateConfiguration(groupName);
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
        AccessControlClient.revoke(admin.getConnection(), TableName.valueOf(tableName), userOrGroup,
          familyBytes, qualifierBytes);
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
        rows.add(Arrays.asList(permission.getUser(), permission.getPermission().toString()));
      }
      return rows;
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
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
    throwIfVisibilityFailures(response);
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
    for (ByteString label : response.getLabelList()) {
      labels.add(Bytes.toStringBinary(label.toByteArray()));
    }
    return labels;
  }

  @Override
  public void setAuths(String user, List<String> labels) throws IOException {
    VisibilityLabelsResponse response;
    try {
      response =
        VisibilityClient.setAuths(admin.getConnection(), labels.toArray(new String[0]), user);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    throwIfVisibilityFailures(response);
  }

  @Override
  public List<String> getAuths(String user) throws IOException {
    org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.GetAuthsResponse response;
    try {
      response = VisibilityClient.getAuths(admin.getConnection(), user);
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
    for (ByteString auth : response.getAuthList()) {
      labels.add(Bytes.toStringBinary(auth.toByteArray()));
    }
    return labels;
  }

  @Override
  public void clearAuths(String user, List<String> labels) throws IOException {
    VisibilityLabelsResponse response;
    try {
      response =
        VisibilityClient.clearAuths(admin.getConnection(), labels.toArray(new String[0]), user);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    throwIfVisibilityFailures(response);
  }

  @Override
  public long setVisibility(String tableName, String visibility, Map<String, Object> scanOptions)
    throws IOException {
    return VisibilityOps.setVisibility(admin.getConnection(), tableName, visibility, scanOptions);
  }

  @Override
  public List<String> listSecurityCapabilities() throws IOException {
    List<String> names = new ArrayList<>();
    for (org.apache.hadoop.hbase.client.security.SecurityCapability capability : admin
      .getSecurityCapabilities()) {
      names.add(capability.getName());
    }
    return names;
  }

  private static void throwIfVisibilityFailures(VisibilityLabelsResponse response)
    throws IOException {
    if (response == null) {
      throw new IOException("DISABLED: Visibility labels feature is not available");
    }
    StringBuilder failures = new StringBuilder();
    for (RegionActionResult result : response.getResultList()) {
      if (result.hasException()) {
        failures.append(result.getException().getValue().toStringUtf8());
      }
    }
    if (failures.length() > 0) {
      throw new IOException(failures.toString());
    }
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
  public void removeRsGroup(String groupName) throws IOException {
    admin.removeRSGroup(groupName);
  }

  @Override
  public BalanceResult balanceRsGroup(String groupName, boolean dryRun,
    boolean ignoreRegionsInTransition) throws IOException {
    return BalanceResult.from(admin.balanceRSGroup(groupName, BalanceRequest.newBuilder()
      .setDryRun(dryRun).setIgnoreRegionsInTransition(ignoreRegionsInTransition).build()));
  }

  @Override
  public void moveTablesToRsGroup(List<String> tables, String groupName) throws IOException {
    Set<TableName> tableNames = tables.stream().map(TableName::valueOf).collect(Collectors.toSet());
    admin.setRSGroup(tableNames, groupName);
  }

  @Override
  public void moveNamespacesToRsGroup(List<String> namespaces, String groupName)
    throws IOException {
    Set<TableName> tables = new HashSet<>();
    for (String ns : namespaces) {
      try {
        admin.getNamespaceDescriptor(ns);
      } catch (org.apache.hadoop.hbase.NamespaceNotFoundException e) {
        throw new IOException("Can't find a namespace: " + ns, e);
      }
      for (TableName table : admin.listTableNamesByNamespace(ns)) {
        tables.add(table);
      }
    }
    if (!tables.isEmpty()) {
      admin.setRSGroup(tables, groupName);
    }
    for (String ns : namespaces) {
      Map<String, Object> props = new HashMap<>();
      props.put("METHOD", "set");
      props.put("hbase.rsgroup.name", groupName);
      alterNamespace(ns, props);
    }
  }

  @Override
  public void moveServersAndTablesToRsGroup(List<String> hostPorts, List<String> tables,
    String groupName) throws IOException {
    moveServersToRsGroup(hostPorts, groupName);
    moveTablesToRsGroup(tables, groupName);
  }

  @Override
  public void moveServersAndNamespacesToRsGroup(List<String> hostPorts, List<String> namespaces,
    String groupName) throws IOException {
    moveServersToRsGroup(hostPorts, groupName);
    moveNamespacesToRsGroup(namespaces, groupName);
  }

  @Override
  public String getRsGroupOfServer(String hostPort) throws IOException {
    RSGroupInfo group = admin.getRSGroup(Address.fromString(hostPort));
    if (group == null) {
      throw new IOException("Server has no group: " + hostPort);
    }
    return group.getName();
  }

  @Override
  public String getRsGroupOfTable(String tableName) throws IOException {
    RSGroupInfo group = admin.getRSGroup(TableName.valueOf(tableName));
    if (group == null) {
      throw new IOException("Table has no group: " + tableName);
    }
    return group.getName();
  }

  @Override
  public void removeServersFromRsGroup(List<String> hostPorts) throws IOException {
    Set<Address> addresses =
      hostPorts.stream().map(Address::fromString).collect(Collectors.toSet());
    admin.removeServersFromRSGroup(addresses);
  }

  @Override
  public void renameRsGroup(String oldName, String newName) throws IOException {
    admin.renameRSGroup(oldName, newName);
  }

  @Override
  public void alterRsGroupConfig(String groupName, Map<String, Object> args) throws IOException {
    RSGroupInfo group = admin.getRSGroup(groupName);
    if (group == null) {
      throw new IOException("RSGroup does not exist");
    }
    Map<String, String> configuration = new HashMap<>(group.getConfiguration());
    Object method = args.get("METHOD");
    if ("unset".equals(String.valueOf(method))) {
      configuration.remove(String.valueOf(args.get("NAME")));
    } else {
      for (Map.Entry<String, Object> e : args.entrySet()) {
        if ("METHOD".equals(e.getKey())) {
          continue;
        }
        configuration.put(e.getKey(), String.valueOf(e.getValue()));
      }
    }
    admin.updateRSGroupConfig(groupName, configuration);
  }

  @Override
  public List<List<String>> showRsGroupConfig(String groupName) throws IOException {
    RSGroupInfo group = admin.getRSGroup(groupName);
    if (group == null) {
      throw new IOException("RSGroup does not exist");
    }
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<String, String> e : group.getConfiguration().entrySet()) {
      rows.add(Arrays.asList(e.getKey(), e.getValue()));
    }
    return rows;
  }

  @Override
  public String getNamespaceRsGroup(String namespace) throws IOException {
    NamespaceDescriptor nsd = admin.getNamespaceDescriptor(namespace);
    return nsd.getConfigurationValue("hbase.rsgroup.name");
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

  @Override
  public void addPeer(String peerId, Map<String, Object> peerConfigSpec) throws IOException {
    replication.addPeer(peerId, peerConfigSpec);
  }

  @Override
  public void removePeer(String peerId) throws IOException {
    replication.removePeer(peerId);
  }

  @Override
  public List<PeerDescription> listPeers() throws IOException {
    return replication.listPeers();
  }

  @Override
  public void enablePeer(String peerId) throws IOException {
    replication.enablePeer(peerId);
  }

  @Override
  public void disablePeer(String peerId) throws IOException {
    replication.disablePeer(peerId);
  }

  @Override
  public void setPeerReplicateAll(String peerId, boolean replicateAll) throws IOException {
    replication.setPeerReplicateAll(peerId, replicateAll);
  }

  @Override
  public void setPeerSerial(String peerId, boolean serial) throws IOException {
    replication.setPeerSerial(peerId, serial);
  }

  @Override
  public void setPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.setPeerNamespaces(peerId, namespaces);
  }

  @Override
  public void appendPeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.appendPeerNamespaces(peerId, namespaces);
  }

  @Override
  public void removePeerNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.removePeerNamespaces(peerId, namespaces);
  }

  @Override
  public void setPeerExcludeNamespaces(String peerId, List<String> namespaces) throws IOException {
    replication.setPeerExcludeNamespaces(peerId, namespaces);
  }

  @Override
  public void appendPeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    replication.appendPeerExcludeNamespaces(peerId, namespaces);
  }

  @Override
  public void removePeerExcludeNamespaces(String peerId, List<String> namespaces)
    throws IOException {
    replication.removePeerExcludeNamespaces(peerId, namespaces);
  }

  @Override
  public String showPeerTableCFs(String peerId) throws IOException {
    return replication.showPeerTableCFs(peerId);
  }

  @Override
  public void setPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    replication.setPeerTableCFs(peerId, tableCFs);
  }

  @Override
  public void appendPeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    replication.appendPeerTableCFs(peerId, tableCFs);
  }

  @Override
  public void removePeerTableCFs(String peerId, Map<String, Object> tableCFs) throws IOException {
    replication.removePeerTableCFs(peerId, tableCFs);
  }

  @Override
  public void setPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    replication.setPeerExcludeTableCFs(peerId, tableCFs);
  }

  @Override
  public void appendPeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    replication.appendPeerExcludeTableCFs(peerId, tableCFs);
  }

  @Override
  public void removePeerExcludeTableCFs(String peerId, Map<String, Object> tableCFs)
    throws IOException {
    replication.removePeerExcludeTableCFs(peerId, tableCFs);
  }

  @Override
  public void setPeerBandwidth(String peerId, long bandwidth) throws IOException {
    replication.setPeerBandwidth(peerId, bandwidth);
  }

  @Override
  public List<List<String>> listReplicatedTables(String regex) throws IOException {
    return replication.listReplicatedTables(regex);
  }

  @Override
  public void enableTableReplication(String tableName) throws IOException {
    replication.enableTableReplication(tableName);
  }

  @Override
  public void disableTableReplication(String tableName) throws IOException {
    replication.disableTableReplication(tableName);
  }

  @Override
  public List<List<String>> getPeerConfigRows(String peerId) throws IOException {
    return replication.getPeerConfigRows(peerId);
  }

  @Override
  public List<List<String>> listPeerConfigRows() throws IOException {
    return replication.listPeerConfigRows();
  }

  @Override
  public void updatePeerConfig(String peerId, Map<String, Object> args) throws IOException {
    replication.updatePeerConfig(peerId, args);
  }

  @Override
  public void transitPeerSyncReplicationState(String peerId, String state) throws IOException {
    replication.transitPeerSyncReplicationState(peerId, state);
  }

  @Override
  public boolean peerModificationSwitch(boolean enabled, boolean drainProcs) throws IOException {
    return replication.peerModificationSwitch(enabled, drainProcs);
  }

  @Override
  public boolean peerModificationEnabled() throws IOException {
    return replication.peerModificationEnabled();
  }

  @Override
  public void setQuota(Map<String, Object> args) throws IOException {
    quotas.setQuota(args);
  }

  @Override
  public List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException {
    return quotas.listQuotas(filterArgs);
  }

  @Override
  public List<List<String>> listQuotaTableSizes() throws IOException {
    return quotas.listQuotaTableSizes();
  }

  @Override
  public List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) throws IOException {
    return quotas.listQuotaSnapshots(filterArgs);
  }

  @Override
  public List<List<String>> listSnapshotSizes() throws IOException {
    return quotas.listSnapshotSizes();
  }

  @Override
  public boolean switchRpcThrottle(boolean enabled) throws IOException {
    return quotas.switchRpcThrottle(enabled);
  }

  @Override
  public boolean isRpcThrottleEnabled() throws IOException {
    return quotas.isRpcThrottleEnabled();
  }

  @Override
  public boolean switchExceedThrottleQuota(boolean enabled) throws IOException {
    return quotas.switchExceedThrottleQuota(enabled);
  }

  @Override
  public List<List<String>> listProcedures() throws IOException {
    return procedures.listProcedures();
  }

  @Override
  public List<String> listLocks() throws IOException {
    return procedures.listLocks();
  }

  @Override
  public boolean normalize(Map<String, Object> filterArgs) throws IOException {
    NormalizeTableFilterParams.Builder builder = new NormalizeTableFilterParams.Builder();
    if (filterArgs != null && !filterArgs.isEmpty()) {
      if (filterArgs.containsKey("TABLE_NAME")) {
        Object tableName = filterArgs.get("TABLE_NAME");
        builder.tableNames(Arrays.asList(TableName.valueOf(String.valueOf(tableName))));
      } else if (filterArgs.containsKey("TABLE_NAMES")) {
        Object tableNames = filterArgs.get("TABLE_NAMES");
        if (!(tableNames instanceof List)) {
          throw new IOException("TABLE_NAMES must be of type Array");
        }
        List<TableName> names = new ArrayList<>();
        for (Object tn : (List<?>) tableNames) {
          names.add(TableName.valueOf(String.valueOf(tn)));
        }
        builder.tableNames(names);
      }
      if (filterArgs.containsKey("REGEX")) {
        builder.regex(String.valueOf(filterArgs.get("REGEX")));
      }
      if (filterArgs.containsKey("NAMESPACE")) {
        builder.namespace(String.valueOf(filterArgs.get("NAMESPACE")));
      }
    }
    return admin.normalize(builder.build());
  }

  @Override
  public boolean isInMaintenanceMode() throws IOException {
    return admin.isMasterInMaintenanceMode();
  }

  @Override
  public void unassign(String regionName) throws IOException {
    admin.unassign(Bytes.toBytes(regionName));
  }

  @Override
  public void flushMasterStore() throws IOException {
    admin.flushMasterStore();
  }

  @Override
  public int catalogJanitorRun() throws IOException {
    return admin.runCatalogJanitor();
  }

  @Override
  public boolean hbckChoreRun() throws IOException {
    try (Hbck hbck = admin.getConnection().getHbck()) {
      return hbck.runHbckChore();
    }
  }

  @Override
  public boolean cleanerChoreRun() throws IOException {
    return admin.runCleanerChore();
  }

  @Override
  public boolean cleanerChoreSwitch(boolean enabled) throws IOException {
    return admin.cleanerChoreSwitch(enabled);
  }

  @Override
  public boolean cleanerChoreEnabled() throws IOException {
    return admin.isCleanerChoreEnabled();
  }

  @Override
  public void walRoll(String serverName) throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    ServerName resolved = resolveServerName(serverName, liveServers).orElseThrow(
      () -> new IOException("Could not find server with specified name: " + serverName));
    admin.rollWALWriter(resolved);
  }

  @Override
  public void walRollAll() throws IOException {
    admin.rollAllWALWriters();
  }

  @Override
  public boolean snapshotCleanupSwitch(boolean enabled) throws IOException {
    return admin.snapshotCleanupSwitch(enabled, false);
  }

  @Override
  public boolean snapshotCleanupEnabled() throws IOException {
    return admin.isSnapshotCleanupEnabled();
  }

  @Override
  public long refreshMeta() throws IOException {
    return admin.refreshMeta();
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

  @Override
  public String clearSlowLogResponses(List<String> serverNames) throws IOException {
    Set<ServerName> servers = resolveLogServers(serverNames, true);
    List<Boolean> responses = admin.clearSlowLogResponses(servers);
    int success = 0;
    for (Boolean response : responses) {
      if (Boolean.TRUE.equals(response)) {
        success++;
      }
    }
    return "Cleared Slowlog responses from " + success + "/" + responses.size() + " RegionServers";
  }

  @Override
  public void reopenRegions(String tableName, List<String> regionNames) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (regionNames == null || regionNames.isEmpty()) {
      admin.reopenTableRegions(table);
      return;
    }
    List<RegionInfo> allRegions = admin.getRegions(table);
    List<RegionInfo> targetRegions = new ArrayList<>();
    for (String region : regionNames) {
      RegionInfo found = null;
      for (RegionInfo info : allRegions) {
        if (info.getEncodedName().equals(region) || info.getRegionNameAsString().equals(region)) {
          found = info;
          break;
        }
      }
      if (found == null) {
        throw new IOException("Region " + region + " not found in table " + tableName);
      }
      targetRegions.add(found);
    }
    admin.reopenTableRegions(table, targetRegions);
  }

  @Override
  public List<String> getBalancerDecisions(Map<String, Object> args) throws IOException {
    int limit = intArg(args, "LIMIT", 250);
    return logEntriesToJson(
      admin.getLogEntries(null, "BALANCER_DECISION", ServerType.MASTER, limit, null));
  }

  @Override
  public List<String> getBalancerRejections(Map<String, Object> args) throws IOException {
    int limit = intArg(args, "LIMIT", 250);
    return logEntriesToJson(
      admin.getLogEntries(null, "BALANCER_REJECTION", ServerType.MASTER, limit, null));
  }

  @Override
  public List<String> getSlowLogResponses(List<String> serverNames, Map<String, Object> args,
    boolean largeLog) throws IOException {
    Set<ServerName> servers;
    if (serverNames != null && serverNames.size() == 1 && "*".equals(serverNames.get(0))) {
      servers = resolveLogServers(Collections.emptyList(), true);
    } else {
      servers = resolveLogServers(serverNames, false);
    }
    int limit = intArg(args, "LIMIT", 10);
    Map<String, Object> filterParams = slowLogFilterParams(args);
    String logType = largeLog ? "LARGE_LOG" : "SLOW_LOG";
    return logEntriesToJson(
      admin.getLogEntries(servers, logType, ServerType.REGION_SERVER, limit, filterParams));
  }

  @Override
  public void mergeRegion(List<String> regionNames, boolean force) throws IOException {
    if (regionNames == null || regionNames.size() < 2) {
      throw new IOException("Must pass at least 2 regions to merge");
    }
    byte[][] regions = new byte[regionNames.size()][];
    for (int i = 0; i < regionNames.size(); i++) {
      regions[i] = Bytes.toBytes(regionNames.get(i));
    }
    FutureUtils.get(admin.mergeRegionsAsync(regions, force));
  }

  @Override
  public String zkDump() throws IOException {
    try (ZKWatcher watcher = new ZKWatcher(admin.getConfiguration(), "admin", null)) {
      return ZKDump.dump(watcher);
    }
  }

  @Override
  public void compactRegionServer(String serverName, boolean major) throws IOException {
    ServerName sn = ServerName.valueOf(serverName);
    if (major) {
      admin.majorCompactRegionServer(sn);
    } else {
      admin.compactRegionServer(sn);
    }
  }

  @Override
  public String getCompactionState(String tableName) throws IOException {
    return admin.getCompactionState(TableName.valueOf(tableName)).name();
  }

  @Override
  public void clearCompactionQueues(String serverName, List<String> queueNames) throws IOException {
    Set<String> queues = new HashSet<>();
    if (queueNames == null || queueNames.isEmpty()) {
      queues.add("long");
      queues.add("short");
    } else {
      for (String queue : queueNames) {
        if (!"long".equals(queue) && !"short".equals(queue)) {
          throw new IOException("Unknown queue name " + queue);
        }
        queues.add(queue);
      }
    }
    try {
      admin.clearCompactionQueues(ServerName.valueOf(serverName), queues);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new IOException(ie);
    }
  }

  @Override
  public String clearBlockCache(String tableName) throws IOException {
    return admin.clearBlockCache(TableName.valueOf(tableName)).toString();
  }

  @Override
  public String regionInfo(String regionName) throws IOException {
    Pair<RegionInfo, ServerName> fromMeta =
      MetaTableAccessor.getRegion(admin.getConnection(), Bytes.toBytes(regionName));
    if (fromMeta != null) {
      return fromMeta.getFirst().toString();
    }
    for (RegionInfo info : admin.getRegions(TableName.META_TABLE_NAME)) {
      if (
        info.getEncodedName().equals(regionName) || info.getRegionNameAsString().equals(regionName)
      ) {
        return info.toString();
      }
    }
    for (TableName table : admin.listTableNames()) {
      for (RegionInfo info : admin.getRegions(table)) {
        if (
          info.getEncodedName().equals(regionName)
            || info.getRegionNameAsString().equals(regionName)
        ) {
          return info.toString();
        }
      }
    }
    throw new UnknownRegionException(regionName);
  }

  @Override
  public List<String> regionsInTransition() throws IOException {
    List<String> rows = new ArrayList<>();
    for (RegionState state : admin.getClusterMetrics().getRegionStatesInTransition()) {
      rows.add(state.toDescriptiveString());
    }
    return rows;
  }

  @Override
  public void truncateRegion(String regionName) throws IOException {
    byte[] bytes = Bytes.toBytes(regionName);
    try {
      FutureUtils.get(admin.truncateRegionAsync(bytes));
    } catch (IllegalArgumentException | UnknownRegionException e) {
      admin.truncateRegion(bytes);
    }
  }

  @Override
  public long refreshHFiles(Map<String, Object> args) throws IOException {
    Object tableName = args == null ? null : args.get("TABLE_NAME");
    Object namespace = args == null ? null : args.get("NAMESPACE");
    if (namespace != null && tableName != null) {
      throw new IOException("Specify either a TABLE_NAME or a NAMESPACE, not both");
    }
    if ("".equals(namespace) || "".equals(tableName)) {
      throw new IOException("TABLE_NAME or NAMESPACE cannot be empty string");
    }
    if (namespace instanceof List || tableName instanceof List) {
      throw new IOException("TABLE_NAME or NAMESPACE must be a single string, not an array");
    }
    if (namespace != null) {
      return admin.refreshHFiles(String.valueOf(namespace));
    }
    if (tableName != null) {
      return admin.refreshHFiles(TableName.valueOf(String.valueOf(tableName)));
    }
    return admin.refreshHFiles();
  }

  private Set<ServerName> resolveLogServers(List<String> serverNames, boolean allIfEmpty)
    throws IOException {
    Collection<ServerName> liveServers = admin.getClusterMetrics().getLiveServerMetrics().keySet();
    if (serverNames == null || serverNames.isEmpty()) {
      if (allIfEmpty) {
        return new HashSet<>(liveServers);
      }
      return Collections.emptySet();
    }
    Set<ServerName> resolved = new HashSet<>();
    for (String hostOrServer : serverNames) {
      resolveServerName(hostOrServer, liveServers).ifPresent(resolved::add);
    }
    return resolved;
  }

  private static int intArg(Map<String, Object> args, String key, int defaultValue) {
    if (args == null || !args.containsKey(key)) {
      return defaultValue;
    }
    Object value = args.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return Integer.parseInt(String.valueOf(value));
  }

  private static Map<String, Object> slowLogFilterParams(Map<String, Object> args) {
    Map<String, Object> filterParams = new HashMap<>();
    if (args == null) {
      return filterParams;
    }
    if (args.containsKey("REGION_NAME")) {
      filterParams.put("regionName", String.valueOf(args.get("REGION_NAME")));
    }
    if (args.containsKey("TABLE_NAME")) {
      filterParams.put("tableName", String.valueOf(args.get("TABLE_NAME")));
    }
    if (args.containsKey("CLIENT_IP")) {
      filterParams.put("clientAddress", String.valueOf(args.get("CLIENT_IP")));
    }
    if (args.containsKey("USER")) {
      filterParams.put("userName", String.valueOf(args.get("USER")));
    }
    if (args.containsKey("FILTER_BY_OP")) {
      String op = String.valueOf(args.get("FILTER_BY_OP"));
      if (!"OR".equals(op) && !"AND".equals(op)) {
        throw new IllegalArgumentException("FILTER_BY_OP should be either OR / AND");
      }
      if ("AND".equals(op)) {
        filterParams.put("filterByOperator", "AND");
      }
    }
    return filterParams;
  }

  private static List<String> logEntriesToJson(List<LogEntry> entries) {
    List<String> result = new ArrayList<>();
    if (entries == null) {
      return result;
    }
    for (LogEntry entry : entries) {
      result.add(entry.toJsonPrettyPrint());
    }
    return result;
  }

}
