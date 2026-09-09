#!/usr/bin/env python3
"""Generate a Zimbra preauth URL.  Usage: python3 /tmp/pa.py <account> [host]"""
import hmac, hashlib, time, sys, os, urllib.parse

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "saml-mfa"))
import ssoenv  # noqa: E402  -- reads saml-mfa/local.env (untracked)

# The domain PreAuth key is a CREDENTIAL: it mints a login for any account in the domain, with no
# password. It is never hardcoded here -- zm-mailbox is a public repository. Get it with
# `zmprov gdpak <domain>` and put it in saml-mfa/local.env.
KEY  = ssoenv.need("PREAUTH_KEY")
HOST = ssoenv.need("ZIMBRA_HOST")
DOMAIN = ssoenv.get("ZIMBRA_DOMAIN", HOST)

acct = sys.argv[1] if len(sys.argv) > 1 else ssoenv.get("TEST_USER", "rm1")
if "@" not in acct:
    acct = acct + "@" + DOMAIN
host = sys.argv[2] if len(sys.argv) > 2 else HOST

ts = str(int(time.time() * 1000))
p = {"account": acct, "by": "name", "expires": "0", "timestamp": ts}
mac = hmac.new(KEY.encode(), "|".join(p[k] for k in sorted(p)).encode(), hashlib.sha1).hexdigest()
qs = urllib.parse.urlencode(
    {"account": acct, "by": "name", "timestamp": ts, "expires": "0", "preauth": mac})

print("https://%s/service/preauth?%s" % (host, qs))
print("", file=sys.stderr)
print("account : %s" % acct, file=sys.stderr)
print("expires : %s (5 min window)"
      % time.strftime("%H:%M:%S", time.localtime(int(ts) / 1000 + 300)), file=sys.stderr)
