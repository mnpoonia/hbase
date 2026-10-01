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

/**
 * {@link NamespaceAdminContract} with every method defaulted to throw
 * {@link UnsupportedOperationException}. Command-under-test fakes implement this (directly or
 * via {@link StubShellAdmin}) and override only the method(s) their command actually calls.
 */
public interface StubNamespaceAdminContract extends NamespaceAdminContract {
  @Override
  default void createNamespace(String namespace, Map<String, Object> properties)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void dropNamespace(String namespace) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default void alterNamespace(String namespace, Map<String, Object> properties)
    throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String describeNamespace(String namespace) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<String> listNamespaces(String regex) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default List<String> listNamespaceTables(String namespace) throws IOException {
    throw StubContractSupport.notNeeded();
  }

  @Override
  default String getNamespaceRsGroup(String namespace) throws IOException {
    throw StubContractSupport.notNeeded();
  }
}
