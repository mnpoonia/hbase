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
package org.apache.hadoop.hbase.newshell.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.newshell.hbase.TableAdminContract;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Shared skeleton for {@code disable_all} / {@code enable_all} / {@code drop_all}: list matches,
 * confirm, apply a per-table action, emit the Ruby-parity success/failure summary.
 */
@InterfaceAudience.Private
public final class BatchTableOp {
  @FunctionalInterface
  public interface TableAction {
    void apply(TableAdminContract admin, String tableName) throws IOException;
  }

  private BatchTableOp() {
  }

  public static CommandResult run(ParsedCommand command, ExecutionContext context,
    String commandName, String pastParticiple, TableAction action)
    throws ShellCommandException, IOException {
    String regex = ArgParsing.requireArg(command, 0, commandName + " requires a regex argument");
    TableAdminContract admin = context.tableAdmin();
    List<String> tables = admin.listTables(regex);
    if (tables.isEmpty()) {
      return TextResult.of("No tables matched the regex " + regex);
    }
    DestructiveBatchConfirm.confirm(context, command, commandName, tables);
    List<String> failed = new ArrayList<>();
    IOException firstFailure = null;
    for (String table : tables) {
      try {
        action.apply(admin, table);
      } catch (IOException e) {
        failed.add(table + " (" + e.getMessage() + ")");
        if (firstFailure == null) {
          firstFailure = e;
        }
      }
    }
    String summary = (tables.size() - failed.size()) + " tables successfully " + pastParticiple;
    if (failed.isEmpty()) {
      return TextResult.of(summary);
    }
    // Surface partial failure as an error so a -n run exits non-zero instead of reporting success.
    throw new ShellCommandException(summary + "\n" + failed.size() + " tables not " + pastParticiple
      + " due to an exception: " + String.join(", ", failed), firstFailure);
  }
}
