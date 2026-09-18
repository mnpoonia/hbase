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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of {@link DefaultShellAdmin#setQuota} and
 * {@link DefaultShellAdmin#listQuotas} against a real minicluster with quota support enabled -
 * mirrors {@code hbase/quotas.rb}'s THROTTLE quota forms.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestQuotaAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    var conf = TEST_UTIL.getConfiguration();
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

    admin.setQuota(Map.of("TYPE", "THROTTLE", "USER", user, "THROTTLE_TYPE", "REQUEST", "LIMIT",
      "10req/sec"));

    List<List<String>> rows = admin.listQuotas(Map.of("USER", user));
    assertTrue(rows.stream().anyMatch(row -> row.get(0).contains(user)));

    admin.setQuota(Map.of("TYPE", "THROTTLE", "USER", user, "LIMIT", "NONE"));
    List<List<String>> afterUnthrottle = admin.listQuotas(Map.of("USER", user));
    assertFalse(afterUnthrottle.stream().anyMatch(row -> row.get(0).contains(user)));
  }

  @Test
  public void setQuotaAppliesANamespaceThrottle() throws Exception {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    String namespace = "newshell_quota_ns";
    connection.getAdmin()
      .createNamespace(org.apache.hadoop.hbase.NamespaceDescriptor.create(namespace).build());

    admin.setQuota(Map.of("TYPE", "THROTTLE", "NAMESPACE", namespace, "LIMIT", "1000000b/sec"));

    List<List<String>> rows = admin.listQuotas(Map.of("NAMESPACE", namespace));
    assertTrue(rows.stream().anyMatch(row -> row.get(0).contains(namespace)));

    admin.setQuota(Map.of("TYPE", "THROTTLE", "NAMESPACE", namespace, "LIMIT", "NONE"));
  }
}
