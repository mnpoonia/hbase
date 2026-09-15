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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.RegionMetrics;
import org.apache.hadoop.hbase.ServerMetrics;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.ServerTask;
import org.apache.hadoop.hbase.master.RegionState;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code status.rb} / {@code hbase/admin.rb#status}: {@code summary}
 * (default, no argument) and {@code detailed}. The {@code replication}/{@code tasks}/
 * {@code simple} format branches are not ported.
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
    return "status ['detailed'] - show a summary, or a detailed report, of cluster status";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    var positionals = command.positionalArgs();
    String format = positionals.isEmpty() ? "summary" : String.valueOf(positionals.get(0));
    ClusterMetrics metrics = context.admin().status();
    if (format.equalsIgnoreCase(DETAILED)) {
      return new TextResult(detailedReport(metrics));
    }
    if (!format.equalsIgnoreCase("summary")) {
      throw new ShellCommandException("status format '" + format + "' is not supported");
    }
    return new TextResult(List.of(
      "1 active master, " + metrics.getBackupMasterNames().size() + " backup masters,",
      "              " + metrics.getLiveServerMetrics().size() + " servers,",
      "              " + metrics.getDecommissionedServerNames().size() + " decommissioned,",
      "              " + metrics.getDeadServerNames().size() + " dead,",
      String.format("              %.4f average load", metrics.getAverageLoad())));
  }

  private static List<String> detailedReport(ClusterMetrics metrics) {
    List<String> lines = new ArrayList<>();
    lines.add(String.format("version %s", metrics.getHBaseVersion()));

    List<RegionState> regionsInTransition = metrics.getRegionStatesInTransition();
    lines.add(String.format("%d regionsInTransition", regionsInTransition.size()));
    for (RegionState state : regionsInTransition) {
      lines.add(String.format("    %s", state));
    }

    ServerName master = metrics.getMasterName();
    if (master != null) {
      lines.add(String.format("active master:  %s:%d %d", master.getHostname(), master.getPort(),
        master.getStartcode()));
      for (ServerTask task : metrics.getMasterTasks()) {
        lines.add(String.format("    %s", task));
      }
    }

    List<ServerName> backupMasters = metrics.getBackupMasterNames();
    lines.add(String.format("%d backup masters", backupMasters.size()));
    for (ServerName server : backupMasters) {
      lines.add(
        String.format("    %s:%d %d", server.getHostname(), server.getPort(),
          server.getStartcode()));
    }

    lines.add(String.format("master coprocessors: %s", metrics.getMasterCoprocessorNames()));

    Map<ServerName, ServerMetrics> liveServers = metrics.getLiveServerMetrics();
    lines.add(String.format("%d live servers", liveServers.size()));
    for (Map.Entry<ServerName, ServerMetrics> entry : liveServers.entrySet()) {
      ServerName server = entry.getKey();
      ServerMetrics serverMetrics = entry.getValue();
      lines.add(String.format("    %s:%d %d", server.getHostname(), server.getPort(),
        server.getStartcode()));
      lines.add(String.format("        %s", serverMetrics));
      for (RegionMetrics region : serverMetrics.getRegionMetrics().values()) {
        lines.add(String.format("        \"%s\"", region.getNameAsString()));
        lines.add(String.format("            %s", region));
      }
      for (ServerTask task : serverMetrics.getTasks()) {
        lines.add(String.format("        %s", task));
      }
    }

    List<ServerName> deadServers = metrics.getDeadServerNames();
    lines.add(String.format("%d dead servers", deadServers.size()));
    for (ServerName server : deadServers) {
      lines.add(String.format("    %s", server));
    }
    return lines;
  }
}
