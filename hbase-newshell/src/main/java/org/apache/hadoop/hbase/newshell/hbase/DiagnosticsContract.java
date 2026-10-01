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

/** Cluster diagnostics: slow-log responses, balancer decisions/rejections, ZooKeeper dump. */
@InterfaceAudience.Private
public interface DiagnosticsContract {
  /** Returns summary message of how many RegionServers were cleared */
  String clearSlowLogResponses(List<String> serverNames) throws IOException;

  List<String> getBalancerDecisions(Map<String, Object> args) throws IOException;

  List<String> getBalancerRejections(Map<String, Object> args) throws IOException;

  /**
   * @param serverNames {@code null} or empty with {@code allServers=true} meaning all live RS;
   *                    otherwise host/port/startcode strings (or a single {@code "*"})
   */
  List<String> getSlowLogResponses(List<String> serverNames, Map<String, Object> args,
    boolean largeLog) throws IOException;

  String zkDump() throws IOException;
}
