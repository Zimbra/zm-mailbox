# zm-mailbox `store` → `com.zimbra.cs.account` — Focused Test Coverage Prompt

This file contains a focused prompt for adding quality JUnit tests to the **`com.zimbra.cs.account`** package within the `store` submodule of `zm-mailbox`.

The `cs.account` package contains ~376 source files with only ~10 existing tests, covering critical provisioning, authentication, and identity logic. This prompt focuses exclusively on this package to achieve comprehensive coverage.

**MANDATORY RULE**: All tests must be **FULL FUNCTIONAL TESTS** — not shallow unit tests or mock-heavy snapshots. See QUALITY BAR section (items 10-18) for definitions and requirements. Agents MUST follow these rules strictly before beginning.

Use this document as-is — all guidance below applies to the account package only.

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

### Universal Requirements

1. **Behavioral assertions**: assert return values, mutated state, thrown exception class AND message substring, and interactions with mocks via `verify(...)`. Never write a test whose only assertion is "no exception thrown."
2. **Branch coverage per method**: for every public method, cover happy path + each if/else branch + each declared exception path + null/empty/boundary inputs.
3. **No over-mocking**: use real DTOs, real POJOs, real enum values, real exceptions. Mock only true boundaries: `Provisioning.getInstance()`, `Mailbox`, `DbConnection`, network/HTTP/LDAP/filesystem clients, `Clock`/`System.currentTimeMillis`, static factories. Do NOT mock `ZimbraLog` unless asserting a contractually-meaningful log message (almost never).
4. **Use `MailboxTestUtil`** when a test needs a `Mailbox`, `Account`, `Provisioning`, or DB. Existing tests use it heavily — search for `MailboxTestUtil.initServer()` / `clearData()` in `src/java-test/` for setup patterns. Prefer this real-but-in-memory approach over mocking a `Mailbox`.
5. **Naming**: `methodName_condition_expectedBehavior`. Example: `addMessage_oversizedAttachment_throwsQuotaExceeded`.
6. **AAA**: clear Arrange / Act / Assert blocks. Comments only on non-obvious mock setup. No multi-line javadoc on test methods.
7. **One logical assertion per test** where practical.
8. **Imports**: no wildcards. Static-import `org.junit.Assert.*` and `org.mockito.Mockito.*`.
9. **No `@Ignore`**. If a test can't be written, report it; don't stub it.

### FULL FUNCTIONAL TESTS — MANDATORY

**These rules MUST be followed to write genuine functional tests, not shallow unit tests:**

10. **State transitions**: test not just individual method outcomes, but how methods interact and affect object state across calls. Example: `create → lookup → update → delete` workflows.
   - Example: `Provisioning.createAccount() → getAccountByName() → modifyAttrs() → deleteAccount()`
   - Verify state changes propagate through the system

11. **Real-world workflows**: write tests that simulate actual Zimbra usage patterns, not just method-by-method snapshots.
   - **Account provisioning**: Create account → Set attributes → Grant rights → Lookup → Verify all properties persist → Delete
   - **Auth flow**: Login → Generate token → Verify token → Use token for action → Token expires → Reject expired token
   - **ACL operations**: Grant right → Check right (succeeds) → Revoke right → Check right (fails)

12. **Side effects and dependencies**: test that operations correctly handle dependent objects and side effects.
   - Example: Creating a DistributionList should create underlying LDAP entry, create cache entry, and fire provisioning callbacks
   - Assert all downstream effects, not just return value

13. **Error recovery**: test that partial failures don't corrupt state.
   - Example: If mid-operation update fails, verify previous changes are rolled back or the object remains in a valid state
   - Test transactional boundaries

14. **Boundary conditions as workflows**: test edge cases in the context of a full operation chain.
   - Wrong auth → Correct auth → Wrong auth again (verify proper state recovery)
   - Oversized quota → Operation fails → Adjust quota → Retry succeeds
   - These are functional workflows, not isolated edge cases

15. **Mock boundaries wisely**: mock ONLY external systems (LDAP, HTTP, filesystem). For in-domain operations, use real objects via `MailboxTestUtil`.
   - BAD: Mock Account to test Provisioning.createAccount()
   - GOOD: Create real Account via Provisioning, test Provisioning methods against it
   - EXCEPTION: Mock external LDAP calls, but test real provisioning logic with MockProvisioning

16. **Assertion depth**: assert not just "method returned successfully," but the complete resulting state.
   - BAD: `assertTrue(account != null);` — this is placeholder
   - GOOD: Assert name, email, COS, attributes, permissions, cache state, callback invocations
   - Test verifies the operation produced the correct final state, end-to-end

17. **No isolation tests**: avoid creating tests where each test has zero dependencies on prior test state. Functional tests often require setup (which is fine — use `@Before` and MailboxTestUtil.initServer()).
   - This is not about test execution order (tests must be independent), but about testing real workflows
   - Each test should simulate a complete, realistic scenario

18. **Coverage of normal + failure**: for each workflow, test both success and failure paths.
   - Create account: OK case (succeeds), duplicate name (fails + rejects), invalid email (fails + error message), quota exceeded (fails)
   - Test the full operation and its error handling, not just "no exception"

---

**CRITICAL**: Tests that pass the above requirements are "functional tests." Tests that only verify one method in isolation against mocks are "unit tests" — they are NOT acceptable for this task. We need BOTH:
- Unit coverage (every branch)
- Functional coverage (real workflows, state transitions, side effects)

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

# PACKAGE FOCUS — `com.zimbra.cs.account`

| Package | Src Files | Existing Tests | Gap | Focus Areas |
|---|---|---|---|---|
| `cs.account` | ~376 | ~10 | huge | Provisioning, auth, identity — security-critical and largely untested |

**Sub-areas to cover:**
- Root package: `Account`, `Provisioning`, `LdapProvisioning`, `MockProvisioning`, `Cos`, `Domain`, `Server`, `DistributionList`, `DynamicGroup`, `Identity`, `DataSource`, `Signature`, `Alias`, `Zimlet`
- Subpackages: `auth`, `accesscontrol`, `cache`, `callback`, `gal`, `grouphandler`, `krb5`, `ldap`, `names`, `oauth2`, `soap`, `ya`

**All other packages in store module are out of scope for this prompt.**

---

# EXECUTION — `cs.account` (376 src / 10 tests)

Goal: cover the provisioning, auth, identity, and account-attribute logic across all sub-areas of the `cs.account` package.

## Sub-areas to inventory (under `com.zimbra.cs.account`):

**Root package** (core types): `Account`, `Provisioning`, `LdapProvisioning`, `MockProvisioning`, `Cos`, `Domain`, `Server`, `DistributionList`, `DynamicGroup`, `Identity`, `DataSource`, `Signature`, `Alias`, `Zimlet`, `XMPPComponent`, etc.

**Subpackages** (organized features):
- `auth/` — Auth providers and token lifecycle
- `accesscontrol/` — Grant/revoke/check right enforcement
- `cache/` — Caching logic
- `callback/` — Provisioning callbacks
- `gal/` — GAL search
- `grouphandler/` — Dynamic group handling
- `krb5/` — Kerberos auth
- `ldap/` — LDAP integration
- `names/` — Email parsing / validation
- `oauth2/` — OAuth provider
- `soap/` — SOAP request handlers
- `ya/` — Yahoo-specific handling

## Testing strategy per category:

- **`Provisioning` subclasses & `*ProvisioningExt` callbacks**: each public method, normal path + error path; argument validation.
- **`AttributeManager` / `AttributeInfo` / `AttributeCallback`**: schema validation logic; bad-value rejection.
- **Access control (`accesscontrol`)**: grant/check/revoke for each right type; right inheritance; chained grants.
- **Auth (`auth`)**: each auth provider (`ZimbraAuth`, `LdapAuth`, etc.) — success / wrong-password / locked / expired / two-factor / fallback.
- **`AuthToken` / token lifecycle**: generation, verification, expiry, revocation.
- **Names (`names`)**: email-address parsing/validation, IDN edge cases, length limits.
- **Cache (`cache`)**: hit / miss / invalidate / size-bound eviction.

PowerMock is required for `Provisioning.getInstance()` and `LdapClient` statics in many files.

Use `MockProvisioning` and `MailboxTestUtil` whenever possible instead of mocking.

**Execution**: Run the inventory step first; expect ~250–300 testable classes after the skip list. Work through classes in alphabetical order by subpackage. Group output by sub-area in the final summary.

---

# FINAL DELIVERABLE

After completing the `cs.account` package, output:

1. Total new test count for `cs.account` package.
2. Production bugs surfaced (list with `file:line` and short description).
3. Classes skipped beyond the skip-list, with reasons.
4. Confirmation that `ant test` is fully green.
5. Coverage summary: existing ~10 tests → new total.

---

# REMINDERS

- Java 8 toolchain.
- JUnit 4.13.2 + Mockito 1.10.19 + PowerMock 1.6.5.
- Use `MailboxTestUtil` rather than mocking the world.
- Production code is read-only.
- Quality bar applies to every test.
- Run `ant test` after each class. Run full `ant test` at the end.

Begin with the inventory step and proceed through all classes in `cs.account` and its subpackages.
