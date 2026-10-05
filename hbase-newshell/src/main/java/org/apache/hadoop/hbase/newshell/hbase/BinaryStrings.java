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
package org.apache.hadoop.hbase.newshell.hbase;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Converts user-typed strings to bytes. {@code \xNN} sequences become the raw byte {@code NN} (the
 * inverse of {@code Bytes.toStringBinary}, so printed binary rows and values can be typed back in);
 * everything else is UTF-8 encoded. {@code Bytes.toBytesBinary} is not used because it truncates
 * non-ASCII characters to a single byte.
 */
final class BinaryStrings {

  private BinaryStrings() {
  }

  static byte[] toBytes(String s) {
    if (!s.contains("\\x")) {
      return s.getBytes(StandardCharsets.UTF_8);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
    int i = 0;
    while (i < s.length()) {
      if (isHexEscape(s, i)) {
        out.write(Integer.parseInt(s.substring(i + 2, i + 4), 16));
        i += 4;
      } else {
        int end = i + 1;
        while (end < s.length() && !isHexEscape(s, end)) {
          end++;
        }
        byte[] chunk = s.substring(i, end).getBytes(StandardCharsets.UTF_8);
        out.write(chunk, 0, chunk.length);
        i = end;
      }
    }
    return out.toByteArray();
  }

  private static boolean isHexEscape(String s, int i) {
    return i + 4 <= s.length() && s.charAt(i) == '\\' && s.charAt(i + 1) == 'x'
      && Character.digit(s.charAt(i + 2), 16) >= 0 && Character.digit(s.charAt(i + 3), 16) >= 0;
  }
}
