# hbase-newshell: remaining work

Status snapshot as of 2026-09-18. `hbase-newshell` currently ports **63 of 184**
hbase-shell Ruby commands (`hbase-shell/src/main/ruby/shell/commands/*.rb`).
Groups below follow the same grouping hbase-shell itself uses
(`hbase-shell/src/main/ruby/shell.rb`). Every command/script entry also lists
its filesystem path.

> Checkbox note: GitHub-flavored Markdown only renders `- [ ]` as a real
> (togglable) checkbox when it's the first token of a bullet-list item — put
> it in a table cell and it just shows as literal text. So these are plain
> single-column checklists rather than multi-column tables; that's a
> Markdown limitation, not a formatting choice. For a genuinely clickable,
> state-persisting version see `docs/newshell/REMAINING_WORK.html`.

## 1. Scripts / infrastructure work

Non-command-porting tasks that block newshell from being a full replacement
for the JRuby shell.

- [ ] **Repoint `bin/hbase shell` at `NewShellMain`.** (`bin/hbase`) Today
  `bin/hbase` has two branches: `shell` (still `org.jruby.Main
  jar-bootstrap.rb`) and `newshell` (`NewShellMain`), added side-by-side.
  Once command coverage is sufficient, change the `shell` case in `bin/hbase`
  to `CLASS='org.apache.hadoop.hbase.newshell.NewShellMain'` (mirroring the
  `newshell` case) and drop the `jruby_cmds`/JRuby classpath wiring. This is
  the actual "JRuby removal" cutover for HBASE-30250.
- [ ] **Preserve `-n` / piped-stdin one-shot semantics.** Real ops scripts
  already depend on `hbase shell -n` for one-shot commands piped over
  stdin — see section 3 below (`bin/graceful_stop.sh`,
  `bin/rolling-restart.sh`, both piping `balance_switch`). `bin/hbase
  newshell -n` must behave identically for these to keep working after the
  cutover.
- [x] **Expand `dev-support/hbase_docker/shell-parity-corpus.tsv`** with one
  entry per newly-ported command (see
  `dev-support/hbase_docker/verify-shell-parity.sh`), so every command lands
  with an automated shell-vs-newshell parity check before being considered
  "done," not just unit-tested in isolation. — completed: true. All 20
  commands ported in this pass (14 ddl stragglers + 6 namespace commands) now
  have a corpus row. Full-suite run (`./verify-shell-parity.sh
  hbase_newshell_parity`, built via `./build-hbase.sh --source ../..
  hbase_newshell_parity`): **0 exact failures, 13 smoke divergences** across
  all 46 corpus rows (33 exact passes). Fixing this uncovered a real bug in
  the harness itself (see below) plus three real command bugs — see the
  `describe_namespace`, `list_namespace_tables`, and `show_filters` entries in
  section 8 below.
  - **Harness bug found and fixed**: `verify-shell-parity.sh` ran under
    `set -euo pipefail`, and captured each engine's output via
    `x="$(... | bin/hbase ... -n | filter)"`. If either engine's invocation
    exited nonzero — e.g. the legacy shell crashing on an unanswered `y/n`
    confirmation prompt for `disable_all`/`drop_all`/`enable_all` when piped
    non-interactively — `pipefail` propagated that exit code straight through
    `set -e`, silently killing the whole script mid-loop: no FAIL/DIVERGE
    line, no SUMMARY line, no nonzero exit visible to a caller piping through
    `| tail`. Fixed by appending `|| true` to both capture lines so a crash's
    output is captured and diffed like any other divergence, instead of
    aborting the run.
- [ ] **Decide on the descriptor-driven command catalog** (deferred design
  item from the original HBASE-30250 design doc): today every `ShellCommand`
  is a hand-written class re-implementing its own arg validation
  (`hbase-newshell/src/main/java/org/apache/hadoop/hbase/newshell/command/impl/`).
  Revisit once the ported-command count grows enough that this boilerplate
  hurts (roughly 2x current count) — not worth building preemptively.
- [ ] **One-shot / non-REPL invocation story** — the design doc envisions
  `./bin/hbase newshell create --table t1 --name f1`-style one-shot commands
  derived from the same descriptor. Currently only the REPL grammar and
  `-n`/script-file modes exist.
- [ ] **JRuby removal follow-through** once `bin/hbase shell` is repointed:
  drop `hbase-shell`'s JRuby runtime dependency from the distribution/assembly
  if no other consumer needs it, and update any docs/README referencing the
  Ruby shell as the only shell.
- [x] **`bin/hbase <script>.jsh`/`.java` dispatch.** (`bin/hbase`) Implemented:
  a `.jsh` argument resolves the script path (bare filename under
  `${HBASE_HOME}/bin/`, otherwise passed through as-is), passes positional
  args via `HBASE_JSH_ARG_0..N` + `HBASE_JSH_ARG_COUNT` env vars (jshell
  script mode has no `String[] args` equivalent, and re-joining args into
  one string breaks on embedded spaces), then execs
  `jdk.internal.jshell.tool.JShellToolProvider` with `--execution local -q
  <script>.jsh`. **`--execution local` is required for correct exit codes** —
  without it, jshell forks a separate JDI-driven remote VM to run snippets,
  so `System.exit()` inside the script only kills that remote VM while the
  local jshell process (whose exit status `bin/hbase`'s `exec` actually
  returns) just exits 0 via EOF regardless, with a `State engine
  terminated.` notice interleaving into the script's buffered stdout.
  `--execution local` runs snippets in-process, so `System.exit()` correctly
  terminates the real process with the intended code. A `.java` argument
  resolves the path the same way and execs `java <script>.java` directly
  (JEP 330 single-file launch, which forwards trailing args natively to
  `main(String[] args)` — no env-var passthrough needed). Both mirror
  today's `.rb` → `org.jruby.Main` dispatch. This is the replacement path for
  the standalone scripts in section 2 and the external-consumer scripts in
  sections 5-6 — but **no new class gets added to `hbase-newshell` for any of
  them.** `.jsh` is the default; `.java` is for scripts complex enough to
  want real class structure and native argv (none of section 2's scripts
  ended up needing it — see section 2 below). See the Principles section in
  [EXTERNAL_CONSUMER_HANDLING_PLAN.md](EXTERNAL_CONSUMER_HANDLING_PLAN.md).
  Both `jshell` and single-file `java` ship with the JDK already (Java 9+/11+;
  this repo already targets 17), so this adds no new runtime dependency.
  One jshell script-loader quirk to watch for: a multi-line string built via
  leading `+` continuation (no enclosing braces) gets mis-split into separate
  snippets and throws non-fatal `bad operand type... for unary operator '+'`
  errors, silently breaking the resulting string — keep such literals on one
  physical source line (multi-line **method bodies** with braces are fine).
- [ ] **`docs/superpowers/specs/` design doc reconciliation** — if the
  original HBASE-30250 spec doc is checked in anywhere in this worktree,
  keep it in sync with actual scope decisions made along the way (e.g. the
  deferred descriptor-catalog item above).

### Pre-built demo Docker image

For a live walkthrough of `newshell` (standalone HBase, no cluster setup):

```
cd dev-support/hbase_docker
./build-hbase.sh --source ../.. hbase_newshell_demo   # ~15-20 min, one-time
./live-newshell-demo.sh hbase_newshell_demo           # boots + runs the tour
```

`build-hbase.sh --source ../.. <tag>` compiles this worktree (including
`hbase-newshell`) into a standalone-HBase Docker image tagged `<tag>`; the
image used for this pass was tagged `hbase_newshell_demo`. Rebuild with the
same command any time the source changes — there's no separate "publish"
step, the tag is purely local.

`dev-support/hbase_docker/live-newshell-demo.sh <image>` (new script, this
pass) boots a fresh container from that image, runs `start-hbase.sh`, waits
for the master, then walks a curated `newshell` command tour one command at
a time — echoing each command and pausing (`PAUSE_SECONDS`, default 10s)
between steps so an audience can read the output as it streams by: `status`;
namespace create/describe/list/alter/drop; table create/list/describe;
put/get/scan; `disable_all`/`is_disabled`/`enable_all`/`is_enabled`;
`list_regions`/`get_table`/`locate_region`; `balance_switch`. It was dry-run
end to end against `hbase_newshell_demo` with `PAUSE_SECONDS=1` and completed
cleanly (23 steps, no errors, `DEMO COMPLETE` printed) — see the corpus-fix
section above for the two real command bugs this and the parity run
surfaced, both already fixed before this dry run.

If you don't have `hbase_newshell_demo` built locally (e.g. a fresh
checkout), run the two commands above in order — the build step is the only
prerequisite.

## 2. Standalone JRuby scripts in `bin/` (1 remaining, separate from shell commands)

These run as `hbase org.jruby.Main <script>.rb`, outside the shell REPL
entirely, so they weren't caught by the shell-commands diff in section 4.
Every one of them is a `.rb` file, so **all of them need the JRuby
interpreter just to be parsed and started** — even a script that only
`exec`s straight into a Java class still fails once JRuby is gone, because
JRuby is what's running the `.rb` file in the first place.

Same principles as the external-consumer scripts in sections 5-6 (see
[EXTERNAL_CONSUMER_HANDLING_PLAN.md](EXTERNAL_CONSUMER_HANDLING_PLAN.md)):
no new `hbase-newshell` class gets added for any of these. Each `.rb` file
is rewritten in place as a `.jsh` (default) or `.java` (when compile-time
checking is worth it) driver against the public HBase API, at the same
`bin/<name>.jsh`/`.java` path, run via the new `bin/hbase
<script>.jsh|.java` dispatch (section 1). `bin/` keeps looking like `bin/`
— same filenames, same invocation shape — just `.jsh`/`.java` instead of
`.rb`. Genuinely generic content shared across these scripts (no
per-script business logic) may become a small helper class in this repo;
anything consumer/script-specific stays in the driver itself.

- [x] `draining_servers.rb` — ported to `bin/draining_servers.jsh`.
  Add/remove/list draining region servers via ZooKeeper (`ZKWatcher`/
  `ZKUtil`, `ZNodePaths.drainingZNode`). **Deprecated since 2.0** in favor of
  the Admin decommission API — largely superseded by
  `decommission_regionservers`/`list_decommissioned_regionservers`/
  `recommission_regionserver`, which are **already ported** — but kept as a
  real port rather than dropped. Verified in Docker: add/list/remove ZK
  round trip byte-identical to the ruby version; exit codes correct (`3` for
  no-args/bad-command, `0` for success) after the `--execution local` fix
  (section 1).
- [x] `get-active-master.rb` — ported to `bin/get-active-master.jsh`. Prints
  the active master's hostname via `MasterAddressTracker`/`ZKWatcher`.
  Referenced by k8s_hbase's `get_active_hmaster.sh` (see memory
  `project_hbase_newshell_jruby_removal`). Verified in Docker: output
  byte-identical to `hbase org.jruby.Main bin/get-active-master.rb`.
- [x] `region_mover.rb` — ported to `bin/region_mover.jsh`. The original was
  a one-line `exec "#{$BIN}/hbase org.apache.hadoop.hbase.util.RegionMover
  #{ARGV.join(' ')}"` wrapper; `RegionMover` is already pure Java, so the
  `.jsh` is a one-line forward of `HBASE_JSH_ARG_*` into
  `RegionMover.main(args)`. Verified in Docker: missing-required-option
  errors propagate as the correct process exit code (`1`) via
  `AbstractHBaseTool`'s `System.exit(ret)` path — confirms the
  `--execution local` fix (section 1) matters for delegating scripts too,
  not just ones with their own `System.exit` calls. Live region-unload not
  exercised (would drain the only regionserver in the single-node test
  cluster); the script itself is a trivial one-line delegation.
- [x] `region_status.rb` — ported to `bin/region_status.jsh`. Polls META for
  online region count, with a `wait` mode and `--table` filter. Two API-drift
  fixes needed versus the ruby original: `Scan(byte[])` no longer exists
  (removed constructor) — replaced with `new Scan().withStartRow(...)`; and
  `MetaTableAccessor.getTableRegions` explicitly rejects `hbase:meta` itself
  ("can't be used to locate meta regions") — added a
  `TableName.isMetaTableName(...)` branch using `admin.getRegions(...)`
  instead for that case. Verified in Docker: no-table, `--table hbase:meta`,
  and `--table <nonexistent>` all produce correct counts/exit codes.
- [x] `shutdown_regionserver.rb` — ported to `bin/shutdown_regionserver.jsh`.
  Issues an RPC stop command (`admin.stopRegionServer(hostport)`) to one or
  more regionservers, for environments where SSH access isn't available.
  Verified in Docker: usage (no args) and invalid-`host:port` validation
  paths both correct (`EXIT=1`); live RPC stop not exercised against the test
  cluster (would kill its only regionserver) — low risk given it's a
  one-line delegation to `Admin.stopRegionServer`.
- [ ] `hirb.rb` — `bin/hirb.rb`. The JRuby shell's own legacy
  bootstrap/entrypoint script; retire alongside the `bin/hbase shell` →
  `NewShellMain` cutover (item 1 above), not something to "port" itself.
- [x] `copy_tables_desc.rb` — ported to `bin/replication/copy_tables_desc.jsh`
  (invoked via `bin/hbase replication/copy_tables_desc.jsh <src-zk> <dst-zk>
  [tables]`, resolving the path since it's outside `bin/`'s top level).
  Recreates table descriptors from one cluster on another using
  `HBaseConfiguration`/`ConnectionFactory` directly rather than shell
  commands. Originally planned as `.java` for compile-time checking given
  its multi-cluster/descriptor-recreation complexity, but converted to
  `.jsh` once `draining_servers.jsh` proved jshell handles multiple top-level
  methods and control flow fine (the one real jshell bug found — multi-line
  string concatenation, see section 1 — is easy to avoid). The `.java`
  dispatch mechanism itself stays in `bin/hbase` as a documented option for
  any future script that wants a real class or native `String[] args`
  instead of the `HBASE_JSH_ARG_*` env-var pattern — it just has no active
  user in this batch. Verified in Docker: usage/`EXIT=1` on bad args, and a
  self-copy smoke test (pointing both cluster specs at the same standalone
  ZK) exercises both the namespace-exists and table-exists skip paths
  correctly.

## 3. Shell scripts (`.sh`) that invoke Ruby-based shell commands

These aren't standalone JRuby scripts — they're ordinary bash scripts that
shell out to `hbase ... shell -n` and pipe Ruby shell commands over stdin
for one-shot execution. They matter because they're real, in-the-wild
callers of the shell REPL's non-interactive (`-n`) mode, which is exactly
the gap called out in section 1 ("Preserve `-n` / piped-stdin one-shot
semantics").

- [ ] `bin/graceful_stop.sh` — pipes `balance_switch false` into
  `hbase --config "${HBASE_CONF_DIR}" shell -n` (line 118), and
  `balance_switch <state>` into plain `... shell` with no `-n` (line 199).
  `balance_switch` is **already ported**
  (`hbase-newshell/src/main/java/org/apache/hadoop/hbase/newshell/command/impl/BalanceSwitchCommand.java`),
  but the script still hardcodes the legacy `shell` subcommand and depends
  on piped-stdin one-shot behavior matching once repointed to newshell.
- [ ] `bin/rolling-restart.sh` (lines 205, 225) — identical
  `balance_switch` piped-into-`shell -n` pattern as `graceful_stop.sh`.
- [x] `dev-support/hbase_docker/verify-shell-parity.sh` — already
  newshell-aware; runs the same command through both
  `bin/hbase shell -n` and `bin/hbase newshell -n` and diffs the output.
  This is the parity tool referenced in section 1, not something needing
  porting itself.

## 4. Thrift Ruby examples (`hbase-examples`)

Separate from the shell/JRuby-removal effort, but still Ruby scripts living
in this repo, so tracked here for completeness.

- [ ] `DemoClient.rb` — `hbase-examples/src/main/ruby/DemoClient.rb`. Plain
  Ruby (not JRuby) Thrift client example; requires the standalone `thrift`
  gem and the generated `gen-rb` bindings, not `bin/hbase`. Marked
  `TODO: fix rb/php/py examples to actually work` (HBASE-3630) in its own
  header — already broken/unmaintained independent of newshell.
- [ ] `gen-rb/hbase.rb`, `gen-rb/hbase_constants.rb`, `gen-rb/hbase_types.rb`
  — `hbase-examples/src/main/ruby/gen-rb/`. Thrift-generated bindings
  consumed by `DemoClient.rb`. Not hand-maintained; regenerate from the
  Thrift IDL rather than porting.
- [ ] `index-builder-setup.rb` — `hbase-examples/src/main/ruby/index-builder-setup.rb`.
  Not a JRuby program — it's a batch of shell commands (`create`, `put`)
  meant to be fed into an interactive/non-interactive **hbase shell**
  session to seed sample data for the IndexBuilder example. Both commands
  it uses are already ported (`create`, `put`), so it should work
  unmodified once one-shot script-file invocation (section 1) is
  supported by newshell.

## 5. External consumer: `k8s_hbase` JRuby scripts

[View handling plan](EXTERNAL_CONSUMER_HANDLING_PLAN.md#k8s_hbase)

Not in this repo — these live in `~/Code/sfdc/k8s_hbase` (a separate,
internal ops repo) and are invoked as `$HBASE_HOME/bin/hbase org.jruby.Main
<script>.rb`, so they depend on JRuby exactly the same way the `bin/*.rb`
scripts in section 2 do. They're real external consumers that will break
the day `bin/hbase shell`/JRuby is actually removed (section 1), so listed
here for visibility even though nothing in this repo needs to change for
them — the dependency is one-directional (they call *into* `bin/hbase`,
this repo doesn't call into them). Paths are relative to the `k8s_hbase`
repo root, under `docker/scripts/bigdata-custom-managed-ops-scripts/`.

- [ ] `migration_validation.rb` — `eks_fkp_migration/migration_validation.rb`. Validates an EKS→FKP migration (table size comparison) for a given migration command id.
- [ ] `open_region_parity.rb` — `eks_fkp_migration/open_region_parity.rb`. Compares open-region parity between source/target clusters during migration.
- [ ] `raw_scan_rows.rb` — `eks_fkp_migration/raw_scan_rows.rb`. Raw-scans a row-key range on a target cluster/DC for migration verification.
- [ ] `update_global_backup_ttl.rb` — `eks_fkp_migration/update_global_backup_ttl.rb`. Gets/sets the global backup TTL value.
- [ ] `migration_impact_analysis_row_counter.rb` — `hgrate_scripts/migration_impact_analysis/migration_impact_analysis_row_counter.rb`. Row-counts tables from an input CSV to size a migration's impact.
- [ ] `fkp-oordr.rb` — `hgrate_scripts/new_oorb_customer_onboarding/fkp-oordr-setup/fkp-oordr.rb`. Sets up FKP OORDR (out-of-region disaster recovery) replication peer cloned from an existing peer.
- [ ] `check_orgstore_connectivity.rb` — `hgrate_scripts/orgstore_connectivity_check/check_orgstore_connectivity.rb`. Checks org-store connectivity as a migration pre-check.
- [ ] `tenant_count_analyzer.rb` — `hgrate_scripts/tenant_count_via_distinct_query_analyzer/tenant_count_analyzer.rb`. Estimates tenant counts via Phoenix distinct-value queries.
- [ ] `tenant_delete_deduplication.rb` — `hgrate_scripts/tenant_delete_deduplication/tenant_delete_deduplication.rb`. Deduplicates tenant-delete markers/records.
- [ ] `tenant_profiler_validator.rb` — `hgrate_scripts/tenant_profiler_validator/tenant_profiler_validator.rb`. Validates tenant profiler output/config.
- [ ] `tenant_size_estimation_via_phoenix.rb` — `hgrate_scripts/tenant_size_estimation_via_phoenix/tenant_size_estimation_via_phoenix.rb`. Estimates per-tenant table size via Phoenix queries.
- [ ] `migration_validation_daemon.rb` — `migration_validation_daemon/migration_validation_daemon.rb`. Long-running daemon variant of migration validation; run continuously rather than one-shot.

> Also confirmed in `k8s_hbase`: a large number of `.sh` wrapper scripts
> under `docker/scripts/bigdata-custom-managed-ops-scripts/hbase/`,
> `phoenix/`, and `eks_fkp_migration/` that pipe shell commands (`create`,
> `alter`, `describe`, `enable`, `disable`, `drop`, `major_compact`,
> `compact`, `split`, `remove_peer`, `delete_snapshot`, `list_snapshots`)
> into `/hbase/bin/hbase shell` (some with `-n`), e.g.
> `hbase/shell_operations/*.sh`, `hbase/delete_hbase_snapshots.sh`,
> `hbase/remove_custom_replication_peer.sh`, `phoenix/enable-disable-compression.sh`.
> All of the piped-in commands are already ported except `alter`-adjacent
> variants used ad hoc (`alter` itself is ported). Not itemized individually
> since they're just thin `hbase shell` callers, same pattern as section 3's
> `graceful_stop.sh`/`rolling-restart.sh` — the real dependency is `-n`/piped-stdin
> parity, already tracked in section 1.

## 6. External consumer: Ambari HBASE service management scripts

[View handling plan](EXTERNAL_CONSUMER_HANDLING_PLAN.md#ambari)

Not in this repo — these live in `~/Code/sfdc/ambari`
(`ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/`),
Apache Ambari's HBASE service-lifecycle package. Ambari's Python
management scripts (`hbase_master.py`, `hbase_regionserver.py`,
`hbase_decommission.py`) `StaticFile`-copy these `.rb` files onto the
target host, then run them with
`{hbase_cmd} --config {hbase_conf_dir} org.jruby.Main <script>.rb ...`
(and a `cmd /c ... org.jruby.Main ...` variant on Windows) — the exact
same JRuby dependency as section 2's `bin/*.rb` scripts. Same one-directional
relationship as section 5: nothing in this repo needs to change for them,
but they're a real external consumer that breaks once `bin/hbase shell`/JRuby
is actually removed. Paths below are relative to the `ambari` repo root.

- [ ] `draining_servers.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/draining_servers.rb`. HBase-1-era region drainer, invoked from `hbase_decommission.py` as `org.jruby.Main {region_drainer} add|remove <host>`.
- [ ] `draining_servers2.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/draining_servers2.rb`. HBase-2 fallback region drainer, used when the HBase-1 `draining_servers.rb` invocation fails.
- [ ] `region_mover.rb` — referenced by `hbase_regionserver.py`/`hbase_decommission.py` but not vendored in Ambari's `package/files/`; expected to already exist at `{stack_root}/current/hbase-client/bin/region_mover.rb` (i.e. this repo's `bin/region_mover.rb`, already tracked in section 2). Not a separate file to port.
- [ ] `hbase_replication.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/hbase_replication.rb`. Manages replication peers during Ambari-driven start/stop, invoked as `org.jruby.Main {hbase_replication} ...`.
- [ ] `hbase_upgrade_schema.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/hbase_upgrade_schema.rb`. Runs HBase schema upgrade during an Ambari stack upgrade; `require`s `HBaseUtil`, `phoenixUtil`, `clusterInfoUtil` from the same directory.
- [ ] `hbase_upgrade_schema_no_prescript.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/hbase_upgrade_schema_no_prescript.rb`. Same schema-upgrade entrypoint, skipping the prescript-generation step.
- [ ] `HBaseUtil.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/HBaseUtil.rb`. Shared library `require`d by both schema-upgrade scripts above; not run standalone.
- [ ] `phoenixUtil.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/phoenixUtil.rb`. Shared library `require`d by both schema-upgrade scripts above; not run standalone.
- [ ] `clusterInfoUtil.rb` — `ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/clusterInfoUtil.rb`. Shared library `require`d by both schema-upgrade scripts above; not run standalone.

## 7. What external consumers actually need from newshell

Cross-cutting inventory (built by reading every script in sections 2, 3, 5,
and 6): what shell commands vs. raw Java/Phoenix APIs do these consumers
actually call? This determines what newshell's generic surface needs to
support, as opposed to the per-script porting mechanics tracked above.

**Shell commands actually invoked: exactly one.** `balance_switch`, piped
via `shell -n` in this repo's own `bin/graceful_stop.sh` and
`bin/rolling-restart.sh` (section 3). Already ported
(`BalanceSwitchCommand.java`); fully covered by section 1's
`preserve-n-mode` item. **Nothing in k8s_hbase or Ambari pipes a single
shell command** — every one of their scripts is direct Java/JDBC interop,
`org.jruby.Main <script>.rb` used purely as "a Ruby interpreter with the
HBase classpath attached," not as a caller of the shell command layer.

**Non-shell-command APIs used (deduplicated):**

- Standard `Admin`/`Table`/`ConnectionFactory`/`Scan`/`ClusterMetrics`/
  `TableDescriptorBuilder` — k8s_hbase's `open_region_parity.rb`,
  `raw_scan_rows.rb`, `tenant_size_estimation_via_phoenix.rb`,
  `migration_validation_daemon.rb`; this repo's `region_status.rb`,
  `shutdown_regionserver.rb`, `draining_servers.rb`.
- `ZKWatcher`/`ZKUtil`/`MasterAddressTracker` — this repo's
  `draining_servers.rb`, `get-active-master.rb`; Ambari's
  `draining_servers.rb`/`draining_servers2.rb` (legacy `HBaseAdmin`/
  `ZooKeeperWatcher`).
- Deprecated `ReplicationAdmin` (`addPeer`/`removePeer`/
  `listPeerConfigs`/`getPeerState`) — Ambari's `hbase_replication.rb`.
  Maps 1:1 onto this repo's already-ported `add_peer`/`remove_peer`/
  `list_peers` commands — reclassified in
  [EXTERNAL_CONSUMER_HANDLING_PLAN.md](EXTERNAL_CONSUMER_HANDLING_PLAN.md#ambari)
  from "rewrite as driver" to "retire in favor of ported command."
- Hadoop `FileSystem`/`Path`/`CommonFSUtils`/`UserGroupInformation` —
  k8s_hbase's `check_orgstore_connectivity.rb`,
  `tenant_size_estimation_via_phoenix.rb`, `migration_validation_daemon.rb`.
- Phoenix JDBC (`DriverManager`, `PreparedStatement`, `QueryUtil`) —
  k8s_hbase's tenant-analysis scripts, `migration_validation.rb`; Ambari's
  `phoenixUtil.rb`/`hbase_upgrade_schema*.rb`.
- MR tool submission (`RowCounter`, `PhoenixSyncTableTool`,
  `VerifyReplication` via `ToolRunner`) — k8s_hbase only.
- Salesforce-proprietary (`hgrate.*`, `com.salesforce.hbase.*`,
  `com.salesforce.scoot.upgrade.UpgradeTool`, OrgStore credentials) —
  pervasive in k8s_hbase and Ambari's schema-upgrade scripts; zero
  relevance to newshell's generic surface, out of scope per
  [EXTERNAL_CONSUMER_HANDLING_PLAN.md](EXTERNAL_CONSUMER_HANDLING_PLAN.md)'s
  Principles (no consumer-specific logic in this repo).

**Net conclusion: newshell's generic API surface needs essentially no
expansion for these consumers.** The entire "shell commands used" list is
one already-ported command, handled by `-n` parity. Everything else is
either standard public API — already reachable from any `.jsh`/`.java`
driver via classpath, no newshell change required — or Salesforce-internal
tooling that's explicitly out of scope.

## 8. Commands still to port (115 remaining, plus 2 skipped)

Grouped exactly as `shell.rb`'s `load_command_group` calls group them, so
porting can proceed group-by-group with a natural test boundary per group.
Unported commands live at
`hbase-shell/src/main/ruby/shell/commands/<name>.rb`; ported commands live
at
`hbase-newshell/src/main/java/org/apache/hadoop/hbase/newshell/command/impl/<Name>Command.java`.

**Priority key:** <span style="color:#0b6623">■ dark green</span> = used directly by k8s_hbase or Ambari scripts (section 7 inventory) · <span style="color:#4caf50">■ green</span> = second-most-important (general + dml, high-traffic commands) · <span style="color:#a5d6a7">■ light green</span> = rare for us. (Colors are inline HTML and may not render on github.com's default viewer — see the Checkbox note above; they do render in most local Markdown viewers.)

### GENERAL HBASE SHELL COMMANDS (2 remaining — skipped, not planned)

- [ ] <span style="color:#4caf50">`table_help`</span> — `hbase-shell/src/main/ruby/shell/commands/table_help.rb` — **skipped/N/A**, not worth porting (interactive per-table help text, no scripted callers).
- [ ] <span style="color:#4caf50">`processlist`</span> — `hbase-shell/src/main/ruby/shell/commands/processlist.rb` — **skipped/N/A**, not worth porting (interactive process listing, no scripted callers).

> Already ported: <span style="color:#4caf50">`status`</span>, <span style="color:#4caf50">`version`</span>, <span style="color:#4caf50">`whoami`</span> —
> `.../command/impl/{Status,Version,Whoami}Command.java`.

### TABLES MANAGEMENT COMMANDS — ddl (0 remaining)

- [x] <span style="color:#a5d6a7">`disable_all`</span> — `hbase-shell/src/main/ruby/shell/commands/disable_all.rb` — completed: true. Unit-tested (`DisableAllCommandTest`) and mini-cluster-verified; corpus row added (`smoke` — the legacy shell crashes with `NoMethodError` reading its `y/n` confirmation from exhausted piped stdin under `-n`; newshell's one-shot port intentionally skips the prompt and acts directly).
- [x] <span style="color:#a5d6a7">`is_disabled`</span> — `hbase-shell/src/main/ruby/shell/commands/is_disabled.rb` — completed: true. Unit-tested (`IsDisabledCommandTest`) and mini-cluster-verified; corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`drop_all`</span> — `hbase-shell/src/main/ruby/shell/commands/drop_all.rb` — completed: true. Unit-tested (`DropAllCommandTest`); corpus row added (`smoke` — same `y/n`-confirmation-vs-piped-stdin divergence as `disable_all`).
- [x] <span style="color:#a5d6a7">`enable_all`</span> — `hbase-shell/src/main/ruby/shell/commands/enable_all.rb` — completed: true. Unit-tested (`EnableAllCommandTest`); corpus row added (`smoke` — same `y/n`-confirmation-vs-piped-stdin divergence as `disable_all`).
- [x] <span style="color:#a5d6a7">`is_enabled`</span> — `hbase-shell/src/main/ruby/shell/commands/is_enabled.rb` — completed: true. Unit-tested (`IsEnabledCommandTest`); corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`show_filters`</span> — `hbase-shell/src/main/ruby/shell/commands/show_filters.rb` — completed: true. Unit-tested (`ShowFiltersCommandTest`); corpus row added (`exact`). **Real bug found and fixed**: `ShowFiltersCommand` rendered its output as a `TabularResult`, which prints a `FILTER` header and an `N row(s)` footer the Ruby original never prints (it just calls `formatter.row` per filter, no header/footer) — changed to a plain `TextResult`.
- [x] <span style="color:#a5d6a7">`alter_status`</span> — `hbase-shell/src/main/ruby/shell/commands/alter_status.rb` — completed: true. Unit-tested (`AlterStatusCommandTest`) and mini-cluster-verified; corpus row added (`smoke` — region-update progress count is timing-sensitive).
- [x] <span style="color:#a5d6a7">`alter_async`</span> — `hbase-shell/src/main/ruby/shell/commands/alter_async.rb` — completed: true. Unit-tested (`AlterAsyncCommandTest`); corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`get_table`</span> — `hbase-shell/src/main/ruby/shell/commands/get_table.rb` — completed: true. Unit-tested (`GetTableCommandTest`); corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`locate_region`</span> — `hbase-shell/src/main/ruby/shell/commands/locate_region.rb` — completed: true. Unit-tested (`LocateRegionCommandTest`) and mini-cluster-verified (`locateRegionReturnsHostAndRegionForRowKey`); corpus row added (`smoke` — encoded region-name hash differs run to run). **Real bug found and fixed**: `DefaultShellAdmin.locateRegion` returned `RegionInfo.toString()` (the descriptive `{ENCODED => ..., NAME => ...}` dict) instead of the bare region name — changed to `getRegionNameAsString()`.
- [x] <span style="color:#a5d6a7">`list_regions`</span> — `hbase-shell/src/main/ruby/shell/commands/list_regions.rb` — completed: true. Unit-tested (`ListRegionsCommandTest`) and mini-cluster-verified; corpus row added (`smoke` — region name/size/req/locality embed live metrics).
- [x] <span style="color:#a5d6a7">`clone_table_schema`</span> — `hbase-shell/src/main/ruby/shell/commands/clone_table_schema.rb` — completed: true. Unit-tested (`CloneTableSchemaCommandTest`) and mini-cluster-verified; corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`list_enabled_tables`</span> — `hbase-shell/src/main/ruby/shell/commands/list_enabled_tables.rb` — completed: true. Unit-tested (`ListEnabledTablesCommandTest`); corpus row added (`smoke` — cluster-wide table listing, not scoped to this script).
- [x] <span style="color:#a5d6a7">`list_disabled_tables`</span> — `hbase-shell/src/main/ruby/shell/commands/list_disabled_tables.rb` — completed: true. Unit-tested (`ListDisabledTablesCommandTest`); corpus row added (`smoke` — same reasoning).

> Already ported: <span style="color:#0b6623">`alter`</span>, <span style="color:#0b6623">`create`</span>, <span style="color:#0b6623">`describe`</span>, <span style="color:#0b6623">`disable`</span>, <span style="color:#0b6623">`drop`</span>, <span style="color:#0b6623">`enable`</span>,
> `exists`, `list` — `.../command/impl/{Alter,Create,Describe,Disable,Drop,Enable,Exists,List}Command.java`.

### NAMESPACE MANAGEMENT COMMANDS (0 remaining)

- [x] <span style="color:#a5d6a7">`create_namespace`</span> — `hbase-shell/src/main/ruby/shell/commands/create_namespace.rb` — completed: true. Unit-tested (`CreateNamespaceCommandTest`) and mini-cluster-verified (`TestNamespaceCommandsAgainstMiniCluster`); corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`drop_namespace`</span> — `hbase-shell/src/main/ruby/shell/commands/drop_namespace.rb` — completed: true. Unit-tested (`DropNamespaceCommandTest`) and mini-cluster-verified; corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`alter_namespace`</span> — `hbase-shell/src/main/ruby/shell/commands/alter_namespace.rb` — completed: true. Unit-tested (`AlterNamespaceCommandTest`) and mini-cluster-verified; corpus row added (`exact`).
- [x] <span style="color:#a5d6a7">`describe_namespace`</span> — `hbase-shell/src/main/ruby/shell/commands/describe_namespace.rb` — completed: true. Unit-tested (`DescribeNamespaceCommandTest`) and mini-cluster-verified; corpus row added (`exact`). **Real bug found and fixed**: `DescribeNamespaceCommand` rendered its output as a `TabularResult`, printing a `1 row(s)` footer where the Ruby original (quotas disabled, the normal case) prints `Quota is disabled` instead — changed to a plain `TextResult` with the `DESCRIPTION` header, the descriptor row, and the `Quota is disabled` line.
- [x] <span style="color:#a5d6a7">`list_namespace`</span> — `hbase-shell/src/main/ruby/shell/commands/list_namespace.rb` — completed: true. Unit-tested (`ListNamespaceCommandTest`) and mini-cluster-verified; corpus row added (`exact`, filtered to a specific created namespace name).
- [x] <span style="color:#a5d6a7">`list_namespace_tables`</span> — `hbase-shell/src/main/ruby/shell/commands/list_namespace_tables.rb` — completed: true. Unit-tested (`ListNamespaceTablesCommandTest`) and mini-cluster-verified; corpus row added (`exact`). **Real bug found and fixed**: `DefaultShellAdmin.listNamespaceTables` returned `TableName.getNameAsString()` (the full `namespace:table` name) instead of `getQualifierAsString()` (the bare table name the Ruby `admin.rb` wrapper returns) — table names were showing the namespace prefix twice.

### DATA MANIPULATION COMMANDS — dml (0 remaining — fully ported)

> Already ported: <span style="color:#4caf50">`get`</span>, <span style="color:#0b6623">`put`</span>, <span style="color:#0b6623">`scan`</span>,
> <span style="color:#4caf50">`count`</span>, <span style="color:#4caf50">`delete`</span>, <span style="color:#4caf50">`deleteall`</span>,
> <span style="color:#4caf50">`get_counter`</span>, <span style="color:#4caf50">`incr`</span>, <span style="color:#4caf50">`truncate`</span>,
> <span style="color:#4caf50">`truncate_preserve`</span>, <span style="color:#4caf50">`append`</span>, <span style="color:#4caf50">`get_splits`</span> —
> `.../command/impl/{Get,Put,Scan,Count,Delete,Deleteall,GetCounter,Incr,Truncate,TruncatePreserve,Append,GetSplits}Command.java`.

### HBASE SURGERY TOOLS — tools (42 remaining)

- [ ] <span style="color:#a5d6a7">`assign`</span> — `hbase-shell/src/main/ruby/shell/commands/assign.rb`
- [ ] <span style="color:#a5d6a7">`balancer`</span> — `hbase-shell/src/main/ruby/shell/commands/balancer.rb`
- [ ] <span style="color:#a5d6a7">`normalize`</span> — `hbase-shell/src/main/ruby/shell/commands/normalize.rb`
- [ ] <span style="color:#a5d6a7">`is_in_maintenance_mode`</span> — `hbase-shell/src/main/ruby/shell/commands/is_in_maintenance_mode.rb`
- [ ] <span style="color:#a5d6a7">`clear_slowlog_responses`</span> — `hbase-shell/src/main/ruby/shell/commands/clear_slowlog_responses.rb`
- [ ] <span style="color:#a5d6a7">`reopen_regions`</span> — `hbase-shell/src/main/ruby/shell/commands/reopen_regions.rb`
- [ ] <span style="color:#a5d6a7">`close_region`</span> — `hbase-shell/src/main/ruby/shell/commands/close_region.rb`
- [ ] <span style="color:#a5d6a7">`flush`</span> — `hbase-shell/src/main/ruby/shell/commands/flush.rb`
- [ ] <span style="color:#a5d6a7">`flush_master_store`</span> — `hbase-shell/src/main/ruby/shell/commands/flush_master_store.rb`
- [ ] <span style="color:#a5d6a7">`get_balancer_decisions`</span> — `hbase-shell/src/main/ruby/shell/commands/get_balancer_decisions.rb`
- [ ] <span style="color:#a5d6a7">`get_balancer_rejections`</span> — `hbase-shell/src/main/ruby/shell/commands/get_balancer_rejections.rb`
- [ ] <span style="color:#a5d6a7">`get_slowlog_responses`</span> — `hbase-shell/src/main/ruby/shell/commands/get_slowlog_responses.rb`
- [ ] <span style="color:#a5d6a7">`get_largelog_responses`</span> — `hbase-shell/src/main/ruby/shell/commands/get_largelog_responses.rb`
- [ ] <span style="color:#a5d6a7">`move`</span> — `hbase-shell/src/main/ruby/shell/commands/move.rb`
- [ ] <span style="color:#a5d6a7">`merge_region`</span> — `hbase-shell/src/main/ruby/shell/commands/merge_region.rb`
- [ ] <span style="color:#a5d6a7">`unassign`</span> — `hbase-shell/src/main/ruby/shell/commands/unassign.rb`
- [ ] <span style="color:#a5d6a7">`zk_dump`</span> — `hbase-shell/src/main/ruby/shell/commands/zk_dump.rb`
- [ ] <span style="color:#a5d6a7">`wal_roll`</span> — `hbase-shell/src/main/ruby/shell/commands/wal_roll.rb`
- [ ] <span style="color:#a5d6a7">`wal_roll_all`</span> — `hbase-shell/src/main/ruby/shell/commands/wal_roll_all.rb`
- [ ] <span style="color:#a5d6a7">`hbck_chore_run`</span> — `hbase-shell/src/main/ruby/shell/commands/hbck_chore_run.rb`
- [ ] <span style="color:#a5d6a7">`catalogjanitor_run`</span> — `hbase-shell/src/main/ruby/shell/commands/catalogjanitor_run.rb`
- [ ] <span style="color:#a5d6a7">`cleaner_chore_run`</span> — `hbase-shell/src/main/ruby/shell/commands/cleaner_chore_run.rb`
- [ ] <span style="color:#a5d6a7">`cleaner_chore_switch`</span> — `hbase-shell/src/main/ruby/shell/commands/cleaner_chore_switch.rb`
- [ ] <span style="color:#a5d6a7">`cleaner_chore_enabled`</span> — `hbase-shell/src/main/ruby/shell/commands/cleaner_chore_enabled.rb`
- [ ] <span style="color:#a5d6a7">`compact_rs`</span> — `hbase-shell/src/main/ruby/shell/commands/compact_rs.rb`
- [ ] <span style="color:#a5d6a7">`compaction_state`</span> — `hbase-shell/src/main/ruby/shell/commands/compaction_state.rb`
- [ ] <span style="color:#a5d6a7">`trace`</span> — `hbase-shell/src/main/ruby/shell/commands/trace.rb`
- [ ] <span style="color:#a5d6a7">`snapshot_cleanup_switch`</span> — `hbase-shell/src/main/ruby/shell/commands/snapshot_cleanup_switch.rb`
- [ ] <span style="color:#a5d6a7">`snapshot_cleanup_enabled`</span> — `hbase-shell/src/main/ruby/shell/commands/snapshot_cleanup_enabled.rb`
- [ ] <span style="color:#a5d6a7">`clear_compaction_queues`</span> — `hbase-shell/src/main/ruby/shell/commands/clear_compaction_queues.rb`
- [ ] <span style="color:#a5d6a7">`list_deadservers`</span> — `hbase-shell/src/main/ruby/shell/commands/list_deadservers.rb`
- [ ] <span style="color:#a5d6a7">`list_liveservers`</span> — `hbase-shell/src/main/ruby/shell/commands/list_liveservers.rb`
- [ ] <span style="color:#a5d6a7">`list_unknownservers`</span> — `hbase-shell/src/main/ruby/shell/commands/list_unknownservers.rb`
- [ ] <span style="color:#a5d6a7">`clear_deadservers`</span> — `hbase-shell/src/main/ruby/shell/commands/clear_deadservers.rb`
- [ ] <span style="color:#a5d6a7">`clear_block_cache`</span> — `hbase-shell/src/main/ruby/shell/commands/clear_block_cache.rb`
- [ ] <span style="color:#a5d6a7">`stop_master`</span> — `hbase-shell/src/main/ruby/shell/commands/stop_master.rb`
- [ ] <span style="color:#a5d6a7">`stop_regionserver`</span> — `hbase-shell/src/main/ruby/shell/commands/stop_regionserver.rb`
- [ ] <span style="color:#a5d6a7">`regioninfo`</span> — `hbase-shell/src/main/ruby/shell/commands/regioninfo.rb`
- [ ] <span style="color:#a5d6a7">`rit`</span> — `hbase-shell/src/main/ruby/shell/commands/rit.rb`
- [ ] <span style="color:#a5d6a7">`truncate_region`</span> — `hbase-shell/src/main/ruby/shell/commands/truncate_region.rb`
- [ ] <span style="color:#a5d6a7">`refresh_meta`</span> — `hbase-shell/src/main/ruby/shell/commands/refresh_meta.rb`
- [ ] <span style="color:#a5d6a7">`refresh_hfiles`</span> — `hbase-shell/src/main/ruby/shell/commands/refresh_hfiles.rb`

> Already ported: <span style="color:#0b6623">`balance_switch`</span>, <span style="color:#a5d6a7">`balancer_enabled`</span>, <span style="color:#a5d6a7">`normalizer_switch`</span>,
> `normalizer_enabled`, `compact`, `compaction_switch`, `major_compact`,
> `split`, `catalogjanitor_switch`, `catalogjanitor_enabled`,
> `splitormerge_switch`, `splitormerge_enabled`,
> `list_decommissioned_regionservers`, `decommission_regionservers`,
> `recommission_regionserver` —
> `.../command/impl/{BalanceSwitch,BalancerEnabled,NormalizerSwitch,NormalizerEnabled,Compact,CompactionSwitch,MajorCompact,Split,CatalogjanitorSwitch,CatalogjanitorEnabled,SplitormergeSwitch,SplitormergeEnabled,ListDecommissionedRegionServers,DecommissionRegionServers,RecommissionRegionServer}Command.java`.

### CLUSTER REPLICATION TOOLS — replication (27 remaining)

- [ ] <span style="color:#a5d6a7">`enable_peer`</span> — `hbase-shell/src/main/ruby/shell/commands/enable_peer.rb`
- [ ] <span style="color:#a5d6a7">`disable_peer`</span> — `hbase-shell/src/main/ruby/shell/commands/disable_peer.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_replicate_all`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_replicate_all.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_serial`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_serial.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_namespaces`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_namespaces.rb`
- [ ] <span style="color:#a5d6a7">`append_peer_namespaces`</span> — `hbase-shell/src/main/ruby/shell/commands/append_peer_namespaces.rb`
- [ ] <span style="color:#a5d6a7">`remove_peer_namespaces`</span> — `hbase-shell/src/main/ruby/shell/commands/remove_peer_namespaces.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_exclude_namespaces`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_exclude_namespaces.rb`
- [ ] <span style="color:#a5d6a7">`append_peer_exclude_namespaces`</span> — `hbase-shell/src/main/ruby/shell/commands/append_peer_exclude_namespaces.rb`
- [ ] <span style="color:#a5d6a7">`remove_peer_exclude_namespaces`</span> — `hbase-shell/src/main/ruby/shell/commands/remove_peer_exclude_namespaces.rb`
- [ ] <span style="color:#a5d6a7">`show_peer_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/show_peer_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_exclude_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_exclude_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`append_peer_exclude_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/append_peer_exclude_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`remove_peer_exclude_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/remove_peer_exclude_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`set_peer_bandwidth`</span> — `hbase-shell/src/main/ruby/shell/commands/set_peer_bandwidth.rb`
- [ ] <span style="color:#a5d6a7">`list_replicated_tables`</span> — `hbase-shell/src/main/ruby/shell/commands/list_replicated_tables.rb`
- [ ] <span style="color:#a5d6a7">`append_peer_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/append_peer_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`remove_peer_tableCFs`</span> — `hbase-shell/src/main/ruby/shell/commands/remove_peer_tableCFs.rb`
- [ ] <span style="color:#a5d6a7">`enable_table_replication`</span> — `hbase-shell/src/main/ruby/shell/commands/enable_table_replication.rb`
- [ ] <span style="color:#a5d6a7">`disable_table_replication`</span> — `hbase-shell/src/main/ruby/shell/commands/disable_table_replication.rb`
- [ ] <span style="color:#a5d6a7">`get_peer_config`</span> — `hbase-shell/src/main/ruby/shell/commands/get_peer_config.rb`
- [ ] <span style="color:#a5d6a7">`list_peer_configs`</span> — `hbase-shell/src/main/ruby/shell/commands/list_peer_configs.rb`
- [ ] <span style="color:#a5d6a7">`update_peer_config`</span> — `hbase-shell/src/main/ruby/shell/commands/update_peer_config.rb`
- [ ] <span style="color:#a5d6a7">`transit_peer_sync_replication_state`</span> — `hbase-shell/src/main/ruby/shell/commands/transit_peer_sync_replication_state.rb`
- [ ] <span style="color:#a5d6a7">`peer_modification_enabled`</span> — `hbase-shell/src/main/ruby/shell/commands/peer_modification_enabled.rb`
- [ ] <span style="color:#a5d6a7">`peer_modification_switch`</span> — `hbase-shell/src/main/ruby/shell/commands/peer_modification_switch.rb`

> Already ported: <span style="color:#0b6623">`add_peer`</span>, <span style="color:#0b6623">`remove_peer`</span>, <span style="color:#0b6623">`list_peers`</span> —
> `.../command/impl/{AddPeer,RemovePeer,ListPeers}Command.java`.

### CLUSTER SNAPSHOT TOOLS — snapshots (5 remaining)

- [ ] <span style="color:#a5d6a7">`clone_snapshot`</span> — `hbase-shell/src/main/ruby/shell/commands/clone_snapshot.rb`
- [ ] <span style="color:#a5d6a7">`restore_snapshot`</span> — `hbase-shell/src/main/ruby/shell/commands/restore_snapshot.rb`
- [ ] <span style="color:#a5d6a7">`delete_all_snapshot`</span> — `hbase-shell/src/main/ruby/shell/commands/delete_all_snapshot.rb`
- [ ] <span style="color:#a5d6a7">`delete_table_snapshots`</span> — `hbase-shell/src/main/ruby/shell/commands/delete_table_snapshots.rb`
- [ ] <span style="color:#a5d6a7">`list_table_snapshots`</span> — `hbase-shell/src/main/ruby/shell/commands/list_table_snapshots.rb`

> Already ported: <span style="color:#a5d6a7">`snapshot`</span>, <span style="color:#0b6623">`delete_snapshot`</span>, <span style="color:#0b6623">`list_snapshots`</span> —
> `.../command/impl/{Snapshot,DeleteSnapshot,ListSnapshots}Command.java`.

### ONLINE CONFIGURATION TOOLS — configuration (3 remaining — none ported yet)

- [ ] <span style="color:#a5d6a7">`update_config`</span> — `hbase-shell/src/main/ruby/shell/commands/update_config.rb`
- [ ] <span style="color:#a5d6a7">`update_all_config`</span> — `hbase-shell/src/main/ruby/shell/commands/update_all_config.rb`
- [ ] <span style="color:#a5d6a7">`update_rsgroup_config`</span> — `hbase-shell/src/main/ruby/shell/commands/update_rsgroup_config.rb`

### CLUSTER QUOTAS TOOLS — quotas (10 remaining — none ported yet)

- [ ] <span style="color:#a5d6a7">`set_quota`</span> — `hbase-shell/src/main/ruby/shell/commands/set_quota.rb`
- [ ] <span style="color:#a5d6a7">`list_quotas`</span> — `hbase-shell/src/main/ruby/shell/commands/list_quotas.rb`
- [ ] <span style="color:#a5d6a7">`list_quota_table_sizes`</span> — `hbase-shell/src/main/ruby/shell/commands/list_quota_table_sizes.rb`
- [ ] <span style="color:#a5d6a7">`list_quota_snapshots`</span> — `hbase-shell/src/main/ruby/shell/commands/list_quota_snapshots.rb`
- [ ] <span style="color:#a5d6a7">`list_snapshot_sizes`</span> — `hbase-shell/src/main/ruby/shell/commands/list_snapshot_sizes.rb`
- [ ] <span style="color:#a5d6a7">`enable_rpc_throttle`</span> — `hbase-shell/src/main/ruby/shell/commands/enable_rpc_throttle.rb`
- [ ] <span style="color:#a5d6a7">`disable_rpc_throttle`</span> — `hbase-shell/src/main/ruby/shell/commands/disable_rpc_throttle.rb`
- [ ] <span style="color:#a5d6a7">`rpc_throttle_enabled`</span> — `hbase-shell/src/main/ruby/shell/commands/rpc_throttle_enabled.rb`
- [ ] <span style="color:#a5d6a7">`enable_exceed_throttle_quota`</span> — `hbase-shell/src/main/ruby/shell/commands/enable_exceed_throttle_quota.rb`
- [ ] <span style="color:#a5d6a7">`disable_exceed_throttle_quota`</span> — `hbase-shell/src/main/ruby/shell/commands/disable_exceed_throttle_quota.rb`

### SECURITY TOOLS — security (3 remaining)

- [ ] <span style="color:#a5d6a7">`list_security_capabilities`</span> — `hbase-shell/src/main/ruby/shell/commands/list_security_capabilities.rb`
- [ ] <span style="color:#a5d6a7">`revoke`</span> — `hbase-shell/src/main/ruby/shell/commands/revoke.rb`
- [ ] <span style="color:#a5d6a7">`user_permission`</span> — `hbase-shell/src/main/ruby/shell/commands/user_permission.rb`

> Only applicable with the AccessController coprocessor.
> Already ported: <span style="color:#0b6623">`grant`</span> — `.../command/impl/GrantCommand.java`.

### PROCEDURES & LOCKS MANAGEMENT — procedures (2 remaining — none ported yet)

- [ ] <span style="color:#a5d6a7">`list_procedures`</span> — `hbase-shell/src/main/ruby/shell/commands/list_procedures.rb`
- [ ] <span style="color:#a5d6a7">`list_locks`</span> — `hbase-shell/src/main/ruby/shell/commands/list_locks.rb`

### VISIBILITY LABEL TOOLS — visibility labels (6 remaining — none ported yet)

- [ ] <span style="color:#a5d6a7">`add_labels`</span> — `hbase-shell/src/main/ruby/shell/commands/add_labels.rb`
- [ ] <span style="color:#a5d6a7">`list_labels`</span> — `hbase-shell/src/main/ruby/shell/commands/list_labels.rb`
- [ ] <span style="color:#a5d6a7">`set_auths`</span> — `hbase-shell/src/main/ruby/shell/commands/set_auths.rb`
- [ ] <span style="color:#a5d6a7">`get_auths`</span> — `hbase-shell/src/main/ruby/shell/commands/get_auths.rb`
- [ ] <span style="color:#a5d6a7">`clear_auths`</span> — `hbase-shell/src/main/ruby/shell/commands/clear_auths.rb`
- [ ] <span style="color:#a5d6a7">`set_visibility`</span> — `hbase-shell/src/main/ruby/shell/commands/set_visibility.rb`

> Only applicable with the VisibilityController coprocessor.

### RSGroups — rsgroup (15 remaining)

- [ ] <span style="color:#a5d6a7">`list_rsgroups`</span> — `hbase-shell/src/main/ruby/shell/commands/list_rsgroups.rb`
- [ ] <span style="color:#a5d6a7">`add_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/add_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`remove_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/remove_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`balance_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/balance_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`move_tables_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/move_tables_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`move_namespaces_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/move_namespaces_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`move_servers_tables_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/move_servers_tables_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`move_servers_namespaces_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/move_servers_namespaces_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`get_server_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/get_server_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`get_table_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/get_table_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`remove_servers_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/remove_servers_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`rename_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/rename_rsgroup.rb`
- [ ] <span style="color:#a5d6a7">`alter_rsgroup_config`</span> — `hbase-shell/src/main/ruby/shell/commands/alter_rsgroup_config.rb`
- [ ] <span style="color:#a5d6a7">`show_rsgroup_config`</span> — `hbase-shell/src/main/ruby/shell/commands/show_rsgroup_config.rb`
- [ ] <span style="color:#a5d6a7">`get_namespace_rsgroup`</span> — `hbase-shell/src/main/ruby/shell/commands/get_namespace_rsgroup.rb`

> Already ported: <span style="color:#0b6623">`get_rsgroup`</span>, <span style="color:#0b6623">`move_servers_rsgroup`</span> — `.../command/impl/{GetRsgroup,MoveServersRsgroup}Command.java`.

### StoreFileTracker (2 remaining — none ported yet)

- [ ] <span style="color:#a5d6a7">`change_sft`</span> — `hbase-shell/src/main/ruby/shell/commands/change_sft.rb`
- [ ] <span style="color:#a5d6a7">`change_sft_all`</span> — `hbase-shell/src/main/ruby/shell/commands/change_sft_all.rb`

## 9. New scripts (not ports of any existing `.rb`) — Kubernetes lifecycle drivers

Unlike every other item in this doc, these have no `.rb` counterpart to port
— `bin/graceful_stop.sh` predates Kubernetes and models a single continuous
script that stops and restarts the RegionServer daemon itself (see section 2,
`draining_servers.rb`, and `region_mover.rb`). That model doesn't fit a pod:
the RegionServer process is the container's PID 1, so "stop the daemon then
restart it" can't be two steps of one running script — stopping the process
*is* the container exiting. Kubernetes instead fires two independent,
decoupled lifecycle events (a `preStop` hook, then — separately, later,
possibly on a different node — a fresh container start), with no live process
bridging the two; the only handoff channel is whatever state gets persisted
(the region-list file) on the pod's PVC. This only works for pods with stable
identity and storage (a StatefulSet's per-ordinal hostname + PVC) — a
`Deployment` has neither, so there's no "same server" for this pattern to
resume.

Both drivers below are built on top of already-planned section 2 scripts
(`draining_servers.jsh`, `region_mover.jsh`) rather than reimplementing
draining/move logic themselves.

- [ ] **`regionserver-prestop.jsh`** (working name; final path/name TBD) — run
  from the pod's `preStop` hook, before Kubernetes sends SIGTERM to the
  RegionServer process. Bundles `draining_servers.jsh add <pod-hostname>`
  followed by `region_mover.jsh unload -f <pvc-path> <pod-hostname>`, so the
  region-list file lands on the pod's persistent volume rather than the
  container's ephemeral filesystem. Must complete within
  `terminationGracePeriodSeconds`, which needs to be sized generously (region
  unload of a loaded RS can take minutes) — the k8s default of 30s is far too
  short and will cause Kubernetes to SIGKILL mid-drain.
- [ ] **`regionserver-poststart.jsh`** (working name; final path/name TBD) —
  named for the pairing with `-prestop.jsh`, but run as a foreground step in
  the container's own entrypoint/startup script, *after* it confirms the
  RegionServer has finished registering with the master — **not** wired to
  Kubernetes' actual `postStart` hook, which fires asynchronously with no
  such guarantee and would race `region_mover load` against the RS not yet
  being recognized as live. Bundles a drain-check-and-remove-self step
  (`draining_servers.jsh list` / `remove`) followed by
  `region_mover.jsh load -f <pvc-path> <pod-hostname>`, reloading exactly the
  regions `regionserver-prestop.jsh` recorded. Note the RS always registers
  under a brand-new startcode on every restart (Kubernetes doesn't change
  this) — the drain-check step relies on `draining_servers.jsh`'s host-only
  resolution, not an exact stale `ServerName` match.

## Suggested porting order

1. **general** + **dml** — small, high-traffic, most likely to be scripted
   against externally (matches the k8s_hbase/bigdata-at-scale-fedx scripts
   already depending on `hbase shell -n`; see memory
   `project_hbase_newshell_jruby_removal`).
2. **ddl** stragglers + **namespace** — rounds out table/namespace lifecycle
   to match what's already ported.
3. **snapshots** stragglers + **replication** stragglers — extends groups
   that are already partially ported, so formatter/admin-wrapper code can be
   reused.
4. **tools** — largest group; can be split into sub-batches (region
   assignment/move/split, WAL/compaction/cleaner chores, dead/live server
   listing) each landing with its own parity-corpus entries.
5. **quotas**, **security**, **visibility labels**, **rsgroup**,
   **procedures**, **configuration**, **storefiletracker** — lower-traffic,
   coprocessor-gated, or admin-rare groups; port last.
