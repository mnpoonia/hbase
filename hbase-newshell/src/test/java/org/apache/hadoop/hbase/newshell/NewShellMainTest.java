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
package org.apache.hadoop.hbase.newshell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.hadoop.hbase.newshell.spi.Completer;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class NewShellMainTest {

  /** Minimal in-memory {@link ShellTerminal} fake - no real terminal/JLine involved. */
  private static final class FakeShellTerminal implements ShellTerminal {
    private final Deque<String> lines;
    private final StringWriter buffer = new StringWriter();
    private final PrintWriter writer = new PrintWriter(buffer);
    private boolean closed;

    FakeShellTerminal(String... lines) {
      this.lines = new ArrayDeque<>(java.util.Arrays.asList(lines));
    }

    @Override
    public String readLine(String prompt) {
      return lines.poll();
    }

    @Override
    public PrintWriter writer() {
      return writer;
    }

    @Override
    public void setCompleter(Completer completer) {
      // not exercised by this stub-level test
    }

    @Override
    public void close() {
      closed = true;
    }

    String output() {
      writer.flush();
      return buffer.toString();
    }
  }

  @Test
  public void echoesLinesUntilExitCommand() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("status", "exit", "list");
    NewShellMain.run(terminal);
    String output = terminal.output();
    assertTrue(output.contains("newshell (stub): status"));
    assertTrue(!output.contains("list"), "lines after exit must not be processed");
  }

  @Test
  public void stopsCleanlyOnEndOfInput() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("whoami");
    NewShellMain.run(terminal);
    assertEquals("newshell (stub): whoami" + System.lineSeparator(), terminal.output());
  }

  @Test
  public void quitCommandIsCaseInsensitive() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("QUIT", "should-not-run");
    NewShellMain.run(terminal);
    assertEquals("", terminal.output());
  }
}
