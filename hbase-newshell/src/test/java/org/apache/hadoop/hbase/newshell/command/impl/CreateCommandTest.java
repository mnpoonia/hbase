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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
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
public class CreateCommandTest {

  /** Captures the family specs it was called with instead of talking to a real Admin. */
  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastTableName;
    private List<Map<String, Object>> lastFamilySpecs;
    private Map<String, Object> lastTableAttributes;

    @Override
    public void createTable(String tableName, List<Map<String, Object>> familySpecs,
      Map<String, Object> tableAttributes) {
      this.lastTableName = tableName;
      this.lastFamilySpecs = familySpecs;
      this.lastTableAttributes = tableAttributes;
    }
  }

  private final CreateCommand command = new CreateCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void createsSingleFamilyFromLegacyHashLiteral() throws Exception {
    var parsed = ShellLineParser.parse("create 't1', {NAME => 'f1', VERSIONS => 3}");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals(List.of(Map.of("NAME", "f1", "VERSIONS", 3L)), admin.lastFamilySpecs);
    assertEquals(List.of("t1 created"), result.lines());
  }

  @Test
  public void createsSingleFamilyFromNativeFlagSyntax() throws Exception {
    var parsed = ShellLineParser.parse("create 't1' --name=f1 --versions=3");
    command.execute(parsed, context);

    assertEquals(List.of(Map.of("NAME", "f1", "VERSIONS", 3L)), admin.lastFamilySpecs);
  }

  @Test
  public void createsFamilyFromBarewordName() throws Exception {
    var parsed = ShellLineParser.parse("create 't1', 'f1'");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals(List.of(Map.of("NAME", "f1")), admin.lastFamilySpecs);
  }

  @Test
  public void createsMultipleFamiliesFromBarewordNames() throws Exception {
    var parsed = ShellLineParser.parse("create 't1', 'f1', 'f2'");
    command.execute(parsed, context);

    assertEquals(List.of(Map.of("NAME", "f1"), Map.of("NAME", "f2")), admin.lastFamilySpecs);
  }

  @Test
  public void createsMultipleFamiliesFromMultipleHashLiterals() throws Exception {
    var parsed =
      ShellLineParser.parse("create 't1', {NAME => 'f1'}, {NAME => 'f2', VERSIONS => 5}");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals(
      List.of(Map.of("NAME", "f1"), Map.of("NAME", "f2", "VERSIONS", 5L)), admin.lastFamilySpecs);
  }

  @Test
  public void bareTrailingAttributeHashWithoutNameBecomesTableAttribute() throws Exception {
    var parsed =
      ShellLineParser.parse("create 't1', {NAME => 'f1'}, SPLITS => ['1000', '2000']");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals(List.of(Map.of("NAME", "f1")), admin.lastFamilySpecs);
    assertEquals(List.of("1000", "2000"), admin.lastTableAttributes.get("SPLITS"));
  }

  @Test
  public void throwsWhenTableNameMissing() throws Exception {
    var parsed = ShellLineParser.parse("create");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }

  @Test
  public void throwsWhenNoFamilySpecGiven() throws Exception {
    var parsed = ShellLineParser.parse("create 't1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
