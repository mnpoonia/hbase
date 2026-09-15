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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.ClusterMetrics;
import org.apache.hadoop.hbase.ClusterMetricsBuilder;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class StatusCommandTest {

  private static final class FakeShellAdmin extends StubShellAdmin {
    private final ClusterMetrics metrics;

    FakeShellAdmin(ClusterMetrics metrics) {
      this.metrics = metrics;
    }

    @Override
    public ClusterMetrics status() {
      return metrics;
    }
  }

  private final StatusCommand command = new StatusCommand();

  private static ExecutionContext contextFor(ClusterMetrics metrics) {
    return new ExecutionContext(new FakeShellAdmin(metrics), new StubShellTableFactory(),
      new PrintWriter(new StringWriter()));
  }

  @Test
  public void detailedReportsActiveMasterWithDoubleSpaceFormat() throws Exception {
    ServerName master = ServerName.valueOf("master.example.com", 16000, 1L);
    ClusterMetrics metrics = ClusterMetricsBuilder.newBuilder().setHBaseVersion("3.0.0")
      .setMasterName(master).setBackerMasterNames(List.of()).setMasterCoprocessorNames(List.of())
      .setMasterTasks(List.of()).setLiveServerMetrics(Map.of()).setDeadServerNames(List.of())
      .build();

    var parsed = ShellLineParser.parse("status 'detailed'");
    TextResult result = (TextResult) command.execute(parsed, contextFor(metrics));

    assertTrue(result.lines().contains("active master:  master.example.com:16000 1"));
  }

  @Test
  public void detailedReportsVersionBackupMastersAndServerCounts() throws Exception {
    ServerName master = ServerName.valueOf("master.example.com", 16000, 1L);
    ServerName backup = ServerName.valueOf("backup.example.com", 16000, 2L);
    ClusterMetrics metrics = ClusterMetricsBuilder.newBuilder().setHBaseVersion("3.0.0")
      .setMasterName(master).setBackerMasterNames(List.of(backup))
      .setMasterCoprocessorNames(List.of()).setMasterTasks(List.of()).setLiveServerMetrics(Map.of())
      .setDeadServerNames(List.of()).build();

    var parsed = ShellLineParser.parse("status 'detailed'");
    TextResult result = (TextResult) command.execute(parsed, contextFor(metrics));
    List<String> lines = result.lines();

    assertTrue(lines.contains("version 3.0.0"));
    assertTrue(lines.contains("1 backup masters"));
    assertTrue(lines.contains("    backup.example.com:16000 2"));
    assertTrue(lines.contains("0 live servers"));
    assertTrue(lines.contains("0 dead servers"));
  }

  @Test
  public void summaryMatchesLegacyShellFormat() throws Exception {
    ServerName master = ServerName.valueOf("master.example.com", 16000, 1L);
    ServerName live = ServerName.valueOf("rs.example.com", 16020, 2L);
    ClusterMetrics metrics = ClusterMetricsBuilder.newBuilder().setHBaseVersion("3.0.0")
      .setMasterName(master).setBackerMasterNames(List.of()).setMasterCoprocessorNames(List.of())
      .setLiveServerMetrics(Map.of(live, org.apache.hadoop.hbase.ServerMetricsBuilder.of(live)))
      .setDeadServerNames(List.of()).build();

    var parsed = ShellLineParser.parse("status");
    TextResult result = (TextResult) command.execute(parsed, contextFor(metrics));

    assertTrue(result.lines().get(0).startsWith("1 active master, 0 backup masters,"));
    assertTrue(result.lines().get(1).equals("              1 servers,"));
    assertTrue(result.lines().get(2).equals("              0 decommissioned,"));
    assertTrue(result.lines().get(3).equals("              0 dead,"));
    assertTrue(result.lines().get(4).matches("              [0-9.]+ average load"));
  }

  @Test
  public void unsupportedFormatThrows() throws Exception {
    ClusterMetrics metrics = ClusterMetricsBuilder.newBuilder().setHBaseVersion("3.0.0")
      .setBackerMasterNames(List.of()).setMasterCoprocessorNames(List.of())
      .setLiveServerMetrics(Map.of()).setDeadServerNames(List.of()).build();

    var parsed = ShellLineParser.parse("status 'replication'");
    assertThrows(ShellCommandException.class,
      () -> command.execute(parsed, contextFor(metrics)));
  }
}
