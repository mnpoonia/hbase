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
package org.apache.hadoop.hbase.newshell.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class JsonFormatterTest {
  @Test
  public void textResultEmitsEnvelope() {
    StringWriter buf = new StringWriter();
    new JsonFormatter().format("status", TextResult.of("ok"), new PrintWriter(buf));
    String out = buf.toString().trim();
    assertTrue(out.contains("\"status\":\"ok\""));
    assertTrue(out.contains("\"command\":\"status\""));
    assertTrue(out.contains("\"lines\""));
  }

  @Test
  public void tabularResultEmitsNdjsonThenTrailer() {
    StringWriter buf = new StringWriter();
    TabularResult result = new TabularResult(Arrays.asList("ROW", "CELL"),
      Arrays.asList(Arrays.asList("r1", "v1"), Arrays.asList("r2", "v2")));
    new JsonFormatter().format("scan", result, new PrintWriter(buf));
    String[] lines = buf.toString().trim().split("\n");
    assertEquals(3, lines.length);
    assertTrue(lines[0].contains("\"ROW\":\"r1\""));
    assertTrue(lines[1].contains("\"ROW\":\"r2\""));
    assertTrue(lines[2].contains("\"rows\":2"));
    assertTrue(lines[2].contains("\"command\":\"scan\""));
  }
}
