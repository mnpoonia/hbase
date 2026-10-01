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

import java.util.Objects;
import org.apache.hadoop.hbase.client.BalanceResponse;
import org.apache.yetus.audience.InterfaceAudience;

/** Newshell view of one balancer run — keeps {@link BalanceResponse} inside the hbase package. */
@InterfaceAudience.Private
public final class BalanceResult {
  private final boolean ran;
  private final int movesCalculated;
  private final int movesExecuted;

  public BalanceResult(boolean ran, int movesCalculated, int movesExecuted) {
    this.ran = ran;
    this.movesCalculated = movesCalculated;
    this.movesExecuted = movesExecuted;
  }

  public static BalanceResult from(BalanceResponse response) {
    return new BalanceResult(response.isBalancerRan(), response.getMovesCalculated(),
      response.getMovesExecuted());
  }

  public boolean ran() {
    return ran;
  }

  public int movesCalculated() {
    return movesCalculated;
  }

  public int movesExecuted() {
    return movesExecuted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BalanceResult)) {
      return false;
    }
    BalanceResult other = (BalanceResult) o;
    return ran == other.ran && movesCalculated == other.movesCalculated
      && movesExecuted == other.movesExecuted;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ran, movesCalculated, movesExecuted);
  }

  @Override
  public String toString() {
    return "BalanceResult[ran=" + ran + ", movesCalculated=" + movesCalculated
      + ", movesExecuted=" + movesExecuted + "]";
  }
}
