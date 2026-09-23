#!/usr/bin/env bash
#
#/**
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

# Stop a regionserver that was already drained via graceful_drain.sh, then
# remove it from the draining list. Stops the local process via its pidfile
# (hbase-daemon.sh stop regionserver) - run this on the host running the
# regionserver, not remotely.
function usage {
  echo "Usage: graceful_stop_drained.sh [--config <conf-dir>] <hostname>"
  echo " hostname       Hostname to remove from the draining list; match what HBase uses"
  exit 1
}

if [ $# -ne 1 ]; then
  usage
fi

bin=`dirname "$0"`
bin=`cd "$bin">/dev/null; pwd`
# This will set HBASE_HOME, etc.
. "$bin"/hbase-config.sh

hostname=$1

function log {
  echo "`date` $1"
}

log "Stopping regionserver"
"$bin"/hbase-daemon.sh --config "${HBASE_CONF_DIR}" stop regionserver

log "Removing $hostname from draining servers"
"$bin"/hbase --config "${HBASE_CONF_DIR}" "$bin"/draining_servers.jsh remove "$hostname"

log "Done. $hostname stopped and removed from draining"
