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
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/clone_snapshot.rb}: a snapshot name, a
 * destination table name, and an optional {@code {RESTORE_ACL=>true, CLONE_SFT=>'...'}} hash.
 */
@InterfaceAudience.Private
public final class CloneSnapshotCommand implements ShellCommand {
  @Override
  public String name() {
    return "clone_snapshot";
  }

  @Override
  public String help() {
    return "clone_snapshot 'snapshotName', 'tableName' [, {RESTORE_ACL=>true, CLONE_SFT=>'FILE'}]"
      + " - create a new table by cloning a snapshot";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.size() < 2) {
      throw new ShellCommandException(
        "clone_snapshot requires a snapshot name and a table name argument");
    }
    String snapshotName = String.valueOf(positionals.get(0));
    String tableName = String.valueOf(positionals.get(1));
    Map<String, Object> args =
      command.hashLiterals().isEmpty() ? command.options() : command.hashLiterals().get(0);
    boolean restoreAcl = Boolean.parseBoolean(String.valueOf(args.getOrDefault("RESTORE_ACL",
      false)));
    Object cloneSft = args.get("CLONE_SFT");
    context.admin().cloneSnapshot(snapshotName, tableName, restoreAcl,
      cloneSft == null ? null : String.valueOf(cloneSft));
    return TextResult.of();
  }
}
