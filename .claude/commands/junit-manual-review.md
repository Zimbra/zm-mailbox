# JUnit Governance — Categories 2 & 3: Manual Justification & Quality Flags

Review test file(s) at `$ARGUMENTS` for patterns that require human judgment before accepting. These are not automatic rejects — they need a reviewer to decide whether the pattern is intentional and correct.

## Category 2: Requires Manual Justification

### MJ-1: Implementation class assertions
- `assertEquals(...SomeConcreteClass.class, obj.getClass())` or `assertTrue(obj instanceof SomeConcreteClass)` where `SomeConcreteClass` is a production implementation type
- Flag: "Tests implementation class name — will break on valid refactors. Reviewer: confirm this is an intentional contract"

### MJ-2: Reflection-based singleton manipulation
- Use of `java.lang.reflect.Field` to set or get private/static fields on production singletons (e.g., `AccessManager.sManager`)
- Flag: "Manipulates internal singleton state — reviewer must confirm @Before/@After restoration is correct and this pattern is acceptable"

### MJ-3: Global log level modification
- `cfg.setLevel(Level.WARN)` or any `LoggerContext` / `LogManager` configuration change inside a test
- Check for a matching restore in `@After` or `finally`
- Flag with restore status: "Log level modified [WITH restore | WITHOUT restore] — reviewer must verify suite-level isolation"

### MJ-4: Mutation-killing test orientation
- Tests with inline comments referencing mutation operators ("kills", "negate mutations", "EmptyObjectReturnVals", "boundary mutation")
- Flag: "Mutation-targeted test — reviewer should confirm the underlying business rule is clear and the test is not overfitting to a specific mutation tool configuration"

### MJ-5: Test that pinned encoding format as a security contract
- Assertions on the internal serialized form of auth tokens, keys, or encrypted data
- Flag: "Internal format assertion — reviewer (security) must confirm this format is a stable contract"

### MJ-6: Exception message string assertions
- `assertEquals("system failure: ...", e.getMessage())` or similar exact message match
- Flag: "Pins exception message wording — reviewer must confirm this message is part of the public API contract"

## Category 3: Quality Flags (Informational)

These do not block the PR but should be noted in the review.

### QF-1: Dual-scenario in one @Test (positive + negative)
- A single `@Test` method that contains both a valid-input assertion and an invalid-input `assertThrows`
- Flag: "Consider splitting into separate @Test methods for clarity"

### QF-2: @Before that creates accounts but no @After cleanup
- `@Before` creates accounts; no `@After` deletes them
- Flag: "No teardown — acceptable for MockProvisioning but note if switching to real LDAP"

### QF-3: Test names not following convention
- Test method names that don't follow `method_scenario_expectedResult` naming
- Examples: `test()`, `testGetAccount`, `verifyFoo` — flag for rename suggestion

### QF-4: Old-style Assert vs static import
- Mix of `Assert.assertEquals(...)` (old style) and `import static org.junit.Assert.assertEquals` (new style) in the same class
- Flag: "Inconsistent assertion style — standardize to static imports"

### QF-5: No negative test for exception-declaring method
- Production method declares `throws CheckedException` but test class has only happy-path tests for it
- Flag: "Missing exception path test — consider adding a negative case"

## Output format

Per file, two sections:

```
FILE: <relative path>

  MANUAL JUSTIFICATION REQUIRED:
  [MJ-1] Line 165: assertEquals(ACLAccessManager.class, m.getClass()) — implementation class assertion
  [MJ-3] Line 670: cfg.setLevel(Level.WARN) with no @After restore — log level leak
  [MJ-4] Line 179: // kills L43/L71 negate mutations

  QUALITY FLAGS (informational):
  [QF-1] Line 156: checkValue_portOutOfRange tests both valid and invalid input in one @Test
  [QF-3] Line 42: method named test() — rename to describe scenario and expected result
  [QF-4] Line 64: Assert.assertFalse mixed with static import assertEquals

  VERDICT: NEEDS JUSTIFICATION (N items) | FLAGS ONLY (N items) | CLEAN
```

Final summary:
```
MANUAL REVIEW SUMMARY
---------------------
Items requiring justification : N
  MJ-1 Implementation class assertions : N
  MJ-2 Singleton reflection            : N
  MJ-3 Log level modification          : N
  MJ-4 Mutation-killing orientation    : N
  MJ-5 Encoding format security pins   : N
  MJ-6 Exception message pins          : N

Quality flags (informational) : N
  QF-1 Dual-scenario tests    : N
  QF-2 No @After cleanup      : N
  QF-3 Naming violations      : N
  QF-4 Mixed assertion style  : N
  QF-5 Missing exception path : N
```
