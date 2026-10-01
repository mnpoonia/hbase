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

/**
 * Rows plus optional stdout warnings for {@code list_regions}, matching the Ruby shell's "Can not
 * find all details for region: …" messages when region metrics are missing.
 */
@InterfaceAudience.Private
public final class ListRegionsView {
  private final List<String> warnings;
  private final List<List<String>> rows;

  public ListRegionsView(List<String> warnings, List<List<String>> rows) {
    this.warnings = warnings;
    this.rows = rows;
  }

  public List<String> warnings() {
    return warnings;
  }

  public List<List<String>> rows() {
    return rows;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ListRegionsView)) {
      return false;
    }
    ListRegionsView other = (ListRegionsView) o;
    return Objects.equals(warnings, other.warnings) && Objects.equals(rows, other.rows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(warnings, rows);
  }

  @Override
  public String toString() {
    return "ListRegionsView[warnings=" + warnings + ", rows=" + rows + "]";
  }
}
