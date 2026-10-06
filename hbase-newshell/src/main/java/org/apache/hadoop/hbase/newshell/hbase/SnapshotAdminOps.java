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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.SnapshotDescription;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link SnapshotAdminContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class SnapshotAdminOps implements SnapshotAdminContract {
  private final Admin admin;

  SnapshotAdminOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public void snapshot(String tableName, String snapshotName) throws IOException {
    admin.snapshot(snapshotName, TableName.valueOf(tableName));
  }

  @Override
  public void deleteSnapshot(String snapshotName) throws IOException {
    admin.deleteSnapshot(snapshotName);
  }

  @Override
  public void deleteAllSnapshots(String regex) throws IOException {
    admin.deleteSnapshots(Pattern.compile(regex));
  }

  @Override
  public List<SnapshotInfo> listSnapshots(String regex) throws IOException {
    List<SnapshotInfo> snapshots = new ArrayList<>();
    for (SnapshotDescription snapshot : admin.listSnapshots(Pattern.compile(regex))) {
      snapshots.add(toSnapshotInfo(snapshot));
    }
    return snapshots;
  }

  @Override
  public List<SnapshotInfo> listTableSnapshots(String tableNameRegex, String snapshotNameRegex)
    throws IOException {
    List<SnapshotInfo> snapshots = new ArrayList<>();
    for (SnapshotDescription snapshot : admin.listTableSnapshots(Pattern.compile(tableNameRegex),
      Pattern.compile(snapshotNameRegex))) {
      snapshots.add(toSnapshotInfo(snapshot));
    }
    return snapshots;
  }

  private static SnapshotInfo toSnapshotInfo(SnapshotDescription snapshot) {
    return new SnapshotInfo(snapshot.getName(), snapshot.getTableNameAsString(),
      snapshot.getCreationTime(), snapshot.getTtl());
  }

  @Override
  public void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl,
    String cloneSft) throws IOException {
    admin.cloneSnapshot(snapshotName, TableName.valueOf(tableName), restoreAcl, cloneSft);
  }

  @Override
  public void restoreSnapshot(String snapshotName, boolean restoreAcl) throws IOException {
    admin.restoreSnapshot(snapshotName, false, restoreAcl);
  }

  @Override
  public boolean snapshotCleanupSwitch(boolean enabled) throws IOException {
    return admin.snapshotCleanupSwitch(enabled, false);
  }

  @Override
  public boolean snapshotCleanupEnabled() throws IOException {
    return admin.isSnapshotCleanupEnabled();
  }
}
