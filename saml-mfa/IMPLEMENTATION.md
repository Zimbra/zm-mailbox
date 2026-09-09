# ZCS-20807 — Email 2FA after SAML SSO

Implementation handover. Companion document: [TESTING.md](TESTING.md).

---

## 1. The problem

An SSO door proves that *some upstream system vouched for this user*. It does not prove a second
factor. Both of Zimbra's SSO doors used to ignore 2FA entirely and mint a full session:

| Door | Entry point | Old behaviour |
|---|---|---|
| PreAuth | `PreAuthServlet` (`/service/preauth`) | valid HMAC → usable auth token, 2FA never consulted |
| SAML | `SamlLoginReceiverHandler` (`/service/extension/samlreceiver`) | valid assertion → `setAuthCookies()`, 2FA never consulted |

ZCS-20575 fixed the PreAuth door. **ZCS-20807 fixes the SAML door, and in doing so makes both doors
share one implementation** — so they cannot drift apart.

Scope is deliberately narrow: **email** 2FA, browser flows, one customer. Not upstream-bound.

---

## 2. Repos and branches

All four are on `feature/ZCS-20807_claude_test`.

| Repo | Change | State |
|---|---|---|
| `zm-mailbox` | the shared gate + PreAuth refactor + two standalone pages | 1 commit (cherry-pick) + uncommitted work |
| `zm-saml-consumer-store` | call the gate from the SAML receiver | uncommitted |
| `zm-api-js-client` | `ssoTwoFactorSetup` client method | uncommitted, **unbuilt** |
| `zm-x-web` | SSO 2FA handoff in the login component | uncommitted, **unbuilt** |

> **The one commit.** `zm-mailbox` carries `30ebcaf7fd`, a cherry-pick of ZCS-20575 (`9844574fd9`).
> The POC branch was cut from `develop` and had none of the PreAuth work, so there was nothing to
> mirror or refactor. Everything else is working-tree only.

---

## 3. Design

### 3.1 The gate

`store/src/java/com/zimbra/cs/service/sso/SsoTwoFactorGate.java` — new, ~228 lines. This is the
whole design. It answers one question — *may this account be handed a session yet?* — and owns the
redirects for the two answers where it may not.

```java
public enum Decision { NONE, CHALLENGE, SETUP }

public static Decision evaluate(Account acct);
public static boolean  gate(HttpServletRequest, HttpServletResponse, Account, String door);
public static void     redirectToChallenge(...);
public static void     redirectToEnrolment(...);
```

`gate()` is the only method a door needs. It returns `true` if it wrote a redirect, meaning the
caller must stop:

```java
if (SsoTwoFactorGate.gate(req, resp, acct, "SAML")) {
    return;   // redirect written; do not issue a session
}
```

The `door` string ("PreAuth" / "SAML") appears as `cmd=` in the security log so the two are
distinguishable in an audit trail.

### 3.2 The decision

Two account attributes decide everything:

| `zimbraFeatureTwoFactorAuthAvailable` | `zimbraTwoFactorAuthEnabled` | Decision | Session issued? |
|---|---|---|---|
| FALSE | — | `NONE` | yes — straight to the mailbox |
| TRUE | FALSE | `SETUP` | **yes**, then redirected to enrolment |
| TRUE | TRUE | `CHALLENGE` | **no** — withheld until a code is accepted |

Two non-obvious choices, both inherited from ZCS-20575 and deliberately preserved:

**Keyed on *available*, not *required*.** The 2FA extension's `twoFactorAuthRequired()` is
`available && (required || enabled)`, so an account that merely *may* use 2FA reports
`required == false`. Keying off availability is what makes the enrolment prompt appear for accounts
that are allowed 2FA but have not set it up. Consequence: every un-enrolled account with the feature
available is prompted on every SSO sign-in until it enrols.

**SETUP issues the session before enrolment.** Enrolment is skippable — the user has already been
vouched for by the door — so withholding the session would gain nothing a skip would not give back.
Issuing it up front also lets the stock setup dialog authenticate by session rather than by password.
The security consequence is explicit: **an un-enrolled user can always reach the mailbox.** Only
enrolled users are truly gated.

### 3.3 Why a shared helper rather than duplicating in the extension

`zm-saml-consumer-store` ships as its own jar in `/opt/zimbra/lib/ext/saml/`. Zimbra's
`ZimbraExtensionClassLoader` extends `URLClassLoader` with **standard parent-first delegation**, so
every `com.zimbra.cs.*` class in `zimbrastore.jar` is visible to the extension for free. No plumbing,
no service loader, no reflection.

The cost is a build-order coupling: the extension compiles against `zm-store`, so a change to the
gate means publishing `zm-store` before building the extension, and deploying **both** jars together.
That is the trade for the two doors being unable to diverge. Given the whole point of the ticket is
that they behaved differently, it is the right trade.

---

## 4. Flows

### 4.1 SAML — CHALLENGE (enrolled user)

```
Browser            Zimbra                     Keycloak
   |                 |                            |
   |-- GET /service/extension/samllogin ---------->|
   |                 |-- 302 + AuthnRequest ------>|
   |<-------------------- login form --------------|
   |-- credentials ------------------------------->|
   |<------- auto-POST form w/ SAMLResponse -------|
   |-- POST /service/extension/samlreceiver ------>|
   |                 |
   |                 | validate signature, Audience, NotBefore/NotOnOrAfter
   |                 | getAccountByName(NameID)
   |                 | isAccountStatusActive()
   |                 | setSamlDetailsCookie()      <-- SessionIndex, ALWAYS
   |                 | SsoTwoFactorGate.gate() -> CHALLENGE
   |                 |   mint AuthToken(Usage.TWO_FACTOR_AUTH)
   |                 |   NO session cookie
   |<-- 302 /modern/tfa-challenge.html?tfa=..&account=..&tfaEmail=r**@..
   |                 |
   |-- SendTwoFactorAuthCodeRequest(action=email, authToken=tfa) -->
   |                 |   emails a 7-digit code to the recovery address (120s TTL)
   |-- AuthRequest(twoFactorCode=NNNNNNN, <authToken>tfa</authToken>) -->
   |                 |   Auth accepts a TWO_FACTOR_AUTH token in place of a password
   |<-- ZM_AUTH_TOKEN (real session) --------------|
   |-- redirect /modern/ ------------------------->|
```

### 4.2 SAML — SETUP (available, not enrolled)

Identical up to the gate, then:

```
   |                 | SsoTwoFactorGate.gate() -> SETUP
   |                 |   AuthProvider.getAuthToken(acct)          <-- real session, issued
   |                 |   AuthProvider.getAuthToken(acct, ENABLE_TWO_FACTOR_AUTH)
   |<-- 302 /modern/tfa-enroll.html?t=<enrolment token>
   |
   |-- PreAuthTwoFactorSetupRequest(action=sendCode, email=..) --> "sent"
   |-- PreAuthTwoFactorSetupRequest(action=validateCode, code=..) -> "enabled"
   |   (or Skip -> mailbox, prompted again next sign-in)
```

### 4.3 SAML — NONE

Gate returns false, `setAuthCookies()` runs, redirect to the mailbox. Unchanged from before.

### 4.4 PreAuth

Same three outcomes from the same code, reached after HMAC verification instead of assertion
validation. `!admin` still guards it — admin PreAuth is excluded.

---

## 5. Change inventory

### 5.1 zm-mailbox

| File | Change |
|---|---|
| `store/.../service/sso/SsoTwoFactorGate.java` | **NEW.** The gate. Decision + both redirects + URL building. |
| `store/.../service/PreAuthServlet.java` | **−128 / +6.** Deleted the private `TwoFactorState` enum, `twoFactorState()`, `redirectToTwoFactorChallenge()`, `redirectToTwoFactorEnrolment()`, `twoFactorChallengeBaseUrl()`; replaced with one `gate()` call. `PARAM_TFA*` constants kept as aliases of the gate's so existing references still compile. Three imports dropped. |
| `preauth-mfa/tfa-challenge.html` | **NEW, 167 lines.** Standalone challenge page. |
| `saml-mfa/` | **NEW.** Test kit + these docs + `ssoenv.py` / `local.env.example` (config loader; `local.env` is gitignored). |
| *(from the cherry-pick)* | `PreAuthTwoFactorSetup.java`, `AccountConstants.java` (+7), `AccountService.java` (handler registration), `preauth-mfa/{README.md,tfa-enroll.html,preauth-url.py,tfatest.sh}` |

`TWO_FACTOR_CHALLENGE_PATH` changed from `modern/` to `modern/tfa-challenge.html` — see §6.

### 5.2 zm-saml-consumer-store

`src/java/com/zimbra/cs/security/saml/SamlLoginReceiverHandler.java`, **+25 / −2**:

- import `SsoTwoFactorGate`
- in `handleSamlResponse()`, after the account-status check and **before** `setAuthCookies()`:
  call `setSamlDetailsCookie()`, then `gate()`, return if it handled the request
- extracted `setSamlDetailsCookie()` out of `setAuthCookies()`

**Why the split matters.** The SessionIndex cookie (`ZM_AUTH_SAML_DETAILS`) used to be written inside
`setAuthCookies()`. On the CHALLENGE path that method never runs, so the SessionIndex would be lost —
and `/samllogout` returns 401 without it, because it needs the SessionIndex to build its
`LogoutRequest`. Login would look perfectly fine; **logout would break silently.** The cookie is now
written on every accepted assertion, before the gate branches.

`setSamlDetailsCookie()` declares `throws ServiceException, ServletException` because
`SamlUtilities.getSessionIndex()` throws the latter.

The gate is skipped when `testCtx.isTest()` — the `GenerateSamlTest` config-test flow proves the
assertion round-trips and should not depend on the tester's own 2FA state.

### 5.3 zm-api-js-client — **scaffolding, unbuilt**

`ssoTwoFactorSetup` wired at four points:

| File | Change |
|---|---|
| `src/schema/schema.graphql` | `SsoTwoFactorSetupInput`, `SsoTwoFactorSetupResponse`, mutation |
| `src/schema/generated-schema-types.ts` | the two TS types — **hand-written; this file is normally codegen output, regenerate it** |
| `src/batch-client/index.ts` | `ssoTwoFactorSetup()` → `PreAuthTwoFactorSetupRequest` |
| `src/schema/schema.ts` | resolver |

### 5.4 zm-x-web — **scaffolding, unbuilt**

`src/components/login/index.js`, **+43 / −2**:

- `parseSsoTwoFactorHandoff()` reads `?tfa=`, `?account=`, `?tfaEmail=`
- state field `ssoTwoFactor`
- `componentDidMount()` drops straight into the challenge, skipping username/password
- `handleLogin()` sends `authToken` instead of `password` when in the SSO flow

Two open TODOs are marked in the file — see §9.

---

## 6. The two standalone pages

Both `tfa-enroll.html` and `tfa-challenge.html` are plain, dependency-free pages served from
`/opt/zimbra/jetty_base/webapps/zimbra/modern/`. They are **not** part of the SPA, and that is
deliberate.

The SPA has several auth guards that all assume *"no auth token ⇒ this user belongs on the login
screen"*. The challenge path issues no session cookie by design, so the SPA discards the handoff and
renders its own login form. During bring-up this presented exactly as **"after the Keycloak login I
get the Zimbra login page again"** — the backend was correct, the client simply did not understand
`?tfa=`.

Two ways to solve it:

1. **Standalone pages** (what is implemented). No web build, no SPA guard exemptions, works today.
2. **In-SPA** — the `zm-x-web` scaffolding in §5.4, requiring a CI build of `zimbra-modern-ui`, plus
   exemptions in each guard, plus surviving service-worker precaching.

They are not mutually exclusive. The standalone pages unblock the POC; the SPA route is the
production answer. If you adopt the SPA route, point `TWO_FACTOR_CHALLENGE_PATH` back at `modern/`.

---

## 7. SOAP contracts

### `PreAuthTwoFactorSetupRequest` (new, `urn:zimbraAccount`)

Password-free enrolment. Exists because stock `EnableTwoFactorAuthRequest` calls
`authAccount(password)` unconditionally on its first leg and accepts neither an enrolment token nor a
live session in its place — so an SSO user, who has no password, could never enrol.

Authenticated by an `ENABLE_TWO_FACTOR_AUTH`-scoped token, passed in the request because SOAP here
rejects cookie-only auth and the session cookie is `HttpOnly`.

```xml
<PreAuthTwoFactorSetupRequest xmlns="urn:zimbraAccount"
    action="sendCode" email="recovery@example.com" authToken="..."/>
<PreAuthTwoFactorSetupRequest xmlns="urn:zimbraAccount"
    action="validateCode" twoFactorCode="ABC12345" authToken="..."/>
```
Response: `<status>sent</status>` / `<status>enabled</status>`. Attributes, not child elements.

### `SendTwoFactorAuthCodeRequest` (existing, twofactorauth extension)

Child elements, not attributes:
```xml
<SendTwoFactorAuthCodeRequest xmlns="urn:zimbraAccount">
  <action>email</action><authToken>&lt;tfa token&gt;</authToken>
</SendTwoFactorAuthCodeRequest>
```

### `AuthRequest` second leg (existing)

`Auth.java` accepts an `authToken` with `Usage.TWO_FACTOR_AUTH` in place of a password:
```xml
<AuthRequest xmlns="urn:zimbraAccount" twoFactorCode="1234567">
  <account by="name">user@example.com</account>
  <authToken>&lt;tfa token&gt;</authToken>
</AuthRequest>
```

---

## 8. Environment

> **Secrets.** zm-mailbox is public. Nothing in `saml-mfa/` or `preauth-mfa/` hardcodes a host name,
> a test password or the domain PreAuth key — all of it comes from the gitignored
> `saml-mfa/local.env` (template: `local.env.example`), read by both the Python and shell scripts.
> The PreAuth key in particular mints a login for any account in its domain, so it is handled as a
> credential and rotated with `zmprov gdpak <domain>` if exposed.

### Zimbra — per account
```bash
zmprov ma <user> zimbraFeatureTwoFactorAuthAvailable TRUE \
                 zimbraTwoFactorAuthMethodAllowed email \
                 zimbraPrefPrimaryTwoFactorAuthMethod email \
                 zimbraPrefPasswordRecoveryAddress <recovery@addr>
```
Verify the real 2FA factory is loaded — the OSS default is a **silent no-op** that makes the gate
fail open:
```bash
grep -i "two-factor auth factory" /opt/zimbra/log/mailbox.log | tail -1
# expect: Using two-factor auth factory ZimbraTwoFactorAuth
```

### Zimbra — per domain (SAML)
```bash
zmprov md <domain> \
  zimbraSamlSpEntityId   'https://<host>/service/extension/samlreceiver' \
  zimbraSamlACSURL       'https://<host>/service/extension/samlreceiver' \
  zimbraSamlNameIdFormat 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress' \
  +zimbraSamlSSOURL 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect=<idp>/protocol/saml' \
  +zimbraSamlSLOURL 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect=<idp>/protocol/saml'
cat idp.pem | xargs -0 zmprov md <domain> zimbraMyoneloginSamlSigningCert
zmprov mcf +zimbraCsrfAllowedRefererHosts <idp-host>
```

Two traps worth knowing before you touch this config:

- **`zimbraSamlSpEntityId` must equal the ACS URL.** `SamlLoginReceiverHandler` compares the
  assertion's `<Audience>` host+path against `req.getRequestURL()`. Keycloak puts the *Client ID* in
  `<Audience>`, so the Keycloak Client ID must literally be the `samlreceiver` URL. The
  `saml_skip_audience_restriction` escape hatch is read only from the deprecated properties file,
  not from LDAP.
- **The IdP signing cert lives in the legacy attr `zimbraMyoneloginSamlSigningCert`**, even though
  every other setting moved to the per-domain `zimbraSaml*` attributes resolved by
  `SamlDomainConfigResolver`. PEM, with `BEGIN`/`END` lines.

### Keycloak SAML client

| Setting | Value | Why |
|---|---|---|
| Client ID | the `samlreceiver` URL | audience check above |
| Client signature required | **OFF** | Zimbra sends unsigned AuthnRequests *and* LogoutRequests |
| Sign documents | ON | something must be signed |
| Include AuthnStatement | **ON** | no SessionIndex otherwise → `/samllogout` 401s |
| Force POST binding | ON | matches the AuthnRequest's `ProtocolBinding` |
| Name ID format | `email` | NameID is looked up with `getAccountByName()` |
| Fine-grain SLO redirect URL | the `samlslo` URL | else logout is sent to `samlreceiver` |

There is **no JIT provisioning** — the Zimbra account must already exist.

---

## 9. Known limitations & open work

Carried over from ZCS-20575:

- **Only the HMAC PreAuth door is gated.** `/service/preauth?isredirect=1&authtoken=…` and the SOAP
  PreAuth branch in `Auth.java` still bypass 2FA. As a security control this remains incomplete.
- **Enrolment is a prompt, not a gate** (§3.2). Un-enrolled users always reach the mailbox.
- **Availability-keyed** — on a large tenant, every un-enrolled eligible account is prompted at once.
- **Admin PreAuth excluded** (`!admin`). The SAML path has no equivalent carve-out — decide whether
  it needs one.
- **Browser only.** IMAP/POP/ActiveSync unaffected.
- **CSRF.** The standalone pages' SOAP calls are not CSRF-protected; they authenticate with scoped
  tokens. Review before production.
- **The standalone pages are not the Zimbra design system.**

New to this ticket:

- **`zm-x-web` and `zm-api-js-client` are unbuilt and unverified.** Neither has been through CI.
- **`generated-schema-types.ts` was hand-edited.** Regenerate.
- **`TODO(ZCS-20807)` in `handleLogin()`** — assumes the login decorator forwards `authToken` through
  to `AuthRequest`. If it does not, the challenge leg needs its own mutation instead of reusing
  `login()`.
- **`TODO(ZCS-20807)` in `componentDidMount()`** — strip `tfa`/`account`/`tfaEmail` from the address
  bar once consumed, so a refresh or a shared URL cannot replay the handoff token.
- **`preauth-mfa/preauth-url.py` and `tfatest.sh` were de-hardcoded** as part of this ticket; they
  previously carried a live domain PreAuth key in-tree. If that key was ever pushed, rotate it.
- **No unit tests.** `SsoTwoFactorGate.evaluate()` is pure given an `Account` and is the obvious
  first target — three cases, no I/O.
- **Overlap to check:** `ParseSAMLMetadata` (ZCS-19567) already exists in `develop` — an admin SOAP
  API to fetch and parse IdP metadata. Likely overlaps any admin-UI story for SAML config.

---

## 10. Build and deploy

Build order matters — `common` → `soap` → `store`, then the extension:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
cd zm-mailbox/common && ant publish-local -Dzimbra.buildinfo.version=10.1.20
cd ../soap        && ant publish-local -Dzimbra.buildinfo.version=10.1.20
cd ../store       && ant publish-local -Dzimbra.buildinfo.version=10.1.20
cd ../../zm-saml-consumer-store && ant jar -Dzimbra.buildinfo.version=10.1.20
```

`AccountConstants` changed, so `common` **must** be published before `store` compiles. `soap` is in
the chain because `ParseSAMLMetadata` (pre-existing, unrelated) needs its JAXB classes.

Deploy — class injection into the shipped jars, as ZCS-20575 did:

```bash
# local
cd zm-mailbox/store/build/classes
COPYFILE_DISABLE=1 tar czf /tmp/pa.tgz \
  com/zimbra/cs/service/PreAuthServlet.class \
  com/zimbra/cs/service/sso/*.class \
  com/zimbra/cs/service/account/PreAuthTwoFactorSetup.class \
  com/zimbra/cs/service/account/AccountService.class
cd ../../../common/build/classes
COPYFILE_DISABLE=1 tar czf /tmp/common.tgz com/zimbra/common/soap/AccountConstants.class
scp /tmp/pa.tgz /tmp/common.tgz \
    ../../../zm-saml-consumer-store/build/samlextn.jar \
    preauth-mfa/tfa-enroll.html preauth-mfa/tfa-challenge.html ubuntu@<host>:/tmp/

# server
JAR=/opt/zimbra/common/lib/jvm/java/bin/jar          # there is no `jar` on root's PATH
sudo mkdir -p /opt/zimbra/ext-backups
sudo cp /opt/zimbra/lib/ext/saml/samlextn.jar /opt/zimbra/ext-backups/samlextn.jar.orig
sudo cp /opt/zimbra/lib/jars/zimbrastore.jar  /opt/zimbra/lib/jars/zimbrastore.jar.bak
sudo cp /opt/zimbra/lib/jars/zimbracommon.jar /opt/zimbra/lib/jars/zimbracommon.jar.bak
mkdir -p /tmp/inj /tmp/cinj && tar xzf /tmp/pa.tgz -C /tmp/inj && tar xzf /tmp/common.tgz -C /tmp/cinj
sudo chmod u+w /opt/zimbra/lib/jars/zimbra{store,common}.jar
(cd /tmp/inj  && sudo $JAR uf /opt/zimbra/lib/jars/zimbrastore.jar  $(find com -name '*.class'))
(cd /tmp/cinj && sudo $JAR uf /opt/zimbra/lib/jars/zimbracommon.jar $(find com -name '*.class'))
sudo chmod 444 /opt/zimbra/lib/jars/zimbra{store,common}.jar
sudo cp /tmp/samlextn.jar /opt/zimbra/lib/ext/saml/ && sudo chown zimbra:zimbra /opt/zimbra/lib/ext/saml/samlextn.jar
sudo cp /tmp/tfa-*.html /opt/zimbra/jetty_base/webapps/zimbra/modern/
sudo chmod 644 /opt/zimbra/jetty_base/webapps/zimbra/modern/tfa-*.html
sudo su - zimbra -c "zmmailboxdctl restart"
```

Three deployment traps, each of which cost real time:

- **Never leave a `.bak` jar in `/opt/zimbra/lib/ext/<name>/`.** Zimbra's loader scans *every* file
  in the directory, so the backup registers as a second `SamlExtension`; its duplicate `register()`
  throws, and the failure path unregisters by extension **name** — tearing down the working copy's
  handlers too. Symptom: HTTP 404 on every `/service/extension/saml*` path with *"Extension HTTP
  handler not found"*, and two `extension ... found in` lines in `mailbox.log`. Backups belong
  outside the ext tree.
- **`COPYFILE_DISABLE=1` when tarring from macOS**, or you inject AppleDouble `._*` entries into the
  jars.
- **Include nested classes.** `SsoTwoFactorGate$Decision.class` and `$1.class` must go in. Omitting
  nested classes makes the whole `/service` webapp fail to start, which surfaces as a blanket 503.

### Rollback
```bash
sudo cp /opt/zimbra/lib/jars/zimbrastore.jar.bak  /opt/zimbra/lib/jars/zimbrastore.jar
sudo cp /opt/zimbra/lib/jars/zimbracommon.jar.bak /opt/zimbra/lib/jars/zimbracommon.jar
sudo cp /opt/zimbra/ext-backups/samlextn.jar.orig /opt/zimbra/lib/ext/saml/samlextn.jar
sudo su - zimbra -c "zmmailboxdctl restart"
```
