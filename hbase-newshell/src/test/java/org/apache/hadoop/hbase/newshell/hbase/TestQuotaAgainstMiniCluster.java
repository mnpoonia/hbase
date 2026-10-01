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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of quota shell APIs against a real minicluster with quota support enabled
 * - mirrors {@code hbase/quotas.rb}'s THROTTLE forms and rpc/exceed-throttle switches.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestQuotaAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    Configuration conf = TEST_UTIL.getConfiguration();
    conf.setBoolean("hbase.quota.enabled", true);
    TEST_UTIL.startMiniCluster(1);
    connection = TEST_UTIL.getConnection();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    connection.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test
  public void setQuotaThenListQuotasFindsAUserThrottle() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    String user = "newshell_quota_user";

    admin.setQuota(
      mapOf("TYPE", "THROTTLE", "USER", user, "THROTTLE_TYPE", "REQUEST", "LIMIT", "10req/sec"));

    List<List<String>> rows = admin.listQuotas(Collections.singletonMap("USER", user));
    assertTrue(rows.stream().anyMatch(row -> row.get(0).contains(user)));

    admin.setQuota(mapOf("TYPE", "THROTTLE", "USER", user, "LIMIT", "NONE"));
    List<List<String>> afterUnthrottle = admin.listQuotas(Collections.singletonMap("USER", user));
    assertFalse(afterUnthrottle.stream().anyMatch(row -> row.get(0).contains(user)));
  }

  @Test
  public void setQuotaAppliesANamespaceThrottle() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    String namespace = "newshell_quota_ns";
    connection.getAdmin()
      .createNamespace(org.apache.hadoop.hbase.NamespaceDescriptor.create(namespace).build());

    admin.setQuota(mapOf("TYPE", "THROTTLE", "NAMESPACE", namespace, "LIMIT", "1000000b/sec"));

    List<List<String>> rows = admin.listQuotas(Collections.singletonMap("NAMESPACE", namespace));
    assertTrue(rows.stream().anyMatch(row -> row.get(0).contains(namespace)));

    admin.setQuota(mapOf("TYPE", "THROTTLE", "NAMESPACE", namespace, "LIMIT", "NONE"));
  }

  @Test
  public void rpcThrottleSwitchRoundTrips() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    boolean beforeDisable = admin.switchRpcThrottle(false);
    assertFalse(admin.isRpcThrottleEnabled());
    boolean beforeEnable = admin.switchRpcThrottle(true);
    assertFalse(beforeEnable);
    assertTrue(admin.isRpcThrottleEnabled());
    // restore prior state if the cluster started disabled
    if (!beforeDisable) {
      admin.switchRpcThrottle(false);
    }
  }

  @Test
  public void exceedThrottleQuotaSwitchRoundTrips() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    // Enabling exceed-throttle requires region-server READ+WRITE quotas in seconds.
    admin.setQuota(mapOf("TYPE", "THROTTLE", "REGIONSERVER", "all", "THROTTLE_TYPE", "WRITE",
      "LIMIT", "100req/sec"));
    admin.setQuota(mapOf("TYPE", "THROTTLE", "REGIONSERVER", "all", "THROTTLE_TYPE", "READ",
      "LIMIT", "20req/sec"));

    assertFalse(admin.switchExceedThrottleQuota(true));
    assertTrue(admin.switchExceedThrottleQuota(true));
    assertTrue(admin.switchExceedThrottleQuota(false));
    assertFalse(admin.switchExceedThrottleQuota(false));

    admin.setQuota(mapOf("TYPE", "THROTTLE", "REGIONSERVER", "all", "LIMIT", "NONE"));
  }

  @Test
  public void listQuotaTableSizesSnapshotsAndSnapshotSizesDoNotThrow() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    assertDoesNotThrow(admin::listQuotaTableSizes);
    assertDoesNotThrow(() -> admin.listQuotaSnapshots(Collections.emptyMap()));
    assertDoesNotThrow(admin::listSnapshotSizes);
  }

  private static Map<String, Object> mapOf(Object... keyValuePairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }
}
