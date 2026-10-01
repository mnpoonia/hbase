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
import org.apache.yetus.audience.InterfaceAudience;

/** RegionServer and Master lifecycle: decommission/recommission, stop, and liveness listing. */
@InterfaceAudience.Private
public interface ServerLifecycleContract {
  void decommissionRegionServers(List<String> hostOrServers, boolean offload) throws IOException;

  void recommissionRegionServer(String hostOrServer, List<String> encodedRegionNames)
    throws IOException;

  List<String> listDecommissionedRegionServers() throws IOException;

  void stopMaster() throws IOException;

  void stopRegionServer(String hostPort) throws IOException;

  List<String> listDeadServers() throws IOException;

  List<String> listLiveServers() throws IOException;

  List<String> listUnknownServers() throws IOException;

  List<String> clearDeadServers(List<String> serverNames) throws IOException;
}
