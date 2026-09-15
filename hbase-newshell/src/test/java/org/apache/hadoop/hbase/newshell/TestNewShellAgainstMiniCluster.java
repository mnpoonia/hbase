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
package org.apache.hadoop.hbase.newshell;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.impl.AddPeerCommand;
import org.apache.hadoop.hbase.newshell.command.impl.AlterCommand;
import org.apache.hadoop.hbase.newshell.command.impl.BalanceSwitchCommand;
import org.apache.hadoop.hbase.newshell.command.impl.BalancerEnabledCommand;
import org.apache.hadoop.hbase.newshell.command.impl.CatalogjanitorEnabledCommand;
import org.apache.hadoop.hbase.newshell.command.impl.CatalogjanitorSwitchCommand;
import org.apache.hadoop.hbase.newshell.command.impl.CompactCommand;
import org.apache.hadoop.hbase.newshell.command.impl.CompactionSwitchCommand;
import org.apache.hadoop.hbase.newshell.command.impl.CreateCommand;
import org.apache.hadoop.hbase.newshell.command.impl.DecommissionRegionServersCommand;
import org.apache.hadoop.hbase.newshell.command.impl.DeleteSnapshotCommand;
import org.apache.hadoop.hbase.newshell.command.impl.DescribeCommand;
import org.apache.hadoop.hbase.newshell.command.impl.DisableCommand;
import org.apache.hadoop.hbase.newshell.command.impl.DropCommand;
import org.apache.hadoop.hbase.newshell.command.impl.EnableCommand;
import org.apache.hadoop.hbase.newshell.command.impl.ExistsCommand;
import org.apache.hadoop.hbase.newshell.command.impl.GetCommand;
import org.apache.hadoop.hbase.newshell.command.impl.ListCommand;
import org.apache.hadoop.hbase.newshell.command.impl.ListDecommissionedRegionServersCommand;
import org.apache.hadoop.hbase.newshell.command.impl.ListPeersCommand;
import org.apache.hadoop.hbase.newshell.command.impl.ListSnapshotsCommand;
import org.apache.hadoop.hbase.newshell.command.impl.MajorCompactCommand;
import org.apache.hadoop.hbase.newshell.command.impl.NormalizerEnabledCommand;
import org.apache.hadoop.hbase.newshell.command.impl.PutCommand;
import org.apache.hadoop.hbase.newshell.command.impl.RecommissionRegionServerCommand;
import org.apache.hadoop.hbase.newshell.command.impl.NormalizerSwitchCommand;
import org.apache.hadoop.hbase.newshell.command.impl.RemovePeerCommand;
import org.apache.hadoop.hbase.newshell.command.impl.SnapshotCommand;
import org.apache.hadoop.hbase.newshell.command.impl.SplitCommand;
import org.apache.hadoop.hbase.newshell.command.impl.SplitormergeEnabledCommand;
import org.apache.hadoop.hbase.newshell.command.impl.SplitormergeSwitchCommand;
import org.apache.hadoop.hbase.newshell.command.impl.StatusCommand;
import org.apache.hadoop.hbase.newshell.format.DefaultFormatter;
import org.apache.hadoop.hbase.newshell.hbase.DefaultShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.DefaultShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.SelfReplicationEndpointForTest;
import org.apache.hadoop.hbase.newshell.hbase.ShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.spi.Completer;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end "typed line in, rendered text out" test of the full newshell stack - real parser,
 * real {@link CommandRegistry} of all pilot commands, real {@link DefaultShellAdmin}/
 * {@link DefaultShellTableFactory} wired to an actual minicluster - driven through
 * {@link NewShellMain#run}. This is the newshell analogue of hbase-shell's Ruby-script-driven
 * {@code AbstractTestShell}: unlike {@code TestPilotCommandsAgainstMiniCluster} (which calls the
 * wrapper layer directly), this test feeds the exact strings a user would type and asserts on
 * the terminal's rendered output.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestNewShellAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    TEST_UTIL.startMiniCluster(1);
    connection = TEST_UTIL.getConnection();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    connection.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  private static final class ScriptedShellTerminal implements ShellTerminal {
    private final Deque<String> lines;
    private final StringWriter buffer = new StringWriter();
    private final PrintWriter writer = new PrintWriter(buffer);

    ScriptedShellTerminal(String... lines) {
      this.lines = new ArrayDeque<>(Arrays.asList(lines));
    }

    @Override
    public String readLine(String prompt) {
      return lines.poll();
    }

    @Override
    public PrintWriter writer() {
      return writer;
    }

    @Override
    public void setCompleter(Completer completer) {
    }

    @Override
    public void close() {
    }

    String output() {
      writer.flush();
      return buffer.toString();
    }
  }

  private void runScript(String... lines) throws IOException {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    ShellTableFactory tables = new DefaultShellTableFactory(connection);
    ScriptedShellTerminal terminal = new ScriptedShellTerminal(lines);
    ExecutionContext context = new ExecutionContext(admin, tables, terminal.writer());
    CommandRegistry registry = new CommandRegistry(
      List.of(new StatusCommand(), new CreateCommand(), new DisableCommand(), new GetCommand(),
        new EnableCommand(), new DropCommand(), new PutCommand(), new ListCommand(),
        new DescribeCommand(), new DecommissionRegionServersCommand(),
        new RecommissionRegionServerCommand(), new ListDecommissionedRegionServersCommand(),
        new AlterCommand(), new ExistsCommand(), new CompactCommand(), new MajorCompactCommand(),
        new SplitCommand(), new AddPeerCommand(), new RemovePeerCommand(),
        new ListPeersCommand(), new SnapshotCommand(), new DeleteSnapshotCommand(),
        new ListSnapshotsCommand(), new BalanceSwitchCommand(), new NormalizerSwitchCommand(),
        new CatalogjanitorSwitchCommand(), new CompactionSwitchCommand(),
        new SplitormergeSwitchCommand(), new BalancerEnabledCommand(),
        new CatalogjanitorEnabledCommand(), new NormalizerEnabledCommand(),
        new SplitormergeEnabledCommand()));
    NewShellMain.run(terminal, context, registry, new DefaultFormatter());
    lastOutput = terminal.output();
  }

  private String lastOutput;

  @Test
  public void statusCommandPrintsLiveServerCount() throws Exception {
    runScript("status", "exit");
    assertTrue(lastOutput.contains("live servers"));
  }

  @Test
  public void statusDetailedPrintsActiveMasterAndLiveServers() throws Exception {
    runScript("status 'detailed'", "exit");
    assertTrue(lastOutput.contains("active master:  "));
    assertTrue(lastOutput.contains("live servers"));
    assertTrue(lastOutput.contains("regionsInTransition"));
  }

  @Test
  public void createWithMultipleHashLiteralsBuildsAllFamilies() throws Exception {
    String tableName = "newshell_typed_multi_family";
    runScript("create '" + tableName + "', {NAME => 'f1'}, {NAME => 'f2'}", "exit");
    assertTrue(lastOutput.contains(tableName + " created"));
    var descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertTrue(descriptor.hasColumnFamily(Bytes.toBytes("f1")));
    assertTrue(descriptor.hasColumnFamily(Bytes.toBytes("f2")));
  }

  @Test
  public void createThenGetRoundTripsCellsAcrossMultipleFamilies() throws Exception {
    String tableName = "newshell_typed_create_then_get";
    runScript("create '" + tableName + "', {NAME => 'f1'}, {NAME => 'f2'}", "exit");

    try (Table table = connection.getTable(TableName.valueOf(tableName))) {
      Put put = new Put(Bytes.toBytes("r1"));
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes("v1"));
      put.addColumn(Bytes.toBytes("f2"), Bytes.toBytes("c1"), Bytes.toBytes("v2"));
      table.put(put);
    }

    runScript("get '" + tableName + "', 'r1', {COLUMN => ['f1:c1', 'f2:c1']}", "exit");
    assertTrue(lastOutput.contains("f1:c1"));
    assertTrue(lastOutput.contains("f2:c1"));
    assertTrue(lastOutput.contains("v1"));
    assertTrue(lastOutput.contains("v2"));
  }

  @Test
  public void createThenPutThenGetRoundTripsACell() throws Exception {
    String tableName = "newshell_typed_create_then_put";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("put '" + tableName + "', 'r1', 'f1:c1', 'v1'", "exit");
    assertTrue(lastOutput.contains("1 row(s) put"));

    runScript("get '" + tableName + "', 'r1'", "exit");
    assertTrue(lastOutput.contains("v1"));
  }

  @Test
  public void disableThenEnableThenDropRoundTrips() throws Exception {
    String tableName = "newshell_typed_disable_enable_drop";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("disable '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains(tableName + " disabled"));

    runScript("enable '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains(tableName + " enabled"));

    runScript("disable '" + tableName + "'", "drop '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains(tableName + " dropped"));
    assertTrue(!connection.getAdmin().tableExists(TableName.valueOf(tableName)));
  }

  @Test
  public void listShowsCreatedTable() throws Exception {
    String tableName = "newshell_typed_list";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("list", "exit");
    assertTrue(lastOutput.contains(tableName));
  }

  @Test
  public void describeShowsEnabledStatusAndColumnFamily() throws Exception {
    String tableName = "newshell_typed_describe";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("describe '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains("Table " + tableName + " is ENABLED"));
    assertTrue(lastOutput.contains("f1"));
  }

  @Test
  public void decommissionThenListThenRecommissionRoundTrips() throws Exception {
    Admin realAdmin = connection.getAdmin();
    ServerName liveServer =
      realAdmin.getClusterMetrics().getLiveServerMetrics().keySet().iterator().next();

    runScript("decommission_regionservers '" + liveServer.getHostname() + "'", "exit");
    assertTrue(lastOutput.contains("1 region server(s) decommissioned"));

    runScript("list_decommissioned_regionservers", "exit");
    assertTrue(lastOutput.contains(liveServer.getServerName()));

    runScript("recommission_regionserver '" + liveServer.getHostname() + "'", "exit");
    assertTrue(lastOutput.contains("recommissioned"));

    runScript("list_decommissioned_regionservers", "exit");
    assertTrue(!lastOutput.contains(liveServer.getServerName()));
  }

  @Test
  public void alterChangesColumnFamilyTtl() throws Exception {
    String tableName = "newshell_typed_alter";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("alter '" + tableName + "', {NAME => 'f1', TTL => 100}", "exit");
    assertTrue(lastOutput.contains(tableName + " altered"));
    var descriptor = connection.getAdmin().getDescriptor(TableName.valueOf(tableName));
    assertTrue(descriptor.getColumnFamily(Bytes.toBytes("f1")).getTimeToLive() == 100);
  }

  @Test
  public void existsReportsPresenceAndAbsence() throws Exception {
    String tableName = "newshell_typed_exists";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("exists '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains(tableName + " does exist"));

    runScript("exists 'newshell_typed_exists_missing'", "exit");
    assertTrue(lastOutput.contains("newshell_typed_exists_missing does not exist"));
  }

  @Test
  public void compactMajorCompactAndSplitDoNotThrow() throws Exception {
    String tableName = "newshell_typed_maintenance";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("compact '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains(tableName + " compaction requested"));

    runScript("major_compact '" + tableName + "'", "exit");
    assertTrue(lastOutput.contains(tableName + " major compaction requested"));

    runScript("split '" + tableName + "', 'm'", "exit");
    assertTrue(lastOutput.contains(tableName + " split requested"));
  }

  @Test
  public void addPeerThenListPeersThenRemovePeerRoundTrips() throws Exception {
    String peerId = "newshell_typed_peer_" + UUID.randomUUID().toString().replace("-", "");
    String clusterKey = TEST_UTIL.getClusterKey();

    runScript("add_peer '" + peerId + "', CLUSTER_KEY => '" + clusterKey + "', "
      + "ENDPOINT_CLASSNAME => '" + SelfReplicationEndpointForTest.class.getName() + "'", "exit");
    assertTrue(lastOutput.contains(peerId + " peer added"));

    runScript("list_peers", "exit");
    assertTrue(lastOutput.contains(peerId));

    runScript("remove_peer '" + peerId + "'", "exit");
    assertTrue(lastOutput.contains(peerId + " peer removed"));

    runScript("list_peers", "exit");
    assertTrue(!lastOutput.contains(peerId));
  }

  @Test
  public void snapshotThenListSnapshotsThenDeleteSnapshotRoundTrips() throws Exception {
    String tableName = "newshell_typed_snapshot";
    String snapshotName = "newshell_typed_snapshot_snap";
    runScript("create '" + tableName + "', {NAME => 'f1'}", "exit");

    runScript("snapshot '" + tableName + "', '" + snapshotName + "'", "exit");
    assertTrue(lastOutput.contains(snapshotName + " snapshot of table " + tableName + " created"));

    runScript("list_snapshots", "exit");
    assertTrue(lastOutput.contains(snapshotName));

    runScript("delete_snapshot '" + snapshotName + "'", "exit");
    assertTrue(lastOutput.contains(snapshotName + " snapshot deleted"));

    runScript("list_snapshots", "exit");
    assertTrue(!lastOutput.contains(snapshotName));
  }

  @Test
  public void clusterSwitchesToggleAndReportPreviousState() throws Exception {
    runScript("balance_switch false", "exit");
    assertTrue(lastOutput.contains("Previous balancer state : true"));
    runScript("balance_switch true", "exit");

    runScript("normalizer_switch false", "exit");
    assertTrue(lastOutput.contains("true"));
    runScript("normalizer_switch true", "exit");

    runScript("catalogjanitor_switch false", "exit");
    assertTrue(lastOutput.contains("true"));
    runScript("catalogjanitor_switch true", "exit");

    runScript("compaction_switch false", "exit");
    runScript("compaction_switch true", "exit");
    assertTrue(lastOutput.contains("SERVER"));

    runScript("splitormerge_switch 'SPLIT', false", "exit");
    assertTrue(lastOutput.contains("true"));
    runScript("splitormerge_switch 'SPLIT', true", "exit");

    runScript("splitormerge_switch 'MERGE', false", "exit");
    assertTrue(lastOutput.contains("true"));
    runScript("splitormerge_switch 'MERGE', true", "exit");
  }

  @Test
  public void enabledQueriesReportCurrentClusterSwitchStates() throws Exception {
    runScript("balancer_enabled", "exit");
    assertTrue(lastOutput.contains("true"));

    runScript("catalogjanitor_enabled", "exit");
    assertTrue(lastOutput.contains("true"));

    runScript("normalizer_enabled", "exit");
    assertTrue(lastOutput.contains("true"));

    runScript("splitormerge_enabled 'SPLIT'", "exit");
    assertTrue(lastOutput.contains("true"));

    runScript("splitormerge_enabled 'MERGE'", "exit");
    assertTrue(lastOutput.contains("true"));
  }
}
