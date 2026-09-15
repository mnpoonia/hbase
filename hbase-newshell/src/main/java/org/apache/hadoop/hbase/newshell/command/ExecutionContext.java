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

import java.io.PrintWriter;
import org.apache.hadoop.hbase.newshell.hbase.ShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Bundles what a {@link ShellCommand} needs to do its work for one session. Deliberately exposes
 * only the newshell {@code hbase} wrapper abstractions ({@link ShellAdmin},
 * {@link ShellTableFactory}) - never the underlying
 * {@code org.apache.hadoop.hbase.client.Connection}/{@code Admin} - so commands depend on these
 * narrow interfaces rather than the full HBase client surface, and never reach into globals.
 */
@InterfaceAudience.Private
public final class ExecutionContext {
  private final ShellAdmin admin;
  private final ShellTableFactory tables;
  private final PrintWriter out;

  public ExecutionContext(ShellAdmin admin, ShellTableFactory tables, PrintWriter out) {
    this.admin = admin;
    this.tables = tables;
    this.out = out;
  }

  public ShellAdmin admin() {
    return admin;
  }

  public ShellTableFactory tables() {
    return tables;
  }

  public PrintWriter out() {
    return out;
  }
}
