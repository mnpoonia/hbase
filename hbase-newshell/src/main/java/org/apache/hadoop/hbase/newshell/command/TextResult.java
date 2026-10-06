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
package org.apache.hadoop.hbase.newshell.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.yetus.audience.InterfaceAudience;

/** Pre-formatted lines of text - e.g. {@code status}'s summary or {@code create}'s confirmation. */
@InterfaceAudience.Private
public final class TextResult implements CommandResult {
  private final List<String> lines;

  public TextResult(List<String> lines) {
    this.lines = lines;
  }

  public static TextResult of(String... lines) {
    return new TextResult(Arrays.asList(lines));
  }

  public List<String> lines() {
    return lines;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TextResult)) {
      return false;
    }
    TextResult other = (TextResult) o;
    return Objects.equals(lines, other.lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lines);
  }

  @Override
  public String toString() {
    return "TextResult[lines=" + lines + "]";
  }

  @Override
  public void accept(ResultVisitor visitor) throws IOException {
    visitor.visit(this);
  }
}
