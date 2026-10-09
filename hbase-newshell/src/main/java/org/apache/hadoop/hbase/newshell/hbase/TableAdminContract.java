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

/** Table DDL, region/compaction/snapshot-adjacent admin, and cluster status operations. */
@InterfaceAudience.Private
public interface TableAdminContract {
  StatusView status() throws IOException;

  void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) throws IOException;

  void disableTable(String tableName) throws IOException;

  void enableTable(String tableName) throws IOException;

  void dropTable(String tableName) throws IOException;

  List<String> listTables(String regex) throws IOException;

  TableDescription describeTable(String tableName) throws IOException;

  void alterTable(String tableName, List<Map<String, Object>> familySpecs) throws IOException;

  /** Like {@link #alterTable} but returns once the modify procedure is submitted. */
  void alterTableNoWait(String tableName, List<Map<String, Object>> familySpecs) throws IOException;

  boolean tableExists(String tableName) throws IOException;

  void compact(String tableOrRegionName, String family, String type) throws IOException;

  void majorCompact(String tableOrRegionName, String family, String type) throws IOException;

  void split(String tableOrRegionName, String splitPoint) throws IOException;

  void truncateTable(String tableName, boolean preserveSplits) throws IOException;

  boolean isTableDisabled(String tableName) throws IOException;

  boolean isTableEnabled(String tableName) throws IOException;

  List<String> listTablesByState(boolean enabled) throws IOException;

  AlterStatusView alterStatus(String tableName) throws IOException;

  void cloneTableSchema(String tableName, String newTableName, boolean preserveSplits)
    throws IOException;

  RegionLocationView locateRegion(String tableName, String rowKey) throws IOException;

  ListRegionsView listRegions(String tableName) throws IOException;

  void flush(String tableOrRegionOrServerName, String family) throws IOException;

  void assign(String regionName) throws IOException;

  void unassign(String regionName) throws IOException;

  void reopenRegions(String tableName, List<String> regionNames) throws IOException;

  void mergeRegion(List<String> regionNames, boolean force) throws IOException;

  String regionInfo(String regionName) throws IOException;

  List<String> regionsInTransition() throws IOException;

  void truncateRegion(String regionName) throws IOException;

  void compactRegionServer(String serverName, boolean major) throws IOException;

  String getCompactionState(String tableName) throws IOException;

  void clearCompactionQueues(String serverName, List<String> queueNames) throws IOException;

  String clearBlockCache(String tableName) throws IOException;

  void changeSft(String tableName, String family, String sft) throws IOException;

  void changeSftAll(String tableRegex, String sft) throws IOException;

  void move(String encodedRegionName, String destServerName) throws IOException;

  long refreshHFiles(Map<String, Object> args) throws IOException;
}
