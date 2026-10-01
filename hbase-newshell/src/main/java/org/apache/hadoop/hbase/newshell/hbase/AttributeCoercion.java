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

import org.apache.yetus.audience.InterfaceAudience;

/**
 * Coerces the loosely-typed attribute values a shell command receives (numeric or numeric-string,
 * per hbase-shell compatibility) into the primitive type a descriptor setter expects. Shared by
 * {@link TableAttributes} and {@link ColumnFamilyAttributes}.
 */
@InterfaceAudience.Private
final class AttributeCoercion {
  private AttributeCoercion() {
  }

  static int toInt(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return Integer.parseInt(requireNonNull(value).toString());
  }

  static long toLong(Object value) {
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return Long.parseLong(requireNonNull(value).toString());
  }

  static boolean toBoolean(Object value) {
    return Boolean.parseBoolean(requireNonNull(value).toString());
  }

  // Rewraps a null value as IllegalArgumentException so callers' existing
  // catch (IllegalArgumentException | ClassCastException) blocks turn it into a shell-friendly
  // "Invalid value" message instead of letting a raw NullPointerException escape.
  private static Object requireNonNull(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("value is null");
    }
    return value;
  }
}
