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
package org.apache.hadoop.hbase.newshell.command;

import java.util.List;
import java.util.Objects;
import org.apache.yetus.audience.InterfaceAudience;

/** A header row plus data rows - e.g. {@code get}'s COLUMN/CELL table. */
@InterfaceAudience.Private
public final class TabularResult implements CommandResult {
  private final List<String> header;
  private final List<List<String>> rows;

  public TabularResult(List<String> header, List<List<String>> rows) {
    this.header = header;
    this.rows = rows;
  }

  public List<String> header() {
    return header;
  }

  public List<List<String>> rows() {
    return rows;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TabularResult)) {
      return false;
    }
    TabularResult other = (TabularResult) o;
    return Objects.equals(header, other.header) && Objects.equals(rows, other.rows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(header, rows);
  }

  @Override
  public String toString() {
    return "TabularResult[header=" + header + ", rows=" + rows + "]";
  }
}
