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

import org.apache.hadoop.hbase.newshell.format.OutputFormat;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Session-wide options parsed from {@code NewShellMain} argv (and defaults for tests). Shared by
 * every command via {@link ExecutionContext}.
 */
@InterfaceAudience.Private
public record SessionOptions(OutputFormat outputFormat, boolean verbose, boolean forceYes,
  boolean quiet, boolean interactive) {

  public static SessionOptions defaults() {
    return new SessionOptions(OutputFormat.TEXT, false, false, false, false);
  }

  public SessionOptions withInteractive(boolean value) {
    return new SessionOptions(outputFormat, verbose, forceYes, quiet, value);
  }

  public SessionOptions withForceYes(boolean value) {
    return new SessionOptions(outputFormat, verbose, value, quiet, interactive);
  }
}
