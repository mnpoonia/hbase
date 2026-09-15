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
 * A plain-Java view of one HBase {@code Cell} - deliberately holds binary-safe string fields
 * rather than a raw {@code org.apache.hadoop.hbase.Cell}, so no HBase client type leaks past the
 * {@code hbase} wrapper package into the command/format layers.
 */
@InterfaceAudience.Private
public record CellView(String family, String qualifier, long timestamp, String value) {
}
