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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Resolves command names to {@link ShellCommand} implementations, discovered via
 * {@link ServiceLoader} - mirroring
 * {@code org.apache.hadoop.hbase.newshell.TerminalProviderRegistry}'s extensibility pattern.
 * Adding command #5 onward means adding a class and one {@code META-INF/services} line; this
 * registry never needs to change.
 */
@InterfaceAudience.Private
public final class CommandRegistry {
  private final Map<String, ShellCommand> commandsByName;

  public CommandRegistry() {
    this(ServiceLoader.load(ShellCommand.class).stream().map(ServiceLoader.Provider::get)
      .collect(Collectors.toList()));
  }

  public CommandRegistry(List<ShellCommand> commands) {
    Map<String, ShellCommand> byName = new LinkedHashMap<>();
    for (ShellCommand command : commands) {
      String name = command.name().toLowerCase(Locale.ROOT);
      ShellCommand existing = byName.putIfAbsent(name, command);
      if (existing != null) {
        throw new IllegalStateException("Duplicate ShellCommand registered for name '" + name + "': "
          + existing.getClass().getName() + " and " + command.getClass().getName());
      }
    }
    this.commandsByName = byName;
  }

  public Optional<ShellCommand> lookup(String name) {
    return Optional.ofNullable(commandsByName.get(name.toLowerCase(Locale.ROOT)));
  }
}
