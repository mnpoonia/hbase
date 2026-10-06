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
package org.apache.hadoop.hbase.newshell.command.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * The trailing QUOTAS section of {@code describe} / {@code describe_namespace}: the owner's quota
 * settings when the {@code hbase:quota} table exists, else {@code Quota is disabled}.
 */
@InterfaceAudience.Private
final class QuotaSection {
  private static final String QUOTA_TABLE = "hbase:quota";

  private QuotaSection() {
  }

  static List<String> lines(ExecutionContext context, String filterKey, String owner)
    throws IOException {
    if (!context.tableAdmin().tableExists(QUOTA_TABLE)) {
      return Collections.singletonList("Quota is disabled");
    }
    List<String> lines = new ArrayList<>();
    lines.add("QUOTAS");
    List<List<String>> quotas =
      context.quotaAdmin().listQuotas(Collections.<String, Object> singletonMap(filterKey, owner));
    for (List<String> quota : quotas) {
      lines.add(quota.get(quota.size() - 1));
    }
    lines.add(quotas.size() + " row(s)");
    return lines;
  }
}
