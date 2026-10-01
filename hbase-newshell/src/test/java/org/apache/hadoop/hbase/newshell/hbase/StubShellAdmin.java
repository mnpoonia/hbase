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

/**
 * {@link ShellAdmin} with every method of every {@code *Contract} sub-interface defaulted to
 * throw {@link UnsupportedOperationException}, via the per-domain {@code Stub*Contract} mixins.
 * Command-under-test fakes extend this and override only the method(s) their command actually
 * calls, instead of every test re-declaring all of {@link ShellAdmin}'s methods.
 */
public class StubShellAdmin implements ShellAdmin, StubTableAdminContract,
  StubNamespaceAdminContract, StubSnapshotAdminContract, StubReplicationPeerContract,
  StubRsGroupAdminContract, StubClusterOpsContract, StubServerLifecycleContract,
  StubDiagnosticsContract, StubSecurityAdminContract, StubVisibilityLabelContract,
  StubQuotaAdminContract, StubProcedureAdminContract {
}
