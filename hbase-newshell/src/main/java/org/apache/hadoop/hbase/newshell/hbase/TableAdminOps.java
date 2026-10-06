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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.MetaTableAccessor;
import org.apache.hadoop.hbase.RegionMetrics;
import org.apache.hadoop.hbase.ServerMetrics;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.UnknownRegionException;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.CompactType;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.client.RegionStatesCount;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.master.RegionState;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.FutureUtils;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link TableAdminContract} operations on a real {@link Admin}, used by {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class TableAdminOps implements TableAdminContract {
  private final Admin admin;

  TableAdminOps(Admin admin) {
    this.admin = admin;
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
      throw new ClientErrorException("Invalid table name '" + tableName + "': " + e.getMessage(),
        e);
    }
    TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(table);
    for (Map<String, Object> familySpec : familySpecs) {
      tableBuilder
        .setColumnFamily(ColumnFamilyAttributes.build(familySpec, admin.getConfiguration()));
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
      throw new ClientErrorException("Table '" + tableName + "' does not exist");
    }
    if (admin.isTableDisabled(table)) {
      throw new ClientErrorException("Table '" + tableName + "' is already disabled");
    }
    admin.disableTable(table);
  }

  @Override
  public void enableTable(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new ClientErrorException("Table '" + tableName + "' does not exist");
    }
    if (admin.isTableEnabled(table)) {
      throw new ClientErrorException("Table '" + tableName + "' is already enabled");
    }
    admin.enableTable(table);
  }

  @Override
  public void dropTable(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new ClientErrorException("Table '" + tableName + "' does not exist");
    }
    if (admin.isTableEnabled(table)) {
      throw new ClientErrorException("Table '" + tableName + "' is enabled. Disable it first.");
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
      throw new ClientErrorException("Table '" + tableName + "' does not exist");
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
  public void alterTable(String tableName, List<Map<String, Object>> specs) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.tableExists(table)) {
      throw new ClientErrorException("Table '" + tableName + "' does not exist");
    }
    TableDescriptorBuilder tableBuilder =
      TableDescriptorBuilder.newBuilder(admin.getDescriptor(table));
    boolean reopenRegions = true;
    for (Map<String, Object> original : specs) {
      Map<String, Object> spec = new LinkedHashMap<>(original);
      // Shortcut delete syntax: alter 't', 'delete' => 'cf'
      if (spec.containsKey("delete")) {
        Object family = spec.remove("delete");
        spec.put("METHOD", "delete");
        spec.put("NAME", family);
      }
      Object reopen = spec.remove("REOPEN_REGIONS");
      if (reopen != null) {
        String text = reopen.toString().toLowerCase(Locale.ROOT);
        if (!"true".equals(text) && !"false".equals(text)) {
          throw new ClientErrorException(
            "Invalid 'REOPEN_REGIONS' for non-boolean value: " + reopen);
        }
        reopenRegions = Boolean.parseBoolean(text);
      }
      Object method = spec.remove("METHOD");
      if (method == null && spec.containsKey("NAME")) {
        upsertColumnFamily(tableBuilder, spec);
      } else if (method == null || "table_att".equals(method.toString())) {
        spec.remove("NAME");
        TableAttributes.applyToExisting(tableBuilder, spec, admin.getConfiguration());
      } else {
        applyAlterMethod(tableBuilder, method.toString(), spec, tableName);
      }
    }
    if (reopenRegions) {
      admin.modifyTable(tableBuilder.build());
    } else {
      try {
        admin.modifyTableAsync(tableBuilder.build(), false).get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while modifying table " + tableName, e);
      } catch (ExecutionException e) {
        throw new ClientErrorException("Failed to modify table " + tableName, e.getCause());
      }
    }
  }

  /** Modifies the named family if it exists, otherwise adds it (hbase-shell alter semantics). */
  private void upsertColumnFamily(TableDescriptorBuilder tableBuilder,
    Map<String, Object> familySpec) throws IOException {
    byte[] familyName = Bytes.toBytes(familySpec.get("NAME").toString());
    ColumnFamilyDescriptor existingFamily = tableBuilder.build().getColumnFamily(familyName);
    if (existingFamily == null) {
      tableBuilder
        .setColumnFamily(ColumnFamilyAttributes.build(familySpec, admin.getConfiguration()));
      return;
    }
    ColumnFamilyDescriptorBuilder familyBuilder =
      ColumnFamilyDescriptorBuilder.newBuilder(existingFamily);
    ColumnFamilyAttributes.applyAttributes(familyBuilder, familySpec, admin.getConfiguration());
    tableBuilder.modifyColumnFamily(familyBuilder.build());
  }

  private static void applyAlterMethod(TableDescriptorBuilder tableBuilder, String method,
    Map<String, Object> spec, String tableName) throws IOException {
    switch (method) {
      case "delete": {
        Object name = requireSpecValue(spec, "NAME", method);
        if (spec.size() > 1) {
          throw new ClientErrorException(
            "METHOD => 'delete' takes only NAME, got: " + spec.keySet());
        }
        byte[] familyName = Bytes.toBytes(name.toString());
        if (!tableBuilder.build().hasColumnFamily(familyName)) {
          throw new ClientErrorException(
            "Column family '" + name + "' does not exist on table '" + tableName + "'");
        }
        tableBuilder.removeColumnFamily(familyName);
        break;
      }
      case "table_att_unset":
      case "table_conf_unset": {
        String what = "table_att_unset".equals(method) ? "attribute" : "configuration";
        for (String key : asStrings(requireSpecValue(spec, "NAME", method))) {
          if (tableBuilder.build().getValue(key) == null) {
            throw new ClientErrorException("Could not find " + what + ": " + key);
          }
          tableBuilder.removeValue(key);
        }
        break;
      }
      case "table_remove_coprocessor":
        for (String className : asStrings(requireSpecValue(spec, "CLASSNAME", method))) {
          tableBuilder.removeCoprocessor(className);
        }
        break;
      default:
        throw new ClientErrorException("Unknown method: " + method);
    }
  }

  private static Object requireSpecValue(Map<String, Object> spec, String key, String method)
    throws IOException {
    Object value = spec.get(key);
    if (value == null) {
      throw new ClientErrorException(key + " parameter missing for " + method + " method");
    }
    return value;
  }

  private static List<String> asStrings(Object value) {
    List<String> result = new ArrayList<>();
    if (value instanceof Collection) {
      for (Object item : (Collection<?>) value) {
        result.add(String.valueOf(item));
      }
    } else {
      result.add(value.toString());
    }
    return result;
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

  private static CompactType parseCompactType(String type) throws IOException {
    if (type == null || type.equalsIgnoreCase("NORMAL")) {
      return CompactType.NORMAL;
    }
    if (type.equalsIgnoreCase("MOB")) {
      return CompactType.MOB;
    }
    throw new ClientErrorException("only NORMAL or MOB accepted for type!");
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
      throw new ClientErrorException("Table '" + tableName + "' does not exist");
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
    HRegionLocation location;
    try (RegionLocator locator =
      admin.getConnection().getRegionLocator(TableName.valueOf(tableName))) {
      location = locator.getRegionLocation(BinaryStrings.toBytes(rowKey));
    }
    // Ruby locate_region prints RegionInfo#toString (ENCODED/NAME/STARTKEY/ENDKEY dict).
    return new RegionLocationView(location.getHostnamePort(), location.getRegion().toString());
  }

  @Override
  public ListRegionsView listRegions(String tableName) throws IOException {
    TableName table = TableName.valueOf(tableName);
    if (!admin.isTableEnabled(table)) {
      throw new ClientErrorException("Table " + tableName + " must be enabled.");
    }
    ClusterMetrics clusterMetrics = admin.getClusterMetrics();
    List<String> warnings = new ArrayList<>();
    List<List<String>> rows = new ArrayList<>();
    List<HRegionLocation> locations;
    try (RegionLocator locator = admin.getConnection().getRegionLocator(table)) {
      locations = locator.getAllRegionLocations();
    }
    for (HRegionLocation location : locations) {
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
  public void move(String encodedRegionName, String destServerName) throws IOException {
    byte[] encoded = Bytes.toBytes(encodedRegionName);
    if (destServerName == null) {
      admin.move(encoded);
    } else {
      admin.move(encoded, ServerName.valueOf(destServerName));
    }
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

  @Override
  public void unassign(String regionName) throws IOException {
    admin.unassign(Bytes.toBytes(regionName));
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
        throw new ClientErrorException("Region " + region + " not found in table " + tableName);
      }
      targetRegions.add(found);
    }
    admin.reopenTableRegions(table, targetRegions);
  }

  @Override
  public void mergeRegion(List<String> regionNames, boolean force) throws IOException {
    if (regionNames == null || regionNames.size() < 2) {
      throw new ClientErrorException("Must pass at least 2 regions to merge");
    }
    byte[][] regions = new byte[regionNames.size()][];
    for (int i = 0; i < regionNames.size(); i++) {
      regions[i] = Bytes.toBytes(regionNames.get(i));
    }
    FutureUtils.get(admin.mergeRegionsAsync(regions, force));
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
          throw new ClientErrorException("Unknown queue name " + queue);
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
      throw new ClientErrorException("Specify either a TABLE_NAME or a NAMESPACE, not both");
    }
    if ("".equals(namespace) || "".equals(tableName)) {
      throw new ClientErrorException("TABLE_NAME or NAMESPACE cannot be empty string");
    }
    if (namespace instanceof List || tableName instanceof List) {
      throw new ClientErrorException(
        "TABLE_NAME or NAMESPACE must be a single string, not an array");
    }
    if (namespace != null) {
      return admin.refreshHFiles(String.valueOf(namespace));
    }
    if (tableName != null) {
      return admin.refreshHFiles(TableName.valueOf(String.valueOf(tableName)));
    }
    return admin.refreshHFiles();
  }
}
