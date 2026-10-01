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
import java.util.Map;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Visibility label operations. Named distinctly from the existing {@code VisibilityOps} helper
 * class ({@link DefaultShellAdmin} delegates {@code setVisibility} to its static methods) to avoid
 * a same-package name collision.
 */
@InterfaceAudience.Private
public interface VisibilityLabelContract {
  void addLabels(List<String> labels) throws IOException;

  List<String> listLabels(String regex) throws IOException;

  void setAuths(String user, List<String> labels) throws IOException;

  List<String> getAuths(String user) throws IOException;

  void clearAuths(String user, List<String> labels) throws IOException;

  long setVisibility(String tableName, String visibility, Map<String, Object> scanOptions)
    throws IOException;
}
