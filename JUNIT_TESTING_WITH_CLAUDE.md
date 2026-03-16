# End-to-End Guide: Writing JUnit Tests with Claude Code

> **Purpose:** A reusable, repo-agnostic playbook for systematically increasing JUnit test
> coverage using Claude Code as an AI pair programmer.
>
> **Based on:** Real experience adding tests to `zm-mailbox` (Zimbra Collaboration Suite),
> a 300k-line Java 8 / Ant / JUnit 4 codebase with ~13% coverage at the start.
>
> **Works for any Java project** — Maven, Gradle, or Ant. Adapt the build commands to your
> toolchain; the strategy is identical.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Phase 1 — Build & Establish a Coverage Baseline](#2-phase-1--build--establish-a-coverage-baseline)
3. [Phase 2 — Analyse & Study the Codebase](#3-phase-2--analyse--study-the-codebase)
4. [Phase 3 — Save Learning for the Next Developer](#4-phase-3--save-learning-for-the-next-developer)
5. [Phase 4 — Write Tests Package by Package](#5-phase-4--write-tests-package-by-package)
6. [Session Management Prompts](#6-session-management-prompts)
7. [Common Gotchas & Fixes](#7-common-gotchas--fixes)
8. [Checklist — One Package Done](#8-checklist--one-package-done)
9. [Quick Reference: Test Strategy by Class Category](#9-quick-reference-test-strategy-by-class-category)

---

## 1. Prerequisites

| Tool | Purpose | Install |
|---|---|---|
| Claude Code | AI pair programmer with repo context | `npm install -g @anthropic/claude-code` |
| Java 8+ | Project runtime | Your distro / brew |
| Your build tool | Ant / Maven / Gradle | Already in the repo |
| JaCoCo | Coverage measurement | Add to build (see Phase 1) |

### Start Claude Code in your repo
```bash
cd your-repo/
claude
```
Claude Code reads `CLAUDE.md` from your repo root automatically on every session.
If it doesn't exist yet, you'll create it in Phase 3.

---

## 2. Phase 1 — Build & Establish a Coverage Baseline

### Goal
Get the project compiling, confirm the existing tests pass, measure coverage, and produce
a ranked table of the highest-value packages to test first.

---

### Step 1a — Confirm the build works

**Prompt:**
```
This is a [Java 8 / Maven / Gradle / Ant + Ivy] project.
Help me get the project compiling and the existing test suite running from scratch.

1. List every prerequisite (JDK version, env vars, tool versions).
2. Give me the exact commands to:
   - Build the project
   - Run the full test suite
   - Run a single test class
3. Identify any known gotchas (classpath ordering, version pins, generated sources, etc.).
4. If the build fails, diagnose the error and suggest a fix.
```

**Ant example (zm-mailbox):**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
ant clean test -Dzimbra.buildinfo.version=10.1.6
```

**Maven example:**
```bash
mvn clean test
```

**Gradle example:**
```bash
./gradlew clean test
```

---

### Step 1b — Add JaCoCo coverage instrumentation

**Prompt:**
```
Add JaCoCo coverage reporting to this [Ant/Maven/Gradle] project.

Requirements:
- Line coverage % per package
- Branch coverage % per package
- A CSV report I can sort in a spreadsheet
- An HTML report I can open in a browser
- Keep the change minimal — just instrument and report, no coverage gates

Show me:
1. The exact changes to the build file(s)
2. The command to run coverage
3. The path where the report will appear
```

**Maven — add to `pom.xml`:**
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```
```bash
mvn clean test        # report → target/site/jacoco/index.html
```

**Gradle — add to `build.gradle`:**
```groovy
plugins { id 'jacoco' }
jacocoTestReport { reports { csv.required = true } }
test { finalizedBy jacocoTestReport }
```
```bash
./gradlew clean test jacocoTestReport   # report → build/reports/jacoco/
```

---

### Step 1c — Diagnose build / coverage failures

If the coverage run fails, use this prompt:

**Prompt:**
```
The coverage run failed with this error:
[paste full error output]

Diagnose the root cause. Is it:
- A missing dependency?
- A classpath ordering problem?
- A JaCoCo version incompatibility?
- A test failure that's aborting the run?

Suggest the minimal fix and the command to verify it works.
```

---

### Step 1d — Generate the priority ranking table

Once the CSV report exists:

**Prompt:**
```
Read the JaCoCo CSV report at [path/to/jacoco.csv].

Produce a ranked table with these columns:
  Package | Total Lines | Lines Covered | Coverage % | Lines Missed | Priority Score

Sort by Lines Missed DESC (most uncovered code first).

Also flag:
- Packages with 0% coverage today
- Packages you estimate are business-critical (auth, payment, core logic)
- Packages that look like pure utilities (easy wins)

Explain your priority scoring formula so I can adjust it.
```

**What to look for:**
- Packages with 0% coverage and high line counts = highest ROI
- Business-critical packages (auth, payment, core logic) even if small = must-test
- Pure utility packages = easiest wins to start with

**Save this table** — you'll use it to sequence your work in Phase 4.

---

### Step 1e — Spot-check that the coverage numbers are trustworthy

**Prompt:**
```
I want to verify the JaCoCo numbers are reliable.

1. Pick the 3 packages with the most existing tests.
2. For each, compare: (a) what JaCoCo says is covered vs (b) what you find in the test files.
3. Do the numbers look consistent? If not, what might be wrong
   (test not running, wrong source root, bytecode not instrumented, etc.)?
```

---

## 3. Phase 2 — Analyse & Study the Codebase

### Goal
Before writing a single test, understand what you're testing.
Run this analysis **once per package** before writing tests for that package.

---

### Step 2a — Categorize all classes by testability

**Prompt:**
```
Analyze all source files in [com.example.targetpackage].

Categorize each class as exactly one of:
- PURE     — no I/O, no DB, no network, no static singletons → instantiate and call, no mocks
- INTEGRATION — uses DB / LDAP / network / filesystem / external APIs → needs mocking
- FRAMEWORK — lifecycle classes, servlets, handlers, DI containers → needs full stack

For each class give:
  ClassName | Category | Line Count | Brief Reason

Then summarize:
  - Count per category
  - Top 10 PURE classes by line count (highest ROI)
  - For each INTEGRATION class: list what external systems it calls
  - Which FRAMEWORK classes (if any) have extractable pure logic worth testing separately
```

---

### Step 2b — Deep-dive a single class

**Prompt:**
```
Analyze [com.example.MyClass] in depth.

1. Single responsibility — what is this class's ONE job? (one sentence)

2. For every public and package-private method:
   - Full signature (return type, name, parameters, checked exceptions)
   - Cyclomatic complexity estimate (count: if / else-if / for / while / do / catch / case /
     && / || — add 1 for the method itself)
   - External dependencies called (DB, LDAP, HTTP, file I/O, static singletons, etc.)
   - Whether it is pure (no external deps) or integration

3. Coverage bang-for-buck ranking:
   Sort methods by (CC × purity_bonus) where purity_bonus = 3 for PURE, 1 for INTEGRATION.
   These are the methods to test first.

4. Required test doubles:
   For each external dependency, name the specific mock/stub/fake strategy and
   any existing utility in this codebase that already provides it.
```

---

### Step 2c — Identify existing test infrastructure

**Prompt:**
```
Look at every file under [src/test/java or src/java-test].

For each test utility class, base class, or helper you find:
1. Full class name and file path
2. What it sets up / tears down
3. What helper methods it provides (with brief descriptions)
4. When to use it vs. write from scratch

Also list:
- Any reusable mock objects or fakes already defined
- Any JUnit @Rule or @ClassRule implementations
- Any shared fixture data (test accounts, test domains, sample XML, etc.)
```

---

### Step 2d — Understand the static-dependency landscape

**Prompt:**
```
Scan [com.example.targetpackage] for static-dependency patterns that make testing hard:

1. Static singletons — classes that have getInstance() or a static instance field
   → For each: is there a setInstance() or other injection seam?

2. Global config / property systems — static config reads like Config.get("key")
   → How are config values loaded? Is there a test-override mechanism?

3. Static utility calls — calls to static methods on framework classes
   → Which ones are pure (safe to call in tests) vs. infrastructure (need faking)?

4. Logging — which logging framework? Are log calls benign in tests or do they
   trigger infrastructure initialisation?

For each pattern found, recommend the simplest test double approach.
```

---

### Step 2e — Understand what a failing test looks like

Run one smoke test to validate the test infrastructure before writing dozens of tests:

**Prompt:**
```
Write a single minimal smoke-test for [com.example.SimpleClass].
It just needs to instantiate the class and call one method — no assertions needed.
The goal is to confirm the test compiles and the test runner finds it.

Include:
- The test file (correct package, correct annotations)
- The exact command to run just this one test
- What a passing result looks like in the output
```

---

## 4. Phase 3 — Save Learning for the Next Developer

### Goal
Write down everything you've learned so it doesn't need to be rediscovered.
This is the most important phase for team productivity.

---

### Step 3a — Create CLAUDE.md at the repo root

Claude Code **automatically reads** any file named `CLAUDE.md` at the start of every
session. This gives Claude (and teammates using Claude) permanent project context.

**Prompt:**
```
Based on everything we've learned in this session, create a CLAUDE.md at the repo root.

It must include:
1. Project overview (language, build tool, test framework, key modules)
2. Exact build commands that work — including required env vars and flags
3. How to run a single test class
4. How to run coverage and where the report appears
5. Any known gotchas (Java version requirements, classpath issues, false-positive IDE errors, etc.)
6. How to resolve "unresolved dependencies" if they appear
7. A one-line note pointing to the detailed JUnit test guide file

Keep it short — it's a quick-reference card, not a tutorial.
```

---

### Step 3b — Create a detailed JUNIT_TEST_GUIDE.md

**Prompt:**
```
Create [module]/JUNIT_TEST_GUIDE.md — a living document for test writers in this module.

Structure it with these sections:

1. Test Directory Layout
   - Diagram of src/test vs src/main and how they mirror each other
   - Where test resource files go

2. Shared Test Utilities
   - Every base class, helper, and utility in the test directory
   - For each: class name, path, what it provides, copy-paste usage example

3. Test Patterns & Conventions
   - JUnit version (4 or 5) and its specific annotations
   - Naming convention for test methods (methodName_scenario_expectedResult)
   - How to test expected exceptions
   - When to use @Before vs @BeforeClass

4. Handling Static Dependencies
   - Config/property system: how to override values in tests
   - Logging: how to capture log output for assertions
   - Singletons: how to replace the singleton during a test and restore it after

5. Class Categories & Test Strategy
   - PURE: strategy, example
   - INTEGRATION: strategy, which mock library to use, example
   - FRAMEWORK: strategy, base class to extend, example

6. Coverage Status Table
   - Package | Tests Written | Coverage % | Date | Notes

7. Per-Package Gotchas
   - One sub-section per package covered so far

Include real code snippets — copy-pasteable, not pseudocode.
```

---

### Step 3c — Keep CLAUDE.md thin using @import

**Prompt:**
```
Rewrite CLAUDE.md as a thin wrapper that imports the detailed guide:

The file should contain only:
  @JUNIT_TEST_GUIDE.md

Then confirm:
1. That JUNIT_TEST_GUIDE.md exists at the expected path
2. That the @import syntax will cause Claude Code to auto-load it
3. That the guide is readable by a teammate who clones the repo fresh
```

This way:
- `CLAUDE.md` is a 1-line file that auto-loads the guide
- `JUNIT_TEST_GUIDE.md` has a clear name teammates can find and read directly
- Committing both files gives every teammate the context automatically

---

### Step 3d — Create persistent memory for your Claude Code session

**Prompt:**
```
Save everything we've learned to your persistent memory so it's available in future sessions.

Create or update memory files under ~/.claude/projects/[project-path]/memory/:

1. MEMORY.md (top-level index, max ~150 lines)
   - Project overview
   - Current test coverage work status (what's done, what's next)
   - Key technical facts that always matter

2. [package-name]-analysis.md (one file per analysed package)
   - Package responsibility
   - Per-class category (PURE/INTEGRATION/FRAMEWORK) with line counts
   - Per-method signatures, CC, dependencies
   - Required test doubles and how to create them
   - Test files written and their test counts

3. test-utilities.md
   - Every reusable test utility with copy-paste code snippets
   - When to use each one
```

---

### Step 3e — After completing each package, update the guide

**Prompt:**
```
We just finished tests for [com.example.mypackage].

Update JUNIT_TEST_GUIDE.md:
1. Add a row to the Coverage Status Table:
   [package] | [N tests] | [new coverage %] | [today's date] | [one-line summary]

2. Add a sub-section under "Per-Package Gotchas" for [com.example.mypackage]:
   - What surprised us
   - Any test double setup that was non-obvious
   - Any classes we skipped and why
   - Anything that would save the next person an hour

3. Update MEMORY.md to reflect this package is complete.
```

---

## 5. Phase 4 — Write Tests Package by Package

### Goal
Systematically cover each package, starting with the highest-ROI classes.

---

### Step 4a — Choose the next package

Use the ranked table from Phase 1 and pick by this order:
1. **PURE classes with high line count** — fastest wins, no infrastructure needed
2. **Business-critical packages** — highest business value even if harder
3. **INTEGRATION classes** — after utilities and infrastructure are in place

**Prompt:**
```
Looking at the coverage table we built in Phase 1, help me choose the next package to test.

My constraints:
- I have [N hours] available
- I want to maximise lines covered per hour
- I want to avoid packages that need a running DB or LDAP server today

Given those constraints:
1. Which package should I target next?
2. How many PURE classes does it have?
3. What's a realistic coverage improvement target?
4. What infrastructure (if any) do I need before I can start?
```

---

### Step 4b — Package analysis before writing any tests

**Prompt:**
```
I'm about to write JUnit tests for [com.example.targetpackage].
Before I write anything, analyse the package.

1. List every source file with:
   - Category (PURE / INTEGRATION / FRAMEWORK)
   - Line count
   - Main responsibility (one sentence)

2. For the top 5 highest-value classes (PURE first, sorted by CC × line count):
   - Every public and package-private method: signature + CC + external deps
   - What test doubles I'll need and how to create them

3. Are there existing tests in this package I should read as style examples?

4. Which shared utilities from [ZimbraTestUtil / MyTestHelper] apply here?

5. Recommend the order to write the test files: easiest first → hardest last.
```

---

### Step 4c — Write tests for a PURE class

**Prompt:**
```
Write complete JUnit [4/5] tests for [com.example.PureClass].

Context:
- Category: PURE — no mocks needed
- Build system: [Ant/Maven/Gradle] — IDE red underlines are false positives; trust ant/mvn/gradle test
- Test file location: [src/java-test/com/example/PureClassTest.java]

Requirements:
- Cover every public and package-private method
- For each method test: happy path, edge cases, boundary values, null inputs
- Test expected exceptions with @Test(expected = ...) or assertThrows
- Name tests as: methodName_scenario_expectedResult
- Use import static org.junit.Assert.* (JUnit 4) or import static org.junit.jupiter.api.Assertions.* (JUnit 5)
- No mocking libraries needed

After writing, tell me:
- How many tests were written
- Which methods still need edge-case coverage
- Any method you couldn't reach without mocking (so I can handle it separately)
```

---

### Step 4d — Write tests for an INTEGRATION class (config/singleton dependency)

**Prompt:**
```
Write complete JUnit [4/5] tests for [com.example.IntegrationClass].

Context:
- Category: INTEGRATION
- It reads from [config system / singleton / static state]
- Available override utility: [ZimbraTestUtil.setLcKey() / MyTestUtil.setConfig()]
- Test file location: [src/java-test/com/example/IntegrationClassTest.java]

Requirements:
- Use [utility] to override config values; restore in @After or via @Rule
- For singletons: save the current instance in @Before, restore in @After via reflection
- Cover: happy path, config-overridden behaviour, missing-config defaults, error paths
- Keep each test independent — no shared mutable state between @Test methods

Include the @Before/@After setup and teardown code in full.
```

---

### Step 4e — Write tests for an INTEGRATION class (Provisioning / DB dependency)

**Prompt:**
```
Write complete JUnit [4/5] tests for [com.example.ServiceClass].

Context:
- Category: INTEGRATION
- External dependencies: [Provisioning / DB / LDAP / HTTP]
- Mock utilities available: [ZimbraTestUtil.installMockProvisioning() / Mockito.mock()]
- Test file location: [src/java-test/com/example/ServiceClassTest.java]

Requirements:
- Install the mock [Provisioning] in @BeforeClass or @Before
- Restore in @AfterClass or @After
- Stub only the methods that [ServiceClass] actually calls — don't over-stub
- Cover:
    - Happy path (successful result)
    - Not-found path (e.g., null account, empty result set)
    - Exception path (e.g., ServiceException, IOException)
- Use [Mockito.when(...).thenReturn(...)] for stubbing
- Use [Mockito.verify(...)] where you need to assert a call was made

After writing, list every Mockito stub you added so I can verify the setup is minimal.
```

---

### Step 4f — Write tests for a FRAMEWORK class (handler / servlet)

**Prompt:**
```
Write complete JUnit [4/5] tests for [com.example.MyHandler].

Context:
- Category: FRAMEWORK — needs full stack initialisation
- Base class available: [BaseAdminTest / MailboxTestUtil]
- This handler handles [describe the request]
- Test file location: [src/java-test/com/example/MyHandlerTest.java]

Requirements:
- Extend [BaseAdminTest] or call [MailboxTestUtil.initServer()] in @BeforeClass
- Build a minimal valid request XML/object
- Execute the handler using the provided execute() helper or directly
- Assert on the response element / returned data
- Cover:
    - Successful request from an admin account
    - Successful request from a delegated admin
    - Request that fails due to missing permission → expect ServiceException
    - Request with invalid/missing parameters → expect INVALID_REQUEST

Keep the test XML minimal — only include elements the handler actually reads.
```

---

### Step 4g — Verify tests compile and pass

After writing any test file:

**Ant:**
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  ant test -Dtest.name=MyClassTest \
  -lib ~/.ivy2/cache/ant-contrib/ant-contrib/jars/ant-contrib-1.0b3.jar \
  -Dzimbra.buildinfo.version=10.1.6
```

**Maven:**
```bash
mvn test -Dtest=MyClassTest
```

**Gradle:**
```bash
./gradlew test --tests "com.example.MyClassTest"
```

**If a test fails, use this prompt:**
```
This test failed with the following error. Fix it.

Test: [TestClassName#testMethodName]
Full error output:
[paste complete stack trace and any preceding output]

Before suggesting a fix:
1. Identify the root cause (not the symptom).
2. Check whether the failure is a test-code bug or a production-code bug.
3. If it's a test bug, show the minimal change to the test.
4. If it's a production bug, describe what you found and ask before touching production code.
```

---

### Step 4h — Handle tests that are hard to isolate

**Prompt — static method without a mock seam:**
```
[com.example.MyClass.myMethod()] calls the static method [SomeUtil.doThing()],
which has side effects I can't prevent in a unit test.

Options I know about:
1. PowerMock (already on classpath) — can mock static methods
2. Subclass-and-override — extract the static call to a protected method and override in a test subclass
3. Skip and test indirectly through a higher-level test

For this specific case, which option produces the most maintainable test?
Show me the minimal code change needed.
```

**Prompt — test isolation failure (tests pass alone, fail together):**
```
These tests pass individually but fail when run together:
[list test class names]

The failure message is:
[paste the error]

Diagnose:
1. What static or instance state is leaking between tests?
2. Which test is the "polluter" and which is the "victim"?
3. Show the exact @Before/@After code needed to save and restore the leaked state.
```

---

### Step 4i — Measure coverage improvement

After each batch of tests:

**Prompt:**
```
Re-run coverage for [module] and re-read the CSV at [path/to/jacoco.csv].

Tell me:
1. What is the new coverage % for [com.example.targetpackage]?
2. Which specific methods are still 0% covered?
3. For each uncovered method, estimate: is it worth testing, or is it dead/generated/trivial code?
4. Update the Coverage Status Table in JUNIT_TEST_GUIDE.md with the new numbers.
```

---

### Step 4j — Repeat for next package

**Prompt:**
```
[com.example.lastpackage] tests are done.

Looking at the priority table, what's the next best package to test?
Give me:
1. Package name
2. PURE class count and total line count
3. What infrastructure I'll need
4. Whether any of the patterns we built (ZimbraTestUtil / BaseAdminTest) apply directly
```

---

## 6. Session Management Prompts

### Start a new session
```
Resume JUnit test writing for [project-name].

Context:
- Read CLAUDE.md and JUNIT_TEST_GUIDE.md for project setup and existing patterns.
- Read your memory files for current status (especially MEMORY.md).
- Current status: [e.g., "auth package complete, accesscontrol package 70% done"]
- Today's goal: [e.g., "finish the remaining 7 classes in accesscontrol"]

Before starting, confirm you have read:
1. CLAUDE.md (build commands, classpath, known gotchas)
2. JUNIT_TEST_GUIDE.md (test utilities, patterns, per-package notes)
3. MEMORY.md (current progress)
```

### End a session — save state
```
We're wrapping up this session. Before I close:

1. Update MEMORY.md with:
   - What we completed today (package names, test counts)
   - What's next (next target package)
   - Any new gotchas discovered

2. Update JUNIT_TEST_GUIDE.md with:
   - Mark today's packages as complete in the Coverage Status Table
   - Add per-package gotcha notes for anything non-obvious

3. Confirm everything is committed / saved so the next session can pick up cleanly.
```

### Hand off to a teammate
```
A teammate is picking up the JUnit test work for [project-name].
They have Claude Code installed but have never worked on this repo.

Write them a 1-page handoff note that covers:
1. What the goal is (target coverage %, which module)
2. What's already done (packages complete, test file locations)
3. What's next (next package, priority order)
4. The 3 most important gotchas they'll hit in the first hour
5. The one command they need to run first to verify their setup works
```

---

## 7. Common Gotchas & Fixes

### IDE shows red underlines in test files
**Cause:** IDE cannot resolve the build tool's classpath (common with Ant/Ivy).
**Fix:** These are false positives. Trust `mvn test` / `ant test` / `./gradlew test`, not the IDE.

**Prompt:**
```
My IDE shows red underlines on [import statement or method call] in the test file.
The build tool (ant/mvn/gradle) compiles and runs the test correctly.
Is this a false positive from the IDE not seeing the classpath, or a real error?
If it's a real error, what's the fix?
```

---

### Tests pass in isolation but fail together
**Cause:** Static state leaking between tests (singletons, registries, caches).
**Fix:** Save static state in `@Before`/`@BeforeEach`, restore in `@After`/`@AfterEach`.
```java
private static final Field MY_SINGLETON;
static {
    MY_SINGLETON = MyClass.class.getDeclaredField("instance");
    MY_SINGLETON.setAccessible(true);
}
private Object saved;
@Before public void save()    { saved = MY_SINGLETON.get(null); }
@After  public void restore() { MY_SINGLETON.set(null, saved); }
```

---

### Config/property values don't change in tests
**Cause:** The config system caches expanded values; setting the underlying field isn't enough.
**Fix:** Also evict the key from the config cache. See `ZimbraTestUtil.setLcKey()` for the pattern.

**Prompt:**
```
I set [Config.someKey] to a test value via reflection but the code under test
still reads the old value.
The config class is [FullClassName].
How does it cache values, and what's the minimal code to bust that cache in a test?
```

---

### Can't mock a static method without PowerMock
**Cause:** Mockito (pre-v5) can't mock statics. PowerMock can, but adds complexity.
**Better fix:** Extract the static call to a protected method and override it in a test subclass:
```java
// Production code
protected String getCurrentUser() { return StaticClass.getUser(); }

// Test
MyClass sut = new MyClass() {
    @Override protected String getCurrentUser() { return "testuser"; }
};
```

---

### Tests need DB/LDAP but you want unit tests
**Cause:** Class mixes business logic with infrastructure calls.
**Short-term fix:** Mock the infrastructure calls with Mockito.
**Long-term fix:** Extract an interface, inject via constructor — then tests just use a fake.

**Prompt:**
```
[com.example.MyClass] mixes pure business logic with direct LDAP/DB calls.
I want to write a unit test for the pure logic without spinning up LDAP/DB.

Option A: Mock the LDAP/DB calls with Mockito — show me the setup.
Option B: Identify what interface/method to extract so I can inject a fake.

Which option is less invasive for this specific class? Show me the code for the better option.
```

---

### Coverage report shows a class as 0% but tests exist
**Cause:** The test class isn't in the right directory / not picked up by the test runner.
**Fix:** Verify the test file is in the correct source root and the class name ends in `Test`.

**Prompt:**
```
JaCoCo shows [com.example.MyClass] at 0% coverage even though I have [MyClassTest.java].
The tests pass when I run them explicitly.
What could cause coverage to not be recorded?
Check: source root, test class name convention, JaCoCo agent attachment, fork configuration.
```

---

### Unresolved dependency during the build
**Prompt:**
```
The build failed with:
[paste the unresolved dependency error]

This is an [Ant+Ivy / Maven / Gradle] project.
What does this mean and how do I fix it?
Specifically:
- Is this a missing JAR?
- Is it a local publish step I need to run first?
- Is there a repository URL I need to add?
```

---

## 8. Checklist — One Package Done

Use this after completing tests for each package:

```
Package: com.example.mypackage
Date: YYYY-MM-DD

Pre-work:
[ ] Coverage baseline measured for this package
[ ] All classes categorized (PURE/INTEGRATION/FRAMEWORK)
[ ] Highest-value methods identified by CC analysis
[ ] Existing test utilities reviewed

Test writing:
[ ] PURE classes: all public/package-private methods covered
[ ] INTEGRATION classes: mocks in place, happy + error paths covered
[ ] Edge cases covered: null inputs, empty collections, boundary values
[ ] Exception paths covered: expected exceptions asserted

Verification:
[ ] All new tests pass: mvn test / ant test / ./gradlew test
[ ] No test isolation issues (tests pass in any order)
[ ] Coverage improvement measured and recorded

Documentation:
[ ] JUNIT_TEST_GUIDE.md updated with package status
[ ] Gotchas added to per-package notes in the guide
[ ] CLAUDE.md / JUNIT_TEST_GUIDE.md committed to repo
```

---

## 9. Quick Reference: Test Strategy by Class Category

| Category | Strategy | Mocks Needed | Startup Cost |
|---|---|---|---|
| **PURE** | Instantiate + call | None | None |
| **INTEGRATION - Config** | Override config key via reflection | Config system | Low |
| **INTEGRATION - Logging** | Install capturing log appender | None (benign) | Low |
| **INTEGRATION - Singleton** | Replace singleton via `setInstance()` or reflection | Mockito | Low |
| **INTEGRATION - DB** | In-memory DB (H2/HSQLDB) or mock DAO | DB driver | Medium |
| **INTEGRATION - LDAP** | MockProvisioning or structural tests | LDAP client | Medium |
| **INTEGRATION - HTTP** | Mock HTTP client or WireMock | HTTP client | Medium |
| **FRAMEWORK - Handler** | Extend base test class with full stack | Full infra | High |
| **FRAMEWORK - Servlet** | MockMvc / Spring Test or integration test | Servlet container | High |

---

## Appendix: Prompt Cheat-Sheet (One-Liners)

| Task | Prompt starter |
|---|---|
| Get the build working | `"Help me compile and run tests in this [Java/Ant/Maven/Gradle] project from scratch."` |
| Add JaCoCo | `"Add JaCoCo to this [Ant/Maven/Gradle] project — line%, branch%, CSV output."` |
| Rank packages | `"Read jacoco.csv at [path]. Rank by lines_missed DESC. Flag 0% packages."` |
| Categorize a package | `"Categorize all classes in [package] as PURE/INTEGRATION/FRAMEWORK with line counts."` |
| Analyse a class | `"Analyse [Class]: responsibility, method signatures, CC, deps, bang-for-buck ranking."` |
| Find test utilities | `"List all test base classes and helpers under [src/test]. What does each provide?"` |
| Write PURE tests | `"Write full JUnit 4 tests for [PureClass]. No mocks. Cover all branches."` |
| Write INTEGRATION tests | `"Write JUnit 4 tests for [IntClass]. Mock [dependency] with [utility]. Cover happy + error paths."` |
| Write FRAMEWORK tests | `"Write JUnit 4 tests for [Handler]. Extend [BaseTest]. Cover success + permission failure."` |
| Fix a failing test | `"This test failed. Diagnose root cause and fix: [paste stack trace]"` |
| Measure improvement | `"Re-read jacoco.csv. What did [package] improve to? Which methods are still 0%?"` |
| Save learnings | `"Update JUNIT_TEST_GUIDE.md: mark [package] complete, add date, count, and gotchas."` |
| Start next session | `"Resume JUnit test writing. Read CLAUDE.md + JUNIT_TEST_GUIDE.md. Status: [current state]. Today's goal: [target]."` |

---

*This guide was created from real test-writing sessions on the zm-mailbox project.
Update it as you discover new patterns, gotchas, and utilities.*
