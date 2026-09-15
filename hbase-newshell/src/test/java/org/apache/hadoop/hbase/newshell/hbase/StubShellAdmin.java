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

import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.ClusterMetrics;

/**
 * Every method throws {@link UnsupportedOperationException} by default. Command-under-test
 * fakes extend this and override only the method(s) their command actually calls, instead of
 * every test re-declaring all of {@link ShellAdmin}'s methods.
 */
public class StubShellAdmin implements ShellAdmin {
  private static UnsupportedOperationException notNeeded() {
    return new UnsupportedOperationException("not needed for this test");
  }

  @Override
  public ClusterMetrics status() {
    throw notNeeded();
  }

  @Override
  public void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) {
    throw notNeeded();
  }

  @Override
  public void disableTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void enableTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void dropTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public List<String> listTables(String regex) {
    throw notNeeded();
  }

  @Override
  public TableDescription describeTable(String tableName) {
    throw notNeeded();
  }

  @Override
  public void decommissionRegionServers(List<String> hostOrServers, boolean offload) {
    throw notNeeded();
  }

  @Override
  public void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames) {
    throw notNeeded();
  }

  @Override
  public List<String> listDecommissionedRegionServers() {
    throw notNeeded();
  }

  @Override
  public void alterTable(String tableName, List<Map<String, Object>> familySpecs) {
    throw notNeeded();
  }

  @Override
  public boolean tableExists(String tableName) {
    throw notNeeded();
  }

  @Override
  public void compact(String tableOrRegionName, String family, String type) {
    throw notNeeded();
  }

  @Override
  public void majorCompact(String tableOrRegionName, String family, String type) {
    throw notNeeded();
  }

  @Override
  public void split(String tableOrRegionName, String splitPoint) {
    throw notNeeded();
  }

  @Override
  public void addPeer(String peerId, Map<String, Object> peerConfigSpec) {
    throw notNeeded();
  }

  @Override
  public void removePeer(String peerId) {
    throw notNeeded();
  }

  @Override
  public List<PeerDescription> listPeers() {
    throw notNeeded();
  }

  @Override
  public void snapshot(String tableName, String snapshotName) {
    throw notNeeded();
  }

  @Override
  public void deleteSnapshot(String snapshotName) {
    throw notNeeded();
  }

  @Override
  public List<SnapshotInfo> listSnapshots(String regex) {
    throw notNeeded();
  }

  @Override
  public boolean balancerSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean normalizerSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean catalogJanitorSwitch(boolean enabled) {
    throw notNeeded();
  }

  @Override
  public Map<String, Boolean> compactionSwitch(boolean enabled, List<String> serverNames) {
    throw notNeeded();
  }

  @Override
  public boolean splitOrMergeSwitch(String switchType, boolean enabled) {
    throw notNeeded();
  }

  @Override
  public boolean balancerEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean normalizerEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean catalogJanitorEnabled() {
    throw notNeeded();
  }

  @Override
  public boolean splitOrMergeEnabled(String switchType) {
    throw notNeeded();
  }
}
