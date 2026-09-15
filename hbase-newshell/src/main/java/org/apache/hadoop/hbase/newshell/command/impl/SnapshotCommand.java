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
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported, minimal slice, from hbase-shell's {@code shell/commands/snapshot.rb}: a table name plus
 * a snapshot name, taking a default FLUSH-type snapshot. The {@code TTL}/{@code MAX_FILESIZE}/
 * {@code SKIP_FLUSH} options hash is explicitly not ported for this slice.
 */
@InterfaceAudience.Private
public final class SnapshotCommand implements ShellCommand {
  @Override
  public String name() {
    return "snapshot";
  }

  @Override
  public String help() {
    return "snapshot 'table', 'snapshotName' - take a snapshot of the named table";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    if (command.positionalArgs().size() < 2) {
      throw new ShellCommandException("snapshot requires a table name and a snapshot name");
    }
    String tableName = String.valueOf(command.positionalArgs().get(0));
    String snapshotName = String.valueOf(command.positionalArgs().get(1));
    context.admin().snapshot(tableName, snapshotName);
    return TextResult.of(snapshotName + " snapshot of table " + tableName + " created");
  }
}
