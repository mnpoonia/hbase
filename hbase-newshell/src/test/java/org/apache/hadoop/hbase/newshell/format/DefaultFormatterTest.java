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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.apache.hadoop.hbase.newshell.command.StreamingTabularResult;
import org.apache.hadoop.hbase.newshell.command.TabularResult;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class DefaultFormatterTest {
  @Test
  public void tabularResultEmitsHeaderRowsAndFooter() {
    StringWriter buf = new StringWriter();
    TabularResult result = new TabularResult(Arrays.asList("ROW", "COLUMN+CELL"),
      Arrays.asList(Arrays.asList("r1", "cell1"), Arrays.asList("r2", "cell2")));
    new DefaultFormatter().format("scan", result, new PrintWriter(buf));
    assertEquals("ROW  COLUMN+CELL\n r1 cell1\n r2 cell2\n2 row(s)\n", buf.toString());
  }

  @Test
  public void streamingTabularResultMatchesTabularResultOutput() {
    StringWriter buf = new StringWriter();
    StreamingTabularResult result =
      new StreamingTabularResult(Arrays.asList("ROW", "COLUMN+CELL"), rowConsumer -> {
        rowConsumer.accept(Arrays.asList("r1", "cell1"));
        rowConsumer.accept(Arrays.asList("r2", "cell2"));
      });
    new DefaultFormatter().format("scan", result, new PrintWriter(buf));
    assertEquals("ROW  COLUMN+CELL\n r1 cell1\n r2 cell2\n2 row(s)\n", buf.toString());
  }
}
