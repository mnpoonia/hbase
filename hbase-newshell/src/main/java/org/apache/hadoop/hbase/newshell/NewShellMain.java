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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.newshell.command.CommandNameCompleter;
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.format.DefaultFormatter;
import org.apache.hadoop.hbase.newshell.format.Formatter;
import org.apache.hadoop.hbase.newshell.hbase.DefaultShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.DefaultShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.ShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.newshell.parser.ShellParseException;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.TerminalConfig;
import org.apache.hadoop.hbase.newshell.spi.TerminalProvider;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Entry point for newshell. Resolves a {@link TerminalProvider} backend via
 * {@link TerminalProviderRegistry} (or a {@link FileScriptTerminal} when a script-file argument is
 * given), opens one HBase {@link Connection} for the session, and runs a
 * parse-lookup-execute-format loop against the {@link CommandRegistry} until the user types
 * {@code exit}/{@code quit} or reaches end-of-input. Supports hbase-shell's {@code -n}/
 * {@code --noninteractive} flag and positional {@code SCRIPTFILE} argument (see
 * {@code jar-bootstrap.rb}): with {@code -n}, the process exits with status 1 on the first command
 * failure instead of printing the error and continuing.
 */
@InterfaceAudience.Private
public final class NewShellMain {
  static final String EXIT_COMMAND = "exit";
  static final String QUIT_COMMAND = "quit";

  private NewShellMain() {
  }

  public static void main(String[] args) throws IOException {
    // Same fail-fast retry overrides hbase-shell applies before creating a connection
    // (see hbase-shell's Hbase::Hbase#initialize).
    Configuration conf = HBaseConfiguration.create();
    conf.set("hbase.client.retries.number", "7");

    boolean exitOnFirstError = false;
    String scriptFile = null;
    for (String arg : args) {
      if (arg.equals("-n") || arg.equals("--noninteractive")) {
        exitOnFirstError = true;
      } else if (scriptFile == null) {
        scriptFile = arg;
      }
    }

    boolean success;
    try (ShellTerminal terminal = openTerminal(scriptFile); Connection connection =
      ConnectionFactory.createConnection(conf)) {
      ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
      ShellTableFactory tables = new DefaultShellTableFactory(connection);
      ExecutionContext context = new ExecutionContext(admin, tables, terminal.writer());
      CommandRegistry registry = new CommandRegistry();
      terminal.setCompleter(new CommandNameCompleter(registry));
      success = run(terminal, context, registry, new DefaultFormatter(), exitOnFirstError);
    }
    if (!success) {
      System.exit(1);
    }
  }

  private static ShellTerminal openTerminal(String scriptFile) throws IOException {
    if (scriptFile != null) {
      return new FileScriptTerminal(scriptFile);
    }
    TerminalProvider provider = new TerminalProviderRegistry().resolve();
    TerminalConfig config = TerminalConfig.builder().appName("newshell").build();
    return provider.open(config);
  }

  static boolean run(ShellTerminal terminal, ExecutionContext context, CommandRegistry registry,
    Formatter formatter) throws IOException {
    return run(terminal, context, registry, formatter, false);
  }

  static boolean run(ShellTerminal terminal, ExecutionContext context, CommandRegistry registry,
    Formatter formatter, boolean exitOnFirstError) throws IOException {
    PrintWriter out = context.out();
    String line;
    while ((line = terminal.readLine("newshell> ")) != null) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.equalsIgnoreCase(EXIT_COMMAND) || trimmed.equalsIgnoreCase(QUIT_COMMAND)) {
        break;
      }
      if (!dispatch(trimmed, context, registry, formatter, out) && exitOnFirstError) {
        return false;
      }
    }
    return true;
  }

  private static boolean dispatch(String line, ExecutionContext context, CommandRegistry registry,
    Formatter formatter, PrintWriter out) {
    ParsedCommand parsed;
    try {
      parsed = ShellLineParser.parse(line);
    } catch (ShellParseException e) {
      out.println("ERROR: " + e.getMessage());
      out.flush();
      return false;
    }
    ShellCommand command = registry.lookup(parsed.commandName()).orElse(null);
    if (command == null) {
      out.println("ERROR: unknown command '" + parsed.commandName() + "'");
      out.flush();
      return false;
    }
    try {
      formatter.format(command.execute(parsed, context), out);
    } catch (ShellCommandException | IOException e) {
      out.println("ERROR: " + e.getMessage());
      out.flush();
      return false;
    }
    return true;
  }
}
