# zm-mailbox — JUnit Test Guide

Captured patterns, build commands, and gotchas for writing JUnit tests in this repo.
Detailed test patterns for the `store` module live in [`store/JUNIT_TEST_GUIDE.md`](store/JUNIT_TEST_GUIDE.md).

## Project Overview
Zimbra Collaboration Suite Server (ZCS) — monorepo.
- **Language:** Java 8
- **Build:** Apache Ant + Ivy (NOT Maven/Gradle)
- **Tests:** JUnit 4
- **Key module for test work:** `store/`

## Build Commands

```bash
# Always use Java 8 — system Java (25) breaks JAXB
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home

# Build + publish a module locally (run from that module's directory)
ant clean publish-local -Dzimbra.buildinfo.version=10.1.6

# Build + publish ALL modules from repo root (needed before store coverage)
ant clean publish-local-all \
  -lib ~/.ivy2/cache/ant-contrib/ant-contrib/jars/ant-contrib-1.0b3.jar \
  -Dzimbra.buildinfo.version=10.1.6

# Run a single test class (from store/)
ant test -Dtest.name=PasswordUtilTest \
  -lib ~/.ivy2/cache/ant-contrib/ant-contrib/jars/ant-contrib-1.0b3.jar \
  -Dzimbra.buildinfo.version=10.1.6

# Run full test suite with JaCoCo coverage (from store/)
ant coverage \
  -lib ~/.ivy2/cache/ant-contrib/ant-contrib/jars/ant-contrib-1.0b3.jar \
  -Dzimbra.buildinfo.version=10.1.6
# Coverage report → store/build/coverage/index.html
```

## IDE Diagnostics Are False Positives
VS Code Java Language Server **cannot** resolve the Ant/Ivy classpath.
All red underlines in test files (`cannot be resolved`, `undefined method`) are false positives.
The Ant build compiles and runs tests correctly — trust `ant test`, not the IDE.

## Internal Ivy Dependencies
`store` depends on `zm-common`, `zm-soap`, `zm-client`, `zm-native` at `latest.integration`.
These must be published to `~/.zcs-deps/zimbra/` before `ant coverage` will resolve them.
Run `publish-local-all` from the repo root first if you hit "unresolved dependencies".
