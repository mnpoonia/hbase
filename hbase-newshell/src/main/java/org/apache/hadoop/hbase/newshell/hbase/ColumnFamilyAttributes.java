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
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.hadoop.hbase.KeepDeletedCells;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Builds a real {@link ColumnFamilyDescriptor} from a {@code create}-style family spec map
 * ({@code NAME} required, everything else optional). Each supported attribute is a single map entry
 * below rather than a hand-written {@code if (x != null) builder.setX(...)} branch, so adding
 * coverage for another {@code hbase/admin.rb#cfd} attribute (see hbase-shell's own generic
 * hash-driven column descriptor builder) is a one-line addition here, not a new branch spread
 * across {@link DefaultShellAdmin}. {@code CONFIGURATION} and {@code METADATA} hashes are applied
 * generically via {@code setConfiguration}/{@code setValue}. Any attribute key that is neither a
 * recognized setter nor {@code CONFIGURATION}/{@code METADATA} is rejected with an
 * {@link IOException} rather than silently ignored.
 */
@InterfaceAudience.Private
final class ColumnFamilyAttributes {
  private static final Map<String, BiConsumer<ColumnFamilyDescriptorBuilder, Object>> SETTERS;

  static {
    Map<String, BiConsumer<ColumnFamilyDescriptorBuilder, Object>> m = new HashMap<>();
    m.put("VERSIONS", (builder, value) -> builder.setMaxVersions(AttributeCoercion.toInt(value)));
    m.put("MIN_VERSIONS",
      (builder, value) -> builder.setMinVersions(AttributeCoercion.toInt(value)));
    m.put("TTL", (builder, value) -> builder.setTimeToLive(AttributeCoercion.toInt(value)));
    m.put("BLOCKCACHE",
      (builder, value) -> builder.setBlockCacheEnabled(AttributeCoercion.toBoolean(value)));
    m.put("IN_MEMORY", (builder, value) -> builder.setInMemory(AttributeCoercion.toBoolean(value)));
    m.put("COMPRESSION", (builder, value) -> builder
      .setCompressionType(Compression.Algorithm.valueOf(toUpper(value))));
    m.put("BLOOMFILTER",
      (builder, value) -> builder.setBloomFilterType(BloomType.valueOf(toUpper(value))));
    m.put("DATA_BLOCK_ENCODING",
      (builder, value) -> builder.setDataBlockEncoding(DataBlockEncoding.valueOf(toUpper(value))));
    m.put("REPLICATION_SCOPE",
      (builder, value) -> builder.setScope(AttributeCoercion.toInt(value)));
    m.put("KEEP_DELETED_CELLS",
      (builder, value) -> builder.setKeepDeletedCells(KeepDeletedCells.valueOf(toUpper(value))));
    m.put("NEW_VERSION_BEHAVIOR",
      (builder, value) -> builder.setNewVersionBehavior(AttributeCoercion.toBoolean(value)));
    m.put("IS_MOB", (builder, value) -> builder.setMobEnabled(AttributeCoercion.toBoolean(value)));
    m.put("MOB_THRESHOLD",
      (builder, value) -> builder.setMobThreshold(AttributeCoercion.toLong(value)));
    m.put("DFS_REPLICATION",
      (builder, value) -> builder.setDFSReplication((short) AttributeCoercion.toInt(value)));
    m.put("BLOCKSIZE", (builder, value) -> builder.setBlocksize(AttributeCoercion.toInt(value)));
    m.put("CACHE_DATA_ON_WRITE",
      (builder, value) -> builder.setCacheDataOnWrite(AttributeCoercion.toBoolean(value)));
    SETTERS = Collections.unmodifiableMap(m);
  }

  private ColumnFamilyAttributes() {
  }

  static ColumnFamilyDescriptor build(Map<String, Object> familySpec) throws IOException {
    Object name = familySpec.get("NAME");
    if (name == null) {
      throw new IOException("Column family spec requires a NAME");
    }
    try {
      ColumnFamilyDescriptorBuilder builder =
        ColumnFamilyDescriptorBuilder.newBuilder(name.toString().getBytes(StandardCharsets.UTF_8));
      applyAttributes(builder, familySpec);
      return builder.build();
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid column family name or attribute: " + e.getMessage(), e);
    }
  }

  /**
   * Applies every non-{@code NAME} attribute in {@code familySpec} onto {@code builder}, leaving
   * attributes not present in the spec untouched - used by {@code alter} to modify an existing
   * {@link ColumnFamilyDescriptor} in place rather than building a fresh one.
   */
  static void applyAttributes(ColumnFamilyDescriptorBuilder builder, Map<String, Object> familySpec)
    throws IOException {
    Map<String, Object> remaining = new HashMap<>(familySpec);
    remaining.remove("NAME");
    applyValueMap(builder, remaining, "CONFIGURATION", true);
    applyValueMap(builder, remaining, "METADATA", false);
    for (Map.Entry<String, Object> entry : remaining.entrySet()) {
      BiConsumer<ColumnFamilyDescriptorBuilder, Object> setter = SETTERS.get(entry.getKey());
      if (setter == null) {
        throw new IOException("Unknown column family attribute '" + entry.getKey() + "'");
      }
      try {
        setter.accept(builder, entry.getValue());
      } catch (IllegalArgumentException | ClassCastException e) {
        throw new IOException(
          "Invalid value for column family attribute '" + entry.getKey() + "': " + entry.getValue(),
          e);
      }
    }
  }

  private static void applyValueMap(ColumnFamilyDescriptorBuilder builder,
    Map<String, Object> remaining, String key, boolean asConfiguration) throws IOException {
    Object rawMap = remaining.remove(key);
    if (rawMap == null) {
      return;
    }
    if (!(rawMap instanceof Map)) {
      throw new IOException(
        "Column family attribute '" + key + "' must be a map, but was: " + rawMap);
    }
    for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawMap).entrySet()) {
      Object value = entry.getValue();
      String stringValue = value == null ? null : value.toString();
      if (asConfiguration) {
        builder.setConfiguration(entry.getKey().toString(), stringValue);
      } else {
        builder.setValue(entry.getKey().toString(), stringValue);
      }
    }
  }

  private static String toUpper(Object value) {
    return value.toString().toUpperCase(Locale.ROOT);
  }
}
