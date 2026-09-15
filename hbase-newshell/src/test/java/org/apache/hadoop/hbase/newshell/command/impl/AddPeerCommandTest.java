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
package org.apache.hadoop.hbase.newshell.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.newshell.command.ExecutionContext;
import org.apache.hadoop.hbase.newshell.command.ShellCommandException;
import org.apache.hadoop.hbase.newshell.command.TextResult;
import org.apache.hadoop.hbase.newshell.hbase.StubShellAdmin;
import org.apache.hadoop.hbase.newshell.hbase.StubShellTableFactory;
import org.apache.hadoop.hbase.newshell.parser.ShellLineParser;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(SmallTests.TAG)
public class AddPeerCommandTest {

  private static final class RecordingShellAdmin extends StubShellAdmin {
    private String lastPeerId;
    private Map<String, Object> lastPeerConfigSpec;

    @Override
    public void addPeer(String peerId, Map<String, Object> peerConfigSpec) {
      this.lastPeerId = peerId;
      this.lastPeerConfigSpec = peerConfigSpec;
    }
  }

  private final AddPeerCommand command = new AddPeerCommand();
  private final RecordingShellAdmin admin = new RecordingShellAdmin();
  private final ExecutionContext context =
    new ExecutionContext(admin, new StubShellTableFactory(), new PrintWriter(new StringWriter()));

  @Test
  public void addsPeerWithClusterKey() throws Exception {
    var parsed = ShellLineParser.parse("add_peer '1', CLUSTER_KEY => 'zk1,zk2:2181:/hbase'");
    TextResult result = (TextResult) command.execute(parsed, context);

    assertEquals("1", admin.lastPeerId);
    assertEquals("zk1,zk2:2181:/hbase", admin.lastPeerConfigSpec.get("CLUSTER_KEY"));
    assertEquals(List.of("1 peer added"), result.lines());
  }

  @Test
  public void addsPeerWithNestedTableCfsMap() throws Exception {
    var parsed = ShellLineParser
      .parse("add_peer '1', CLUSTER_KEY => 'zk1,zk2:2181:/hbase', "
        + "TABLE_CFS => {'ns:tab' => ['cf1', 'cf2']}");
    command.execute(parsed, context);

    @SuppressWarnings("unchecked")
    Map<String, Object> tableCfs =
      (Map<String, Object>) admin.lastPeerConfigSpec.get("TABLE_CFS");
    assertEquals(List.of("cf1", "cf2"), tableCfs.get("ns:tab"));
  }

  @Test
  public void throwsWhenPeerIdMissing() throws Exception {
    var parsed = ShellLineParser.parse("add_peer");
    assertThrows(ShellCommandException.class, () -> command.execute(parsed, context));
  }
}
