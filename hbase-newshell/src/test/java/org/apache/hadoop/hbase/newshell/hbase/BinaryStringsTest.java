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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.charset.StandardCharsets;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class BinaryStringsTest {

  @Test
  public void plainTextIsUtf8() {
    assertArrayEquals("héllo".getBytes(StandardCharsets.UTF_8), BinaryStrings.toBytes("héllo"));
  }

  @Test
  public void hexEscapesBecomeRawBytes() {
    assertArrayEquals(new byte[] { 0x00, (byte) 0xFF }, BinaryStrings.toBytes("\\x00\\xFF"));
    assertArrayEquals(new byte[] { 'a', (byte) 0x80, 'b' }, BinaryStrings.toBytes("a\\x80b"));
  }

  @Test
  public void mixesHexEscapesWithNonAscii() {
    byte[] expected = new byte[] { (byte) 0xC3, (byte) 0xA9, 0x01 };
    assertArrayEquals(expected, BinaryStrings.toBytes("é\\x01"));
  }

  @Test
  public void malformedEscapesStayLiteral() {
    assertArrayEquals("\\xZZ".getBytes(StandardCharsets.UTF_8), BinaryStrings.toBytes("\\xZZ"));
    assertArrayEquals("\\x4".getBytes(StandardCharsets.UTF_8), BinaryStrings.toBytes("\\x4"));
  }

  @Test
  public void roundTripsToStringBinary() {
    byte[] raw = new byte[] { 0x00, 0x01, (byte) 0xFE, 'x', '\\', (byte) 0x80 };
    assertArrayEquals(raw, BinaryStrings.toBytes(Bytes.toStringBinary(raw)));
  }
}
