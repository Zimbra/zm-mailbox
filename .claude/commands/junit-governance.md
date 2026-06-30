# JUnit Governance — Full Check

Run the complete JUnit governance review on test file(s) at `$ARGUMENTS`. This orchestrates all category checks and produces a single triage report that tells reviewers exactly where to focus their time.

## How to use

```
/junit-governance store/src/java-test/com/zimbra/cs/account/
/junit-governance store/src/java-test/com/zimbra/cs/account/AccessManagerTest.java
```

Pass a file path or directory. All `.java` files under a directory are scanned recursively.

## Execution order

Run all checks in this order. Do NOT skip a category because a file already failed a previous one — every finding is reported independently.

1. **Category 1 — Auto-Reject** (from `junit-auto-reject`)
   - AR-1: Vacuous assertions (`assertTrue(true)`)
   - AR-2: Zero-assertion test methods
   - AR-3: Console output (`System.out.println`)
   - AR-4: Static mutable fixture fields reassigned in `@Before`
   - AR-5: Silent exception swallowing

2. **Category 4 — Structural Completeness** (from `junit-structural`)
   - SC-1: Single-test class stubs
   - SC-2: FunctionalTest/IntegrationTest with no fixture setup
   - SC-3: @Ignore/@Disabled without a ticket reference
   - SC-4: Empty or comment-only test bodies
   - SC-5: Missing negative paths for exception-throwing methods

3. **Category 5 — Test-to-Production Alignment** (from `junit-alignment`)
   - AL-1: Production class not found
   - AL-2: Target method not found in production class
   - AL-3: Inverted/wrong assertion logic
   - AL-4: Unresolvable imports
   - AL-5: Tests that exercise MockProvisioning, not the class under test

4. **Category 6 — Test Independence** (from `junit-independence`)
   - TI-1: Hardcoded account/domain name collisions
   - TI-2: Static mutable field assigned in @Test
   - TI-3: Thread.sleep in test body
   - TI-4: Global state without guaranteed restore
   - TI-5: @BeforeClass/@AfterClass asymmetry
   - TI-6: Domain creation without cleanup

5. **Category 7 — Coverage Honesty** (from `junit-coverage-honesty`)
   - CH-1: assertNotNull-only tests
   - CH-2: Happy-path-only for branchy methods
   - CH-3: Swallowed-exception pattern
   - CH-4: verify(times(0)) as sole assertion
   - CH-5: Mutation-targeted tests with no stated contract
   - CH-6: Duplicate test coverage

6. **Category 8 — Security Review** (from `junit-security`)
   - SEC-1 through SEC-7
   - Files matching auth/crypto name patterns get MANDATORY HUMAN REVIEW verdict

7. **Category 9 — Maintenance Burden** (from `junit-maintenance`)
   - MB-1 through MB-8

8. **Categories 2 & 3 — Manual Justification & Quality Flags** (from `junit-manual-review`)
   - MJ-1 through MJ-6
   - QF-1 through QF-5

## Output format

### Per-file report

For each file, print a block:

```
══════════════════════════════════════════════════════
FILE: store/src/java-test/com/zimbra/cs/account/AccessManagerTest.java
══════════════════════════════════════════════════════

[CAT-1 AUTO-REJECT]
  ✗ AR-1 Line 343: assertTrue(true) — vacuous assertion
  ✗ AR-1 Line 539: assertTrue(true) — vacuous assertion
  ✗ AR-1 Line 565: assertTrue(true) — vacuous assertion
  ✗ AR-1 Line 599: assertTrue(true) — vacuous assertion

[CAT-4 STRUCTURAL]
  ✓ PASS

[CAT-5 ALIGNMENT]
  ✓ Production class AccessManager found
  ⚠ AL-5 Line 338: checkDomainStatus_activeDomain test asserts on MockProvisioning domain state, not AccessManager

[CAT-6 INDEPENDENCE]
  ⚠ TI-4 Line 165: static sManager modified via reflection — verify finally-restore is correct

[CAT-7 COVERAGE HONESTY]
  ✓ PASS

[CAT-8 SECURITY]
  ! TRIGGER FIRED (AccessManager in filename) — MANDATORY HUMAN REVIEW
  ✗ SEC-5 No denial-path tests found for canDo() — all tests assert access GRANTED

[CAT-9 MAINTENANCE]
  ⚠ MB-4 Line 179: // kills L43/L71 negate mutations — replace with business rule description
  ⚠ MB-5 "zimbra.com" used 12 times — extract to constant

[CAT-2/3 MANUAL + FLAGS]
  ! MJ-1 Line 165: assertEquals(ACLAccessManager.class, m.getClass()) — needs justification
  ~ QF-3 Line 47: test() — rename to describe scenario

FILE VERDICT:
  🔴 AUTO-REJECT       — fix before re-review (4 issues)
  🔴 SECURITY REVIEW   — mandatory human sign-off
  🟡 MANUAL REVIEW     — 1 item needs justification
  🟡 INDEPENDENCE      — 1 fragility flag
  🟢 STRUCTURAL        — pass
  🟢 COVERAGE          — pass
  ℹ  QUALITY FLAGS     — 1 informational
```

### Final triage dashboard

After all files, print:

```
╔══════════════════════════════════════════════════════════╗
║           JUNIT GOVERNANCE TRIAGE DASHBOARD              ║
╚══════════════════════════════════════════════════════════╝

Files scanned: N

🔴 AUTO-REJECT (fix before any review)
   N files | N total issues
   Top issues: AR-1 vacuous assertions (N), AR-2 zero-assertion tests (N)

🔴 MANDATORY SECURITY REVIEW
   N files — route to security reviewer before merge

🟡 MANUAL JUSTIFICATION REQUIRED
   N files | N items
   Reviewer focus: MJ-1 implementation assertions (N), MJ-4 mutation-killing (N)

🟡 STRUCTURAL GAPS
   N files | N issues
   Key gaps: SC-5 missing negative paths (N), SC-3 unlinked @Ignore (N)

🟡 INDEPENDENCE / FRAGILITY
   N files | N issues
   Key risks: TI-1 name collisions (N), TI-4 no-restore state mutation (N)

🟡 COVERAGE HONESTY
   N files | N issues
   Key risks: CH-1 assertNotNull-only (N), CH-5 mutation-only no contract (N)

🟢 MAINTENANCE BURDEN
   HIGH: N files | MEDIUM: N files | LOW: N files

ℹ  QUALITY FLAGS (informational, no action required)
   N files | N flags

──────────────────────────────────────────────────
REVIEW ROUTING RECOMMENDATION
──────────────────────────────────────────────────
  Author must fix before review  : N files (auto-reject)
  Security reviewer              : N files
  Tech lead / senior reviewer    : N files (manual justification)
  Standard reviewer              : N files (flags only)
  Ready to merge as-is           : N files
```

## Governance decision rules

| Condition | Action |
|---|---|
| Any AR-* finding | Return to author — do not review |
| Any SEC trigger fired | Block until security reviewer approves |
| AL-1 or AL-2 finding | Block — test is for non-existent code |
| 3+ MJ-* findings in one file | Escalate to tech lead |
| All categories pass | Standard reviewer can approve |
