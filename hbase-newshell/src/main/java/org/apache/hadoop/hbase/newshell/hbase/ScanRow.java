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

import java.util.List;
import java.util.Objects;
import org.apache.yetus.audience.InterfaceAudience;

/** One row of a {@code scan} result: its row key plus the cells returned for it. */
@InterfaceAudience.Private
public final class ScanRow {
  private final String row;
  private final List<CellView> cells;

  public ScanRow(String row, List<CellView> cells) {
    this.row = row;
    this.cells = cells;
  }

  public String row() {
    return row;
  }

  public List<CellView> cells() {
    return cells;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ScanRow)) {
      return false;
    }
    ScanRow other = (ScanRow) o;
    return Objects.equals(row, other.row) && Objects.equals(cells, other.cells);
  }

  @Override
  public int hashCode() {
    return Objects.hash(row, cells);
  }

  @Override
  public String toString() {
    return "ScanRow[row=" + row + ", cells=" + cells + "]";
  }
}
