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

@InterfaceAudience.Private
public final class TableDescription {
  private final boolean enabled;
  private final String tableAttributes;
  private final List<String> columnFamilies;

  public TableDescription(boolean enabled, String tableAttributes, List<String> columnFamilies) {
    this.enabled = enabled;
    this.tableAttributes = tableAttributes;
    this.columnFamilies = columnFamilies;
  }

  public boolean enabled() {
    return enabled;
  }

  public String tableAttributes() {
    return tableAttributes;
  }

  public List<String> columnFamilies() {
    return columnFamilies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TableDescription)) {
      return false;
    }
    TableDescription other = (TableDescription) o;
    return enabled == other.enabled && Objects.equals(tableAttributes, other.tableAttributes)
      && Objects.equals(columnFamilies, other.columnFamilies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, tableAttributes, columnFamilies);
  }

  @Override
  public String toString() {
    return "TableDescription[enabled=" + enabled + ", tableAttributes=" + tableAttributes
      + ", columnFamilies=" + columnFamilies + "]";
  }
}
