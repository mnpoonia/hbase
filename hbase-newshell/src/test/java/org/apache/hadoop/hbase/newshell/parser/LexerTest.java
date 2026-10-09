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

import java.util.List;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class LexerTest {

  @Test
  public void tokenizesIdentAndEof() throws ShellParseException {
    List<Token> tokens = new Lexer("status").tokenize();
    assertEquals(2, tokens.size());
    assertEquals(TokenType.IDENT, tokens.get(0).type());
    assertEquals("status", tokens.get(0).text());
    assertEquals(TokenType.EOF, tokens.get(1).type());
  }

  @Test
  public void tokenizesQuotedStringsWithEscapes() throws ShellParseException {
    List<Token> tokens = new Lexer("'it\\'s a test'").tokenize();
    assertEquals(TokenType.STRING, tokens.get(0).type());
    assertEquals("it's a test", tokens.get(0).text());
  }

  @Test
  public void trailingCommentIsIgnoredButQuotedHashIsKept() throws ShellParseException {
    List<Token> tokens = new Lexer("get 't#1', 'r' # fetch it").tokenize();
    assertEquals(5, tokens.size());
    assertEquals("t#1", tokens.get(1).text());
    assertEquals(TokenType.EOF, tokens.get(4).type());
  }

  @Test
  public void commentEndsAtNewlineSoLaterLinesAreKept() throws ShellParseException {
    List<Token> tokens = new Lexer("create 't', {NAME => 'f', # main\n  VERSIONS => 1}").tokenize();
    assertEquals("VERSIONS", tokens.get(tokens.size() - 5).text());
    assertEquals(TokenType.RBRACE, tokens.get(tokens.size() - 2).type());
  }

  private static String lex(String input) throws ShellParseException {
    return new Lexer(input).tokenize().get(0).text();
  }

  @Test
  public void singleQuotedKeepsOtherBackslashesLiterally() throws ShellParseException {
    assertEquals("C:\\temp", lex("'C:\\temp'"));
    assertEquals("a\\nb", lex("'a\\nb'"));
    assertEquals("a\\b", lex("'a\\\\b'"));
    assertEquals("ValueFilter(=,'regexstring:a\\.b')",
      lex("'ValueFilter(=,\\'regexstring:a\\.b\\')'"));
  }

  @Test
  public void doubleQuotedUnescapesRubyEscapes() throws ShellParseException {
    assertEquals("a\nb\tc\rd", lex("\"a\\nb\\tc\\rd\""));
    assertEquals("say \"hi\"", lex("\"say \\\"hi\\\"\""));
    assertEquals("a\\b", lex("\"a\\\\b\""));
    assertEquals("a.b\\.c", lex("\"a.b\\.c\""));
  }

  @Test
  public void hexEscapesArePreservedForTheTableLayer() throws ShellParseException {
    assertEquals("\\x00\\xFF", lex("\"\\x00\\xFF\""));
    assertEquals("\\x00\\xFF", lex("'\\x00\\xFF'"));
  }

  @Test
  public void tokenizesHashLiteralPunctuation() throws ShellParseException {
    List<Token> tokens = new Lexer("{NAME => 'f1'}").tokenize();
    assertEquals(TokenType.LBRACE, tokens.get(0).type());
    assertEquals(TokenType.IDENT, tokens.get(1).type());
    assertEquals(TokenType.HASHROCKET, tokens.get(2).type());
    assertEquals(TokenType.STRING, tokens.get(3).type());
    assertEquals(TokenType.RBRACE, tokens.get(4).type());
  }

  @Test
  public void tokenizesArrayLiteralPunctuation() throws ShellParseException {
    List<Token> tokens = new Lexer("['c1', 'c2']").tokenize();
    assertEquals(TokenType.LBRACKET, tokens.get(0).type());
    assertEquals(TokenType.STRING, tokens.get(1).type());
    assertEquals(TokenType.COMMA, tokens.get(2).type());
    assertEquals(TokenType.STRING, tokens.get(3).type());
    assertEquals(TokenType.RBRACKET, tokens.get(4).type());
  }

  @Test
  public void tokenizesNativeFlagSyntax() throws ShellParseException {
    List<Token> tokens = new Lexer("--name=f1").tokenize();
    assertEquals(TokenType.FLAG, tokens.get(0).type());
    assertEquals("name", tokens.get(0).text());
    assertEquals(TokenType.EQUALS, tokens.get(1).type());
    assertEquals(TokenType.IDENT, tokens.get(2).type());
  }

  @Test
  public void tokenizesNegativeAndDecimalNumbers() throws ShellParseException {
    List<Token> tokens = new Lexer("-3 2.5").tokenize();
    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals("-3", tokens.get(0).text());
    assertEquals(TokenType.NUMBER, tokens.get(1).type());
    assertEquals("2.5", tokens.get(1).text());
  }

  @Test
  public void tokenizesUnderscoreDigitSeparators() throws ShellParseException {
    List<Token> tokens = new Lexer("1_000_000").tokenize();
    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals("1_000_000", tokens.get(0).text());
    assertEquals(TokenType.EOF, tokens.get(1).type());
  }

  @Test
  public void tokenizesUnderscoreSeparatorsWithDecimal() throws ShellParseException {
    List<Token> tokens = new Lexer("1_000.5").tokenize();
    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals("1_000.5", tokens.get(0).text());
  }

  @Test
  public void tokenizesExponentNotation() throws ShellParseException {
    List<Token> tokens = new Lexer("1e5 1.5e-3 2E+10").tokenize();
    assertEquals(TokenType.NUMBER, tokens.get(0).type());
    assertEquals("1e5", tokens.get(0).text());
    assertEquals(TokenType.NUMBER, tokens.get(1).type());
    assertEquals("1.5e-3", tokens.get(1).text());
    assertEquals(TokenType.NUMBER, tokens.get(2).type());
    assertEquals("2E+10", tokens.get(2).text());
  }

  @Test
  public void tokenizesLeadingUnderscoreAsIdentifierNotNumber() throws ShellParseException {
    // '_' is a valid identifier-start character; a leading digit separator is ambiguous with a
    // bareword and is deliberately left as an IDENT rather than treated as a NUMBER.
    List<Token> tokens = new Lexer("_1000").tokenize();
    assertEquals(TokenType.IDENT, tokens.get(0).type());
    assertEquals("_1000", tokens.get(0).text());
  }

  @Test
  public void tokenizesTrailingAndDoubledUnderscoresLeniently() throws ShellParseException {
    // Deliberately permissive about underscore placement (unlike real Ruby, which rejects both
    // of these): a trailing or doubled separator is still folded into one NUMBER token rather
    // than erroring or splitting into separate tokens.
    List<Token> trailing = new Lexer("1000_").tokenize();
    assertEquals(TokenType.NUMBER, trailing.get(0).type());
    assertEquals("1000_", trailing.get(0).text());

    List<Token> doubled = new Lexer("1__000").tokenize();
    assertEquals(TokenType.NUMBER, doubled.get(0).type());
    assertEquals("1__000", doubled.get(0).text());
  }

  @Test
  public void throwsOnMalformedExponentWithNoDigits() {
    assertThrows(ShellParseException.class, () -> new Lexer("1e").tokenize());
  }

  @Test
  public void throwsOnMalformedExponentWithOnlySign() {
    assertThrows(ShellParseException.class, () -> new Lexer("1e+").tokenize());
  }

  @Test
  public void throwsOnUnterminatedString() {
    assertThrows(ShellParseException.class, () -> new Lexer("'unterminated").tokenize());
  }

  @Test
  public void throwsOnUnexpectedCharacter() {
    assertThrows(ShellParseException.class, () -> new Lexer("get 't1' @").tokenize());
  }

  @Test
  public void throwsOnDanglingFlagMarker() {
    assertThrows(ShellParseException.class, () -> new Lexer("-- ").tokenize());
  }
}
