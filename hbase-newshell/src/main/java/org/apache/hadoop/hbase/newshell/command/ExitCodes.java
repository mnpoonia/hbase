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
 * Process exit codes for non-interactive ({@code -n} / script) newshell runs. Interactive REPL
 * sessions print errors and continue; these codes apply when the process exits on failure.
 */
@InterfaceAudience.Private
public final class ExitCodes {
  public static final int SUCCESS = 0;
  /** Bad args, missing table, invalid state, unknown command, parse errors. */
  public static final int CLIENT_ERROR = 1;
  /** RPC / server / timeout / ZooKeeper failures. */
  public static final int SERVER_ERROR = 2;
  /** Auth / ACL / Kerberos failures. */
  public static final int AUTH_ERROR = 3;
  /** User declined a confirmation prompt, or non-interactive run lacked {@code --yes}. */
  public static final int USER_ABORT = 4;

  private ExitCodes() {
  }
}
