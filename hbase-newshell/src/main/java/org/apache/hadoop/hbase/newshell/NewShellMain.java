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
import org.apache.hadoop.hbase.newshell.command.ErrorMapper;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ExitCodes;
import org.apache.hadoop.hbase.newshell.command.SessionOptions;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.format.Formatter;
import org.apache.hadoop.hbase.newshell.format.Formatters;
import org.apache.hadoop.hbase.newshell.format.OutputFormat;
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
  static final String EXIT_COMMAND = "exit";
  static final String QUIT_COMMAND = "quit";

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

    boolean interactive = launch.scriptFile == null && !launch.exitOnFirstError;
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
      exitCode = run(terminal, context, registry, formatter, launch.exitOnFirstError);
    }
    if (exitCode != ExitCodes.SUCCESS) {
      System.exit(exitCode);
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

  /** Returns {@link ExitCodes#SUCCESS} unless {@code exitOnFirstError} and a command failed */
  static int run(ShellTerminal terminal, ExecutionContext context, CommandRegistry registry,
    Formatter formatter) throws IOException {
    return run(terminal, context, registry, formatter, false);
  }

  static int run(ShellTerminal terminal, ExecutionContext context, CommandRegistry registry,
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
      int code = dispatch(trimmed, context, registry, formatter, out);
      if (code != ExitCodes.SUCCESS && exitOnFirstError) {
        return code;
      }
    }
    return ExitCodes.SUCCESS;
  }

  private static int dispatch(String line, ExecutionContext context, CommandRegistry registry,
    Formatter formatter, PrintWriter out) {
    ParsedCommand parsed;
    try {
      parsed = ShellLineParser.parse(line);
    } catch (ShellParseException e) {
      printError(context, out, e.getMessage(), e);
      return ExitCodes.CLIENT_ERROR;
    }
    ShellCommand command = registry.lookup(parsed.commandName()).orElse(null);
    if (command == null) {
      printError(context, out, "unknown command '" + parsed.commandName() + "'", null);
      return ExitCodes.CLIENT_ERROR;
    }
    try {
      formatter.format(command.name(), command.execute(parsed, context), out);
      return ExitCodes.SUCCESS;
    } catch (ShellCommandException | IOException e) {
      printError(context, out, e.getMessage(), e);
      return ErrorMapper.exitCodeFor(e);
    }
  }

  private static void printError(ExecutionContext context, PrintWriter out, String message,
    Throwable cause) {
    if (!context.options().quiet()) {
      out.println("ERROR: " + message);
      if (context.options().verbose() && cause != null) {
        cause.printStackTrace(out);
      }
      out.flush();
    }
  }

  /** Parsed argv for {@link NewShellMain#main}. Visible for tests. */
  static final class LaunchArgs {
    final boolean exitOnFirstError;
    final boolean forceYes;
    final boolean verbose;
    final boolean quiet;
    final OutputFormat outputFormat;
    final String scriptFile;

    LaunchArgs(boolean exitOnFirstError, boolean forceYes, boolean verbose, boolean quiet,
      OutputFormat outputFormat, String scriptFile) {
      this.exitOnFirstError = exitOnFirstError;
      this.forceYes = forceYes;
      this.verbose = verbose;
      this.quiet = quiet;
      this.outputFormat = outputFormat;
      this.scriptFile = scriptFile;
    }

    static LaunchArgs parse(String[] args) {
      boolean exitOnFirstError = false;
      boolean forceYes = false;
      boolean verbose = false;
      boolean quiet = false;
      OutputFormat outputFormat = OutputFormat.TEXT;
      String scriptFile = null;
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if (arg.equals("-n") || arg.equals("--noninteractive")) {
          exitOnFirstError = true;
        } else if (arg.equals("-y") || arg.equals("--yes")) {
          forceYes = true;
        } else if (arg.equals("-v") || arg.equals("--verbose")) {
          verbose = true;
        } else if (arg.equals("-q") || arg.equals("--quiet")) {
          quiet = true;
        } else if (arg.equals("-o") || arg.equals("--output")) {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException(arg + " requires a value (text|json|csv)");
          }
          outputFormat = OutputFormat.parse(args[++i]);
        } else if (arg.startsWith("--output=")) {
          outputFormat = OutputFormat.parse(arg.substring("--output=".length()));
        } else if (arg.startsWith("-o=") || arg.startsWith("--o=")) {
          outputFormat = OutputFormat.parse(arg.substring(arg.indexOf('=') + 1));
        } else if (scriptFile == null && !arg.startsWith("-")) {
          scriptFile = arg;
        } else if (arg.startsWith("-")) {
          throw new IllegalArgumentException("Unknown option: " + arg);
        }
      }
      return new LaunchArgs(exitOnFirstError, forceYes, verbose, quiet, outputFormat, scriptFile);
    }
  }
}
