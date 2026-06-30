# JUnit Governance — Category 1: Auto-Reject Check

Scan the JUnit test file(s) at `$ARGUMENTS` for patterns that warrant automatic rejection before human review. These are mechanical issues with zero tolerance — a test containing any of them must be fixed before the PR proceeds.

## What to check

For each `.java` file under `$ARGUMENTS` (recursive if a directory):

### AR-1: Vacuous assertions
- `assertTrue(true)` or `assertTrue("...", true)` anywhere in a test method
- `assertFalse(false)` or `assertFalse("...", false)` anywhere in a test method
- `assertEquals(x, x)` where both arguments are identical literals

### AR-2: Zero-assertion test methods
- Any `@Test` method body that contains no calls to: `assert*`, `verify(`, `fail(`, `assertThrows`, `expectThrows`, or `expected =` on the annotation
- Exception: `@Test(expected = SomeException.class)` with no body assertions is acceptable if the method body actually does something that should throw

### AR-3: Console output in tests
- `System.out.println(` inside a `@Test` method
- `System.err.println(` inside a `@Test` method
- `e.printStackTrace()` inside a `@Test` method (swallowed exception)

### AR-4: Static mutable fixture fields reassigned in @Before
- A `private static` field that is assigned (not just read) inside a `@Before` method
- Pattern: `static Account account;` + `account = prov.createAccount(...)` in `@Before`

### AR-5: Silent exception swallowing
- A `catch` block inside a `@Test` method that contains no assertion and no `fail()` call — just a comment or empty body

## Output format

For each file, print a section:

```
FILE: <relative path>
  [AR-1] Line 343: assertTrue(true) — vacuous assertion, cannot catch any regression
  [AR-2] Line 47: test() — @Test method with zero assertions
  [AR-3] Line 52: System.out.println("Elapsed=...") — CI noise, remove
  ...
  VERDICT: REJECT (N issues) | PASS
```

At the end print a summary table:

```
SUMMARY
-------
Files scanned : N
Auto-rejects  : N files
Clean files   : N files

Issues by type:
  AR-1 Vacuous assertions      : N
  AR-2 Zero-assertion tests    : N
  AR-3 Console output          : N
  AR-4 Static fixture antipattern: N
  AR-5 Silent catch            : N
```

If no files are found at the path, say so clearly.
