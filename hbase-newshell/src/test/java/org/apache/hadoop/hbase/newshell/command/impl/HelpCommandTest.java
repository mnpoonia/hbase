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
import java.util.Arrays;
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
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
public class HelpCommandTest {
  private final HelpCommand command =
    new HelpCommand(new CommandRegistry(Arrays.asList(new VersionCommand(), new HelpCommand())));
  private final ExecutionContext context = new ExecutionContext(new StubShellAdmin(),
    new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void listsAllCommandsSorted() throws Exception {
    TextResult result = (TextResult) command.execute(ShellLineParser.parse("help"), context);

    assertEquals(2, result.lines().size());
    assertTrue(result.lines().get(0).startsWith("help"));
    assertTrue(result.lines().get(1).startsWith("version"));
  }

  @Test
  public void showsSingleCommand() throws Exception {
    TextResult result =
      (TextResult) command.execute(ShellLineParser.parse("help 'version'"), context);

    assertEquals(Arrays.asList(new VersionCommand().help()), result.lines());
  }

  @Test
  public void unknownCommandFails() throws Exception {
    assertThrows(ShellCommandException.class,
      () -> command.execute(ShellLineParser.parse("help 'nope'"), context));
  }

  @Test
  public void tooManyArgsFails() throws Exception {
    assertThrows(ShellCommandException.class,
      () -> command.execute(ShellLineParser.parse("help 'a', 'b'"), context));
  }
}
