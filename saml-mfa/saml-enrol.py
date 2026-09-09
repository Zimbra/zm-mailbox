#!/usr/bin/env python3
import re,sys,ssl,urllib.parse,urllib.request as u
ssl._create_default_https_context=ssl._create_unverified_context
import ssoenv
Z="https://"+ssoenv.need("ZIMBRA_HOST"); SOAP=Z+"/service/soap/"
RECOVERY=ssoenv.need("TEST_RECOVERY")
def esc(s): return s.replace("&","&amp;").replace('"',"&quot;").replace("<","&lt;")
def call(attrs, token):
    body='<PreAuthTwoFactorSetupRequest xmlns="urn:zimbraAccount" %s authToken="%s"/>'%(attrs, esc(token))
    env=('<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">'
         '<soap:Header><context xmlns="urn:zimbra"/></soap:Header>'
         '<soap:Body>'+body+'</soap:Body></soap:Envelope>')
    try:
        r=u.urlopen(u.Request(SOAP,env.encode(),{'Content-Type':'application/soap+xml'}),timeout=30)
        t=r.read().decode()
    except u.HTTPError as e:
        t=e.read().decode()
    m=re.search(r'<status>([^<]*)</status>',t)
    if m: return m.group(1)
    raise SystemExit("FAULT: "+ (re.search(r'<soap:Text>([^<]*)</soap:Text>',t) or ["","?"])[1])
if __name__=="__main__":
    tok=urllib.parse.unquote(sys.argv[1])
    if sys.argv[2]=="send":
        print("sendCode ->", call('action="sendCode" email="%s"' % esc(RECOVERY), tok))
    else:
        print("validateCode ->", call('action="validateCode" twoFactorCode="%s"'%esc(sys.argv[3]), tok))
