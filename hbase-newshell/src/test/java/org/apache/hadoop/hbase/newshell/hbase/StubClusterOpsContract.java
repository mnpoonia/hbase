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
 * {@link ClusterOpsContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or via
 * {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubClusterOpsContract extends ClusterOpsContract {
  @Override
  default boolean balancerSwitch(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean normalizerSwitch(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean catalogJanitorSwitch(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean splitOrMergeSwitch(String switchType, boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean balancerEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean normalizerEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean catalogJanitorEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean splitOrMergeEnabled(String switchType) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void updateConfig(String serverName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void updateAllConfig() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void updateRsGroupConfig(String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean normalize(Map<String, Object> filterArgs) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean isInMaintenanceMode() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void flushMasterStore() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default int catalogJanitorRun() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean hbckChoreRun() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean cleanerChoreRun() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean cleanerChoreSwitch(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean cleanerChoreEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void walRoll(String serverName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void walRollAll() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default long refreshMeta() throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
