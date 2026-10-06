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
package org.apache.hadoop.hbase.newshell.command;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * A header plus data rows pushed to a {@code org.apache.hadoop.hbase.newshell.format.Formatter} one
 * row at a time via {@link #forEachRow}, instead of a fully materialized {@link TabularResult} -
 * for producers (like {@code scan}) whose result set must not be buffered in memory before output
 * starts.
 */
@InterfaceAudience.Private
public final class StreamingTabularResult implements CommandResult {
  @FunctionalInterface
  public interface RowProducer {
    void produce(Consumer<List<String>> rowConsumer) throws IOException;
  }

  private final List<String> header;
  private final RowProducer rowProducer;
  private final Supplier<Map<String, String>> trailer;
  private boolean groupedByFirstColumn;
  private long rowCount;

  public StreamingTabularResult(List<String> header, RowProducer rowProducer) {
    this(header, rowProducer, Collections::emptyMap);
  }

  /**
   * @param trailer name/value pairs (e.g. scan metrics) to render after the rows; evaluated only
   *                once {@link #forEachRow} has returned
   */
  public StreamingTabularResult(List<String> header, RowProducer rowProducer,
    Supplier<Map<String, String>> trailer) {
    this.header = header;
    this.rowProducer = rowProducer;
    this.trailer = trailer;
  }

  public Map<String, String> trailer() {
    return trailer.get();
  }

  public List<String> header() {
    return header;
  }

  /**
   * Counts a run of emitted lines sharing the same first column as one logical row, as {@code scan}
   * emits one line per cell but reports {@code N row(s)} per row key.
   */
  public StreamingTabularResult groupedByFirstColumn() {
    this.groupedByFirstColumn = true;
    return this;
  }

  /** Logical rows seen by the last {@link #forEachRow} call; valid once it has returned. */
  public long rowCount() {
    return rowCount;
  }

  public void forEachRow(Consumer<List<String>> rowConsumer) throws IOException {
    rowCount = 0;
    String[] previousKey = { null };
    rowProducer.produce(row -> {
      if (!groupedByFirstColumn || previousKey[0] == null || !previousKey[0].equals(row.get(0))) {
        rowCount++;
      }
      previousKey[0] = row.isEmpty() ? null : row.get(0);
      rowConsumer.accept(row);
    });
  }

  @Override
  public void accept(ResultVisitor visitor) throws IOException {
    visitor.visit(this);
  }
}
