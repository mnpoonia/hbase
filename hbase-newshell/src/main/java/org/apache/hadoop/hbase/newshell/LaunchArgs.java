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
package org.apache.hadoop.hbase.newshell;

import org.apache.hadoop.hbase.newshell.format.OutputFormat;
import org.apache.yetus.audience.InterfaceAudience;

/** Parsed argv for {@link NewShellMain#main}. Visible for tests. */
@InterfaceAudience.Private
final class LaunchArgs {
  final boolean exitOnFirstError;
  final boolean forceYes;
  final boolean verbose;
  final boolean quiet;
  final OutputFormat outputFormat;
  final String scriptFile;

  LaunchArgs(boolean exitOnFirstError, boolean forceYes, boolean verbose, boolean quiet,
    OutputFormat outputFormat, String scriptFile) {
    this.exitOnFirstError = exitOnFirstError;
    this.forceYes = forceYes;
    this.verbose = verbose;
    this.quiet = quiet;
    this.outputFormat = outputFormat;
    this.scriptFile = scriptFile;
  }

  static LaunchArgs parse(String[] args) {
    boolean exitOnFirstError = false;
    boolean forceYes = false;
    boolean verbose = false;
    boolean quiet = false;
    OutputFormat outputFormat = OutputFormat.TEXT;
    String scriptFile = null;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("-n") || arg.equals("--noninteractive")) {
        exitOnFirstError = true;
      } else if (arg.equals("-y") || arg.equals("--yes")) {
        forceYes = true;
      } else if (
        arg.equals("-v") || arg.equals("--verbose") || arg.equals("-d") || arg.equals("--debug")
      ) {
        // -d/--debug is the legacy shell spelling.
        verbose = true;
      } else if (arg.equals("-q") || arg.equals("--quiet")) {
        quiet = true;
      } else if (arg.equals("-o") || arg.equals("--output")) {
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException(arg + " requires a value (text|json|csv)");
        }
        outputFormat = OutputFormat.parse(args[++i]);
      } else if (arg.startsWith("--output=")) {
        outputFormat = OutputFormat.parse(arg.substring("--output=".length()));
      } else if (arg.startsWith("-o=") || arg.startsWith("--o=")) {
        outputFormat = OutputFormat.parse(arg.substring(arg.indexOf('=') + 1));
      } else if (scriptFile == null && !arg.startsWith("-")) {
        scriptFile = arg;
      } else if (arg.startsWith("-")) {
        throw new IllegalArgumentException("Unknown option: " + arg);
      }
    }
    return new LaunchArgs(exitOnFirstError, forceYes, verbose, quiet, outputFormat, scriptFile);
  }
}
