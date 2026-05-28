# FUNCTIONAL TEST RULES — Mandatory for cs.account JUnit Tests

**These rules MUST be followed strictly before agents begin test generation. Any test that violates these rules will be rejected.**

---

## Definition: Functional Test vs Unit Test

**UNIT TEST** (shallow, isolated):
- Tests a single method in isolation
- Heavy use of mocks
- Only verifies that method's return value
- No state transitions or workflows
- No verification of side effects or dependencies
- ❌ **NOT ACCEPTABLE** for this task

**FUNCTIONAL TEST** (complete, realistic):
- Tests real workflows and state transitions
- Uses real objects (via MailboxTestUtil); mocks only external boundaries
- Verifies complete state after operation
- Tests side effects, callbacks, and dependent object updates
- Simulates realistic Zimbra usage patterns
- ✅ **REQUIRED** for this task

---

## Mandatory Rules (Must Follow All)

### Rule 1: State Transitions
**Requirement**: Test not just individual method outcomes, but how methods interact and affect object state across calls.

**Example - REQUIRED**:
```java
@Test
public void createAndManageAccount_fullWorkflow_succeeds() {
    // Arrange: real provisioning setup
    Provisioning prov = MailboxTestUtil.getProvisioning();
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("cn", "Test User");
    attrs.put("mail", "test@example.com");
    
    // Act 1: Create account
    Account account = prov.createAccount("testaccount", "password", attrs);
    
    // Assert 1: Account created with correct attributes
    assertEquals("Test User", account.getDisplayName());
    assertEquals("test@example.com", account.getMail());
    
    // Act 2: Lookup and verify persistence
    Account retrieved = prov.getAccountByName("testaccount");
    assertNotNull(retrieved);
    assertEquals(account.getId(), retrieved.getId());
    
    // Act 3: Modify attributes
    Map<String, Object> updates = new HashMap<>();
    updates.put("description", "Updated description");
    prov.modifyAttrs(retrieved, updates);
    
    // Assert 3: Changes persisted
    Account updated = prov.getAccountByName("testaccount");
    assertEquals("Updated description", updated.getAttr("description"));
    
    // Act 4: Delete
    prov.deleteAccount(account.getId());
    
    // Assert 4: Deletion verified
    assertNull(prov.getAccountByName("testaccount"));
}
```

**Example - REJECTED** (shallow unit test):
```java
@Test
public void createAccount_validEmail_returnsAccount() {
    Account account = mock(Account.class);
    when(account.getMail()).thenReturn("test@example.com");
    assertNotNull(account);  // ← Placeholder assertion
}
```

---

### Rule 2: Real-World Workflows
**Requirement**: Write tests that simulate actual Zimbra usage patterns, not isolated method calls.

**Examples by package**:

**Account Provisioning Workflow**:
```java
// Create account → Set multiple attributes → Grant admin rights → Verify all properties persist
```

**Auth Flow Workflow**:
```java
// Login with credentials → Generate auth token → Use token for privileged operation → 
// Token expires → Reject expired token → Login again succeeds
```

**ACL Operation Workflow**:
```java
// Grant right to user → Verify user can perform action → Revoke right → 
// Verify user cannot perform action anymore
```

**Cache Workflow**:
```java
// First lookup (cache miss) → Second lookup returns cached value (verify same object) → 
// Invalidate cache → Third lookup queries backend again
```

---

### Rule 3: Side Effects and Dependencies
**Requirement**: Test that operations correctly handle dependent objects and side effects.

**Example for Account Creation**:
When `Provisioning.createAccount()` is called:
- ✅ Verify LDAP entry is created
- ✅ Verify cache entry is populated
- ✅ Verify provisioning callbacks are fired
- ✅ Verify all dependent objects (Cos, Domain) reflect the new account
- ❌ DO NOT just verify return value

**Example for ACL Grant**:
When `Provisioning.grantRight()` is called:
- ✅ Verify grant is recorded in LDAP
- ✅ Verify cached rights are invalidated/updated
- ✅ Verify subsequent right checks reflect the grant
- ✅ Verify callbacks are invoked
- ❌ DO NOT just verify "no exception"

---

### Rule 4: Error Recovery
**Requirement**: Test that partial failures don't corrupt state.

**Example**:
```java
@Test
public void modifyAccount_midFailureRollback_stateValid() {
    Account account = createTestAccount();
    String originalEmail = account.getMail();
    
    // Try to modify with invalid data (should fail)
    try {
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("mail", "INVALID_EMAIL_NO_AT_SIGN");  // Will fail validation
        provisioning.modifyAttrs(account, invalid);
        fail("Should have thrown exception");
    } catch (ServiceException e) {
        // Expected
    }
    
    // Verify state is unchanged (recovery)
    Account reloaded = provisioning.getAccountById(account.getId());
    assertEquals(originalEmail, reloaded.getMail());  // Original email should be unchanged
}
```

---

### Rule 5: Boundary Conditions as Workflows
**Requirement**: Test edge cases in the context of a full operation chain, not isolated.

**Bad Example** (isolated edge case):
```java
@Test
public void parseEmail_emptyString_returnsNull() {
    assertNull(NameUtil.parseEmail(""));
}
```

**Good Example** (workflow with edge case):
```java
@Test
public void createAccount_emptyEmailField_rejectionRecovery_success() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("mail", "");  // Empty email
    
    try {
        provisioning.createAccount("test", "password", attrs);
        fail("Should reject empty email");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("mail"));
    }
    
    // Now try with valid email
    attrs.put("mail", "valid@example.com");
    Account account = provisioning.createAccount("test", "password", attrs);
    assertNotNull(account);
    assertEquals("valid@example.com", account.getMail());
}
```

---

### Rule 6: Mock Boundaries Wisely
**Requirement**: Mock ONLY external systems (LDAP, HTTP, filesystem). For in-domain operations, use real objects via MailboxTestUtil.

**BAD Example** (over-mocking):
```java
@Test
public void grantRight_validRight_success() {
    Account acct = mock(Account.class);  // ← DO NOT MOCK THIS
    when(acct.getId()).thenReturn("account-id");
    
    provisioning.grantRight(acct, "right");
    
    verify(acct).getId();  // ← Tests mock behavior, not real provisioning
}
```

**GOOD Example** (real objects):
```java
@Test
public void grantRight_validRight_success() {
    Account acct = createTestAccount();  // ← Real account via MailboxTestUtil
    
    provisioning.grantRight(acct, "admin");
    
    assertTrue(provisioning.checkRight(acct, "admin"));  // ← Verify real behavior
}
```

**EXCEPTION: Mock External Boundaries**:
```java
@Test
public void authenticateViaLdap_validCredentials_success() {
    // Mock LDAP client (external boundary)
    LdapClient mockLdap = mock(LdapClient.class);
    when(mockLdap.authenticate(eq("user"), anyString())).thenReturn(true);
    
    // But use real provisioning logic
    AuthMechanism auth = new LdapAuth(provisioning, mockLdap);  // Real auth, mock LDAP
    assertTrue(auth.authenticate("user", "password"));
}
```

---

### Rule 7: Assertion Depth
**Requirement**: Assert not just "method returned successfully," but the complete resulting state.

**BAD Example**:
```java
@Test
public void createAccount_success() {
    Account account = provisioning.createAccount("test", "password", Collections.emptyMap());
    assertNotNull(account);  // ← Placeholder, doesn't verify state
}
```

**GOOD Example**:
```java
@Test
public void createAccount_withAttributes_allPropertiesCorrect() {
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
}
```

---

### Rule 8: No Isolation Tests
**Requirement**: Avoid creating tests where each test has zero dependencies on prior test state. (Note: Tests must be independent in execution, but they should test real workflows with setup.)

**BAD Pattern**:
```java
@Test
public void createAccount_isolated_noSetup() {
    // This test assumes nothing and sets up nothing realistic
    Account account = mock(Account.class);
    when(account.getMail()).thenReturn("test@example.com");
    assertNotNull(account);
}
```

**GOOD Pattern**:
```java
@Before
public void setUp() {
    MailboxTestUtil.initServer();  // Real setup
}

@Test
public void createAccount_withRealSetup_succeeds() {
    // Now test with real Zimbra infrastructure
    Provisioning prov = MailboxTestUtil.getProvisioning();
    Account account = prov.createAccount("test", "password", Collections.emptyMap());
    assertNotNull(account);
}
```

---

### Rule 9: Coverage of Normal + Failure
**Requirement**: For each workflow, test both success and failure paths with complete state verification.

**Example - Complete Coverage**:
```java
@Test
public void createAccount_validInputs_succeeds() {
    // OK case
    Account account = provisioning.createAccount("test", "password", attrs);
    assertEquals("test", account.getName());
}

@Test
public void createAccount_duplicateName_throwsException() {
    provisioning.createAccount("test", "password", attrs);
    
    try {
        provisioning.createAccount("test", "password", attrs2);  // Duplicate
        fail("Should throw exception");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("already exists"));
        
        // Verify state is unchanged
        assertEquals(1, provisioning.getAllAccounts().size());  // Only one account
    }
}

@Test
public void createAccount_invalidEmail_throwsException() {
    attrs.put("mail", "INVALID");
    
    try {
        provisioning.createAccount("test", "password", attrs);
        fail("Should throw exception");
    } catch (ServiceException e) {
        assertTrue(e.getMessage().contains("mail") || e.getMessage().contains("email"));
        
        // Verify account was NOT created
        assertNull(provisioning.getAccountByName("test"));
    }
}

@Test
public void createAccount_quotaExceeded_throwsException() {
    attrs.put("zimbraMailQuota", "999999999999999");  // Absurd quota
    
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

## Verification Checklist

Before submitting tests, verify:

- ✅ Tests simulate real Zimbra workflows (not mock chains)
- ✅ Tests use `MailboxTestUtil.initServer()` for setup
- ✅ Tests verify complete state, not just "no exception"
- ✅ Tests include both success AND failure cases
- ✅ Tests verify side effects (callbacks, cache, dependent objects)
- ✅ Tests use real objects where possible, mock only external boundaries (LDAP, HTTP, FS)
- ✅ Tests demonstrate state transitions (create → modify → delete chains)
- ✅ No test is a placeholder ("assertNotNull is OK only if paired with other assertions)
- ✅ No `@Ignore` anywhere
- ✅ Assertions use `assertEquals`, `assertTrue`, message contents — not just truth value

---

## Rejection Criteria

Tests WILL BE REJECTED if:
- ❌ They mock domain objects (Account, Provisioning, Domain, etc.)
- ❌ They only assert "no exception thrown"
- ❌ They test single methods in isolation without workflow context
- ❌ They use `@Ignore` or skip tests without reporting
- ❌ They don't verify state mutations and side effects
- ❌ They don't verify persistence (reload and re-verify)
- ❌ They don't test error paths
- ❌ They use `assertTrue(obj != null)` as the only assertion
- ❌ They mock internal Zimbra classes instead of using MailboxTestUtil
- ❌ They don't test real-world workflows

---

## Summary

**Functional tests are required.** Unit tests are only acceptable as part of broader functional coverage. If a test reads like "mock this, verify that method was called, done" — it's too shallow and will be rejected.

**All agents MUST follow these 9 rules strictly.**
