# JUnit Governance — Category 9: Maintenance Burden

Flag patterns in test file(s) at `$ARGUMENTS` that will become maintenance liabilities — tests that are likely to break on innocent refactors or that are hard to diagnose when they fail.

## What to check

### MB-1: Test methods exceeding 60 lines
- Any `@Test` method body longer than 60 lines
- Flag with: "Consider splitting — long tests mix multiple behaviors and are hard to diagnose on failure"

### MB-2: More than 3 assertEquals on same object in one test
- More than 3 `assertEquals` / `assertThat` calls targeting the same variable in one `@Test` method
- Flag: "Testing multiple behaviors in one test — split into focused tests"

### MB-3: Pinned internal log message strings
- String literals used in assertions that are clearly internal log messages (contain "system failure:", "ldap error:", "invalid encoded", etc.)
- Flag: "Brittle — log message wording is not a public API contract; will break on copy/typo fix"

### MB-4: Production line number references in comments
- Comments inside `@Test` methods that reference production source line numbers (e.g., `// kills L382`, `// covers branch at L273`)
- Flag: "Line-number references rot — replace with description of the business rule"

### MB-5: Magic strings that should be constants
- String literals that repeat across tests and look like they should be named constants:
  - Email addresses used 3+ times (e.g., `"user1@example.zimbra.com"`)
  - UUIDs used 2+ times (e.g., `"11111111-1111-1111-1111-111111111111"`)
  - Attribute names used 3+ times as raw strings
- Flag: "Extract to a named constant at the top of the class"

### MB-6: Javadoc longer than the test method
- Test methods with a Javadoc block (`/** ... */`) that is longer (in lines) than the method body itself
- AI tends to over-document tests; the test should be self-documenting via its name and structure

### MB-7: @Before that is nearly a no-op
- `@Before` methods that contain only `provisioning = Provisioning.getInstance()` or similar single-line idempotent calls
- Flag: "Useless @Before — move to field initializer or remove"

### MB-8: Test relies on account creation succeeding silently over duplicate
- `createAccount(` called in `@Before` with a hardcoded email without a guard for "already exists"
- Flag: "Relies on MockProvisioning silently accepting duplicates — add explicit deleteAccount or unique names"

## Output format

Per file:
```
FILE: <relative path>
  [MB-1] getAccountStatus_domainLocked_returnsLocked (line 697): 54 lines — consider splitting
  [MB-3] Line 92: assertEquals("system failure: invalid encoded size: 32", ...) — brittle message pin
  [MB-4] Line 179: // kills L43/L71 negate mutations — replace with business rule description
  [MB-5] "acct@example.com" appears 8 times — extract to private static final String TEST_ACCOUNT
  [MB-6] testGetAllIdentities — 9-line Javadoc for a 5-line method

  VERDICT: HIGH BURDEN (N issues) | MEDIUM (N issues) | LOW | PASS
```

Severity classification:
- HIGH: MB-1, MB-3 (brittle message pins), MB-8
- MEDIUM: MB-2, MB-4, MB-5
- LOW: MB-6, MB-7

Final summary:
```
MAINTENANCE BURDEN SUMMARY
--------------------------
MB-1 Tests > 60 lines               : N
MB-2 Multi-behavior tests           : N
MB-3 Pinned log message strings     : N
MB-4 Line number references         : N
MB-5 Magic strings (extract needed) : N
MB-6 Over-documented test methods   : N
MB-7 Near-empty @Before             : N
MB-8 Duplicate createAccount risk   : N

Overall burden: HIGH | MEDIUM | LOW
```
