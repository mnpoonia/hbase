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
import java.util.function.Consumer;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * The result of a {@code scan}: rows are pushed to a consumer one at a time as they are read from
 * the server, rather than buffered into a list. {@link DefaultShellTable#scan} defers opening the
 * underlying {@code ResultScanner} until {@link #forEachRow} is called, and closes it via
 * try-with-resources when that call returns or throws, so a full-table scan never holds more than
 * one row in memory at a time. Produce-once by design, so unlike the old list-backed version this
 * has no {@code equals}/{@code hashCode}.
 */
@InterfaceAudience.Private
public final class ScanResult {
  @FunctionalInterface
  public interface Producer {
    void produce(Consumer<ScanRow> rowConsumer) throws IOException;
  }

  private final Producer producer;

  public ScanResult(Producer producer) {
    this.producer = producer;
  }

  public void forEachRow(Consumer<ScanRow> rowConsumer) throws IOException {
    producer.produce(rowConsumer);
  }
}
