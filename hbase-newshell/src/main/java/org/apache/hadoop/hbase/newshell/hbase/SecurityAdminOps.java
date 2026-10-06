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
import java.util.Arrays;
import java.util.List;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.Permission;
import org.apache.hadoop.hbase.security.access.UserPermission;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link SecurityAdminContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class SecurityAdminOps implements SecurityAdminContract {
  private final Admin admin;

  SecurityAdminOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public void grant(String userOrGroup, String actions, String tableName, String family,
    String qualifier, String namespace) throws IOException {
    Permission.Action[] permActions = parseActions(actions);
    try {
      if (namespace != null) {
        AccessControlClient.grant(admin.getConnection(), namespace, userOrGroup, permActions);
      } else if (tableName != null) {
        byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
        byte[] qualifierBytes = qualifier == null ? null : Bytes.toBytes(qualifier);
        AccessControlClient.grant(admin.getConnection(), TableName.valueOf(tableName), userOrGroup,
          familyBytes, qualifierBytes, permActions);
      } else {
        AccessControlClient.grant(admin.getConnection(), userOrGroup, permActions);
      }
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
  }

  private static Permission.Action[] parseActions(String actions) throws IOException {
    Permission.Action[] result = new Permission.Action[actions.length()];
    for (int i = 0; i < actions.length(); i++) {
      result[i] = charToAction(actions.charAt(i));
    }
    return result;
  }

  @Override
  public void revoke(String userOrGroup, String tableName, String family, String qualifier,
    String namespace) throws IOException {
    try {
      if (namespace != null) {
        AccessControlClient.revoke(admin.getConnection(), namespace, userOrGroup);
      } else if (tableName != null) {
        byte[] familyBytes = family == null ? null : Bytes.toBytes(family);
        byte[] qualifierBytes = qualifier == null ? null : Bytes.toBytes(qualifier);
        AccessControlClient.revoke(admin.getConnection(), TableName.valueOf(tableName), userOrGroup,
          familyBytes, qualifierBytes);
      } else {
        AccessControlClient.revoke(admin.getConnection(), userOrGroup, new Permission.Action[0]);
      }
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
  }

  @Override
  public List<List<String>> userPermission(String tableOrNamespaceRegex) throws IOException {
    try {
      List<UserPermission> permissions =
        AccessControlClient.getUserPermissions(admin.getConnection(), tableOrNamespaceRegex);
      List<List<String>> rows = new ArrayList<>();
      for (UserPermission permission : permissions) {
        rows.add(Arrays.asList(permission.getUser(), permission.getPermission().toString()));
      }
      return rows;
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
  }

  @Override
  public List<String> listSecurityCapabilities() throws IOException {
    List<String> names = new ArrayList<>();
    for (org.apache.hadoop.hbase.client.security.SecurityCapability capability : admin
      .getSecurityCapabilities()) {
      names.add(capability.getName());
    }
    return names;
  }

  private static Permission.Action charToAction(char c) throws IOException {
    switch (Character.toUpperCase(c)) {
      case 'R':
        return Permission.Action.READ;
      case 'W':
        return Permission.Action.WRITE;
      case 'X':
        return Permission.Action.EXEC;
      case 'C':
        return Permission.Action.CREATE;
      case 'A':
        return Permission.Action.ADMIN;
      default:
        throw new ClientErrorException("Unknown permission action: " + c);
    }
  }
}
