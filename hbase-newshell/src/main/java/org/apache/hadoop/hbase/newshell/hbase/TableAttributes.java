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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.RegionSplitter;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Applies a {@code create}-style table-level attribute hash (a hash literal with no {@code NAME}
 * key, e.g. {@code SPLITS => [...], REGION_REPLICATION => 3}) to a {@link TableDescriptorBuilder}.
 * Ported from the generic dispatch in hbase-shell's {@code hbase/admin.rb#update_tdb_from_arg} -
 * each supported attribute is a single map entry rather than a hand-written
 * {@code if (x != null) builder.setX(...)} branch, same rationale as {@link ColumnFamilyAttributes}.
 * {@code SPLITS}/{@code SPLITS_FILE}/{@code NUMREGIONS}+{@code SPLITALGO} are handled separately
 * from the declarative map since they aren't {@link TableDescriptorBuilder} setters - they are
 * passed to {@code Admin.createTable} as pre-split region boundaries. {@code CONFIGURATION} and
 * {@code METADATA} hashes are applied generically via {@code setValue}. Any attribute key that is
 * neither a recognized setter nor one of the above is rejected with an {@link IOException} rather
 * than silently ignored.
 */
@InterfaceAudience.Private
final class TableAttributes {
  private static final Map<String, BiConsumer<TableDescriptorBuilder, Object>> SETTERS;

  static {
    Map<String, BiConsumer<TableDescriptorBuilder, Object>> m = new HashMap<>();
    m.put("MAX_FILESIZE", (builder, value) -> builder.setMaxFileSize(AttributeCoercion.toLong(value)));
    m.put("MEMSTORE_FLUSHSIZE",
      (builder, value) -> builder.setMemStoreFlushSize(AttributeCoercion.toLong(value)));
    m.put("READONLY", (builder, value) -> builder.setReadOnly(AttributeCoercion.toBoolean(value)));
    m.put("COMPACTION_ENABLED",
      (builder, value) -> builder.setCompactionEnabled(AttributeCoercion.toBoolean(value)));
    m.put("SPLIT_ENABLED",
      (builder, value) -> builder.setSplitEnabled(AttributeCoercion.toBoolean(value)));
    m.put("MERGE_ENABLED",
      (builder, value) -> builder.setMergeEnabled(AttributeCoercion.toBoolean(value)));
    m.put("NORMALIZATION_ENABLED",
      (builder, value) -> builder.setNormalizationEnabled(AttributeCoercion.toBoolean(value)));
    m.put("DURABILITY", (builder, value) -> builder.setDurability(Durability.valueOf(value.toString())));
    m.put("REGION_REPLICATION",
      (builder, value) -> builder.setRegionReplication(AttributeCoercion.toInt(value)));
    m.put("PRIORITY", (builder, value) -> builder.setPriority(AttributeCoercion.toInt(value)));
    SETTERS = Collections.unmodifiableMap(m);
  }

  private TableAttributes() {
  }

  /**
   * Applies every supported non-split attribute in {@code tableAttributes} to {@code builder},
   * then returns the pre-split region boundaries derived from {@code SPLITS}/{@code SPLITS_FILE}/
   * {@code NUMREGIONS}+{@code SPLITALGO} (or {@code null} if none of those were given).
   */
  static byte[][] apply(TableDescriptorBuilder builder, Map<String, Object> tableAttributes,
    Configuration conf) throws IOException {
    Map<String, Object> remaining = new HashMap<>(tableAttributes);
    byte[][] splits = extractSplits(builder, remaining, conf);
    applyValueMap(builder, remaining, "CONFIGURATION");
    applyValueMap(builder, remaining, "METADATA");
    for (Map.Entry<String, Object> entry : remaining.entrySet()) {
      BiConsumer<TableDescriptorBuilder, Object> setter = SETTERS.get(entry.getKey());
      if (setter == null) {
        throw new IOException("Unknown table attribute '" + entry.getKey() + "'");
      }
      try {
        setter.accept(builder, entry.getValue());
      } catch (IllegalArgumentException | ClassCastException e) {
        throw new IOException(
          "Invalid value for table attribute '" + entry.getKey() + "': " + entry.getValue(), e);
      }
    }
    return splits;
  }

  /**
   * Mirrors hbase-shell's create split handling (admin.rb): an explicit {@code SPLITS} list (or
   * {@code SPLITS_FILE}) takes precedence and any {@code NUMREGIONS}/{@code SPLITALGO} pair is
   * ignored; otherwise, when either is present both are required, {@code NUMREGIONS} must be
   * &gt; 1, and the named {@link RegionSplitter.SplitAlgorithm} computes the split points.
   */
  private static byte[][] extractSplits(TableDescriptorBuilder builder,
    Map<String, Object> tableAttributes, Configuration conf) throws IOException {
    Object splitsFile = tableAttributes.remove("SPLITS_FILE");
    Object rawSplits = tableAttributes.remove("SPLITS");
    if (splitsFile != null) {
      rawSplits = readSplitsFile(splitsFile.toString());
      builder.setValue("SPLITS_FILE", splitsFile.toString());
    }
    Object numRegions = tableAttributes.remove("NUMREGIONS");
    Object splitAlgo = tableAttributes.remove("SPLITALGO");
    if (rawSplits != null) {
      try {
        return toSplits(rawSplits);
      } catch (ClassCastException | IllegalArgumentException e) {
        throw new IOException("Invalid value for table attribute 'SPLITS': " + rawSplits, e);
      }
    }
    if (numRegions == null && splitAlgo == null) {
      return null;
    }
    if (numRegions == null) {
      throw new IOException("Number of regions must be specified via NUMREGIONS");
    }
    if (splitAlgo == null) {
      throw new IOException("Split algorithm must be specified via SPLITALGO");
    }
    int numRegionsValue;
    try {
      numRegionsValue = AttributeCoercion.toInt(numRegions);
    } catch (ClassCastException e) {
      throw new IOException("Invalid value for table attribute 'NUMREGIONS': " + numRegions, e);
    }
    if (numRegionsValue <= 1) {
      throw new IOException("NUMREGIONS must be greater than 1");
    }
    RegionSplitter.SplitAlgorithm algorithm =
      RegionSplitter.newSplitAlgoInstance(conf, splitAlgo.toString());
    return algorithm.split(numRegionsValue);
  }

  private static void applyValueMap(TableDescriptorBuilder builder, Map<String, Object> remaining,
    String key) throws IOException {
    Object rawMap = remaining.remove(key);
    if (rawMap == null) {
      return;
    }
    if (!(rawMap instanceof Map)) {
      throw new IOException("Table attribute '" + key + "' must be a map, but was: " + rawMap);
    }
    for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawMap).entrySet()) {
      Object value = entry.getValue();
      builder.setValue(entry.getKey().toString(), value == null ? null : value.toString());
    }
  }

  private static List<String> readSplitsFile(String path) throws IOException {
    File file = new File(path);
    if (!file.exists()) {
      throw new IOException("Splits file " + path + " doesn't exist");
    }
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines;
  }

  private static byte[][] toSplits(Object value) {
    List<?> rawSplits = (List<?>) value;
    byte[][] splits = new byte[rawSplits.size()][];
    for (int i = 0; i < rawSplits.size(); i++) {
      splits[i] = Bytes.toBytesBinary(rawSplits.get(i).toString());
    }
    return splits;
  }
}
