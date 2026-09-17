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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class CreateNamespaceCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastNamespace;
    private Map<String, Object> lastProperties;

    @Override
    public void createNamespace(String namespace, Map<String, Object> properties) {
      this.lastNamespace = namespace;
      this.lastProperties = properties;
    }
  }

  private final CreateNamespaceCommand command = new CreateNamespaceCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void createsWithoutProperties() throws Exception {
    var parsed = ShellLineParser.parse("create_namespace 'ns1'");
    command.execute(parsed, context);

    assertEquals("ns1", admin.lastNamespace);
    assertEquals(Map.of(), admin.lastProperties);
  }

  @Test
  public void createsWithProperties() throws Exception {
    var parsed = ShellLineParser.parse("create_namespace 'ns1', {'PROPERTY_NAME'=>'PROPERTY_VALUE'}");
    command.execute(parsed, context);

    assertEquals("ns1", admin.lastNamespace);
    assertEquals("PROPERTY_VALUE", admin.lastProperties.get("PROPERTY_NAME"));
  }
}
