# zm-mailbox `store` — Comprehensive Test Coverage Prompt

This file contains a package-by-package prompt for an autonomous Claude session (e.g. Claude Co-work) to add quality JUnit tests across the **`store`** submodule of `zm-mailbox`.

The `store` submodule is large (~2,516 production Java files, ~304 existing tests). Running everything in one go is impractical — instead, the prompt is split into **15 phases**, one phase per package or group of related packages. You can:

- **Paste the whole document** to run all phases end-to-end (very long autonomous run).
- **Paste the SHARED HEADER section + a single PHASE block** to run one package at a time.

Each PHASE block is self-contained when paired with the SHARED HEADER.

---

# SHARED HEADER — include with every phase

You are writing high-quality JUnit tests for the `store` submodule of the Zimbra `zm-mailbox` repository. Quality matters more than line count. No placeholder tests, no over-mocking, no churn.

## REPO

- Working directory: `/Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/store`
- Sources: `src/java/com/zimbra/...`
- Tests: `src/java-test/com/zimbra/...` (mirror the source package)
- Build (from `store/`): `ant clean publish-local -Dzimbra.buildinfo.version=10.1.17`
- Run all tests: `ant test`
- Run one test class: `ant test -Dtestcase=com.zimbra.cs.<pkg>.<ClassName>Test`
- Sibling submodules in `zm-mailbox` (`common`, `soap`, `native`): build first if classpath is missing — from each submodule run `ant clean publish-local -Dzimbra.buildinfo.version=10.1.17`.

## JAVA / TOOLCHAIN CONSTRAINT — read carefully

- The repo targets **Java 8 bytecode**. PowerMock 1.6.5, Mockito 1.10.19, and Javassist are pinned to old versions; **they will not run on modern JDKs (17+)**.
- Use a JDK 8 installation. Verify with `java -version` and `javac -version` before running tests. If `ant test` fails with `UnsupportedClassVersionError`, `Could not initialize class javassist.*`, or `IncompatibleClassChangeError`, you are on the wrong JDK — STOP and report; do not try to "upgrade" dependencies to fix it.

## TEST STACK (REQUIRED — match existing tests)

- JUnit 4.13.2 (`org.junit.Test`, `org.junit.Before`, `org.junit.Assert`, `org.junit.BeforeClass`)
- Mockito 1.10.19 (older API: `org.mockito.Matchers` not `ArgumentMatchers`; static imports from `org.mockito.Mockito`)
- PowerMock 1.6.5: `org.powermock.api.mockito.PowerMockito`, `@PrepareForTest`, `@RunWith(PowerMockRunner.class)`, `@PowerMockIgnore("javax.management.*")`
- Reference for style/conventions: `src/java-test/com/zimbra/cs/mailbox/MailboxTest.java`, `src/java-test/com/zimbra/cs/filter/` (largest existing test corpus)

## FILE HEADER (every new test file MUST start with this)

```
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
```

(If a sibling file in the same package uses a different header, match the sibling's wording.)

## QUALITY BAR — apply to EVERY test

1. **Behavioral assertions**: assert return values, mutated state, thrown exception class AND message substring, and interactions with mocks via `verify(...)`. Never write a test whose only assertion is "no exception thrown."
2. **Branch coverage per method**: for every public method, cover happy path + each if/else branch + each declared exception path + null/empty/boundary inputs.
3. **No over-mocking**: use real DTOs, real POJOs, real enum values, real exceptions. Mock only true boundaries: `Provisioning.getInstance()`, `Mailbox`, `DbConnection`, network/HTTP/LDAP/filesystem clients, `Clock`/`System.currentTimeMillis`, static factories. Do NOT mock `ZimbraLog` unless asserting a contractually-meaningful log message (almost never).
4. **Use `MailboxTestUtil`** when a test needs a `Mailbox`, `Account`, `Provisioning`, or DB. Existing tests use it heavily — search for `MailboxTestUtil.initServer()` / `clearData()` in `src/java-test/` for setup patterns. Prefer this real-but-in-memory approach over mocking a `Mailbox`.
5. **Naming**: `methodName_condition_expectedBehavior`. Example: `addMessage_oversizedAttachment_throwsQuotaExceeded`.
6. **AAA**: clear Arrange / Act / Assert blocks. Comments only on non-obvious mock setup. No multi-line javadoc on test methods.
7. **One logical assertion per test** where practical.
8. **Imports**: no wildcards. Static-import `org.junit.Assert.*` and `org.mockito.Mockito.*`.
9. **No `@Ignore`**. If a test can't be written, report it; don't stub it.

## SKIP LIST — do NOT write tests for these

- All exception classes (`*Exception.java`)
- Pure enums — unless the enum has non-trivial logic in a method
- Constants-only files
- Generated source (anything under `build/`, `WebRoot/`, or marked auto-generated)
- `package-info.java`
- Pure interfaces with no default methods — test the impl instead
- Pure POJO classes with only getters/setters/builders — UNLESS `equals`, `hashCode`, `toString`, `compareTo`, builder validation, or factory logic exists; then write one focused test on that logic
- Third-party forks under `src/java/org/apache/mina/**` and similar — out of scope
- Anything under `src/java/com/zimbra/qa/unittest/**` — that's the integration test harness, not unit-testable

When deciding "is this a pure POJO?": look for any branches, non-trivial constructor logic, static factories, `equals`/`hashCode` overrides. If yes → test it. If no → skip.

## WORKFLOW — per phase

1. **Inventory step**: list every source file in the target package(s). For each, classify as:
   - **TEST**: needs a new test class (or test additions to an existing one)
   - **EXTEND**: existing test file in `src/java-test/...` — extend it with missing scenarios
   - **SKIP**: skip-list match (give reason)
   Report this inventory before writing any code.
2. **For each TEST/EXTEND class** (in package alphabetical order):
   1. Read the source file fully.
   2. Read any existing test for it; if present, ADD to it (never delete or rewrite tests you didn't author).
   3. Identify the class category (see CATEGORY PLAYBOOK below) to pick scenario patterns.
   4. Write tests applying the quality bar.
   5. Run `ant test -Dtestcase=<FQN>Test` until green.
3. After all classes in the phase are green individually, run full `ant test`. Ensure no pre-existing test breaks.
4. Output a phase summary: classes covered, new test count per class, production bugs found (file:line + short description), classes skipped with reasons.
5. Move to next phase.

If you discover a production bug, STOP for that class, report it, and continue with the next class without fixing the bug. Do not modify production code unless the user explicitly approves a fix.

## CATEGORY PLAYBOOK — how to test each kind of class

Pick the matching category for each class and apply its scenario pattern in addition to the universal quality bar.

### A. Service handlers (extend `com.zimbra.soap.DocumentHandler` or similar)
- **handle / handleRequest happy path**: build a real request Element, invoke, assert response Element structure (tag names, attribute values, child counts).
- **auth required**: invoke without auth → asserts `ServiceException.AUTH_REQUIRED` or equivalent.
- **invalid input**: missing required attribute, malformed value → asserts `INVALID_REQUEST`.
- **not found**: nonexistent entity id → asserts `NO_SUCH_*`.
- **permission denied**: invoke as wrong-role user → asserts `PERM_DENIED`.
- **collaborator interactions** via `ArgumentCaptor` on `Mailbox`, `Provisioning`, etc.

### B. Mailbox operations (under `com.zimbra.cs.mailbox`)
- Use `MailboxTestUtil` to set up a real in-memory mailbox.
- happy path: operation completes, assert resulting `MailItem` / folder / tag / conversation state.
- transactional rollback: force a failure mid-operation, assert state is unchanged.
- concurrent / locking edge cases if the op uses `Mailbox.lock`.
- quota / size limits: oversized input → asserts `QUOTA_EXCEEDED` or `MAIL_TOO_BIG`.

### C. MIME / parsing / formatting
- well-formed input → assert parsed structure (parts, headers, charset).
- malformed / truncated input → assert specific exception or fallback.
- encoding edge cases: UTF-8 with non-ASCII, base64 with padding variants, quoted-printable line wrap.
- empty / null input per source contract.

### D. Protocol handlers (IMAP, POP3, LMTP, Milter, MAILER)
- state machine transitions: assert valid commands per state, invalid commands rejected per state.
- successful command → expected response code + data.
- error responses formatted correctly.
- timeout / disconnect handling if observable.
- avoid real sockets — mock the I/O layer.

### E. DAV resources (CalDAV / CardDAV under `com.zimbra.cs.dav`)
- XML response shape (DAV namespace, proper status codes 200/207/404/412).
- precondition failures: If-Match, If-None-Match, ETag handling.
- collection vs leaf resource branching.
- malformed XML body → 400.

### F. Index / search
- Query builder: assert generated Lucene query string for typical / edge inputs.
- Sorter / result transformer: assert ordering, paging, truncation.
- Special characters in query: properly escaped.
- Empty corpus → empty result, not exception.

### G. Filter / Sieve (under `com.zimbra.cs.filter`)
- Script parsing: valid script → expected AST / rule list.
- Script execution: action triggered when condition true, not triggered when false.
- Recursion / loop guards: pathological scripts terminate.

### H. Store / Volume / Blob
- Round-trip: store → retrieve → assert bytes-equal.
- Missing blob → expected exception.
- Truncated blob / wrong digest → expected exception.
- Volume offline / permission errors mocked at the FS boundary.

### I. Util / helper / static
- Pure-logic methods: test every branch. Boundary inputs (null, empty, max/min int, leap year, DST transitions, very long strings).
- Static state / caches: assert cache hit, cache miss, cache invalidation.

### J. Session / Listener / Observer
- Event fire → handler called with expected args (verify).
- Exception in handler: swallowed vs propagated per contract.
- Subscriber registration / deregistration.

### K. Provisioning / Account / LDAP
- Use `MockProvisioning` (existing test utility) or real LDAP fake if available; do NOT hit a real LDAP server.
- Each attr getter/setter with branching logic.
- Validation: bad value → exception with attr name in message.
- LDAP filter escape correctness — security critical.

### L. Redo log / recovery
- Operation serialize → deserialize → assert round-trip equality.
- Replay applied to a fresh mailbox → final state matches direct application.
- Truncated / corrupted log entry → expected exception.

## CONSTRAINTS — DO NOT VIOLATE

- Do NOT modify production code unless reporting a bug and the user approves a fix. `src/java/` is read-only for this task.
- Do NOT modify `ivy.xml`, `test-ivy.xml`, or `build.xml`.
- Do NOT introduce JUnit 5 syntax.
- Do NOT add new dependencies.
- Do NOT use `@Ignore`.
- Do NOT delete or rewrite existing tests.
- Do NOT refactor production code "while you're there."
- Do NOT write tests that only assert "no exception thrown."
- Do NOT mock `ZimbraLog` unless asserting a contractual log message.
- Do NOT hit real network services, real LDAP, real DB, or real filesystem outside of the test temp dir. Use `MailboxTestUtil` for in-memory infrastructure.

---

# PACKAGE ROADMAP — current coverage snapshot

Counts are approximate (src files / existing test files). Use this to pick which phase to run if not running end-to-end.

| Phase | Package(s) | Src | Tests | Gap | Priority reason |
|---|---|---|---|---|---|
| 1 | `cs.account` | 376 | 10 | huge | Provisioning, auth, identity — security-critical and largely untested |
| 2 | `cs.mailbox` | 149 | 32 | medium | Core data layer; many ops still uncovered |
| 3 | `cs.service` | 514 | 53 | huge | SOAP API surface; biggest single package; split into 3 sub-phases by sub-package |
| 4 | `cs.db` | 39 | 5 | medium | Persistence; data integrity |
| 5 | `cs.index` | 130 | 33 | medium | Search; performance-sensitive |
| 6 | `cs.imap`, `cs.pop3`, `cs.lmtpserver`, `cs.milter` | 102 | 21 | medium | Protocol handlers |
| 7 | `cs.dav`, `cs.mailclient` | 144 | 4 | huge | DAV protocol, mail client — barely tested |
| 8 | `cs.filter`, `cs.html` | 128 | 48 | small (filter) / medium (html) | Sieve filters well-covered already; HTML sanitization is security-critical |
| 9 | `cs.redolog`, `cs.store`, `cs.volume` | 172 | 38 | medium | Recovery / blob storage |
| 10 | `cs.client`, `cs.ldap`, `cs.gal`, `cs.ephemeral`, `cs.datasource` | 220 | 25 | huge (client) | Client API + identity stores |
| 11 | `cs.session`, `cs.listeners`, `cs.iochannel`, `cs.extension`, `cs.zimlet`, `cs.server`, `cs.servlet` | 105 | 17 | medium | Session/dispatch/extensions |
| 12 | `cs.mime`, `cs.octosync`, `cs.purge`, `cs.convert`, `cs.doc`, `cs.wiki` | 56 | 13 | small | Content handling |
| 13 | `cs.security`, `cs.smime`, `cs.httpclient`, `cs.io`, `cs.localconfig`, `cs.fb`, `cs.stats` | 47 | 0 | total | Zero-test packages |
| 14 | `cs.util`, `cs.rmgmt`, `cs.zookeeper`, `cs.script`, `cs.memcached`, `cs.object`, `cs.zclient` | 87 | 10 | medium | Utilities and infra |
| 15 | `com.zimbra.util`, `com.zimbra.soap` (non-generated only) | varies | varies | varies | Cross-cutting helpers |

Packages skipped entirely: `org.apache.mina.**` (third-party fork), `com.zimbra.qa.unittest.**` (integration harness).

---

# PHASE 1 — `cs.account` (376 src / 10 tests)

Goal: cover the provisioning, auth, identity, and account-attribute logic.

Sub-areas to inventory (under `com.zimbra.cs.account`):
- Root package: `Account`, `AccountServiceException`, `Provisioning`, `LdapProvisioning`, `MockProvisioning`, `Cos`, `Domain`, `Server`, `DistributionList`, `DynamicGroup`, `Identity`, `DataSource`, `Signature`, `Alias`, `Zimlet`, etc.
- Subpackages: `auth`, `accesscontrol`, `cache`, `callback`, `gal`, `grouphandler`, `krb5`, `ldap`, `names`, `oauth2`, `soap`, `ya`, etc.

For each:
- **`Provisioning` subclasses & `*ProvisioningExt` callbacks**: each public method, normal path + error path; argument validation.
- **`AttributeManager` / `AttributeInfo` / `AttributeCallback`**: schema validation logic; bad-value rejection.
- **Access control (`accesscontrol`)**: grant/check/revoke for each right type; right inheritance; chained grants.
- **Auth (`auth`)**: each auth provider (`ZimbraAuth`, `LdapAuth`, etc.) — success / wrong-password / locked / expired / two-factor / fallback.
- **`AuthToken` / token lifecycle**: generation, verification, expiry, revocation.
- **Names (`names`)**: email-address parsing/validation, IDN edge cases, length limits.
- **Cache (`cache`)**: hit / miss / invalidate / size-bound eviction.

PowerMock is required for `Provisioning.getInstance()` and `LdapClient` statics in many files.

Use `MockProvisioning` and `MailboxTestUtil` whenever possible instead of mocking.

Run inventory step first; expect ~250–300 testable classes after the skip list. Group output by sub-area in the summary.

---

# PHASE 2 — `cs.mailbox` (149 src / 32 tests)

Goal: increase coverage of mailbox operations and `MailItem` subclasses.

Sub-areas:
- Root: `Mailbox`, `MailItem`, `Folder`, `Message`, `Conversation`, `Contact`, `CalendarItem`, `Appointment`, `Task`, `Tag`, `Note`, `Comment`, `Document`, `Mountpoint`, `WikiItem`, `SearchFolder`, `VirtualConversation`, etc.
- `acl`, `alerts`, `calendar` subpackages.
- Operations under `mailbox/` such as add/move/copy/delete/rename for each `MailItem` subclass.

For each `MailItem` subclass:
- Construction from `UnderlyingData`: assert fields populated correctly.
- `equals`/`hashCode`/`toString` if overridden.
- Encode/decode (metadata serialization): round-trip equality.
- ACL grant/check methods.

For `Mailbox`:
- Lock/unlock semantics — `MailboxLockTest` is a reference.
- Add/get/delete operations: real in-memory mailbox via `MailboxTestUtil`.
- Concurrent operation safety where observable.

Calendar:
- Recurrence rule expansion — boundary dates, DST transitions, leap years, UNTIL vs COUNT.
- Exception instances (overrides on a recurring series).
- Time zone conversions.

Extend (do not replace) the existing 32 tests.

---

# PHASE 3 — `cs.service` (514 src / 53 tests) — SPLIT INTO 3A / 3B / 3C

This is the largest package. Split by sub-package to keep each run tractable.

## PHASE 3A — `cs.service.mail` (mail SOAP handlers)
Each handler under `com.zimbra.cs.service.mail` extends a `DocumentHandler`. Apply CATEGORY A from the playbook. Common scenarios:
- handle happy path: build request `Element`, invoke `handle(request, context)`, assert response shape.
- AUTH_REQUIRED, INVALID_REQUEST, PERM_DENIED, NO_SUCH_* error paths.
- For batch / bulk handlers, partial-failure path: some items succeed, some fail, response contains both.

## PHASE 3B — `cs.service.admin` (admin SOAP handlers)
Same as 3A but for admin requests. Additional:
- Admin-only auth: non-admin caller → PERM_DENIED.
- Delegated admin: scoped rights, target outside scope → PERM_DENIED.

## PHASE 3C — Remaining: `cs.service.account`, `cs.service.formatter`, `cs.service.servlet`, `cs.service.util`, root `cs.service`
- Formatters (HTML, ICS, VCF, RSS, ATOM, JSON exporters): round-trip a known mailbox content → asserted output.
- Servlets: HTTP method dispatch, auth filter behavior, response codes.
- Root utilities: helper methods, each branch.

---

# PHASE 4 — `cs.db` (39 src / 5 tests)

Goal: cover SQL builders, schema upgraders, connection-pool logic.

For SQL helpers (`DbMailbox`, `DbMailItem`, `DbSearch`, etc.):
- Each query-building method: assert generated SQL string (param placeholders correct) for typical and edge inputs.
- Parameter binding: number, ordering.
- Empty result vs single-row vs multi-row handling.

For schema versioning:
- Each version-step migration: pre-state → run → post-state matches expected (use HSQLDB in-memory, schema scripts under `src/db/hsqldb/`).

For connection pool:
- Acquire / release / leak detection.
- Pool exhaustion: assert blocking behavior or exception per contract.

---

# PHASE 5 — `cs.index` (130 src / 33 tests)

Goal: cover Lucene query construction, sorting, result handling.

For query builders:
- Term, phrase, range, boolean (AND/OR/NOT) combinations.
- Field-specific queries (subject, from, to, in:folder, has:attachment, etc.).
- Special characters: properly escaped in the produced Lucene query.
- Wildcard / fuzzy / proximity edge cases.

For sort / paginate:
- Each sort dimension (date, score, size, sender).
- Pagination cursor stability across queries.
- Empty result, single result, max-result boundary.

For analyzers / tokenizers:
- Tokenization of CJK / unicode / mixed text.
- Stop-word handling.

Mock the actual `IndexReader` / `IndexSearcher` only at the boundary; test transform logic with real Lucene where the index is small.

---

# PHASE 6 — `cs.imap` (67/17), `cs.pop3` (11/2), `cs.lmtpserver` (18/1), `cs.milter` (6/1)

Goal: protocol handler coverage. Apply CATEGORY D.

For each command class (`ImapHandler.do*`, `Pop3Handler.do*`, etc.):
- Valid arguments → success response.
- Invalid arguments (wrong arity, bad type) → protocol error response.
- Pre-condition violations (auth required, wrong state) → state error.
- State transitions verified.

Mock the socket / channel boundary. Use byte/string assertions on the produced response bytes.

For LMTP / Milter specifically:
- Each callback (`HELO`, `MAIL FROM`, `RCPT TO`, `DATA`, etc.): success / temp failure / perm failure.
- Quota enforcement.
- Recipient validation against `Provisioning`.

---

# PHASE 7 — `cs.dav` (77/1), `cs.mailclient` (67/3)

Goal: DAV protocol handlers + IMAP/SMTP client code.

## DAV (CATEGORY E)
For each resource and method handler:
- PROPFIND with `prop`, `propname`, `allprop` — expected multistatus response.
- REPORT (CalDAV `calendar-query`, `calendar-multiget`; CardDAV equivalents).
- PUT precondition handling (If-Match, If-None-Match, ETag mismatch → 412).
- DELETE on collection vs leaf.
- MKCOL / MKCALENDAR.
- ACL handling.

## mailclient
For IMAP / SMTP / POP3 client classes:
- Command-response cycles: send command → assert wire format, parse known response.
- Connection lifecycle: connect / TLS upgrade / auth / disconnect.
- Failure paths: connection refused, timeout, bad response.

Mock the socket — never hit a real server.

---

# PHASE 8 — `cs.filter` (99/45), `cs.html` (29/3)

## filter (CATEGORY G)
Existing coverage is the best in the repo (45 tests). Focus on gaps:
- Inventory which `*.java` files in `cs.filter/**` have NO matching test, prioritize those.
- Each `Action*` class: action triggered correctly, idempotency, ordering across multiple actions.
- Each `Match*` / condition: true/false for representative inputs.
- Sieve script parse errors: line/column reported.

## html (security-critical)
HTML sanitization:
- Each malicious-pattern test: `<script>`, `javascript:`, `data:`, `vbscript:`, on-event attributes, CSS expressions, SVG payloads, base64-encoded payloads, double-encoded payloads.
- Tag whitelist enforcement.
- URL rewriting / blocking.
- Entity normalization.

---

# PHASE 9 — `cs.redolog` (116/14), `cs.store` (51/22), `cs.volume` (5/2)

## redolog (CATEGORY L)
- Each `*Op` operation under `redolog/op/`: serialize → deserialize → assert equality.
- Replay: apply to fresh mailbox → final state == direct apply.
- Header / version handling: read older-version log → upgrades cleanly or errors clearly.
- Truncated stream → expected exception with offset.

## store / volume (CATEGORY H)
- File / external store: round-trip blob → digest match.
- Volume rotation / fallback to secondary on primary failure.
- Compression / encryption layers if present: round-trip preserves bytes.
- Missing volume / wrong volume id → expected exception.

---

# PHASE 10 — `cs.client` (86/0), `cs.ldap` (52/4), `cs.gal` (14/1), `cs.ephemeral` (32/6), `cs.datasource` (36/4)

`cs.client` has ZERO tests — be thorough but apply the POJO skip rule aggressively (many client classes are pure DTOs).

## client
- Each non-trivial method on `ZMailbox`, `ZFolder`, `ZContact`, etc.: builds the right request, parses the right response.
- Use a fake SOAP transport (mock `SoapTransport.invoke`).

## ldap (CATEGORY K — security-critical)
- Filter escape: every special char (`(`, `)`, `*`, `\`, NUL, high-byte UTF-8).
- DN parsing / canonicalization.
- Search result paging.
- LDAP connection pool acquire/release.

## gal
- Search result parsing / filtering.
- Gal sync token round-trip.

## ephemeral
- Each backend (`InMemory`, `Ldap`, `SSDB`): put/get/delete/range round-trip.
- Expiry handling.

## datasource
- Each `DataSource` subclass (`PopDataSource`, `ImapDataSource`, etc.) — sync logic, error recovery, partial-success handling. Mock the network protocol.

---

# PHASE 11 — `cs.session` (23/1), `cs.listeners` (7/0), `cs.iochannel` (7/0), `cs.extension` (10/5), `cs.zimlet` (17/3), `cs.server` (17/4), `cs.servlet` (24/4)

Apply CATEGORY J for sessions/listeners.

## session
- Session create / lookup / expire / cleanup.
- Notification dispatch to subscribers.
- Concurrent session access where observable.

## listeners
- Each registered listener: fire event → assert handler invoked with expected args.

## iochannel
- Channel send / receive / close — mock at the I/O boundary.

## extension / zimlet
- Extension load lifecycle: register, init, shutdown.
- Zimlet config parsing.

## server / servlet
- Servlet doGet/doPost: dispatch on path, auth filter, response codes.
- Server start/stop sequence.

---

# PHASE 12 — `cs.mime` (30/9), `cs.octosync` (14/4), `cs.purge` (3/0), `cs.convert` (4/0), `cs.doc` (1/0), `cs.wiki` (4/0)

## mime (CATEGORY C)
- Parser: well-formed / malformed / nested / multipart / signed / encrypted.
- Header decoding: RFC 2047 encoded-word, line folding.
- Content-Type parameter parsing.

## octosync
- Sync state machine: each transition.
- Delta encoding / decoding round-trip.

## purge / convert / doc / wiki
- Small packages — inventory first; many files may be skipped as POJOs. Cover the few with branching logic.

---

# PHASE 13 — Zero-test packages: `cs.security` (12), `cs.smime` (1), `cs.httpclient` (2), `cs.io` (1), `cs.localconfig` (1), `cs.fb` (11), `cs.stats` (10)

All zero existing tests. Be careful with the test stack — no reference test exists in the package, so look at the closest related package's tests.

## security (CRITICAL — security)
- Each crypto helper: round-trip / wrong key / corrupted input / null input.
- Token / signature verification: valid / tampered / expired.

## smime
- Sign / verify / encrypt / decrypt round-trip.

## httpclient
- Request build / response parse.
- Retry / redirect handling.

## fb (free/busy)
- Lookup happy path / no-data / cross-domain federation.

## stats
- Counter increment / snapshot / reset.
- Histogram bucket boundaries.

---

# PHASE 14 — `cs.util` (64/10), `cs.rmgmt` (6/0), `cs.zookeeper` (2/0), `cs.script` (2/0), `cs.memcached` (2/0), `cs.object` (3/0), `cs.zclient` (1/0)

## util (CATEGORY I)
- Each helper class: every branch + boundaries.

## rmgmt / zookeeper / script / memcached / object / zclient
- Inventory first; many of these are thin wrappers around external services. Mock the boundary; test the wrapper's transformation logic.

---

# PHASE 15 — `com.zimbra.util` and `com.zimbra.soap` (non-generated only)

## com.zimbra.util
- Inventory the files. Apply CATEGORY I.

## com.zimbra.soap
- Many classes here are JAXB-generated request/response types — apply the POJO skip rule.
- Test only classes with `equals`/`hashCode`, builder validation, or non-trivial factory logic.

---

# FINAL DELIVERABLE

After the last phase you run, output:

1. Per-phase test count.
2. Total new test count across all phases run.
3. Production bugs surfaced (list with `file:line` and short description).
4. Classes you skipped beyond the skip-list, with reasons.
5. Confirmation that `ant test` is fully green at the end of each phase.
6. Coverage snapshot if a coverage tool is available; otherwise, the test count delta.

# REMINDERS

- Java 8 toolchain.
- JUnit 4.13.2 + Mockito 1.10.19 + PowerMock 1.6.5.
- Use `MailboxTestUtil` rather than mocking the world.
- Production code is read-only.
- Quality bar applies to every test.
- Run `ant test` after each class. Run full `ant test` after each phase.

Begin with Phase 1 unless the user specifies a different phase.
