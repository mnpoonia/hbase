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
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Wraps a single real {@link Table}. Ported, for the {@code get}/{@code put} pilot commands, from
 * hbase-shell's {@code hbase/table.rb#_get_internal} - handles {@code COLUMN} (including
 * {@code cf:qualifier} splitting and array-of-columns values), {@code VERSIONS}, and
 * {@code TIMESTAMP} - and {@code hbase/table.rb#_put_internal} - a single {@code cf:qualifier}
 * column, required, plus an optional {@code TIMESTAMP}. FILTER/ATTRIBUTES/AUTHORIZATIONS/
 * CONSISTENCY/TIMERANGE (for {@code get}) and ATTRIBUTES/VISIBILITY/TTL (for {@code put}) are
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
