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
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.DestructiveBatchConfirm;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.SnapshotInfo;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/delete_table_snapshots.rb}.
 */
@InterfaceAudience.Private
public final class DeleteTableSnapshotsCommand implements ShellCommand {
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
    ArgParsing.requireMaxArgs(command, "delete_table_snapshots", 2);
    String tableRegex = ArgParsing.requireArg(command, 0,
      "delete_table_snapshots requires a table name regular expression");
    String snapshotRegex = ArgParsing.optionalArg(command, 1, ".*");
    List<SnapshotInfo> list = context.snapshotAdmin().listTableSnapshots(tableRegex, snapshotRegex);
    PrintWriter out = context.out();
    out.println("SNAPSHOT  TABLE + CREATION TIME");
    for (SnapshotInfo snapshot : list) {
      out.println(" " + snapshot.name() + " " + snapshot.describe());
    }
    out.flush();
    if (list.isEmpty()) {
      return TextResult.of("No snapshots matched the table name regular expression " + tableRegex
        + " and the snapshot name regular expression " + snapshotRegex);
    }
    DestructiveBatchConfirm.confirmSnapshotDelete(context, command, "delete_table_snapshots",
      list.size());
    List<String> lines = new ArrayList<>();
    int failures = 0;
    for (SnapshotInfo snapshot : list) {
      try {
        context.snapshotAdmin().deleteSnapshot(snapshot.name());
        lines.add("Successfully deleted snapshot: " + snapshot.name());
        lines.add("");
      } catch (IOException e) {
        failures++;
        lines.add("Failed to delete snapshot: " + snapshot.name() + ", due to below exception,");
        lines.add(e.toString());
        lines.add("");
      }
    }
    if (failures > 0) {
      // The per-snapshot report must still be seen, but a partial failure must not exit 0.
      for (String line : lines) {
        out.println(line);
      }
      out.flush();
      throw new IOException(
        "Failed to delete " + failures + " of " + list.size() + " matching snapshots");
    }
    return new TextResult(lines);
  }
}
