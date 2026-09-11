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
package org.apache.hadoop.hbase.newshell.jline3;

import java.io.IOException;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.TerminalConfig;
import org.apache.hadoop.hbase.newshell.spi.TerminalProvider;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link TerminalProvider} backed by JLine 3.x, intended for legacy JDK8 HBase branches. On
 * this module's own build, whichever backend module is compiled in via the JDK-activated
 * {@code jline3-backend}/{@code jline4-backend} Maven profiles is the only one that registers
 * itself under {@code META-INF/services}, so at most one provider is ever discoverable at once.
 */
@InterfaceAudience.Private
public final class Jline3TerminalProvider implements TerminalProvider {
  static final String NAME = "jline3";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public int priority() {
    return 100;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public ShellTerminal open(TerminalConfig config) throws IOException {
    return new Jline3ShellTerminal(config);
  }
}
