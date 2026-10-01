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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.ParseFilter;
import org.apache.hadoop.hbase.security.visibility.CellVisibility;

/** Package-private collaborator used by {@link DefaultShellAdmin} for set_visibility. */
final class VisibilityOps {
  private VisibilityOps() {
  }

  static long setVisibility(Connection connection, String tableName, String visibility,
    Map<String, Object> options) throws IOException {
    Scan scan = buildScan(options == null ? Collections.emptyMap() : options);
    long count = 0;
    try (Table table = connection.getTable(TableName.valueOf(tableName));
      ResultScanner scanner = table.getScanner(scan)) {
      for (Result result : scanner) {
        List<Cell> cells = result.listCells();
        if (cells == null) {
          continue;
        }
        for (Cell cell : cells) {
          Put put = new Put(result.getRow());
          put.add(cell);
          put.setCellVisibility(new CellVisibility(visibility));
          table.put(put);
        }
        count++;
      }
    }
    return count;
  }

  private static Scan buildScan(Map<String, Object> options) throws IOException {
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
    Object prefix = options.get("ROWPREFIXFILTER");
    if (prefix != null) {
      scan.setStartStopRowForPrefixScan(prefix.toString().getBytes(StandardCharsets.UTF_8));
    }
    Object versions = options.get("VERSIONS");
    if (versions != null) {
      scan.readVersions(((Number) versions).intValue());
    }
    Object timestamp = options.get("TIMESTAMP");
    if (timestamp != null) {
      scan.setTimestamp(((Number) timestamp).longValue());
    }
    Object timerange = options.get("TIMERANGE");
    if (timerange instanceof List<?> && ((List<?>) timerange).size() == 2) {
      List<?> range = (List<?>) timerange;
      scan.setTimeRange(((Number) range.get(0)).longValue(), ((Number) range.get(1)).longValue());
    }
    Object filter = options.get("FILTER");
    if (filter != null) {
      scan.setFilter(new ParseFilter().parseFilterString(filter.toString()));
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

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object columns) {
    if (columns instanceof List) {
      return (List<Object>) columns;
    }
    return Arrays.asList(columns);
  }
}
