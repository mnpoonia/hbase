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
import org.apache.hadoop.hbase.client.CoprocessorDescriptor;
import org.apache.hadoop.hbase.client.CoprocessorDescriptorBuilder;
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
 * {@code if (x != null) builder.setX(...)} branch, same rationale as
 * {@link ColumnFamilyAttributes}.
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
    m.put("MAX_FILESIZE",
      (builder, value) -> builder.setMaxFileSize(AttributeCoercion.toLong(value)));
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
    m.put("DURABILITY",
      (builder, value) -> builder.setDurability(Durability.valueOf(value.toString())));
    m.put("REGION_REPLICATION",
      (builder, value) -> builder.setRegionReplication(AttributeCoercion.toInt(value)));
    m.put("PRIORITY", (builder, value) -> builder.setPriority(AttributeCoercion.toInt(value)));
    m.put("NORMALIZER_TARGET_REGION_COUNT",
      (builder, value) -> builder.setNormalizerTargetRegionCount(AttributeCoercion.toInt(value)));
    m.put("NORMALIZER_TARGET_REGION_SIZE",
      (builder, value) -> builder.setNormalizerTargetRegionSize(AttributeCoercion.toLong(value)));
    m.put("NORMALIZER_TARGET_REGION_SIZE_MB",
      (builder, value) -> builder.setNormalizerTargetRegionSize(AttributeCoercion.toLong(value)));
    m.put("ERASURE_CODING_POLICY",
      (builder, value) -> builder.setErasureCodingPolicy(value.toString()));
    m.put("FLUSH_POLICY", (builder, value) -> builder.setFlushPolicyClassName(value.toString()));
    m.put("SPLIT_POLICY",
      (builder, value) -> builder.setRegionSplitPolicyClassName(value.toString()));
    m.put("REGION_MEMSTORE_REPLICATION",
      (builder, value) -> builder.setRegionMemStoreReplication(AttributeCoercion.toBoolean(value)));
    SETTERS = Collections.unmodifiableMap(m);
  }

  private TableAttributes() {
  }

  /**
   * Applies every supported non-split attribute in {@code tableAttributes} to {@code builder}, then
   * returns the pre-split region boundaries derived from {@code SPLITS}/{@code SPLITS_FILE}/
   * {@code NUMREGIONS}+{@code SPLITALGO} (or {@code null} if none of those were given).
   */
  static byte[][] apply(TableDescriptorBuilder builder, Map<String, Object> tableAttributes,
    Configuration conf) throws IOException {
    Map<String, Object> remaining = new HashMap<>(tableAttributes);
    // METHOD => 'table_att' is the deprecated marker for a table-attribute hash
    if ("table_att".equals(remaining.get("METHOD"))) {
      remaining.remove("METHOD");
    }
    byte[][] splits = extractSplits(builder, remaining, conf);
    applyValueMap(builder, remaining, "CONFIGURATION");
    applyValueMap(builder, remaining, "METADATA");
    for (Map.Entry<String, Object> entry : remaining.entrySet()) {
      BiConsumer<TableDescriptorBuilder, Object> setter = SETTERS.get(entry.getKey());
      if (setter == null) {
        throw new ClientErrorException("Unknown table attribute '" + entry.getKey() + "'");
      }
      try {
        setter.accept(builder, entry.getValue());
      } catch (IllegalArgumentException | ClassCastException e) {
        throw new ClientErrorException(
          "Invalid value for table attribute '" + entry.getKey() + "': " + entry.getValue(), e);
      }
    }
    return splits;
  }

  /**
   * Applies a table-scope {@code alter} attribute hash: the same attributes as {@code create}
   * (minus the pre-split ones, which are meaningless on an existing table) plus {@code COPROCESSOR}
   * (matched case-insensitively like hbase-shell), given as a legacy
   * {@code 'path|class|priority|k=v,k=v'} spec string or a {@code CLASSNAME}/{@code JAR_PATH}/
   * {@code PRIORITY}/{@code PROPERTIES} hash.
   */
  static void applyToExisting(TableDescriptorBuilder builder, Map<String, Object> attributes,
    Configuration conf) throws IOException {
    Map<String, Object> remaining = new HashMap<>();
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      String key = entry.getKey();
      if ("COPROCESSOR".equalsIgnoreCase(key.trim())) {
        builder.setCoprocessor(toCoprocessor(entry.getValue()));
      } else if (
        "SPLITS".equals(key) || "SPLITS_FILE".equals(key) || "NUMREGIONS".equals(key)
          || "SPLITALGO".equals(key)
      ) {
        throw new ClientErrorException("Table attribute '" + key + "' is only valid in create");
      } else {
        remaining.put(key, entry.getValue());
      }
    }
    apply(builder, remaining, conf);
  }

  private static CoprocessorDescriptor toCoprocessor(Object value) throws IOException {
    try {
      if (value instanceof Map) {
        Map<?, ?> spec = (Map<?, ?>) value;
        Object className = spec.get("CLASSNAME");
        if (className == null) {
          throw new ClientErrorException("CLASSNAME must be provided in the COPROCESSOR spec");
        }
        CoprocessorDescriptorBuilder cp =
          CoprocessorDescriptorBuilder.newBuilder(className.toString());
        if (spec.get("JAR_PATH") != null) {
          cp.setJarPath(spec.get("JAR_PATH").toString());
        }
        if (spec.get("PRIORITY") != null) {
          cp.setPriority(AttributeCoercion.toInt(spec.get("PRIORITY")));
        }
        if (spec.get("PROPERTIES") instanceof Map) {
          for (Map.Entry<?, ?> prop : ((Map<?, ?>) spec.get("PROPERTIES")).entrySet()) {
            cp.setProperty(prop.getKey().toString(), String.valueOf(prop.getValue()));
          }
        }
        return cp.build();
      }
      if (value instanceof String) {
        return fromSpecString(((String) value).trim());
      }
    } catch (IllegalArgumentException | ClassCastException e) {
      throw new ClientErrorException("Invalid COPROCESSOR value: " + value, e);
    }
    throw new ClientErrorException("COPROCESSOR must be provided as a String or Hash");
  }

  /** Parses the legacy {@code [jar path]|class|[priority]|[k=v,k=v]} coprocessor spec. */
  private static CoprocessorDescriptor fromSpecString(String spec) throws IOException {
    String[] parts = spec.split("\\|", -1);
    if (parts.length < 2 || parts.length > 4 || parts[1].trim().isEmpty()) {
      throw new ClientErrorException("Invalid COPROCESSOR spec '" + spec
        + "', expected '[jar path]|class name|[priority]|[key=value,...]'");
    }
    CoprocessorDescriptorBuilder cp = CoprocessorDescriptorBuilder.newBuilder(parts[1].trim());
    if (!parts[0].trim().isEmpty()) {
      cp.setJarPath(parts[0].trim());
    }
    if (parts.length > 2 && !parts[2].trim().isEmpty()) {
      cp.setPriority(Integer.parseInt(parts[2].trim()));
    }
    if (parts.length > 3) {
      for (String kv : parts[3].split(",")) {
        String[] pair = kv.split("=", 2);
        if (pair.length == 2) {
          cp.setProperty(pair[0].trim(), pair[1].trim());
        }
      }
    }
    return cp.build();
  }

  /**
   * Mirrors hbase-shell's create split handling (admin.rb): an explicit {@code SPLITS} list (or
   * {@code SPLITS_FILE}) takes precedence and any {@code NUMREGIONS}/{@code SPLITALGO} pair is
   * ignored; otherwise, when either is present both are required, {@code NUMREGIONS} must be &gt;
   * 1, and the named {@link RegionSplitter.SplitAlgorithm} computes the split points.
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
        throw new ClientErrorException("Invalid value for table attribute 'SPLITS': " + rawSplits,
          e);
      }
    }
    if (numRegions == null && splitAlgo == null) {
      return null;
    }
    if (numRegions == null) {
      throw new ClientErrorException("Number of regions must be specified via NUMREGIONS");
    }
    if (splitAlgo == null) {
      throw new ClientErrorException("Split algorithm must be specified via SPLITALGO");
    }
    int numRegionsValue;
    try {
      numRegionsValue = AttributeCoercion.toInt(numRegions);
    } catch (IllegalArgumentException | ClassCastException e) {
      throw new ClientErrorException(
        "Invalid value for table attribute 'NUMREGIONS': " + numRegions, e);
    }
    if (numRegionsValue <= 1) {
      throw new ClientErrorException("NUMREGIONS must be greater than 1");
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
      throw new ClientErrorException(
        "Table attribute '" + key + "' must be a map, but was: " + rawMap);
    }
    for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawMap).entrySet()) {
      Object value = entry.getValue();
      builder.setValue(entry.getKey().toString(), value == null ? null : value.toString());
    }
  }

  private static List<String> readSplitsFile(String path) throws IOException {
    File file = new File(path);
    if (!file.exists()) {
      throw new ClientErrorException("Splits file " + path + " doesn't exist");
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
