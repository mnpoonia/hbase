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

/**
 * {@link SnapshotAdminContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or via
 * {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubSnapshotAdminContract extends SnapshotAdminContract {
  @Override
  default void snapshot(String tableName, String snapshotName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void snapshot(String tableName, String snapshotName,
    java.util.Map<String, Object> options) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void deleteSnapshot(String snapshotName) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void deleteAllSnapshots(String regex) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<SnapshotInfo> listSnapshots(String regex) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<SnapshotInfo> listTableSnapshots(String tableNameRegex, String snapshotNameRegex)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl,
    String cloneSft) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void restoreSnapshot(String snapshotName, boolean restoreAcl) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean snapshotCleanupSwitch(boolean enabled) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default boolean snapshotCleanupEnabled() throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
