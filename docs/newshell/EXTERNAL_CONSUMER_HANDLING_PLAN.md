# External consumer handling plan

Companion to [REMAINING_WORK.md](REMAINING_WORK.md) sections 5 and 6. This
doc assumes JRuby is being fully removed from the distribution (section 1's
`jruby-removal-followthrough`) and answers, script by script: **what has to
replace this external caller so it keeps working with no JRuby runtime at
all?**

## Java version

`jshell` needs Java 9+ (doesn't exist on JDK 8 at all) and single-file
`.java` launch needs Java 11+. Not a real constraint here: this repo's root
`pom.xml` already targets `compileSource`/`releaseTarget` 17, so whatever
JDK runs `bin/hbase` today already satisfies both. Neither extension is
touched by the Maven build regardless of build JDK — `bin/` scripts are
plain resource files, not compiled sources.

## Why these break

`bin/hbase org.jruby.Main <script>.rb` is a generic "run this Ruby file"
entrypoint, independent of the `bin/hbase shell` command registry. Once the
JRuby runtime is dropped from the distribution, this entrypoint disappears
entirely — every script below stops working the moment that ships,
regardless of whether it ever touched a shell command. Each one needs a
concrete replacement, not a wait-and-see.

## Principles

These hold for every script in this plan, and for section 2's `bin/*.rb`.

1. **`hbase-newshell` stays generic — no customer-specific business logic.**
   Nothing that encodes k8s_hbase's or Ambari's own workflows (migration
   validation, tenant analysis, schema-upgrade sequencing, replication
   lifecycle for their deploy process, etc.) gets added as a class inside
   this repo. Adding it would make `newshell` depend on knowledge of a
   specific consumer's operations, reversing the dependency direction:
   consumers call into `bin/hbase`, this repo doesn't call into them.

2. **Newshell's own command classes are always `.java`, never `.jsh`.**
   The command registry, the Admin/Table wrapper layer, and every ported
   shell command (`disable_all`, `scan`, `balancer`, ...) aren't scripts —
   they're loaded into the registry and invoked through the REPL/dispatch
   mechanism, the same role Ruby's `Commands::Get`/`Commands::Put` played.
   That requires a stable class hierarchy, unit testability, and code
   review/compile-time checking as first-class product code. `.jsh` (no
   class structure, no static checking, meant for throwaway/driver code)
   is never a fit here, regardless of how simple a given command is.

3. **External/driver scripts default to `.jsh`; `.java` is available when
   warranted, not mandatory.** Every k8s_hbase/Ambari script and every
   `bin/*.rb` in section 2 is, today, a standalone script — not part of the
   command registry. Its natural replacement is another standalone script,
   not a new newshell class. Default to `.jsh` (closest to today's
   `.rb`-and-go feel, no build step). Reach for `.java` (JEP 330 single-file
   launch) instead when the script is complex enough that compile-time
   checking is worth the extra step — multi-file logic, non-trivial control
   flow — or the script's own author just prefers it. Both dispatch the
   same way; see "Dispatch mechanism" below.

4. **Genuinely generic reusable scaffolding may live in newshell — as
   plain utility classes, not business logic.** If several driver scripts
   would otherwise duplicate something with zero customer-specific content
   (connection bootstrap, common CLI-arg parsing, output formatting), it's
   fine for `hbase-newshell` to expose it — but only as a generic helper
   class, the same category as the existing Admin/Table wrapper layer. The
   test: could this helper ship with zero knowledge of k8s_hbase or Ambari
   ever existing? If yes, it can live here, callable from either a `.jsh`
   or a `.java` driver. If the logic only makes sense in light of a
   specific consumer's workflow, it fails principle 1 and stays out.

5. **The actual per-script logic lives in the consumer's own repo (or in
   `bin/` for section 2), written directly against the standard public
   HBase API** — `Admin`/`Table`/`ConnectionFactory`, Phoenix JDBC — plus
   whatever generic helpers principle 4 exposes. No new
   `org.apache.hadoop.hbase.newshell.tools`-style package gets created for
   this content.

## Dispatch mechanism

`bin/hbase` gains two new dispatch rules, replacing today's single
`.rb` → `org.jruby.Main` rule (tracked in section 1 as `jsh-dispatch`):

- `<script>.jsh` → `jshell --class-path "$CLASSPATH" <script>.jsh`
- `<script>.java` → `java --class-path "$CLASSPATH" <script>.java` (JEP 330
  single-file source launch; no separate compile step needed to run it,
  though `javac <script>.java` remains available for a standalone
  compile-check)

Both give the script the same classpath `org.jruby.Main` scripts get today,
so a driver can import newshell's generic helpers (principle 4) or any
public HBase API class exactly as it would inside the shell.

## Handling strategy categories

| Category | Meaning |
| --- | --- |
| **Rewrite as `.jsh`/`.java` driver** | Default outcome for any Java-interop-only script. Rewritten in place (same repo, same call site) using the public HBase API directly, or newshell's generic helpers if genuinely generic content applies. `.jsh` unless the script's complexity makes `.java`'s compile-time checking worth it. |
| **Retire in favor of ported command** | Functionality already exists as a ported newshell command; drop the script and repoint the caller at `bin/hbase newshell <command>` instead of porting it. |
| **Needs `-n`/piped-stdin parity** | Script pipes shell commands into `bin/hbase shell` (or `shell -n`), not `org.jruby.Main`. Covered by newshell matching that invocation mode — tracked in section 1's `preserve-n-mode`, not a new item. |
| **Redirect / already tracked elsewhere** | Not a distinct porting item; resolves to a file already tracked in this repo. |
| **Ports with its consumer** | Shared library `require`d by another script in the same directory; folds into the same `.jsh`/`.java` driver as its consumer, not ported independently. |

---

## k8s_hbase

External repo: `~/Code/sfdc/k8s_hbase`, scripts under
`docker/scripts/bigdata-custom-managed-ops-scripts/`. All invoked as
`$HBASE_HOME/bin/hbase org.jruby.Main <script>.rb <args>`.

| Script | Handling |
| --- | --- |
| `migration_validation.rb` | Rewrite as `.jsh`/`.java` driver — table-size comparison via Admin/Table API, in k8s_hbase's own repo. |
| `open_region_parity.rb` | Rewrite as `.jsh`/`.java` driver — open-region comparison via Admin API. |
| `raw_scan_rows.rb` | Rewrite as `.jsh` driver — simple row-range scan, no compile-checking needed. |
| `update_global_backup_ttl.rb` | Rewrite as `.jsh` driver — config get/set via Admin API. |
| `migration_impact_analysis_row_counter.rb` | Rewrite as `.java` driver — CSV-driven row counting via Table API, complex enough to benefit from compile-time checking. |
| `fkp-oordr.rb` | Rewrite as `.jsh`/`.java` driver — replication peer setup. |
| `check_orgstore_connectivity.rb` | Rewrite as `.jsh` driver — connectivity probe via Connection API. |
| `tenant_count_analyzer.rb` | Rewrite as `.java` driver — Phoenix JDBC queries, worth compile-checking. |
| `tenant_delete_deduplication.rb` | Rewrite as `.java` driver — mutates data via Table/Scan/Delete API; the destructive path is exactly where compile-time checking earns its keep. |
| `tenant_profiler_validator.rb` | Rewrite as `.jsh`/`.java` driver — validation logic. |
| `tenant_size_estimation_via_phoenix.rb` | Rewrite as `.java` driver — Phoenix JDBC queries, same shape as `tenant_count_analyzer.rb`. |
| `migration_validation_daemon.rb` | Ports with its consumer — wraps the rewritten `migration_validation` driver's logic in a poll loop, same file family. |
| *(the many `.sh` wrappers under `hbase/`, `phoenix/`, `eks_fkp_migration/`)* | Needs `-n`/piped-stdin parity — pipe already-ported shell commands into `/hbase/bin/hbase shell -n`. Fixed by section 1's `preserve-n-mode`, no separate rewrite. |

**Net plan for k8s_hbase:** all 12 `.rb` scripts (11 + the daemon variant)
get rewritten *in k8s_hbase's own repo* as `.jsh` or `.java` drivers against
the public HBase/Phoenix API, dispatched via the same
`$HBASE_HOME/bin/hbase <script>.jsh|.java` invocation shape k8s_hbase
already uses — just a different extension. Nothing new gets added to
`hbase-newshell` for any of these; this repo's only contribution is the
dispatch mechanism itself. The `.sh` wrappers need no rewrite — they're
carried by newshell's `-n` mode.

---

## Ambari

External repo: `~/Code/sfdc/ambari`,
`ambari-server/src/main/resources/common-services/HBASE/0.96.0.2.0/package/files/`.
Invoked as `{hbase_cmd} --config {hbase_conf_dir} org.jruby.Main <script>.rb ...`
(Linux) / `cmd /c {hbase_executable} org.jruby.Main <script>.rb ...` (Windows)
from Ambari's Python service-lifecycle scripts.

| Script | Handling |
| --- | --- |
| `draining_servers.rb` | Retire in favor of ported command — repoint `hbase_decommission.py` at this repo's already-ported `decommission_regionservers`/`recommission_regionserver`/`list_decommissioned_regionservers` instead of porting the drainer logic separately. |
| `draining_servers2.rb` | Retire in favor of ported command — same target as `draining_servers.rb`; drop the HBase-2 fallback path entirely once Ambari calls the ported commands. |
| `region_mover.rb` | Redirect / already tracked elsewhere — resolves to this repo's own `bin/region_mover.rb` (section 2), handled there. |
| `hbase_replication.rb` | Retire in favor of ported command — its add/remove/list-peer logic (via the deprecated `ReplicationAdmin`) maps 1:1 onto this repo's already-ported `add_peer`/`remove_peer`/`list_peers` commands; repoint Ambari's start/stop lifecycle scripts at those instead of porting the logic separately. |
| `hbase_upgrade_schema.rb` | Rewrite as `.java` driver — schema upgrade steps via Admin API, worth compile-checking given it runs during upgrades. |
| `hbase_upgrade_schema_no_prescript.rb` | Ports with its consumer — same driver as `hbase_upgrade_schema.rb` with the prescript step skipped (a boolean/arg branch in the same file), not a separate script. |
| `HBaseUtil.rb` | Ports with its consumer — folds into the `hbase_upgrade_schema` driver. |
| `phoenixUtil.rb` | Ports with its consumer — folds into the `hbase_upgrade_schema` driver. |
| `clusterInfoUtil.rb` | Ports with its consumer — folds into the `hbase_upgrade_schema` driver. |

**Net plan for Ambari:** the two draining-servers scripts and
`hbase_replication.rb` are all dropped outright — Ambari's Python calls
this repo's already-ported decommission and replication-peer commands
(`decommission_regionservers`/`recommission_regionserver`,
`add_peer`/`remove_peer`/`list_peers`) instead of carrying forward its own
drainer/replication logic. The two schema-upgrade scripts and their three
shared libraries collapse into one `.java` driver (with/without prescript)
living in Ambari's own repo. `region_mover.rb` needs no Ambari-side work —
it's this repo's own file. As with k8s_hbase, nothing here adds a new
class to `hbase-newshell`.

---

## This also applies to section 2's `bin/*.rb`

The same principles apply to this repo's own standalone JRuby scripts
(section 2 of [REMAINING_WORK.md](REMAINING_WORK.md)) —
`region_status.rb`, `shutdown_regionserver.rb`, `copy_tables_desc.rb`,
`get-active-master.rb`, `region_mover.rb` are direct Java-interop scripts
today, no different in shape from the k8s_hbase/Ambari ones above, and they
are *not* part of the command registry (principle 2 doesn't apply to them).
Each becomes a `.jsh` or `.java` driver living at the same `bin/<name>.jsh`
/`.java` path its `.rb` predecessor occupies today, written against the
public HBase API directly. `bin/` keeps looking like `bin/` — same
filenames, same invocation shape, same "grab a script, run it against a
cluster" workflow operators already have — just `.jsh`/`.java` instead of
`.rb`. Since these live in this repo already, it's fine for genuinely
generic content shared across them (principle 4) to become a small helper
class here — but that helper must stay free of any per-script business
logic, same bar as the external repos.

## Summary

Every script in both external repos, plus section 2's own `bin/*.rb`, gets
one of four concrete outcomes once JRuby is removed: a `.jsh`/`.java`
driver rewritten in place against the public HBase/Phoenix API (11
k8s_hbase scripts + 1 daemon variant; 2 Ambari scripts + 3 shared libraries
folding into 1 driver; 5 of this repo's own `bin/*.rb`), retirement in
favor of an already-ported newshell command (2 Ambari drainer scripts + `hbase_replication.rb`), a
`-n`/piped-stdin fix that's already scoped elsewhere (k8s_hbase's `.sh`
wrappers), or no action because it's already tracked as this repo's own
file (`region_mover.rb`). All drivers dispatch through the same new
`bin/hbase <script>.jsh|.java` mechanism (section 1, `jsh-dispatch`) that
JRuby's `.rb` dispatch is being retired from. Critically: **no new class is
added to `hbase-newshell` for any consumer-specific logic** — this repo's
only contribution is the dispatch mechanism and, where truly generic,
small shared helpers.
