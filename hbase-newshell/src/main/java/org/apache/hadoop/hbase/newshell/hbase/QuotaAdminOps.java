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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.quotas.QuotaFilter;
import org.apache.hadoop.hbase.quotas.QuotaScope;
import org.apache.hadoop.hbase.quotas.QuotaSettings;
import org.apache.hadoop.hbase.quotas.QuotaSettingsFactory;
import org.apache.hadoop.hbase.quotas.QuotaTableUtil;
import org.apache.hadoop.hbase.quotas.SpaceQuotaSnapshotView;
import org.apache.hadoop.hbase.quotas.ThrottleType;

/** Package-private collaborator used by {@link DefaultShellAdmin}. */
final class QuotaAdminOps {
  private final org.apache.hadoop.hbase.client.Admin admin;

  QuotaAdminOps(org.apache.hadoop.hbase.client.Admin admin) {
    this.admin = admin;
  }

  void setQuota(Map<String, Object> args) throws IOException {
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
    Object[] parsed =
      parseThrottleLimit(limitSpec, "null".equals(throttleTypeName) ? "REQUEST" : throttleTypeName);
    ThrottleType throttleType = (ThrottleType) parsed[0];
    long limit = (Long) parsed[1];
    TimeUnit timeUnit = (TimeUnit) parsed[2];
    QuotaScope scope = QuotaScope.valueOf(String.valueOf(spec.getOrDefault("SCOPE", "MACHINE")));
    if (spec.containsKey("USER")) {
      String user = String.valueOf(spec.get("USER"));
      if (spec.containsKey("TABLE")) {
        return QuotaSettingsFactory.throttleUser(user,
          TableName.valueOf(String.valueOf(spec.get("TABLE"))), throttleType, limit, timeUnit,
          scope);
      } else if (spec.containsKey("NAMESPACE")) {
        return QuotaSettingsFactory.throttleUser(user, String.valueOf(spec.get("NAMESPACE")),
          throttleType, limit, timeUnit, scope);
      }
      return QuotaSettingsFactory.throttleUser(user, throttleType, limit, timeUnit, scope);
    } else if (spec.containsKey("TABLE")) {
      return QuotaSettingsFactory.throttleTable(
        TableName.valueOf(String.valueOf(spec.get("TABLE"))), throttleType, limit, timeUnit, scope);
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

  List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException {
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

  List<List<String>> listQuotaTableSizes() throws IOException {
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<TableName, Long> entry : admin.getSpaceQuotaTableSizes().entrySet()) {
      rows.add(Arrays.asList(entry.getKey().toString(), String.valueOf(entry.getValue())));
    }
    return rows;
  }

  List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) throws IOException {
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

  List<List<String>> listSnapshotSizes() throws IOException {
    List<List<String>> rows = new ArrayList<>();
    for (Map.Entry<String, Long> entry : QuotaTableUtil
      .getObservedSnapshotSizes(admin.getConnection()).entrySet()) {
      rows.add(Arrays.asList(entry.getKey(), String.valueOf(entry.getValue())));
    }
    return rows;
  }

  boolean switchRpcThrottle(boolean enabled) throws IOException {
    return admin.switchRpcThrottle(enabled);
  }

  boolean isRpcThrottleEnabled() throws IOException {
    return admin.isRpcThrottleEnabled();
  }

  boolean switchExceedThrottleQuota(boolean enabled) throws IOException {
    return admin.exceedThrottleQuotaSwitch(enabled);
  }
}
