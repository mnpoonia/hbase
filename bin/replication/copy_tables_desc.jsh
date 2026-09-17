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

// Recreates table descriptors from one cluster on another.
//
//   hbase copy_tables_desc.jsh master_zookeeper.quorum.peers:clientport:znode_parent
//       slave_zookeeper.quorum.peers:clientport:znode_parent [table1,table2,table3,...]

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.NamespaceNotFoundException;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.logging.Log4jUtils;

Log4jUtils.setAllLevels("org.apache.hadoop.hbase", "ERROR");
Log4jUtils.setAllLevels("org.apache.zookeeper", "ERROR");
Log4jUtils.setAllLevels("org.apache.hadoop", "ERROR");

String USAGE = "Usage: hbase copy_tables_desc.jsh master_zookeeper.quorum.peers:clientport:znode_parent slave_zookeeper.quorum.peers:clientport:znode_parent [table1,table2,table3,...]";

void createNamespaceIfNotExists(Admin src, Admin dst, String namespace) throws IOException {
  try {
    dst.getNamespaceDescriptor(namespace);
    System.out.println("Namespace \"" + namespace + "\" already exists.");
  } catch (NamespaceNotFoundException e) {
    dst.createNamespace(src.getNamespaceDescriptor(namespace));
    System.out.println("Namespace \"" + namespace + "\" was successfully created.");
  }
}

void copy(Admin src, Admin dst, String table) throws IOException {
  TableDescriptor t;
  try {
    t = src.getDescriptor(TableName.valueOf(table));
  } catch (TableNotFoundException e) {
    System.out.println("Source table \"" + table + "\" doesn't exist, skipping.");
    return;
  }

  String namespace = TableName.valueOf(table).getNamespaceAsString();
  createNamespaceIfNotExists(src, dst, namespace);

  try {
    dst.createTable(t);
  } catch (TableExistsException e) {
    System.out.println("Destination table \"" + table + "\" exists in remote cluster, skipping.");
    return;
  }

  System.out.println("Schema for table \"" + table + "\" was successfully copied to remote cluster.");
}

Configuration clusterConfig(String spec) {
  String[] parts = spec.split(":");
  Configuration conf = HBaseConfiguration.create();
  conf.set(HConstants.ZOOKEEPER_QUORUM, parts[0]);
  conf.set("hbase.zookeeper.property.clientPort", parts[1]);
  conf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, parts[2]);
  return conf;
}

int argc = Integer.parseInt(System.getenv("HBASE_JSH_ARG_COUNT"));
String[] argv = new String[argc];
for (int i = 0; i < argc; i++) {
  argv[i] = System.getenv("HBASE_JSH_ARG_" + i);
}

if (argv.length < 2 || argv.length > 3) {
  System.out.println(USAGE);
  System.out.flush();
  System.exit(1);
}

Connection connection1 = ConnectionFactory.createConnection(clusterConfig(argv[0]));
Connection connection2 = ConnectionFactory.createConnection(clusterConfig(argv[1]));
try {
  Admin admin1 = connection1.getAdmin();
  Admin admin2 = connection2.getAdmin();
  try {
    if (argv.length < 3) {
      for (TableName t : admin1.listTableNames()) {
        copy(admin1, admin2, t.getNameAsString());
      }
    } else {
      for (String t : argv[2].split(",")) {
        copy(admin1, admin2, t);
      }
    }
  } finally {
    admin1.close();
    admin2.close();
  }
} finally {
  connection1.close();
  connection2.close();
}

/exit
