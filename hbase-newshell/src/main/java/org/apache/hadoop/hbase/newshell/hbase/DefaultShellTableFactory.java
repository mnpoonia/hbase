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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Wraps a real {@link Connection}. One {@link DefaultShellTable} is cached per table name for the
 * life of the session (the shell is single-threaded) so a long session does not open a fresh
 * {@code Table} per command; {@link #close()} releases them all.
 */
@InterfaceAudience.Private
public final class DefaultShellTableFactory implements ShellTableFactory, Closeable {
  private final Connection connection;
  private final Map<String, DefaultShellTable> tables = new HashMap<>();

  public DefaultShellTableFactory(Connection connection) {
    this.connection = connection;
  }

  @Override
  public synchronized ShellTable forTable(String tableName) {
    DefaultShellTable cached = tables.get(tableName);
    if (cached == null) {
      try {
        cached = new DefaultShellTable(connection.getTable(TableName.valueOf(tableName)));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      tables.put(tableName, cached);
    }
    return cached;
  }

  @Override
  public synchronized void close() throws IOException {
    IOException first = null;
    for (DefaultShellTable table : tables.values()) {
      try {
        table.close();
      } catch (IOException e) {
        if (first == null) {
          first = e;
        } else {
          first.addSuppressed(e);
        }
      }
    }
    tables.clear();
    if (first != null) {
      throw first;
    }
  }
}
