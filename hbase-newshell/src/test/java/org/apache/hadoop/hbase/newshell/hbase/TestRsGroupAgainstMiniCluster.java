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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.rsgroup.RSGroupAdminEndpoint;
import org.apache.hadoop.hbase.rsgroup.RSGroupBasedLoadBalancer;
import org.apache.hadoop.hbase.rsgroup.RSGroupInfo;
import org.apache.hadoop.hbase.rsgroup.RSGroupUtil;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of {@link DefaultShellAdmin#getRsGroup} and
 * {@link DefaultShellAdmin#moveServersToRsGroup} against a real minicluster with RSGroups enabled.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestRsGroupAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    var conf = TEST_UTIL.getConfiguration();
    conf.set("hbase.master.loadbalancer.class", RSGroupBasedLoadBalancer.class.getName());
    conf.setBoolean(RSGroupUtil.RS_GROUP_ENABLED, true);
    conf.set("hbase.coprocessor.master.classes", RSGroupAdminEndpoint.class.getName());
    TEST_UTIL.startMiniCluster(2);
    connection = TEST_UTIL.getConnection();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    connection.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test
  public void getRsGroupReturnsDefaultGroupWithAllServers() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    RsGroupView view = admin.getRsGroup(RSGroupInfo.DEFAULT_GROUP);
    assertEquals(2, view.servers().size());
    assertTrue(view.tables().isEmpty());
  }

  @Test
  public void moveServersToRsGroupRoundTrips() throws Exception {
    Admin realAdmin = connection.getAdmin();
    String groupName = "newshell_rsgroup_test";
    realAdmin.addRSGroup(groupName);

    RSGroupInfo defaultGroup = realAdmin.getRSGroup(RSGroupInfo.DEFAULT_GROUP);
    String hostPort = defaultGroup.getServers().iterator().next().toString();

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.moveServersToRsGroup(List.of(hostPort), groupName);

    RsGroupView movedGroup = admin.getRsGroup(groupName);
    assertEquals(List.of(hostPort), movedGroup.servers());

    RsGroupView remainingDefault = admin.getRsGroup(RSGroupInfo.DEFAULT_GROUP);
    assertEquals(1, remainingDefault.servers().size());

    admin.moveServersToRsGroup(List.of(hostPort), RSGroupInfo.DEFAULT_GROUP);
    realAdmin.removeRSGroup(groupName);
  }

  @Test
  public void addRsGroupThenListRsGroupsFindsTheNewGroup() throws Exception {
    Admin realAdmin = connection.getAdmin();
    String groupName = "newshell_add_list_rsgroup_test";

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.addRsGroup(groupName);

    List<RsGroupSummary> groups = admin.listRsGroups(".*");
    assertTrue(groups.stream().anyMatch(group -> group.name().equals(groupName)));

    realAdmin.removeRSGroup(groupName);
  }

  @Test
  public void listRsGroupsFiltersByRegexAndIncludesDefaultGroupServers() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    List<RsGroupSummary> groups = admin.listRsGroups(RSGroupInfo.DEFAULT_GROUP);
    assertEquals(1, groups.size());
    assertEquals(RSGroupInfo.DEFAULT_GROUP, groups.get(0).name());
    assertEquals(2, groups.get(0).servers().size());

    assertTrue(admin.listRsGroups("no_such_group_.*").isEmpty());
  }
}
