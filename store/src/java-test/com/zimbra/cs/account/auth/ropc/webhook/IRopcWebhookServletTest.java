/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
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

package com.zimbra.cs.account.auth.ropc.webhook;

import com.google.common.cache.Cache;
import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.ropc.util.IRopcUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "org.apache.logging.log4j.*", "javax.crypto.*", "javax.net.ssl.*"})
@PrepareForTest({IRopcWebhookServlet.class, Provisioning.class, IRopcUtil.class, URL.class})
public class IRopcWebhookServletTest {

    private static class TestableServlet extends IRopcWebhookServlet {
        private IRopcDomainConfig config;

        private boolean proxyResult;

        @Override
        protected IRopcDomainConfig getCachedDomainConfig(String domainName) {
            return config;
        }

        @Override
        protected boolean proxyToCorrectNode(String targetHost, String requestUri, String jsonPayload,
                                             String authHeader, String provider) {
            return proxyResult;
        }
    }

    @Before
    public void setUp() throws Exception {
        clearDomainConfigCache();
        PowerMockito.mockStatic(Provisioning.class);
        PowerMockito.mockStatic(IRopcUtil.class);
    }

    @Test
    public void testInitRegistersOktaHandler() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        servlet.init();

        @SuppressWarnings("unchecked")
        Map<String, IRopcWebhookHandler> handlers =
                (Map<String, IRopcWebhookHandler>) Whitebox.getInternalState(servlet, "webhookHandlers");

        assertNotNull(handlers.get("okta"));
        assertTrue(handlers.get("okta") instanceof OktaWebhookHandler);
    }

    @Test
    public void testDoGetMissingProviderReturnsBadRequest() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("provider")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoGetUnsupportedProviderReturnsBadRequest() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("provider")).thenReturn("unknown");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoGetDelegatesToProviderHandler() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);

        putHandler(servlet, "okta", handler);
        when(request.getParameter("provider")).thenReturn("OKTA");

        servlet.doGet(request, response);

        verify(handler).handleVerificationChallenge(request, response);
    }

    @Test
    public void testDoPostMissingProviderReturnsBadRequest() throws Exception {
        TestableServlet servlet = new TestableServlet();
        HttpServletRequest request = baseRequest("", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("provider")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoPostUnsupportedProviderReturnsBadRequest() throws Exception {
        TestableServlet servlet = new TestableServlet();
        HttpServletRequest request = baseRequest("unknown", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoPostPayloadTooLargeReturns413() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        putHandler(servlet, "okta", handler);

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 20 * 1024 * 1024; i++) {
            huge.append('a');
        }

        HttpServletRequest request = baseRequest("okta", huge.toString());
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    }

    @Test
    public void testDoPostInvalidUsernameReturnsBadRequest() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        putHandler(servlet, "okta", handler);

        HttpServletRequest request = baseRequest("okta", "{}\n");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("invalid-user");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoPostMissingDomainConfigReturnsUnauthorized() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        putHandler(servlet, "okta", handler);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = null;

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void testDoPostNullSecretInDomainConfigReturnsUnauthorized() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        putHandler(servlet, "okta", handler);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", null);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void testDoPostProviderMismatchReturnsBadRequest() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        putHandler(servlet, "okta", handler);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("keycloak", PasswordUtil.SSHA512.generateSSHA512("secret", null));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoPostInvalidAuthHeaderReturnsUnauthorized() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        putHandler(servlet, "okta", handler);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("wrong-secret");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void testDoPostAccountNotFoundReturnsNotFound() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        Provisioning provisioning = mock(Provisioning.class);
        putHandler(servlet, "okta", handler);

        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.AccountBy.class), anyString())).thenReturn(null);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("secret");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testDoPostLocalAccountClearsCacheAndReturnsNoContent() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        Provisioning provisioning = mock(Provisioning.class);
        Account account = mock(Account.class);
        putHandler(servlet, "okta", handler);

        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.AccountBy.class), anyString())).thenReturn(account);
        when(Provisioning.onLocalServer(account)).thenReturn(true);

        HttpServletRequest request = baseRequest("okta", "{\n\"a\":1\n}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("secret");

        servlet.doPost(request, response);

        PowerMockito.verifyStatic();
        IRopcUtil.clearCacheSession(account, "user@test.com");
        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testDoPostRemoteRoutingLoopReturns500() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        Provisioning provisioning = mock(Provisioning.class);
        Account account = mock(Account.class);
        putHandler(servlet, "okta", handler);

        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.AccountBy.class), anyString())).thenReturn(account);
        when(Provisioning.onLocalServer(account)).thenReturn(false);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("X-Zimbra-Proxied")).thenReturn("true");
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("secret");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoPostRemoteNullTargetServerReturns500() throws Exception {
        TestableServlet servlet = new TestableServlet();
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        Provisioning provisioning = mock(Provisioning.class);
        Account account = mock(Account.class);
        putHandler(servlet, "okta", handler);

        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.AccountBy.class), anyString())).thenReturn(account);
        when(Provisioning.onLocalServer(account)).thenReturn(false);
        when(account.getServer()).thenReturn(null);

        HttpServletRequest request = baseRequest("okta", "{}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("secret");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoPostRemoteProxySuccessReturnsNoContent() throws Exception {
        TestableServlet servlet = new TestableServlet();
        servlet.proxyResult = true;
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        Provisioning provisioning = mock(Provisioning.class);
        Account account = mock(Account.class);
        Server server = mock(Server.class);
        putHandler(servlet, "okta", handler);

        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.AccountBy.class), anyString())).thenReturn(account);
        when(Provisioning.onLocalServer(account)).thenReturn(false);
        when(account.getServer()).thenReturn(server);
        when(server.getServiceHostname()).thenReturn("mailbox2.example.com");

        HttpServletRequest request = baseRequest("okta", "{\"k\":\"v\"}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/service/extension/ropc/webhook");
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("secret");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testDoPostRemoteProxyFailureReturns500() throws Exception {
        TestableServlet servlet = new TestableServlet();
        servlet.proxyResult = false;
        IRopcWebhookHandler handler = mock(IRopcWebhookHandler.class);
        Provisioning provisioning = mock(Provisioning.class);
        Account account = mock(Account.class);
        Server server = mock(Server.class);
        putHandler(servlet, "okta", handler);

        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.AccountBy.class), anyString())).thenReturn(account);
        when(Provisioning.onLocalServer(account)).thenReturn(false);
        when(account.getServer()).thenReturn(server);
        when(server.getServiceHostname()).thenReturn("mailbox2.example.com");

        HttpServletRequest request = baseRequest("okta", "{\"k\":\"v\"}");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/service/extension/ropc/webhook");
        when(handler.extractUsername(anyString())).thenReturn("user@test.com");
        servlet.config = new IRopcDomainConfig("okta", PasswordUtil.SSHA512.generateSSHA512("secret", null));
        when(request.getHeader(anyString())).thenReturn("secret");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoPostUnexpectedExceptionReturns500() throws Exception {
        TestableServlet servlet = new TestableServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("provider")).thenThrow(new RuntimeException("boom"));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testParseAuthMechArgsHappyPathWithQuotes() {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        String authMech = "idp \"provider=okta\" \"webhook_secret=superSecret\"";

        assertEquals("okta", servlet.parseAuthMechArgs(authMech, "provider"));
        assertEquals("superSecret", servlet.parseAuthMechArgs(authMech, "webhook_secret"));
    }

    @Test
    public void testParseAuthMechArgsReturnsNullForNullInputsAndMissingKey() {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();

        assertNull(servlet.parseAuthMechArgs(null, "provider"));
        assertNull(servlet.parseAuthMechArgs("provider=okta", null));
        assertNull(servlet.parseAuthMechArgs("provider=okta", "webhook_secret"));
    }

    @Test
    public void testGetCachedDomainConfigCacheHitSkipsLdap() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        IRopcDomainConfig cached = new IRopcDomainConfig("okta", "cached");

        @SuppressWarnings("unchecked")
        Cache<String, IRopcDomainConfig> cache =
                (Cache<String, IRopcDomainConfig>) Whitebox.getInternalState(IRopcWebhookServlet.class,
                        "DOMAIN_SECRET_CACHE");
        cache.put("example.com", cached);

        IRopcDomainConfig result = servlet.getCachedDomainConfig("example.com");

        assertSame(cached, result);
    }

    @Test
    public void testGetCachedDomainConfigFromLdapAndCache() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        Provisioning provisioning = mock(Provisioning.class);
        Domain domain = mock(Domain.class);

        PowerMockito.mockStatic(Provisioning.class);
        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.DomainBy.class), anyString())).thenReturn(domain);
        when(domain.getAuthMech()).thenReturn("idp \"provider=okta\" \"webhook_secret=plainSecret\"");

        IRopcDomainConfig result = servlet.getCachedDomainConfig("example.com");

        assertNotNull(result);
        assertEquals("okta", result.getProvider());
        assertTrue(PasswordUtil.SSHA512.verifySSHA512(result.getAuthSecret(), "plainSecret"));

        IRopcDomainConfig cached = servlet.getCachedDomainConfig("example.com");
        assertSame(result.getProvider(), cached.getProvider());
    }

    @Test
    public void testGetCachedDomainConfigDomainMissingReturnsNull() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        Provisioning provisioning = mock(Provisioning.class);

        PowerMockito.mockStatic(Provisioning.class);
        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.DomainBy.class), anyString())).thenReturn(null);

        assertNull(servlet.getCachedDomainConfig("example.com"));
    }

    @Test
    public void testGetCachedDomainConfigMissingProviderOrSecretReturnsNull() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        Provisioning provisioning = mock(Provisioning.class);
        Domain domain = mock(Domain.class);

        PowerMockito.mockStatic(Provisioning.class);
        when(Provisioning.getInstance()).thenReturn(provisioning);
        when(provisioning.get(any(Key.DomainBy.class), anyString())).thenReturn(domain);
        when(domain.getAuthMech()).thenReturn("idp \"provider=okta\"");

        assertNull(servlet.getCachedDomainConfig("example.com"));
    }

    @Test
    public void testGetCachedDomainConfigExceptionReturnsNull() {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();

        PowerMockito.mockStatic(Provisioning.class);
        when(Provisioning.getInstance()).thenThrow(new RuntimeException("ldap down"));

        assertNull(servlet.getCachedDomainConfig("example.com"));
    }

    @Test
    public void testProxyToCorrectNodeReturnsTrueFor204AndDisconnects() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        URL mockUrl = mock(URL.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);

        PowerMockito.whenNew(URL.class)
                .withArguments("https://target.example.com/service/extension/ropc/webhook?provider=okta")
                .thenReturn(mockUrl);
        when(mockUrl.openConnection()).thenReturn(conn);
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(conn.getResponseCode()).thenReturn(204);

        boolean result = servlet.proxyToCorrectNode("target.example.com", "/service/extension/ropc/webhook",
                "{\"a\":1}", "header-secret", "okta");

        assertTrue(result);
        verify(conn).disconnect();
    }

    @Test
    public void testProxyToCorrectNodeReturnsTrueFor200() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        URL mockUrl = mock(URL.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);

        PowerMockito.whenNew(URL.class)
                .withArguments("https://target.example.com/service/extension/ropc/webhook?provider=okta")
                .thenReturn(mockUrl);
        when(mockUrl.openConnection()).thenReturn(conn);
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(conn.getResponseCode()).thenReturn(200);

        boolean result = servlet.proxyToCorrectNode("target.example.com", "/service/extension/ropc/webhook",
                "{\"a\":1}", "header-secret", "okta");

        assertTrue(result);
    }

    @Test
    public void testProxyToCorrectNodeReturnsFalseForNonSuccessCode() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();
        URL mockUrl = mock(URL.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);

        PowerMockito.whenNew(URL.class)
                .withArguments("https://target.example.com/service/extension/ropc/webhook?provider=okta")
                .thenReturn(mockUrl);
        when(mockUrl.openConnection()).thenReturn(conn);
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(conn.getResponseCode()).thenReturn(500);

        boolean result = servlet.proxyToCorrectNode("target.example.com", "/service/extension/ropc/webhook",
                "{\"a\":1}", "header-secret", "okta");

        assertFalse(result);
    }

    @Test
    public void testProxyToCorrectNodeExceptionReturnsFalse() throws Exception {
        IRopcWebhookServlet servlet = new IRopcWebhookServlet();

        PowerMockito.whenNew(URL.class)
                .withArguments("https://target.example.com/service/extension/ropc/webhook?provider=okta")
                .thenThrow(new RuntimeException("connect fail"));

        boolean result = servlet.proxyToCorrectNode("target.example.com", "/service/extension/ropc/webhook",
                "{\"a\":1}", "header-secret", "okta");

        assertFalse(result);
    }

    private HttpServletRequest baseRequest(String provider, String payload) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("provider")).thenReturn(provider);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(payload)));
        when(request.getRequestURI()).thenReturn("/service/extension/ropc/webhook");
        return request;
    }

    private void putHandler(IRopcWebhookServlet servlet, String provider, IRopcWebhookHandler handler) {
        @SuppressWarnings("unchecked")
        Map<String, IRopcWebhookHandler> handlers =
                (Map<String, IRopcWebhookHandler>) Whitebox.getInternalState(servlet, "webhookHandlers");
        handlers.put(provider, handler);
    }

    private void clearDomainConfigCache() throws Exception {
        @SuppressWarnings("unchecked")
        Cache<String, IRopcDomainConfig> cache =
                (Cache<String, IRopcDomainConfig>) Whitebox.getInternalState(IRopcWebhookServlet.class,
                        "DOMAIN_SECRET_CACHE");
        cache.invalidateAll();
    }
}


