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
package org.apache.hadoop.hbase.newshell.command;

import java.io.PrintWriter;
import org.apache.hadoop.hbase.newshell.hbase.ClusterOpsContract;
import org.apache.hadoop.hbase.newshell.hbase.DiagnosticsContract;
import org.apache.hadoop.hbase.newshell.hbase.NamespaceAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.ProcedureAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.QuotaAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.ReplicationPeerContract;
import org.apache.hadoop.hbase.newshell.hbase.RsGroupAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.SecurityAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.ServerLifecycleContract;
import org.apache.hadoop.hbase.newshell.hbase.ShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.ShellTableFactory;
import org.apache.hadoop.hbase.newshell.hbase.SnapshotAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.TableAdminContract;
import org.apache.hadoop.hbase.newshell.hbase.VisibilityLabelContract;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Bundles what a {@link ShellCommand} needs to do its work for one session. Deliberately exposes
 * only the newshell {@code hbase} wrapper abstractions ({@link ShellAdmin},
 * {@link ShellTableFactory}) - never the underlying
 * {@code org.apache.hadoop.hbase.client.Connection}/{@code Admin} - so commands depend on these
 * narrow interfaces rather than the full HBase client surface, and never reach into globals.
 * <p>
 * Admin access is exposed one contract at a time (e.g. {@link #tableAdmin()}) so each command
 * depends only on the operations it uses, not the whole {@link ShellAdmin}.
 * <p>
 * Intended for single-threaded use: one instance per session, accessed only by that session's REPL
 * loop.
 */
@InterfaceAudience.Private
public final class ExecutionContext {
  private final ShellAdmin admin;
  private final ShellTableFactory tables;
  private final PrintWriter out;
  private final SessionOptions options;
  private final ConfirmationReader confirmationReader;

  public ExecutionContext(ShellAdmin admin, ShellTableFactory tables, PrintWriter out) {
    this(admin, tables, out, SessionOptions.defaults(), null);
  }

  public ExecutionContext(ShellAdmin admin, ShellTableFactory tables, PrintWriter out,
    SessionOptions options) {
    this(admin, tables, out, options, null);
  }

  public ExecutionContext(ShellAdmin admin, ShellTableFactory tables, PrintWriter out,
    SessionOptions options, ConfirmationReader confirmationReader) {
    this.admin = admin;
    this.tables = tables;
    this.out = out;
    this.options = options == null ? SessionOptions.defaults() : options;
    this.confirmationReader = confirmationReader;
  }

  public TableAdminContract tableAdmin() {
    return admin;
  }

  public NamespaceAdminContract namespaceAdmin() {
    return admin;
  }

  public SnapshotAdminContract snapshotAdmin() {
    return admin;
  }

  public ReplicationPeerContract replicationPeers() {
    return admin;
  }

  public RsGroupAdminContract rsGroupAdmin() {
    return admin;
  }

  public ClusterOpsContract clusterOps() {
    return admin;
  }

  public ServerLifecycleContract serverLifecycle() {
    return admin;
  }

  public DiagnosticsContract diagnostics() {
    return admin;
  }

  public SecurityAdminContract securityAdmin() {
    return admin;
  }

  public VisibilityLabelContract visibilityLabels() {
    return admin;
  }

  public QuotaAdminContract quotaAdmin() {
    return admin;
  }

  public ProcedureAdminContract procedureAdmin() {
    return admin;
  }

  public ShellTableFactory tables() {
    return tables;
  }

  public PrintWriter out() {
    return out;
  }

  public SessionOptions options() {
    return options;
  }

  public ConfirmationReader confirmationReader() {
    return confirmationReader;
  }
}
