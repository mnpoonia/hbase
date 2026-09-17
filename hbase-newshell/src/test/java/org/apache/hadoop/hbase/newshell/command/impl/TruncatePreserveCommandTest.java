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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class TruncatePreserveCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastTableName;
    private Boolean lastPreserveSplits;

    @Override
    public void truncateTable(String tableName, boolean preserveSplits) {
      this.lastTableName = tableName;
      this.lastPreserveSplits = preserveSplits;
    }
  }

  private final TruncatePreserveCommand command = new TruncatePreserveCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void truncatesPreservingSplits() throws Exception {
    var parsed = ShellLineParser.parse("truncate_preserve 't1'");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertTrue(admin.lastPreserveSplits);
  }

  @Test
  public void throwsWhenTableNameMissing() throws Exception {
    var parsed = ShellLineParser.parse("truncate_preserve");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
