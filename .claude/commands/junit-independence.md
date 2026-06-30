# JUnit Governance — Category 6: Test Independence

Check that test file(s) at `$ARGUMENTS` do not have ordering dependencies, shared-state corruption, or timing assumptions that make them pass individually but fail as a suite.

## What to check

### TI-1: Hardcoded account/domain name collisions
- Find all string literals that look like email addresses (contain `@`) used in `createAccount(`, `createDomain(`, or `get(AccountBy.name,`
- If the same email string appears in more than one `@Test` method (not in `@Before`/`@BeforeClass`), flag it
- These tests will collide if run in any order that leaves state behind

### TI-2: Static mutable field assigned in @Test (not @Before)
- `static` fields assigned inside `@Test` methods (not setup methods)
- These create ordering dependencies where test B relies on test A having run first

### TI-3: Thread.sleep in test body
- Any `Thread.sleep(` call inside a `@Test` method
- Flag with: "Use awaitility or a proper callback — sleep-based timing is CI-flaky"

### TI-4: Global state modification without guaranteed restore
- Any test that modifies a static/singleton field (e.g., via reflection or direct assignment to a `*Manager.instance`)
- Check whether the modification is wrapped in a `try { ... } finally { restore }` block
- If no `finally` restore exists, flag as TI-4

### TI-5: @BeforeClass / @AfterClass asymmetry
- If `@BeforeClass` creates resources (accounts, domains, servers), verify a matching `@AfterClass` cleans them up
- Absence of `@AfterClass` when `@BeforeClass` creates state is a flag for suite-level pollution

### TI-6: Domain creation without cleanup
- Any `createDomain(` call inside a `@Test` method or `@Before` without a corresponding `deleteDomain(` in `@After` or the test method itself
- Domain objects in MockProvisioning persist across all tests in the JVM session

## Output format

Per file:
```
FILE: <relative path>
  [TI-1] "acct@zimbra.com" used in 3 separate @Test methods — collision risk
  [TI-3] Line 312: Thread.sleep(500) — replace with deterministic assertion
  [TI-4] Line 178: sManager modified via reflection with no finally-restore
  [TI-6] Line 440: createDomain("locked.dom") — no matching deleteDomain in @After
  VERDICT: FRAGILE (N issues) | PASS
```

Final summary:
```
INDEPENDENCE SUMMARY
--------------------
TI-1 Hardcoded name collisions      : N
TI-2 Static field assigned in @Test : N
TI-3 Thread.sleep usage             : N
TI-4 Global state without restore   : N
TI-5 BeforeClass/AfterClass mismatch: N
TI-6 Domain creation without cleanup: N
```
