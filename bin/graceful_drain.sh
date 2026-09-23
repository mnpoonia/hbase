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

# Disable the balancer, mark a server draining, and unload its regions.
# Unlike graceful_stop.sh, this does NOT stop the server or restore the
# draining list - run this before you stop the regionserver process, and
# remove it from the draining list yourself afterwards.
function usage {
  echo "Usage: graceful_drain.sh [--config <conf-dir>] [--maxthreads <number of threads>] \
[-f |--filename <file path>] <hostname>"
  echo " hostname       Hostname or hostname:port to drain; match what HBase uses. Note: the \
region mover (RegionMover -r) only accepts hostname or hostname:port, not a full \
host,port,startcode triplet, even though draining_servers.jsh/rs_znode_cleanup.jsh accept one."
  echo " maxthreads xx  Limit the number of threads used by the region mover. Default value is 1."
  echo " f|filename xx  File to write the list of unloaded regions to. Default is /tmp/<hostname>."
  exit 1
}

if [ $# -lt 1 ]; then
  usage
fi

bin=`dirname "$0"`
bin=`cd "$bin">/dev/null; pwd`
# This will set HBASE_HOME, etc.
. "$bin"/hbase-config.sh

maxthreads=1
filename=
while [ $# -gt 0 ]
do
  case "$1" in
    --maxthreads) shift; maxthreads=$1; shift;;
    -f|--filename) shift; filename=$1; shift;;
    --config) shift; shift;;
    -*) usage;;
    *) break;;
  esac
done

if [ $# -ne 1 ]; then
  usage
fi

hostname=$1
if [ "$filename" == "" ]; then
  filename="/tmp/$hostname"
fi

function log {
  echo "`date` $1"
}

log "Disabling load balancer"
# Force the legacy shell engine for this internal pipe: newshell's terminal
# binds directly to the tty (system(true)) and never writes to the pipe, so
# this capture comes back empty under HBASE_SHELL_ENGINE=newshell.
HBASE_BALANCER_STATE=$(echo 'balance_switch false' | HBASE_SHELL_ENGINE=jruby "$bin"/hbase --config "${HBASE_CONF_DIR}" shell -n | grep 'Previous balancer state' | awk -F": " '{print $2}')
log "Previous balancer state was $HBASE_BALANCER_STATE"

log "Adding $hostname to draining servers"
"$bin"/hbase --config "${HBASE_CONF_DIR}" "$bin"/draining_servers.jsh add "$hostname"

log "Unloading $hostname region(s)"
HBASE_NOEXEC=true "$bin"/hbase --config "${HBASE_CONF_DIR}" org.apache.hadoop.hbase.util.RegionMover \
--filename "$filename" --maxthreads "$maxthreads" --operation unload --regionserverhost "$hostname"
log "Unloaded $hostname region(s)"

if [ "$HBASE_BALANCER_STATE" != "false" ]; then
  log "Restoring balancer state to $HBASE_BALANCER_STATE"
  echo "balance_switch $HBASE_BALANCER_STATE" | HBASE_SHELL_ENGINE=jruby "$bin"/hbase --config "${HBASE_CONF_DIR}" shell &> /dev/null
else
  log "Balancer was already off, leaving it off"
fi

log "Done. $hostname is draining and unloaded - stop the regionserver, then run: draining_servers.jsh remove $hostname"
