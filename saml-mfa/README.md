# ZCS-20807 — Email 2FA after SAML SSO

Enforces email two-factor auth on the SAML door, and refactors the PreAuth door (ZCS-20575) so both
share one implementation.

| Document | Read it for |
|---|---|
| **[IMPLEMENTATION.md](IMPLEMENTATION.md)** | what changed and why — design, flows, per-repo change inventory, SOAP contracts, config, build & deploy, limitations, open TODOs |
| **[TESTING.md](TESTING.md)** | how to verify it — the three states, browser walkthrough, scripted tests, test matrix, gotchas, rollback |

## Scripts

| Script | Purpose |
|---|---|
| `saml-login.py <user>` | full SP-initiated SAML login; reports the branch and whether a session was issued |
| `saml-enrol.py <token> send\|validate [code]` | password-free enrolment legs (`PreAuthTwoFactorSetupRequest`) |
| `saml-challenge.py <tfa-token> <account> send\|verify [code]` | challenge legs (`SendTwoFactorAuthCode` + `Auth`) |
| `preauth-login.py <user>` | same account through the PreAuth door — must agree with `saml-login.py` |

## Configuration

zm-mailbox is a **public** repository, so no host name, password or key is stored in these files:

```bash
cp local.env.example local.env && $EDITOR local.env
```

`local.env` is gitignored and is read by every script here and in `preauth-mfa/`. `PREAUTH_KEY`
(`zmprov gdpak <domain>`) is a credential — it mints a login for any account in the domain. Never
commit it; rotate with the same command if exposed.

## One-minute summary

An SSO door proves only that an upstream system vouched for the user — not a second factor. Both
doors now call `SsoTwoFactorGate` immediately after resolving the account and before writing any
session cookie. It returns one of three decisions:

- **NONE** — no 2FA on the account; session issued, unchanged behaviour.
- **SETUP** — 2FA available but never set up; session issued, then redirected to enrolment (skippable).
- **CHALLENGE** — enrolled; **session withheld**, redirected to a code prompt holding only a
  `TWO_FACTOR_AUTH`-scoped token.

The enrolment and challenge pages are standalone HTML, not the SPA — see IMPLEMENTATION §6 for why.
