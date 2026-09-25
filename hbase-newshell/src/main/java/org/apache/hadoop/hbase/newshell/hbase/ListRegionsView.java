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

import java.util.List;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Rows plus optional stdout warnings for {@code list_regions}, matching the Ruby shell's
 * "Can not find all details for region: …" messages when region metrics are missing.
 */
@InterfaceAudience.Private
public record ListRegionsView(List<String> warnings, List<List<String>> rows) {
}
