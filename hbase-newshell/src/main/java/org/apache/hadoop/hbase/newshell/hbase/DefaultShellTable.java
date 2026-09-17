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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Wraps a single real {@link Table}. Ported, for the pilot commands, from hbase-shell's
 * {@code hbase/table.rb#_get_internal} - handles {@code COLUMN} (including {@code cf:qualifier}
 * splitting and array-of-columns values), {@code VERSIONS}, and {@code TIMESTAMP} - and
 * {@code hbase/table.rb#_put_internal} - a single {@code cf:qualifier} column, required, plus an
 * optional {@code TIMESTAMP}; {@code _count_internal} (row count only, no {@code INTERVAL}
 * progress reporting, no {@code FILTER}); {@code _delete_internal}/{@code _deleteall_internal}
 * (single-version vs all-versions column delete, plus {@code ROWPREFIXFILTER}/{@code CACHE}
 * batched range delete); {@code _get_counter_internal}/{@code _incr_internal}/
 * {@code _append_internal}; and {@code _get_splits_internal}. FILTER/ATTRIBUTES/AUTHORIZATIONS/
 * CONSISTENCY/TIMERANGE (for {@code get}), ATTRIBUTES/VISIBILITY/TTL (for {@code put}) are
 * explicitly not ported.
 */
@InterfaceAudience.Private
public final class DefaultShellTable implements ShellTable {
  private final Table table;

  public DefaultShellTable(Table table) {
    this.table = table;
  }

  @Override
  public GetResult get(String row, Map<String, Object> options) throws IOException {
    Get get = new Get(row.getBytes(StandardCharsets.UTF_8));
    Object columns = options.get("COLUMN");
    if (columns != null) {
      for (Object column : asList(columns)) {
        addColumn(get, column.toString());
      }
    }
    Object versions = options.get("VERSIONS");
    if (versions != null) {
      get.readVersions(((Number) versions).intValue());
    }
    Object timestamp = options.get("TIMESTAMP");
    if (timestamp != null) {
      get.setTimestamp(((Number) timestamp).longValue());
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
    throws IOException {
    int colonIndex = column.indexOf(':');
    if (colonIndex < 0 || colonIndex == column.length() - 1) {
      throw new IOException("Column '" + column + "' must be of the form 'family:qualifier'");
    }
    byte[] family = column.substring(0, colonIndex).getBytes(StandardCharsets.UTF_8);
    byte[] qualifier = column.substring(colonIndex + 1).getBytes(StandardCharsets.UTF_8);
    Put put = new Put(row.getBytes(StandardCharsets.UTF_8));
    Object timestamp = options.get("TIMESTAMP");
    if (timestamp != null) {
      put.addColumn(family, qualifier, ((Number) timestamp).longValue(),
        value.getBytes(StandardCharsets.UTF_8));
    } else {
      put.addColumn(family, qualifier, value.getBytes(StandardCharsets.UTF_8));
    }
    table.put(put);
  }

  @Override
  public ScanResult scan(Map<String, Object> options) throws IOException {
    Scan scan = buildScan(options);
    List<ScanRow> rows = new ArrayList<>();
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
        rows.add(new ScanRow(Bytes.toStringBinary(result.getRow()), cells));
      }
    }
    return new ScanResult(rows);
  }

  @Override
  public long count(Map<String, Object> options) throws IOException {
    Scan scan = buildScan(options);
    long count = 0;
    try (ResultScanner scanner = table.getScanner(scan)) {
      for (Result ignored : scanner) {
        count++;
      }
    }
    return count;
  }

  @Override
  public void delete(String row, String column, Long timestamp) throws IOException {
    long ts = timestamp == null ? HConstants.LATEST_TIMESTAMP : timestamp;
    Delete delete = new Delete(row.getBytes(StandardCharsets.UTF_8), ts);
    addDeleteColumn(delete, column, ts, false);
    table.delete(delete);
  }

  @Override
  public void deleteAll(String row, String column, Long timestamp, Map<String, Object> options)
    throws IOException {
    long ts = timestamp == null ? HConstants.LATEST_TIMESTAMP : timestamp;
    Object prefix = options.get("ROWPREFIXFILTER");
    if (prefix != null) {
      Object cacheOption = options.get("CACHE");
      int cache = cacheOption == null ? 100 : ((Number) cacheOption).intValue();
      byte[] prefixBytes = prefix.toString().getBytes(StandardCharsets.UTF_8);
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
    Delete delete = new Delete(row.getBytes(StandardCharsets.UTF_8), ts);
    addDeleteColumn(delete, column, ts, true);
    table.delete(delete);
  }

  @Override
  public Long getCounter(String row, String column) throws IOException {
    String[] parts = requireFamilyAndQualifier(column);
    Get get = new Get(row.getBytes(StandardCharsets.UTF_8));
    get.addColumn(Bytes.toBytes(parts[0]), Bytes.toBytes(parts[1]));
    get.readVersions(1);
    Result result = table.get(get);
    return decodeLong(result);
  }

  @Override
  public Long increment(String row, String column, long amount) throws IOException {
    String[] parts = requireFamilyAndQualifier(column);
    Increment increment = new Increment(row.getBytes(StandardCharsets.UTF_8));
    increment.addColumn(Bytes.toBytes(parts[0]), Bytes.toBytes(parts[1]), amount);
    Result result = table.increment(increment);
    return decodeLong(result);
  }

  @Override
  public String append(String row, String column, String value) throws IOException {
    String[] parts = requireFamilyAndQualifier(column);
    Append append = new Append(row.getBytes(StandardCharsets.UTF_8));
    append.addColumn(Bytes.toBytes(parts[0]), Bytes.toBytes(parts[1]),
      value.getBytes(StandardCharsets.UTF_8));
    Result result = table.append(append);
    if (result.isEmpty()) {
      return null;
    }
    Cell cell = result.listCells().get(0);
    return Bytes.toStringBinary(cell.getValueArray(), cell.getValueOffset(),
      cell.getValueLength());
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
      family = column.getBytes(StandardCharsets.UTF_8);
    } else {
      family = column.substring(0, colonIndex).getBytes(StandardCharsets.UTF_8);
      String qualifierPart = column.substring(colonIndex + 1);
      if (!qualifierPart.isEmpty()) {
        qualifier = qualifierPart.getBytes(StandardCharsets.UTF_8);
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

  private static Scan buildScan(Map<String, Object> options) {
    Scan scan = new Scan();
    Object columns = options.get("COLUMNS");
    if (columns != null) {
      for (Object column : asList(columns)) {
        addScanColumn(scan, column.toString());
      }
    }
    Object limit = options.get("LIMIT");
    if (limit != null) {
      scan.setLimit(((Number) limit).intValue());
    }
    Object startRow = options.get("STARTROW");
    if (startRow != null) {
      scan.withStartRow(startRow.toString().getBytes(StandardCharsets.UTF_8));
    }
    Object stopRow = options.get("STOPROW");
    if (stopRow != null) {
      scan.withStopRow(stopRow.toString().getBytes(StandardCharsets.UTF_8));
    }
    Object versions = options.get("VERSIONS");
    if (versions != null) {
      scan.readVersions(((Number) versions).intValue());
    }
    return scan;
  }

  private static void addScanColumn(Scan scan, String columnSpec) {
    int colonIndex = columnSpec.indexOf(':');
    if (colonIndex < 0) {
      scan.addFamily(columnSpec.getBytes(StandardCharsets.UTF_8));
      return;
    }
    byte[] family = columnSpec.substring(0, colonIndex).getBytes(StandardCharsets.UTF_8);
    String qualifierPart = columnSpec.substring(colonIndex + 1);
    if (qualifierPart.isEmpty()) {
      scan.addFamily(family);
    } else {
      scan.addColumn(family, qualifierPart.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void addColumn(Get get, String columnSpec) {
    int colonIndex = columnSpec.indexOf(':');
    if (colonIndex < 0) {
      get.addFamily(columnSpec.getBytes(StandardCharsets.UTF_8));
      return;
    }
    byte[] family = columnSpec.substring(0, colonIndex).getBytes(StandardCharsets.UTF_8);
    String qualifierPart = columnSpec.substring(colonIndex + 1);
    if (qualifierPart.isEmpty()) {
      get.addFamily(family);
    } else {
      get.addColumn(family, qualifierPart.getBytes(StandardCharsets.UTF_8));
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object columns) {
    if (columns instanceof List) {
      return (List<Object>) columns;
    }
    return List.of(columns);
  }
}
