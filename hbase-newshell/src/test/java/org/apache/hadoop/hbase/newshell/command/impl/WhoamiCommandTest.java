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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class WhoamiCommandTest {
  private final WhoamiCommand command = new WhoamiCommand();
  private final ExecutionContext context = new ExecutionContext(new StubShellAdmin(),
    new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void printsCurrentUser() throws Exception {
    var parsed = ShellLineParser.parse("whoami");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertTrue(result.lines().get(0).contains(User.getCurrent().getName()));
    String[] groups = User.getCurrent().getGroupNames();
    if (groups != null && groups.length > 0) {
      assertEquals(2, result.lines().size());
      assertEquals("    groups: " + String.join(", ", groups), result.lines().get(1));
    } else {
      assertEquals(1, result.lines().size());
    }
  }
}
