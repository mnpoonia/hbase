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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.hbase.SnapshotInfo;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/list_table_snapshots.rb}.
 */
@InterfaceAudience.Private
public final class ListTableSnapshotsCommand implements ShellCommand {
  private static final DateTimeFormatter CREATION_TIME_FORMAT =
    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");

  @Override
  public String name() {
    return "list_table_snapshots";
  }

  @Override
  public String help() {
    return "list_table_snapshots 'tableRegex' [, 'snapshotRegex'] - list snapshots for matching "
      + "tables";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "list_table_snapshots", 2);
    String tableRegex = ArgParsing.requireArg(command, 0,
      "list_table_snapshots requires a table name regular expression");
    String snapshotRegex = ArgParsing.optionalArg(command, 1, ".*");
    List<SnapshotInfo> snapshots = context.admin().listTableSnapshots(tableRegex, snapshotRegex);
    List<List<String>> rows = new ArrayList<>();
    for (SnapshotInfo snapshot : snapshots) {
      String creationTime = CREATION_TIME_FORMAT
        .format(Instant.ofEpochMilli(snapshot.creationTime()).atZone(ZoneOffset.systemDefault()));
      rows.add(Arrays.asList(snapshot.name(), snapshot.tableName() + " (" + creationTime + ")"));
    }
    return new TabularResult(Arrays.asList("SNAPSHOT", "TABLE + CREATION TIME"), rows);
  }
}
