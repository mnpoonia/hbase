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
 * {@link TableAdminContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or via
 * {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubTableAdminContract extends TableAdminContract {
  @Override
  default StatusView status() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void createTable(String tableName, List<Map<String, Object>> familySpecs,
    Map<String, Object> tableAttributes) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void disableTable(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void enableTable(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void dropTable(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<String> listTables(String regex) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default TableDescription describeTable(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void alterTable(String tableName, List<Map<String, Object>> familySpecs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void alterTableNoWait(String tableName, List<Map<String, Object>> familySpecs)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean tableExists(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void compact(String tableOrRegionName, String family, String type) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void majorCompact(String tableOrRegionName, String family, String type)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void split(String tableOrRegionName, String splitPoint) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void truncateTable(String tableName, boolean preserveSplits) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean isTableDisabled(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean isTableEnabled(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<String> listTablesByState(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default AlterStatusView alterStatus(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void cloneTableSchema(String tableName, String newTableName, boolean preserveSplits)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default RegionLocationView locateRegion(String tableName, String rowKey) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default ListRegionsView listRegions(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void flush(String tableOrRegionOrServerName, String family) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void assign(String regionName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void unassign(String regionName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void reopenRegions(String tableName, List<String> regionNames) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void mergeRegion(List<String> regionNames, boolean force) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String regionInfo(String regionName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<String> regionsInTransition() throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void truncateRegion(String regionName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void compactRegionServer(String serverName, boolean major) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String getCompactionState(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void clearCompactionQueues(String serverName, List<String> queueNames)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String clearBlockCache(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void changeSft(String tableName, String family, String sft) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void changeSftAll(String tableRegex, String sft) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void move(String encodedRegionName, String destServerName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default long refreshHFiles(Map<String, Object> args) throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
