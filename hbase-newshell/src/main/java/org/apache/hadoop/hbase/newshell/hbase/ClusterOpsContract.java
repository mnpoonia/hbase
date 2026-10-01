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

/** Cluster-wide operational switches, chore triggers, and config-reload RPCs. */
@InterfaceAudience.Private
public interface ClusterOpsContract {
  boolean balancerSwitch(boolean enabled) throws IOException;

  boolean normalizerSwitch(boolean enabled) throws IOException;

  boolean catalogJanitorSwitch(boolean enabled) throws IOException;

  Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames)
    throws IOException;

  boolean splitOrMergeSwitch(String switchType, boolean enabled) throws IOException;

  boolean balancerEnabled() throws IOException;

  boolean normalizerEnabled() throws IOException;

  boolean catalogJanitorEnabled() throws IOException;

  boolean splitOrMergeEnabled(String switchType) throws IOException;

  void updateConfig(String serverName) throws IOException;

  void updateAllConfig() throws IOException;

  void updateRsGroupConfig(String groupName) throws IOException;

  BalanceResult balance(boolean dryRun, boolean ignoreRegionsInTransition) throws IOException;

  boolean normalize(Map<String, Object> filterArgs) throws IOException;

  boolean isInMaintenanceMode() throws IOException;

  void flushMasterStore() throws IOException;

  int catalogJanitorRun() throws IOException;

  boolean hbckChoreRun() throws IOException;

  boolean cleanerChoreRun() throws IOException;

  boolean cleanerChoreSwitch(boolean enabled) throws IOException;

  boolean cleanerChoreEnabled() throws IOException;

  void walRoll(String serverName) throws IOException;

  void walRollAll() throws IOException;

  long refreshMeta() throws IOException;
}
