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

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.StatusView;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code status.rb} / {@code hbase/admin.rb#status}: {@code summary}
 * (default, no argument), {@code simple}, {@code tasks}, {@code detailed} and {@code replication}
 * (with an optional {@code SOURCE}/{@code SINK}/{@code BOTH} type).
 */
@InterfaceAudience.Private
public final class StatusCommand implements ShellCommand {
  private static final String DETAILED = "detailed";

  @Override
  public String name() {
    return "status";
  }

  @Override
  public String help() {
    return "status ['summary'|'simple'|'tasks'|'detailed'|'replication' [, 'SOURCE'|'SINK'|'BOTH']] - show cluster status";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    String format = positionals.isEmpty() ? "summary" : String.valueOf(positionals.get(0));
    StatusView status = context.tableAdmin().status();
    if (format.equalsIgnoreCase(DETAILED)) {
      return new TextResult(status.detailedLines());
    }
    if (format.equalsIgnoreCase("simple")) {
      return new TextResult(status.simpleLines());
    }
    if (format.equalsIgnoreCase("tasks")) {
      return new TextResult(status.tasksLines());
    }
    if (format.equalsIgnoreCase("replication")) {
      String type = positionals.size() > 1 ? String.valueOf(positionals.get(1)) : "BOTH";
      if (
        !type.equalsIgnoreCase("SOURCE") && !type.equalsIgnoreCase("SINK")
          && !type.equalsIgnoreCase("BOTH")
      ) {
        throw new ShellCommandException("replication status type must be SOURCE, SINK or BOTH");
      }
      return new TextResult(status.replicationLines(type));
    }
    if (!format.equalsIgnoreCase("summary")) {
      throw new ShellCommandException("status format '" + format + "' is not supported");
    }
    return new TextResult(status.summaryLines());
  }
}
