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

import org.apache.yetus.audience.InterfaceAudience;

/**
 * Resolves a {@link ShellTable} for a table name, fresh per call - matching hbase-shell's
 * {@code table(name)} accessor semantics. This is what data-plane commands (e.g. {@code get})
 * depend on instead of a raw {@code org.apache.hadoop.hbase.client.Connection}.
 */
@InterfaceAudience.Private
public interface ShellTableFactory {
  ShellTable forTable(String tableName);
}
