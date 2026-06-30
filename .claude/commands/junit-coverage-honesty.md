# JUnit Governance — Category 7: Coverage Honesty

Check that test file(s) at `$ARGUMENTS` provide real branch/behavior coverage rather than inflating line-coverage metrics with tests that can never fail.

## What to check

### CH-1: assertNotNull-only tests
- `@Test` methods where every assertion is `assertNotNull(...)` or `assertNotNull("...", ...)`
- These verify the object exists, not that it is correct
- Flag: "All assertions are assertNotNull — add field/state verification"

### CH-2: Happy-path only for branchy methods
- Identify production methods that have multiple `if/else` branches or `switch` statements
- If the test class covers the method but all tests use inputs that hit only one branch, flag as CH-2
- Heuristic: if a method has >2 logical branches (count `if`, `else if`, `? :`, `switch case`) and only 1 test covers it, flag

### CH-3: Swallowed-exception coverage antipattern
- Test methods structured as:
  ```java
  try {
      methodUnderTest();
  } catch (Exception e) {
      fail("unexpected exception: " + e);
  }
  ```
  with no assertions inside the `try` block
- This catches all failures AND passes vacuously — flag: "Add assertions inside try block or restructure"

### CH-4: verify(mock, times(0)) as sole assertion
- Mockito `verify(mock, times(0)).someMethod(...)` or `verifyNoInteractions(mock)` as the only assertion
- Verifies nothing was called — does not verify the system did the right thing

### CH-5: Mutation-targeted tests with no behavioral contract
- Tests with comments like "kills L43 negate mutations", "kills EmptyObjectReturnVals", "kills boundary mutation"
- These are valid in mutation test suites but should be supplemented with a plain-language description of the real contract being enforced
- Flag: "Mutation-targeted test — add a one-line comment stating the business rule this protects"

### CH-6: Duplicate test coverage
- Two or more `@Test` methods in the same class that exercise exactly the same production code path with essentially the same inputs
- Check by comparing: same method under test + same significant inputs + same assertion target
- Flag likely duplicates for consolidation

### CH-7: Branch coverage gap on the primary method under test
- For the primary method the test class targets (derived from class name): count distinct input variations across all tests
- If there are fewer unique input scenarios than there are documented parameters/modes in the production method's Javadoc, flag

## Output format

Per file:
```
FILE: <relative path>
  [CH-1] Line 335: getAllIdentities_freshAccount_returnsNonNull — all 1 assertions are assertNotNull
  [CH-3] Line 651: cleanExpiredTokens_freshAccount_noException — try block has no assertions
  [CH-5] Line 179: comment "kills L43/L71 negate mutations" — no plain-English contract stated
  [CH-6] Lines 89 + 102: both test isAccountStatusActive with status=ACTIVE — possible duplicate

  VERDICT: INFLATED COVERAGE (N issues) | PASS
```

Final summary:
```
COVERAGE HONESTY SUMMARY
------------------------
CH-1 assertNotNull-only tests        : N
CH-2 Happy-path-only branchy methods : N
CH-3 Swallowed exception pattern     : N
CH-4 verify(times(0)) sole assertion : N
CH-5 Mutation-targeted without contract: N
CH-6 Duplicate test coverage         : N
CH-7 Branch coverage gaps            : N
```
