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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hbase.newshell.spi.NoAvailableTerminalProviderException;
import org.apache.hadoop.hbase.newshell.spi.ShellTerminal;
import org.apache.hadoop.hbase.newshell.spi.TerminalConfig;
import org.apache.hadoop.hbase.newshell.spi.TerminalProvider;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class TerminalProviderRegistryTest {

  private static final class FakeProvider implements TerminalProvider {
    private final String name;
    private final int priority;
    private final boolean available;

    FakeProvider(String name, int priority, boolean available) {
      this.name = name;
      this.priority = priority;
      this.available = available;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public int priority() {
      return priority;
    }

    @Override
    public boolean isAvailable() {
      return available;
    }

    @Override
    public ShellTerminal open(TerminalConfig config) throws IOException {
      throw new UnsupportedOperationException("not needed for this test");
    }
  }

  @AfterEach
  public void clearOverride() {
    System.clearProperty(TerminalProviderRegistry.PROVIDER_OVERRIDE_PROPERTY);
  }

  @Test
  public void picksHighestPriorityAvailableProvider() {
    TerminalProviderRegistry registry = new TerminalProviderRegistry(
      List.of(new FakeProvider("low", 1, true), new FakeProvider("high", 100, true)));
    assertEquals("high", registry.resolve().name());
  }

  @Test
  public void skipsUnavailableProviders() {
    TerminalProviderRegistry registry = new TerminalProviderRegistry(
      List.of(new FakeProvider("unavailable-high", 100, false), new FakeProvider("available-low", 1, true)));
    assertEquals("available-low", registry.resolve().name());
  }

  @Test
  public void honorsExplicitOverrideProperty() {
    System.setProperty(TerminalProviderRegistry.PROVIDER_OVERRIDE_PROPERTY, "low");
    TerminalProviderRegistry registry = new TerminalProviderRegistry(
      List.of(new FakeProvider("low", 1, true), new FakeProvider("high", 100, true)));
    assertEquals("low", registry.resolve().name());
  }

  @Test
  public void throwsWhenOverrideNamesUnavailableProvider() {
    System.setProperty(TerminalProviderRegistry.PROVIDER_OVERRIDE_PROPERTY, "missing");
    TerminalProviderRegistry registry =
      new TerminalProviderRegistry(List.of(new FakeProvider("high", 100, true)));
    assertThrows(NoAvailableTerminalProviderException.class, registry::resolve);
  }

  @Test
  public void throwsWhenNoProvidersAvailable() {
    TerminalProviderRegistry registry =
      new TerminalProviderRegistry(List.of(new FakeProvider("high", 100, false)));
    assertThrows(NoAvailableTerminalProviderException.class, registry::resolve);
  }
}
