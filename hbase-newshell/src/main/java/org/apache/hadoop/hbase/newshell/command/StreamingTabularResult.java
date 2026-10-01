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
import java.util.List;
import java.util.function.Consumer;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * A header plus data rows pushed to a {@code org.apache.hadoop.hbase.newshell.format.Formatter}
 * one row at a time via {@link #forEachRow}, instead of a fully materialized
 * {@link TabularResult} - for producers (like {@code scan}) whose result set must not be buffered
 * in memory before output starts.
 */
@InterfaceAudience.Private
public final class StreamingTabularResult implements CommandResult {
  @FunctionalInterface
  public interface RowProducer {
    void produce(Consumer<List<String>> rowConsumer) throws IOException;
  }

  private final List<String> header;
  private final RowProducer rowProducer;

  public StreamingTabularResult(List<String> header, RowProducer rowProducer) {
    this.header = header;
    this.rowProducer = rowProducer;
  }

  public List<String> header() {
    return header;
  }

  public void forEachRow(Consumer<List<String>> rowConsumer) throws IOException {
    rowProducer.produce(rowConsumer);
  }
}
