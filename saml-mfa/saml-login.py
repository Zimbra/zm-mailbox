#!/usr/bin/env python3
import urllib.request as u, urllib.parse as up, http.cookiejar as cj, re, sys, gzip, io
import ssoenv
HOST=ssoenv.need("ZIMBRA_HOST"); DOMAIN=ssoenv.get("ZIMBRA_DOMAIN", HOST)
PW=ssoenv.need("IDP_TEST_PASSWORD")
_u=sys.argv[1] if len(sys.argv)>1 else ssoenv.get("TEST_USER","rm1")
USER=_u if "@" in _u else _u+"@"+DOMAIN
Z="https://"+HOST
jar=cj.CookieJar()
op=u.build_opener(u.HTTPCookieProcessor(jar), u.HTTPRedirectHandler())
op.addheaders=[('User-Agent','Mozilla/5.0'),('Accept','text/html')]
import ssl; ssl._create_default_https_context=ssl._create_unverified_context

def get(url,data=None):
    r=op.open(url,data.encode() if data else None,timeout=30)
    b=r.read()
    if r.headers.get('Content-Encoding')=='gzip': b=gzip.decompress(b)
    return r.geturl(), b.decode('utf-8','replace')

# 1. SP-initiated login -> should land on Keycloak login form
url,html=get(Z+"/service/extension/samllogin")
print("1. after samllogin ->", url[:90])
assert "keycloak" in url, "did not reach IdP"

# 2. post credentials to the form action
m=re.search(r'action="([^"]+)"',html)
action=m.group(1).replace("&amp;","&")
print("2. form action ->", action[:90])
url,html=get(action, up.urlencode({"username":USER,"password":PW}))
print("3. after credentials ->", url[:90])

# 3. auto-submit SAMLResponse form back to Zimbra ACS
m=re.search(r'name="SAMLResponse" value="([^"]+)"',html)
if not m:
    print("   NO SAMLResponse. body snippet:"); print(html[:900]); sys.exit(1)
import base64
xml=base64.b64decode(m.group(1)).decode('utf-8','replace')
print("4. got SAMLResponse; NameID =", (re.search(r'<saml:NameID[^>]*>([^<]+)<',xml) or ["","?"])[1])
print("   Audience =", (re.search(r'<saml:Audience>([^<]+)<',xml) or ["","?"])[1])
# Namespace prefix varies by IdP (Keycloak emits dsig:), and Signature is not the first child --
# Issuer precedes it -- so classify by position relative to the Assertion element instead.
# Zimbra requires an Assertion signature only when the Response itself is unsigned, so report both.
_a = re.search(r"<(\w+:)?Assertion\b", xml)
_ae = re.search(r"</(\w+:)?Assertion>", xml)
_lo, _hi = (_a.start(), _ae.end()) if (_a and _ae) else (-1, -1)
_sigs = [m.start() for m in re.finditer(r"<(\w+:)?Signature\b", xml)]
_in_assertion = any(_lo <= i <= _hi for i in _sigs)
_at_response  = any(not (_lo <= i <= _hi) for i in _sigs)
print("   signed   = Response:%s Assertion:%s | AuthnStatement = %s"
      % (_at_response, _in_assertion, "AuthnStatement" in xml))
act=re.search(r'<form[^>]+action="([^"]+)"',html).group(1).replace("&amp;","&")
rs=re.search(r'name="RelayState" value="([^"]*)"',html)
data={"SAMLResponse":m.group(1)}
if rs: data["RelayState"]=rs.group(1)
url,html=get(act, up.urlencode(data))
print("5. after ACS post ->", url)
print("6. cookies:", sorted(c.name for c in jar))
tok=any(c.name=="ZM_AUTH_TOKEN" for c in jar)
sid=any(c.name=="ZM_AUTH_SAML_DETAILS" for c in jar)
if "tfa-enroll.html" in url:   branch="SETUP     -> enrolment page"; ok=tok and sid
elif "tfa=" in url:            branch="CHALLENGE -> code prompt";    ok=(not tok) and sid
else:                          branch="NONE      -> mailbox";        ok=tok and sid
print("\nBRANCH:", branch)
print("  session cookie:", "yes" if tok else "no (withheld)")
print("  SessionIndex  :", "yes" if sid else "MISSING - logout will 401")
print("  RESULT:", "PASS" if ok else "FAIL")
