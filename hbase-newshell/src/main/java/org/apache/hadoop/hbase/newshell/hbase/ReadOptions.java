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

import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.asList;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optBoolean;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optEnum;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optInt;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optLong;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optPositiveInt;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.optPositiveLong;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.parseFilter;
import static org.apache.hadoop.hbase.newshell.hbase.OptionValues.parseTimeRange;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.IsolationLevel;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * The read options shared by {@code get}, {@code scan} and {@code count}, validated once when
 * parsed and then applied to a {@link Get} or {@link Scan}. Which options a given command accepts
 * is decided separately, by its allow-list; this class only knows how to interpret them.
 */
@InterfaceAudience.Private
final class ReadOptions {
  private final List<Object> columns;
  private final Integer versions;
  private final Long timestamp;
  private final long[] timeRange;
  private final Filter filter;
  private final Integer limit;
  private final byte[] startRow;
  private final byte[] stopRow;
  private final byte[] rowPrefix;
  private final Boolean cacheBlocks;
  private final Boolean reversed;
  private final Boolean raw;
  private final Boolean allowPartialResults;
  private final Integer caching;
  private final Integer batch;
  private final Long maxResultSize;
  private final IsolationLevel isolationLevel;
  private final Scan.ReadType readType;

  private ReadOptions(Map<String, Object> options) throws ShellCommandException {
    // COLUMNS wins over COLUMN, as in the legacy scan.
    Object cols = options.get("COLUMNS") != null ? options.get("COLUMNS") : options.get("COLUMN");
    this.columns = cols == null ? null : asList(cols);
    this.versions = optInt(options, "VERSIONS");
    this.timestamp = optLong(options, "TIMESTAMP");
    this.timeRange =
      options.get("TIMERANGE") == null ? null : parseTimeRange(options.get("TIMERANGE"));
    this.filter = options.get("FILTER") == null ? null : parseFilter(options.get("FILTER"));
    this.limit = optPositiveInt(options, "LIMIT");
    this.startRow = bytes(options.get("STARTROW"));
    this.stopRow = bytes(options.get("STOPROW"));
    this.rowPrefix = bytes(options.get("ROWPREFIXFILTER"));
    this.cacheBlocks = optBoolean(options, "CACHE_BLOCKS");
    this.reversed = optBoolean(options, "REVERSED");
    this.raw = optBoolean(options, "RAW");
    this.allowPartialResults = optBoolean(options, "ALLOW_PARTIAL_RESULTS");
    this.caching = optPositiveInt(options, "CACHE");
    this.batch = optPositiveInt(options, "BATCH");
    this.maxResultSize = optPositiveLong(options, "MAX_RESULT_SIZE");
    this.isolationLevel = optEnum(options, "ISOLATION_LEVEL", IsolationLevel.class);
    this.readType = optEnum(options, "READ_TYPE", Scan.ReadType.class);
  }

  static ReadOptions parse(Map<String, Object> options) throws ShellCommandException {
    return new ReadOptions(options);
  }

  private static byte[] bytes(Object value) {
    return value == null ? null : BinaryStrings.toBytes(value.toString());
  }

  /** Whether {@code CACHE_BLOCKS} was given, for commands that need a different default. */
  boolean hasCacheBlocks() {
    return cacheBlocks != null;
  }

  void applyTo(Get get, Map<String, String> columnConverters)
    throws ShellCommandException, IOException {
    if (columns != null) {
      for (Object column : columns) {
        byte[][] spec = parseColumn(column.toString(), columnConverters);
        if (spec[1] == null) {
          get.addFamily(spec[0]);
        } else {
          get.addColumn(spec[0], spec[1]);
        }
      }
    }
    if (versions != null) {
      get.readVersions(versions);
    }
    if (timestamp != null) {
      get.setTimestamp(timestamp);
    }
    if (timeRange != null) {
      get.setTimeRange(timeRange[0], timeRange[1]);
    }
    if (filter != null) {
      get.setFilter(filter);
    }
  }

  /** A new scan carrying every option this class understands. */
  Scan newScan(Map<String, String> columnConverters) throws ShellCommandException, IOException {
    Scan scan = new Scan();
    if (columns != null) {
      for (Object column : columns) {
        byte[][] spec = parseColumn(column.toString(), columnConverters);
        if (spec[1] == null) {
          scan.addFamily(spec[0]);
        } else {
          scan.addColumn(spec[0], spec[1]);
        }
      }
    }
    if (limit != null) {
      scan.setLimit(limit);
    }
    if (startRow != null) {
      scan.withStartRow(startRow);
    }
    if (stopRow != null) {
      scan.withStopRow(stopRow);
    }
    // Like the legacy shell, a prefix overrides any STARTROW/STOPROW.
    if (rowPrefix != null) {
      scan.setStartStopRowForPrefixScan(rowPrefix);
    }
    if (versions != null) {
      scan.readVersions(versions);
    }
    if (timeRange != null) {
      scan.setTimeRange(timeRange[0], timeRange[1]);
    }
    if (filter != null) {
      scan.setFilter(filter);
    }
    if (timestamp != null) {
      scan.setTimestamp(timestamp);
    }
    if (cacheBlocks != null) {
      scan.setCacheBlocks(cacheBlocks);
    }
    if (reversed != null) {
      scan.setReversed(reversed);
    }
    if (raw != null) {
      scan.setRaw(raw);
    }
    if (allowPartialResults != null) {
      scan.setAllowPartialResults(allowPartialResults);
    }
    if (caching != null) {
      scan.setCaching(caching);
    }
    if (batch != null) {
      scan.setBatch(batch);
    }
    if (maxResultSize != null) {
      scan.setMaxResultSize(maxResultSize);
    }
    if (isolationLevel != null) {
      scan.setIsolationLevel(isolationLevel);
    }
    if (readType != null) {
      scan.setReadType(readType);
    }
    return scan;
  }

  /**
   * Parses {@code FAMILY[:QUALIFIER[:CONVERTER]]} into {@code [family, qualifier-or-null]},
   * recording any converter against the {@code family:qualifier} it applies to.
   */
  static byte[][] parseColumn(String columnSpec, Map<String, String> converters) {
    int colonIndex = columnSpec.indexOf(':');
    if (colonIndex < 0) {
      return new byte[][] { BinaryStrings.toBytes(columnSpec), null };
    }
    String familyPart = columnSpec.substring(0, colonIndex);
    String qualifierPart = columnSpec.substring(colonIndex + 1);
    int converterIndex = qualifierPart.lastIndexOf(':');
    if (
      converterIndex >= 0
        && ValueConverters.isConverter(qualifierPart.substring(converterIndex + 1))
    ) {
      String qualifierOnly = qualifierPart.substring(0, converterIndex);
      converters.put(
        Bytes.toStringBinary(BinaryStrings.toBytes(familyPart)) + ":"
          + Bytes.toStringBinary(BinaryStrings.toBytes(qualifierOnly)),
        qualifierPart.substring(converterIndex + 1));
      qualifierPart = qualifierOnly;
    }
    byte[] family = BinaryStrings.toBytes(familyPart);
    // 'cf:' means the empty qualifier (CellUtil.parseColumn semantics); only 'cf' means the family.
    return new byte[][] { family, BinaryStrings.toBytes(qualifierPart) };
  }
}
