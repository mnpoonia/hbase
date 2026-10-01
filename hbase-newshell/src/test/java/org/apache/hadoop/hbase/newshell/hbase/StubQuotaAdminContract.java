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

/**
 * {@link QuotaAdminContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or via
 * {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubQuotaAdminContract extends QuotaAdminContract {
  @Override
  default void setQuota(Map<String, Object> args) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> listQuotas(Map<String, Object> filterArgs) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> listQuotaTableSizes() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> listQuotaSnapshots(Map<String, Object> filterArgs) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> listSnapshotSizes() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean switchRpcThrottle(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean isRpcThrottleEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean switchExceedThrottleQuota(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
