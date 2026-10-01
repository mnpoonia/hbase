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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.security.AccessDeniedException;
import org.apache.yetus.audience.InterfaceAudience;

/** Maps thrown failures to {@link ExitCodes} for non-interactive process exit. */
@InterfaceAudience.Private
public final class ErrorMapper {
  private ErrorMapper() {
  }

  public static int exitCodeFor(Throwable thrown) {
    if (thrown == null) {
      return ExitCodes.CLIENT_ERROR;
    }
    if (thrown instanceof ShellCommandException) {
      return ((ShellCommandException) thrown).exitCode();
    }
    Throwable cursor = thrown;
    while (cursor != null) {
      if (cursor instanceof AccessDeniedException) {
        return ExitCodes.AUTH_ERROR;
      }
      if (
        cursor instanceof TableNotFoundException || cursor instanceof TableExistsException
          || cursor instanceof DoNotRetryIOException
      ) {
        return ExitCodes.CLIENT_ERROR;
      }
      if (cursor instanceof SocketTimeoutException || cursor instanceof TimeoutException) {
        return ExitCodes.SERVER_ERROR;
      }
      cursor = cursor.getCause();
    }
    if (thrown instanceof IOException) {
      return ExitCodes.SERVER_ERROR;
    }
    return ExitCodes.CLIENT_ERROR;
  }
}
