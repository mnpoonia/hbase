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

import java.util.Objects;
import org.apache.yetus.audience.InterfaceAudience;

/** Ports {@code Admin#getAlterStatus}'s pair: regions still pending the new schema, and total. */
@InterfaceAudience.Private
public final class AlterStatusView {
  private final int regionsYetToUpdate;
  private final int totalRegions;

  public AlterStatusView(int regionsYetToUpdate, int totalRegions) {
    this.regionsYetToUpdate = regionsYetToUpdate;
    this.totalRegions = totalRegions;
  }

  public int regionsYetToUpdate() {
    return regionsYetToUpdate;
  }

  public int totalRegions() {
    return totalRegions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AlterStatusView)) {
      return false;
    }
    AlterStatusView other = (AlterStatusView) o;
    return regionsYetToUpdate == other.regionsYetToUpdate && totalRegions == other.totalRegions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(regionsYetToUpdate, totalRegions);
  }

  @Override
  public String toString() {
    return "AlterStatusView[regionsYetToUpdate=" + regionsYetToUpdate + ", totalRegions="
      + totalRegions + "]";
  }
}
