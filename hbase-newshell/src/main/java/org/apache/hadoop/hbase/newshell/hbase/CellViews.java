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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.yetus.audience.InterfaceAudience;

/** Formatting helpers for {@link CellView} that keep Ruby shell cell text stable. */
@InterfaceAudience.Private
public final class CellViews {
  private CellViews() {
  }

  /** {@code timestamp=<local-datetime>, value=<value>} — mirrors get/scan Ruby cell layout. */
  public static String formatCell(CellView cell) {
    String timestamp = LocalDateTime
      .ofInstant(Instant.ofEpochMilli(cell.timestamp()), ZoneId.systemDefault()).toString();
    return "timestamp=" + timestamp + ", value=" + cell.value();
  }

  public static String column(CellView cell) {
    return cell.family() + ":" + cell.qualifier();
  }
}
