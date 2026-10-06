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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import org.apache.hadoop.hbase.newshell.command.CommandRegistry;
import org.apache.hadoop.hbase.newshell.command.CommandResult;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ExitCodes;
import org.apache.hadoop.hbase.newshell.command.ShellCommand;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.format.DefaultFormatter;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ParsedCommand;
import org.apache.hadoop.hbase.newshell.spi.Completer;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class NewShellMainTest {

  /** Minimal in-memory {@link ShellTerminal} fake - no real terminal/JLine involved. */
  private static final class FakeShellTerminal implements ShellTerminal {
    private final Deque<String> lines;
    private final StringWriter buffer = new StringWriter();
    private final PrintWriter writer = new PrintWriter(buffer);

    FakeShellTerminal(String... lines) {
      this.lines = new ArrayDeque<>(Arrays.asList(lines));
    }

    @Override
    public String readLine(String prompt) {
      return lines.poll();
    }

    @Override
    public PrintWriter writer() {
      return writer;
    }

    @Override
    public void setCompleter(Completer completer) {
      // not exercised by this test
    }

    @Override
    public void close() {
    }

    String output() {
      writer.flush();
      return buffer.toString();
    }
  }

  private static final class SucceedingCommand implements ShellCommand {
    @Override
    public String name() {
      return "hello";
    }

    @Override
    public String help() {
      return "hello";
    }

    @Override
    public CommandResult execute(ParsedCommand command, ExecutionContext context) {
      return TextResult.of("hello world");
    }
  }

  private static final class FailingCommand implements ShellCommand {
    @Override
    public String name() {
      return "boom";
    }

    @Override
    public String help() {
      return "boom";
    }

    @Override
    public CommandResult execute(ParsedCommand command, ExecutionContext context)
      throws ShellCommandException {
      throw new ShellCommandException("kaboom");
    }
  }

  /** Throws an unchecked wrapper around an IOException, as streaming output does. */
  private static final class WrappedIoCommand implements ShellCommand {
    @Override
    public String name() {
      return "wrapped";
    }

    @Override
    public String help() {
      return "wrapped";
    }

    @Override
    public CommandResult execute(ParsedCommand command, ExecutionContext context) {
      throw new IllegalStateException("stream failed", new IOException("rpc down"));
    }
  }

  /**
   * Throws an unchecked exception, to verify the dispatch loop's RuntimeException trust boundary.
   */
  private static final class UncheckedThrowingCommand implements ShellCommand {
    @Override
    public String name() {
      return "boom";
    }

    @Override
    public String help() {
      return "boom";
    }

    @Override
    public CommandResult execute(ParsedCommand command, ExecutionContext context) {
      throw new IllegalArgumentException("unchecked boom");
    }
  }

  /** Throws a {@link ShellCommandException} with a null message. */
  private static final class NullMessageFailingCommand implements ShellCommand {
    @Override
    public String name() {
      return "boom";
    }

    @Override
    public String help() {
      return "boom";
    }

    @Override
    public CommandResult execute(ParsedCommand command, ExecutionContext context)
      throws ShellCommandException {
      throw new ShellCommandException(null);
    }
  }

  private ExecutionContext newContext(PrintWriter out) {
    return new ExecutionContext(new StubShellAdmin(), new StubShellTableFactory(), out);
  }

  @Test
  public void stopsCleanlyOnEndOfInput() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal();
    CommandRegistry registry = new CommandRegistry(java.util.Collections.emptyList());
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertEquals("", terminal.output());
  }

  @Test
  public void exitAndQuitAreCaseInsensitiveAndStopTheLoop() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("QUIT", "hello");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertEquals("", terminal.output());
  }

  @Test
  public void exitWithStatusReturnsThatStatus() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("exit 3", "hello");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    int code =
      ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertEquals(3, code);
    assertEquals("", terminal.output());
  }

  @Test
  public void exitWithNonNumericStatusIsAnErrorAndDoesNotExit() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("quit abc", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertTrue(terminal.output().contains("exit status must be an integer"));
    assertTrue(terminal.output().contains("hello world"));
  }

  @Test
  public void dispatchesRecognizedCommandThroughFormatter() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertTrue(terminal.output().contains("hello world"));
  }

  @Test
  public void printsErrorForUnknownCommandAndContinues() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("nope", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    String output = terminal.output();
    assertTrue(output.contains("ERROR: unknown command 'nope'"));
    assertTrue(output.contains("hello world"));
  }

  @Test
  public void printsErrorForParseFailureAndContinues() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("get @bad", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    String output = terminal.output();
    assertTrue(output.contains("ERROR:"));
    assertTrue(output.contains("hello world"));
  }

  @Test
  public void printsErrorForCommandFailureAndContinues() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("boom", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand(), new FailingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    String output = terminal.output();
    assertTrue(output.contains("ERROR: kaboom"));
    assertTrue(output.contains("hello world"));
  }

  @Test
  public void exitOnFirstErrorFalseContinuesPastFailure() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("boom", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand(), new FailingCommand()));
    int code = ShellRepl.run(terminal, newContext(terminal.writer()), registry,
      new DefaultFormatter(), false);
    assertEquals(ExitCodes.SUCCESS, code);
    assertTrue(terminal.output().contains("hello world"));
  }

  @Test
  public void exitOnFirstErrorTrueStopsAtFirstFailure() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("boom", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand(), new FailingCommand()));
    int code = ShellRepl.run(terminal, newContext(terminal.writer()), registry,
      new DefaultFormatter(), true);
    assertEquals(ExitCodes.CLIENT_ERROR, code);
    assertFalse(terminal.output().contains("hello world"));
  }

  @Test
  public void exitOnFirstErrorTrueSucceedsWhenNoFailureOccurs() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    int code = ShellRepl.run(terminal, newContext(terminal.writer()), registry,
      new DefaultFormatter(), true);
    assertEquals(ExitCodes.SUCCESS, code);
  }

  @Test
  public void runtimeExceptionFromCommandDoesNotKillSession() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("boom", "hello", "exit");
    CommandRegistry registry = new CommandRegistry(
      java.util.Arrays.asList(new SucceedingCommand(), new UncheckedThrowingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    String output = terminal.output();
    assertTrue(output.contains("unchecked boom"));
    assertTrue(output.contains("hello world"));
  }

  @Test
  public void uncheckedWrapperAroundIoFailureMapsToServerError() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("wrapped", "hello", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand(), new WrappedIoCommand()));
    int code = ShellRepl.run(terminal, newContext(terminal.writer()), registry,
      new DefaultFormatter(), true);
    assertEquals(ExitCodes.SERVER_ERROR, code);
    assertTrue(terminal.output().contains("stream failed"));
    assertFalse(terminal.output().contains("hello world"));
  }

  @Test
  public void exitOnFirstErrorPropagatesRuntimeFailure() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("boom", "hello", "exit");
    CommandRegistry registry = new CommandRegistry(
      java.util.Arrays.asList(new SucceedingCommand(), new UncheckedThrowingCommand()));
    int code = ShellRepl.run(terminal, newContext(terminal.writer()), registry,
      new DefaultFormatter(), true);
    assertEquals(ExitCodes.CLIENT_ERROR, code);
    assertFalse(terminal.output().contains("hello world"));
  }

  @Test
  public void nullMessageShellCommandExceptionFallsBackToClassName() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("boom", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new NullMessageFailingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    String output = terminal.output();
    assertTrue(output.contains("ShellCommandException"));
    assertFalse(output.contains("ERROR: null"));
  }

  @Test
  public void skipsCommentLines() throws IOException {
    FakeShellTerminal terminal =
      new FakeShellTerminal("# this is a comment", "  # indented too", "exit");
    CommandRegistry registry = new CommandRegistry(java.util.Collections.emptyList());
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertFalse(terminal.output().contains("ERROR"));
  }

  @Test
  public void launchArgsParsesOutputAndYes() {
    LaunchArgs args =
      LaunchArgs.parse(new String[] { "-n", "--yes", "--output", "json", "script.ns" });
    assertTrue(args.exitOnFirstError);
    assertTrue(args.forceYes);
    assertEquals(org.apache.hadoop.hbase.newshell.format.OutputFormat.JSON, args.outputFormat);
    assertEquals("script.ns", args.scriptFile);
  }

  @Test
  public void launchArgsParsesHelpAndDefines() {
    LaunchArgs args = LaunchArgs.parse(new String[] { "-h", "-Dhbase.zookeeper.quorum=zk1" });
    assertTrue(args.help);
    assertEquals("zk1", args.properties.get("hbase.zookeeper.quorum"));
  }

  @Test
  public void launchArgsRejectsSecondScriptFileAndBadDefine() {
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
      () -> LaunchArgs.parse(new String[] { "a.ns", "b.ns" }));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
      () -> LaunchArgs.parse(new String[] { "-Dnovalue" }));
    // legacy no-op flag of the JRuby shell; deliberately rejected rather than silently ignored
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
      () -> LaunchArgs.parse(new String[] { "-r" }));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
      () -> LaunchArgs.parse(new String[] { "--return-values" }));
  }

  @Test
  public void isIncompleteDetectsOpenQuotesBracketsAndTrailingComma() {
    assertTrue(ShellRepl.isIncomplete("create 't', {NAME => 'f',"));
    assertTrue(ShellRepl.isIncomplete("put 't', 'r', 'f:c', 'multi"));
    assertTrue(ShellRepl.isIncomplete("get 't', 'r',"));
    assertFalse(ShellRepl.isIncomplete("get 't', 'r' # trailing, comment ["));
    assertFalse(ShellRepl.isIncomplete("get 't', {COLUMNS => ['f:a']}"));
    assertFalse(ShellRepl.isIncomplete("put 't', 'r', 'f:c', 'it\\'s'"));
  }

  @Test
  public void multiLineCommandIsJoinedBeforeDispatch() throws IOException {
    FakeShellTerminal terminal = new FakeShellTerminal("hello \"a,", "b\"", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertFalse(terminal.output().contains("ERROR"));
  }

  @Test
  public void trailingBackslashContinuesTheCommandOntoTheNextLine() throws IOException {
    assertTrue(ShellRepl.isIncomplete("get 't', \\"));
    assertTrue(ShellRepl.endsWithContinuation("get 't', \\  "));
    assertFalse(ShellRepl.endsWithContinuation("put 't', 'r', 'f:c', 'C:\\\\"));
    assertFalse(ShellRepl.endsWithContinuation("get 't'"));
    assertEquals("get 't', ", ShellRepl.stripContinuation("get 't', \\  "));

    FakeShellTerminal terminal = new FakeShellTerminal("hello \\", "world", "exit");
    CommandRegistry registry =
      new CommandRegistry(java.util.Arrays.asList(new SucceedingCommand()));
    ShellRepl.run(terminal, newContext(terminal.writer()), registry, new DefaultFormatter());
    assertFalse(terminal.output().contains("ERROR"));
  }

  @Test
  public void streamTerminalReadsLinesFromTheGivenStreamWithoutPrompting() throws IOException {
    try (FileScriptTerminal terminal = new FileScriptTerminal(new java.io.ByteArrayInputStream(
      "one\ntwo\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
      assertEquals("one", terminal.readLine("ignored> "));
      assertEquals("two", terminal.readLine("ignored> "));
      assertNull(terminal.readLine("ignored> "));
    }
  }

  @Test
  public void hasTerminalIsFalseWithoutAConsole() {
    // Surefire runs with redirected stdin/stdout, so there is no terminal on any Java version.
    assertFalse(NewShellMain.hasTerminal());
  }

  @Test
  public void launchArgsAcceptsLegacyDebugFlag() {
    assertTrue(LaunchArgs.parse(new String[] { "-d" }).verbose);
    assertTrue(LaunchArgs.parse(new String[] { "--debug" }).verbose);
  }

  @Test
  public void sessionIsInteractiveOnlyOnARealTerminal() {
    LaunchArgs plain = LaunchArgs.parse(new String[0]);
    assertTrue(NewShellMain.isInteractive(plain, true));
    assertFalse(NewShellMain.isInteractive(plain, false));
    assertFalse(NewShellMain.isInteractive(LaunchArgs.parse(new String[] { "-n" }), true));
    assertFalse(NewShellMain.isInteractive(LaunchArgs.parse(new String[] { "f.ns" }), true));
  }
}
