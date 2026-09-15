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
import java.util.Map;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Narrow, newshell-specific facade over the data-plane operations the pilot commands need.
 * Command implementations depend on this interface, never on
 * {@code org.apache.hadoop.hbase.client.Table} directly - {@link DefaultShellTable} is the only
 * class that does.
 */
@InterfaceAudience.Private
public interface ShellTable {
  GetResult get(String row, Map<String, Object> options) throws IOException;

  void put(String row, String column, String value, Map<String, Object> options) throws IOException;
}
