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

// Issues a stop command to one or more regionservers via RPC. Intended for
// environments where sshing around is inappropriate.

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.logging.Log4jUtils;

Log4jUtils.setAllLevels("org.apache.hadoop.hbase", "ERROR");
Log4jUtils.setAllLevels("org.apache.zookeeper", "ERROR");
Log4jUtils.setAllLevels("org.apache.hadoop", "ERROR");

String USAGE = "Usage: hbase shutdown_regionserver.jsh <host:port>..\nStops the specified regionservers via RPC";

int argc = Integer.parseInt(System.getenv("HBASE_JSH_ARG_COUNT"));
String[] argv = new String[argc];
for (int i = 0; i < argc; i++) {
  argv[i] = System.getenv("HBASE_JSH_ARG_" + i);
}

if (argc == 0) {
  System.err.println(USAGE);
  System.err.flush();
  System.exit(1);
}

for (String hp : argv) {
  if (!hp.contains(":")) {
    System.err.println("Error: Invalid host:port: " + hp);
    System.err.println(USAGE);
    System.err.flush();
    System.exit(1);
  }
}

var config = HBaseConfiguration.create();
Connection connection = ConnectionFactory.createConnection(config);
Admin admin = null;
try {
  admin = connection.getAdmin();
} catch (Exception e) {
  System.err.println("Error: Couldn't instantiate HBaseAdmin");
  System.err.flush();
  System.exit(1);
}

for (String hostport : argv) {
  admin.stopRegionServer(hostport);
}
admin.close();
connection.close();
