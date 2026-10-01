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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Quota and RPC-throttle operations. Mirrors the method set delegated internally by
 * {@link DefaultShellAdmin} to {@code QuotaAdminOps}; named {@code QuotaAdminContract} (not
 * {@code QuotaAdminOps}) to avoid a same-package name collision with that implementation class.
 */
@InterfaceAudience.Private
public interface QuotaAdminContract {
  void setQuota(Map<String, Object> args) throws IOException;

  List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException;

  List<List<String>> listQuotaTableSizes() throws IOException;

  List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) throws IOException;

  List<List<String>> listSnapshotSizes() throws IOException;

  boolean switchRpcThrottle(boolean enabled) throws IOException;

  boolean isRpcThrottleEnabled() throws IOException;

  boolean switchExceedThrottleQuota(boolean enabled) throws IOException;
}
