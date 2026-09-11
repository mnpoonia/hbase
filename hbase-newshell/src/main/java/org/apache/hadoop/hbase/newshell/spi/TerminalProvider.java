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

import java.io.IOException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Service Provider Interface for terminal/line-editing backends. Implementations are
 * discovered via {@link java.util.ServiceLoader} - newshell's core module never names a
 * concrete backend (JLine 3, JLine 4, or any future replacement such as a different TUI
 * library) directly. Adding a new backend means implementing this interface, registering it
 * under {@code META-INF/services/org.apache.hadoop.hbase.newshell.spi.TerminalProvider}, and
 * making sure it - and only it - ends up on the runtime classpath; no changes to this
 * interface or to newshell's core are required.
 */
@InterfaceAudience.Private
public interface TerminalProvider {
  /**
   * Short, stable identifier for this backend (e.g. {@code "jline3"}, {@code "jline4"}), used
   * for logging and for the {@code hbase.newshell.terminal.provider} override property.
   */
  String name();

  /**
   * Higher values are preferred when more than one provider is available and no explicit
   * override was requested. Providers should pick a value that reflects how well-suited they
   * are to the current runtime (e.g. JDK baseline).
   */
  int priority();

  /**
   * Runtime capability probe, checked in addition to the provider simply being present on the
   * classpath (e.g. a provider requiring a native library might report {@code false} if that
   * library failed to load).
   */
  boolean isAvailable();

  /**
   * Opens a new terminal session. Called once selection has already happened; implementations
   * do not need to re-check {@link #isAvailable()}.
   */
  ShellTerminal open(TerminalConfig config) throws IOException;
}
