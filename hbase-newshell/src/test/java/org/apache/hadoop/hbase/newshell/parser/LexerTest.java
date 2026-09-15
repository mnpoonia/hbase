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
