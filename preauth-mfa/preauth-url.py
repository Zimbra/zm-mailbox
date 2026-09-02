#!/usr/bin/env python3
"""Generate a Zimbra preauth URL.  Usage: python3 /tmp/pa.py <account> [host]"""
import hmac, hashlib, time, sys, urllib.parse

KEY  = "80cf431190dc6bea72be7f5ba1a5c2a2763f6dbf68024b4a72f893d25e75d923"
HOST = "rakeshdev-machine1.zimbradev.com"

acct = sys.argv[1] if len(sys.argv) > 1 else "rm5@" + HOST
if "@" not in acct:
    acct = acct + "@" + HOST
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
