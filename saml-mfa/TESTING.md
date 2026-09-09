# ZCS-20807 — Test procedure

How to verify email 2FA after SAML SSO. Companion document: [IMPLEMENTATION.md](IMPLEMENTATION.md).

The same gate serves the PreAuth door, so **a behaviour difference between the two doors is a bug**
— §7 is the check that catches it.

---

## 1. Setup

> **zm-mailbox is a public repository** (`github.com/Zimbra/zm-mailbox`). Host names, test-account
> passwords and — above all — the domain PreAuth key are **never** written into these files. They
> live in `saml-mfa/local.env`, which is gitignored.

```bash
cd zm-mailbox/saml-mfa
cp local.env.example local.env
$EDITOR local.env
```

| Setting | What it is |
|---|---|
| `ZIMBRA_HOST` / `ZIMBRA_DOMAIN` | the ZCS box under test |
| `TEST_USER` | account exercised by the tests (e.g. `rm1`) |
| `TEST_RECOVERY` | mailbox that receives the 2FA codes — must be a **different** account |
| `IDP_TEST_PASSWORD` | the IdP password of the test users |
| `PREAUTH_KEY` | `zmprov gdpak <domain>` — **a credential**, see the warning below |

> ⚠️ **`PREAUTH_KEY` mints a login for any account in the domain, with no password and no 2FA on the
> doors that remain ungated.** Treat it like a root password: never in a commit, a ticket, or a chat.
> If it leaks, rotate it — `zmprov gdpak <domain>` issues a new one and invalidates the old.

Every script reads `local.env`; the environment overrides it, so `PREAUTH_KEY=… ./preauth-login.py rm1`
also works. Run any script without configuration and it tells you which setting is missing.

### Endpoints

With `$H` = `ZIMBRA_HOST` and `$IDP` = your IdP realm base URL:

| Endpoint | URL |
|---|---|
| SP-initiated login | `https://$H/service/extension/samllogin` |
| IdP-initiated login | `$IDP/protocol/saml/clients/<idp-initiated-name>` |
| ACS (IdP posts here) | `https://$H/service/extension/samlreceiver` |
| Logout | `https://$H/service/extension/samllogout` |
| SLO receiver | `https://$H/service/extension/samlslo` |

The reference environment for this POC — host names, realm, IdP-initiated client name — is recorded
in the ticket, not here.

**Prerequisite:** the Keycloak user's email must exactly equal the Zimbra account address — the
NameID is resolved with `getAccountByName()` and there is no JIT provisioning.

Before anything else, confirm the real 2FA factory is loaded. The OSS default is a silent no-op that
makes the gate **fail open**, so every test below would pass for the wrong reason:

```bash
grep -i "two-factor auth factory" /opt/zimbra/log/mailbox.log | tail -1
# expect: Using two-factor auth factory ZimbraTwoFactorAuth
```

---

## 2. The three states

Everything in this document is moving an account between these and checking where it lands.

| `…FeatureTwoFactorAuthAvailable` | `…TwoFactorAuthEnabled` | Branch | Expected landing | Session? |
|---|---|---|---|---|
| FALSE | — | `NONE` | mailbox | issued |
| TRUE | FALSE | `SETUP` | `/modern/tfa-enroll.html?t=…` | issued |
| TRUE | TRUE | `CHALLENGE` | `/modern/tfa-challenge.html?tfa=…` | **withheld** |

```bash
# on the server, as the zimbra user
A=$TEST_USER@$ZIMBRA_DOMAIN     # from local.env

# -> NONE
zmprov ma $A zimbraFeatureTwoFactorAuthAvailable FALSE

# -> SETUP   (also the way back from CHALLENGE)
zmprov ma $A zimbraFeatureTwoFactorAuthAvailable TRUE \
             zimbraTwoFactorAuthMethodAllowed email \
             zimbraPrefPrimaryTwoFactorAuthMethod email \
             zimbraPrefPasswordRecoveryAddress $TEST_RECOVERY
zmprov ma $A zimbraTwoFactorAuthEnabled FALSE

# -> CHALLENGE
#    Cannot be set directly: `zmprov ma $A zimbraTwoFactorAuthEnabled TRUE` fails with
#    "cannot enable two-factor auth because a shared secret is unavailable".
#    The account must genuinely enrol -- run the SETUP flow (§4 or §6) to completion.
#    Turning it back OFF with zmprov does work, so the loop is:
#        SETUP -> enrol -> CHALLENGE -> zmprov ...Enabled FALSE -> SETUP
```

Check current state at any time:
```bash
zmprov ga $A zimbraFeatureTwoFactorAuthAvailable zimbraTwoFactorAuthEnabled \
             zimbraPrefPasswordRecoveryAddress zimbraPrefPasswordRecoveryAddressStatus
```

---

## 3. Reading the 2FA code out of the recovery mailbox

Needed for both the enrolment and challenge flows.

```bash
# newest message in the recovery mailbox
zmmailbox -z -m $TEST_RECOVERY search -l 1 -t message "in:inbox"
zmmailbox -z -m $TEST_RECOVERY getMessage <id>
```

**There are two different emails. Do not confuse them** — an old enrolment mail sitting in the inbox
is the classic false lead:

| Flow | Subject | Body |
|---|---|---|
| Enrolment | `Request for two-factor authentication email address verification…` | `verification code: aB3xY9Zq` (alphanumeric) |
| Login challenge | `Two-factor authentication code` | `code is: 1848086` (digits) |

**The login code lives 120 seconds** (`zimbraTwoFactorCodeLifetimeForEmail`) and delivery lags a few
seconds. Read it and submit it promptly. A code fetched a couple of minutes later fails as
`account.TWO_FACTOR_AUTH_FAILED`, which is indistinguishable from a wrong code.

---

## 4. Browser test — the main pass

Use a **private window for every attempt**. A stale `ZM_AUTH_TOKEN` short-circuits the whole flow and
is the single most common way to "prove" something that is not happening.

### 4.1 SETUP — enrolment

Put `rm1` in SETUP state (§2), then:

1. Open `https://$ZIMBRA_HOST/service/extension/samllogin`
2. IdP login form → your `TEST_USER` and `IDP_TEST_PASSWORD`
3. **Expect:** the enrolment page, `/modern/tfa-enroll.html?t=…`
4. Enter your `TEST_RECOVERY` address, click **Send code**
5. Read the code from the recovery mailbox (§3 — the *verification code* one) and submit it
6. **Expect:** success. Confirm on the server:
   ```bash
   zmprov ga $A zimbraTwoFactorAuthEnabled              # TRUE
   zmprov ga $A zimbraPrefPasswordRecoveryAddressStatus # verified
   ```

Also test **Skip**: from a fresh SETUP state, skip enrolment → you reach the mailbox, and you are
prompted again on the next sign-in. This is by design (see IMPLEMENTATION §3.2).

### 4.2 CHALLENGE — the security-critical case

`rm1` is now enrolled. New private window:

1. Open the `samllogin` URL, authenticate at Keycloak
2. **Expect:** the challenge page, `/modern/tfa-challenge.html?tfa=…&account=…&tfaEmail=r**@…`
3. **Before entering anything**, open devtools → Application → Cookies. This is the actual assertion:

   | Cookie | Expected |
   |---|---|
   | `ZM_AUTH_TOKEN` | **absent** |
   | `ZM_AUTH_SAML_DETAILS` | present |

   > A session cookie at this point means the gate has **failed open** — the user is already logged
   > in and the code prompt is decoration. This looks completely normal on screen, which is why the
   > cookie check is the real test.

4. The page mails a code on load. Read it from rm2's mailbox (§3 — the *digits* one) and submit it
   within 120s.
5. **Expect:** redirect to `/modern/`, and `ZM_AUTH_TOKEN` now present.

Also worth doing: click **Resend code** and confirm a new mail arrives; submit a deliberately wrong
code and confirm you stay on the page with an error rather than getting in.

### 4.3 NONE — regression

Put `rm2` in NONE state (`zimbraFeatureTwoFactorAuthAvailable FALSE`), sign in as `rm2`.
**Expect:** straight to the mailbox, exactly as before this change.

### 4.4 Logout

Sign in fully (any branch), then open `…/service/extension/samllogout`.

**Expect:** a round trip through Keycloak, ending logged out. This is the regression that catches a
missing SessionIndex — `/samllogout` returns **401** without the `ZM_AUTH_SAML_DETAILS` cookie.

Test it specifically **after a CHALLENGE login**, because that is the path where the cookie is written
outside `setAuthCookies()`. Login works fine either way, so a regression here hides unless you look.

### 4.5 IdP-initiated

Open the IdP-initiated URL for the client. Same three
branches apply. Useful for isolating assertion/cert/NameID problems from AuthnRequest problems,
since no AuthnRequest is sent at all.

---

## 5. Scripted test

`saml-mfa/` holds non-interactive drivers. All of them read `local.env` (§1) — there is nothing to
edit inside the scripts.

```bash
cd zm-mailbox/saml-mfa

./saml-login.py rm1        # full SP-initiated flow
./saml-login.py rm2
```

`saml-login.py` decodes the assertion and reports the branch. The `signed` field distinguishes a
Response-level signature from an Assertion-level one, because Zimbra only *requires* the latter when
the Response itself is unsigned:

```
4. got SAMLResponse; NameID = rm1@zcs.example.com
   Audience = https://zcs.example.com/service/extension/samlreceiver
   signed   = Response:True Assertion:True | AuthnStatement = True
5. after ACS post -> https://zcs.example.com/modern/tfa-challenge.html?tfa=…
BRANCH: CHALLENGE -> code prompt
  session cookie: no (withheld)
  SessionIndex  : yes
  RESULT: PASS
```

`RESULT` is branch-aware: CHALLENGE passes only when the session is **withheld** and the SessionIndex
is present; SETUP and NONE pass only when both are present.

---

## 6. Scripted enrolment and challenge legs

Both legs must complete inside the 120s code window — run them back to back.

**Enrolment** (account in SETUP state):
```bash
TOKEN=$(./saml-login.py rm1 | sed -n 's/.*[?]t=//p')
./saml-enrol.py "$TOKEN" send                    # -> sent
# read the *verification code* from rm2's mailbox
./saml-enrol.py "$TOKEN" validate <code>         # -> enabled
```

**Challenge** (account enrolled):
```bash
TFA=$(./saml-login.py rm1 | sed -n 's/.*[?]tfa=//p' | sed 's/&account.*//')
./saml-challenge.py "$TFA" "$TEST_USER@$ZIMBRA_DOMAIN" send        # -> sent
# read the *digits* code from rm2's mailbox
./saml-challenge.py "$TFA" "$TEST_USER@$ZIMBRA_DOMAIN" verify <code>
# -> SESSION ISSUED
```

The enrolment leg is the one that proves the whole point of the ticket: it enrols an account in 2FA
**without a password ever being involved**, which stock `EnableTwoFactorAuthRequest` cannot do.

---

## 7. Door parity — the highest-value check

Both doors call the same `SsoTwoFactorGate`. Run the same account through both; the branch must
match. A disagreement is a bug by definition.

```bash
./saml-login.py rm1     ; ./preauth-login.py rm1
./saml-login.py rm2     ; ./preauth-login.py rm2
```

```
BRANCH: CHALLENGE -> code prompt          # SAML
  rm1  -> CHALLENGE | session cookie: no  # PreAuth
```

---

## 8. Server-side verification

```bash
sudo tail -f /opt/zimbra/log/mailbox.log | grep -E "two-factor|samlreceiver"
```

| Log line | Means |
|---|---|
| `SAML login for <acct> handed off to two-factor auth.` | the gate fired on the SAML door |
| `info=two-factor auth required` | CHALLENGE |
| `info=two-factor auth enrolment required` | SETUP |
| *no gate line at all* | NONE — **or** the gate never ran, which is the bug you are hunting |

The security-log lines carry `cmd=SAML` or `cmd=PreAuth`, so the two doors are distinguishable.

---

## 9. Test matrix

| # | Case | Setup | Expected |
|---|---|---|---|
| 1 | SAML, feature off | rm2 NONE | mailbox, session issued |
| 2 | SAML, available not enrolled | rm1 SETUP | enrolment page, session issued |
| 3 | Enrolment completes | from 2 | `zimbraTwoFactorAuthEnabled TRUE`, no password used |
| 4 | Enrolment skipped | from 2 | mailbox; re-prompted next sign-in |
| 5 | SAML, enrolled | rm1 CHALLENGE | challenge page, **no `ZM_AUTH_TOKEN`** |
| 6 | Correct code | from 5 | session issued, lands in mailbox |
| 7 | Wrong code | from 5 | stays on page, error, still no session |
| 8 | Expired code (>120s) | from 5 | `TWO_FACTOR_AUTH_FAILED` |
| 9 | Logout after CHALLENGE | from 6 | clean SLO round trip, no 401 |
| 10 | PreAuth parity | each of 1/2/5 | same branch as SAML |
| 11 | IdP-initiated | any state | same branch as SP-initiated |
| 12 | SAML config test | `GenerateSamlTestRequest` | unaffected by tester's 2FA state |

---

## 10. Gotchas

- **Stale cookies.** Private window, every time.
- **120-second code lifetime**, plus delivery lag. See §3.
- **Two different code emails.** See §3.
- **Zimbra login page after Keycloak** ⇒ the challenge redirect is pointing at the SPA
  (`modern/`) instead of `modern/tfa-challenge.html`, or the page is not deployed. The SPA's auth
  guards assume "no auth token ⇒ login screen" and discard the handoff. See IMPLEMENTATION §6.
- **404 on every `/service/extension/saml*` path** ⇒ there is a stray `.bak` jar in
  `/opt/zimbra/lib/ext/saml/`. Zimbra loads every file in that directory; the backup registers as a
  second `SamlExtension` and the collision tears down the good copy's handlers. Backups belong in
  `/opt/zimbra/ext-backups/`.
- **Blanket HTTP 503** after a class injection ⇒ a nested class was omitted
  (`SsoTwoFactorGate$Decision`, `$1`).
- **Keycloak `Client signature required` must be OFF** — Zimbra sends unsigned AuthnRequests and
  LogoutRequests. (The `platform-dev-sdesouza` client in the same realm has it ON only because it is
  IdP-initiated only, where no AuthnRequest is ever verified. Do not copy it.)
- **Keycloak `Include AuthnStatement` must be ON**, or there is no SessionIndex and logout 401s.
  Login still works, so this hides until §4.4.
- **Never paste a PreAuth key anywhere.** It is the highest-value secret in this system. Keep it in
  `local.env`; rotate with `zmprov gdpak <domain>` if it is ever exposed.
- **Beware substring matching in your own test scripts.** During bring-up a check of
  `"sent" in response` matched the word "pre**sent**" in the fault *"no valid authtoken present"*,
  making a rejected request look successful. Match on `<status>sent</status>`.

---

## 11. Rollback

```bash
sudo cp /opt/zimbra/lib/jars/zimbrastore.jar.bak  /opt/zimbra/lib/jars/zimbrastore.jar
sudo cp /opt/zimbra/lib/jars/zimbracommon.jar.bak /opt/zimbra/lib/jars/zimbracommon.jar
sudo cp /opt/zimbra/ext-backups/samlextn.jar.orig /opt/zimbra/lib/ext/saml/samlextn.jar
sudo su - zimbra -c "zmmailboxdctl restart"
```

The enrolment and challenge pages can be left in place — nothing references them once
`TWO_FACTOR_CHALLENGE_PATH` and `TWO_FACTOR_ENROLL_PATH` are rolled back with the jar.
