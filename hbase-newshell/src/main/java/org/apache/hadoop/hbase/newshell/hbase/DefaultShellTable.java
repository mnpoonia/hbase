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

import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.enumValue;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optString;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.requireBoolean;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.requireNumber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.Consistency;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.OperationWithAttributes;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Query;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.client.RegionReplicaUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.ParseFilter;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.security.visibility.Authorizations;
import org.apache.hadoop.hbase.security.visibility.CellVisibility;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Wraps a single real {@link Table}. Ported, for the pilot commands, from hbase-shell's
 * {@code hbase/table.rb#_get_internal} - handles {@code COLUMN} (including {@code cf:qualifier}
 * splitting and array-of-columns values), {@code VERSIONS}, and {@code TIMESTAMP} - and
 * {@code hbase/table.rb#_put_internal} - a single {@code cf:qualifier} column, required, plus an
 * optional {@code TIMESTAMP}; {@code _count_internal} (row count, with {@code INTERVAL} progress
 * reporting via {@link ShellTable.CountProgressListener}, {@code CACHE_BLOCKS}, and
 * {@code FILTER}); {@code _delete_internal}/{@code _deleteall_internal} (single-version vs
 * all-versions column delete, plus {@code ROWPREFIXFILTER}/{@code CACHE} batched range delete);
 * {@code _get_counter_internal}/{@code _incr_internal}/ {@code _append_internal}; and
 * {@code _get_splits_internal}. {@code FILTER} (via {@link ParseFilter}'s textual filter grammar)
 * and {@code TIMERANGE} are supported for {@code get} and {@code scan}; ATTRIBUTES/
 * ATTRIBUTES/VISIBILITY/TTL (for {@code put}) are supported, as are the scan options in
 * {@code SCAN_OPTIONS} and the {@code get}/{@code scan} FORMATTER/FORMATTER_CLASS plus per-column
 * {@code cf:qualifier:CONVERTER}. Scan metrics (ALL_METRICS/METRICS) are not ported.
 */
@InterfaceAudience.Private
public final class DefaultShellTable implements ShellTable {
  private static final Set<String> GET_OPTIONS =
    optionSet("COLUMN", "VERSIONS", "TIMESTAMP", "TIMERANGE", "FILTER", "ATTRIBUTES",
      "AUTHORIZATIONS", "CONSISTENCY", "REGION_REPLICA_ID", "FORMATTER", "FORMATTER_CLASS");
  private static final Set<String> PUT_OPTIONS =
    optionSet("TIMESTAMP", "ATTRIBUTES", "VISIBILITY", "TTL");
  private static final Set<String> SCAN_OPTIONS = optionSet("COLUMN", "COLUMNS", "LIMIT",
    "STARTROW", "STOPROW", "ROWPREFIXFILTER", "TIMESTAMP", "VERSIONS", "TIMERANGE", "FILTER",
    "CACHE", "CACHE_BLOCKS", "REVERSED", "RAW", "ATTRIBUTES", "AUTHORIZATIONS", "CONSISTENCY",
    "REGION_REPLICA_ID", "ISOLATION_LEVEL", "READ_TYPE", "ALLOW_PARTIAL_RESULTS", "BATCH",
    "MAX_RESULT_SIZE", "ALL_METRICS", "METRICS", "FORMATTER", "FORMATTER_CLASS");
  private static final Set<String> COUNT_OPTIONS = optionSet("COLUMN", "COLUMNS", "LIMIT",
    "STARTROW", "STOPROW", "ROWPREFIXFILTER", "VERSIONS", "FILTER", "CACHE_BLOCKS", "INTERVAL");
  private static final Set<String> DELETEALL_OPTIONS = optionSet("ROWPREFIXFILTER", "CACHE");

  private final Table table;

  public DefaultShellTable(Table table) {
    this.table = table;
  }

  @Override
  public GetResult get(String row, Map<String, Object> options)
    throws ShellCommandException, IOException {
    rejectUnsupportedOptions("get", options, GET_OPTIONS);
    Get get = new Get(BinaryStrings.toBytes(row));
    Map<String, String> columnConverters = new HashMap<>();
    ReadOptions.parse(options).applyTo(get, columnConverters);
    applyQueryOptions(get, options);
    ValueConverters converters =
      new ValueConverters(optString(options, "FORMATTER_CLASS"), optString(options, "FORMATTER"));
    Result result = table.get(get);
    List<CellView> cells = new ArrayList<>();
    List<Cell> resultCells = result.listCells();
    if (resultCells != null) {
      for (Cell cell : resultCells) {
        cells.add(toCellView(cell, converters, columnConverters));
      }
    }
    return new GetResult(cells);
  }

  @Override
  public void put(String row, String column, String value, Map<String, Object> options)
    throws ShellCommandException, IOException {
    rejectUnsupportedOptions("put", options, PUT_OPTIONS);
    int colonIndex = column.indexOf(':');
    if (colonIndex < 0 || colonIndex == column.length() - 1) {
      throw new IOException("Column '" + column + "' must be of the form 'family:qualifier'");
    }
    byte[] family = BinaryStrings.toBytes(column.substring(0, colonIndex));
    byte[] qualifier = BinaryStrings.toBytes(column.substring(colonIndex + 1));
    Put put = new Put(BinaryStrings.toBytes(row));
    applyPutOptions(put, options);
    Object timestamp = options.get("TIMESTAMP");
    if (timestamp != null) {
      put.addColumn(family, qualifier, requireNumber(timestamp, "TIMESTAMP").longValue(),
        BinaryStrings.toBytes(value));
    } else {
      put.addColumn(family, qualifier, BinaryStrings.toBytes(value));
    }
    table.put(put);
  }

  @Override
  public ScanResult scan(Map<String, Object> options) throws ShellCommandException, IOException {
    rejectUnsupportedOptions("scan", options, SCAN_OPTIONS);
    Map<String, String> columnConverters = new HashMap<>();
    Scan scan = ReadOptions.parse(options).newScan(columnConverters);
    applyQueryOptions(scan, options);
    Set<String> metricFilter = applyMetricsOptions(scan, options);
    ValueConverters converters =
      new ValueConverters(optString(options, "FORMATTER_CLASS"), optString(options, "FORMATTER"));
    Map<String, Long> metrics = new TreeMap<>();
    return new ScanResult(rowConsumer -> {
      try (ResultScanner scanner = table.getScanner(scan)) {
        for (Result result : scanner) {
          List<CellView> cells = new ArrayList<>();
          List<Cell> resultCells = result.listCells();
          if (resultCells != null) {
            for (Cell cell : resultCells) {
              cells.add(toCellView(cell, converters, columnConverters));
            }
          }
          rowConsumer.accept(new ScanRow(converters.convertRow(result.getRow()), cells));
        }
        if (scan.isScanMetricsEnabled() && scanner.getScanMetrics() != null) {
          for (Map.Entry<String, Long> metric : scanner.getScanMetrics().getMetricsMap()
            .entrySet()) {
            if (metricFilter.isEmpty() || metricFilter.contains(metric.getKey())) {
              metrics.put(metric.getKey(), metric.getValue());
            }
          }
        }
      } catch (ShellCommandException e) {
        throw new IOException(e.getMessage(), e);
      }
    }, metrics);
  }

  /**
   * Enables scan metrics for {@code ALL_METRICS => true} or any {@code METRICS => [names]}; returns
   * the names to keep (empty means all), as in the legacy shell.
   */
  private static Set<String> applyMetricsOptions(Scan scan, Map<String, Object> options)
    throws ShellCommandException {
    Set<String> names = new HashSet<>();
    Object metrics = options.get("METRICS");
    if (metrics != null) {
      if (!(metrics instanceof List)) {
        throw new ShellCommandException("METRICS must be a list of metric names");
      }
      for (Object name : (List<?>) metrics) {
        names.add(String.valueOf(name));
      }
    }
    boolean all = options.get("ALL_METRICS") != null
      && requireBoolean(options.get("ALL_METRICS"), "ALL_METRICS");
    if (all || metrics != null) {
      scan.setScanMetricsEnabled(true);
    }
    return names;
  }

  @Override
  public long count(Map<String, Object> options, CountProgressListener progressListener)
    throws ShellCommandException, IOException {
    rejectUnsupportedOptions("count", options, COUNT_OPTIONS);
    ReadOptions readOptions = ReadOptions.parse(options);
    Scan scan = readOptions.newScan(new HashMap<>());
    if (!readOptions.hasCacheBlocks()) {
      // count reads every row once; do not churn the block cache unless asked to.
      scan.setCacheBlocks(false);
    }
    long interval = resolveInterval(options.get("INTERVAL"));
    long count = 0;
    try (ResultScanner scanner = table.getScanner(scan)) {
      for (Result result : scanner) {
        count++;
        if (count % interval == 0) {
          progressListener.onProgress(count, Bytes.toStringBinary(result.getRow()));
        }
      }
    }
    return count;
  }

  private static long resolveInterval(Object intervalOption) throws ShellCommandException {
    if (intervalOption == null) {
      return 1000L;
    }
    long interval = requireNumber(intervalOption, "INTERVAL").longValue();
    if (interval <= 0) {
      throw new ShellCommandException("INTERVAL must be a positive number: " + interval);
    }
    return interval;
  }

  @Override
  public void delete(String row, String column, Long timestamp) throws IOException {
    long ts = timestamp == null ? HConstants.LATEST_TIMESTAMP : timestamp;
    Delete delete = new Delete(BinaryStrings.toBytes(row), ts);
    addDeleteColumn(delete, column, ts, false);
    table.delete(delete);
  }

  @Override
  public void deleteAll(String row, String column, Long timestamp, Map<String, Object> options)
    throws ShellCommandException, IOException {
    rejectUnsupportedOptions("deleteall", options, DELETEALL_OPTIONS);
    long ts = timestamp == null ? HConstants.LATEST_TIMESTAMP : timestamp;
    Object prefix = options.get("ROWPREFIXFILTER");
    if (prefix != null) {
      Object cacheOption = options.get("CACHE");
      int cache = cacheOption == null ? 100 : requireNumber(cacheOption, "CACHE").intValue();
      byte[] prefixBytes = BinaryStrings.toBytes(prefix.toString());
      Scan scan = new Scan().setStartStopRowForPrefixScan(prefixBytes);
      List<Delete> batch = new ArrayList<>();
      try (ResultScanner scanner = table.getScanner(scan)) {
        for (Result result : scanner) {
          Delete rowDelete = new Delete(result.getRow(), ts);
          addDeleteColumn(rowDelete, column, ts, true);
          batch.add(rowDelete);
          if (batch.size() >= cache) {
            table.delete(batch);
            batch.clear();
          }
        }
      }
      if (!batch.isEmpty()) {
        table.delete(batch);
      }
      return;
    }
    Delete delete = new Delete(BinaryStrings.toBytes(row), ts);
    addDeleteColumn(delete, column, ts, true);
    table.delete(delete);
  }

  @Override
  public Long getCounter(String row, String column) throws IOException {
    String[] parts = requireFamilyAndQualifier(column);
    Get get = new Get(BinaryStrings.toBytes(row));
    get.addColumn(BinaryStrings.toBytes(parts[0]), BinaryStrings.toBytes(parts[1]));
    get.readVersions(1);
    Result result = table.get(get);
    return decodeLong(result);
  }

  @Override
  public Long increment(String row, String column, long amount) throws IOException {
    String[] parts = requireFamilyAndQualifier(column);
    Increment increment = new Increment(BinaryStrings.toBytes(row));
    increment.addColumn(BinaryStrings.toBytes(parts[0]), BinaryStrings.toBytes(parts[1]), amount);
    Result result = table.increment(increment);
    return decodeLong(result);
  }

  @Override
  public String append(String row, String column, String value) throws IOException {
    String[] parts = requireFamilyAndQualifier(column);
    Append append = new Append(BinaryStrings.toBytes(row));
    append.addColumn(BinaryStrings.toBytes(parts[0]), BinaryStrings.toBytes(parts[1]),
      BinaryStrings.toBytes(value));
    Result result = table.append(append);
    if (result.isEmpty()) {
      return null;
    }
    Cell cell = result.listCells().get(0);
    return Bytes.toStringBinary(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
  }

  @Override
  public List<String> getSplits() throws IOException {
    List<String> splits = new ArrayList<>();
    try (RegionLocator locator = table.getRegionLocator()) {
      for (HRegionLocation location : locator.getAllRegionLocations()) {
        if (RegionReplicaUtil.isDefaultReplica(location.getRegion())) {
          splits.add(Bytes.toStringBinary(location.getRegion().getStartKey()));
        }
      }
    }
    splits.remove("");
    return splits;
  }

  private static Long decodeLong(Result result) {
    if (result.isEmpty()) {
      return null;
    }
    Cell cell = result.listCells().get(0);
    return Bytes.toLong(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
  }

  private static String[] requireFamilyAndQualifier(String column) throws IOException {
    int colonIndex = column.indexOf(':');
    if (colonIndex < 0 || colonIndex == column.length() - 1) {
      throw new IOException("Column '" + column + "' must be of the form 'family:qualifier'");
    }
    return new String[] { column.substring(0, colonIndex), column.substring(colonIndex + 1) };
  }

  private static void addDeleteColumn(Delete delete, String column, long timestamp,
    boolean allVersions) {
    if (column == null || column.isEmpty()) {
      return;
    }
    int colonIndex = column.indexOf(':');
    byte[] family;
    byte[] qualifier = null;
    if (colonIndex < 0) {
      family = BinaryStrings.toBytes(column);
    } else {
      family = BinaryStrings.toBytes(column.substring(0, colonIndex));
      String qualifierPart = column.substring(colonIndex + 1);
      if (!qualifierPart.isEmpty()) {
        qualifier = BinaryStrings.toBytes(qualifierPart);
      }
    }
    if (qualifier == null) {
      delete.addFamily(family, timestamp);
    } else if (allVersions) {
      delete.addColumns(family, qualifier, timestamp);
    } else {
      delete.addColumn(family, qualifier, timestamp);
    }
  }

  private static void applyPutOptions(Put put, Map<String, Object> options)
    throws ShellCommandException {
    applyAttributes(put, options.get("ATTRIBUTES"));
    Object visibility = options.get("VISIBILITY");
    if (visibility != null) {
      put.setCellVisibility(new CellVisibility(visibility.toString()));
    }
    Object ttl = options.get("TTL");
    if (ttl != null) {
      put.setTTL(requireNumber(ttl, "TTL").longValue());
    }
  }

  /**
   * Converts a server cell to a view. Delete markers (returned with {@code RAW}) carry their cell
   * type instead of a value, as in the legacy shell.
   */
  private CellView toCellView(Cell cell, ValueConverters converters,
    Map<String, String> columnConverters) throws ShellCommandException {
    byte[] family = CellUtil.cloneFamily(cell);
    byte[] qualifier = CellUtil.cloneQualifier(cell);
    String family2 = converters.convert(family);
    String qualifier2 = converters.convert(qualifier);
    if (CellUtil.isDelete(cell)) {
      return new CellView(family2, qualifier2, cell.getTimestamp(), "", cell.getType().toString());
    }
    String columnKey = Bytes.toStringBinary(family) + ":" + Bytes.toStringBinary(qualifier);
    if (TableName.META_TABLE_NAME.equals(table.getName())) {
      String metaValue = metaValue(columnKey, cell);
      if (metaValue != null) {
        return new CellView(family2, qualifier2, cell.getTimestamp(), metaValue);
      }
    }
    return new CellView(family2, qualifier2, cell.getTimestamp(),
      converters.convert(CellUtil.cloneValue(cell), columnConverters.get(columnKey)));
  }

  /**
   * hbase:meta cells whose bytes are not plain strings, rendered as the legacy shell does: region
   * info (and split/merge references) as a {@code RegionInfo}, the server start code as a long.
   * Returns {@code null} for every other column so the normal converters apply.
   */
  private static String metaValue(String column, Cell cell) {
    if (
      column.equals("info:regioninfo") || column.equals("info:splitA")
        || column.equals("info:splitB") || column.startsWith("info:merge")
    ) {
      RegionInfo info = RegionInfo.parseFromOrNull(cell.getValueArray(), cell.getValueOffset(),
        cell.getValueLength());
      return info == null ? "" : info.toString();
    }
    if (column.equals("info:serverstartcode")) {
      return cell.getValueLength() == Bytes.SIZEOF_LONG
        ? String
          .valueOf(Bytes.toLong(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()))
        : Bytes.toStringBinary(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
    }
    return null;
  }

  private static Set<String> optionSet(String... names) {
    return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
  }

  /**
   * Fails on any option the command does not implement, rather than silently dropping it and
   * returning (or deleting) more than the user asked for, e.g. {@code scan 't', {REVERSED => true}}
   * or {@code scan 't', {ROWPREFIXFILTER => 'x'}}.
   */
  private static void rejectUnsupportedOptions(String command, Map<String, Object> options,
    Set<String> supported) throws ShellCommandException {
    for (String key : options.keySet()) {
      if (!supported.contains(key)) {
        throw new ShellCommandException("Unsupported option '" + key + "' for " + command
          + " (supported: " + new TreeSet<>(supported) + ")");
      }
    }
  }

  private static String optString(Map<String, Object> options, String key) {
    Object value = options.get(key);
    return value == null ? null : value.toString();
  }

  @SuppressWarnings("unchecked")
  private static void applyAttributes(OperationWithAttributes op, Object attributes)
    throws ShellCommandException {
    if (attributes == null) {
      return;
    }
    if (!(attributes instanceof Map)) {
      throw new ShellCommandException("ATTRIBUTES must be a hash such as {'k' => 'v'}");
    }
    for (Map.Entry<String, Object> e : ((Map<String, Object>) attributes).entrySet()) {
      op.setAttribute(e.getKey(),
        e.getValue() == null ? null : Bytes.toBytes(e.getValue().toString()));
    }
  }

  /**
   * Options shared by {@code get} and {@code scan}: ATTRIBUTES, AUTHORIZATIONS, CONSISTENCY,
   * replica.
   */
  @SuppressWarnings("unchecked")
  private static void applyQueryOptions(Query query, Map<String, Object> options)
    throws ShellCommandException {
    applyAttributes(query, options.get("ATTRIBUTES"));
    Object authorizations = options.get("AUTHORIZATIONS");
    if (authorizations != null) {
      if (!(authorizations instanceof List)) {
        throw new ShellCommandException("AUTHORIZATIONS must be an array such as ['SECRET']");
      }
      List<String> auths = new ArrayList<>();
      for (Object auth : (List<Object>) authorizations) {
        auths.add(auth.toString());
      }
      query.setAuthorizations(new Authorizations(auths));
    }
    if (options.get("CONSISTENCY") != null) {
      query.setConsistency(enumValue(Consistency.class, options.get("CONSISTENCY"), "CONSISTENCY"));
    }
    Object replicaId = options.get("REGION_REPLICA_ID");
    if (replicaId != null) {
      query.setReplicaId(requireNumber(replicaId, "REGION_REPLICA_ID").intValue());
    }
  }
}
