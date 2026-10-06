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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * Resolves the legacy shell's {@code FORMATTER}/{@code FORMATTER_CLASS} and per-column
 * {@code cf:qualifier:CONVERTER} specs: a converter is a public static method taking a single
 * {@code byte[]}, either a {@link Bytes} method name ({@code toInt}) or {@code c(Class).method}.
 */
@InterfaceAudience.Private
final class ValueConverters {
  static final String DEFAULT_CLASS = Bytes.class.getName();
  static final String DEFAULT_METHOD = "toStringBinary";

  private static final Pattern CUSTOM = Pattern.compile("^c\\((.+)\\)\\.(.+)$");

  private final String defaultClass;
  private final String defaultMethod;

  ValueConverters(String formatterClass, String formatter) {
    this.defaultClass = formatterClass == null ? DEFAULT_CLASS : formatterClass;
    this.defaultMethod = formatter == null ? DEFAULT_METHOD : formatter;
  }

  /**
   * True when {@code expr} can be a converter: {@code c(Class).method} or a {@link Bytes} method.
   */
  static boolean isConverter(String expr) {
    if (CUSTOM.matcher(expr).matches()) {
      return true;
    }
    return findMethod(DEFAULT_CLASS, expr) != null;
  }

  /** Applies the default (global) converter. */
  String convert(byte[] bytes) throws ShellCommandException {
    return invoke(defaultClass, defaultMethod, bytes);
  }

  /** Row keys use the default converter class with the global FORMATTER method, as in legacy. */
  String convertRow(byte[] row) throws ShellCommandException {
    return invoke(DEFAULT_CLASS, defaultMethod, row);
  }

  /** Applies a per-column converter spec, or the default when {@code spec} is null. */
  String convert(byte[] bytes, String spec) throws ShellCommandException {
    if (spec == null) {
      return convert(bytes);
    }
    Matcher m = CUSTOM.matcher(spec);
    if (m.matches()) {
      return invoke(m.group(1), m.group(2), bytes);
    }
    return invoke(defaultClass, spec, bytes);
  }

  private static String invoke(String className, String methodName, byte[] bytes)
    throws ShellCommandException {
    Method method = findMethod(className, methodName);
    if (method == null) {
      throw new ShellCommandException(
        "No static method " + methodName + "(byte[]) in " + className + " for FORMATTER");
    }
    try {
      return String.valueOf(method.invoke(null, (Object) bytes));
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new ShellCommandException(
        "FORMATTER " + className + "." + methodName + " failed: " + cause.getMessage(), cause);
    } catch (IllegalAccessException e) {
      throw new ShellCommandException(
        "FORMATTER " + className + "." + methodName + " is not accessible", e);
    }
  }

  private static Method findMethod(String className, String methodName) {
    try {
      Method m = Class.forName(className).getMethod(methodName, byte[].class);
      return Modifier.isStatic(m.getModifiers()) ? m : null;
    } catch (ReflectiveOperationException | LinkageError e) {
      return null;
    }
  }
}
