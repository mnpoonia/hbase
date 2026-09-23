// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Force-delete the ephemeral /hbase/rs znode(s) matching the given host.
// A regionserver killed with SIGKILL never runs its own shutdown hook, so
// its znode otherwise lingers until the ZK session times out. This lets a
// stop script clean it up immediately instead of waiting on that timeout.

import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.logging.Log4jUtils;
import org.apache.hadoop.hbase.zookeeper.ZKUtil;
import org.apache.hadoop.hbase.zookeeper.ZKWatcher;
import org.apache.hadoop.hbase.zookeeper.ZNodePaths;

Log4jUtils.setAllLevels("org.apache.hadoop.hbase", "ERROR");
Log4jUtils.setAllLevels("org.apache.zookeeper", "ERROR");
Log4jUtils.setAllLevels("org.apache.hadoop", "ERROR");

String USAGE = "Usage: hbase rs_znode_cleanup.jsh <hostname>|<host:port>|<servername>\nForce-delete the /hbase/rs znode(s) matching the given host. Retries a few times since the master may be deleting it concurrently.";

int argc = Integer.parseInt(System.getenv("HBASE_JSH_ARG_COUNT"));
String[] argv = new String[argc];
for (int i = 0; i < argc; i++) {
  argv[i] = System.getenv("HBASE_JSH_ARG_" + i);
}

if (argc != 1) {
  System.out.println(USAGE);
  System.exit(1);
}

String hostOrServer = argv[0];
Configuration config = HBaseConfiguration.create();
ZKWatcher zkw = new ZKWatcher(config, "rs_znode_cleanup", null);
int exitCode = 0;
try {
  String parentZnode = zkw.getZNodePaths().rsZNode;
  List<String> children = ZKUtil.listChildrenNoWatch(zkw, parentZnode);
  List<String> matches = new ArrayList<>();
  if (children != null) {
    String[] parts = hostOrServer.split(",");
    if (parts.length == 3) {
      if (children.contains(hostOrServer)) {
        matches.add(hostOrServer);
      }
    } else {
      String prefix = hostOrServer.replace(':', ',');
      for (String child : children) {
        if (child.startsWith(prefix)) {
          matches.add(child);
        }
      }
    }
  }

  if (matches.isEmpty()) {
    System.out.println("No /hbase/rs znode found matching " + hostOrServer + " - already gone.");
  } else {
    int retries = 5;
    for (String server : matches) {
      String path = ZNodePaths.joinZNode(parentZnode, server);
      boolean deleted = false;
      for (int i = 0; i < retries && !deleted; i++) {
        ZKUtil.deleteNodeFailSilent(zkw, path);
        deleted = ZKUtil.checkExists(zkw, path) == -1;
        if (!deleted) {
          Thread.sleep(1000);
        }
      }
      System.out.println((deleted ? "Deleted " : "FAILED to delete ") + path);
      if (!deleted) {
        exitCode = 1;
      }
    }
  }
} finally {
  zkw.close();
}

System.exit(exitCode);
