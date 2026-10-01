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
 * {@link RsGroupAdminContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or
 * via {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubRsGroupAdminContract extends RsGroupAdminContract {
  @Override
  default RsGroupView getRsGroup(String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void moveServersToRsGroup(List<String> hostPorts, String groupName)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<RsGroupSummary> listRsGroups(String regex) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void addRsGroup(String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removeRsGroup(String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default BalanceResult balanceRsGroup(String groupName, boolean dryRun,
    boolean ignoreRegionsInTransition) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void moveTablesToRsGroup(List<String> tables, String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void moveNamespacesToRsGroup(List<String> namespaces, String groupName)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void moveServersAndTablesToRsGroup(List<String> hostPorts, List<String> tables,
    String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void moveServersAndNamespacesToRsGroup(List<String> hostPorts,
    List<String> namespaces, String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String getRsGroupOfServer(String hostPort) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String getRsGroupOfTable(String tableName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void removeServersFromRsGroup(List<String> hostPorts) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void renameRsGroup(String oldName, String newName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void alterRsGroupConfig(String groupName, Map<String, Object> args)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<List<String>> showRsGroupConfig(String groupName) throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
