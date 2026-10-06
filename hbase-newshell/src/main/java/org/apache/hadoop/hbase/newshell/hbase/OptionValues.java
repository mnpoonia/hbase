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

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.ParseFilter;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Typed, validating readers for the untyped {@code KEY => value} option maps of the table commands.
 * Every reader fails with a {@link ShellCommandException} naming the offending option rather than
 * leaking a {@link ClassCastException}, and the {@code opt*} readers return {@code null} when the
 * option is absent.
 */
@InterfaceAudience.Private
final class OptionValues {
  private OptionValues() {
  }

  /** A numeric option value (e.g. {@code VERSIONS => 'x'}, a typo, is rejected). */
  static Number requireNumber(Object value, String optionName) throws ShellCommandException {
    if (!(value instanceof Number)) {
      throw new ShellCommandException(optionName + " must be numeric: '" + value + "'");
    }
    return (Number) value;
  }

  /** Strict boolean: a Boolean, or the string true/false (case-insensitive). */
  static boolean requireBoolean(Object value, String optionName) throws ShellCommandException {
    try {
      return AttributeCoercion.toBoolean(value);
    } catch (IllegalArgumentException e) {
      throw new ShellCommandException(optionName + " must be true or false: '" + value + "'", e);
    }
  }

  static <E extends Enum<E>> E enumValue(Class<E> type, Object value, String optionName)
    throws ShellCommandException {
    try {
      return Enum.valueOf(type, value.toString());
    } catch (IllegalArgumentException e) {
      throw new ShellCommandException(
        optionName + " must be one of " + Arrays.toString(type.getEnumConstants()), e);
    }
  }

  static Integer optInt(Map<String, Object> options, String name) throws ShellCommandException {
    Object value = options.get(name);
    return value == null ? null : requireNumber(value, name).intValue();
  }

  static Long optLong(Map<String, Object> options, String name) throws ShellCommandException {
    Object value = options.get(name);
    return value == null ? null : requireNumber(value, name).longValue();
  }

  /** Like {@link #optInt} but a zero or negative value counts as absent, as in the legacy shell. */
  static Integer optPositiveInt(Map<String, Object> options, String name)
    throws ShellCommandException {
    Integer value = optInt(options, name);
    return value == null || value <= 0 ? null : value;
  }

  static Long optPositiveLong(Map<String, Object> options, String name)
    throws ShellCommandException {
    Long value = optLong(options, name);
    return value == null || value <= 0 ? null : value;
  }

  static Boolean optBoolean(Map<String, Object> options, String name) throws ShellCommandException {
    Object value = options.get(name);
    return value == null ? null : requireBoolean(value, name);
  }

  static <E extends Enum<E>> E optEnum(Map<String, Object> options, String name, Class<E> type)
    throws ShellCommandException {
    Object value = options.get(name);
    return value == null ? null : enumValue(type, value, name);
  }

  static String optString(Map<String, Object> options, String name) {
    Object value = options.get(name);
    return value == null ? null : value.toString();
  }

  /**
   * Parses a {@code TIMERANGE => [minStamp, maxStamp]} option value into a {@code [min, max]}
   * {@code long} pair, matching old-shell's plain-array {@code TIMERANGE} semantics.
   */
  static long[] parseTimeRange(Object timerangeOption) throws ShellCommandException {
    if (!(timerangeOption instanceof List)) {
      throw new ShellCommandException(
        "TIMERANGE must be a two-element array of [minStamp, maxStamp]");
    }
    List<?> range = (List<?>) timerangeOption;
    if (range.size() != 2) {
      throw new ShellCommandException(
        "TIMERANGE must be a two-element array of [minStamp, maxStamp], got " + range.size()
          + " element(s)");
    }
    return new long[] { requireNumber(range.get(0), "TIMERANGE").longValue(),
      requireNumber(range.get(1), "TIMERANGE").longValue() };
  }

  /**
   * Parses a {@code FILTER => "<filter-string>"} option value using {@link ParseFilter}'s textual
   * filter grammar (HBASE-4176), e.g. {@code "PrefixFilter('row')"}. Raw object-construction syntax
   * ({@code FILTER => SomeFilter.new(...)}) is not supported.
   */
  static Filter parseFilter(Object filterOption) throws ShellCommandException {
    if (!(filterOption instanceof String)) {
      throw new ShellCommandException(
        "FILTER must be a filter string such as \"PrefixFilter('row')\", got: " + filterOption);
    }
    String filterString = (String) filterOption;
    try {
      return new ParseFilter().parseFilterString(filterString);
    } catch (CharacterCodingException | IllegalArgumentException e) {
      throw new ShellCommandException(
        "Invalid FILTER string '" + filterString + "': " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  static List<Object> asList(Object value) {
    if (value instanceof List) {
      return (List<Object>) value;
    }
    return Arrays.asList(value);
  }
}
