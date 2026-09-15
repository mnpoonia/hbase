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

import org.apache.yetus.audience.InterfaceAudience;

/**
 * The result of executing a {@link ShellCommand}, rendered by a
 * {@code org.apache.hadoop.hbase.newshell.format.Formatter}. Deliberately a small, closed set of
 * shapes - the two output shapes the pilot commands actually need - rather than a general "any
 * object graph" result type. If a third fundamentally different shape is needed later, add a
 * third sealed subtype rather than generalizing this ahead of need.
 */
@InterfaceAudience.Private
public sealed interface CommandResult permits TextResult, TabularResult {
}
