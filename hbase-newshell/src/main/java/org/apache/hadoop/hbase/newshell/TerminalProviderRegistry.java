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

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.newshell.spi.NoAvailableTerminalProviderException;
import org.apache.hadoop.hbase.newshell.spi.TerminalProvider;
import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link TerminalProvider} newshell should use, via {@link ServiceLoader}. This
 * class is the only place in newshell's core that reasons about "which backend" - and it never
 * names one; it only asks the classpath what's there. Which concrete backend module is
 * actually present is decided at build/packaging time.
 */
@InterfaceAudience.Private
public final class TerminalProviderRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(TerminalProviderRegistry.class);

  public static final String PROVIDER_OVERRIDE_PROPERTY = "hbase.newshell.terminal.provider";

  private final List<TerminalProvider> providers;

  public TerminalProviderRegistry() {
    this(ServiceLoader.load(TerminalProvider.class));
  }

  TerminalProviderRegistry(ServiceLoader<TerminalProvider> serviceLoader) {
    this(serviceLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList()));
  }

  TerminalProviderRegistry(List<TerminalProvider> providers) {
    this.providers = providers;
  }

  /**
   * Resolves a single provider to use: honors the {@value #PROVIDER_OVERRIDE_PROPERTY} system
   * property if set, otherwise picks the highest-{@link TerminalProvider#priority()} provider
   * that reports {@link TerminalProvider#isAvailable()}.
   * @throws NoAvailableTerminalProviderException if no suitable provider could be resolved
   */
  public TerminalProvider resolve() {
    String override = System.getProperty(PROVIDER_OVERRIDE_PROPERTY);
    TerminalProvider resolved;
    if (override != null && !override.isEmpty()) {
      resolved = providers.stream().filter(p -> p.name().equals(override)).filter(TerminalProvider::isAvailable)
        .findFirst()
        .orElseThrow(() -> new NoAvailableTerminalProviderException("No available TerminalProvider named '"
          + override + "' (requested via -D" + PROVIDER_OVERRIDE_PROPERTY + "). Providers found: "
          + describeProviders()));
    } else {
      resolved = providers.stream().filter(TerminalProvider::isAvailable)
        .max(Comparator.comparingInt(TerminalProvider::priority))
        .orElseThrow(() -> new NoAvailableTerminalProviderException(
          "No available TerminalProvider found on the classpath. Providers found: " + describeProviders()));
    }
    LOG.info("Resolved newshell terminal provider: {}", resolved.name());
    return resolved;
  }

  private String describeProviders() {
    if (providers.isEmpty()) {
      return "[]";
    }
    return providers.stream()
      .map(p -> p.name() + "(available=" + p.isAvailable() + ", priority=" + p.priority() + ")")
      .collect(Collectors.joining(", ", "[", "]"));
  }
}
