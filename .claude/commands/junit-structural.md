# JUnit Governance — Category 4: Structural Completeness

Check that test file(s) at `$ARGUMENTS` are structurally complete — not stubs, not partial, and not missing the basic shape of a real test class.

## What to check

### SC-1: Single-test classes
- Any test class with exactly 1 `@Test` method
- Flag with: "Single test method — likely an incomplete stub; verify intentional"

### SC-2: Missing fixture setup on functional/integration tests
- Any class named `*FunctionalTest` or `*IntegrationTest` that has no `@Before`, `@BeforeClass`, `@BeforeEach`, or `@BeforeAll`
- These tests almost certainly need server/provisioning initialization

### SC-3: @Ignore / @Disabled without linked ticket
- Any `@Ignore` or `@Disabled` annotation
- Check for a comment above or on the same line that contains a ticket reference (e.g., `JIRA`, `ZCS-`, `#123`)
- Flag any that lack a reference: "Disabled test with no ticket — may be an AI placeholder"

### SC-4: Empty or comment-only test body
- `@Test` methods whose body contains only comments, whitespace, or a single `// TODO` line
- These are unfished tests that will always pass

### SC-5: Negative path coverage
- For each `@Test` that targets a method capable of throwing a checked exception (the production method has `throws` in its signature):
  - Check whether there is at least one test in the class that covers the exception path (uses `expected=`, `assertThrows`, or catches and asserts on the exception)
- If only happy-path tests exist for an exception-throwing method, flag as SC-5

### SC-6: Test class naming mismatch
- Test class name should end in `Test`, `Tests`, `FunctionalTest`, or `IntegrationTest`
- The base name (before the suffix) should match a production class name
- Flag mismatches

## Output format

Per file:
```
FILE: <relative path>
  [SC-1] Only 1 @Test method (test) — verify this is complete
  [SC-3] Line 22: @Ignore with no ticket reference — "broken after upgrade"
  [SC-5] Method createAccount() throws ServiceException but no negative test found
  VERDICT: INCOMPLETE (N issues) | PASS
```

Final summary:
```
STRUCTURAL SUMMARY
------------------
SC-1 Single-test stubs          : N
SC-2 Missing fixture setup      : N
SC-3 Unlinked @Ignore/@Disabled : N
SC-4 Empty test bodies          : N
SC-5 Missing negative paths     : N
SC-6 Naming mismatches          : N
```
