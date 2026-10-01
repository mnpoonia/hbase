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

/** A plain-Java view of one RegionServer group's servers and tables. */
@InterfaceAudience.Private
public final class RsGroupView {
  private final List<String> servers;
  private final List<String> tables;

  public RsGroupView(List<String> servers, List<String> tables) {
    this.servers = servers;
    this.tables = tables;
  }

  public List<String> servers() {
    return servers;
  }

  public List<String> tables() {
    return tables;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RsGroupView)) {
      return false;
    }
    RsGroupView other = (RsGroupView) o;
    return Objects.equals(servers, other.servers) && Objects.equals(tables, other.tables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(servers, tables);
  }

  @Override
  public String toString() {
    return "RsGroupView[servers=" + servers + ", tables=" + tables + "]";
  }
}
