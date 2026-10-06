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

  /** An int, rejecting fractional values and values outside the int range instead of truncating. */
  static int toInt(Object value) {
    long result = toLong(value);
    if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Value out of int range: " + value);
    }
    return (int) result;
  }

  /** A long, rejecting fractional values instead of truncating them. */
  static long toLong(Object value) {
    if (value instanceof Number) {
      Number number = (Number) value;
      if (number instanceof Double || number instanceof Float) {
        double d = number.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d)) {
          throw new IllegalArgumentException("Value must be a whole number: " + value);
        }
      } else if (number instanceof java.math.BigDecimal) {
        try {
          return ((java.math.BigDecimal) number).longValueExact();
        } catch (ArithmeticException e) {
          throw new IllegalArgumentException("Value must be a whole number in range: " + value);
        }
      } else if (number instanceof java.math.BigInteger) {
        try {
          return ((java.math.BigInteger) number).longValueExact();
        } catch (ArithmeticException e) {
          throw new IllegalArgumentException("Value out of long range: " + value);
        }
      }
      return number.longValue();
    }
    return Long.parseLong(requireNonNull(value).toString());
  }

  /**
   * Strict boolean coercion: accepts a {@link Boolean} or the string {@code true}/{@code false}
   * (case-insensitive). Anything else, e.g. a typo such as {@code 'ture'}, is rejected rather than
   * silently becoming {@code false}.
   */
  static boolean toBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    String text = requireNonNull(value).toString();
    if (text.equalsIgnoreCase("true")) {
      return true;
    }
    if (text.equalsIgnoreCase("false")) {
      return false;
    }
    throw new IllegalArgumentException("not a boolean: '" + text + "'");
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
