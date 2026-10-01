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
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.hadoop.hbase.newshell.spi.Completer;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.SupportsHistory;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class NewShellMainHistoryTest {

  @Test
  public void savesHistoryWhenTerminalSupportsIt() throws IOException {
    RecordingHistoryTerminal terminal = new RecordingHistoryTerminal();
    NewShellMain.saveHistoryIfSupported(terminal);
    assertTrue(terminal.saved);
  }

  @Test
  public void doesNothingWhenTerminalDoesNotSupportHistory() throws IOException {
    PlainTerminal terminal = new PlainTerminal();
    NewShellMain.saveHistoryIfSupported(terminal);
    // No exception, nothing to assert on PlainTerminal - absence of a SupportsHistory
    // implementation means there is nothing to save.
  }

  @Test
  public void defaultHistoryFileLivesUnderUserHome() {
    Path expected = Paths.get(System.getProperty("user.home"), ".hbase-newshell-history");
    assertEquals(expected, NewShellMain.defaultHistoryFile());
  }

  private static final class RecordingHistoryTerminal implements ShellTerminal, SupportsHistory {
    private boolean saved;

    @Override
    public String readLine(String prompt) {
      return null;
    }

    @Override
    public PrintWriter writer() {
      return new PrintWriter(new StringWriter());
    }

    @Override
    public void setCompleter(Completer completer) {
    }

    @Override
    public void saveHistory() {
      saved = true;
    }

    @Override
    public void close() {
    }
  }

  private static final class PlainTerminal implements ShellTerminal {
    @Override
    public String readLine(String prompt) {
      return null;
    }

    @Override
    public PrintWriter writer() {
      return new PrintWriter(new StringWriter());
    }

    @Override
    public void setCompleter(Completer completer) {
    }

    @Override
    public void close() {
    }
  }
}
