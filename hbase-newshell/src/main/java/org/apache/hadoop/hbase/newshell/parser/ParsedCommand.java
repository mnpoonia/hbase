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
package org.apache.hadoop.hbase.newshell.parser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * The result of parsing one typed line: a command name, its positional arguments, and its options -
 * whether those options came from a legacy Ruby-style trailing hash literal ({@code {NAME =>
 * 'f1'}}) or the native bareword/flag syntax ({@code --name f1}), both populate this same
 * {@code options} map, so {@code ShellCommand} implementations never know which syntax was typed.
 * <p>
 * {@code hashLiterals} additionally preserves each trailing hash literal as its own map, in the
 * order written - needed by commands like {@code create} where multiple hash literals each describe
 * a separate column family (e.g. {@code {NAME => 'f1'}, {NAME => 'f2'}}) and would otherwise
 * collide if flattened into the single {@code options} map.
 */
@InterfaceAudience.Private
public final class ParsedCommand {
  private final String commandName;
  private final List<Object> positionalArgs;
  private final Map<String, Object> options;
  private final List<Map<String, Object>> hashLiterals;

  public ParsedCommand(String commandName, List<Object> positionalArgs, Map<String, Object> options,
    List<Map<String, Object>> hashLiterals) {
    this.commandName = commandName;
    this.positionalArgs = positionalArgs;
    this.options = options;
    this.hashLiterals = hashLiterals;
  }

  public String commandName() {
    return commandName;
  }

  public List<Object> positionalArgs() {
    return positionalArgs;
  }

  public Map<String, Object> options() {
    return options;
  }

  public List<Map<String, Object>> hashLiterals() {
    return hashLiterals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParsedCommand)) {
      return false;
    }
    ParsedCommand other = (ParsedCommand) o;
    return Objects.equals(commandName, other.commandName)
      && Objects.equals(positionalArgs, other.positionalArgs)
      && Objects.equals(options, other.options) && Objects.equals(hashLiterals, other.hashLiterals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandName, positionalArgs, options, hashLiterals);
  }

  @Override
  public String toString() {
    return "ParsedCommand[commandName=" + commandName + ", positionalArgs=" + positionalArgs
      + ", options=" + options + ", hashLiterals=" + hashLiterals + "]";
  }
}
