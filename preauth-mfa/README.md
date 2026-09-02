# PreAuth + Email MFA (one-off)

Enforces an email second factor **after** a trusted PreAuth/SSO sign-in, and lets users who have
never set 2FA up enrol without a password.

> **Scope:** this is a one-off for a specific client. It is not upstream Zimbra and is not intended
> to be. It patches jars in place and ships a standalone HTML page.

---

## 1. Why it exists

Stock Zimbra PreAuth bypasses 2FA entirely. In `Auth.java` the `preAuthEl` branch is a *sibling* of
the whole 2FA block, and `PreAuthServlet` never mentions two-factor at all — a valid PreAuth
signature goes straight to a usable auth token.

This change adds a gate immediately after the HMAC is verified.

| Account state | Outcome |
|---|---|
| 2FA enrolled | **CHALLENGE** — `TWO_FACTOR_AUTH` token, **no session cookie** until the code is verified |
| `zimbraFeatureTwoFactorAuthAvailable=TRUE`, not enrolled | **SETUP** — session cookie + enrolment page, skippable |
| Feature off | Normal login, unchanged |

Skip policy: skipping issues the session and leaves the account un-enrolled; the user is prompted
again on the next PreAuth sign-in.

---

## 2. What is where

| Repo | Files |
|---|---|
| **zm-mailbox** | `store/…/cs/service/PreAuthServlet.java` (gate) · `store/…/cs/service/account/PreAuthTwoFactorSetup.java` (**new** SOAP handler) · `AccountConstants.java` · `AccountService.java` |
| **zm-mailbox** | `preauth-mfa/tfa-enroll.html` (**new** enrolment page) · `preauth-mfa/tfatest.sh` · `preauth-mfa/preauth-url.py` |
| **zm-x-web** | `src/components/login/index.js` · `src/graphql/queries/login.graphql` · `src/components/app/app.js` · `src/lib/util.js` |
| **zm-api-js-client** | `src/schema/schema.graphql` · `generated-schema-types.ts` · `batch-client/types.ts` · `batch-client/index.ts` |

Branch on all three: `spike/ZCS-20575_claude_test`.

The stock `twofactorauth` extension is **not** modified — it still supplies code generation,
validation and the enable primitives.

---

## 3. Build

### 3.1 Backend (zm-mailbox)

```bash
cd zm-mailbox
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home   # or your JDK 8
ANT_LIB=~/.ivy2/cache/ant-contrib/ant-contrib/jars/ant-contrib-1.0b3.jar

# AccountConstants changed, so common must be published before store compiles
(cd common && ant publish-local -lib $ANT_LIB -Dzimbra.buildinfo.version=10.1.20)
(cd soap   && ant publish-local -lib $ANT_LIB -Dzimbra.buildinfo.version=10.1.20)
(cd store  && ant compile       -lib $ANT_LIB -Dzimbra.buildinfo.version=10.1.20)
```

Output classes land in `store/build/classes` and `common/build/classes`.

### 3.2 Frontend

zm-x-web is built by **CircleCI**, not locally (the repo pins node `^18.20.0` and carries an old
npm-v1 lockfile whose upgrade resolution is very slow).

**Build order matters:** `zm-api-js-client` must be published **first**, and zm-x-web's dependency
bumped to that version. zm-x-web currently pins `"@zimbra/api-client": "^101.0.0"`, which resolves
`>=101.0.0 <102.0.0` — it will **not** pick up a 102.x publish. Building zm-x-web before the new
api-client lands produces a green build that fails in the browser with
*"Unknown argument authToken on field Mutation.login"*.

---

## 4. Deploy

### 4.1 Backend classes

```bash
# on your machine
cd zm-mailbox/store/build/classes
tar czf /tmp/pa.tgz \
  com/zimbra/cs/service/PreAuthServlet*.class \
  com/zimbra/cs/service/account/PreAuthTwoFactorSetup.class \
  com/zimbra/cs/service/account/AccountService.class
cd ../../../common/build/classes
tar czf /tmp/common.tgz com/zimbra/common/soap/AccountConstants.class
scp /tmp/pa.tgz /tmp/common.tgz ubuntu@<host>:/tmp/

# on the server
sudo cp /opt/zimbra/lib/jars/zimbrastore.jar  /opt/zimbra/lib/jars/zimbrastore.jar.bak
sudo cp /opt/zimbra/lib/jars/zimbracommon.jar /opt/zimbra/lib/jars/zimbracommon.jar.bak
mkdir -p /tmp/inj /tmp/cinj
tar xzf /tmp/pa.tgz     -C /tmp/inj
tar xzf /tmp/common.tgz -C /tmp/cinj
sudo chmod u+w /opt/zimbra/lib/jars/zimbrastore.jar /opt/zimbra/lib/jars/zimbracommon.jar
(cd /tmp/inj  && sudo jar uf /opt/zimbra/lib/jars/zimbrastore.jar  $(find com -name '*.class'))
(cd /tmp/cinj && sudo jar uf /opt/zimbra/lib/jars/zimbracommon.jar $(find com -name '*.class'))
sudo chmod 444 /opt/zimbra/lib/jars/zimbrastore.jar /opt/zimbra/lib/jars/zimbracommon.jar
sudo su - zimbra -c "zmmailboxdctl restart"
```

> **Include the nested classes.** `PreAuthServlet$TwoFactorState.class` and `PreAuthServlet$1.class`
> must go in too — the `PreAuthServlet*.class` glob covers them. Omitting them makes the whole
> `/service` webapp fail to start with `NoClassDefFoundError`, which surfaces as a blanket HTTP 503.

### 4.2 Enrolment page

```bash
scp preauth-mfa/tfa-enroll.html ubuntu@<host>:/tmp/
sudo cp /tmp/tfa-enroll.html /opt/zimbra/jetty_base/webapps/zimbra/modern/
sudo chown root:root /opt/zimbra/jetty_base/webapps/zimbra/modern/tfa-enroll.html
sudo chmod 644       /opt/zimbra/jetty_base/webapps/zimbra/modern/tfa-enroll.html
```

`PreAuthServlet` redirects to `<zimbraMailURL>/modern/tfa-enroll.html`. If your `zimbraMailURL`
differs, adjust `TWO_FACTOR_ENROLL_PATH` / `TWO_FACTOR_CHALLENGE_PATH` in the servlet.

### 4.3 Web client

Deploy the CI-built `zimbra-modern-ui` .deb as usual (`dpkg -i`). Its maintainer scripts are empty —
it is a plain file drop.

---

## 5. Server configuration

```bash
# per domain — generate the PreAuth key (share with the upstream/portal, keep it secret:
# it can mint a login for ANY account in the domain)
zmprov gdpak <domain>

# per account — make 2FA available; email as the method
zmprov ma <user> zimbraFeatureTwoFactorAuthAvailable TRUE \
                 zimbraTwoFactorAuthMethodAllowed email \
                 zimbraPrefPrimaryTwoFactorAuthMethod email
```

Verify the real 2FA factory is loaded (the OSS default is a **silent no-op** that would make the
gate fail open):

```bash
zmlocalconfig -s zimbra_class_two_factor_auth_factory
grep -i "two-factor auth factory" /opt/zimbra/log/mailbox.log | tail -1
# expect: Using two-factor auth factory ZimbraTwoFactorAuth
```

The `twofactorauth` extension registers the factory itself at init, so the localconfig value is
usually irrelevant — but check the log line, not the config.

---

## 6. Test

`preauth-mfa/tfatest.sh` manages test accounts (copy it to the server and run as `ubuntu`; it
sudoes to `zimbra` itself). Set `DOMAIN` and `PREAUTH_KEY` at the top first.

```bash
./tfatest.sh create tfa1     # create + configure, un-enrolled
./tfatest.sh status tfa1     # prints which flow to expect
./tfatest.sh url    tfa1     # fresh PreAuth URL (5-minute window)
./tfatest.sh reset  tfa1     # back to un-enrolled, to re-run enrolment
./tfatest.sh code            # newest code in the recovery inbox
./tfatest.sh delete tfa1
```

### Enrolment path
1. `./tfatest.sh url tfa1`, open it in a browser → enrolment page.
2. Enter a recovery address (**must differ** from the account's own) → **Send code**.
3. Wait for the mail, enter the code → **Verify & enable** → mailbox.
4. Re-run the URL → you now get the **code challenge** instead.

### Challenge path
Any account with `zimbraTwoFactorAuthEnabled=TRUE`. Expect a redirect to `/modern/?tfa=…`
with **no** `ZM_AUTH_TOKEN` cookie, a code by email, then the mailbox.

### Skip path
On the enrolment page click **Skip for now** → mailbox, still un-enrolled, prompted again next time.

### Gotchas that will waste your time
- **The account name is inside the HMAC.** Editing `rm3` → `rm5` in an existing URL gives
  `preauth mismatch`. Always regenerate.
- **5-minute freshness window.** Stale URL → `authentication failed`.
- **Click "Send code" once.** Each send overwrites the stored code; mail can lag minutes, so a
  second click invalidates the code that is still in flight.
- Use a **fresh/incognito window** so an existing `ZM_AUTH_TOKEN` doesn't short-circuit the flow.

### Verifying from the shell

```bash
# should show the outcome per request
sudo grep "cmd=PreAuth" /opt/zimbra/log/audit.log | tail -5
#   info=two-factor auth required          -> challenge
#   info=two-factor auth enrolment required -> setup
```

---

## 7. Rollback

```bash
sudo cp /opt/zimbra/lib/jars/zimbrastore.jar.bak  /opt/zimbra/lib/jars/zimbrastore.jar
sudo cp /opt/zimbra/lib/jars/zimbracommon.jar.bak /opt/zimbra/lib/jars/zimbracommon.jar
sudo rm -f /opt/zimbra/jetty_base/webapps/zimbra/modern/tfa-enroll.html
sudo su - zimbra -c "zmmailboxdctl restart"
```

---

## 8. Known limitations

- **Only the HMAC door is gated.** `/service/preauth?isredirect=1&authtoken=…` and the SOAP PreAuth
  branch (`Auth.java`, the `preAuthEl` branch) still bypass 2FA. As a security control this is
  incomplete.
- **Enrolment is a prompt, not a gate.** By the chosen skip policy the session cookie is issued
  before enrolment, so an un-enrolled user can always reach the mailbox. Enrolled users are
  properly gated.
- **Availability-keyed.** Every un-enrolled account with `zimbraFeatureTwoFactorAuthAvailable=TRUE`
  is prompted on every PreAuth sign-in until it enrols. On a large tenant that is a lot of users at
  once.
- **Admin PreAuth is excluded** (`!admin`).
- **Browser-only.** IMAP/POP/ActiveSync are unaffected.
- **CSRF.** SOAP calls from the enrolment page are not CSRF-protected; the page authenticates with a
  scoped `ENABLE_TWO_FACTOR_AUTH` token because SOAP here rejects cookie-only auth and the session
  cookie is `HttpOnly`. Worth a look before production.
- **The enrolment page is not the Zimbra design system.** It is a standalone page, chosen because
  the SPA has at least three guards that assume "no auth token ⇒ login screen"; each needed an
  exemption, and an in-SPA form would need to survive all of them plus service-worker precaching.
