#!/usr/bin/env python3
import hmac,hashlib,time,sys,urllib.parse,urllib.request,ssl,http.cookiejar as cj
ssl._create_default_https_context=ssl._create_unverified_context
import ssoenv
KEY=ssoenv.need("PREAUTH_KEY")            # zmprov gdpak <domain> -- credential, never commit
HOST=ssoenv.need("ZIMBRA_HOST")
DOMAIN=ssoenv.get("ZIMBRA_DOMAIN", HOST)
acct=sys.argv[1] if "@" in sys.argv[1] else sys.argv[1]+"@"+DOMAIN
ts=str(int(time.time()*1000))
p={"account":acct,"by":"name","expires":"0","timestamp":ts}
mac=hmac.new(KEY.encode(),"|".join(p[k] for k in sorted(p)).encode(),hashlib.sha1).hexdigest()
url="https://%s/service/preauth?%s"%(HOST,urllib.parse.urlencode(
    {"account":acct,"by":"name","timestamp":ts,"expires":"0","preauth":mac}))
jar=cj.CookieJar(); op=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
r=op.open(url,timeout=30); final=r.geturl()
tok=any(c.name=="ZM_AUTH_TOKEN" for c in jar)
b = "SETUP" if "tfa-enroll" in final else ("CHALLENGE" if "tfa=" in final else "NONE")
print("  %-4s -> %-9s | session cookie: %s" % (sys.argv[1], b, "yes" if tok else "no (withheld)"))
