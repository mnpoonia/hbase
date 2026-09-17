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

// Add, remove, or list servers in draining mode via zookeeper.

import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.logging.Log4jUtils;
import org.apache.hadoop.hbase.zookeeper.ZKUtil;
import org.apache.hadoop.hbase.zookeeper.ZKWatcher;
import org.apache.hadoop.hbase.zookeeper.ZNodePaths;

Log4jUtils.setAllLevels("org.apache.hadoop.hbase", "ERROR");
Log4jUtils.setAllLevels("org.apache.zookeeper", "ERROR");
Log4jUtils.setAllLevels("org.apache.hadoop", "ERROR");

String USAGE = "Usage: hbase draining_servers.jsh add|remove|list <hostname>|<host:port>|<servername> ...\nAdd, remove, or list servers in draining mode. Can accept either a hostname to drain all region servers in that host, a host:port pair, or a host,port,startCode triplet. More than one server can be given, separated by space.";

List<String> getLiveServers(Admin admin) throws Exception {
  List<String> servers = new ArrayList<>();
  for (var server : admin.getClusterMetrics().getLiveServerMetrics().keySet()) {
    servers.add(server.getServerName());
  }
  return servers;
}

List<String> resolveServerNames(List<String> hostOrServers, Configuration config) throws Exception {
  List<String> ret = new ArrayList<>();
  Connection connection = null;
  Admin admin = null;
  try {
    for (String hostOrServer : hostOrServers) {
      String[] parts = hostOrServer.split(",");
      if (parts.length == 3) {
        ret.add(hostOrServer);
      } else {
        if (admin == null) {
          connection = ConnectionFactory.createConnection(config);
          admin = connection.getAdmin();
        }
        String prefix = hostOrServer.replace(':', ',');
        for (String server : getLiveServers(admin)) {
          if (server.startsWith(prefix)) {
            ret.add(server);
          }
        }
      }
    }
  } finally {
    if (admin != null) admin.close();
    if (connection != null) connection.close();
  }
  return ret;
}

void addServers(List<String> hostOrServers, Configuration config) throws Exception {
  List<String> servers = resolveServerNames(hostOrServers, config);
  ZKWatcher zkw = new ZKWatcher(config, "draining_servers", null);
  try {
    String parentZnode = zkw.getZNodePaths().drainingZNode;
    for (String server : servers) {
      ZKUtil.createAndFailSilent(zkw, ZNodePaths.joinZNode(parentZnode, server));
    }
  } finally {
    zkw.close();
  }
}

void removeServers(List<String> hostOrServers, Configuration config) throws Exception {
  List<String> servers = resolveServerNames(hostOrServers, config);
  ZKWatcher zkw = new ZKWatcher(config, "draining_servers", null);
  try {
    String parentZnode = zkw.getZNodePaths().drainingZNode;
    for (String server : servers) {
      ZKUtil.deleteNodeFailSilent(zkw, ZNodePaths.joinZNode(parentZnode, server));
    }
  } finally {
    zkw.close();
  }
}

void listServers(Configuration config) throws Exception {
  ZKWatcher zkw = new ZKWatcher(config, "draining_servers", null);
  try {
    String parentZnode = zkw.getZNodePaths().drainingZNode;
    List<String> servers = ZKUtil.listChildrenNoWatch(zkw, parentZnode);
    if (servers != null) {
      for (String server : servers) {
        System.out.println(server);
      }
    }
  } finally {
    zkw.close();
  }
}

int argc = Integer.parseInt(System.getenv("HBASE_JSH_ARG_COUNT"));
String[] argv = new String[argc];
for (int i = 0; i < argc; i++) {
  argv[i] = System.getenv("HBASE_JSH_ARG_" + i);
}

if (argc == 0) {
  System.out.println(USAGE);
  System.out.flush();
  System.exit(3);
}

String command = argv[0];
List<String> hostOrServers = new ArrayList<>();
for (int i = 1; i < argc; i++) {
  hostOrServers.add(argv[i]);
}

Configuration config = HBaseConfiguration.create();

switch (command) {
  case "add":
    if (hostOrServers.isEmpty()) {
      System.out.println(USAGE);
      System.exit(1);
    }
    addServers(hostOrServers, config);
    break;
  case "remove":
    if (hostOrServers.isEmpty()) {
      System.out.println(USAGE);
      System.exit(1);
    }
    removeServers(hostOrServers, config);
    break;
  case "list":
    listServers(config);
    break;
  default:
    System.out.println(USAGE);
    System.exit(3);
}

/exit
