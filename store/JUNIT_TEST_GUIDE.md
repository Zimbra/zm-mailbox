# store module — JUnit Test Guide

Everything learned writing JUnit tests for this module, so teammates don't have to rediscover it.

---

## Test Directory Layout

```
store/
├── src/java/                          # Production code
├── src/java-test/                     # Test code (mirrors production package structure)
│   └── com/zimbra/cs/
│       ├── util/ZimbraTestUtil.java   # ← SHARED TEST UTILITIES — read this first
│       ├── service/admin/
│       │   └── BaseAdminTest.java     # ← BASE CLASS for admin handler tests
│       ├── account/auth/              # Auth package tests (complete)
│       └── account/accesscontrol/    # Access control tests (mostly complete)
└── src/java-test/localconfig-test.xml # LC overrides for tests (zimbra_home, etc.)
```

---

## Shared Test Utilities

### ZimbraTestUtil — `com/zimbra/cs/util/ZimbraTestUtil.java`

Three static-dependency patterns every test writer needs:

#### 1. Override a LocalConfig (LC) key
```java
// Set
ZimbraTestUtil.setLcKey(LC.zimbra_tmp_directory, "/tmp/test");
ZimbraTestUtil.setLcKey(LC.some_flag, true);     // boolean overload
ZimbraTestUtil.setLcKey(LC.some_int,  42);       // int overload

// Restore in @After
ZimbraTestUtil.resetLcKey(LC.zimbra_tmp_directory);

// Or use the JUnit @Rule for automatic cleanup
@Rule public final ZimbraTestUtil.LcKeyRule lcRule =
    ZimbraTestUtil.lcKeyRule(LC.zimbra_mailbox_lock_timeout, "5");
```
**Why:** `LC.some_key.value()` reads from `LocalConfig.mExpanded` first; this utility
evicts the key from that map so `KnownKey.value` (set via reflection) takes effect.

#### 2. Capture log output for assertions
```java
LogCapture cap = ZimbraTestUtil.captureLog(ZimbraLog.security, Level.WARN);
// ... exercise code ...
assertTrue(cap.contains("authentication failed"));          // substring, case-insensitive
assertTrue(cap.contains(Level.WARN, "auth failed"));        // with level filter

ZimbraTestUtil.removeLogCapture(ZimbraLog.security, cap);

// Or use @Rule
@Rule public final ZimbraTestUtil.LogCaptureRule logRule =
    ZimbraTestUtil.logCaptureRule(ZimbraLog.security, Level.WARN);
// In test: logRule.getCapture().contains(...)
```
**Why:** `ZimbraLog.xxx` fields are concrete `Log` objects backed by log4j2 — they
do NOT need mocking. Install a capturing appender when you need to assert on log output.

#### 3. Override Provisioning.getInstance()
```java
// Option A — full in-memory (recommended for most tests)
MockProvisioning prov = ZimbraTestUtil.installMockProvisioning();
prov.createAccount("user@test.com", "secret", new HashMap<>());
// Provisioning.getInstance() now returns this in-memory implementation

// Option B — Mockito stub (lightweight, stub only what you need)
Provisioning prov = ZimbraTestUtil.installMockitoProvisioning();
when(prov.getAccountByName("user@test.com")).thenReturn(fakeAccount);
// Always restore in @After:
ZimbraTestUtil.restoreProvisioning();

// Or use @Rule
@Rule public final ZimbraTestUtil.ProvisioningMockRule provRule =
    ZimbraTestUtil.provisioningMockRule();
// In test: when(provRule.getProv().getAccountByName(...)).thenReturn(...)
```

---

### BaseAdminTest — `com/zimbra/cs/service/admin/BaseAdminTest.java`

Base class for tests in `com.zimbra.cs.service.admin`. Extend it for any admin handler test.

```java
public class MyHandlerTest extends BaseAdminTest {
    // @BeforeClass init() from parent boots: MockProvisioning + HSQLDB + IndexStore
    // @AfterClass tearDown() from parent clears all data

    @Test
    public void myTest() throws Exception {
        Account user = createRegularAccount("user@test.zimbra.com");
        Element req  = Element.parseXML("<MyRequest/>");
        Element resp = execute(new MyHandler(), req);          // runs as adminAccount
        // assert on resp...
    }
}
```

Provided helpers: `createAdminAccount(email)`, `createRegularAccount(email)`,
`createDomain(name)`, `adminContext()`, `getRequestContext(auth, target)`,
`execute(handler, request)`, `execute(handler, request, caller)`, `initMocks()`.

---

## Test Patterns & Conventions

### JUnit 4 (not 5)
```java
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import static org.junit.Assert.*;

@Test public void foo() { ... }
@Test(expected = ServiceException.class) public void bar() { ... }
```

### Class categories (determines test strategy)
- **PURE** (no I/O/DB/network) — 1,305 classes — just instantiate and call, no mocks needed
- **INTEGRATION** (DB/LDAP/network) — 952 classes — use MockProvisioning / Mockito stubs
- **FRAMEWORK** (servlets/handlers) — 248 classes — extend BaseAdminTest or use MailboxTestUtil

### Resetting static singletons between tests
```java
// Pattern: save in @Before, restore in @After via reflection
private static final Field MY_FIELD;
static {
    MY_FIELD = MyClass.class.getDeclaredField("instance");
    MY_FIELD.setAccessible(true);
}
private Object saved;
@Before  public void save()    throws Exception { saved = MY_FIELD.get(null); }
@After   public void restore() throws Exception { MY_FIELD.set(null, saved); }
```

### Same-package access
Test files in the same package as production code can call package-private constructors,
methods, and enums directly — no reflection needed.

---

## Coverage Status (measured 2026-03-10)

| Metric | Value |
|---|---|
| Project-wide line coverage | 12.9% |
| Project-wide branch coverage | ~12.1% |
| Total lines (store module) | 302,687 |
| Zero-coverage packages | 51 |

**Highest-priority gaps** (lines_missed × business_criticality):
1. `cs.account` — 3.6% covered, score 626k
2. `cs.service.mail` — 4.4% covered
3. `cs.service.admin` — 0.3% covered

Full ranked table in `store/build/coverage/report.csv` (regenerate with `ant coverage`).

---

## Package-Specific Notes

### com.zimbra.cs.account.accesscontrol
Test directory: `store/src/java-test/com/zimbra/cs/account/accesscontrol/`
Status: ~27 of 34 files covered. **7 still missing:**
`ParticallyDeniedTest`, `PseudoTargetTest`, `RightBearerTest`, `RightCommandTest`,
`RightManagerTest`, `SearchGrantsTest`, `TargetIteratorTest`

Key gotchas:
- `RightManager.init()` is NOT called in unit tests → `Rights.Admin.*` / `Rights.User.*` fields are **null**
- `ZimbraACL` has no no-arg constructor — use `ZimbraACL(String[] aces, TargetType, String name)`
- For LDAP-dependent classes (CheckPresetRight, CollectEffectiveRights, etc.) write structural/reflection tests

### com.zimbra.cs.account.auth
Test directory: `store/src/java-test/com/zimbra/cs/account/auth/`
Status: **Complete** (5 files, 78 tests total, written 2026-03-11)

Key gotchas:
- `PasswordUtil.generateSSHA(password, salt)` — pass a non-null `byte[]` salt to bypass `InMemoryLdapServer.isOn()` and make the test deterministic
- `TwoFactorAuth.factory` is a static field — save/restore via reflection in `@Before`/`@After` (see `TwoFactorAuthTest`)
- `AuthMechanism.namePassedIn(null)` returns `""` — safe to call with null context
- `ZimbraCustomAuth.register()` silently ignores duplicate names — first registration wins

### com.zimbra.cs.service.admin
Test directory: `store/src/java-test/com/zimbra/cs/service/admin/`
Infrastructure: extend `BaseAdminTest` (see above). Mockito 1.10.19 + PowerMock 1.6.5 on classpath.

---

## Dependencies Already on Test Classpath
(declared in `store/ivy.xml` — no additions needed)
- JUnit 4.13.2
- Mockito 1.10.19
- PowerMock 1.6.5 (for static/final mocking when truly needed)
- log4j-core 2.17.x (for `AbstractAppender` in ZimbraTestUtil)
- commons-codec (Base64 used by PasswordUtil)

## When to Use PowerMock vs Mockito vs ZimbraTestUtil
| Need | Use |
|---|---|
| Mock `Provisioning.getInstance()` | `ZimbraTestUtil.installMockitoProvisioning()` |
| Mock `LC.some_key.value()` | `ZimbraTestUtil.setLcKey()` |
| Assert on `ZimbraLog` output | `ZimbraTestUtil.captureLog()` |
| Stub interface/class methods | `Mockito.mock()` |
| Mock a `final` class or JDK static (`System.currentTimeMillis`) | PowerMock |
| Stub only selected calls, lighter than MockProvisioning | `Mockito.mock(Provisioning.class)` |
