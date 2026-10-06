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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class GrantCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastUserOrGroup;
    private String lastActions;
    private String lastTableName;
    private String lastFamily;
    private String lastQualifier;
    private String lastNamespace;

    @Override
    public void grant(String userOrGroup, String actions, String tableName, String family,
      String qualifier, String namespace) {
      this.lastUserOrGroup = userOrGroup;
      this.lastActions = actions;
      this.lastTableName = tableName;
      this.lastFamily = family;
      this.lastQualifier = qualifier;
      this.lastNamespace = namespace;
    }
  }

  private final GrantCommand command = new GrantCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void grantsGlobalPermissions() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("grant 'bobsmith', 'RWXCA'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("bobsmith", admin.lastUserOrGroup);
    assertEquals("RWXCA", admin.lastActions);
    assertNull(admin.lastTableName);
    assertNull(admin.lastNamespace);
    assertEquals(Collections.emptyList(), result.lines());
  }

  @Test
  public void cellAclFormPassesPermissionsAndScanSpecToTable() throws Exception {
    java.util.Map<String, String>[] seenPerms = new java.util.Map[1];
    java.util.Map<String, Object>[] seenScan = new java.util.Map[1];
    org.apache.hadoop.hbase.newshell.hbase.StubShellTable table =
      new org.apache.hadoop.hbase.newshell.hbase.StubShellTable() {
        @Override
        public long setCellPermissions(java.util.Map<String, String> permissions,
          java.util.Map<String, Object> scanSpec) {
          seenPerms[0] = permissions;
          seenScan[0] = scanSpec;
          return 2;
        }
      };
    ExecutionContext cellContext = new ExecutionContext(admin, name -> {
      assertEquals("t1", name);
      return table;
    }, new PrintWriter(new StringWriter()));

    ParsedCommand parsed =
      ShellLineParser.parse("grant 't1', {'bob' => 'RW'}, {COLUMNS => ['f:q'], LIMIT => 5}");
    TextResult result = (TextResult) command.execute(parsed, cellContext);

    assertEquals(Collections.singletonList("2 row(s)"), result.lines());
    assertEquals("RW", seenPerms[0].get("bob"));
    assertEquals(5L, ((Number) seenScan[0].get("LIMIT")).longValue());
  }

  @Test
  public void cellAclFormRequiresScanSpec() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("grant 't1', {'bob' => 'RW'}");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }

  @Test
  public void grantsNamespacePermissions() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("grant 'bobsmith', 'RWXCA', '@ns1'");
    command.execute(parsed, context);

    assertEquals("ns1", admin.lastNamespace);
    assertNull(admin.lastTableName);
  }

  @Test
  public void grantsTableFamilyQualifierPermissions() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("grant 'bobsmith', 'RW', 't1', 'f1', 'col1'");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals("f1", admin.lastFamily);
    assertEquals("col1", admin.lastQualifier);
    assertNull(admin.lastNamespace);
  }

  @Test
  public void throwsWhenPermissionsMissing() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("grant 'bobsmith'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
