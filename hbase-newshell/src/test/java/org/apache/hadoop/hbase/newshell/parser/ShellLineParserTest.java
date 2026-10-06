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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class ShellLineParserTest {

  @Test
  public void parsesBareCommandWithNoArgs() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("status");
    assertEquals("status", parsed.commandName());
    assertTrue(parsed.positionalArgs().isEmpty());
    assertTrue(parsed.options().isEmpty());
  }

  @Test
  public void parsesCommandNameCaseInsensitively() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("STATUS");
    assertEquals("status", parsed.commandName());
  }

  @Test
  public void parsesPositionalStringArgument() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("disable 't1'");
    assertEquals("disable", parsed.commandName());
    assertEquals(Arrays.asList("t1"), parsed.positionalArgs());
  }

  @Test
  public void parsesLegacyHashLiteralWithScalarValue() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1', {NAME => 'f1', VERSIONS => 3}");
    assertEquals("create", parsed.commandName());
    assertEquals(Arrays.asList("t1"), parsed.positionalArgs());
    assertEquals("f1", parsed.options().get("NAME"));
    assertEquals(3L, parsed.options().get("VERSIONS"));
  }

  @Test
  public void parsesMultipleHashLiteralsAsSeparateFamilySpecs() throws ShellParseException {
    ParsedCommand parsed =
      ShellLineParser.parse("create 't1', {NAME => 'f1'}, {NAME => 'f2', VERSIONS => 5}");
    assertEquals("create", parsed.commandName());
    assertEquals(Arrays.asList("t1"), parsed.positionalArgs());
    assertEquals(2, parsed.hashLiterals().size());
    assertEquals("f1", parsed.hashLiterals().get(0).get("NAME"));
    assertEquals("f2", parsed.hashLiterals().get(1).get("NAME"));
    assertEquals(5L, parsed.hashLiterals().get(1).get("VERSIONS"));
    // options() still merges everything, mirroring pre-multi-family behavior for callers that
    // only care about a single flattened view (e.g. get/disable).
    assertEquals("f2", parsed.options().get("NAME"));
  }

  @Test
  public void parsesBareTrailingHashEntriesAsOwnHashLiteral() throws ShellParseException {
    ParsedCommand parsed =
      ShellLineParser.parse("create 't1', {NAME => 'f1'}, SPLITS => ['1000', '2000']");
    assertEquals("create", parsed.commandName());
    assertEquals(Arrays.asList("t1"), parsed.positionalArgs());
    assertEquals(2, parsed.hashLiterals().size());
    assertEquals("f1", parsed.hashLiterals().get(0).get("NAME"));
    assertEquals(Arrays.asList("1000", "2000"), parsed.hashLiterals().get(1).get("SPLITS"));
  }

  @Test
  public void parsesMultipleBareTrailingHashEntriesIntoOneHashLiteral() throws ShellParseException {
    ParsedCommand parsed =
      ShellLineParser.parse("create 't1', 'f1', SPLITS => ['1000'], REGION_REPLICATION => 3");
    assertEquals(1, parsed.hashLiterals().size());
    assertEquals(Arrays.asList("1000"), parsed.hashLiterals().get(0).get("SPLITS"));
    assertEquals(3L, parsed.hashLiterals().get(0).get("REGION_REPLICATION"));
  }

  @Test
  public void parsesNativeFlagSyntaxIntoSameOptionsMap() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1' --NAME=f1 --VERSIONS=3");
    assertEquals("f1", parsed.options().get("NAME"));
    assertEquals(3L, parsed.options().get("VERSIONS"));
  }

  @Test
  public void upperCasesFlagKeysSoLowercaseFlagsWork() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1' --name=f1 --versions=3 --yes");
    assertEquals("f1", parsed.options().get("NAME"));
    assertEquals(3L, parsed.options().get("VERSIONS"));
    assertEquals(Boolean.TRUE, parsed.options().get("YES"));
  }

  @Test
  public void parsesHashLiteralWithArrayValue() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("get 't1', 'r1', {COLUMN => ['c1', 'c2']}");
    assertEquals("get", parsed.commandName());
    assertEquals(Arrays.asList("t1", "r1"), parsed.positionalArgs());
    assertEquals(Arrays.asList("c1", "c2"), parsed.options().get("COLUMN"));
  }

  @Test
  public void parsesHashLiteralWithSingleColumnValue() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("get 't1', 'r1', {COLUMN => 'cf:col'}");
    assertEquals("cf:col", parsed.options().get("COLUMN"));
  }

  @Test
  public void parsesHashLiteralValueAsNestedMap() throws ShellParseException {
    ParsedCommand parsed =
      ShellLineParser.parse("add_peer '1', TABLE_CFS => {'ns:tab' => ['cf1', 'cf2']}");
    @SuppressWarnings("unchecked")
    Map<String, Object> tableCfs = (Map<String, Object>) parsed.options().get("TABLE_CFS");
    assertEquals(Arrays.asList("cf1", "cf2"), tableCfs.get("ns:tab"));
  }

  @Test
  public void nestedHashLiteralKeysPreserveOriginalCase() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("add_peer '1', TABLE_CFS => {'ns:Tab' => []}");
    @SuppressWarnings("unchecked")
    Map<String, Object> tableCfs = (Map<String, Object>) parsed.options().get("TABLE_CFS");
    assertTrue(tableCfs.containsKey("ns:Tab"));
  }

  @Test
  public void parsesDeeplyNestedHashLiteral() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("foo, A => {'x' => {'y' => 'z'}}");
    @SuppressWarnings("unchecked")
    Map<String, Object> a = (Map<String, Object>) parsed.options().get("A");
    @SuppressWarnings("unchecked")
    Map<String, Object> x = (Map<String, Object>) a.get("x");
    assertEquals("z", x.get("y"));
  }

  @Test
  public void throwsOnMalformedHashLiteral() {
    assertThrows(ShellParseException.class,
      () -> ShellLineParser.parse("create 't1', {NAME 'f1'}"));
  }

  @Test
  public void throwsOnUnclosedHashLiteral() {
    assertThrows(ShellParseException.class,
      () -> ShellLineParser.parse("create 't1', {NAME => 'f1'"));
  }

  @Test
  public void throwsOnUnclosedQuote() {
    assertThrows(ShellParseException.class, () -> ShellLineParser.parse("disable 't1"));
  }

  @Test
  public void throwsWhenLineIsNotACommandName() {
    assertThrows(ShellParseException.class, () -> ShellLineParser.parse("'t1'"));
  }

  @Test
  public void throwsParseExceptionForOutOfRangeNumericLiteral() {
    assertThrows(ShellParseException.class,
      () -> ShellLineParser.parse("describe 99999999999999999999"));
  }

  @Test
  public void parsesUnderscoreDigitSeparatorAsLong() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1', {VERSIONS => 1_000_000}");
    assertEquals(1000000L, parsed.options().get("VERSIONS"));
  }

  @Test
  public void parsesUnderscoreDigitSeparatorWithDecimalAsDouble() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1', {VERSIONS => 1_000.5}");
    assertEquals(1000.5, parsed.options().get("VERSIONS"));
  }

  @Test
  public void parsesExponentNotationAsDouble() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1', {VERSIONS => 1e5}");
    assertEquals(100000.0, parsed.options().get("VERSIONS"));
  }

  @Test
  public void parsesNegativeExponentNotationAsDouble() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1', {VERSIONS => 1.5e-3}");
    assertEquals(0.0015, parsed.options().get("VERSIONS"));
  }

  @Test
  public void parsesUppercaseExponentWithExplicitPlusSign() throws ShellParseException {
    ParsedCommand parsed = ShellLineParser.parse("create 't1', {VERSIONS => 2E+10}");
    assertEquals(2.0E10, parsed.options().get("VERSIONS"));
  }

  @Test
  public void parsesTrailingAndDoubledUnderscoresLenientlyAsLong() throws ShellParseException {
    // Unlike real Ruby (which raises SyntaxError on these), this parser is deliberately lenient
    // about underscore placement - both still resolve to the obvious numeric value.
    ParsedCommand trailing = ShellLineParser.parse("create 't1', {VERSIONS => 1000_}");
    assertEquals(1000L, trailing.options().get("VERSIONS"));

    ParsedCommand doubled = ShellLineParser.parse("create 't1', {VERSIONS => 1__000}");
    assertEquals(1000L, doubled.options().get("VERSIONS"));
  }

  @Test
  public void throwsOnMalformedExponentInNumericLiteral() {
    assertThrows(ShellParseException.class, () -> ShellLineParser.parse("describe 1e"));
    assertThrows(ShellParseException.class, () -> ShellLineParser.parse("describe 1e+"));
  }

  @Test
  public void yesFlagBeforePositionalDoesNotSwallowIt() throws Exception {
    ParsedCommand parsed = ShellLineParser.parse("drop_all --yes 't.*'");
    assertEquals(java.util.Arrays.asList("t.*"), parsed.positionalArgs());
    assertEquals(Boolean.TRUE, parsed.options().get("YES"));
  }
}
