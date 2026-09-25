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
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ArgParsing;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Ported from hbase-shell's {@code shell/commands/merge_region.rb}: two or more region names,
 * optional trailing force boolean.
 */
@InterfaceAudience.Private
public final class MergeRegionCommand implements ShellCommand {
  @Override
  public String name() {
    return "merge_region";
  }

  @Override
  public String help() {
    return "merge_region 'r1', 'r2', merge_region ['r1','r2'], true - merge regions";
  }

  @Override
  public CommandResult execute(ParsedCommand command, ExecutionContext context)
    throws ShellCommandException, IOException {
    List<Object> positionals = command.positionalArgs();
    if (positionals.isEmpty()) {
      throw new ShellCommandException("Must pass at least 2 regions to merge");
    }
    List<String> regions = new ArrayList<>();
    boolean force = false;
    for (int i = 0; i < positionals.size(); i++) {
      Object arg = positionals.get(i);
      boolean isLast = i == positionals.size() - 1;
      if (isLast && isBoolean(arg) && !regions.isEmpty()) {
        force = ArgParsing.parseBoolean(arg);
        break;
      }
      if (arg instanceof List) {
        for (Object element : (List<?>) arg) {
          regions.add(String.valueOf(element));
        }
      } else {
        regions.add(String.valueOf(arg));
      }
    }
    // When regions came as a single array plus trailing boolean already handled above; when the
    // last element of a flat list is true/false string and we already have >=2 regions, strip it.
    if (regions.size() > 2) {
      String last = regions.get(regions.size() - 1);
      if ("true".equalsIgnoreCase(last) || "false".equalsIgnoreCase(last)) {
        // Only strip when the original last positional was a bare boolean-like token, not when it
        // was part of an array argument.
        Object lastPos = positionals.get(positionals.size() - 1);
        if (!(lastPos instanceof List)) {
          force = ArgParsing.parseBoolean(last);
          regions = new ArrayList<>(regions.subList(0, regions.size() - 1));
        }
      }
    }
    if (regions.size() < 2) {
      throw new ShellCommandException("Must pass at least 2 regions to merge");
    }
    context.admin().mergeRegion(regions, force);
    return TextResult.of();
  }

  private static boolean isBoolean(Object value) {
    if (value instanceof Boolean) {
      return true;
    }
    if (value instanceof String) {
      String s = (String) value;
      return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }
    return false;
  }
}
