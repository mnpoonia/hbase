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

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/list_security_capabilities.rb}.
 */
@InterfaceAudience.Private
public final class ListSecurityCapabilitiesCommand implements ShellCommand {
  @Override
  public String name() {
    return "list_security_capabilities";
  }

  @Override
  public String help() {
    return "list_security_capabilities - list supported security capabilities";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    try {
      List<String> caps = context.admin().listSecurityCapabilities();
      return new TextResult(caps);
    } catch (UnsupportedOperationException e) {
      return TextResult.of("ERROR: Master does not support getSecurityCapabilities");
    } catch (IOException e) {
      if (e.getMessage() != null && e.getMessage().contains("UnsupportedOperationException")) {
        return TextResult.of("ERROR: Master does not support getSecurityCapabilities");
      }
      Throwable cause = e.getCause();
      if (
        cause instanceof UnsupportedOperationException
          || (cause != null && String.valueOf(cause).contains("UnsupportedOperationException"))
      ) {
        return TextResult.of("ERROR: Master does not support getSecurityCapabilities");
      }
      throw e;
    }
  }
}
