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
package org.apache.hadoop.hbase.newshell;

import java.io.IOException;
import java.io.PrintWriter;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.TerminalConfig;
import org.apache.hadoop.hbase.newshell.spi.TerminalProvider;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Entry point for newshell. This phase-1 slice only proves the module builds, launches, and
 * resolves a {@link TerminalProvider} backend via {@link TerminalProviderRegistry} - it has no
 * command parser, catalog, or execution engine yet. Input lines are simply echoed back until
 * the user types {@code exit}/{@code quit} or reaches end-of-input.
 */
@InterfaceAudience.Private
public final class NewShellMain {
  static final String EXIT_COMMAND = "exit";
  static final String QUIT_COMMAND = "quit";

  private NewShellMain() {
  }

  public static void main(String[] args) throws IOException {
    // Same fail-fast retry overrides hbase-shell applies before creating a connection
    // (see hbase-shell's Hbase::Hbase#initialize) - kept here even though this slice never
    // actually opens a connection, so later phases don't need to rediscover this default.
    HBaseConfiguration.create().set("hbase.client.retries.number", "7");

    TerminalProvider provider = new TerminalProviderRegistry().resolve();
    TerminalConfig config = TerminalConfig.builder().appName("newshell").build();
    try (ShellTerminal terminal = provider.open(config)) {
      run(terminal);
    }
  }

  static void run(ShellTerminal terminal) throws IOException {
    PrintWriter out = terminal.writer();
    String line;
    while ((line = terminal.readLine("newshell> ")) != null) {
      String trimmed = line.trim();
      if (trimmed.equalsIgnoreCase(EXIT_COMMAND) || trimmed.equalsIgnoreCase(QUIT_COMMAND)) {
        break;
      }
      out.println("newshell (stub): " + line);
      out.flush();
    }
  }
}
