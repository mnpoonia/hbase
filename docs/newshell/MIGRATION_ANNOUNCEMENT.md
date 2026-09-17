# JRuby shell scripts are going away — moving to `.jsh`/`.java`

We're removing the JRuby runtime from HBase (`bin/hbase shell` cutover to
`NewShellMain`, tracked under HBASE-30250). One side effect: `bin/hbase
org.jruby.Main <script>.rb` — the entrypoint every `.rb` ops script (yours
and ours) uses — goes away with it.

**What this means for you:** almost none of your scripts actually depend
on shell commands — they use the public HBase/Phoenix Java API directly,
just through JRuby's Ruby syntax. So the fix is mechanical: rewrite each
`.rb` file as a `.jsh` (jshell script, default) or `.java` (single-file
launch, for anything complex enough to want compile-time checking) file
using the *same* API calls, same classpath. `bin/hbase` will dispatch
`.jsh`/`.java` scripts exactly like it dispatches `.rb` today — same
invocation shape, new extension.

## Example — `bin/get-active-master.rb` → `bin/get-active-master.jsh`

Before:
```ruby
include Java
java_import org.apache.hadoop.hbase.HBaseConfiguration
java_import org.apache.hadoop.hbase.zookeeper.ZKWatcher
java_import org.apache.hadoop.hbase.zookeeper.MasterAddressTracker

config = HBaseConfiguration.create
zk = ZKWatcher.new(config, 'get-active-master', nil)
begin
  puts MasterAddressTracker.getMasterAddress(zk).getHostname
ensure
  zk.close
end
```

After:
```java
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.zookeeper.ZKWatcher;
import org.apache.hadoop.hbase.zookeeper.MasterAddressTracker;

var config = HBaseConfiguration.create();
var zk = new ZKWatcher(config, "get-active-master", null);
try {
    System.out.println(MasterAddressTracker.getMasterAddress(zk).getHostname());
} finally {
    zk.close();
}

/exit
```

Run the same way: `bin/hbase get-active-master.jsh` instead of `bin/hbase
org.jruby.Main get-active-master.rb`. (Note the trailing `/exit` — without
it jshell drops into an interactive prompt after the script runs, which
you don't want in a driver script.)

## Full breakdown

Per-script handling, categorized:
[`EXTERNAL_CONSUMER_HANDLING_PLAN.md`](EXTERNAL_CONSUMER_HANDLING_PLAN.md).

Two of your scripts don't need porting at all — they're being retired in
favor of already-ported shell commands:

- `hbase_replication.rb` (Ambari) — replaced by `add_peer`/`remove_peer`/
  `list_peers`.
- `draining_servers.rb`/`draining_servers2.rb` (Ambari) — replaced by
  `decommission_regionservers`/`recommission_regionserver`.
