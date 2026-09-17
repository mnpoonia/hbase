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
import java.util.Map;

/**
 * Every method throws {@link UnsupportedOperationException} by default. Command-under-test
 * fakes extend this and override only the method(s) their command actually calls, instead of
 * every test re-declaring all of {@link ShellTable}'s methods.
 */
public class StubShellTable implements ShellTable {
  private static UnsupportedOperationException notNeeded() {
    return new UnsupportedOperationException("not needed for this test");
  }

  @Override
  public GetResult get(String row, Map<String, Object> options) {
    throw notNeeded();
  }

  @Override
  public void put(String row, String column, String value, Map<String, Object> options) {
    throw notNeeded();
  }

  @Override
  public ScanResult scan(Map<String, Object> options) {
    throw notNeeded();
  }

  @Override
  public long count(Map<String, Object> options) {
    throw notNeeded();
  }

  @Override
  public void delete(String row, String column, Long timestamp) {
    throw notNeeded();
  }

  @Override
  public void deleteAll(String row, String column, Long timestamp, Map<String, Object> options) {
    throw notNeeded();
  }

  @Override
  public Long getCounter(String row, String column) {
    throw notNeeded();
  }

  @Override
  public Long increment(String row, String column, long amount) {
    throw notNeeded();
  }

  @Override
  public String append(String row, String column, String value) {
    throw notNeeded();
  }

  @Override
  public List<String> getSplits() {
    throw notNeeded();
  }
}
