# Syntax comparison: old hbase-shell vs. newshell

For each pilot command, three equivalent forms of the same call:

1. **Old syntax** — real hbase-shell (JRuby), from `hbase-shell/src/main/ruby/shell/commands/*.rb` docstrings.
2. **New syntax** — newshell's Ruby-hash-compatible grammar (`ShellLineParser`'s `hashLiteral`/`bareHashEntries`), the direct port of (1).
3. **Native flag syntax** — newshell's own `--FLAG=value` CLI-style grammar (`Lexer.java`'s `FLAG` token), closer to a conventional CLI than to Ruby.

(2) and (3) both work in newshell today and populate the same `options` map — pick whichever reads better; they are not a staged migration, just two accepted spellings for the same pilot commands.

Hash-literal keys (2) are case-sensitive, matching real Ruby — `NAME`, not `name`. Native flag keys (3) are upper-cased by the parser, so `--versions=3` and `--VERSIONS=3` are equivalent.

**Unsupported options fail loudly.** `get`, `put`, `scan`, `count` and `deleteall` only accept the options shown below; anything else (`RAW` on `get`, `TTL` on `scan`, a non-string `FILTER`, ...) raises an error instead of being ignored.

**Option parity with old-shell.** `get` accepts `COLUMN`/`COLUMNS`, bare columns (`get 't1','r1','c1','c2'` or `['c1','c2']`), `VERSIONS`, `TIMESTAMP`, `TIMERANGE`, `FILTER`, `MAXLENGTH`, `FORMATTER`, `FORMATTER_CLASS`, `ATTRIBUTES`, `AUTHORIZATIONS`, `CONSISTENCY` and `REGION_REPLICA_ID`. `scan` adds `STARTROW`, `STOPROW`, `ROWPREFIXFILTER`, `LIMIT`, `CACHE`, `CACHE_BLOCKS`, `REVERSED`, `RAW` (delete markers print as `type=...`), `BATCH`, `MAX_RESULT_SIZE`, `ISOLATION_LEVEL`, `READ_TYPE` and `ALLOW_PARTIAL_RESULTS`. `put` accepts `TIMESTAMP`, `ATTRIBUTES`, `VISIBILITY` and `TTL`. Per-column converters (`cf:q:toInt`, `cf:q:c(pkg.Class).method`) work in `get` and `scan`. `scan` also supports `ALL_METRICS => true` / `METRICS => ['RPC_CALLS', ...]` (a `METRIC VALUE` table after the rows; a `metrics` object in JSON), and `hbase:meta` region info / server start code print like the legacy shell.

**Errors in machine formats.** With `--output json` a failure is `{"status":"error","error":"..."}`; with `--output csv` it is a `status,error` header plus an `error,<message>` row. Text mode keeps `ERROR: <message>`.

**`alter` and `create` cover the legacy option set.** `create` accepts every table and family attribute of the old shell (including `NORMALIZER_TARGET_*`, `SPLIT_POLICY`, `FLUSH_POLICY`, `ERASURE_CODING_POLICY`, `ENCRYPTION*`, `COMPRESSION_COMPACT*`, `STORAGE_POLICY`, ...). `alter` modifies a family or adds it when missing, drops one with `METHOD => 'delete'` or `'delete' => 'f1'`, sets table attributes (`MAX_FILESIZE`, `READONLY`, ...), sets `COPROCESSOR` (spec string or hash), and supports `table_att_unset`, `table_conf_unset`, `table_remove_coprocessor` and `REOPEN_REGIONS`. Differences: a bare family name in `alter` only adds the family if missing (the old shell would reset an existing family to defaults), and `create` does not print the duplicate-family warning.

## create

```
# old
hbase> create 'ns1:t1', {NAME => 'f1', VERSIONS => 5}

# new (hash literal)
newshell> create 'ns1:t1', {NAME => 'f1', VERSIONS => 5}

# native flags
newshell> create 'ns1:t1' --NAME=f1 --VERSIONS=5
```

Multiple families, bareword shorthand (both forms supported, same as old):
```
hbase>    create 't1', {NAME => 'f1'}, {NAME => 'f2'}, {NAME => 'f3'}
hbase>    create 't1', 'f1', 'f2', 'f3'
newshell> create 't1', {NAME => 'f1'}, {NAME => 'f2'}, {NAME => 'f3'}
newshell> create 't1', 'f1', 'f2', 'f3'
```
Native flags have no multi-family equivalent — a flag is one key in one flat map, so
`--NAME=f1 --NAME=f2` just overwrites itself. Use the bareword or hash-literal form for
more than one family.

SPLITS (array value — flags can carry arrays too, since a flag's value parses through
the same grammar as a hash value):
```
hbase>    create 't1', 'f1', SPLITS => ['10', '20', '30', '40']
newshell> create 't1', 'f1', SPLITS => ['10', '20', '30', '40']
newshell> create 't1' --NAME=f1 "--SPLITS=['10', '20', '30', '40']"
```
(Quote the whole `--SPLITS=...` token in a real shell so `[`/`]`/`,` don't get mangled by
your OS shell before newshell ever sees them.)

## alter

```
# old / new (identical)
hbase>    alter 't1', NAME => 'f1', VERSIONS => 5
newshell> alter 't1', NAME => 'f1', VERSIONS => 5

# native flags
newshell> alter 't1' --NAME=f1 --VERSIONS=5
```

Delete a column family:
```
hbase>    alter 'ns1:t1', NAME => 'f1', METHOD => 'delete'
newshell> alter 'ns1:t1', NAME => 'f1', METHOD => 'delete'
newshell> alter 'ns1:t1' --NAME=f1 --METHOD=delete
```

## get

```
# old / new (identical)
hbase>    get 't1', 'r1', {COLUMN => 'c1', TIMESTAMP => ts1, VERSIONS => 4}
newshell> get 't1', 'r1', {COLUMN => 'c1', TIMESTAMP => ts1, VERSIONS => 4}

# native flags
newshell> get 't1' 'r1' --COLUMN=c1 --TIMESTAMP=ts1 --VERSIONS=4
```

FILTER (string grammar, `org.apache.hadoop.hbase.filter.ParseFilter`, HBASE-4176):
```
hbase>    get 't1', 'r1', {FILTER => "ValueFilter(=, 'binary:abc')"}
newshell> get 't1', 'r1', {FILTER => "ValueFilter(=, 'binary:abc')"}
newshell> get 't1' 'r1' "--FILTER=ValueFilter(=, 'binary:abc')"
```

TIMERANGE:
```
hbase>    get 't1', 'r1', {TIMERANGE => [ts1, ts2]}
newshell> get 't1', 'r1', {TIMERANGE => [ts1, ts2]}
newshell> get 't1' 'r1' "--TIMERANGE=[ts1, ts2]"
```

## put

```
# old / new (identical)
hbase>    put 't1', 'r1', 'c1', 'value', ts1
newshell> put 't1', 'r1', 'c1', 'value', ts1

# native flags (positional args stay positional; only the trailing options hash becomes flags)
newshell> put 't1' 'r1' 'c1' 'value' ts1
```
`put` options in flag form:
```
hbase>    put 't1', 'r1', 'c1', 'value', ts1, {ATTRIBUTES => {'mykey' => 'myvalue'}}
newshell> put 't1', 'r1', 'c1', 'value', ts1, {ATTRIBUTES => {'mykey' => 'myvalue'}}
newshell> put 't1' 'r1' 'c1' 'value' ts1 --VISIBILITY='PRIVATE|SECRET' --TTL=5000
```

## scan

```
# old / new (identical)
hbase>    scan 't1', {COLUMNS => ['c1', 'c2'], LIMIT => 10, STARTROW => 'xyz'}
newshell> scan 't1', {COLUMNS => ['c1', 'c2'], LIMIT => 10, STARTROW => 'xyz'}

# native flags
newshell> scan 't1' "--COLUMNS=['c1', 'c2']" --LIMIT=10 --STARTROW=xyz
```

Formatting and read options:
```
hbase>    scan 't1', {COLUMNS => ['f:n:toInt'], MAXLENGTH => 30, RAW => true, VERSIONS => 10}
newshell> scan 't1', {COLUMNS => ['f:n:toInt'], MAXLENGTH => 30, RAW => true, VERSIONS => 10}
newshell> scan 't1' "--COLUMNS=['f:n:toInt']" --MAXLENGTH=30 --RAW --VERSIONS=10
```

FILTER + TIMERANGE together:
```
hbase>    scan 't1', {FILTER => "PrefixFilter('row')", TIMERANGE => [100, 200]}
newshell> scan 't1', {FILTER => "PrefixFilter('row')", TIMERANGE => [100, 200]}
newshell> scan 't1' "--FILTER=PrefixFilter('row')" "--TIMERANGE=[100, 200]"
```

## count

```
# old / new (identical; note old-shell also accepts these as *bare*
# trailing KEY => value pairs with no braces - newshell matches that too)
hbase>    count 't1', INTERVAL => 100000
newshell> count 't1', INTERVAL => 100000

# native flags
newshell> count 't1' --INTERVAL=100000
```

```
hbase>    count 'ns1:t1', CACHE_BLOCKS => true
newshell> count 'ns1:t1', CACHE_BLOCKS => true
newshell> count 'ns1:t1' --CACHE_BLOCKS=true
```

FILTER:
```
hbase>    count 't1', FILTER => "(QualifierFilter (>=, 'binary:xyz'))"
newshell> count 't1', FILTER => "(QualifierFilter (>=, 'binary:xyz'))"
newshell> count 't1' "--FILTER=(QualifierFilter (>=, 'binary:xyz'))"
```

## delete / deleteall

```
# old / new (identical)
hbase>    delete 't1', 'r1', 'c1', ts1
newshell> delete 't1', 'r1', 'c1', ts1
newshell> delete 't1' 'r1' 'c1' ts1
```

ROWPREFIXFILTER + CACHE (batched range delete):
```
hbase>    deleteall 't1', {ROWPREFIXFILTER => 'prefix', CACHE => 100}
newshell> deleteall 't1', {ROWPREFIXFILTER => 'prefix', CACHE => 100}
newshell> deleteall 't1' --ROWPREFIXFILTER=prefix --CACHE=100
```

## disable_all / enable_all

Old-shell's confirmation is purely interactive (`y/N` prompt) with no inline-skip flag.
newshell adds one (`--YES`), since there's no TTY to prompt against in a scripted/CI
context:

```
# old (always interactive)
hbase>    disable_all 't.*'
          # ... prompts "Disable tables matching 't.*'? (y/N)"

# new - same interactive prompt, or skip it:
newshell> disable_all 't.*'
newshell> disable_all 't.*' --YES
```

There is no hash-literal form of `--YES` in old-shell to compare against (it's a newshell
addition, documented in `DisableAllCommand`'s own javadoc) - this is the one case in this
document where the "native flag" column has no old/new-hash equivalent at all.

## General native-flag notes

- Bare `--FLAG` with no `=value` means `true` (boolean flag) - e.g. `--YES` above, or
  `scan 't1' --REVERSED`.
- A flag's value parses through the exact same grammar as a hash-literal value, so it can
  be a string, number, array (`--SPLITS=['10','20']`), or even a nested hash
  (`--COPROCESSOR={CLASSNAME => '...', JAR_PATH => '...'}`) - just quote the whole
  `--FLAG=...` token in your OS shell so brackets/braces/commas survive.
- Flags and hash-literal/bare-hash syntax can be freely mixed on the same command line
  (`ShellLineParser.parseCommand`'s main loop dispatches on token type per-argument) -
  e.g. `get 't1' 'r1' {COLUMN => 'c1'} --VERSIONS=4` works, though mixing styles in one
  call is not recommended for readability.
