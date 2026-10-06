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
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.newshell.command.CommandNameCompleter;
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ExitCodes;
import org.apache.hadoop.hbase.newshell.command.SessionOptions;
import org.apache.hadoop.hbase.newshell.format.Formatter;
import org.apache.hadoop.hbase.newshell.format.Formatters;
import org.apache.hadoop.hbase.newshell.hbase.DefaultShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.DefaultShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.ShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.SupportsHistory;
import org.apache.hadoop.hbase.newshell.spi.TerminalConfig;
import org.apache.hadoop.hbase.newshell.spi.TerminalProvider;
import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for newshell. Resolves a {@link TerminalProvider} backend via
 * {@link TerminalProviderRegistry} (or a {@link FileScriptTerminal} when a script-file argument is
 * given), opens one HBase {@link Connection} for the session, and runs a
 * parse-lookup-execute-format loop against the {@link CommandRegistry} until the user types
 * {@code exit}/{@code quit} or reaches end-of-input. Supports hbase-shell's {@code -n}/
 * {@code --noninteractive} flag and positional {@code SCRIPTFILE} argument (see
 * {@code jar-bootstrap.rb}): with {@code -n}, the process exits with a mapped status on the first
 * command failure instead of printing the error and continuing.
 * <p>
 * Session flags: {@code --output}/{@code -o} ({@code text}|{@code json}|{@code csv}),
 * {@code --verbose}/{@code -v}, {@code --quiet}/{@code -q}, {@code --yes}/{@code -y} (skip
 * confirmation prompts for destructive batch commands).
 * </p>
 */
@InterfaceAudience.Private
public final class NewShellMain {
  private static final Logger LOG = LoggerFactory.getLogger(NewShellMain.class);

  private NewShellMain() {
  }

  public static void main(String[] args) throws IOException {
    // Same fail-fast retry overrides hbase-shell applies before creating a connection
    // (see hbase-shell's Hbase::Hbase#initialize).
    Configuration conf = HBaseConfiguration.create();
    conf.set("hbase.client.retries.number", "7");

    LaunchArgs launch;
    try {
      launch = LaunchArgs.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println("ERROR: " + e.getMessage());
      System.exit(ExitCodes.CLIENT_ERROR);
      return;
    }

    if (launch.help) {
      System.out.println(LaunchArgs.USAGE);
      return;
    }
    launch.properties.forEach(conf::set);

    boolean interactive = isInteractive(launch, System.console() != null);
    SessionOptions options = new SessionOptions(launch.outputFormat, launch.verbose,
      launch.forceYes, launch.quiet, interactive);
    Formatter formatter = Formatters.forFormat(options.outputFormat());

    int exitCode = ExitCodes.SUCCESS;
    try (ShellTerminal terminal = openTerminal(launch.scriptFile);
      Connection connection = ConnectionFactory.createConnection(conf)) {
      ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
      ShellTableFactory tables = new DefaultShellTableFactory(connection);
      ExecutionContext context = new ExecutionContext(admin, tables, terminal.writer(), options,
        options.interactive() ? terminal::readLine : null);
      CommandRegistry registry = new CommandRegistry();
      terminal.setCompleter(new CommandNameCompleter(registry));
      exitCode = ShellRepl.run(terminal, context, registry, formatter, launch.exitOnFirstError);
      saveHistoryQuietly(terminal, context.out());
    }
    if (exitCode != ExitCodes.SUCCESS) {
      System.exit(exitCode);
    }
  }

  /**
   * A session is interactive only on a real terminal: piped stdin (as used by graceful_stop.sh and
   * rolling-restart.sh) must not have its following lines consumed as y/N answers or be preceded by
   * a prompt on stdout.
   */
  static boolean isInteractive(LaunchArgs launch, boolean hasConsole) {
    return launch.scriptFile == null && !launch.exitOnFirstError && hasConsole;
  }

  private static ShellTerminal openTerminal(String scriptFile) throws IOException {
    if (scriptFile != null) {
      return new FileScriptTerminal(scriptFile);
    }
    TerminalProvider provider = new TerminalProviderRegistry().resolve();
    TerminalConfig config =
      TerminalConfig.builder().appName("newshell").historyFile(defaultHistoryFile()).build();
    return provider.open(config);
  }

  static Path defaultHistoryFile() {
    return Paths.get(System.getProperty("user.home"), ".hbase-newshell-history");
  }

  static void saveHistoryIfSupported(ShellTerminal terminal) throws IOException {
    if (terminal instanceof SupportsHistory) {
      ((SupportsHistory) terminal).saveHistory();
    }
  }

  private static void saveHistoryQuietly(ShellTerminal terminal, PrintWriter out) {
    try {
      saveHistoryIfSupported(terminal);
    } catch (IOException e) {
      out.println("ERROR: " + e.getMessage());
      out.flush();
    }
  }
}
