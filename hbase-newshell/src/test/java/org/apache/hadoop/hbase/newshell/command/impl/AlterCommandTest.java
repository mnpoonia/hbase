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
public class AlterCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastAlteredTable;
    private List<Map<String, Object>> lastFamilySpecs;

    @Override
    public void alterTable(String tableName, List<Map<String, Object>> familySpecs) {
      this.lastAlteredTable = tableName;
      this.lastFamilySpecs = familySpecs;
    }
  }

  private final AlterCommand command = new AlterCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void altersColumnFamilyAttributes() throws Exception {
    var parsed = ShellLineParser.parse("alter 't1', {NAME => 'f1', TTL => 100}");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", admin.lastAlteredTable);
    assertEquals(1, admin.lastFamilySpecs.size());
    assertEquals(Map.of("NAME", "f1", "TTL", 100L), admin.lastFamilySpecs.get(0));
    assertEquals(List.of("Updating all regions with the new schema..."), result.lines());
  }

  @Test
  public void altersMultipleColumnFamilies() throws Exception {
    var parsed =
      ShellLineParser.parse("alter 't1', {NAME => 'f1', TTL => 100}, {NAME => 'f2', VERSIONS => 3}");
    command.execute(parsed, context);

    assertEquals(2, admin.lastFamilySpecs.size());
  }

  @Test
  public void throwsWhenNoFamilySpecGiven() throws Exception {
    var parsed = ShellLineParser.parse("alter 't1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
