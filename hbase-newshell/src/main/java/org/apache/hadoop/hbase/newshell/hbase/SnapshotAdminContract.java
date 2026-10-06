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
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.yetus.audience.InterfaceAudience;

/** Snapshot create/delete/list/clone/restore operations. */
@InterfaceAudience.Private
public interface SnapshotAdminContract {
  void snapshot(String tableName, String snapshotName) throws IOException;

  /** Snapshot with the {@code TTL}, {@code MAX_FILESIZE} and {@code SKIP_FLUSH} options. */
  void snapshot(String tableName, String snapshotName, Map<String, Object> options)
    throws ShellCommandException, IOException;

  void deleteSnapshot(String snapshotName) throws IOException;

  void deleteAllSnapshots(String regex) throws IOException;

  List<SnapshotInfo> listSnapshots(String regex) throws IOException;

  List<SnapshotInfo> listTableSnapshots(String tableNameRegex, String snapshotNameRegex)
    throws IOException;

  void cloneSnapshot(String snapshotName, String tableName, boolean restoreAcl, String cloneSft)
    throws IOException;

  void restoreSnapshot(String snapshotName, boolean restoreAcl) throws IOException;

  boolean snapshotCleanupSwitch(boolean enabled) throws IOException;

  boolean snapshotCleanupEnabled() throws IOException;
}
