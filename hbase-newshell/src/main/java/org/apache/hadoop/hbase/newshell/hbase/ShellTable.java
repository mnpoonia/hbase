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
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Narrow, newshell-specific facade over the data-plane operations the pilot commands need. Command
 * implementations depend on this interface, never on {@code org.apache.hadoop.hbase.client.Table}
 * directly - {@link DefaultShellTable} is the only class that does.
 */
@InterfaceAudience.Private
public interface ShellTable {
  GetResult get(String row, Map<String, Object> options) throws ShellCommandException, IOException;

  void put(String row, String column, String value, Map<String, Object> options)
    throws ShellCommandException, IOException;

  ScanResult scan(Map<String, Object> options) throws ShellCommandException, IOException;

  /**
   * Counts the rows matching {@code options} (the same {@code COLUMNS}/{@code LIMIT}/
   * {@code STARTROW}/{@code STOPROW}/{@code VERSIONS}/{@code FILTER} options {@link #scan} accepts,
   * plus {@code CACHE_BLOCKS} and {@code INTERVAL}). Mirrors hbase-shell's {@code _count_internal}:
   * every {@code INTERVAL} rows (default {@code 1000}), {@code progressListener} is invoked with
   * the running count and the row key just counted, so a long-running count can report progress to
   * the caller.
   */
  long count(Map<String, Object> options, CountProgressListener progressListener)
    throws ShellCommandException, IOException;

  /** Receives periodic progress updates from {@link #count}. */
  @FunctionalInterface
  interface CountProgressListener {
    void onProgress(long count, String row);
  }

  void delete(String row, String column, Long timestamp) throws IOException;

  /**
   * Rewrites every cell matched by the {@code scanSpec} (the options {@link #scan} accepts) with
   * the cell ACL {@code permissions} (user or {@code @group} to action string); returns the number
   * of rows touched. Mirrors the cell-ACL form of hbase-shell's {@code grant}.
   */
  long setCellPermissions(Map<String, String> permissions, Map<String, Object> scanSpec)
    throws ShellCommandException, IOException;

  void deleteAll(String row, String column, Long timestamp, Map<String, Object> options)
    throws ShellCommandException, IOException;

  Long getCounter(String row, String column) throws IOException;

  Long increment(String row, String column, long amount) throws IOException;

  String append(String row, String column, String value) throws IOException;

  List<String> getSplits() throws IOException;
}
