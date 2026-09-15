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

import java.io.IOException;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * One newshell command. Implementations are discovered via {@link CommandRegistry} through
 * {@link java.util.ServiceLoader} - adding a new command means adding a new implementation class
 * plus one {@code META-INF/services} line, with no change to the registry, the execution engine,
 * or any other command.
 */
@InterfaceAudience.Private
public interface ShellCommand {
  /** The command name as typed by the user, e.g. {@code "status"}. Matched case-insensitively. */
  String name();

  /** A one-line usage summary shown by a future {@code help} command. */
  String help();

  /**
   * Executes this command against the parsed line. Implementations must only throw
   * {@link ShellCommandException} (or let a checked {@link java.io.IOException} propagate) for
   * command-level failures - never an unchecked exception - so the execution engine can always
   * report an error and keep the REPL running.
   */
  CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException;
}
