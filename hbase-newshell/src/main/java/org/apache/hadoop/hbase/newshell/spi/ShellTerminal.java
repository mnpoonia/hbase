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
import java.io.PrintWriter;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * A single interactive terminal session, opened by a {@link TerminalProvider}. Callers
 * (newshell's core REPL loop) interact only through this interface, never through whatever
 * concrete line-editing library backs it.
 *
 * <p>
 * Backends that support additional capabilities beyond this minimal contract implement the
 * relevant optional marker interface (e.g. {@link SupportsHistory}) rather than forcing every
 * backend to implement a lowest-common-denominator surface.
 * </p>
 */
@InterfaceAudience.Private
public interface ShellTerminal extends AutoCloseable {
  /**
   * Reads a single line of input, displaying {@code prompt} first.
   * @return the line read, or {@code null} on end-of-input (e.g. Ctrl-D / closed stdin)
   */
  String readLine(String prompt) throws IOException;

  /**
   * @return a writer for output that participates correctly with the terminal's line-editing
   *         state (e.g. redrawing the prompt after printing above it)
   */
  PrintWriter writer();

  /**
   * Installs a completer to be consulted while the user is editing a line. Optional - backends
   * that don't support completion may make this a no-op.
   */
  void setCompleter(Completer completer);

  @Override
  void close() throws IOException;
}
