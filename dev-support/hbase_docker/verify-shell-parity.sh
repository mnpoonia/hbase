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

# verify-shell-parity.sh - Runs the shell-parity-corpus.tsv commands through
# both the legacy JRuby `hbase shell` and `hbase newshell`, on the same
# standalone HBase instance, and diffs the output.
#
# Must run against a Docker image built via build-hbase.sh (source-dir mode
# recommended, so hbase-newshell is compiled in and dev-support/hbase_docker
# is present in the image at /root/hbase/dev-support/hbase_docker):
#
#   ./build-hbase.sh --source ../.. hbase_newshell_parity
#   ./verify-shell-parity.sh hbase_newshell_parity
#
# Logs are teed to LOG_FILE (default: <script-dir>/shell-parity.log). Override
# with LOG_FILE=/path/to/file ./verify-shell-parity.sh …
#
# mode=exact  in the corpus: any stdout diff fails the run (nonzero exit).
# mode=smoke: a diff is logged but does not fail the run.
#
# A command field may hold several ;-separated commands, run as one shell
# session (e.g. "create 't';put 't','r','cf:c','v';get 't','r'") so a
# mutating command can set up its own state and be diffed together with it.
# {ENGINE}, {HOST}, and {CLUSTER_KEY} placeholders are substituted per run -
# {ENGINE} to "shell"/"newshell" so the two engines' runs use disjoint
# table/peer/snapshot names against the same live cluster, {HOST} to the
# container's hostname for regionserver-targeting commands, and
# {CLUSTER_KEY} to this standalone's ZK quorum:port:znode (so add_peer
# rows do not hang on unreachable fake ZK hosts).

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="${1:-hbase_local}"
PLATFORM="linux/amd64"
SETUP_TABLE="_newshell_parity_test"
LOG_FILE="${LOG_FILE:-${SCRIPT_DIR}/shell-parity.log}"
# HBase master/RS logs from inside the container are mirrored here so a
# --rm run still leaves them for post-mortem (OOM, abort, etc.).
HBASE_LOG_DIR="${HBASE_LOG_DIR:-${SCRIPT_DIR}/parity-hbase-logs}"
# Per corpus-row wall-clock budget (seconds). Caps hangs like a stuck add_peer
# against unreachable ZK. Rosetta amd64 + multi-command peer rows routinely
# need >90s (JVM start + several replication admin RPCs), so default is 180.
COMMAND_TIMEOUT_SECS="${COMMAND_TIMEOUT_SECS:-180}"

echo "Parity image: ${IMAGE_NAME}"
echo "Log file:     ${LOG_FILE}"
echo "HBase logs:   ${HBASE_LOG_DIR}"
echo "Follow with:  tail -f ${LOG_FILE}"
echo "Cmd timeout:  ${COMMAND_TIMEOUT_SECS}s"
echo

# Truncate log up front so a killed run still leaves a partial file behind.
: > "${LOG_FILE}"
mkdir -p "${HBASE_LOG_DIR}"
# Drop prior-run master/RS logs so this run's files are unambiguous.
rm -f "${HBASE_LOG_DIR}"/hbase-*.log "${HBASE_LOG_DIR}"/hbase-*.out "${HBASE_LOG_DIR}"/*.log 2>/dev/null || true

# Capture host+container stdout/stderr to the log while still printing live.
# PIPESTATUS[0] is docker's exit code (tee always exits 0).
# stdbuf -oL keeps tee line-buffered so Ctrl-C still leaves readable output.
set +e
docker run --platform "$PLATFORM" --rm -i \
  -v "${SCRIPT_DIR}:/root/hbase/dev-support/hbase_docker:ro" \
  -v "${HBASE_LOG_DIR}:/root/hbase-bin/logs" \
  -e HBASE_SHELL_ENGINE= \
  -e COMMAND_TIMEOUT_SECS="${COMMAND_TIMEOUT_SECS}" \
  "$IMAGE_NAME" bash -s -- "$SETUP_TABLE" 2>&1 <<'CONTAINER_SCRIPT' | stdbuf -oL tee -a "${LOG_FILE}"
set -euo pipefail
SETUP_TABLE="$1"
CORPUS=/root/hbase/dev-support/hbase_docker/shell-parity-corpus.tsv
CMD_TIMEOUT="${COMMAND_TIMEOUT_SECS:-180}"
cd /root/hbase-bin

start-hbase.sh

echo "INFO: waiting for HBase master to come up..."
for _ in $(seq 1 60); do
  if echo "status" | bin/hbase shell -n > /dev/null 2>&1; then
    break
  fi
  sleep 2
done

# Peer CLUSTER_KEY must be reachable (fake hostnames hang ~10m) but must NOT
# match this cluster's own key (HBase rejects self-replication). Use the live
# ZK quorum/port with a distinct znode parent so add_peer stores config only.
ZK_QUORUM="$(bin/hbase org.apache.hadoop.hbase.util.HBaseConfTool hbase.zookeeper.quorum)"
ZK_PORT="$(bin/hbase org.apache.hadoop.hbase.util.HBaseConfTool hbase.zookeeper.property.clientPort)"
ZK_PARENT="$(bin/hbase org.apache.hadoop.hbase.util.HBaseConfTool zookeeper.znode.parent)"
CLUSTER_KEY="${ZK_QUORUM}:${ZK_PORT}:${ZK_PARENT}-parity-peer"
echo "INFO: using CLUSTER_KEY=${CLUSTER_KEY} (live ZK, non-self parent)"

echo "INFO: creating shared setup table '${SETUP_TABLE}'"
printf "create '%s', 'cf'\n" "${SETUP_TABLE}" | bin/hbase shell -n

filter_shell_noise() {
  # Drops the JRuby REPL's echoed prompt/command, timing line, and the
  # trailing irb-style "=> <return value>" echo - none of that is part of
  # the command's actual output.
  grep -Ev '^hbase:[0-9]+:[0-9]+>|^Took [0-9.]+ seconds$|^=> '
}

filter_newshell_noise() {
  # JLine doesn't always echo a newline after the "newshell> " prompt in
  # piped/non-interactive mode, so the prompt can end up glued to the front
  # of the command's own first output line (e.g. "newshell> DECOMMISSIONED
  # REGION SERVERS") or to an async logger line that lands on the same
  # physical line (e.g. "newshell> 2026-...T...INFO..."). Strip every
  # occurrence unanchored - not just a leading one - before log4j lines
  # (ISO-8601 timestamp prefix) can be recognized and dropped.
  sed -E 's/\r//g; s/newshell> ?//g' \
    | grep -Ev '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:,.]+ (INFO|WARN|ERROR|DEBUG) '
}

run_with_timeout() {
  # Runs stdin commands through the given hbase engine under a wall-clock
  # budget so a single hung RPC cannot stall the whole corpus for ~10m.
  local engine="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout --signal=KILL "${CMD_TIMEOUT}s" "$@"
  else
    "$@"
  fi
}

HOST="$(hostname)"
exact_failures=0
smoke_divergences=0
row_num=0

while IFS=$'\t' read -r command mode note; do
  [[ -z "${command}" || "${command}" == \#* ]] && continue
  row_num=$((row_num + 1))

  shell_cmd="${command//\{ENGINE\}/shell}"
  shell_cmd="${shell_cmd//\{HOST\}/${HOST}}"
  shell_cmd="${shell_cmd//\{CLUSTER_KEY\}/${CLUSTER_KEY}}"
  newshell_cmd="${command//\{ENGINE\}/newshell}"
  newshell_cmd="${newshell_cmd//\{HOST\}/${HOST}}"
  newshell_cmd="${newshell_cmd//\{CLUSTER_KEY\}/${CLUSTER_KEY}}"

  echo "INFO: [${row_num}] (${mode}) ${command}"

  # A corpus command may legitimately make either engine exit nonzero (e.g. a
  # shell command that crashes when a y/n confirmation prompt reads from
  # already-exhausted piped stdin). That must show up as a diff below, not
  # silently kill this whole script via set -e/pipefail.
  shell_out="$(echo "${shell_cmd}" | tr ';' '\n' \
    | run_with_timeout shell bin/hbase shell -n 2>&1 | filter_shell_noise)" || true
  newshell_out="$(echo "${newshell_cmd}" | tr ';' '\n' \
    | run_with_timeout newshell bin/hbase newshell -n --yes 2>&1 | filter_newshell_noise)" || true

  # {ENGINE} substitutes to "shell"/"newshell" so the two engines' runs use
  # disjoint resource names against the same live cluster; normalize those
  # engine-specific names back to a common token before comparing, so a
  # command that only differs by its {ENGINE}-derived table/peer name isn't
  # reported as a false diff.
  shell_cmp="${shell_out//_newshell_parity_shell/_newshell_parity_ENGINE}"
  shell_cmp="${shell_cmp//shell_parity_peer/ENGINE_parity_peer}"
  newshell_cmp="${newshell_out//_newshell_parity_newshell/_newshell_parity_ENGINE}"
  newshell_cmp="${newshell_cmp//newshell_parity_peer/ENGINE_parity_peer}"

  if [ "${shell_cmp}" = "${newshell_cmp}" ]; then
    echo "PASS  [${mode}] ${command}"
    continue
  fi

  case "${mode}" in
    exact)
      exact_failures=$((exact_failures + 1))
      echo "FAIL  [exact] ${command}  (${note})"
      diff <(echo "${shell_out}") <(echo "${newshell_out}") || true
      ;;
    smoke)
      smoke_divergences=$((smoke_divergences + 1))
      echo "DIVERGE  [smoke] ${command}  (${note})"
      diff <(echo "${shell_out}") <(echo "${newshell_out}") || true
      ;;
    *)
      echo "ERROR: unknown mode '${mode}' for command '${command}'" >&2
      exact_failures=$((exact_failures + 1))
      ;;
  esac
done < "${CORPUS}"

echo
echo "SUMMARY: ${exact_failures} exact failure(s), ${smoke_divergences} smoke divergence(s)"

if [ "${exact_failures}" -gt 0 ]; then
  exit 1
fi
CONTAINER_SCRIPT
status=${PIPESTATUS[0]}
set -e

echo
echo "Full log:  ${LOG_FILE}"
echo "HBase logs: ${HBASE_LOG_DIR}  (master: ls ${HBASE_LOG_DIR}/*master*)"
exit "${status}"
