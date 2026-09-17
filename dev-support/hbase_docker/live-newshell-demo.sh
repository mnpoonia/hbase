#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -euo pipefail

# live-newshell-demo.sh - Boots a fresh standalone HBase container and walks
# through a curated tour of `newshell` commands, one at a time, echoing each
# command and pausing between them so an audience can read the output as it
# streams by.
#
# Must run against a Docker image built via build-hbase.sh (source-dir mode
# recommended, so hbase-newshell is compiled in):
#
#   ./build-hbase.sh --source ../.. hbase_newshell_demo
#   ./live-newshell-demo.sh hbase_newshell_demo

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="${1:-hbase_local}"
PLATFORM="linux/amd64"
PAUSE_SECONDS="${PAUSE_SECONDS:-10}"

docker run --platform "$PLATFORM" --rm -i \
  -e PAUSE_SECONDS="$PAUSE_SECONDS" \
  "$IMAGE_NAME" bash -s <<'CONTAINER_SCRIPT'
set -euo pipefail
cd /root/hbase-bin

start-hbase.sh

echo "INFO: waiting for HBase master to come up..."
for _ in $(seq 1 60); do
  if echo "status" | bin/hbase shell -n > /dev/null 2>&1; then
    break
  fi
  sleep 2
done

run_step() {
  local cmd="$1"
  echo
  echo "================================================================"
  echo "newshell> ${cmd}"
  echo "================================================================"
  echo "${cmd}" | tr ';' '\n' | bin/hbase newshell -n
  sleep "${PAUSE_SECONDS}"
}

run_step "status"

run_step "create_namespace 'demo_ns'"
run_step "describe_namespace 'demo_ns'"
run_step "list_namespace"
run_step "alter_namespace 'demo_ns', {METHOD => 'set', 'PROPERTY' => 'VALUE'}"

run_step "create 'demo_ns:demo_table', 'cf'"
run_step "list"
run_step "describe 'demo_ns:demo_table'"

run_step "put 'demo_ns:demo_table', 'row1', 'cf:c1', 'hello newshell'"
run_step "get 'demo_ns:demo_table', 'row1'"
run_step "scan 'demo_ns:demo_table'"

run_step "disable_all 'demo_ns:demo_.*'"
run_step "is_disabled 'demo_ns:demo_table'"
run_step "enable_all 'demo_ns:demo_.*'"
run_step "is_enabled 'demo_ns:demo_table'"

run_step "list_regions 'demo_ns:demo_table'"
run_step "get_table 'demo_ns:demo_table'"
run_step "locate_region 'demo_ns:demo_table', 'row1'"

run_step "balance_switch false"
run_step "balance_switch true"

run_step "disable 'demo_ns:demo_table'"
run_step "drop_all 'demo_ns:demo_.*'"
run_step "drop_namespace 'demo_ns'"

echo
echo "DEMO COMPLETE"
CONTAINER_SCRIPT
