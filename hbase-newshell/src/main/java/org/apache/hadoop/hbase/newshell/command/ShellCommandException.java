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
 * Thrown by {@link ShellCommand#execute} for any command-level failure (bad arguments, an
 * underlying HBase client error, etc). The execution engine catches this, checked
 * {@code IOException} (mapped to an exit code by {@link ErrorMapper}) and
 * {@code IllegalArgumentException} (bad user input from validation helpers) to print an error and
 * keep the REPL running. Any other unchecked exception is treated as a defect and logged.
 */
@InterfaceAudience.Private
public class ShellCommandException extends Exception {
  private static final long serialVersionUID = 1L;

  public ShellCommandException(String message) {
    super(message);
  }

  public ShellCommandException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Process exit code when this failure ends a non-interactive run. */
  public int exitCode() {
    return ExitCodes.CLIENT_ERROR;
  }
}
