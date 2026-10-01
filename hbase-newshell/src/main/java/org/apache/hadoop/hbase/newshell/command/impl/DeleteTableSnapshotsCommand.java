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
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.command.UserAbortException;
import org.apache.hadoop.hbase.newshell.hbase.SnapshotInfo;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/delete_table_snapshots.rb}.
 */
@InterfaceAudience.Private
public final class DeleteTableSnapshotsCommand implements ShellCommand {
  private static final DateTimeFormatter CREATION_TIME_FORMAT =
    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");

  @Override
  public String name() {
    return "delete_table_snapshots";
  }

  @Override
  public String help() {
    return "delete_table_snapshots 'tableRegex' [, 'snapshotRegex'] [--yes] - delete matching "
      + "table snapshots";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().isEmpty()) {
      throw new ShellCommandException(
        "delete_table_snapshots requires a table name regular expression");
    }
    String tableRegex = String.valueOf(command.positionalArgs().get(0));
    String snapshotRegex = command.positionalArgs().size() > 1
      ? String.valueOf(command.positionalArgs().get(1))
      : ".*";
    List<SnapshotInfo> list = context.admin().listTableSnapshots(tableRegex, snapshotRegex);
    PrintWriter out = context.out();
    out.println("SNAPSHOT  TABLE + CREATION TIME");
    for (SnapshotInfo snapshot : list) {
      out.println(" " + snapshot.name() + " " + formatInfo(snapshot));
    }
    out.flush();
    if (list.isEmpty()) {
      return TextResult.of("No snapshots matched the table name regular expression " + tableRegex
        + " and the snapshot name regular expression " + snapshotRegex);
    }
    confirm(context, command, list.size());
    List<String> lines = new ArrayList<>();
    for (SnapshotInfo snapshot : list) {
      try {
        context.admin().deleteSnapshot(snapshot.name());
        lines.add("Successfully deleted snapshot: " + snapshot.name());
        lines.add("");
      } catch (IOException e) {
        lines.add("Failed to delete snapshot: " + snapshot.name() + ", due to below exception,");
        lines.add(e.toString());
        lines.add("");
      }
    }
    return new TextResult(lines);
  }

  private static String formatInfo(SnapshotInfo snapshot) {
    String creationTime = CREATION_TIME_FORMAT
      .format(Instant.ofEpochMilli(snapshot.creationTime()).atZone(ZoneOffset.systemDefault()));
    return snapshot.tableName() + " (" + creationTime + ")";
  }

  private static void confirm(ExecutionContext context, ParsedCommand command, int count)
    throws ShellCommandException, IOException {
    if (context.options().forceYes() || isYesOption(command)) {
      return;
    }
    PrintWriter out = context.out();
    out.println();
    out.flush();
    if (context.options().interactive() && context.confirmationReader() != null) {
      String answer =
        context.confirmationReader().readLine("Delete the above " + count + " snapshots (y/n)? ");
      if (answer != null && answer.trim().toLowerCase(Locale.ROOT).startsWith("y")) {
        return;
      }
      throw new UserAbortException("delete_table_snapshots aborted");
    }
    throw new UserAbortException(
      "Refusing delete_table_snapshots without confirmation; re-run with --yes "
        + "(or in an interactive TTY)");
  }

  private static boolean isYesOption(ParsedCommand command) {
    Object yes = command.options().get("YES");
    if (yes == null) {
      return false;
    }
    if (yes instanceof Boolean) {
      return (Boolean) yes;
    }
    return Boolean.parseBoolean(String.valueOf(yes)) || "Y".equalsIgnoreCase(String.valueOf(yes));
  }
}
