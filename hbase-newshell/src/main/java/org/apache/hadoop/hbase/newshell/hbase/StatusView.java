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
package org.apache.hadoop.hbase.newshell.hbase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.RegionMetrics;
import org.apache.hadoop.hbase.ServerMetrics;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.ServerTask;
import org.apache.hadoop.hbase.master.RegionState;
import org.apache.hadoop.hbase.replication.ReplicationLoadSink;
import org.apache.hadoop.hbase.replication.ReplicationLoadSource;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Newshell view of cluster status. Built from {@link ClusterMetrics} inside the {@code hbase}
 * package so command code never depends on client metric types.
 */
@InterfaceAudience.Private
public final class StatusView {
  private final ClusterMetrics metrics;

  StatusView(ClusterMetrics metrics) {
    this.metrics = metrics;
  }

  public static StatusView from(ClusterMetrics metrics) {
    return new StatusView(metrics);
  }

  public List<String> summaryLines() {
    return Arrays.asList(
      "1 active master, " + metrics.getBackupMasterNames().size() + " backup masters,",
      "              " + metrics.getLiveServerMetrics().size() + " servers,",
      "              " + metrics.getDecommissionedServerNames().size() + " decommissioned,",
      "              " + metrics.getDeadServerNames().size() + " dead,",
      String.format("              %.4f average load", metrics.getAverageLoad()));
  }

  public List<String> simpleLines() {
    List<String> lines = new ArrayList<>();
    ServerName master = metrics.getMasterName();
    lines.add(String.format("active master:  %s:%d %d", master.getHostname(), master.getPort(),
      master.getStartcode()));
    List<ServerName> backupMasters = metrics.getBackupMasterNames();
    lines.add(String.format("%d backup masters", backupMasters.size()));
    for (ServerName server : backupMasters) {
      lines.add(String.format("    %s:%d %d", server.getHostname(), server.getPort(),
        server.getStartcode()));
    }
    Map<ServerName, ServerMetrics> liveServers = metrics.getLiveServerMetrics();
    lines.add(String.format("%d live servers", liveServers.size()));
    long load = 0;
    long regions = 0;
    for (Map.Entry<ServerName, ServerMetrics> entry : liveServers.entrySet()) {
      ServerName server = entry.getKey();
      lines.add(String.format("    %s:%d %d", server.getHostname(), server.getPort(),
        server.getStartcode()));
      lines.add(String.format("        %s", entry.getValue()));
      load += entry.getValue().getRequestCountPerSecond();
      regions += entry.getValue().getRegionMetrics().size();
    }
    List<ServerName> deadServers = metrics.getDeadServerNames();
    lines.add(String.format("%d dead servers", deadServers.size()));
    for (ServerName server : deadServers) {
      lines.add(String.format("    %s", server));
    }
    lines.add(String.format("Aggregate load: %d, regions: %d", load, regions));
    return lines;
  }

  public List<String> tasksLines() {
    List<String> lines = new ArrayList<>();
    ServerName master = metrics.getMasterName();
    if (master != null) {
      lines.add(String.format("active master:  %s:%d %d", master.getHostname(), master.getPort(),
        master.getStartcode()));
      appendRunningTasks(lines, metrics.getMasterTasks(), "    ");
    }
    Map<ServerName, ServerMetrics> liveServers = metrics.getLiveServerMetrics();
    lines.add(String.format("%d live servers", liveServers.size()));
    for (Map.Entry<ServerName, ServerMetrics> entry : liveServers.entrySet()) {
      ServerName server = entry.getKey();
      lines.add(String.format("    %s:%d %d", server.getHostname(), server.getPort(),
        server.getStartcode()));
      appendRunningTasks(lines, entry.getValue().getTasks(), "        ");
    }
    return lines;
  }

  /**
   * The {@code replication} format of {@code admin.rb#status}. {@code type} is {@code SOURCE},
   * {@code SINK} or {@code BOTH}; servers without a sink report are skipped, as in the old shell.
   */
  public List<String> replicationLines(String type) {
    List<String> lines = new ArrayList<>();
    lines.add(String.format("version %s", metrics.getHBaseVersion()));
    Map<ServerName, ServerMetrics> liveServers = metrics.getLiveServerMetrics();
    lines.add(String.format("%d live servers", liveServers.size()));
    for (Map.Entry<ServerName, ServerMetrics> entry : liveServers.entrySet()) {
      ReplicationLoadSink sink = entry.getValue().getReplicationLoadSink();
      if (sink == null) {
        continue;
      }
      ServerName server = entry.getKey();
      lines.add(String.format("    %s:%s %s", server.getHostname(), server.getPort(),
        server.getStartcode()));
      if (!"SINK".equalsIgnoreCase(type)) {
        addAll(lines, sourceString(entry.getValue().getReplicationLoadSourceMap()));
      }
      if (!"SOURCE".equalsIgnoreCase(type)) {
        addAll(lines, sinkString(sink));
      }
    }
    return lines;
  }

  private static void addAll(List<String> lines, String block) {
    lines.addAll(Arrays.asList(block.split("\n", -1)));
  }

  private static String sinkString(ReplicationLoadSink sink) {
    StringBuilder sb = new StringBuilder("        SINK:");
    sb.append("\n            TimeStampStarted=").append(sink.getTimestampStarted());
    if (sink.getTimestampsOfLastAppliedOp() == sink.getTimestampStarted()) {
      // nothing applied since start: this server is not acting as a sink
      sb.append(",\n            Waiting for OPs... ");
    } else {
      sb.append(",\n            AgeOfLastAppliedOp=").append(sink.getAgeOfLastAppliedOp());
      sb.append(",\n            TimeStampsOfLastAppliedOp=")
        .append(sink.getTimestampsOfLastAppliedOp());
    }
    return sb.toString();
  }

  private static String sourceString(Map<String, List<ReplicationLoadSource>> sources) {
    StringBuilder sb = new StringBuilder("        SOURCE:");
    for (Map.Entry<String, List<ReplicationLoadSource>> peer : sources.entrySet()) {
      sb.append("\n            PeerID=").append(peer.getKey());
      for (ReplicationLoadSource source : peer.getValue()) {
        sb.append(source.isRecovered()
          ? ",\n            Queue(Recovered)="
          : ",\n            Queue(Normal)=").append(source.getQueueId());
        if (source.isRunning()) {
          if (source.getTimestampOfLastShippedOp() == 0) {
            sb.append(
              ",\n            TimeStampOfLastShippedOp=0, No Ops shipped since last restart");
          } else {
            sb.append(",\n            AgeOfLastShippedOp=").append(source.getAgeOfLastShippedOp())
              .append(",\n            TimeStampOfLastShippedOp=")
              .append(source.getTimestampOfLastShippedOp());
          }
          sb.append(",\n            SizeOfLogQueue=").append(source.getSizeOfLogQueue())
            .append(",\n            EditsReadFromLogQueue=").append(source.getEditsRead())
            .append(",\n            OpsShippedToTarget=").append(source.getOPsShipped());
          if (source.hasEditsSinceRestart()) {
            sb.append(",\n            TimeStampOfNextToReplicate=")
              .append(source.getTimeStampOfNextToReplicate());
          } else {
            sb.append(",\n            HasEditsSinceRestart=false, "
              + "No edits for this source since it started");
          }
          sb.append(",\n            ReplicationLag=").append(source.getReplicationLag());
        } else {
          sb.append(",\n            IsRunning=false, No Reader/Shipper threads runnning yet.");
        }
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  private static void appendRunningTasks(List<String> lines, List<ServerTask> tasks,
    String indent) {
    boolean printed = false;
    for (ServerTask task : tasks) {
      if (task.getState() == ServerTask.State.RUNNING) {
        lines.add(indent + task);
        printed = true;
      }
    }
    if (!printed) {
      lines.add(indent + "no active tasks");
    }
  }

  public List<String> detailedLines() {
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
      lines.add(String.format("    %s:%d %d", server.getHostname(), server.getPort(),
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
