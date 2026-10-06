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
import java.util.Map;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.security.visibility.VisibilityClient;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

import org.apache.hbase.thirdparty.com.google.protobuf.ByteString;

import org.apache.hadoop.hbase.shaded.protobuf.generated.ClientProtos.RegionActionResult;
import org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.ListLabelsResponse;
import org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.VisibilityLabelsResponse;

/**
 * {@link VisibilityLabelContract} operations on a real {@link Admin}, used by
 * {@link DefaultShellAdmin}.
 */
@InterfaceAudience.Private
final class VisibilityLabelOps implements VisibilityLabelContract {
  private final Admin admin;

  VisibilityLabelOps(Admin admin) {
    this.admin = admin;
  }

  @Override
  public void addLabels(List<String> labels) throws IOException {
    VisibilityLabelsResponse response;
    try {
      response = VisibilityClient.addLabels(admin.getConnection(), labels.toArray(new String[0]));
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    throwIfVisibilityFailures(response);
  }

  @Override
  public List<String> listLabels(String regex) throws IOException {
    ListLabelsResponse response;
    try {
      response = VisibilityClient.listLabels(admin.getConnection(), regex);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    if (response == null) {
      throw new IOException("DISABLED: Visibility labels feature is not available");
    }
    List<String> labels = new ArrayList<>();
    for (ByteString label : response.getLabelList()) {
      labels.add(Bytes.toStringBinary(label.toByteArray()));
    }
    return labels;
  }

  @Override
  public void setAuths(String user, List<String> labels) throws IOException {
    VisibilityLabelsResponse response;
    try {
      response =
        VisibilityClient.setAuths(admin.getConnection(), labels.toArray(new String[0]), user);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    throwIfVisibilityFailures(response);
  }

  @Override
  public List<String> getAuths(String user) throws IOException {
    org.apache.hadoop.hbase.shaded.protobuf.generated.VisibilityLabelsProtos.GetAuthsResponse response;
    try {
      response = VisibilityClient.getAuths(admin.getConnection(), user);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    if (response == null) {
      throw new IOException("DISABLED: Visibility labels feature is not available");
    }
    List<String> labels = new ArrayList<>();
    for (ByteString auth : response.getAuthList()) {
      labels.add(Bytes.toStringBinary(auth.toByteArray()));
    }
    return labels;
  }

  @Override
  public void clearAuths(String user, List<String> labels) throws IOException {
    VisibilityLabelsResponse response;
    try {
      response =
        VisibilityClient.clearAuths(admin.getConnection(), labels.toArray(new String[0]), user);
    } catch (Throwable t) {
      if (t instanceof IOException) {
        throw (IOException) t;
      }
      throw new IOException(t);
    }
    throwIfVisibilityFailures(response);
  }

  @Override
  public long setVisibility(String tableName, String visibility, Map<String, Object> scanOptions)
    throws IOException {
    return VisibilityOps.setVisibility(admin.getConnection(), tableName, visibility, scanOptions);
  }

  private static void throwIfVisibilityFailures(VisibilityLabelsResponse response)
    throws IOException {
    if (response == null) {
      throw new IOException("DISABLED: Visibility labels feature is not available");
    }
    StringBuilder failures = new StringBuilder();
    for (RegionActionResult result : response.getResultList()) {
      if (result.hasException()) {
        failures.append(result.getException().getValue().toStringUtf8());
      }
    }
    if (failures.length() > 0) {
      throw new IOException(failures.toString());
    }
  }
}
