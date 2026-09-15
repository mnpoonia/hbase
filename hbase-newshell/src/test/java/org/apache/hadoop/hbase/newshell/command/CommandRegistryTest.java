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
package org.apache.hadoop.hbase.newshell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class CommandRegistryTest {

  private static final class FakeCommand implements ShellCommand {
    private final String name;

    FakeCommand(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String help() {
      return name;
    }

    @Override
    public CommandResult execute(ParsedCommand command, ExecutionContext context)
      throws ShellCommandException, IOException {
      throw new UnsupportedOperationException("not needed for this test");
    }
  }

  @Test
  public void looksUpCommandByNameCaseInsensitively() {
    CommandRegistry registry = new CommandRegistry(List.of(new FakeCommand("status")));
    assertTrue(registry.lookup("STATUS").isPresent());
    assertEquals("status", registry.lookup("status").get().name());
  }

  @Test
  public void returnsEmptyForUnknownCommand() {
    CommandRegistry registry = new CommandRegistry(List.of(new FakeCommand("status")));
    assertFalse(registry.lookup("nope").isPresent());
  }

  @Test
  public void throwsOnDuplicateCommandName() {
    assertThrows(IllegalStateException.class,
      () -> new CommandRegistry(List.of(new FakeCommand("status"), new FakeCommand("STATUS"))));
  }
}
