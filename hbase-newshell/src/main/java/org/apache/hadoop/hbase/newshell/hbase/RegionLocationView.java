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

/** Ports {@code shell/commands/locate_region.rb}'s host-plus-region-name view. */
@InterfaceAudience.Private
public final class RegionLocationView {
  private final String hostnamePort;
  private final String regionName;

  public RegionLocationView(String hostnamePort, String regionName) {
    this.hostnamePort = hostnamePort;
    this.regionName = regionName;
  }

  public String hostnamePort() {
    return hostnamePort;
  }

  public String regionName() {
    return regionName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RegionLocationView)) {
      return false;
    }
    RegionLocationView other = (RegionLocationView) o;
    return Objects.equals(hostnamePort, other.hostnamePort)
      && Objects.equals(regionName, other.regionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostnamePort, regionName);
  }

  @Override
  public String toString() {
    return "RegionLocationView[hostnamePort=" + hostnamePort + ", regionName=" + regionName + "]";
  }
}
