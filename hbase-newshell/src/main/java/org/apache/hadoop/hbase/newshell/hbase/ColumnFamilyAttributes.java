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
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Builds a real {@link ColumnFamilyDescriptor} from a {@code create}-style family spec map
 * ({@code NAME} required, everything else optional). Each supported attribute is a single map
 * entry below rather than a hand-written {@code if (x != null) builder.setX(...)} branch, so
 * adding coverage for another {@code hbase/admin.rb#cfd} attribute (see hbase-shell's own
 * generic hash-driven column descriptor builder) is a one-line addition here, not a new branch
 * spread across {@link DefaultShellAdmin}. Attributes not listed here (e.g. {@code CONFIGURATION},
 * MOB-related keys) are still explicitly out of scope for the pilot slice and are silently
 * ignored, same as before this refactor.
 */
@InterfaceAudience.Private
final class ColumnFamilyAttributes {
  private static final Map<String, BiConsumer<ColumnFamilyDescriptorBuilder, Object>> SETTERS = Map.of(
    "VERSIONS", (builder, value) -> builder.setMaxVersions(toInt(value)),
    "MIN_VERSIONS", (builder, value) -> builder.setMinVersions(toInt(value)),
    "TTL", (builder, value) -> builder.setTimeToLive(toInt(value)),
    "BLOCKCACHE", (builder, value) -> builder.setBlockCacheEnabled(toBoolean(value)),
    "IN_MEMORY", (builder, value) -> builder.setInMemory(toBoolean(value)),
    "COMPRESSION",
    (builder, value) -> builder.setCompressionType(Compression.Algorithm.valueOf(toUpper(value))),
    "BLOOMFILTER", (builder, value) -> builder.setBloomFilterType(BloomType.valueOf(toUpper(value))),
    "DATA_BLOCK_ENCODING",
    (builder, value) -> builder.setDataBlockEncoding(DataBlockEncoding.valueOf(toUpper(value))));

  private ColumnFamilyAttributes() {
  }

  static ColumnFamilyDescriptor build(Map<String, Object> familySpec) throws IOException {
    Object name = familySpec.get("NAME");
    if (name == null) {
      throw new IOException("Column family spec requires a NAME");
    }
    ColumnFamilyDescriptorBuilder builder =
      ColumnFamilyDescriptorBuilder.newBuilder(name.toString().getBytes(StandardCharsets.UTF_8));
    applyAttributes(builder, familySpec);
    return builder.build();
  }

  /**
   * Applies every non-{@code NAME} attribute in {@code familySpec} onto {@code builder}, leaving
   * attributes not present in the spec untouched - used by {@code alter} to modify an existing
   * {@link ColumnFamilyDescriptor} in place rather than building a fresh one.
   */
  static void applyAttributes(ColumnFamilyDescriptorBuilder builder, Map<String, Object> familySpec) {
    for (Map.Entry<String, Object> entry : familySpec.entrySet()) {
      if (entry.getKey().equals("NAME")) {
        continue;
      }
      BiConsumer<ColumnFamilyDescriptorBuilder, Object> setter = SETTERS.get(entry.getKey());
      if (setter != null) {
        setter.accept(builder, entry.getValue());
      }
    }
  }

  private static int toInt(Object value) {
    return ((Number) value).intValue();
  }

  private static boolean toBoolean(Object value) {
    return Boolean.parseBoolean(value.toString());
  }

  private static String toUpper(Object value) {
    return value.toString().toUpperCase(Locale.ROOT);
  }
}
