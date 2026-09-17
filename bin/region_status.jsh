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

// View the current status of all regions on an HBase cluster. Predominantly
// used to determine if all the regions in META have been onlined yet on
// startup.

import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.MetaTableAccessor;
import org.apache.hadoop.hbase.CatalogFamilyFormat;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.logging.Log4jUtils;
import org.apache.hadoop.hbase.util.Bytes;

Log4jUtils.setAllLevels("org.apache.hadoop.hbase", "ERROR");
Log4jUtils.setAllLevels("org.apache.zookeeper", "ERROR");
Log4jUtils.setAllLevels("org.apache.hadoop", "ERROR");

String USAGE = "Usage: hbase region_status.jsh [wait] [--table <table_name>]";

int argc = Integer.parseInt(System.getenv("HBASE_JSH_ARG_COUNT"));
String[] rawArgv = new String[argc];
for (int i = 0; i < argc; i++) {
  rawArgv[i] = System.getenv("HBASE_JSH_ARG_" + i);
}

List<String> positional = new ArrayList<>();
String tablename = null;
for (int i = 0; i < rawArgv.length; i++) {
  String a = rawArgv[i];
  if (a.equals("-t") || a.equals("--table")) {
    i++;
    tablename = rawArgv[i];
  } else if (a.equals("-h") || a.equals("--help")) {
    System.out.println(USAGE);
    System.out.flush();
    System.exit(0);
  } else {
    positional.add(a);
  }
}

boolean shouldWait = !positional.isEmpty() && positional.get(0).equals("wait");
if (!positional.isEmpty() && !shouldWait) {
  System.out.println(USAGE);
  System.out.flush();
  System.exit(1);
}

String finalTablename = tablename;

var config = HBaseConfiguration.create();
config.set("fs.defaultFS", config.get(HConstants.HBASE_DIR));
Connection connection = ConnectionFactory.createConnection(config);
Admin admin = connection.getAdmin();

int metaCount = 0;

Scan scan;
byte[] tableNameMetaPrefix = null;
if (finalTablename == null) {
  scan = new Scan();
} else {
  tableNameMetaPrefix = Bytes.toBytes(finalTablename + (char) HConstants.META_ROW_DELIMITER);
  scan = new Scan().withStartRow(tableNameMetaPrefix);
}
scan.setCacheBlocks(false);
scan.setCaching(10);
scan.setFilter(new FirstKeyOnlyFilter());
scan.addColumn(Bytes.toBytes("info"), Bytes.toBytes("regioninfo"));

Table metaTable = connection.getTable(TableName.valueOf("hbase:meta"));
ResultScanner scanner = metaTable.getScanner(scan);
for (Result result : scanner) {
  String rowid = Bytes.toString(result.getRow());
  if (tableNameMetaPrefix != null && !rowid.startsWith(Bytes.toString(tableNameMetaPrefix))) {
    // gone too far, stop
    break;
  }
  RegionInfo region = CatalogFamilyFormat.getRegionInfo(result);
  if (region != null && !region.isOffline()) {
    metaCount++;
  }
}
scanner.close();
metaTable.close();
// hbase:meta itself isn't included in the scan of hbase:meta
if (finalTablename == null) {
  metaCount++;
}

TableName targetTable = finalTablename == null ? null : TableName.valueOf(finalTablename);
long serverCount;
while (true) {
  if (targetTable == null) {
    serverCount = admin.getClusterMetrics().getRegionCount();
  } else if (TableName.isMetaTableName(targetTable)) {
    serverCount = admin.getRegions(targetTable).size();
  } else {
    serverCount = MetaTableAccessor.getTableRegions(connection, targetTable).size();
  }
  System.out.println("Region Status: " + serverCount + " / " + metaCount);
  System.out.flush();
  if (shouldWait && serverCount < metaCount) {
    Thread.sleep(10000);
  } else {
    break;
  }
}

admin.close();
connection.close();

System.exit(serverCount == metaCount ? 0 : 1);
