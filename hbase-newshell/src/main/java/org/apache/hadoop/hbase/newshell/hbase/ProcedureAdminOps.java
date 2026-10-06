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

import org.apache.hbase.thirdparty.com.google.gson.Gson;
import org.apache.hbase.thirdparty.com.google.gson.JsonArray;
import org.apache.hbase.thirdparty.com.google.gson.JsonElement;
import org.apache.hbase.thirdparty.com.google.gson.JsonObject;
import org.apache.hbase.thirdparty.com.google.gson.JsonParser;

/** Package-private collaborator used by {@link DefaultShellAdmin}. */
final class ProcedureAdminOps implements ProcedureAdminContract {
  private final org.apache.hadoop.hbase.client.Admin admin;

  ProcedureAdminOps(org.apache.hadoop.hbase.client.Admin admin) {
    this.admin = admin;
  }

  private static final Gson GSON = new Gson();

  public List<List<String>> listProcedures() throws IOException {
    JsonArray procedures = JsonParser.parseString(admin.getProcedures()).getAsJsonArray();
    List<List<String>> rows = new ArrayList<>();
    for (JsonElement element : procedures) {
      JsonObject proc = element.getAsJsonObject();
      rows.add(Arrays.asList(getAsString(proc, "procId"), getAsString(proc, "className"),
        getAsString(proc, "state"), getAsString(proc, "submittedTime"),
        getAsString(proc, "lastUpdate"), getAsString(proc, "stateMessage")));
    }
    return rows;
  }

  private static String getAsString(JsonObject object, String member) {
    if (!object.has(member) || object.get(member).isJsonNull()) {
      return "";
    }
    JsonElement element = object.get(member);
    return element.isJsonPrimitive() ? element.getAsString() : element.toString();
  }

  public List<String> listLocks() throws IOException {
    JsonArray locks = JsonParser.parseString(admin.getLocks()).getAsJsonArray();
    List<String> lines = new ArrayList<>();
    for (JsonElement element : locks) {
      JsonObject lock = element.getAsJsonObject();
      lines.add(getAsString(lock, "resourceType") + "(" + getAsString(lock, "resourceName") + ")");
      String lockType = getAsString(lock, "lockType");
      if ("EXCLUSIVE".equals(lockType)) {
        lines.add("Lock type: " + lockType + ", procedure: "
          + getAsString(lock, "exclusiveLockOwnerProcedure"));
      } else if ("SHARED".equals(lockType)) {
        lines.add("Lock type: " + lockType + ", count: " + getAsString(lock, "sharedLockCount"));
      }
      if (lock.has("waitingProcedures") && lock.get("waitingProcedures").isJsonArray()) {
        for (JsonElement waiting : lock.getAsJsonArray("waitingProcedures")) {
          lines.add("    " + waiting.getAsString());
        }
      }
      lines.add("");
    }
    return lines;
  }
}
