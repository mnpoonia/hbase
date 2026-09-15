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

import java.util.UUID;
import org.apache.hadoop.hbase.replication.BaseReplicationEndpoint;
import org.apache.hadoop.hbase.replication.WALEntryFilter;

/**
 * A no-op replication endpoint whose only purpose is
 * {@link #canReplicateToSameCluster()} returning {@code true}, so
 * {@code add_peer}/{@code list_peers}/{@code remove_peer} minicluster tests can add a peer
 * pointing back at the same single-node minicluster without HBase's normal
 * "should not replicate to itself" guard rejecting it.
 */
public class SelfReplicationEndpointForTest extends BaseReplicationEndpoint {
  @Override
  public boolean canReplicateToSameCluster() {
    return true;
  }

  @Override
  public UUID getPeerUUID() {
    return ctx.getClusterId();
  }

  @Override
  public WALEntryFilter getWALEntryfilter() {
    return null;
  }

  @Override
  public boolean replicate(ReplicateContext replicateContext) {
    return true;
  }

  @Override
  public void start() {
    startAsync();
  }

  @Override
  public void stop() {
    stopAsync();
  }

  @Override
  protected void doStart() {
    notifyStarted();
  }

  @Override
  protected void doStop() {
    notifyStopped();
  }
}
