# POC: IP-based conditional MFA bypass (ZCS-19245)

Skips the two-factor auth challenge when a user authenticates from a whitelisted
source IP range. The password is still validated; only the second factor is
skipped. Configured with the new multi-valued `zimbraMFAbyPassIP` attribute on a
COS or a domain.

Branch: `bypassMFA` (also created in `zm-admin-console`, `zm-web-client`,
`zm-build` so the same branch name checks out everywhere; only `zm-mailbox` and
`zm-admin-console` actually carry changes).

Tickets: ZCS-19245, ZRFE-1939.

## Hard prerequisites

**The Network two-factor auth extension must be installed on the test box.**
`TwoFactorAuth.getFactory()` otherwise falls back to `TwoFactorAuthUnavailable`,
where `twoFactorAuthRequired()` returns `false` for every account. The bypass
then cannot be distinguished from 2FA simply being off, and the test proves
nothing. You also need an account enrolled in TOTP.

**Build with JDK 8.** `build-common.xml` sets `javac.target=1.8`, and hundreds of
files import `javax.activation`, removed from the JDK in 11. On JDK 17 the store
module fails with ~530 errors in untouched files.

**Do not check out to a path containing a space.** `soap/build.xml:87` passes
`<arg line="--dir ${xml.schema.dir}"/>`, and Ant's `arg line` splits on
whitespace, so the schema generator receives a truncated path and dies.

**These jars are built from `develop` HEAD** (post-10.1.21). Deploy them on a
dev/SIT box tracking `develop`. Dropping them on an older install risks breakage
well beyond this feature.

## Build

Needs `zm-mailbox` plus a sibling clone of `zm-ldap-utilities` (the schema
template lives there).

    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk        # or your JDK 8
    V=-Dzimbra.buildinfo.version=10.1.22_GA_0001        # any target fails without this

    for m in common soap client store; do (cd $m && ant publish-local $V) || break; done
    (cd store && ant generate-ldap-config $V)

Artifacts:

| What | Where |
|---|---|
| jars | `{common,soap,client,store}/build/zm-*-*.jar` |
| attribute metadata | `store/conf/attrs/zimbra-attrs.xml` |
| LDAP schema | `store/build/ldap-config/zimbra.schema` |

### Regenerating the attribute getters

Only needed if you change `zimbra-attrs.xml`. `ant generate-getters` depends on
`compile`, but the code references `Provisioning.A_zimbraMFAbyPassIP`, which
generation produces — a bootstrap cycle. Move `MFABypassIP.java`,
`MFABypassIPCallback.java`, `MFABypassIPTest.java` aside and revert the
`Auth.java` hunk, run `generate-getters`, then restore them. The generated output
(`ZAttrProvisioning.java`, `ZAttrCos.java`, `ZAttrDomain.java`) is committed on
this branch, so you do not need to do this to build.

## Deploy

`deploy-poc/deploy.sh` (staged outside the repo, alongside the jars) does all of
the below with backups and a rollback recipe. Manually, as root:

1. `jars/*.jar` -> `/opt/zimbra/lib/jars/` as `zimbra{common,soap,client,store}.jar`
2. `zimbra-attrs.xml` -> `/opt/zimbra/conf/attrs/` — AttributeManager reads this at
   mailboxd startup for the new attribute's cardinality and to wire the CIDR validator
3. `zimbra.schema` -> `/opt/zimbra/common/etc/openldap/schema/`, then
   `su - zimbra -c /opt/zimbra/libexec/zmldapschema`. OpenLDAP rejects an unknown
   attribute on `zimbraCOS`/`zimbraDomain`, so `zmprov` cannot set it until this runs
4. admin console sources -> `/opt/zimbra/jetty_base/webapps/zimbraAdmin/js/zimbraAdmin/...`
   and `ZaMsg.properties` -> `.../WEB-INF/classes/messages/`. The production war
   excludes `js/zimbraAdmin/**`, so open the console as `?dev=1` to load them
5. `su - zimbra -c "zmmailboxdctl restart"`

## Configure

    zmprov mc default zimbraFeatureTwoFactorAuthRequired TRUE
    zmprov mc default zimbraMFAbyPassIP 10.0.0.0/8
    zmprov gc default zimbraMFAbyPassIP

    zmprov md example.com zimbraMFAbyPassIP 192.168.0.0/16   # domain fallback

A COS that sets any range decides on its own; the domain list is not unioned in.
Invalid CIDRs are rejected at write time by `MFABypassIPCallback`.

## Observe

Whenever the feature is configured, every login emits one line:

    tail -f /opt/zimbra/log/mailbox.log | grep 'MFA IP bypass'

It reports `originatingIP`, `peerIP`, which one was `evaluated`, the level the
range list came from, and the result. Use it to settle what the server actually
sees behind the proxy: test once directly against mailboxd on 7070, then again
through nginx on 443, and compare.

`getOrigIP()` is null unless `X-Forwarded-For` is present **and** the connecting
peer is listed in `zimbraMailTrustedIP`. Behind nginx, `peerIP` is the proxy for
every user, so `zimbraMailTrustedIP` must name the proxy or every client looks
like it came from there.

## Test matrix

| Case | Expected |
|---|---|
| 2FA required, client inside range | no challenge, login completes |
| 2FA required, client outside range | challenge as normal |
| Attribute unset anywhere | unchanged behaviour |
| COS range + domain range, non-overlapping | COS wins, domain ignored |
| Malformed CIDR alongside a valid one | valid one still matches, warning logged |
| Only a malformed CIDR | challenge issued |
| No resolvable source IP | challenge issued |
| Never-enrolled user inside range | see "open questions" below |
| Direct 7070 vs proxied 443 | answers the `X-Forwarded-For` question |
| Admin console `?dev=1` | ranges round-trip on COS and Domain; trusted-device toggle round-trips on Account |

Unit tests: `ant test -Dtest.pattern="**/util/CidrMatcherTest.java"` in `common`
(21 tests) and `-Dtest.pattern="**/twofactor/MFABypassIPTest.java"` in `store`
(25 tests).

## What changed

`zm-mailbox`

| File | |
|---|---|
| `store/conf/attrs/zimbra-attrs.xml` | new attr `zimbraMFAbyPassIP`, id 4170, `domain,cos`, multi |
| `common/src/java/com/zimbra/common/util/CidrMatcher.java` | new — IPv4/IPv6 CIDR matching |
| `store/src/java/com/zimbra/cs/account/auth/twofactor/MFABypassIP.java` | new — policy evaluation, IP resolution, logging |
| `store/src/java/com/zimbra/cs/account/callback/MFABypassIPCallback.java` | new — rejects bad CIDRs at write time |
| `store/src/java/com/zimbra/cs/service/account/Auth.java` | the gate, one term added to `usingTwoFactorAuth` |
| `ZAttrProvisioning.java`, `ZAttrCos.java`, `ZAttrDomain.java` | generated |

The gate sits beside the existing trusted-device override, which is the only
other thing that skips the challenge while still completing the login:

    boolean usingTwoFactorAuth = acct != null && twoFactorManager.twoFactorAuthRequired()
            && !trustedDeviceOverride && !MFABypassIP.isBypassed(acct, authCtxt);

`zm-admin-console`

| Form | Control |
|---|---|
| COS -> Advanced | new "Two-Factor Authentication" group: trusted-device checkbox + repeating range list |
| Domain -> Advanced | new "Two-Factor Authentication" group: repeating range list |
| Account -> Features -> General | trusted-device checkbox with "Reset to COS" |

The "trust this device" checkbox on the MFA challenge screen already exists in
`zm-web-client` (`WebRoot/public/login.jsp:643`) and needs no change — it is
gated on `zimbraFeatureTrustedDevicesEnabled`, which is why the admin toggle is
all that was missing.

## Decisions baked in, pending product confirmation

1. **COS overrides domain** rather than unioning.
2. **A bypass skips enrollment too.** Falls out of gating at `Auth.java:317`:
   `needTwoFactorAuth()` is never reached, so `TWO_FACTOR_SETUP_REQUIRED` never
   fires. Intentional for the air-gapped use case (no mobile phones on site, so
   neither the authenticator app nor an emailed code to a recovery address is
   reachable from inside). Forcing enrollment while skipping the challenge would
   be the harder implementation.
3. **Fail closed everywhere** — unresolvable IP, unparseable range, or a
   provisioning failure all issue the challenge. One bad range is skipped with a
   warning rather than voiding the whole list.
4. **A bare address means a single host.**
5. **Web/SOAP login only.** IMAP/POP/EWS/EAS never issue an MFA challenge — they
   use app-specific passwords — so there is nothing to bypass there.
6. Kept the ticket's spelling `zimbraMFAbyPassIP`, unusual `byPass` casing included.

## Discrepancies found against the ticket

- `zimbraTwoFactorAuthEnabled` is `optionalIn="account"` only, not "COS / Account"
  as the ticket's table states. A COS control for it would be rejected by the server.
- The ticket's other four "Exists today" rows are **not** in the admin console
  today — they are CLI-only, same as the trusted-device toggle was. Only the two
  controls in scope here were added.
- `ZaCosXFormView.js:334-337` references four `ZaCos.A_*` constants that are never
  defined anywhere in the repo; they evaluate to `undefined` inside
  `ADVANCED_TAB_ATTRS`. Pre-existing, left alone, worth its own ticket.
- `ZaDomain.createMethod` serializes attributes by hand, so the range cannot be set
  in the new-domain wizard. `modifyMethod` is generic, so editing an existing domain
  works. Configuring an existing domain is the use case, so this was left as is.
