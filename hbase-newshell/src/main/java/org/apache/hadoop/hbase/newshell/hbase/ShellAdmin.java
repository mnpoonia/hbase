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

import org.apache.yetus.audience.InterfaceAudience;

/**
 * Narrow, newshell-specific facade over the admin-plane operations the pilot commands need. Command
 * implementations depend on this interface, never on {@code org.apache.hadoop.hbase.client.Admin}
 * directly - {@link DefaultShellAdmin} is the only class that does.
 * <p>
 * Composed of cohesive per-domain sub-interfaces rather than declaring ~155 methods directly, so
 * each domain's contract can be read, tested, and (if ever needed) implemented independently of the
 * others.
 */
@InterfaceAudience.Private
public interface ShellAdmin extends TableAdminContract, NamespaceAdminContract,
  SnapshotAdminContract, ReplicationPeerContract, RsGroupAdminContract, ClusterOpsContract,
  ServerLifecycleContract, DiagnosticsContract, SecurityAdminContract, VisibilityLabelContract,
  QuotaAdminContract, ProcedureAdminContract {
}
