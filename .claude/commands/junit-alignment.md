# JUnit Governance — Category 5: Test-to-Production Alignment

Verify that the JUnit test file(s) at `$ARGUMENTS` actually correspond to real production behavior. This is the highest-risk failure mode for AI-generated tests — confident assertions about methods that don't exist or behave differently than described.

## What to check

For each test class found under `$ARGUMENTS`:

### AL-1: Production class exists
- Derive the production class from the test class name (strip `Test`, `FunctionalTest`, `IntegrationTest` suffix)
- Locate the production `.java` file in `src/java/` or `src/main/`
- If not found: flag as AL-1 — tests for a non-existent class

### AL-2: Method under test exists
- For each `@Test` method, identify what production method it targets (from method name or Javadoc `@see`)
- Using the `method_scenario_expected` naming convention, the prefix before the first `_` is the production method name
- Verify that method exists in the production class (or its superclasses)
- Flag any test method whose derived target method cannot be found

### AL-3: Inverted / wrong assertions
- Look for patterns where the test asserts the opposite of what the production method contract states
- Examples: asserting `null` when the method's Javadoc says it never returns null; asserting `false` for a method called `isActive` on an active account
- Flag for human review — these require reading the production code

### AL-4: Import resolution
- List any `import` statements that reference classes not resolvable within the module (common sign of hallucinated APIs)
- Cross-check against `src/java/` and known library jars
- Flag unresolvable imports as AL-4

### AL-5: MockProvisioning proxy tests
- Identify tests where the only observable behavior is on `MockProvisioning` / the test harness, not on the class under test
- Patterns: test creates account, calls `prov.getAccount()`, asserts on result — this tests the mock, not the target class
- Flag these as "tests MockProvisioning, not `<ClassName>`" for reviewer awareness

## Output format

For each file:

```
FILE: <relative path>
  Production class: com.zimbra.cs.account.Account  [FOUND | NOT FOUND (AL-1)]

  [AL-2] testGetAllIdentities_freshAccount — target method 'getAllIdentities' not found in Account.java
  [AL-3] isAccountStatusActive_activeAccount_returnsTrue (line 89) — REVIEW: asserting false for active account, verify logic
  [AL-4] import com.zimbra.cs.account.NonExistentHelper — cannot resolve
  [AL-5] getAllIdentities_freshAccount_containsDefaultIdentity (line 335) — asserts on MockProvisioning synthesized identity, not Account behavior

  VERDICT: FAIL (N blocking issues) | REVIEW (N soft flags) | PASS
```

Summary at the end:
```
ALIGNMENT SUMMARY
-----------------
AL-1 Missing production class : N
AL-2 Method not found         : N
AL-3 Inverted logic (review)  : N
AL-4 Unresolvable imports     : N
AL-5 MockProvisioning proxies : N (informational)
```
