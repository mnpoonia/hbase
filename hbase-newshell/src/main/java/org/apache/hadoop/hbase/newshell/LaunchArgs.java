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

import java.util.LinkedHashMap;
import java.util.Map;
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
  final boolean help;
  /** {@code -Dkey=value} client configuration overrides, in command-line order. */
  final Map<String, String> properties;

  static final String USAGE = "Usage: newshell [options] [script-file]\n"
    + "  -n, --noninteractive   run a script, exiting on the first error\n"
    + "  -y, --yes              answer yes to confirmation prompts\n"
    + "  -v, --verbose, -d      print stack traces on errors\n"
    + "  -q, --quiet            suppress error output\n"
    + "  -o, --output FORMAT    text (default), json or csv\n"
    + "  -Dkey=value            set a client configuration property\n"
    + "  -h, --help             show this help";

  LaunchArgs(boolean exitOnFirstError, boolean forceYes, boolean verbose, boolean quiet,
    OutputFormat outputFormat, String scriptFile, boolean help, Map<String, String> properties) {
    this.help = help;
    this.properties = properties;
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
    boolean help = false;
    Map<String, String> properties = new LinkedHashMap<>();
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
      } else if (
        arg.equals("-c") || arg.equals("--colorize") || arg.equals("-a")
          || arg.equals("--autocomplete") || arg.equals("--top-level-defs")
      ) {
        // Legacy shell spellings for IRB cosmetics; accepted so existing launch scripts keep
        // working.
      } else if (arg.equals("-h") || arg.equals("--help")) {
        help = true;
      } else if (arg.startsWith("-D")) {
        int eq = arg.indexOf('=');
        if (eq <= 2) {
          throw new IllegalArgumentException(arg + " must be of the form -Dkey=value");
        }
        properties.put(arg.substring(2, eq), arg.substring(eq + 1));
      } else if (arg.startsWith("-")) {
        throw new IllegalArgumentException("Unknown option: " + arg);
      } else if (scriptFile == null) {
        scriptFile = arg;
      } else {
        throw new IllegalArgumentException(
          "Only one script file is supported, got '" + scriptFile + "' and '" + arg + "'");
      }
    }
    return new LaunchArgs(exitOnFirstError, forceYes, verbose, quiet, outputFormat, scriptFile,
      help, properties);
  }
}
