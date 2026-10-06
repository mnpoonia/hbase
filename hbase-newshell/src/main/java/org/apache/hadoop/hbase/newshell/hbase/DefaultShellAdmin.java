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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Builds the real {@link ShellAdmin} over an {@link Admin}. {@link ShellAdmin} is the union of the
 * per-domain contracts ({@link TableAdminContract}, {@link NamespaceAdminContract},
 * {@link ClusterOpsContract}, {@link ServerLifecycleContract}, {@link SnapshotAdminContract},
 * {@link ReplicationPeerContract}, {@link SecurityAdminContract}, {@link VisibilityLabelContract},
 * {@link QuotaAdminContract}, {@link ProcedureAdminContract}, {@link RsGroupAdminContract} and
 * {@link DiagnosticsContract}), each implemented by a package-private {@code *Ops} class ported
 * from hbase-shell's {@code hbase/admin.rb} and the matching {@code shell/commands/*.rb}.
 * <p>
 * Rather than hand-writing one forwarding method per contract method, {@link #create} resolves
 * every {@link ShellAdmin} method to the {@code *Ops} object that implements it once, up front, and
 * fails immediately if any method has no implementation or more than one. So a contract method
 * added without an implementation breaks construction (and the unit tests), never a user command.
 */
@InterfaceAudience.Private
public final class DefaultShellAdmin {
  private DefaultShellAdmin() {
  }

  public static ShellAdmin create(Admin admin) {
    List<Object> ops = new ArrayList<>();
    ops.add(new TableAdminOps(admin));
    ops.add(new NamespaceAdminOps(admin));
    ops.add(new ClusterAdminOps(admin));
    ops.add(new ServerLifecycleOps(admin));
    ops.add(new SnapshotAdminOps(admin));
    ops.add(new ReplicationAdminOps(admin));
    ops.add(new SecurityAdminOps(admin));
    ops.add(new VisibilityLabelOps(admin));
    ops.add(new QuotaAdminOps(admin));
    ops.add(new ProcedureAdminOps(admin));
    ops.add(new RsGroupAdminOps(admin));
    ops.add(new DiagnosticsOps(admin));
    return (ShellAdmin) Proxy.newProxyInstance(ShellAdmin.class.getClassLoader(),
      new Class<?>[] { ShellAdmin.class }, new Dispatcher(resolve(ops)));
  }

  private static Map<Method, Target> resolve(List<Object> ops) {
    Map<Method, Target> targets = new HashMap<>();
    for (Method method : ShellAdmin.class.getMethods()) {
      Target found = null;
      for (Object op : ops) {
        Method impl;
        try {
          impl = op.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
          continue;
        }
        if (found != null) {
          throw new IllegalStateException(
            "ShellAdmin." + method.getName() + " is implemented by both "
              + found.op.getClass().getSimpleName() + " and " + op.getClass().getSimpleName());
        }
        impl.setAccessible(true);
        found = new Target(op, impl);
      }
      if (found == null) {
        throw new IllegalStateException(
          "No implementation for ShellAdmin." + method.getName() + " in any *Ops class");
      }
      targets.put(method, found);
    }
    return targets;
  }

  private static final class Target {
    private final Object op;
    private final Method impl;

    Target(Object op, Method impl) {
      this.op = op;
      this.impl = impl;
    }
  }

  private static final class Dispatcher implements InvocationHandler {
    private final Map<Method, Target> targets;

    Dispatcher(Map<Method, Target> targets) {
      this.targets = targets;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        switch (method.getName()) {
          case "equals":
            return proxy == args[0];
          case "hashCode":
            return System.identityHashCode(proxy);
          default:
            return "DefaultShellAdmin";
        }
      }
      Target target = targets.get(method);
      try {
        return target.impl.invoke(target.op, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
