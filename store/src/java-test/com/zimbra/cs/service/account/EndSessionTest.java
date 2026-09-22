/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.service.account;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.EndSessionRequest;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EndSessionTest {

    private static final MockProvisioning PROV = new MockProvisioning();

    private Account account;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.setInstance(PROV);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        final Map<String, Object> attrs = new HashMap<>(1);
        attrs.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        account = PROV.createAccount("logout-test@zcs.fazigu.org", "password", attrs);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        if (account != null) {
            account.deleteAccount();
        }
    }

    @Test
    public void testHandleClearsCookiesAndDeregistersCurrentSessionToken() throws Exception {
        Map<String, Object> context = ServiceTestUtil.getRequestContext(account);
        RecordingAuthToken authToken = new RecordingAuthToken(account.getId());
        ZimbraSoapContext baseZsc = new ZimbraSoapContext(authToken, account.getId(), SoapProtocol.Soap12,
                SoapProtocol.Soap12);

        SoapSession session = new SoapSession(baseZsc).register();
        ZimbraSoapContext zsc = new ZimbraSoapContext(baseZsc, authToken, baseZsc.getRequestedAccountId(), session);
        context.put(SoapEngine.ZIMBRA_CONTEXT, zsc);
        context.put(SoapEngine.ZIMBRA_SESSION, session);

        Assert.assertNotNull("session should be registered before logout",
                SessionCache.lookup(session.getSessionId(), authToken.getAccountId()));
        Assert.assertTrue("auth token should start in registered state", authToken.isRegistered());

        EndSession handler = new EndSession();
        Element request = JaxbUtil.jaxbToElement(new EndSessionRequest());
        Element response = handler.handle(request, context);

        Assert.assertNotNull("response", response);
        Assert.assertEquals(AccountConstants.END_SESSION_RESPONSE.getName(), response.getName());
        Assert.assertEquals(Boolean.TRUE, context.get(SoapServlet.INVALIDATE_COOKIES));
        Assert.assertNull("session should be removed from cache after logout",
                SessionCache.lookup(session.getSessionId(), authToken.getAccountId()));
        Assert.assertTrue("auth token should be encoded for cookie invalidation", authToken.wasEncodedForDeregister());
        Assert.assertTrue("auth token should be deregistered after logout", authToken.wasDeregistered());
        Assert.assertFalse("auth token should report unregistered after logout", authToken.isRegistered());
    }

    @Test
    public void testHandleWithoutCurrentSessionDoesNotInvalidateCookiesByDefault() throws Exception {
        Map<String, Object> context = ServiceTestUtil.getRequestContext(account);
        RecordingAuthToken authToken = new RecordingAuthToken(account.getId());
        ZimbraSoapContext zsc = new ZimbraSoapContext(authToken, account.getId(), SoapProtocol.Soap12,
                SoapProtocol.Soap12);
        context.put(SoapEngine.ZIMBRA_CONTEXT, zsc);

        EndSession handler = new EndSession();
        Element request = JaxbUtil.jaxbToElement(new EndSessionRequest());
        Element response = handler.handle(request, context);

        Assert.assertNotNull("response", response);
        Assert.assertEquals(AccountConstants.END_SESSION_RESPONSE.getName(), response.getName());
        Assert.assertNull("cookies should not be invalidated when there is no current session",
                context.get(SoapServlet.INVALIDATE_COOKIES));
        Assert.assertFalse("auth token should not be encoded when there is no current session",
                authToken.wasEncodedForDeregister());
        Assert.assertFalse("auth token should not be deregistered when there is no current session",
                authToken.wasDeregistered());
        Assert.assertTrue("auth token should still be registered when there is no current session",
                authToken.isRegistered());
    }

    private static final class RecordingAuthToken extends AuthToken {

        private final String accountId;

        private boolean deregistered;

        private boolean encodedForDeregister;

        private RecordingAuthToken(String accountId) {
            this.accountId = accountId;
        }

        @Override
        public String toString() {
            return "RecordingAuthToken{" + accountId + '}';
        }

        @Override
        public String getAccountId() {
            return accountId;
        }

        @Override
        public String getAdminAccountId() {
            return null;
        }

        @Override
        public long getExpires() {
            return System.currentTimeMillis() + 60000;
        }

        @Override
        public void deRegister() {
            deregistered = true;
        }

        @Override
        public boolean isRegistered() {
            return !deregistered;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isAdmin() {
            return false;
        }

        @Override
        public boolean isDomainAdmin() {
            return false;
        }

        @Override
        public boolean isDelegatedAdmin() {
            return false;
        }

        @Override
        public boolean isZimbraUser() {
            return true;
        }

        @Override
        public String getExternalUserEmail() {
            return null;
        }

        @Override
        public String getDigest() {
            return null;
        }

        @Override
        public String getCrumb() {
            return "crumb";
        }

        @Override
        public void encode(HttpClient client, HttpRequestBase method, boolean isAdminReq, String cookieDomain) {
        }

        @Override
        public void encode(BasicCookieStore state, boolean isAdminReq, String cookieDomain) {
        }

        @Override
        public void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie, boolean rememberMe) {
        }

        @Override
        public void encode(HttpServletRequest reqst, HttpServletResponse resp, boolean deregister) {
            encodedForDeregister = deregister;
        }

        @Override
        public void encode(HttpClientBuilder clientBuilder, HttpRequestBase method, boolean isAdminReq,
                String cookieDomain) {
        }

        @Override
        public void encodeAuthResp(Element parent, boolean isAdmin) {
        }

        @Override
        public ZAuthToken toZAuthToken() {
            return new ZAuthToken("recording-token");
        }

        @Override
        public String getEncoded() {
            return "recording-token";
        }

        @Override
        public Usage getUsage() {
            return Usage.AUTH;
        }

        private boolean wasDeregistered() {
            return deregistered;
        }

        private boolean wasEncodedForDeregister() {
            return encodedForDeregister;
        }
    }
}

