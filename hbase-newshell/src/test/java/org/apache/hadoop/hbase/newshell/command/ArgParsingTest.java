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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class ArgParsingTest {
  @Test
  public void requireMaxArgsAllowsUpToMax() throws Exception {
    ArgParsing.requireMaxArgs(ShellLineParser.parse("truncate 't1'"), "truncate", 1);
  }

  @Test
  public void requireMaxArgsRejectsSurplus() throws Exception {
    assertThrows(ShellCommandException.class,
      () -> ArgParsing.requireMaxArgs(ShellLineParser.parse("truncate 't1', 't2'"), "truncate", 1));
  }

  @Test
  public void requireArgReturnsStringOrFailsWithMessage() throws Exception {
    assertEquals("t1", ArgParsing.requireArg(ShellLineParser.parse("describe 't1'"), 0, "msg"));
    ShellCommandException e = assertThrows(ShellCommandException.class,
      () -> ArgParsing.requireArg(ShellLineParser.parse("describe"), 0, "describe needs a table"));
    assertEquals("describe needs a table", e.getMessage());
  }

  @Test
  public void requireArgsChecksMinimumCount() throws Exception {
    ArgParsing.requireArgs(ShellLineParser.parse("move 'a', 'b'"), 2, "msg");
    assertThrows(ShellCommandException.class,
      () -> ArgParsing.requireArgs(ShellLineParser.parse("move 'a'"), 2, "msg"));
  }

  @Test
  public void optionalArgFallsBackWhenAbsent() throws Exception {
    assertEquals(".*", ArgParsing.optionalArg(ShellLineParser.parse("list"), 0, ".*"));
    assertEquals("x", ArgParsing.optionalArg(ShellLineParser.parse("list 'x'"), 0, ".*"));
  }

  @Test
  public void stringListWrapsScalarsAndStringifiesElements() {
    assertEquals(Collections.singletonList("a"), ArgParsing.stringList("a"));
    assertEquals(Arrays.asList("1", "b"), ArgParsing.stringList(Arrays.<Object> asList(1, "b")));
  }

  @Test
  public void tableCfsReadsHashArgumentOrReturnsNull() throws Exception {
    assertNull(ArgParsing.tableCfs(ShellLineParser.parse("set_peer_tableCFs '1'")));
    assertThrows(ShellCommandException.class,
      () -> ArgParsing.tableCfs(ShellLineParser.parse("set_peer_tableCFs '1', 'notahash'")));
    assertTrue(
      ArgParsing.tableCfs(ShellLineParser.parse("set_peer_tableCFs '1', { 't1' => ['cf'] }"))
        .containsKey("t1"));
  }

  @Test
  public void parseBooleanIsStrict() throws Exception {
    org.junit.jupiter.api.Assertions.assertTrue(ArgParsing.parseBoolean("TRUE"));
    org.junit.jupiter.api.Assertions.assertFalse(ArgParsing.parseBoolean("false"));
    org.junit.jupiter.api.Assertions.assertTrue(ArgParsing.parseBoolean(Boolean.TRUE));
    org.junit.jupiter.api.Assertions.assertThrows(ShellCommandException.class,
      () -> ArgParsing.parseBoolean("ture"));
  }
}
