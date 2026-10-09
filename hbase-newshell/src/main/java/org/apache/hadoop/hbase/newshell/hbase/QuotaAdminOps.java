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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.quotas.QuotaFilter;
import org.apache.hadoop.hbase.quotas.QuotaScope;
import org.apache.hadoop.hbase.quotas.QuotaSettings;
import org.apache.hadoop.hbase.quotas.QuotaSettingsFactory;
import org.apache.hadoop.hbase.quotas.QuotaTableUtil;
import org.apache.hadoop.hbase.quotas.SpaceQuotaSnapshotView;
import org.apache.hadoop.hbase.quotas.SpaceViolationPolicy;
import org.apache.hadoop.hbase.quotas.ThrottleType;

/** Package-private collaborator used by {@link DefaultShellAdmin}. */
final class QuotaAdminOps implements QuotaAdminContract {
  private final org.apache.hadoop.hbase.client.Admin admin;

  QuotaAdminOps(org.apache.hadoop.hbase.client.Admin admin) {
    this.admin = admin;
  }

  public void setQuota(Map<String, Object> args) throws IOException {
    Map<String, Object> spec = new LinkedHashMap<>(args);
    Object type = spec.remove("TYPE");
    if (type == null && spec.containsKey("GLOBAL_BYPASS")) {
      admin.setQuota(buildGlobalBypass(spec));
      return;
    }
    if ("SPACE".equals(type)) {
      admin.setQuota(buildSpaceQuota(spec));
      return;
    }
    if (!"THROTTLE".equals(type)) {
      throw new ClientErrorException("set_quota requires TYPE => THROTTLE or TYPE => SPACE, "
        + "or USER => '...', GLOBAL_BYPASS => true|false");
    }
    Object limit = spec.remove("LIMIT");
    QuotaSettings settings;
    if ("NONE".equals(limit)) {
      settings = buildUnthrottle(spec);
    } else {
      if (limit == null) {
        throw new ClientErrorException("set_quota requires a LIMIT");
      }
      settings = buildThrottle(spec, String.valueOf(limit));
    }
    admin.setQuota(settings);
  }

  private static QuotaSettings buildGlobalBypass(Map<String, Object> spec) throws IOException {
    Object bypass = spec.remove("GLOBAL_BYPASS");
    Object user = spec.remove("USER");
    if (user == null) {
      throw new ClientErrorException("Expected USER");
    }
    if (!spec.isEmpty()) {
      throw new ClientErrorException("Unexpected arguments: " + spec);
    }
    String flag = String.valueOf(bypass);
    if (!flag.equalsIgnoreCase("true") && !flag.equalsIgnoreCase("false")) {
      throw new ClientErrorException("GLOBAL_BYPASS must be true or false, got: " + bypass);
    }
    return QuotaSettingsFactory.bypassGlobals(String.valueOf(user), Boolean.parseBoolean(flag));
  }

  /** {@code TYPE => SPACE}: limit (or, with {@code LIMIT => NONE}, remove) a table/namespace. */
  private static QuotaSettings buildSpaceQuota(Map<String, Object> spec) throws IOException {
    Object table = spec.remove("TABLE");
    Object namespace = spec.remove("NAMESPACE");
    if (table != null && namespace != null) {
      throw new ClientErrorException("Only one of TABLE or NAMESPACE can be specified.");
    }
    if (table == null && namespace == null) {
      throw new ClientErrorException("One of TABLE or NAMESPACE must be specified.");
    }
    Object limit = spec.remove("LIMIT");
    Object policyName = spec.remove("POLICY");
    if (!spec.isEmpty()) {
      throw new ClientErrorException("Unexpected arguments: " + spec);
    }
    if (limit == null) {
      throw new ClientErrorException("set_quota TYPE => SPACE requires a LIMIT (or NONE)");
    }
    if ("NONE".equals(limit)) {
      return table != null
        ? QuotaSettingsFactory.removeTableSpaceLimit(TableName.valueOf(String.valueOf(table)))
        : QuotaSettingsFactory.removeNamespaceSpaceLimit(String.valueOf(namespace));
    }
    long bytes = parseSpaceLimit(limit);
    if (policyName == null) {
      throw new ClientErrorException("set_quota TYPE => SPACE requires a POLICY");
    }
    SpaceViolationPolicy policy;
    try {
      policy = SpaceViolationPolicy.valueOf(String.valueOf(policyName));
    } catch (IllegalArgumentException e) {
      throw new ClientErrorException("Invalid POLICY '" + policyName + "', expected one of "
        + Arrays.toString(SpaceViolationPolicy.values()));
    }
    return table != null
      ? QuotaSettingsFactory.limitTableSpace(TableName.valueOf(String.valueOf(table)), bytes,
        policy)
      : QuotaSettingsFactory.limitNamespaceSpace(String.valueOf(namespace), bytes, policy);
  }

  /** A raw byte count, or digits with a B/K/M/G/T/P suffix (1024-based), as in quotas.rb. */
  static long parseSpaceLimit(Object limit) throws IOException {
    long bytes;
    if (limit instanceof Number) {
      bytes = ((Number) limit).longValue();
    } else {
      Matcher match = Pattern.compile("^(\\d+)([bkmgtp]?)$")
        .matcher(String.valueOf(limit).toLowerCase(java.util.Locale.ROOT));
      if (!match.matches()) {
        throw new ClientErrorException("Invalid size limit syntax: " + limit);
      }
      int shift = "bkmgtp".indexOf(match.group(2).isEmpty() ? 'b' : match.group(2).charAt(0)) * 10;
      bytes = Long.parseLong(match.group(1)) << shift;
    }
    if (bytes <= 0) {
      throw new ClientErrorException("Invalid space limit, must be greater than 0");
    }
    return bytes;
  }

  private static QuotaSettings buildThrottle(Map<String, Object> spec, String limitSpec)
    throws IOException {
    String throttleTypeName = String.valueOf(spec.remove("THROTTLE_TYPE"));
    Object[] parsed =
      parseThrottleLimit(limitSpec, "null".equals(throttleTypeName) ? "REQUEST" : throttleTypeName);
    ThrottleType throttleType = (ThrottleType) parsed[0];
    long limit = (Long) parsed[1];
    TimeUnit timeUnit = (TimeUnit) parsed[2];
    if (limit <= 0) {
      throw new ClientErrorException(
        "Invalid throttle limit, must be greater than 0: " + limitSpec);
    }
    Object scopeName = spec.remove("SCOPE");
    QuotaScope scope;
    try {
      scope = QuotaScope.valueOf(
        scopeName == null ? "MACHINE" : String.valueOf(scopeName).toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ClientErrorException("Invalid SCOPE, expected MACHINE or CLUSTER", e);
    }
    Object user = spec.remove("USER");
    Object table = spec.remove("TABLE");
    Object namespace = spec.remove("NAMESPACE");
    boolean regionServer = spec.containsKey("REGIONSERVER");
    spec.remove("REGIONSERVER");
    if (!spec.isEmpty()) {
      throw new ClientErrorException("Unexpected arguments: " + spec);
    }
    if (user == null && table != null && namespace != null) {
      throw new ClientErrorException("Only one of TABLE or NAMESPACE can be specified.");
    }
    if (user != null) {
      String userName = String.valueOf(user);
      if (table != null) {
        return QuotaSettingsFactory.throttleUser(userName, TableName.valueOf(String.valueOf(table)),
          throttleType, limit, timeUnit, scope);
      } else if (namespace != null) {
        return QuotaSettingsFactory.throttleUser(userName, String.valueOf(namespace), throttleType,
          limit, timeUnit, scope);
      }
      return QuotaSettingsFactory.throttleUser(userName, throttleType, limit, timeUnit, scope);
    } else if (table != null) {
      return QuotaSettingsFactory.throttleTable(TableName.valueOf(String.valueOf(table)),
        throttleType, limit, timeUnit, scope);
    } else if (namespace != null) {
      return QuotaSettingsFactory.throttleNamespace(String.valueOf(namespace), throttleType, limit,
        timeUnit, scope);
    } else if (regionServer) {
      if (scope == QuotaScope.CLUSTER) {
        throw new ClientErrorException("Invalid region server throttle scope, must be MACHINE");
      }
      return QuotaSettingsFactory.throttleRegionServer("all", throttleType, limit, timeUnit);
    }
    throw new ClientErrorException(
      "One of USER, TABLE, NAMESPACE or REGIONSERVER must be specified");
  }

  private static QuotaSettings buildUnthrottle(Map<String, Object> spec) throws IOException {
    Object typeName = spec.remove("THROTTLE_TYPE");
    ThrottleType type = null;
    if (typeName != null) {
      try {
        type = ThrottleType.valueOf(String.valueOf(typeName));
      } catch (IllegalArgumentException e) {
        throw new ClientErrorException("Invalid THROTTLE_TYPE '" + typeName + "', expected one of "
          + java.util.Arrays.toString(ThrottleType.values()), e);
      }
    }
    Object user = spec.remove("USER");
    Object table = spec.remove("TABLE");
    Object namespace = spec.remove("NAMESPACE");
    boolean regionServer = spec.containsKey("REGIONSERVER");
    spec.remove("REGIONSERVER");
    if (!spec.isEmpty()) {
      throw new ClientErrorException("Unexpected arguments: " + spec);
    }
    if (user == null && table != null && namespace != null) {
      throw new ClientErrorException("Only one of TABLE or NAMESPACE can be specified.");
    }
    if (user != null) {
      String userName = String.valueOf(user);
      if (table != null) {
        TableName tableName = TableName.valueOf(String.valueOf(table));
        return type == null
          ? QuotaSettingsFactory.unthrottleUser(userName, tableName)
          : QuotaSettingsFactory.unthrottleUserByThrottleType(userName, tableName, type);
      } else if (namespace != null) {
        String ns = String.valueOf(namespace);
        return type == null
          ? QuotaSettingsFactory.unthrottleUser(userName, ns)
          : QuotaSettingsFactory.unthrottleUserByThrottleType(userName, ns, type);
      }
      return type == null
        ? QuotaSettingsFactory.unthrottleUser(userName)
        : QuotaSettingsFactory.unthrottleUserByThrottleType(userName, type);
    } else if (table != null) {
      TableName tableName = TableName.valueOf(String.valueOf(table));
      return type == null
        ? QuotaSettingsFactory.unthrottleTable(tableName)
        : QuotaSettingsFactory.unthrottleTableByThrottleType(tableName, type);
    } else if (namespace != null) {
      String ns = String.valueOf(namespace);
      return type == null
        ? QuotaSettingsFactory.unthrottleNamespace(ns)
        : QuotaSettingsFactory.unthrottleNamespaceByThrottleType(ns, type);
    } else if (regionServer) {
      return type == null
        ? QuotaSettingsFactory.unthrottleRegionServer("all")
        : QuotaSettingsFactory.unthrottleRegionServerByThrottleType("all", type);
    }
    throw new ClientErrorException(
      "One of USER, TABLE, NAMESPACE or REGIONSERVER must be specified");
  }

  private static final java.util.regex.Pattern LIMIT_PATTERN =
    java.util.regex.Pattern.compile("^(\\d+)(req|cu|[bkmgtp])/(sec|min|hour|day)$");

  private static Object[] parseThrottleLimit(String limitSpec, String throttleTypePrefix)
    throws IOException {
    Matcher matcher = LIMIT_PATTERN.matcher(limitSpec.toLowerCase(java.util.Locale.ROOT));
    if (!matcher.matches()) {
      throw new ClientErrorException("Invalid limit syntax: " + limitSpec);
    }
    long limit = Long.parseLong(matcher.group(1));
    String unit = matcher.group(2);
    String suffix;
    if ("req".equals(unit)) {
      suffix = "_NUMBER";
    } else if ("cu".equals(unit)) {
      suffix = "_CAPACITY_UNIT"; // capacity units are unit-less counts
    } else {
      suffix = "_SIZE";
      limit = sizeFromUnit(limit, unit);
    }
    ThrottleType type;
    try {
      type = ThrottleType.valueOf(throttleTypePrefix + suffix);
    } catch (IllegalArgumentException e) {
      throw new ClientErrorException(
        "Invalid THROTTLE_TYPE '" + throttleTypePrefix + "' for limit '" + limitSpec
          + "', expected one of " + Arrays.toString(ThrottleType.values()),
        e);
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
        throw new ClientErrorException("Invalid time unit in limit: " + limitSpec);
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
        throw new ClientErrorException("Invalid size unit: " + unit);
    }
  }

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
        .map(entry -> entry.getKey() + " => " + entry.getValue()).collect(Collectors.joining(", "));
      rows.add(Arrays.asList(ownerString, settings.toString()));
    }
    return rows;
  }

  public List<List<String>> listQuotaTableSizes() throws IOException {
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<TableName, Long> entry : admin.getSpaceQuotaTableSizes().entrySet()) {
      rows.add(Arrays.asList(entry.getKey().toString(), String.valueOf(entry.getValue())));
    }
    return rows;
  }

  public List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) throws IOException {
    Map<String, Object> args = filterArgs == null ? Collections.emptyMap() : filterArgs;
    Object desiredTable = args.get("TABLE");
    Object desiredNamespace = args.get("NAMESPACE");
    Object desiredRegionServer = args.get("REGIONSERVER");
    Map<TableName, ? extends SpaceQuotaSnapshotView> snapshots;
    if (desiredRegionServer != null) {
      snapshots = admin.getRegionServerSpaceQuotaSnapshots(
        ServerName.valueOf(String.valueOf(desiredRegionServer)));
    } else {
      snapshots = QuotaTableUtil.getSnapshots(admin.getConnection());
    }
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<TableName, ? extends SpaceQuotaSnapshotView> entry : snapshots.entrySet()) {
      TableName tableName = entry.getKey();
      if (!accept(tableName, desiredTable, desiredNamespace)) {
        continue;
      }
      SpaceQuotaSnapshotView snapshot = entry.getValue();
      SpaceQuotaSnapshotView.SpaceQuotaStatusView status = snapshot.getQuotaStatus();
      String policy = "None";
      if (status.isInViolation() && status.getPolicy().isPresent()) {
        policy = status.getPolicy().get().name();
      }
      rows.add(Arrays.asList(tableName.toString(), String.valueOf(snapshot.getUsage()),
        String.valueOf(snapshot.getLimit()), String.valueOf(status.isInViolation()), policy));
    }
    return rows;
  }

  private static boolean accept(TableName tableName, Object desiredTable, Object desiredNamespace) {
    if (
      desiredTable != null && !tableName.getQualifierAsString().equals(String.valueOf(desiredTable))
    ) {
      return false;
    }
    if (
      desiredNamespace != null
        && !tableName.getNamespaceAsString().equals(String.valueOf(desiredNamespace))
    ) {
      return false;
    }
    return true;
  }

  public List<List<String>> listSnapshotSizes() throws IOException {
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<String, Long> entry : QuotaTableUtil
      .getObservedSnapshotSizes(admin.getConnection()).entrySet()) {
      rows.add(Arrays.asList(entry.getKey(), String.valueOf(entry.getValue())));
    }
    return rows;
  }

  public boolean switchRpcThrottle(boolean enabled) throws IOException {
    return admin.switchRpcThrottle(enabled);
  }

  public boolean isRpcThrottleEnabled() throws IOException {
    return admin.isRpcThrottleEnabled();
  }

  public boolean switchExceedThrottleQuota(boolean enabled) throws IOException {
    return admin.exceedThrottleQuotaSwitch(enabled);
  }
}
