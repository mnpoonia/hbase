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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class OptionValuesTest {
  @Test
  public void wholeNumbersPassThrough() throws Exception {
    assertEquals(5, OptionValues.optInt(Collections.singletonMap("N", (Object) 5L), "N"));
    assertEquals(3, OptionValues.optInt(Collections.singletonMap("N", (Object) 3.0d), "N"));
    assertEquals(7L, OptionValues.optLong(Collections.singletonMap("N", (Object) 7), "N"));
  }

  @Test
  public void fractionalValuesAreRejectedNotTruncated() {
    assertThrows(ShellCommandException.class,
      () -> OptionValues.optInt(Collections.singletonMap("N", (Object) 2.5d), "N"));
    assertThrows(ShellCommandException.class,
      () -> OptionValues.optLong(Collections.singletonMap("N", (Object) 2.5d), "N"));
  }

  @Test
  public void intOverflowIsRejectedNotWrapped() {
    assertThrows(ShellCommandException.class,
      () -> OptionValues.optInt(Collections.singletonMap("N", (Object) 4294967296L), "N"));
    assertThrows(IllegalArgumentException.class, () -> AttributeCoercion.toInt(4294967296L));
    assertThrows(IllegalArgumentException.class, () -> AttributeCoercion.toLong(1.5d));
  }
}
