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
import java.util.List;

/**
 * Tokenizes one typed shell line. Deliberately narrow: only the punctuation/literal shapes the
 * pilot commands' grammar actually needs (quoted strings, numbers, barewords, one level of
 * {@code {key => value}} hash literals, {@code [a, b]} array literals, and {@code --flag}/
 * {@code =} native syntax) - not a general-purpose tokenizer for arbitrary Ruby source.
 */
final class Lexer {
  private final String input;
  private int pos;

  Lexer(String input) {
    this.input = input;
    this.pos = 0;
  }

  List<Token> tokenize() throws ShellParseException {
    List<Token> tokens = new ArrayList<>();
    while (true) {
      skipWhitespace();
      if (atEnd()) {
        tokens.add(new Token(TokenType.EOF, "", pos));
        break;
      }
      int start = pos;
      char c = input.charAt(pos);
      if (c == '\'' || c == '"') {
        tokens.add(readString(c));
      } else if (c == ',') {
        pos++;
        tokens.add(new Token(TokenType.COMMA, ",", start));
      } else if (c == '{') {
        pos++;
        tokens.add(new Token(TokenType.LBRACE, "{", start));
      } else if (c == '}') {
        pos++;
        tokens.add(new Token(TokenType.RBRACE, "}", start));
      } else if (c == '[') {
        pos++;
        tokens.add(new Token(TokenType.LBRACKET, "[", start));
      } else if (c == ']') {
        pos++;
        tokens.add(new Token(TokenType.RBRACKET, "]", start));
      } else if (c == '=' && peekAt(pos + 1) == '>') {
        pos += 2;
        tokens.add(new Token(TokenType.HASHROCKET, "=>", start));
      } else if (c == '=') {
        pos++;
        tokens.add(new Token(TokenType.EQUALS, "=", start));
      } else if (c == '-' && peekAt(pos + 1) == '-') {
        pos += 2;
        tokens.add(readFlag(start));
      } else if (Character.isDigit(c) || (c == '-' && Character.isDigit(peekAt(pos + 1)))) {
        tokens.add(readNumber(start));
      } else if (isIdentStart(c)) {
        tokens.add(readIdent(start));
      } else if (c == '#') {
        // Trailing comment: the rest of the line is ignored (quoted '#' is consumed by readString).
        pos = input.length();
      } else {
        throw new ShellParseException("Unexpected character '" + c + "' at position " + pos);
      }
    }
    return tokens;
  }

  private Token readFlag(int start) throws ShellParseException {
    int nameStart = pos;
    while (!atEnd() && isIdentPart(input.charAt(pos))) {
      pos++;
    }
    if (pos == nameStart) {
      throw new ShellParseException("Expected a flag name after '--' at position " + start);
    }
    return new Token(TokenType.FLAG, input.substring(nameStart, pos), start);
  }

  private Token readNumber(int start) throws ShellParseException {
    if (input.charAt(pos) == '-') {
      pos++;
    }
    readDigitsWithUnderscores();
    if (
      !atEnd() && input.charAt(pos) == '.' && pos + 1 < input.length()
        && Character.isDigit(input.charAt(pos + 1))
    ) {
      pos++;
      readDigitsWithUnderscores();
    }
    if (!atEnd() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
      int exponentStart = pos;
      pos++;
      if (!atEnd() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
        pos++;
      }
      if (atEnd() || !Character.isDigit(input.charAt(pos))) {
        throw new ShellParseException(
          "Malformed exponent in numeric literal at position " + exponentStart);
      }
      readDigitsWithUnderscores();
    }
    return new Token(TokenType.NUMBER, input.substring(start, pos), start);
  }

  /**
   * Consumes a run of digits that may contain {@code _} digit separators (Ruby numeric literal
   * syntax, e.g. {@code 1_000_000}). Deliberately permissive about placement - leading/trailing/
   * doubled underscores are tolerated rather than rejected, since this is a narrow literal
   * tokenizer rather than a full Ruby-fidelity lexer; {@link ShellLineParser#parseNumber} strips
   * all underscores before handing the text to {@code Long}/{@code Double}.
   */
  private void readDigitsWithUnderscores() {
    while (!atEnd() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
      pos++;
    }
  }

  private Token readIdent(int start) {
    while (!atEnd() && isIdentPart(input.charAt(pos))) {
      pos++;
    }
    return new Token(TokenType.IDENT, input.substring(start, pos), start);
  }

  /**
   * Reads a quoted string following Ruby rules. Single-quoted strings only unescape {@code \\} and
   * {@code \'}; any other backslash is kept literally, so {@code 'C:\temp'} stays as typed.
   * Double-quoted strings also unescape {@code \n \t \r \0 \e \a \b \s} and {@code \"}. A
   * {@code \xNN} sequence is deliberately left intact in both forms so the table layer can decode
   * it to a raw byte with {@code BinaryStrings.toBytes}; that is the inverse of
   * {@code Bytes.toStringBinary}, which is how rows and values are printed.
   */
  private Token readString(char quote) throws ShellParseException {
    int start = pos;
    pos++; // consume opening quote
    StringBuilder value = new StringBuilder();
    while (true) {
      if (atEnd()) {
        throw new ShellParseException("Unterminated string starting at position " + start);
      }
      char c = input.charAt(pos);
      if (c == '\\' && pos + 1 < input.length()) {
        char next = input.charAt(pos + 1);
        if (next == '\\' || next == quote) {
          value.append(next);
        } else if (quote == '"' && escapeFor(next) >= 0) {
          value.append((char) escapeFor(next));
        } else {
          value.append(c).append(next);
        }
        pos += 2;
      } else if (c == quote) {
        pos++;
        break;
      } else {
        value.append(c);
        pos++;
      }
    }
    return new Token(TokenType.STRING, value.toString(), start);
  }

  /** Returns the character a double-quoted {@code \\<c>} escape denotes, or -1 if not an escape. */
  private static int escapeFor(char c) {
    switch (c) {
      case 'n':
        return '\n';
      case 't':
        return '\t';
      case 'r':
        return '\r';
      case '0':
        return '\0';
      case 'e':
        return 0x1B;
      case 'a':
        return 0x07;
      case 'b':
        return '\b';
      case 's':
        return ' ';
      default:
        return -1;
    }
  }

  private void skipWhitespace() {
    while (!atEnd() && Character.isWhitespace(input.charAt(pos))) {
      pos++;
    }
  }

  private boolean atEnd() {
    return pos >= input.length();
  }

  private char peekAt(int index) {
    return index < input.length() ? input.charAt(index) : '\0';
  }

  private static boolean isIdentStart(char c) {
    return Character.isLetter(c) || c == '_';
  }

  private static boolean isIdentPart(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '-';
  }
}
