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
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * {@link NamespaceAdminContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class NamespaceAdminOps implements NamespaceAdminContract {
  private final Admin admin;

  NamespaceAdminOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public void createNamespace(String namespace, Map<String, Object> properties) throws IOException {
    NamespaceDescriptor.Builder builder = NamespaceDescriptor.create(namespace);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      builder.addConfiguration(entry.getKey(), String.valueOf(entry.getValue()));
    }
    admin.createNamespace(builder.build());
  }

  @Override
  public void dropNamespace(String namespace) throws IOException {
    admin.deleteNamespace(namespace);
  }

  @Override
  public void alterNamespace(String namespace, Map<String, Object> properties) throws IOException {
    NamespaceDescriptor existing = admin.getNamespaceDescriptor(namespace);
    NamespaceDescriptor.Builder builder = NamespaceDescriptor.create(existing);
    String method = String.valueOf(properties.get("METHOD"));
    if ("unset".equalsIgnoreCase(method)) {
      Object name = properties.get("NAME");
      if (name == null) {
        throw new ClientErrorException("alter_namespace unset requires NAME");
      }
      builder.removeConfiguration(String.valueOf(name));
    } else {
      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        if ("METHOD".equals(entry.getKey())) {
          continue;
        }
        builder.addConfiguration(entry.getKey(), String.valueOf(entry.getValue()));
      }
    }
    admin.modifyNamespace(builder.build());
  }

  @Override
  public String describeNamespace(String namespace) throws IOException {
    return admin.getNamespaceDescriptor(namespace).toString();
  }

  @Override
  public List<String> listNamespaces(String regex) throws IOException {
    Pattern pattern = Pattern.compile(regex);
    List<String> result = new ArrayList<>();
    for (NamespaceDescriptor descriptor : admin.listNamespaceDescriptors()) {
      if (pattern.matcher(descriptor.getName()).matches()) {
        result.add(descriptor.getName());
      }
    }
    return result;
  }

  @Override
  public List<String> listNamespaceTables(String namespace) throws IOException {
    return Arrays.stream(admin.listTableNamesByNamespace(namespace))
      .map(TableName::getQualifierAsString).collect(Collectors.toList());
  }

  @Override
  public String getNamespaceRsGroup(String namespace) throws IOException {
    NamespaceDescriptor nsd = admin.getNamespaceDescriptor(namespace);
    return nsd.getConfigurationValue("hbase.rsgroup.name");
  }
}
