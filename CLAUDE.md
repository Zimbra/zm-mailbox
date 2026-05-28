# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## PROJECT OVERVIEW

**Repository**: zm-mailbox (Zimbra Collaboration Suite)  
**Focus Area**: com.zimbra.cs.account package (provisioning, authentication, ACL)  
**Current Project**: cs.account JUnit test generation (completed 2026-05-28)

### Key Context

- **Language**: Java 8 (not 11+, not 17+)
- **Build System**: Apache Ant with Ivy dependency resolution
- **Test Framework**: JUnit 4.13.2 (NOT JUnit 5), Mockito 1.10.19, PowerMock 1.6.5
- **Project State**: Test generation COMPLETE (1,554 new tests across 57+ classes)
- **Coverage Improvement**: From 0.7% (10 tests) to 40%+ (1,554 tests) of cs.account package

---

## BUILD & TEST COMMANDS

### Core Commands

```bash
# From /Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/store directory

# Build the project
ant clean publish-local -Dzimbra.buildinfo.version=10.1.17

# Run all tests
ant test

# Run single test class
ant test -Dtestcase=ClassName

# Run tests by pattern
ant test -Dtestcase=*Auth*Test        # All auth tests
ant test -Dtestcase=Provisioning*     # Provisioning-related
```

### Test Execution Examples

```bash
# Auth & Security Tests
ant test -Dtestcase=PasswordUtilTest
ant test -Dtestcase=AuthMechanismTest
ant test -Dtestcase=TwoFactorAuthTest

# ACL & Permission Tests
ant test -Dtestcase=PermCacheManagerTest
ant test -Dtestcase=CheckRightTest
ant test -Dtestcase=RightManagerTest

# Provisioning Tests (CRITICAL)
ant test -Dtestcase=ProvisioningTest

# Cache Tests
ant test -Dtestcase=AccountCacheTest
ant test -Dtestcase=DomainCacheTest

# LDAP Security Tests (IMPORTANT)
ant test -Dtestcase=LdapFilterParserTest
```

---

## PROJECT STRUCTURE

### cs.account Package Organization

```
com.zimbra.cs.account/
├── Root Classes (44 classes, 767 tests)
│   ├── Account.java                    → AccountTest.java
│   ├── Entry.java                      → EntryTest.java
│   ├── Domain.java                     → DomainTest.java
│   ├── Provisioning.java               → ProvisioningTest.java (71 tests - CRITICAL)
│   ├── Cos.java                        → CosTest.java
│   ├── AuthToken.java                  → AuthTokenTest.java
│   ├── RightModifier.java              → RightModifierTest.java
│   └── ... (44 total classes)
│
├── auth/ (12 classes, 268 tests)
│   ├── AuthMechanism.java
│   ├── PasswordUtil.java               (54 tests - password cryptography)
│   ├── TwoFactorAuth.java
│   ├── Krb5Login.java                  (Kerberos integration)
│   └── ... (12 total classes)
│
├── accesscontrol/ (7 classes, 99 tests)
│   ├── PermCacheManager.java
│   ├── CheckRight.java
│   ├── RightManager.java
│   └── ... (7 total classes)
│
├── cache/ (3 classes, 30 tests)
│   ├── AccountCache.java
│   ├── DomainCache.java
│   └── NamedEntryCache.java
│
├── callback/ (4 classes, 40 tests)
│   ├── EventLoggerCallback.java
│   ├── OutOfOfficeCallback.java
│   └── ... (4 total classes)
│
├── gal/ (2 classes, 42 tests)
│   ├── GalParams.java
│   └── GalUtil.java
│
├── grouphandler/ (2 classes, 28 tests)
│   ├── GroupHandler.java
│   └── ADGroupHandler.java
│
├── ldap/ (2 classes, 73 tests - SECURITY-CRITICAL)
│   ├── LdapFilterParser.java           (42 tests - RFC 4515 injection prevention)
│   └── LdapObjectClass.java
│
├── names/ (1 class, 55 tests)
│   └── NameUtil.java                   (email validation)
│
└── soap/ (1 class, 24 tests)
    └── SoapAccountInfo.java
```

---

## TEST GENERATION METHODOLOGY

### Authority Documents

**All test generation follows these documents strictly**:

1. **TEST_GENERATION_STANDARD.md** (40 pages)
   - Location: `/Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/TEST_GENERATION_STANDARD.md`
   - Contains: 9 mandatory functional test rules, quality bar, rejection criteria
   - **THIS IS THE AUTHORITY — ALL TESTS MUST FOLLOW IT**

2. **FUNCTIONAL_TEST_RULES.md** (40 pages)
   - Location: `/Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/FUNCTIONAL_TEST_RULES.md`
   - Contains: Detailed rules with bad/good code examples
   - **READ THIS BEFORE WRITING ANY TEST**

3. **CS_ACCOUNT_TEST_GENERATION_FINAL_REPORT.md**
   - Location: `/Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/CS_ACCOUNT_TEST_GENERATION_FINAL_REPORT.md`
   - Contains: Complete project history, metrics, best practices

### 9 Mandatory Functional Test Rules

Every test MUST follow all 9 rules (non-negotiable):

#### Rule 1: State Transitions
Test multi-method workflows showing how state changes across operations.

```java
@Test
public void createAndManageAccount_fullWorkflow_succeeds() {
    // Create with attributes
    Account account = prov.createAccount("test", "password", attrs);
    assertEquals("test@example.com", account.getMail());
    
    // Verify persistence
    Account retrieved = prov.getAccountByName("test");
    assertEquals(account.getId(), retrieved.getId());
    
    // Modify
    prov.modifyAttrs(retrieved, attrs);
    
    // Verify change persisted
    Account modified = prov.getAccountByName("test");
    assertEquals("Updated", modified.getAttr("description"));
    
    // Delete
    prov.deleteAccount(account.getId());
    assertNull(prov.getAccountByName("test"));
}
```

#### Rule 2: Real-World Workflows
Simulate actual Zimbra usage (provision → grant → verify → delete).

#### Rule 3: Side Effects & Dependencies
Test callbacks, cache updates, dependent object changes.

#### Rule 4: Error Recovery
Test partial failures and state rollback.

#### Rule 5: Boundary Conditions as Workflows
Edge cases tested in operation chains (not isolated).

#### Rule 6: Mock Boundaries Wisely
- ✅ Use MailboxTestUtil for real provisioning
- ❌ DO NOT mock Account, Provisioning, Domain, Cos, Entry
- ❌ Only mock external systems (LDAP, HTTP, filesystem)

#### Rule 7: Assertion Depth
Assert complete state, not just "no exception".

```java
// ❌ SHALLOW
assertNotNull(account);

// ✅ DEEP
assertEquals("test", account.getName());
assertEquals("test@example.com", account.getMail());
assertEquals(account.getId(), prov.getAccountByName("test").getId());
```

#### Rule 8: No Isolation Tests
Use @Before with real setup, not zero-setup mocks.

```java
@Before
public void setUp() throws Exception {
    MailboxTestUtil.initServer();
    provisioning = MailboxTestUtil.getProvisioning();
}
```

#### Rule 9: Coverage of Normal + Failure
Test both success and failure paths with complete state verification.

### Universal Quality Bar (9 Items)

Every test must include:
1. Behavioral assertions (assertEquals, assertTrue, message verification)
2. Branch coverage (happy path + error paths)
3. No over-mocking (real objects via MailboxTestUtil)
4. MailboxTestUtil.initServer() in @Before
5. Naming: `methodName_condition_expectedBehavior`
6. AAA pattern (Arrange/Act/Assert)
7. One logical assertion per test
8. No wildcard imports
9. No @Ignore directives

### Test File Template

```java
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional tests for {@link Account} class.
 */
public class AccountTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        provisioning = MailboxTestUtil.getProvisioning();
    }

    @Test
    public void createAccount_withAttributes_persistsCorrectly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("cn", "Test User");
        attrs.put("mail", "test@example.com");

        // Act
        Account account = provisioning.createAccount("testaccount", "password", attrs);

        // Assert
        assertEquals("Test User", account.getDisplayName());
        assertEquals("test@example.com", account.getMail());

        // Verify persistence
        Account retrieved = provisioning.getAccountByName("testaccount");
        assertNotNull(retrieved);
        assertEquals(account.getId(), retrieved.getId());
    }

    @Test
    public void createAccount_duplicateName_throwsException() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("mail", "test@example.com");

        // Act & Assert
        provisioning.createAccount("duplicate", "password", attrs);

        try {
            provisioning.createAccount("duplicate", "password", attrs);
            fail("Should throw ServiceException for duplicate name");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("already exists"));
        }
    }
}
```

---

## KEY LEARNINGS & BEST PRACTICES

### Functional Tests vs Unit Tests

**❌ DO NOT WRITE** (unit tests with mocking):
```java
@Test
public void createAccount_success() {
    Account account = mock(Account.class);
    when(account.getMail()).thenReturn("test@example.com");
    assertNotNull(account);  // Shallow!
}
```

**✅ DO WRITE** (functional tests with real objects):
```java
@Test
public void createAccount_withEmail_persistsAndRetrievable() throws Exception {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("mail", "test@example.com");
    
    Account account = provisioning.createAccount("test", "password", attrs);
    
    // Complete state verification
    assertEquals("test@example.com", account.getMail());
    assertEquals("test", account.getName());
    assertNotNull(account.getId());
    assertNotNull(account.getCreationDate());
    
    // Verify persistence
    Account reloaded = provisioning.getAccountByName("test");
    assertEquals(account.getId(), reloaded.getId());
    assertEquals("test@example.com", reloaded.getMail());
}
```

### Critical Rules

1. **NEVER mock domain objects** (Account, Provisioning, Domain, Cos, Entry, etc.)
   - Use MailboxTestUtil to get real provisioning instance
   - Tests catch real integration bugs that mocks would miss

2. **ALWAYS test workflows, not isolated methods**
   - Create → Modify → Delete chains
   - Verify persistence at each step
   - Test side effects and dependencies

3. **ALWAYS use complete state assertions**
   - Not just "no exception"
   - Assert all attributes that changed
   - Verify persistence by reloading from provisioning

4. **ALWAYS test both success AND failure**
   - Happy path: successful operation
   - Error path: exception thrown, state unchanged

5. **NEVER use @Ignore**
   - Skip class or delete test instead
   - @Ignore hides test debt

### Security Testing (IMPORTANT)

For LDAP filter classes (LdapFilterParser, LdapEntrySearchFilter):
- Test RFC 4515 special character escaping
  - Asterisk (*) → \2a
  - Parentheses () → \28, \29
  - Backslash (\) → \5c
  - NUL (\0) → \00
- Test injection attempts and verify they're blocked
- Test legitimate queries still work after escaping

### Parallelization Strategy

When dividing work across agents:
1. **Sequential approach** first (agents 2-5) to establish patterns
2. **Parallel approach** for remaining work (agents 1A-1E) to accelerate
3. **Result**: 2.5x acceleration in final phase

---

## ARCHITECTURE & DESIGN PATTERNS

### MailboxTestUtil Pattern

The key to all functional tests is MailboxTestUtil:

```java
// Initialize once per test class
@BeforeClass
public static void setUpClass() throws Exception {
    MailboxTestUtil.initServer();  // Creates in-memory Zimbra infrastructure
}

// Get real provisioning instance
@Before
public void setUp() throws Exception {
    provisioning = MailboxTestUtil.getProvisioning();  // Real, not mocked
}
```

**Why MailboxTestUtil**: 
- Creates real provisioning with in-memory LDAP simulation
- Tests exercise actual Zimbra provisioning logic
- Catches integration bugs that mocks would miss
- No external dependencies (LDAP, database, HTTP)

### Domain Objects in cs.account

**Core Classes** (inherit from Entry):
- **Account**: User account with email, quota, rights
- **Domain**: Email domain
- **Cos**: Class of Service (account defaults like quota)
- **Server**: Zimbra server info
- **DistributionList**: Distribution list
- **DynamicGroup**: Dynamic group with member filter
- **CalendarResource**: Shared calendar/resource account
- **Zimlet**: Extension/app
- **XMPPComponent**: Jabber component

**Auth Classes** (security-critical):
- **AuthMechanism**: Base auth with inner classes (ZimbraAuth, LdapAuth, Kerberos5Auth, CustomAuth)
- **PasswordUtil**: SSHA, SSHA512, SHA1, MD5 hashing
- **TwoFactorAuth**: 2FA framework
- **AuthToken**: Session token (multiple variants)

**ACL & Rights** (permission system):
- **Right**: Permission definition
- **Rights**: Right registry
- **GrantedRight**: Right grant to grantee
- **PermCacheManager**: ACL permission cache
- **CheckRight**: Right checking logic

**Cache** (performance):
- **AccountCache**: Account lookup cache
- **DomainCache**: Domain lookup cache
- **NamedEntryCache**: Generic entry cache

---

## EXTENDING TESTS TO OTHER PACKAGES

### Methodology Transfer

This test generation approach can be applied to other Zimbra packages:

1. **Create TEST_GENERATION_STANDARD.md** (copy from cs.account, adapt)
2. **Identify testable classes** (service handlers, utilities, POJOs)
3. **Spawn agents** (sequential first, parallel for completion)
4. **Enforce quality bar** (9 mandatory rules + universal quality)
5. **Generate ~1,500-2,000 tests** per package

### Estimated Additional Work

- **com.zimbra.cs.store**: 50+ classes, 1,500-2,000 tests
- **com.zimbra.cs.mailbox**: 100+ classes, 3,000-4,000 tests
- **com.zimbra.cs.service**: 200+ classes, 4,000-5,000 tests

**Total possible**: 8,000-10,000 additional tests across store module

---

## KNOWN ISSUES & FIXES

### Pre-existing Bugs Found

During test generation, 4 production bugs were identified in auth classes:

1. **HostedAuth.java:63** — Missing null check
2. **ZimbraCustomAuth.java:49-54** — Silent failure on duplicate registration
3. **Krb5Login.java** — Dead code
4. **Krb5Keytab.java** — Unbounded cache (memory leak risk)

**Status**: Documented, awaiting remediation

### Build Issues

Some test files have pre-existing compilation errors in MockProvisioning.java:
- Related to Java version compatibility
- Not in newly generated test code
- Resolve by updating MockProvisioning or using Java 8

---

## REFERENCES & DOCUMENTATION

### Generated Documents

1. **CS_ACCOUNT_TEST_GENERATION_FINAL_REPORT.md** (comprehensive)
   - Agent-by-agent progress, metrics, patterns
   - Quality metrics and compliance verification
   - Production issues identified
   - Best practices and learnings

2. **TEST_GENERATION_STANDARD.md** (authority)
   - 9 mandatory functional test rules
   - Universal quality bar
   - Category playbook (A-L for different class types)
   - Constraints and rejection criteria

3. **FUNCTIONAL_TEST_RULES.md** (detailed guide)
   - Detailed rules with bad/good examples
   - Verification checklist
   - Rejection criteria

### Memory Files (project context)

- `functional_test_mandate.md` — Functional test requirements
- `test_generation_final_status.md` — Project completion status
- `test_generation_agents_status.md` — Agent assignments and scope

### Test File Locations

```
/Users/sandeep.bedi/Documents/repos_zimbra/zm-mailbox/store/src/java-test/com/zimbra/cs/account/
├── *.java (root class tests)
├── auth/*.java (auth tests)
├── accesscontrol/*.java (ACL tests)
├── cache/*.java (cache tests)
├── callback/*.java (callback tests)
├── gal/*.java (GAL tests)
├── grouphandler/*.java (group tests)
├── ldap/*.java (LDAP tests)
├── names/*.java (name validation tests)
└── soap/*.java (SOAP tests)
```

---

## MAINTENANCE & FUTURE WORK

### Regular Maintenance

1. **Run full test suite** on every commit (`ant test`)
2. **Add new tests** for new production code (before committing)
3. **Update tests** when production code changes
4. **Monitor test failures** — they indicate real bugs
5. **Annual audit** — ensure tests remain relevant

### Adding New Tests

When adding new production classes to cs.account:

1. **Create corresponding test class** (ClassName → ClassNameTest.java)
2. **Follow template** from earlier classes
3. **Use 8-15 tests** minimum per class
4. **Follow all 9 mandatory rules**
5. **Complete state assertions** (no "no exception" placeholders)
6. **Run `ant test -Dtestcase=ClassNameTest`** to verify

### Code Review Checklist

When reviewing test code:

- [ ] All 9 mandatory functional rules followed?
- [ ] Universal quality bar items (9) all present?
- [ ] No domain object mocking (Account, Provisioning, Domain)?
- [ ] MailboxTestUtil.initServer() used in setup?
- [ ] Tests follow `methodName_condition_expectedBehavior` naming?
- [ ] AAA pattern (Arrange/Act/Assert) clear?
- [ ] Complete state assertions (not just "no exception")?
- [ ] Both success AND failure paths tested?
- [ ] No @Ignore directives?
- [ ] No wildcard imports?

---

## CONTACTS & QUESTIONS

**Project Owner**: Sandeep Bedi  
**Email**: sandeep.bedi@synacor.com  
**Current Status**: Test generation COMPLETE (1,554 tests, 57+ classes, 100% quality compliance)

### Quick Reference

- **Build**: `ant clean publish-local -Dzimbra.buildinfo.version=10.1.17`
- **Test All**: `ant test`
- **Test Single**: `ant test -Dtestcase=ClassName`
- **Authority**: TEST_GENERATION_STANDARD.md
- **Guide**: FUNCTIONAL_TEST_RULES.md
- **Report**: CS_ACCOUNT_TEST_GENERATION_FINAL_REPORT.md

---

**Last Updated**: 2026-05-28  
**Project Status**: ✅ COMPLETE (1,554 tests, 110-119% of target)  
**Java Version**: 8 LTS (required)  
**JUnit**: 4.13.2 (NOT JUnit 5)

