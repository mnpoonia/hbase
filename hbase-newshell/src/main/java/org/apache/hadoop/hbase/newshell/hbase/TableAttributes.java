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
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Applies a {@code create}-style table-level attribute hash (a hash literal with no {@code NAME}
 * key, e.g. {@code SPLITS => [...], REGION_REPLICATION => 3}) to a {@link TableDescriptorBuilder}.
 * Ported from the generic dispatch in hbase-shell's {@code hbase/admin.rb#update_tdb_from_arg} -
 * each supported attribute is a single map entry rather than a hand-written
 * {@code if (x != null) builder.setX(...)} branch, same rationale as {@link ColumnFamilyAttributes}.
 * {@code SPLITS} is handled separately from the declarative map since it isn't a
 * {@link TableDescriptorBuilder} setter - it is passed to {@code Admin.createTable} as pre-split
 * region boundaries. Attributes not listed here (e.g. {@code CONFIGURATION}, coprocessors,
 * erasure coding, normalizer sizing, split/flush policy class names) are explicitly out of scope
 * for the pilot slice and are silently ignored.
 */
@InterfaceAudience.Private
final class TableAttributes {
  private static final Map<String, BiConsumer<TableDescriptorBuilder, Object>> SETTERS = Map.of(
    "MAX_FILESIZE", (builder, value) -> builder.setMaxFileSize(toLong(value)),
    "MEMSTORE_FLUSHSIZE", (builder, value) -> builder.setMemStoreFlushSize(toLong(value)),
    "READONLY", (builder, value) -> builder.setReadOnly(toBoolean(value)),
    "COMPACTION_ENABLED", (builder, value) -> builder.setCompactionEnabled(toBoolean(value)),
    "SPLIT_ENABLED", (builder, value) -> builder.setSplitEnabled(toBoolean(value)),
    "MERGE_ENABLED", (builder, value) -> builder.setMergeEnabled(toBoolean(value)),
    "NORMALIZATION_ENABLED", (builder, value) -> builder.setNormalizationEnabled(toBoolean(value)),
    "DURABILITY", (builder, value) -> builder.setDurability(Durability.valueOf(value.toString())),
    "REGION_REPLICATION", (builder, value) -> builder.setRegionReplication(toInt(value)),
    "PRIORITY", (builder, value) -> builder.setPriority(toInt(value)));

  private TableAttributes() {
  }

  /**
   * Applies every supported non-{@code SPLITS} attribute in {@code tableAttributes} to
   * {@code builder}, then returns the pre-split region boundaries from {@code SPLITS} (or
   * {@code null} if not present).
   */
  static byte[][] apply(TableDescriptorBuilder builder, Map<String, Object> tableAttributes) {
    byte[][] splits = null;
    for (Map.Entry<String, Object> entry : tableAttributes.entrySet()) {
      if (entry.getKey().equals("SPLITS")) {
        splits = toSplits(entry.getValue());
        continue;
      }
      BiConsumer<TableDescriptorBuilder, Object> setter = SETTERS.get(entry.getKey());
      if (setter != null) {
        setter.accept(builder, entry.getValue());
      }
    }
    return splits;
  }

  private static byte[][] toSplits(Object value) {
    List<?> rawSplits = (List<?>) value;
    byte[][] splits = new byte[rawSplits.size()][];
    for (int i = 0; i < rawSplits.size(); i++) {
      splits[i] = Bytes.toBytesBinary(rawSplits.get(i).toString());
    }
    return splits;
  }

  private static int toInt(Object value) {
    return ((Number) value).intValue();
  }

  private static long toLong(Object value) {
    return ((Number) value).longValue();
  }

  private static boolean toBoolean(Object value) {
    return Boolean.parseBoolean(value.toString());
  }
}
