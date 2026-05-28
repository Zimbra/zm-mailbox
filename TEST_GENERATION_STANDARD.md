# zm-mailbox cs.account JUnit Test Generation Standard

**Single Source of Truth for All Test Generation Work**

This document consolidates all rules, quality standards, and guidelines for writing JUnit tests for the `com.zimbra.cs.account` package.

---

## TABLE OF CONTENTS

1. [Overview](#overview)
2. [Mandatory Rules: Full Functional Tests](#mandatory-rules-full-functional-tests)
3. [Universal Quality Bar](#universal-quality-bar)
4. [Toolchain Requirements](#toolchain-requirements)
5. [Test File Header & Naming](#test-file-header--naming)
6. [Classification & Workflow](#classification--workflow)
7. [Category Playbook](#category-playbook)
8. [Constraints (Do Not Violate)](#constraints-do-not-violate)
9. [Verification Checklist](#verification-checklist)
10. [Rejection Criteria](#rejection-criteria)
11. [Execution Instructions](#execution-instructions)

---

## OVERVIEW

**Scope**: `com.zimbra.cs.account` package (~376 source files, ~10 existing tests)  
**Goal**: Write ~1,300-1,400 comprehensive JUnit tests covering provisioning, authentication, identity, and account logic  
**Quality Bar**: FULL FUNCTIONAL TESTS — not shallow unit tests or mock-heavy snapshots  
**Agents**: upto 10 parallel agents covering different subpackages

---

## MANDATORY RULES: Full Functional Tests

**All tests must be FULL FUNCTIONAL TESTS. Tests that violate these 9 rules WILL BE REJECTED.**

### Rule 1: State Transitions

**Definition**: Test not just individual method outcomes, but how methods interact and affect object state across calls.

**Bad Example** ❌:
```java
@Test
public void getAccount_byName_returnsAccount() {
    Account account = provisioning.getAccountByName("test");
    assertNotNull(account);  // Isolated call, no setup
}
```

**Good Example** ✅:
```java
@Test
public void createAndLookupAccount_persistence_verified() {
    Provisioning prov = MailboxTestUtil.getProvisioning();
    
    // Create account
    Account created = prov.createAccount("test", "password", attrs);
    assertEquals("test@example.com", created.getMail());
    
    // Lookup (state transition: created → retrieved)
    Account retrieved = prov.getAccountByName("test");
    assertEquals(created.getId(), retrieved.getId());
    assertEquals("test@example.com", retrieved.getMail());
    
    // Modify (state transition: retrieved → modified)
    prov.modifyAttrs(retrieved, Collections.singletonMap("description", "Updated"));
    Account modified = prov.getAccountByName("test");
    assertEquals("Updated", modified.getAttr("description"));
}
```

**Why**: Tests must verify that objects maintain state through multiple operations, not just snapshot single calls.

---

### Rule 2: Real-World Workflows

**Definition**: Write tests that simulate actual Zimbra usage patterns, not isolated method calls.

**Examples by Area**:

**Account Provisioning Workflow**:
```
Create account → Set attributes → Grant admin rights → Lookup by email → 
Verify all properties persist → Modify quota → Delete account → Verify deletion
```

**Authentication Workflow**:
```
Login with credentials → Generate auth token → Use token for operation → 
Token expires/revoked → Reject expired token → Login again succeeds
```

**ACL/Right Workflow**:
```
Grant right to user → Check right (returns true) → Use right for operation → 
Revoke right → Check right (returns false) → Operation denied
```

**Cache Workflow**:
```
First lookup (miss, queries backend) → Second lookup (hit, cached) → 
Invalidate cache → Third lookup (miss again, queries backend)
```

**Bad Example** ❌: Testing `getAccount()` in isolation without any setup or context.

**Good Example** ✅: Complete workflow from account creation through deletion with state verification at each step.

---

### Rule 3: Side Effects and Dependencies

**Definition**: Test that operations correctly handle dependent objects, callbacks, cache updates, and other side effects.

**When Account is Created**:
- ✅ Verify LDAP entry is created
- ✅ Verify cache entry is populated
- ✅ Verify provisioning callbacks are fired
- ✅ Verify all dependent objects (Cos, Domain) reflect the change
- ❌ DO NOT just verify "account != null"

**When ACL Right is Granted**:
- ✅ Verify grant is stored in LDAP
- ✅ Verify cached rights are invalidated
- ✅ Verify subsequent right checks reflect the grant
- ✅ Verify callbacks are invoked
- ❌ DO NOT just verify "no exception"

**Example**:
```java
@Test
public void grantRight_completeSideEffects_verified() {
    Account user = createTestAccount();
    
    // Grant right
    provisioning.grantRight(user, "adminAccountRight");
    
    // Assert 1: Right is stored (persistence)
    assertTrue(provisioning.checkRight(user, "adminAccountRight"));
    
    // Assert 2: Reload and verify (cache/persistence)
    Account reloaded = provisioning.getAccountById(user.getId());
    assertTrue(reloaded.hasRight("adminAccountRight"));
    
    // Assert 3: Callback was invoked (can verify via mock if needed)
    // (Use ArgumentCaptor to verify provisioning callbacks were called)
}
```

---

### Rule 4: Error Recovery

**Definition**: Test that partial failures don't corrupt state. Verify rollback or valid state after error.

**Example**:
```java
@Test
public void modifyAccount_failureRollback_stateValid() {
    Account account = createTestAccount();
    String originalEmail = account.getMail();
    
    // Try invalid modification
    try {
        provisioning.modifyAttrs(account, Collections.singletonMap("mail", "INVALID"));
        fail("Should throw exception");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("mail"));
    }
    
    // Verify state unchanged
    Account reloaded = provisioning.getAccountById(account.getId());
    assertEquals(originalEmail, reloaded.getMail());  // Original unchanged
}
```

---

### Rule 5: Boundary Conditions as Workflows

**Definition**: Test edge cases in the context of a full operation chain, not in isolation.

**Bad Example** ❌:
```java
@Test
public void parseEmail_emptyString_returnsNull() {
    assertNull(NameUtil.parseEmail(""));  // Isolated edge case
}
```

**Good Example** ✅:
```java
@Test
public void createAccount_emptyEmailField_rejectionRecovery_success() {
    // Try with empty email (should fail)
    try {
        provisioning.createAccount("test", "password", 
            Collections.singletonMap("mail", ""));
        fail("Should reject empty email");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("mail"));
    }
    
    // Verify account not created
    assertNull(provisioning.getAccountByName("test"));
    
    // Now fix and retry (recovery workflow)
    Account account = provisioning.createAccount("test", "password",
        Collections.singletonMap("mail", "valid@example.com"));
    assertNotNull(account);
    assertEquals("valid@example.com", account.getMail());
}
```

---

### Rule 6: Mock Boundaries Wisely

**Definition**: Mock ONLY external systems (LDAP, HTTP, filesystem). Use real objects for in-domain operations via MailboxTestUtil.

**Bad Example** ❌ (over-mocking):
```java
@Test
public void grantRight_validRight_success() {
    Account acct = mock(Account.class);  // ← DO NOT MOCK DOMAIN OBJECTS
    when(acct.getId()).thenReturn("account-id");
    provisioning.grantRight(acct, "right");
    verify(acct).getId();  // Tests mock behavior, not real provisioning
}
```

**Good Example** ✅ (real objects):
```java
@Test
public void grantRight_validRight_success() {
    Account acct = createTestAccount();  // ← Real account
    provisioning.grantRight(acct, "admin");
    assertTrue(provisioning.checkRight(acct, "admin"));  // Verify real behavior
}
```

**Exception: Mock External Boundaries**:
```java
@Test
public void authenticateViaLdap_validCredentials_success() {
    // Mock LDAP (external boundary) ✅
    LdapClient mockLdap = mock(LdapClient.class);
    when(mockLdap.authenticate(eq("user"), anyString())).thenReturn(true);
    
    // Real auth logic ✅
    AuthMechanism auth = new LdapAuth(provisioning, mockLdap);
    assertTrue(auth.authenticate("user", "password"));
}
```

---

### Rule 7: Assertion Depth

**Definition**: Assert not just "method returned successfully," but the complete resulting state.

**Bad Example** ❌:
```java
@Test
public void createAccount_success() {
    Account account = provisioning.createAccount("test", "password", attrs);
    assertNotNull(account);  // Placeholder assertion
}
```

**Good Example** ✅:
```java
@Test
public void createAccount_allPropertiesSet_correctly() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("cn", "Test User");
    attrs.put("mail", "test@example.com");
    attrs.put("zimbraMailQuota", "10737418240");
    
    Account account = provisioning.createAccount("test", "password", attrs);
    
    // Assert COMPLETE state
    assertEquals("Test User", account.getDisplayName());
    assertEquals("test@example.com", account.getMail());
    assertEquals("10737418240", account.getAttr("zimbraMailQuota"));
    assertEquals("test", account.getName());
    assertNotNull(account.getId());
    assertNotNull(account.getCreationDate());
    
    // Verify persistence
    Account reloaded = provisioning.getAccountByName("test");
    assertEquals(account.getId(), reloaded.getId());
    assertEquals(account.getMail(), reloaded.getMail());
    assertEquals(account.getAttr("zimbraMailQuota"), 
        reloaded.getAttr("zimbraMailQuota"));
}
```

**Why**: Tests must verify the operation produced the correct COMPLETE final state, end-to-end.

---

### Rule 8: No Isolation Tests

**Definition**: Avoid creating tests with zero realistic setup. Tests must use `@Before` and MailboxTestUtil to set up real Zimbra infrastructure.

**Bad Example** ❌:
```java
@Test
public void createAccount_isolated() {
    Account account = mock(Account.class);  // No real setup
    when(account.getMail()).thenReturn("test@example.com");
    assertNotNull(account);
}
```

**Good Example** ✅:
```java
@Before
public void setUp() {
    MailboxTestUtil.initServer();  // Real setup
}

@Test
public void createAccount_withRealSetup() {
    Provisioning prov = MailboxTestUtil.getProvisioning();  // Real provisioning
    Account account = prov.createAccount("test", "password", attrs);
    assertNotNull(account);
    assertEquals("test@example.com", account.getMail());
}
```

**Note**: Tests must be independent in execution (can run in any order), but they should test real workflows with proper setup.

---

### Rule 9: Coverage of Normal + Failure

**Definition**: For each workflow, test both success and failure paths with complete state verification.

**Example - Comprehensive Coverage**:
```java
@Test
public void createAccount_validInputs_succeeds() {
    Account account = provisioning.createAccount("test", "password", attrs);
    assertEquals("test", account.getName());
}

@Test
public void createAccount_duplicateName_throwsException() {
    provisioning.createAccount("test", "password", attrs);
    
    try {
        provisioning.createAccount("test", "password", attrs2);
        fail("Should throw exception");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("already exists"));
        // Verify only one account exists
        assertEquals(1, provisioning.getAllAccounts().size());
    }
}

@Test
public void createAccount_invalidEmail_throwsException() {
    attrs.put("mail", "INVALID_NO_AT");
    
    try {
        provisioning.createAccount("test", "password", attrs);
        fail("Should throw exception");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("mail"));
        // Verify account was NOT created
        assertNull(provisioning.getAccountByName("test"));
    }
}

@Test
public void createAccount_quotaExceeded_throwsException() {
    attrs.put("zimbraMailQuota", "999999999999999");  // Absurdly large
    
    try {
        provisioning.createAccount("test", "password", attrs);
        fail("Should throw exception");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("quota"));
        assertNull(provisioning.getAccountByName("test"));
    }
}
```

---

## UNIVERSAL QUALITY BAR

Apply these to EVERY test:

1. **Behavioral assertions**: Assert return values, mutated state, exception class AND message substring, mock interactions via `verify(...)`. Never write a test whose only assertion is "no exception thrown."

2. **Branch coverage per method**: For every public method, cover:
   - Happy path
   - Each if/else branch
   - Each declared exception path
   - Null/empty/boundary inputs

3. **No over-mocking**: Use real DTOs, real POJOs, real enum values, real exceptions. Mock only true boundaries:
   - `Provisioning.getInstance()`, `Mailbox`, `DbConnection`
   - Network/HTTP/LDAP/filesystem clients
   - `Clock`/`System.currentTimeMillis`
   - Static factories
   - DO NOT mock `ZimbraLog` unless asserting a contractual log message

4. **Use MailboxTestUtil**: When a test needs `Mailbox`, `Account`, `Provisioning`, or DB, use MailboxTestUtil. Prefer real-but-in-memory infrastructure over mocking.

5. **Naming convention**: `methodName_condition_expectedBehavior`
   - Example: `authenticate_validPassword_returnsToken`
   - Example: `grantRight_userAlreadyHasRight_idempotent`

6. **AAA pattern**: Clear Arrange / Act / Assert blocks
   - Comments only on non-obvious mock setup
   - No multi-line javadoc on test methods

7. **One logical assertion per test** where practical
   - Multiple related assertions (verify state) = OK
   - "Create account and verify all properties" = 1 logical test
   - "Create account AND grant right AND check right" = 3 logical tests

8. **Imports**: No wildcards
   - Static import: `org.junit.Assert.*`
   - Static import: `org.mockito.Mockito.*`

9. **No `@Ignore`**: If a test can't be written, report it; don't stub it.

---

## TOOLCHAIN REQUIREMENTS

### Java / JDK
- **Target**: Java 8 bytecode
- **Requirement**: Use JDK 8 (NOT 17+)
- **Verify**: `java -version` and `javac -version` before running tests
- **If error**: `UnsupportedClassVersionError`, `javassist.*`, `IncompatibleClassChangeError` → STOP and report (JDK mismatch)

### Test Stack (REQUIRED — match existing tests)
- **JUnit**: 4.13.2 (`org.junit.Test`, `org.junit.Before`, `org.junit.Assert`, `org.junit.BeforeClass`)
- **Mockito**: 1.10.19 (old API: `org.mockito.Matchers` NOT `ArgumentMatchers`)
- **PowerMock**: 1.6.5 (`@PowerMockIgnore("javax.management.*")`, `@RunWith(PowerMockRunner.class)`)

### Reference Tests
- `src/java-test/com/zimbra/cs/mailbox/MailboxTest.java` — Mailbox testing patterns
- `src/java-test/com/zimbra/cs/filter/` — Large existing test corpus

---

## TEST FILE HEADER & NAMING

### Required Header (Every New Test File)
```java
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
```

(If a sibling test file uses different wording, match the sibling's header.)

### File Location
- Source: `src/java/com/zimbra/cs/account/...`
- Test: `src/java-test/com/zimbra/cs/account/...` (mirror the package structure)

### Test Class Naming
- `ClassName.java` → `ClassNameTest.java`
- Example: `Account.java` → `AccountTest.java`

---

## CLASSIFICATION & WORKFLOW

### Step 1: Inventory
List every source file in the assigned subpackage(s). Classify each:
- **TEST**: Needs new test class (or extend existing)
- **EXTEND**: Existing test exists; add missing scenarios
- **SKIP**: Skip-list match (provide reason)

Report inventory before writing code.

### Step 2: For Each TEST/EXTEND (Alphabetical Order)
1. Read source file fully
2. Read any existing test; if present, ADD to it (never rewrite existing tests)
3. Identify category (see CATEGORY PLAYBOOK)
4. Write tests applying quality bar + 9 mandatory rules
5. Run `ant test -Dtestcase=<FQN>Test` until green

### Step 3: Verify
After all classes in agent assignment are green individually, run full `ant test`. Ensure no pre-existing tests break.

### Step 4: Report
- Classes covered
- New test count per class
- Production bugs found (file:line + description)
- Classes skipped with reasons

---

## CATEGORY PLAYBOOK

Pick the matching category and apply its scenario pattern in addition to the universal quality bar.

### A. Service Handlers (extend `DocumentHandler`)
- **Happy path**: Build real request Element → invoke → assert response structure (tags, attributes, children)
- **Auth required**: No auth → `AUTH_REQUIRED` exception
- **Invalid input**: Missing required attr → `INVALID_REQUEST`
- **Not found**: Nonexistent id → `NO_SUCH_*`
- **Permission denied**: Wrong-role user → `PERM_DENIED`
- **Collaborator interactions**: Use `ArgumentCaptor` on `Mailbox`, `Provisioning`, etc.

### B. Mailbox Operations
- Use `MailboxTestUtil` to set up real in-memory mailbox
- Happy path: Operation completes → assert resulting `MailItem` / folder / tag / conversation state
- Transactional rollback: Force mid-operation failure → assert state unchanged
- Concurrent/locking: If `Mailbox.lock` used, test edge cases
- Quota/size limits: Oversized input → `QUOTA_EXCEEDED` or `MAIL_TOO_BIG`

### C. MIME / Parsing / Formatting
- Well-formed input → assert parsed structure (parts, headers, charset)
- Malformed/truncated → assert specific exception or fallback
- Encoding edge cases: UTF-8 non-ASCII, base64 padding, quoted-printable wrap
- Empty/null input per contract

### D. Protocol Handlers (IMAP, POP3, LMTP, Milter)
- State machine: Valid commands per state; invalid rejected
- Successful command → expected response code + data
- Error responses formatted correctly
- Timeout/disconnect handling if observable
- Mock I/O layer, not real sockets

### E. DAV Resources (CalDAV / CardDAV)
- XML response shape (DAV namespace, status codes 200/207/404/412)
- Preconditions: If-Match, If-None-Match, ETag handling
- Collection vs leaf resource branching
- Malformed XML → 400

### F. Index / Search
- Query builder: Assert generated Lucene query for typical + edge inputs
- Sorter/result transformer: Assert ordering, paging, truncation
- Special characters: Properly escaped
- Empty corpus → empty result, not exception

### G. Filter / Sieve
- Script parsing: Valid → expected AST/rule list
- Script execution: Action triggered when condition true, not triggered when false
- Recursion/loop guards: Pathological scripts terminate

### H. Store / Volume / Blob
- Round-trip: Store → retrieve → bytes-equal
- Missing blob → expected exception
- Truncated blob / wrong digest → expected exception
- Volume offline / FS errors mocked at boundary

### I. Util / Helper / Static
- Pure-logic: Test every branch
- Boundary inputs: null, empty, max/min int, leap year, DST, very long strings
- Static state/caches: Hit, miss, invalidation

### J. Session / Listener / Observer
- Event fire → handler called with expected args (verify)
- Exception in handler: Swallowed vs propagated per contract
- Subscriber registration/deregistration

### K. Provisioning / Account / LDAP
- Use `MockProvisioning` or real LDAP fake; do NOT hit real LDAP
- Each attr getter/setter with branching logic
- Validation: Bad value → exception with attr name in message
- LDAP filter escape correctness — **SECURITY CRITICAL**

### L. Redo Log / Recovery
- Serialize → deserialize → round-trip equality
- Replay to fresh mailbox → final state == direct apply
- Truncated/corrupted → expected exception

---

## CONSTRAINTS — DO NOT VIOLATE

- ❌ Do NOT modify production code unless reporting bug + user approves fix. `src/java/` is read-only.
- ❌ Do NOT modify `ivy.xml`, `test-ivy.xml`, `build.xml`
- ❌ Do NOT introduce JUnit 5 syntax
- ❌ Do NOT add new dependencies
- ❌ Do NOT use `@Ignore`
- ❌ Do NOT delete or rewrite existing tests
- ❌ Do NOT refactor production code "while you're there"
- ❌ Do NOT write tests that only assert "no exception thrown"
- ❌ Do NOT mock `ZimbraLog` unless asserting contractual log message
- ❌ Do NOT hit real network services, real LDAP, real DB, or real filesystem (use MailboxTestUtil for in-memory)

---

## VERIFICATION CHECKLIST

Before submitting tests, verify ALL items:

- ✅ Tests simulate real Zimbra workflows (not mock chains)
- ✅ Tests use `MailboxTestUtil.initServer()` for setup
- ✅ Tests verify complete state, not just "no exception"
- ✅ Tests include both success AND failure cases
- ✅ Tests verify side effects (callbacks, cache, dependent objects)
- ✅ Tests use real objects where possible, mock only external boundaries
- ✅ Tests demonstrate state transitions (create → modify → delete chains)
- ✅ No test is placeholder (e.g., `assertNotNull` only if paired with other assertions)
- ✅ No `@Ignore` anywhere
- ✅ Assertions use `assertEquals`, `assertTrue`, message content checks
- ✅ Test naming follows `methodName_condition_expectedBehavior`
- ✅ AAA pattern clear (Arrange / Act / Assert blocks)
- ✅ No wildcards in imports
- ✅ All 9 mandatory rules followed (state transitions, workflows, side effects, error recovery, boundaries, mock wisely, assertion depth, no isolation, normal+failure)

---

## REJECTION CRITERIA

**Tests WILL BE REJECTED if**:
- ❌ They mock domain objects (Account, Provisioning, Domain, Server, etc.)
- ❌ They only assert "no exception thrown"
- ❌ They test single methods in isolation without workflow context
- ❌ They don't verify state mutations and side effects
- ❌ They don't test error paths
- ❌ They use `@Ignore` or skip tests without reporting
- ❌ They mock internal Zimbra classes instead of using MailboxTestUtil
- ❌ They don't test real-world workflows
- ❌ They use `assertTrue(obj != null)` as the only assertion
- ❌ They read like "setup mock, call once, verify called" (shallow pattern)

---

## EXECUTION INSTRUCTIONS

### Build Commands
```bash
# From: /Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/store

# Build all modules (required first time)
ant clean publish-local -Dzimbra.buildinfo.version=10.1.17

# Run all tests
ant test

# Run single test class
ant test -Dtestcase=com.zimbra.cs.account.<PackageName>.<ClassName>Test

# Run single test method (if supported)
ant test -Dtestcase=com.zimbra.cs.account.<Package>.<Class>Test -Dtestmethod=testMethodName
```

### Agent Execution Flow

1. **Inventory** (report before coding):
   - List all source files in assigned subpackage(s)
   - Classify: TEST / EXTEND / SKIP
   - Count testable classes

2. **For each TEST/EXTEND class**:
   - Read source file fully
   - Read any existing test (if present, extend it)
   - Identify category (A-L from playbook)
   - Write tests following:
     - 9 mandatory rules (state transitions, workflows, side effects, error recovery, boundaries, mock wisely, assertion depth, no isolation, normal+failure)
     - Universal quality bar (behavioral assertions, branch coverage, no over-mocking, MailboxTestUtil, naming, AAA, one logical assertion, imports, no @Ignore)
     - Category-specific patterns
   - Run `ant test -Dtestcase=<FQN>Test` until green

3. **After all classes in agent assignment green individually**:
   - Run full `ant test`
   - Ensure no pre-existing tests break

4. **Report**:
   - Total new test count (by class)
   - Production bugs found (file:line + description)
   - Classes skipped with reasons
   - Confirmation: `ant test` fully green

### Quality Assurance Before Submit

Each test file MUST pass verification checklist (see above) and rejection criteria (see above) before submission.

---

## SUMMARY

**Core Principle**: Tests must be FULL FUNCTIONAL TESTS that exercise real Zimbra workflows, not isolated method snapshots.

**All 9 mandatory rules MUST be followed:**
1. State transitions
2. Real-world workflows
3. Side effects & dependencies
4. Error recovery
5. Boundary conditions as workflows
6. Mock boundaries wisely
7. Assertion depth
8. No isolation tests
9. Coverage of normal + failure

**Quality Bar Applied to Every Test**:
- Behavioral assertions
- Branch coverage per method
- No over-mocking
- Use MailboxTestUtil
- Proper naming & AAA pattern
- One logical assertion per test
- Clean imports
- No @Ignore

**Ready to Execute**: Yes ✅

---

## FILES REFERENCED

- **test-coverage-prompt.md** — Original execution prompt with SHARED HEADER and package focus
- **FUNCTIONAL_TEST_RULES.md** — Detailed functional test mandate (9 rules with examples)
- **This document** — Consolidated standard (definitive source of truth)

All agents will reference THIS document as the authoritative standard.

