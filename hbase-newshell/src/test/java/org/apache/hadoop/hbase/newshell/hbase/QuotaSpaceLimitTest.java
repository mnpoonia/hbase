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

import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class QuotaSpaceLimitTest {
  @Test
  public void parsesRawBytesAndSuffixes() throws Exception {
    assertEquals(100L, QuotaAdminOps.parseSpaceLimit(100));
    assertEquals(100L, QuotaAdminOps.parseSpaceLimit("100"));
    assertEquals(1024L, QuotaAdminOps.parseSpaceLimit("1k"));
    assertEquals(1L << 30, QuotaAdminOps.parseSpaceLimit("1G"));
    assertEquals(2L << 40, QuotaAdminOps.parseSpaceLimit("2T"));
  }

  @Test
  public void rejectsBadOrNonPositiveLimits() {
    assertThrows(ClientErrorException.class, () -> QuotaAdminOps.parseSpaceLimit("abc"));
    assertThrows(ClientErrorException.class, () -> QuotaAdminOps.parseSpaceLimit("1.5G"));
    assertThrows(ClientErrorException.class, () -> QuotaAdminOps.parseSpaceLimit(0));
  }
}
