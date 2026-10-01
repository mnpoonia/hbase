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

/**
 * A plain-Java view of one HBase {@code Cell} - deliberately holds binary-safe string fields rather
 * than a raw {@code org.apache.hadoop.hbase.Cell}, so no HBase client type leaks past the
 * {@code hbase} wrapper package into the command/format layers.
 */
@InterfaceAudience.Private
public final class CellView {
  private final String family;
  private final String qualifier;
  private final long timestamp;
  private final String value;

  public CellView(String family, String qualifier, long timestamp, String value) {
    this.family = family;
    this.qualifier = qualifier;
    this.timestamp = timestamp;
    this.value = value;
  }

  public String family() {
    return family;
  }

  public String qualifier() {
    return qualifier;
  }

  public long timestamp() {
    return timestamp;
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CellView)) {
      return false;
    }
    CellView other = (CellView) o;
    return timestamp == other.timestamp && Objects.equals(family, other.family)
      && Objects.equals(qualifier, other.qualifier) && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(family, qualifier, timestamp, value);
  }

  @Override
  public String toString() {
    return "CellView[family=" + family + ", qualifier=" + qualifier + ", timestamp=" + timestamp
      + ", value=" + value + "]";
  }
}
