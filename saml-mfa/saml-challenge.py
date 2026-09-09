#!/usr/bin/env python3
import re,sys,ssl,urllib.parse,urllib.request as u
ssl._create_default_https_context=ssl._create_unverified_context
import ssoenv
SOAP="https://"+ssoenv.need("ZIMBRA_HOST")+"/service/soap/"
def esc(s): return str(s).replace("&","&amp;").replace('"',"&quot;").replace("<","&lt;")
def soap(b):
    env=('<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">'
         '<soap:Header><context xmlns="urn:zimbra"/></soap:Header><soap:Body>'+b+'</soap:Body></soap:Envelope>')
    try:
        return u.urlopen(u.Request(SOAP,env.encode(),{'Content-Type':'application/soap+xml'}),timeout=30).read().decode()
    except u.HTTPError as e: return e.read().decode()
def fault(t):
    m=re.search(r'<soap:Text>([^<]*)</soap:Text>',t); return m.group(1) if m else None
tok,acct=sys.argv[1],sys.argv[2]
if sys.argv[3]=="send":
    t=soap('<SendTwoFactorAuthCodeRequest xmlns="urn:zimbraAccount"><action>email</action><authToken>%s</authToken></SendTwoFactorAuthCodeRequest>'%esc(tok))
    print("SendTwoFactorAuthCode ->", "sent" if re.search(r"<status>[^<]*sent", t) else (fault(t) or t[:200]))
else:
    t=soap('<AuthRequest xmlns="urn:zimbraAccount" twoFactorCode="%s"><account by="name">%s</account><authToken>%s</authToken></AuthRequest>'%(esc(sys.argv[4]),esc(acct),esc(tok)))
    print("Auth ->", "SESSION ISSUED" if ("<authToken>" in t and "soap:Fault" not in t) else (fault(t) or t[:250]))
