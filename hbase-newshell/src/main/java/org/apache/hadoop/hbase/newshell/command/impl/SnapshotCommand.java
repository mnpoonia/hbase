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
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/snapshot.rb}: a table name, a snapshot name and
 * an optional {@code TTL}/{@code MAX_FILESIZE}/{@code SKIP_FLUSH} options hash.
 */
@InterfaceAudience.Private
public final class SnapshotCommand implements ShellCommand {
  @Override
  public String name() {
    return "snapshot";
  }

  @Override
  public String help() {
    return "snapshot 'table', 'snapshotName' [, {TTL => secs, MAX_FILESIZE => bytes, SKIP_FLUSH => true}]";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    ArgParsing.requireMaxArgs(command, "snapshot", 2);
    ArgParsing.requireArgs(command, 2, "snapshot requires a table name and a snapshot name");
    String tableName = ArgParsing.string(command, 0);
    String snapshotName = ArgParsing.string(command, 1);
    Map<String, Object> options = ArgParsing.allOptions(command);
    if (options.isEmpty()) {
      context.snapshotAdmin().snapshot(tableName, snapshotName);
    } else {
      context.snapshotAdmin().snapshot(tableName, snapshotName, options);
    }
    return TextResult.of();
  }
}
