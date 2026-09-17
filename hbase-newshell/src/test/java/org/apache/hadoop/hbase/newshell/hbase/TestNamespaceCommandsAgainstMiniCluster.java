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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of {@link DefaultShellAdmin}'s namespace CRUD methods
 * ({@code create_namespace}/{@code drop_namespace}/{@code alter_namespace}/
 * {@code describe_namespace}/{@code list_namespace}/{@code list_namespace_tables}) against a real
 * minicluster.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestNamespaceCommandsAgainstMiniCluster {
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

  @Test
  public void createNamespaceThenListNamespaceThenDropNamespaceRoundTrips() throws Exception {
    String namespace = "newshell_ns_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    admin.createNamespace(namespace, Map.of());
    assertTrue(admin.listNamespaces(".*").contains(namespace));

    admin.dropNamespace(namespace);
    assertFalse(admin.listNamespaces(".*").contains(namespace));
  }

  @Test
  public void createNamespaceAppliesConfigurationProperties() throws Exception {
    String namespace = "newshell_ns_props_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    admin.createNamespace(namespace, Map.of("PROPERTY_NAME", "PROPERTY_VALUE"));

    Admin realAdmin = connection.getAdmin();
    assertEquals("PROPERTY_VALUE",
      realAdmin.getNamespaceDescriptor(namespace).getConfigurationValue("PROPERTY_NAME"));

    admin.dropNamespace(namespace);
  }

  @Test
  public void alterNamespaceSetsAndUnsetsProperties() throws Exception {
    String namespace = "newshell_ns_alter_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createNamespace(namespace, Map.of());

    admin.alterNamespace(namespace, Map.of("METHOD", "set", "PROP", "VAL"));
    Admin realAdmin = connection.getAdmin();
    assertEquals("VAL", realAdmin.getNamespaceDescriptor(namespace).getConfigurationValue("PROP"));

    admin.alterNamespace(namespace, Map.of("METHOD", "unset", "NAME", "PROP"));
    assertEquals(null, realAdmin.getNamespaceDescriptor(namespace).getConfigurationValue("PROP"));

    admin.dropNamespace(namespace);
  }

  @Test
  public void describeNamespaceIncludesNamespaceName() throws Exception {
    String namespace = "newshell_ns_describe_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.createNamespace(namespace, Map.of());

    assertTrue(admin.describeNamespace(namespace).contains(namespace));

    admin.dropNamespace(namespace);
  }

  @Test
  public void listNamespaceTablesReturnsOnlyTablesInThatNamespace() throws Exception {
    String namespace = "newshell_ns_tables_" + UUID.randomUUID().toString().replace("-", "");
    Admin realAdmin = connection.getAdmin();
    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.createNamespace(namespace, Map.of());
    String tableName = namespace + ":t1";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    assertEquals(List.of("t1"), admin.listNamespaceTables(namespace));

    realAdmin.disableTable(TableName.valueOf(tableName));
    realAdmin.deleteTable(TableName.valueOf(tableName));
    admin.dropNamespace(namespace);
  }
}
