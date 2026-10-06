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

import java.io.IOException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Double-dispatch over the closed set of {@link CommandResult} shapes, so a renderer that misses a
 * shape fails to compile instead of falling through an {@code instanceof} chain at runtime.
 */
@InterfaceAudience.Private
public interface ResultVisitor {
  void visit(TextResult result) throws IOException;

  void visit(TabularResult result) throws IOException;

  void visit(StreamingTabularResult result) throws IOException;
}
