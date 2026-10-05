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
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.client.RegionReplicaUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.ParseFilter;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
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
 * AUTHORIZATIONS/CONSISTENCY (for {@code get}), ATTRIBUTES/VISIBILITY/TTL (for {@code put}) are
 * explicitly not ported.
 */
@InterfaceAudience.Private
public final class DefaultShellTable implements ShellTable {
  private static final Set<String> GET_OPTIONS =
    optionSet("COLUMN", "VERSIONS", "TIMESTAMP", "TIMERANGE", "FILTER");
  private static final Set<String> PUT_OPTIONS = optionSet("TIMESTAMP");
  private static final Set<String> SCAN_OPTIONS =
    optionSet("COLUMNS", "LIMIT", "STARTROW", "STOPROW", "VERSIONS", "TIMERANGE", "FILTER");
  private static final Set<String> COUNT_OPTIONS = optionSet("COLUMNS", "LIMIT", "STARTROW",
    "STOPROW", "VERSIONS", "FILTER", "CACHE_BLOCKS", "INTERVAL");
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
    Object columns = options.get("COLUMN");
    if (columns != null) {
      for (Object column : asList(columns)) {
        addColumn(get, column.toString());
      }
    }
    Object versions = options.get("VERSIONS");
    if (versions != null) {
      get.readVersions(requireNumber(versions, "VERSIONS").intValue());
    }
    Object timestamp = options.get("TIMESTAMP");
    if (timestamp != null) {
      get.setTimestamp(requireNumber(timestamp, "TIMESTAMP").longValue());
    }
    Object timerange = options.get("TIMERANGE");
    if (timerange != null) {
      long[] range = parseTimeRange(timerange);
      get.setTimeRange(range[0], range[1]);
    }
    Object filter = options.get("FILTER");
    if (filter != null) {
      get.setFilter(parseFilterString(filter));
    }
    Result result = table.get(get);
    List<CellView> cells = new ArrayList<>();
    List<Cell> resultCells = result.listCells();
    if (resultCells != null) {
      for (Cell cell : resultCells) {
        cells.add(new CellView(Bytes.toStringBinary(CellUtil.cloneFamily(cell)),
          Bytes.toStringBinary(CellUtil.cloneQualifier(cell)), cell.getTimestamp(),
          Bytes.toStringBinary(CellUtil.cloneValue(cell))));
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
    Scan scan = buildScan(options);
    Object timerange = options.get("TIMERANGE");
    if (timerange != null) {
      long[] range = parseTimeRange(timerange);
      scan.setTimeRange(range[0], range[1]);
    }
    Object filter = options.get("FILTER");
    if (filter != null) {
      scan.setFilter(parseFilterString(filter));
    }
    return new ScanResult(rowConsumer -> {
      try (ResultScanner scanner = table.getScanner(scan)) {
        for (Result result : scanner) {
          List<CellView> cells = new ArrayList<>();
          List<Cell> resultCells = result.listCells();
          if (resultCells != null) {
            for (Cell cell : resultCells) {
              cells.add(new CellView(Bytes.toStringBinary(CellUtil.cloneFamily(cell)),
                Bytes.toStringBinary(CellUtil.cloneQualifier(cell)), cell.getTimestamp(),
                Bytes.toStringBinary(CellUtil.cloneValue(cell))));
            }
          }
          rowConsumer.accept(new ScanRow(Bytes.toStringBinary(result.getRow()), cells));
        }
      }
    });
  }

  @Override
  public long count(Map<String, Object> options, CountProgressListener progressListener)
    throws ShellCommandException, IOException {
    rejectUnsupportedOptions("count", options, COUNT_OPTIONS);
    Scan scan = buildScan(options);
    Object filter = options.get("FILTER");
    if (filter != null) {
      scan.setFilter(parseFilterString(filter));
    }
    scan.setCacheBlocks(resolveCacheBlocks(options.get("CACHE_BLOCKS")));
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

  /**
   * Coerces a {@code CACHE_BLOCKS} option value, matching hbase-shell's {@code count} command:
   * defaults to {@code false} when absent, accepts a real {@link Boolean}, or a {@link String}
   * whose value is {@code "true"}/{@code "false"} case-insensitively - any other value is rejected.
   * Deliberately not reusing {@link AttributeCoercion#toBoolean}, which leniently delegates to
   * {@link Boolean#parseBoolean} and would silently treat any non-"true" string (e.g. a typo) as
   * {@code false} instead of raising an error.
   */
  private static boolean resolveCacheBlocks(Object cacheBlocksOption) throws ShellCommandException {
    if (cacheBlocksOption == null) {
      return false;
    }
    if (cacheBlocksOption instanceof Boolean) {
      return (Boolean) cacheBlocksOption;
    }
    if (cacheBlocksOption instanceof String) {
      String value = ((String) cacheBlocksOption).toLowerCase(Locale.ROOT);
      if (value.equals("true")) {
        return true;
      }
      if (value.equals("false")) {
        return false;
      }
    }
    throw new ShellCommandException(
      "Expected CACHE_BLOCKS value to be a boolean or the string 'true' or 'false'");
  }

  private static long resolveInterval(Object intervalOption) throws ShellCommandException {
    if (intervalOption == null) {
      return 1000L;
    }
    return requireNumber(intervalOption, "INTERVAL").longValue();
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

  private static Scan buildScan(Map<String, Object> options) throws ShellCommandException {
    Scan scan = new Scan();
    Object columns = options.get("COLUMNS");
    if (columns != null) {
      for (Object column : asList(columns)) {
        addScanColumn(scan, column.toString());
      }
    }
    Object limit = options.get("LIMIT");
    if (limit != null) {
      scan.setLimit(requireNumber(limit, "LIMIT").intValue());
    }
    Object startRow = options.get("STARTROW");
    if (startRow != null) {
      scan.withStartRow(BinaryStrings.toBytes(startRow.toString()));
    }
    Object stopRow = options.get("STOPROW");
    if (stopRow != null) {
      scan.withStopRow(BinaryStrings.toBytes(stopRow.toString()));
    }
    Object versions = options.get("VERSIONS");
    if (versions != null) {
      scan.readVersions(requireNumber(versions, "VERSIONS").intValue());
    }
    return scan;
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

  /**
   * Guards an option value that must be numeric (e.g. {@code VERSIONS => 'x'}, a typo) so it raises
   * a clear {@link ShellCommandException} instead of an unchecked {@link ClassCastException},
   * matching the guard idiom already used for positional args in {@code DeleteCommand}/
   * {@code IncrCommand}/{@code DeleteallCommand}.
   */
  private static Number requireNumber(Object value, String optionName)
    throws ShellCommandException {
    if (!(value instanceof Number)) {
      throw new ShellCommandException(optionName + " must be numeric: '" + value + "'");
    }
    return (Number) value;
  }

  /**
   * Parses a {@code TIMERANGE => [minStamp, maxStamp]} option value into a {@code [min, max]}
   * {@code long} pair, matching old-shell's plain-array {@code TIMERANGE} semantics.
   */
  private static long[] parseTimeRange(Object timerangeOption) throws ShellCommandException {
    if (!(timerangeOption instanceof List)) {
      throw new ShellCommandException(
        "TIMERANGE must be a two-element array of [minStamp, maxStamp]");
    }
    List<?> range = (List<?>) timerangeOption;
    if (range.size() != 2) {
      throw new ShellCommandException(
        "TIMERANGE must be a two-element array of [minStamp, maxStamp], got " + range.size()
          + " element(s)");
    }
    return new long[] { requireNumber(range.get(0), "TIMERANGE").longValue(),
      requireNumber(range.get(1), "TIMERANGE").longValue() };
  }

  /**
   * Parses a {@code FILTER => "<filter-string>"} option value using {@link ParseFilter}'s textual
   * filter grammar (HBASE-4176), e.g. {@code "PrefixFilter('row')"}. Raw object-construction syntax
   * ({@code FILTER => SomeFilter.new(...)}) is not supported.
   */
  private static Filter parseFilterString(Object filterOption) throws ShellCommandException {
    if (!(filterOption instanceof String)) {
      throw new ShellCommandException(
        "FILTER must be a filter string such as \"PrefixFilter('row')\", got: " + filterOption);
    }
    String filterString = (String) filterOption;
    try {
      return new ParseFilter().parseFilterString(filterString);
    } catch (CharacterCodingException | IllegalArgumentException e) {
      throw new ShellCommandException(
        "Invalid FILTER string '" + filterString + "': " + e.getMessage(), e);
    }
  }

  private static void addScanColumn(Scan scan, String columnSpec) {
    int colonIndex = columnSpec.indexOf(':');
    if (colonIndex < 0) {
      scan.addFamily(BinaryStrings.toBytes(columnSpec));
      return;
    }
    byte[] family = BinaryStrings.toBytes(columnSpec.substring(0, colonIndex));
    String qualifierPart = columnSpec.substring(colonIndex + 1);
    if (qualifierPart.isEmpty()) {
      scan.addFamily(family);
    } else {
      scan.addColumn(family, BinaryStrings.toBytes(qualifierPart));
    }
  }

  private static void addColumn(Get get, String columnSpec) {
    int colonIndex = columnSpec.indexOf(':');
    if (colonIndex < 0) {
      get.addFamily(BinaryStrings.toBytes(columnSpec));
      return;
    }
    byte[] family = BinaryStrings.toBytes(columnSpec.substring(0, colonIndex));
    String qualifierPart = columnSpec.substring(colonIndex + 1);
    if (qualifierPart.isEmpty()) {
      get.addFamily(family);
    } else {
      get.addColumn(family, BinaryStrings.toBytes(qualifierPart));
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object columns) {
    if (columns instanceof List) {
      return (List<Object>) columns;
    }
    return Arrays.asList(columns);
  }
}
