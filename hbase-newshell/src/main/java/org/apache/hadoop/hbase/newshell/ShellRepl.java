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
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
import org.apache.hadoop.hbase.newshell.command.ErrorMapper;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ExitCodes;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.format.Formatter;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.newshell.parser.ShellParseException;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The read-parse-lookup-execute-format loop: reads lines from a {@link ShellTerminal} until
 * {@code exit}/{@code quit} or end of input, dispatching each to the {@link CommandRegistry}.
 */
@InterfaceAudience.Private
final class ShellRepl {
  private static final Logger LOG = LoggerFactory.getLogger(ShellRepl.class);
  static final String EXIT_COMMAND = "exit";
  static final String QUIT_COMMAND = "quit";

  private ShellRepl() {
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
    String prompt = context.options().interactive() ? "newshell> " : "";
    while ((line = terminal.readLine(prompt)) != null) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
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
      printError(context, formatter, out, e.getMessage(), e);
      return ExitCodes.CLIENT_ERROR;
    }
    ShellCommand command = registry.lookup(parsed.commandName()).orElse(null);
    if (command == null) {
      printError(context, formatter, out, "unknown command '" + parsed.commandName() + "'", null);
      return ExitCodes.CLIENT_ERROR;
    }
    try {
      formatter.format(command.name(), command.execute(parsed, context), out);
      return ExitCodes.SUCCESS;
    } catch (ShellCommandException | IOException e) {
      printError(context, formatter, out,
        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), e);
      return ErrorMapper.exitCodeFor(e);
    } catch (IllegalArgumentException e) {
      // Bad user input rejected by coercion/validation helpers: a client error, not a defect.
      printError(context, formatter, out,
        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), e);
      return ExitCodes.CLIENT_ERROR;
    } catch (RuntimeException e) {
      // Trust boundary for the REPL: ShellCommand.execute must not leak unchecked
      // exceptions, but attribute translation / TableName validation / Admin calls
      // can still throw. Catch here so one bad command cannot kill the session.
      LOG.warn("Unchecked exception while executing command '{}'", parsed.commandName(), e);
      printError(context, formatter, out,
        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), e);
      // Streaming output wraps scan IOExceptions in unchecked ones; keep the server/client split.
      return ErrorMapper.exitCodeFor(e);
    }
  }

  private static void printError(ExecutionContext context, Formatter formatter, PrintWriter out,
    String message, Throwable cause) {
    if (!context.options().quiet()) {
      formatter.formatError(message, out);
      if (context.options().verbose() && cause != null) {
        cause.printStackTrace(out);
      }
      out.flush();
    }
  }
}
