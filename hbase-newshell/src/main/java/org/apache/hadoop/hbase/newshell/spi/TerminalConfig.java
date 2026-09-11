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
package org.apache.hadoop.hbase.newshell.spi;

import java.nio.file.Path;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Immutable configuration passed to a {@link TerminalProvider} when opening a
 * {@link ShellTerminal}. Deliberately holds only plain data - no reference to any concrete
 * terminal/line-editing library - so that {@code TerminalProvider} implementations remain the
 * only place a particular backend (JLine, or any future replacement) is named.
 */
@InterfaceAudience.Private
public final class TerminalConfig {
  private final boolean interactive;
  private final String appName;
  private final Path historyFile;
  private final boolean colorEnabled;

  private TerminalConfig(Builder builder) {
    this.interactive = builder.interactive;
    this.appName = builder.appName;
    this.historyFile = builder.historyFile;
    this.colorEnabled = builder.colorEnabled;
  }

  public boolean isInteractive() {
    return interactive;
  }

  public String getAppName() {
    return appName;
  }

  public Path getHistoryFile() {
    return historyFile;
  }

  public boolean isColorEnabled() {
    return colorEnabled;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private boolean interactive = true;
    private String appName = "newshell";
    private Path historyFile;
    private boolean colorEnabled = true;

    private Builder() {
    }

    public Builder interactive(boolean isInteractive) {
      this.interactive = isInteractive;
      return this;
    }

    public Builder appName(String name) {
      this.appName = name;
      return this;
    }

    public Builder historyFile(Path path) {
      this.historyFile = path;
      return this;
    }

    public Builder colorEnabled(boolean enabled) {
      this.colorEnabled = enabled;
      return this;
    }

    public TerminalConfig build() {
      return new TerminalConfig(this);
    }
  }
}
