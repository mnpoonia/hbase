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
# mode=exact  in the corpus: any stdout diff fails the run (nonzero exit).
# mode=smoke: a diff is logged but does not fail the run.
#
# A command field may hold several ;-separated commands, run as one shell
# session (e.g. "create 't';put 't','r','cf:c','v';get 't','r'") so a
# mutating command can set up its own state and be diffed together with it.
# {ENGINE} and {HOST} placeholders are substituted per run - {ENGINE} to
# "shell"/"newshell" so the two engines' runs use disjoint table/peer/
# snapshot names against the same live cluster, {HOST} to the container's
# hostname for regionserver-targeting commands.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="${1:-hbase_local}"
PLATFORM="linux/amd64"
SETUP_TABLE="_newshell_parity_test"

docker run --platform "$PLATFORM" --rm -i \
  -v "${SCRIPT_DIR}:/root/hbase/dev-support/hbase_docker:ro" \
  -e HBASE_SHELL_ENGINE= \
  "$IMAGE_NAME" bash -s -- "$SETUP_TABLE" <<'CONTAINER_SCRIPT'
set -euo pipefail
SETUP_TABLE="$1"
CORPUS=/root/hbase/dev-support/hbase_docker/shell-parity-corpus.tsv
cd /root/hbase-bin

start-hbase.sh

echo "INFO: waiting for HBase master to come up..."
for _ in $(seq 1 60); do
  if echo "status" | bin/hbase shell -n > /dev/null 2>&1; then
    break
  fi
  sleep 2
done

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

HOST="$(hostname)"
exact_failures=0
smoke_divergences=0

while IFS=$'\t' read -r command mode note; do
  [[ -z "${command}" || "${command}" == \#* ]] && continue

  shell_cmd="${command//\{ENGINE\}/shell}"
  shell_cmd="${shell_cmd//\{HOST\}/${HOST}}"
  newshell_cmd="${command//\{ENGINE\}/newshell}"
  newshell_cmd="${newshell_cmd//\{HOST\}/${HOST}}"

  # A corpus command may legitimately make either engine exit nonzero (e.g. a
  # shell command that crashes when a y/n confirmation prompt reads from
  # already-exhausted piped stdin). That must show up as a diff below, not
  # silently kill this whole script via set -e/pipefail.
  shell_out="$(echo "${shell_cmd}" | tr ';' '\n' | bin/hbase shell -n 2>&1 | filter_shell_noise)" || true
  newshell_out="$(echo "${newshell_cmd}" | tr ';' '\n' | bin/hbase newshell -n 2>&1 | filter_newshell_noise)" || true

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
