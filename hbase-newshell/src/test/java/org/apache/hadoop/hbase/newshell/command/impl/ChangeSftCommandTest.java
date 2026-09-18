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
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class ChangeSftCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastTableName;
    private String lastFamily;
    private String lastSft;

    @Override
    public void changeSft(String tableName, String family, String sft) {
      this.lastTableName = tableName;
      this.lastFamily = family;
      this.lastSft = sft;
    }
  }

  private final ChangeSftCommand command = new ChangeSftCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void changesTableSftWithTwoArgs() throws Exception {
    var parsed = ShellLineParser.parse("change_sft 't1', 'FILE'");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertNull(admin.lastFamily);
    assertEquals("FILE", admin.lastSft);
  }

  @Test
  public void changesFamilySftWithThreeArgs() throws Exception {
    var parsed = ShellLineParser.parse("change_sft 't1', 'f1', 'FILE'");
    command.execute(parsed, context);

    assertEquals("t1", admin.lastTableName);
    assertEquals("f1", admin.lastFamily);
    assertEquals("FILE", admin.lastSft);
  }

  @Test
  public void throwsWhenArgCountWrong() throws Exception {
    var parsed = ShellLineParser.parse("change_sft 't1'");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
