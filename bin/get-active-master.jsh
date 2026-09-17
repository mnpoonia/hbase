// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.

// Prints the hostname of the machine running the active master.

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.zookeeper.ZKWatcher;
import org.apache.hadoop.hbase.zookeeper.MasterAddressTracker;
import org.apache.hadoop.hbase.logging.Log4jUtils;

Log4jUtils.setAllLevels("org.apache.hadoop.hbase", "ERROR");
Log4jUtils.setAllLevels("org.apache.zookeeper", "ERROR");
Log4jUtils.setAllLevels("org.apache.hadoop", "ERROR");

var config = HBaseConfiguration.create();
var zk = new ZKWatcher(config, "get-active-master", null);
try {
  System.out.println(MasterAddressTracker.getMasterAddress(zk).getHostname());
} finally {
  zk.close();
}

/exit
