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

import java.util.Objects;
import org.apache.hadoop.hbase.newshell.format.OutputFormat;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Session-wide options parsed from {@code NewShellMain} argv (and defaults for tests). Shared by
 * every command via {@link ExecutionContext}.
 */
@InterfaceAudience.Private
public final class SessionOptions {
  private final OutputFormat outputFormat;
  private final boolean verbose;
  private final boolean forceYes;
  private final boolean quiet;
  private final boolean interactive;

  public SessionOptions(OutputFormat outputFormat, boolean verbose, boolean forceYes,
    boolean quiet, boolean interactive) {
    this.outputFormat = outputFormat;
    this.verbose = verbose;
    this.forceYes = forceYes;
    this.quiet = quiet;
    this.interactive = interactive;
  }

  public static SessionOptions defaults() {
    return new SessionOptions(OutputFormat.TEXT, false, false, false, false);
  }

  public SessionOptions withInteractive(boolean value) {
    return new SessionOptions(outputFormat, verbose, forceYes, quiet, value);
  }

  public SessionOptions withForceYes(boolean value) {
    return new SessionOptions(outputFormat, verbose, value, quiet, interactive);
  }

  public OutputFormat outputFormat() {
    return outputFormat;
  }

  public boolean verbose() {
    return verbose;
  }

  public boolean forceYes() {
    return forceYes;
  }

  public boolean quiet() {
    return quiet;
  }

  public boolean interactive() {
    return interactive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SessionOptions)) {
      return false;
    }
    SessionOptions other = (SessionOptions) o;
    return verbose == other.verbose && forceYes == other.forceYes && quiet == other.quiet
      && interactive == other.interactive && Objects.equals(outputFormat, other.outputFormat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(outputFormat, verbose, forceYes, quiet, interactive);
  }

  @Override
  public String toString() {
    return "SessionOptions[outputFormat=" + outputFormat + ", verbose=" + verbose + ", forceYes="
      + forceYes + ", quiet=" + quiet + ", interactive=" + interactive + "]";
  }
}
