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
package org.apache.hadoop.hbase.newshell.command.impl;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.trace.TraceUtil;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/trace.rb}: start/stop/status of an OpenTelemetry
 * span for the shell session. Not an HTrace leftover — uses {@link TraceUtil}.
 */
@InterfaceAudience.Private
public final class TraceCommand implements ShellCommand {
  private static Span span;
  private static Scope scope;

  @Override
  public String name() {
    return "trace";
  }

  @Override
  public String help() {
    return "trace 'start'|'stop'|'status', trace 'start', 'MySpan' - OpenTelemetry shell tracing";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    String action = command.positionalArgs().isEmpty()
      ? "status"
      : String.valueOf(command.positionalArgs().get(0));
    String spanName = command.positionalArgs().size() > 1
      ? String.valueOf(command.positionalArgs().get(1))
      : "HBaseShell";
    if ("start".equals(action)) {
      if (!tracing()) {
        span = TraceUtil.getGlobalTracer().spanBuilder(spanName).startSpan();
        scope = span.makeCurrent();
      }
    } else if ("stop".equals(action)) {
      if (tracing()) {
        scope.close();
        span.end();
        scope = null;
        span = null;
      }
    } else if (!"status".equals(action)) {
      throw new ShellCommandException("trace expects 'start', 'stop', or 'status'");
    }
    return TextResult.of(String.valueOf(tracing()));
  }

  private static boolean tracing() {
    return scope != null;
  }
}
