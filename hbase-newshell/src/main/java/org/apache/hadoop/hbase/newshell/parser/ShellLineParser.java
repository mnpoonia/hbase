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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * A small hand-written recursive-descent parser for the fixed grammar newshell's pilot commands
 * need:
 *
 * <pre>
 *   command    := IDENT arg*
 *   arg        := hashLiteral | bareHashEntries | flag | COMMA | value
 *   hashLiteral := '{' (hashEntry (',' hashEntry)*)? '}'
 *   bareHashEntries := hashEntry (',' hashEntry)*
 *   hashEntry  := (IDENT | STRING) '=&gt;' value
 *   flag       := FLAG ('=' value)?
 *   value      := STRING | NUMBER | IDENT | arrayLiteral
 *   arrayLiteral := '[' (value (',' value)*)? ']'
 * </pre>
 *
 * {@code bareHashEntries} is Ruby's implicit-trailing-hash convention - a run of bare
 * {@code KEY => value} pairs with no enclosing {@code {}} (e.g. {@code SPLITS => ['1', '2']}) is
 * still one hash literal, exactly as if it were braced; this lets commands accept the same
 * trailing-hash argument hbase-shell does without requiring the caller to add braces.
 *
 * <p>This intentionally does not attempt to parse arbitrary Ruby expressions - only the literal
 * shapes hbase-shell's legacy syntax and newshell's native syntax actually use for the pilot
 * commands. Both syntaxes populate the same {@link ParsedCommand#options()} map.
 */
@InterfaceAudience.Private
public final class ShellLineParser {
  private final List<Token> tokens;
  private int pos;

  private ShellLineParser(List<Token> tokens) {
    this.tokens = tokens;
    this.pos = 0;
  }

  public static ParsedCommand parse(String line) throws ShellParseException {
    List<Token> tokens = new Lexer(line).tokenize();
    return new ShellLineParser(tokens).parseCommand();
  }

  private ParsedCommand parseCommand() throws ShellParseException {
    Token first = peek();
    if (first.type() != TokenType.IDENT) {
      throw new ShellParseException("Expected a command name but found " + first);
    }
    advance();
    String commandName = first.text().toLowerCase(Locale.ROOT);

    List<Object> positionals = new ArrayList<>();
    Map<String, Object> options = new LinkedHashMap<>();
    List<Map<String, Object>> hashLiterals = new ArrayList<>();
    while (!check(TokenType.EOF)) {
      switch (peek().type()) {
        case LBRACE:
          Map<String, Object> hashLiteral = new LinkedHashMap<>();
          parseHashLiteral(hashLiteral);
          options.putAll(hashLiteral);
          hashLiterals.add(hashLiteral);
          break;
        case FLAG:
          parseFlag(options);
          break;
        case COMMA:
          advance();
          break;
        default:
          if (isBareHashEntryStartAt(pos)) {
            Map<String, Object> bareHashLiteral = new LinkedHashMap<>();
            parseHashEntry(bareHashLiteral);
            while (check(TokenType.COMMA) && isBareHashEntryStartAt(pos + 1)) {
              advance();
              parseHashEntry(bareHashLiteral);
            }
            options.putAll(bareHashLiteral);
            hashLiterals.add(bareHashLiteral);
          } else {
            positionals.add(parseValue());
          }
      }
    }
    return new ParsedCommand(commandName, positionals, options, hashLiterals);
  }

  /**
   * True if the token at {@code index} starts a bare (unbraced) {@code KEY => value} hash entry -
   * i.e. an {@code IDENT}/{@code STRING} immediately followed by {@code =>}.
   */
  private boolean isBareHashEntryStartAt(int index) {
    if (index < 0 || index >= tokens.size() - 1) {
      return false;
    }
    TokenType type = tokens.get(index).type();
    return (type == TokenType.IDENT || type == TokenType.STRING)
      && tokens.get(index + 1).type() == TokenType.HASHROCKET;
  }

  private void parseFlag(Map<String, Object> options) throws ShellParseException {
    Token flag = advance();
    String key = flag.text().toUpperCase(Locale.ROOT);
    Object value;
    if (check(TokenType.EQUALS)) {
      advance();
      value = parseValue();
    } else if (check(TokenType.EOF) || check(TokenType.COMMA) || check(TokenType.FLAG)) {
      value = Boolean.TRUE;
    } else {
      value = parseValue();
    }
    options.put(key, value);
  }

  private void parseHashLiteral(Map<String, Object> options) throws ShellParseException {
    expect(TokenType.LBRACE);
    if (!check(TokenType.RBRACE)) {
      parseHashEntry(options);
      while (check(TokenType.COMMA)) {
        advance();
        parseHashEntry(options);
      }
    }
    expect(TokenType.RBRACE);
  }

  private void parseHashEntry(Map<String, Object> options) throws ShellParseException {
    String key = readHashKeyAndArrow();
    Object value = parseValue();
    options.put(key.toUpperCase(Locale.ROOT), value);
  }

  /**
   * Parses a hash literal appearing as a <em>value</em> (e.g. {@code TABLE_CFS => {'ns:tab' =>
   * [...]}}). Unlike {@link #parseHashEntry}, keys here are arbitrary data (table names,
   * namespaces, etc.), not attribute names, so they are kept as-is instead of upper-cased.
   */
  private Map<String, Object> parseNestedHashLiteral() throws ShellParseException {
    expect(TokenType.LBRACE);
    Map<String, Object> result = new LinkedHashMap<>();
    if (!check(TokenType.RBRACE)) {
      parseNestedHashEntry(result);
      while (check(TokenType.COMMA)) {
        advance();
        parseNestedHashEntry(result);
      }
    }
    expect(TokenType.RBRACE);
    return result;
  }

  private void parseNestedHashEntry(Map<String, Object> options) throws ShellParseException {
    String key = readHashKeyAndArrow();
    Object value = parseValue();
    options.put(key, value);
  }

  private String readHashKeyAndArrow() throws ShellParseException {
    Token keyToken = advance();
    if (keyToken.type() != TokenType.IDENT && keyToken.type() != TokenType.STRING) {
      throw new ShellParseException("Expected a hash key but found " + keyToken);
    }
    expect(TokenType.HASHROCKET);
    return keyToken.text();
  }

  private Object parseValue() throws ShellParseException {
    Token t = peek();
    switch (t.type()) {
      case STRING:
        advance();
        return t.text();
      case NUMBER:
        advance();
        return parseNumber(t.text());
      case IDENT:
        advance();
        return t.text();
      case LBRACKET:
        return parseArrayLiteral();
      case LBRACE:
        return parseNestedHashLiteral();
      default:
        throw new ShellParseException("Unexpected token " + t);
    }
  }

  private List<Object> parseArrayLiteral() throws ShellParseException {
    expect(TokenType.LBRACKET);
    List<Object> values = new ArrayList<>();
    if (!check(TokenType.RBRACKET)) {
      values.add(parseValue());
      while (check(TokenType.COMMA)) {
        advance();
        values.add(parseValue());
      }
    }
    expect(TokenType.RBRACKET);
    return values;
  }

  private static Object parseNumber(String text) {
    if (text.indexOf('.') >= 0) {
      return Double.valueOf(text);
    }
    return Long.valueOf(text);
  }

  private Token peek() {
    return tokens.get(pos);
  }

  private Token advance() {
    return tokens.get(pos++);
  }

  private boolean check(TokenType type) {
    return peek().type() == type;
  }

  private void expect(TokenType type) throws ShellParseException {
    if (!check(type)) {
      throw new ShellParseException("Expected " + type + " but found " + peek());
    }
    advance();
  }
}
