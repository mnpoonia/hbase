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
public class SnapshotCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastTableName;
    private String lastSnapshotName;

    private java.util.Map<String, Object> lastOptions;

    @Override
    public void snapshot(String tableName, String snapshotName, java.util.Map<String, Object> o) {
      this.lastTableName = tableName;
      this.lastSnapshotName = snapshotName;
      this.lastOptions = o;
    }

    @Override
    public void snapshot(String tableName, String snapshotName) {
      this.lastTableName = tableName;
      this.lastSnapshotName = snapshotName;
    }
  }

  private final SnapshotCommand command = new SnapshotCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void takesSnapshotOfTable() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("snapshot 't1', 'snap1'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals("snap1", admin.lastSnapshotName);
    assertEquals(Collections.emptyList(), result.lines());
  }

  @Test
  public void passesOptionsHash() throws Exception {
    command.execute(
      ShellLineParser.parse("snapshot 't1', 'snap1', {TTL => 3600, SKIP_FLUSH => true}"), context);

    assertEquals("snap1", admin.lastSnapshotName);
    assertEquals(3600L, ((Number) admin.lastOptions.get("TTL")).longValue());
    assertEquals("true", String.valueOf(admin.lastOptions.get("SKIP_FLUSH")));
  }

  @Test
  public void throwsWhenSnapshotNameMissing() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("snapshot 't1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
