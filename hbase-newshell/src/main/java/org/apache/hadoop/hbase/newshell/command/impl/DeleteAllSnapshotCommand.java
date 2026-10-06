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
 * Ported from hbase-shell's {@code shell/commands/delete_all_snapshot.rb}.
 */
@InterfaceAudience.Private
public final class DeleteAllSnapshotCommand implements ShellCommand {
  @Override
  public String name() {
    return "delete_all_snapshot";
  }

  @Override
  public String help() {
    return "delete_all_snapshot 's.*' [--yes] - delete all snapshots matching a regex";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "delete_all_snapshot", 1);
    String regex =
      ArgParsing.requireArg(command, 0, "delete_all_snapshot requires a snapshot regex argument");
    List<SnapshotInfo> list = context.admin().listSnapshots(regex);
    PrintWriter out = context.out();
    out.println("SNAPSHOT  TABLE + CREATION TIME");
    for (SnapshotInfo snapshot : list) {
      out.println(" " + snapshot.name() + " " + snapshot.describe());
    }
    out.flush();
    if (list.isEmpty()) {
      return TextResult.of("No snapshots matched the regex " + regex);
    }
    DestructiveBatchConfirm.confirmSnapshotDelete(context, command, "delete_all_snapshot",
      list.size());
    context.admin().deleteAllSnapshots(regex);
    List<SnapshotInfo> leftover = context.admin().listSnapshots(regex);
    int deleted = list.size() - leftover.size();
    List<String> lines = new ArrayList<>();
    if (deleted != 0) {
      lines.add(deleted + " snapshots successfully deleted.");
    }
    if (!leftover.isEmpty()) {
      lines.add("");
      lines.add("Failed to delete the below " + leftover.size() + " snapshots.");
      lines.add("SNAPSHOT  TABLE + CREATION TIME");
      for (SnapshotInfo snapshot : leftover) {
        lines.add(" " + snapshot.name() + " " + snapshot.describe());
      }
    }
    return new TextResult(lines);
  }
}
