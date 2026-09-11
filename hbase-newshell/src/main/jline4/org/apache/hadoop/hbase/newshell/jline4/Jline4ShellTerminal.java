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
package org.apache.hadoop.hbase.newshell.jline4;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.SupportsHistory;
import org.apache.hadoop.hbase.newshell.spi.TerminalConfig;
import org.apache.yetus.audience.InterfaceAudience;
import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * {@link ShellTerminal} backed by JLine 4.x's {@code Terminal}/{@code LineReader}. Only this
 * class (and {@link Jline4TerminalProvider}) may reference {@code org.jline} types - newshell's
 * core never does.
 */
@InterfaceAudience.Private
final class Jline4ShellTerminal implements ShellTerminal, SupportsHistory {
  private final Terminal terminal;
  private final LineReader reader;
  private final AtomicReference<org.apache.hadoop.hbase.newshell.spi.Completer> delegate =
    new AtomicReference<>((buffer, cursor) -> List.of());

  Jline4ShellTerminal(TerminalConfig config) throws IOException {
    this.terminal = TerminalBuilder.builder().name(config.getAppName()).system(true).build();
    LineReaderBuilder builder = LineReaderBuilder.builder().terminal(terminal)
      .completer((lineReader, line, candidates) -> complete(line, candidates));
    Path historyFile = config.getHistoryFile();
    this.reader = builder.build();
    if (historyFile != null) {
      reader.setVariable(LineReader.HISTORY_FILE, historyFile);
    }
  }

  private void complete(ParsedLine line, List<Candidate> candidates) {
    for (String value : delegate.get().complete(line.line(), line.cursor())) {
      candidates.add(new Candidate(value));
    }
  }

  @Override
  public String readLine(String prompt) throws IOException {
    try {
      return reader.readLine(prompt);
    } catch (EndOfFileException e) {
      return null;
    } catch (UserInterruptException e) {
      return "";
    }
  }

  @Override
  public PrintWriter writer() {
    return terminal.writer();
  }

  @Override
  public void setCompleter(org.apache.hadoop.hbase.newshell.spi.Completer completer) {
    delegate.set(completer);
  }

  @Override
  public void saveHistory() throws IOException {
    reader.getHistory().save();
  }

  @Override
  public void close() throws IOException {
    terminal.close();
  }
}
