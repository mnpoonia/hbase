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
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Confirmation gate for destructive batch commands ({@code disable_all}, {@code drop_all},
 * {@code enable_all}, {@code change_sft_all}). Proceeds when session {@code --yes} / line
 * {@code --yes} is set; otherwise prompts on an interactive TTY, or aborts with
 * {@link UserAbortException} when non-interactive.
 */
@InterfaceAudience.Private
public final class DestructiveBatchConfirm {
  private DestructiveBatchConfirm() {
  }

  public static void confirm(ExecutionContext context, ParsedCommand command, String actionLabel,
    List<String> targets) throws ShellCommandException, IOException {
    if (targets == null || targets.isEmpty()) {
      return;
    }
    if (context.options().forceYes() || ArgParsing.isYes(command)) {
      return;
    }
    PrintWriter out = context.out();
    out.println(targets.size() + " matching tables:");
    for (String target : targets) {
      out.println("  " + target);
    }
    out.flush();
    if (context.options().interactive() && context.confirmationReader() != null) {
      String answer =
        context.confirmationReader().readLine("Proceed with " + actionLabel + "? (y/N) ");
      if (answer != null && answer.trim().toLowerCase(Locale.ROOT).startsWith("y")) {
        return;
      }
      throw new UserAbortException(actionLabel + " aborted");
    }
    throw new UserAbortException("Refusing " + actionLabel
      + " without confirmation; re-run with --yes " + "(or in an interactive TTY)");
  }

  /** Confirmation gate for {@code delete_all_snapshot} / {@code delete_table_snapshots}. */
  public static void confirmSnapshotDelete(ExecutionContext context, ParsedCommand command,
    String commandName, int count) throws ShellCommandException, IOException {
    if (context.options().forceYes() || ArgParsing.isYes(command)) {
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
      throw new UserAbortException(commandName + " aborted");
    }
    throw new UserAbortException("Refusing " + commandName
      + " without confirmation; re-run with --yes (or in an interactive TTY)");
  }
}
