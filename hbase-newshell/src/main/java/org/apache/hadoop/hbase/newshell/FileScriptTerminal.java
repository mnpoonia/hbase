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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.apache.hadoop.hbase.newshell.spi.Completer;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * A {@link ShellTerminal} that reads commands from a script file rather than an interactive
 * session, matching hbase-shell's positional {@code SCRIPTFILE} argument (see
 * {@code jar-bootstrap.rb}'s {@code script2run}). Output goes to stdout; completion is a no-op
 * since there is no user to complete for.
 */
@InterfaceAudience.Private
final class FileScriptTerminal implements ShellTerminal {
  private final BufferedReader reader;
  private final PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

  FileScriptTerminal(String path) throws IOException {
    this.reader = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8));
  }

  @Override
  public String readLine(String prompt) throws IOException {
    return reader.readLine();
  }

  @Override
  public PrintWriter writer() {
    return writer;
  }

  @Override
  public void setCompleter(Completer completer) {
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
