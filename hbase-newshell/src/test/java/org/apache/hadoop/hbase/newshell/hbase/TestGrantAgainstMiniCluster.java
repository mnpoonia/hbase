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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.coprocessor.CoprocessorHost;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.AccessController;
import org.apache.hadoop.hbase.security.access.Permission;
import org.apache.hadoop.hbase.security.access.UserPermission;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of {@link DefaultShellAdmin#grant} against a real minicluster with the
 * {@link AccessController} coprocessor enabled - mirrors {@code hbase/security_manager.rb}'s
 * expectations for the global/namespace/table+family+qualifier grant forms.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestGrantAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    var conf = TEST_UTIL.getConfiguration();
    conf.set(CoprocessorHost.MASTER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
    conf.set(CoprocessorHost.REGION_COPROCESSOR_CONF_KEY, AccessController.class.getName());
    conf.set(CoprocessorHost.REGIONSERVER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
    conf.set(User.HBASE_SECURITY_AUTHORIZATION_CONF_KEY, "true");
    conf.set("hbase.security.exec.permission.checks", "true");
    conf.set("hbase.superuser", User.getCurrent().getName());
    TEST_UTIL.startMiniCluster(1);
    connection = TEST_UTIL.getConnection();
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    connection.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  private static Permission onlyPermissionFor(String userName, String tableRegex)
    throws Throwable {
    for (UserPermission permission : AccessControlClient.getUserPermissions(connection,
      tableRegex, userName)) {
      if (permission.getUser().equals(userName)) {
        return permission.getPermission();
      }
    }
    return null;
  }

  @Test
  public void grantsGlobalPermissions() throws Throwable {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.grant("newshell_global_user", "RWXCA", null, null, null, null);

    Permission granted = onlyPermissionFor("newshell_global_user", null);
    assertTrue(granted != null && granted.implies(Permission.Action.READ)
      && granted.implies(Permission.Action.WRITE) && granted.implies(Permission.Action.EXEC)
      && granted.implies(Permission.Action.CREATE) && granted.implies(Permission.Action.ADMIN));
  }

  @Test
  public void grantsNamespacePermissions() throws Throwable {
    String namespace = "newshell_grant_ns";
    Admin realAdmin = connection.getAdmin();
    realAdmin.createNamespace(NamespaceDescriptor.create(namespace).build());

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.grant("newshell_ns_user", "RW", null, null, null, namespace);

    Permission granted = onlyPermissionFor("newshell_ns_user", "@" + namespace);
    assertTrue(granted != null && granted.implies(Permission.Action.READ)
      && granted.implies(Permission.Action.WRITE)
      && !granted.implies(Permission.Action.ADMIN));
  }

  @Test
  public void grantsTableFamilyQualifierPermissions() throws Throwable {
    String tableName = "newshell_grant_table_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.grant("newshell_table_user", "RW", tableName, "f1", "c1", null);

    Permission granted = onlyPermissionFor("newshell_table_user", tableName);
    assertTrue(granted != null && granted.implies(Permission.Action.READ)
      && granted.implies(Permission.Action.WRITE));
  }

  @Test
  public void revokeRemovesPreviouslyGrantedGlobalPermissions() throws Throwable {
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.grant("newshell_revoke_global_user", "RW", null, null, null, null);
    assertTrue(onlyPermissionFor("newshell_revoke_global_user", null) != null);

    admin.revoke("newshell_revoke_global_user", null, null, null, null);
    assertTrue(onlyPermissionFor("newshell_revoke_global_user", null) == null);
  }

  @Test
  public void revokeRemovesPreviouslyGrantedNamespacePermissions() throws Throwable {
    String namespace = "newshell_revoke_ns";
    Admin realAdmin = connection.getAdmin();
    realAdmin.createNamespace(NamespaceDescriptor.create(namespace).build());

    ShellAdmin admin = new DefaultShellAdmin(realAdmin);
    admin.grant("newshell_revoke_ns_user", "RW", null, null, null, namespace);
    assertTrue(onlyPermissionFor("newshell_revoke_ns_user", "@" + namespace) != null);

    admin.revoke("newshell_revoke_ns_user", null, null, null, namespace);
    assertTrue(onlyPermissionFor("newshell_revoke_ns_user", "@" + namespace) == null);
  }

  @Test
  public void revokeRemovesPreviouslyGrantedTablePermissions() throws Throwable {
    String tableName = "newshell_revoke_table_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.grant("newshell_revoke_table_user", "RW", tableName, null, null, null);
    assertTrue(onlyPermissionFor("newshell_revoke_table_user", tableName) != null);

    admin.revoke("newshell_revoke_table_user", tableName, null, null, null);
    assertTrue(onlyPermissionFor("newshell_revoke_table_user", tableName) == null);
  }

  @Test
  public void userPermissionReturnsGrantedTablePermission() throws Throwable {
    String tableName = "newshell_user_permission_table_test";
    TEST_UTIL.createTable(TableName.valueOf(tableName), Bytes.toBytes("f1"));

    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());
    admin.grant("newshell_user_permission_user", "RW", tableName, null, null, null);

    List<List<String>> rows = admin.userPermission(tableName);
    assertTrue(rows.stream().anyMatch(row -> row.get(0).equals("newshell_user_permission_user")));
  }
}
