# JUnit Governance — Category 8: Security-Sensitive Test Review

Flag test file(s) at `$ARGUMENTS` that touch authentication, cryptography, authorization, or token handling for mandatory human security review. AI-generated tests in these areas can be subtly wrong in ways that give false assurance about security properties.

## Trigger: which files get this review

Apply this check to any file whose name contains any of:
`AuthToken`, `Csrf`, `Password`, `Key`, `Encrypt`, `Decrypt`, `Sign`, `Hash`, `Token`, `Auth`, `Access`, `Permission`, `Right`, `Grant`, `Acl`, `Principal`, `Credential`

For files that don't match — still run the checks, but note that the trigger did not fire.

## What to check

### SEC-1: Hardcoded credentials / secrets
- String literals that look like: base64 blobs > 20 chars, hex strings > 32 chars, JWT-shaped strings (`xxxxx.yyyyy.zzzzz`), or strings containing `password`, `secret`, `token`, `key` as substrings
- Flag each: "Possible hardcoded credential — verify this is a test fixture value with no production sensitivity"

### SEC-2: Auth token encoding format asserted as internal detail
- Tests that assert on individual character positions or split-on-`:` of an encoded auth token string
- These pin internal serialization format — flag as "Security contract: needs explicit sign-off that this format is stable"

### SEC-3: Missing malformed-input test
- For any method that decodes, parses, or validates auth tokens/keys: verify at least one test passes a malformed, truncated, or corrupted input
- If only valid-input tests exist, flag: "No adversarial input coverage for <method>"

### SEC-4: Exception message leakage assertions
- Tests that assert exact exception messages containing internal paths, class names, or stack details (e.g., `"system failure: invalid encoded size: 32"`)
- Flag: "Exception message is a security contract — confirm this message is safe to expose to callers"

### SEC-5: Authorization bypass tests missing
- For `AccessManager`, `AclAccessManager`, `DomainAccessManager` tests: verify there is at least one test where access is expected to be DENIED
- If all `canDo`/`checkRight` tests assert access granted, flag: "No denial-path coverage — verify rejection logic is tested"

### SEC-6: Token expiration not tested
- For any `*AuthToken` test class: verify at least one test covers an expired token
- Common gap in AI-generated token tests

### SEC-7: Crypto algorithm specificity
- Test names or comments that reference a specific algorithm (SHA1, MD5, AES-128) — verify the production code actually uses that algorithm
- MD5 or SHA1 in auth contexts: flag as "Weak algorithm reference — confirm production is not using this"

## Output format

Per file:
```
FILE: <relative path>
  TRIGGER: FIRED (AuthToken in filename) — mandatory human security review required

  [SEC-1] Line 88: "AAAA...base64..." — verify not a real credential
  [SEC-3] encode/decode path has no malformed-input test
  [SEC-5] All canDo tests assert access GRANTED — no denial-path test found
  [SEC-6] No expired token test found in ZimbraAuthTokenTest

  VERDICT: MANDATORY HUMAN REVIEW | REVIEW RECOMMENDED | PASS
```

Final summary:
```
SECURITY SUMMARY
----------------
Files requiring mandatory review : N (trigger fired)
Files with review recommended    : N

SEC-1 Possible hardcoded secrets       : N
SEC-2 Encoding format assertions       : N
SEC-3 Missing malformed-input tests    : N
SEC-4 Exception message leakage        : N
SEC-5 No denial-path coverage          : N
SEC-6 Missing token expiration test    : N
SEC-7 Weak algorithm reference         : N
```
