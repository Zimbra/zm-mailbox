/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.SoapUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ZimbraOAuthProvider;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.qa.unittest.prov.soap.SoapDebugListener.Level;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.AuthToken;
import com.zimbra.soap.admin.message.ClearCookieRequest;
import com.zimbra.soap.admin.message.ClearCookieResponse;
import com.zimbra.soap.admin.message.NoOpRequest;
import com.zimbra.soap.admin.message.NoOpResponse;
import com.zimbra.soap.admin.type.CookieSpec;
import com.zimbra.soap.type.AccountSelector;

public class TestAuth extends SoapTest {

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }

    private String getAuthToken(String acctName, boolean isAdmin) throws Exception {
        SoapTransport transport;
        if (isAdmin) {
            transport = authAdmin(acctName);
        } else {
            transport = authUser(acctName);
        }
        return transport.getAuthToken().getValue();
    }

    /**
     * a SoapTransport that puts auth token in cookie, not SOAP header
     */
    private static class AuthTokenInCookieTransport extends SoapHttpTransport {

        private boolean isAdmin;
        private String authTokenForCookie;

        private AuthTokenInCookieTransport(String authTokenForCookie, boolean isAdmin) {
            this(authTokenForCookie, isAdmin, false, null);
        }

        private AuthTokenInCookieTransport(String authTokenForCookie, boolean isAdmin,
                boolean voidAuthTokenOnExpired, SoapDebugListener debugListener) {
            super(null);
            this.isAdmin = isAdmin;
            this.authTokenForCookie = authTokenForCookie;
            this.setVoidOnExpired(voidAuthTokenOnExpired);
            setHttpDebugListener(
                    debugListener == null ? new SoapDebugListener(Level.ALL) : debugListener);
        }

        @Override
        public Element invoke(Element document, boolean raw, boolean noSession,
                String requestedAccountId, String changeToken, String tokenType)
                throws ServiceException, IOException {
            String uri = isAdmin ? TestUtil.getAdminSoapUrl() : TestUtil.getSoapUrl();
            HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();

            ZAuthToken zAuthToken = new ZAuthToken(authTokenForCookie);
            Map<String, String> cookieMap = zAuthToken.cookieMap(isAdmin);

            HttpPost method = new HttpPost(uri + "unittest");

            try {
                Element envelope = generateSoapMessage(document, raw, noSession,
                        requestedAccountId, changeToken, tokenType);
                SoapUtil.addAuthTokenControl(SoapProtocol.Soap12.getHeader(envelope, HeaderConstants.CONTEXT), this.voidOnExpired());
                String soapMessage = SoapProtocol.toString(envelope, getPrettyPrint());
                method.setEntity(new StringEntity(soapMessage, null, "UTF-8"));

                BasicCookieStore state = null;
                if (cookieMap != null) {
                    for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                        if (state == null) {
                            state = new BasicCookieStore();
                        }
                        BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                        cookie.setDomain(method.getURI().getHost());
                        cookie.setPath("/");
                        cookie.setSecure(false);
                        state.addCookie(cookie);
                    }
                }


                clientBuilder.setDefaultCookieStore(state);

                RequestConfig reqConfig = RequestConfig.copy(
                    ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
                    .setCookieSpec(state == null ? CookiePolicy.IGNORE_COOKIES : CookiePolicy.BROWSER_COMPATIBILITY).build();

                clientBuilder.setDefaultRequestConfig(reqConfig);


                if (getHttpDebugListener() != null) {
                    getHttpDebugListener().sendSoapMessage(method, envelope, state);
                }

                HttpClient client = clientBuilder.build();
                HttpResponse response = client.execute(null, method);

                InputStreamReader reader =
                    new InputStreamReader(response.getEntity().getContent(), SoapProtocol.getCharset());
                String contentLength = response.getFirstHeader(HttpHeader.CONTENT_LENGTH.name()).getValue();
                String responseStr = ByteUtil.getContent(
                        reader,  Integer.parseInt(contentLength), false);
                Element soapResp = parseSoapResponse(responseStr, false);

                if (getHttpDebugListener() != null) {
                    getHttpDebugListener().receiveSoapMessage(method, soapResp);
                }

                return soapResp;
            } finally {
                method.releaseConnection();
            }
        }
    }

    @Test
    public void soapByCookie() throws Exception {
        boolean isAdmin = false;

        String USER_NAME = TestUtil.getAddress("user1");
        String authToken = getAuthToken(USER_NAME, isAdmin);

        Element req = Element.create(SoapProtocol.Soap12, AccountConstants.GET_INFO_REQUEST);

        SoapTransport transport = new AuthTokenInCookieTransport(authToken, isAdmin);
        Element resp = transport.invoke(req);
        Element eName = resp.getElement(AccountConstants.E_NAME);
        String value = eName.getText();
        assertEquals(USER_NAME, value);
    }

    @Test
    public void soapByCookieAdmin() throws Exception {
        boolean isAdmin = true;

        String USER_NAME = TestUtil.getAddress("admin");
        String authToken = getAuthToken(USER_NAME, isAdmin);

        Element req = Element.create(SoapProtocol.Soap12, AdminConstants.GET_CONFIG_REQUEST);
        req.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, Provisioning.A_cn);

        SoapTransport transport = new AuthTokenInCookieTransport(authToken, isAdmin);
        Element resp = transport.invoke(req);
        Element eA = resp.getElement(AdminConstants.E_A);
        String value = eA.getText();
        assertEquals("config", value);
    }

    /*
     * debug listener to verify the Expires attribute is set correctly on the response
     * auth token cookie
     */
    private static class VerifyCookieExpireListener extends SoapDebugListener {
        private String cookieToVerify;

        private VerifyCookieExpireListener(String cookieToVerify) {
            super(Level.ALL);
            this.cookieToVerify = cookieToVerify;
        }

        @Override
        public void receiveSoapMessage(HttpPost postMethod, Element envelope) {
            super.receiveSoapMessage(postMethod, envelope);

            // verify Max-Age attribute on auth token cookie is set properly
            Map<String, String> cookieAttrMap = Maps.newHashMap();

            Header[] headers = postMethod.getAllHeaders();
            for (Header header : headers) {
                System.out.println(header.toString().trim()); // trim the ending crlf

                if (header.getName().equals("Set-Cookie")) {
                    cookieAttrMap.clear();

                    // Set-Cookie: ZM_ADMIN_AUTH_TOKEN=0_2f6bd28...;Path=/;Expires=Fri, 16-Mar-2012 21:23:30 GMT;Secure;HttpOnly
                    String value = header.getValue();
                    String[] attrs = value.split(";");
                    for (String attr : attrs) {
                        String[] kv = attr.split("=");
                        if (kv.length == 2) {
                            cookieAttrMap.put(kv[0], kv[1]);
                        } else if (kv.length == 1) {
                            cookieAttrMap.put(kv[0], "is-present");
                        }
                    }

                    if (cookieAttrMap.get(cookieToVerify) != null) {
                        // found out cookie
                        break;
                    } else {
                        // not the cookie we care
                        cookieAttrMap.clear();
                    }
                }
            }

            // done parsing header, verify
            assertNotNull(cookieAttrMap.get(cookieToVerify));
            String expires = cookieAttrMap.get("Expires");
            try {
                Date date = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z").parse(expires);
                int hour = date.getHours();
                int hourNow = new Date(System.currentTimeMillis()).getHours();
                int expectedExpireHour = hourNow + 1; // authTokenLifetime is 1h
                if (expectedExpireHour >= 24) {
                    expectedExpireHour -= 24;
                }
                assertEquals(expectedExpireHour, hour);
            } catch (ParseException e) {
                fail();
            }
        }
    }

    @Test
    public void authTokenCookieMaxAge() throws Exception {
        String authTokenLifetime = "1h";  // 1 hour, has to match code in VerifyCookieExpireListener

        /*
         * test admin Auth
         */
        Account admin = provUtil.createGlobalAdmin(genAcctNameLocalPart("admin"), domain);

        // set the account's auth token lifetime to a short period
        admin.setAdminAuthTokenLifetime(authTokenLifetime);

        SoapHttpTransport transportAdmin = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        transportAdmin.setHttpDebugListener(new VerifyCookieExpireListener(ZimbraCookie.COOKIE_ZM_ADMIN_AUTH_TOKEN));

        com.zimbra.soap.admin.message.AuthRequest reqAdmin =
            new com.zimbra.soap.admin.message.AuthRequest(admin.getName(), "test123");
        reqAdmin.setPersistAuthTokenCookie(Boolean.TRUE);
        com.zimbra.soap.admin.message.AuthResponse respAdmin = invokeJaxb(transportAdmin, reqAdmin);

        /*
         * test account auth
         */
        Account acct = provUtil.createAccount(genAcctNameLocalPart("user"), domain);

        // set the account's auth token lifetime to a short period
        acct.setAuthTokenLifetime(authTokenLifetime);

        SoapHttpTransport transportAcct = new SoapHttpTransport(TestUtil.getSoapUrl());
        transportAcct.setHttpDebugListener(new VerifyCookieExpireListener(ZimbraCookie.COOKIE_ZM_AUTH_TOKEN));

        com.zimbra.soap.account.message.AuthRequest reqAcct =
            new com.zimbra.soap.account.message.AuthRequest(AccountSelector.fromName(acct.getName()), "test123");
        reqAcct.setPersistAuthTokenCookie(Boolean.TRUE);
        com.zimbra.soap.account.message.AuthResponse respAcct = invokeJaxb(transportAcct, reqAcct);

        provUtil.deleteAccount(admin);
        provUtil.deleteAccount(acct);
    }

    @Test
    public void clearCookie() throws Exception {
        int authTokenLifetimeMSecs = 2000; // 2 seconds
        int waitMSecs = authTokenLifetimeMSecs + 1000;

        Account acct = provUtil.createGlobalAdmin(genAcctNameLocalPart(), domain);

        // set the account's auth token lifetime to a short period
        acct.setAdminAuthTokenLifetime(String.valueOf(authTokenLifetimeMSecs) + "ms");

        // String authToken = getAuthToken(acct.getName(), true);
        SoapTransport transport = authAdmin(acct.getName());

        // wait till the auto token expire
        Thread.sleep(waitMSecs);

        // make sure the auth token is indeed expired
        boolean caughtAuthExpired = false;
        try {
            NoOpRequest noOpReq= new NoOpRequest();
            NoOpResponse noOpResp = invokeJaxb(transport, noOpReq);
        } catch (ServiceException e) {
            if (AccountServiceException.AUTH_EXPIRED.equals(e.getCode())) {
                caughtAuthExpired = true;
            }
        }
        assertTrue(caughtAuthExpired);

        List<CookieSpec> cookiesToClear = Lists.newArrayList(new CookieSpec(ZimbraCookie.COOKIE_ZM_ADMIN_AUTH_TOKEN));
        ClearCookieRequest req = new ClearCookieRequest(cookiesToClear);

        /*
         * test the regular path when auto token control is not set
         * (auth token in soap header)
         */
        caughtAuthExpired = false;
        try {
            invokeJaxb(transport, req);
        } catch (ServiceException e) {
            if (AccountServiceException.AUTH_EXPIRED.equals(e.getCode())) {
                caughtAuthExpired = true;
            }
        }
        assertTrue(caughtAuthExpired);

        /*
         * test the regular path when auto token control is not set
         * (auth token in cookie)
         */
        String authToken = transport.getAuthToken().getValue();
        SoapTransport authTokenInCookieTransport = new AuthTokenInCookieTransport(authToken, true);
        caughtAuthExpired = false;
        try {
            invokeJaxb(authTokenInCookieTransport, req);
        } catch (ServiceException e) {
            if (AccountServiceException.AUTH_EXPIRED.equals(e.getCode())) {
                caughtAuthExpired = true;
            }
        }
        assertTrue(caughtAuthExpired);


        /*
         * test the path when auth token control voidOnExpired is true
         */

        // debug listener to verify the cookie is cleared
        SoapDebugListener verifyCookieClearedListener = new SoapDebugListener(Level.ALL) {
            @Override
            public void receiveSoapMessage(HttpPost postMethod, Element envelope) {
                super.receiveSoapMessage(postMethod, envelope);

                // verify cookies are cleared
                Header[] headers = postMethod.getAllHeaders();
                boolean cookieCleared = false;
                for (Header header : headers) {
                    if (header.toString().trim().equals(
                            "Set-Cookie: ZM_ADMIN_AUTH_TOKEN=;Path=/;Expires=Thu, 01-Jan-1970 00:00:00 GMT")) {
                        cookieCleared = true;
                    }
                    // System.out.println(header.toString().trim()); // trim the ending crlf
                }
                assertTrue(cookieCleared);

            }
        };

        authTokenInCookieTransport = new AuthTokenInCookieTransport(authToken, true, true,
                verifyCookieClearedListener);

        // should NOT get AUTH_EXPIRED
        ClearCookieResponse resp = invokeJaxb(authTokenInCookieTransport, req);

        provUtil.deleteAccount(acct);
    }

    @Test
    public void accountStatusMaintenance() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain,
                Collections.singletonMap(
                Provisioning.A_zimbraAccountStatus, (Object)AccountStatus.maintenance.name()));

        String errorCode = null;
        try {
            SoapTransport transport = authUser(acct.getName());
        } catch (SoapFaultException e) {
            errorCode = e.getCode();
        }
        assertEquals(AccountServiceException.MAINTENANCE_MODE, errorCode);

        provUtil.deleteAccount(acct);
    }

    @Test
    public void accountStatusMaintenanceAfterAuth() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);

        SoapTransport transport = authUser(acct.getName());

        /*
         * change account status to maintenance
         */
        prov.modifyAccountStatus(acct, AccountStatus.maintenance.name());

        GetInfoRequest req = new GetInfoRequest();

        String errorCode = null;
        try {
            GetInfoResponse resp = invokeJaxb(transport, req);
        } catch (SoapFaultException e) {
            errorCode = e.getCode();
        }
        assertEquals(AccountServiceException.AUTH_EXPIRED, errorCode);

        provUtil.deleteAccount(acct);
    }

    @Test
    public void attrsReturnedInAuthResponse() throws Exception {
        String ATTR_NAME = Provisioning.A_zimbraFeatureExternalFeedbackEnabled;
        String ATTR_VALUE = ProvisioningConstants.TRUE;

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(ATTR_NAME, ATTR_VALUE);

        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain, attrs);

        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setHttpDebugListener(new SoapDebugListener());

        com.zimbra.soap.type.AccountSelector acctSel =
            new com.zimbra.soap.type.AccountSelector(com.zimbra.soap.type.AccountBy.name, acct.getName());

        AuthRequest req = new AuthRequest(acctSel, "test123");
        req.addAttr(ATTR_NAME);
        AuthResponse resp = invokeJaxb(transport, req);

        Set<String> result = Sets.newHashSet();
        for (Attr attr : resp.getAttrs()) {
            String attrName = attr.getName();
            String attrValue = attr.getValue();

            result.add(Verify.makeResultStr(attrName, attrValue));
        }
        Verify.verifyEquals(Sets.newHashSet(Verify.makeResultStr(ATTR_NAME, ATTR_VALUE)), result);

        /*
         * test the auth by auth toke npath
         */
        String authTokenStr = resp.getAuthToken();
        AuthToken authToken = new AuthToken(authTokenStr, Boolean.FALSE);
        req = new AuthRequest();
        req.setAuthToken(authToken);
        req.addAttr(ATTR_NAME);

        transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setHttpDebugListener(new SoapDebugListener());

        resp = invokeJaxb(transport, req);

        result = Sets.newHashSet();
        for (Attr attr : resp.getAttrs()) {
            String attrName = attr.getName();
            String attrValue = attr.getValue();

            result.add(Verify.makeResultStr(attrName, attrValue));
        }
        Verify.verifyEquals(Sets.newHashSet(Verify.makeResultStr(ATTR_NAME, ATTR_VALUE)), result);
    }


    @Test
    public void OAuth() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);

        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());

        Element eAuthReq = Element.create(transport.getRequestProtocol(),
                AccountConstants.AUTH_REQUEST);

        // <authtoken>
        Element eAuthToken = eAuthReq.addElement(AccountConstants.E_AUTH_TOKEN);
        eAuthToken.addAttribute(AccountConstants.A_TYPE, "oauth");

        String accessToken = "whatever";
        Element eAccessToken = eAuthToken.addElement(AccountConstants.E_A);
        eAccessToken.addAttribute(AccountConstants.A_N, ZimbraOAuthProvider.OAUTH_ACCESS_TOKEN);
        eAccessToken.setText(accessToken);

        // <account>
        Element eAcct = eAuthReq.addElement(AccountConstants.E_ACCOUNT);
        eAcct.addAttribute(AccountConstants.A_BY, AccountBy.name.name());
        eAcct.setText(acct.getName());

        Element eResp = transport.invoke(eAuthReq);

        Element eAuthTokenResp = eResp.getElement(AccountConstants.E_AUTH_TOKEN);
        String authToken = eAuthTokenResp.getText();
        assertNotNull(authToken);

    }
}


