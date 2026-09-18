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
import java.util.UUID;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.coprocessor.CoprocessorHost;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.security.visibility.VisibilityConstants;
import org.apache.hadoop.hbase.security.visibility.VisibilityController;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of {@link DefaultShellAdmin#addLabels} and
 * {@link DefaultShellAdmin#listLabels} against a real minicluster with the
 * {@link VisibilityController} coprocessor enabled - mirrors {@code hbase/visibility_labels.rb}'s
 * expectations.
 */
@Tag(LargeTests.TAG)
@Tag(ClientTests.TAG)
public class TestVisibilityLabelsAgainstMiniCluster {
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static Connection connection;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    var conf = TEST_UTIL.getConfiguration();
    conf.setInt("hfile.format.version", 3);
    conf.setBoolean(User.HBASE_SECURITY_AUTHORIZATION_CONF_KEY, true);
    conf.set(CoprocessorHost.MASTER_COPROCESSOR_CONF_KEY, VisibilityController.class.getName());
    conf.set(CoprocessorHost.REGION_COPROCESSOR_CONF_KEY, VisibilityController.class.getName());
    conf.set("hbase.superuser", User.getCurrent().getName());
    TEST_UTIL.startMiniCluster(1);
    connection = TEST_UTIL.getConnection();
    TEST_UTIL.waitTableEnabled(VisibilityConstants.LABELS_TABLE_NAME.getName(), 50000);
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
    connection.close();
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test
  public void addLabelsThenListLabelsFindsTheNewLabel() throws Exception {
    String label = "newshell_label_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    admin.addLabels(List.of(label));

    List<String> labels = admin.listLabels(".*");
    assertTrue(labels.contains(label));
  }

  @Test
  public void listLabelsFiltersByRegex() throws Exception {
    String label = "newshell_regex_label_" + UUID.randomUUID().toString().replace("-", "");
    ShellAdmin admin = new DefaultShellAdmin(connection.getAdmin());

    admin.addLabels(List.of(label));

    assertTrue(admin.listLabels(label).contains(label));
    assertTrue(admin.listLabels("no_such_label_.*").isEmpty());
  }
}
