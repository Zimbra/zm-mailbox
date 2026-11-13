/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.soap.SoapServlet;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.objenesis.ObjenesisStd;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OAuthRequestDetectionTest {

    private final Auth auth = new Auth();

    @Test
    public void detectsOAuthFromAuthTypeHeaderCaseInsensitively() {
        assertTrue(auth.isOAuthRequested(authRequest(),
                contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, "zm_oauth2_1.0"), enabledAccount()));
    }

    @Test
    public void detectsOAuthFromBooleanOAuthHeader() {
        assertTrue(auth.isOAuthRequested(authRequest(),
                contextWithHeader(OAuthConstants.AUTH_TYPE_ZM_OAUTH2, "true"), enabledAccount()));
    }

    @Test
    public void detectsOAuthFromAuthTypeSoapAttribute() {
        Element request = authRequest();
        request.addAttribute("authType", OAuthConstants.AUTH_TYPE_ZM_OAUTH2);

        assertTrue(auth.isOAuthRequested(request, new HashMap<String, Object>(), enabledAccount()));
    }

    @Test
    public void doesNotDetectOAuthWithoutAnOAuthSignal() {
        assertFalse(auth.isOAuthRequested(
                authRequest(), new HashMap<String, Object>(), enabledAccount()));
    }

    @Test
    public void disabledPersistentSessionsDoNotEnableOAuthEvenWithHeader() {
        TestAccount account = enabledAccount();
        account.setPersistentSessionEnabled(false);

        assertFalse(auth.isOAuthRequested(authRequest(),
                contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, OAuthConstants.AUTH_TYPE_ZM_OAUTH2), account));
    }

    @Test
    public void nonPositiveRefreshLifetimeDoesNotEnableOAuthEvenWithAttribute() {
        TestAccount account = enabledAccount();
        account.setRefreshTokenLifetime(0L);
        Element request = authRequest();
        request.addAttribute("authType", OAuthConstants.AUTH_TYPE_ZM_OAUTH2);

        assertFalse(auth.isOAuthRequested(request, new HashMap<String, Object>(), account));
    }

    @Test
    public void nullAccountIsNotAnOAuthRequest() {
        assertFalse(auth.isOAuthRequested(authRequest(), new HashMap<String, Object>(), null));
    }

    @Test
    public void refreshWithoutAccountRoutesToProviderForEachOAuthSignal() throws Exception {
        final Element response = new XMLElement(AccountConstants.AUTH_RESPONSE);
        final Element[] capturedRequest = new Element[1];
        OAuthProvider previous = OAuthRegistry.getProvider();
        OAuthRegistry.registerProvider(new OAuthProvider() {
            @Override
            public AuthToken issueToken(Account account, Usage usage, Element req, Element resp,
                    Map<String, Object> context) {
                throw new AssertionError("Unexpected token issuance");
            }

            @Override
            public Element handleRefresh(Element req) {
                capturedRequest[0] = req;
                return response;
            }
        });
        try {
            Element request = authRequest();
            request.addElement("refreshToken").setText("test-token");
            assertSame(response, auth.handle(request,
                    contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, OAuthConstants.AUTH_TYPE_ZM_OAUTH2)));
            assertSame(request, capturedRequest[0]);
            assertSame(response, auth.handle(request, contextWithHeader(OAuthConstants.AUTH_TYPE_ZM_OAUTH2, "true")));
            assertSame(request, capturedRequest[0]);
            request.addAttribute("authType", OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
            assertSame(response, auth.handle(request, new HashMap<String, Object>()));
            assertSame(request, capturedRequest[0]);
        } finally {
            OAuthRegistry.registerProvider(previous);
        }
    }

    @Test
    public void refreshWithoutProviderFailsExplicitly() throws Exception {
        OAuthProvider previous = OAuthRegistry.getProvider();
        OAuthRegistry.registerProvider(null);
        try {
            Element request = authRequest();
            request.addAttribute("authType", OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
            request.addElement("refreshToken").setText("test-token");
            try {
                auth.handle(request, new HashMap<String, Object>());
                fail("Expected failure when OAuth provider is unavailable");
            } catch (ServiceException e) {
                assertEquals(ServiceException.FAILURE, e.getCode());
            }
        } finally {
            OAuthRegistry.registerProvider(previous);
        }
    }

    private static TestAccount enabledAccount() {
        TestAccount account = new ObjenesisStd().newInstance(TestAccount.class);
        account.setPersistentSessionEnabled(true);
        account.setRefreshTokenLifetime(3600000L);
        return account;
    }

    private static Element authRequest() {
        return new XMLElement(AccountConstants.AUTH_REQUEST);
    }

    private static Map<String, Object> contextWithHeader(final String name, final String value) {
        Map<String, Object> context = new HashMap<String, Object>();
        HttpServletRequest servletRequest = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getHeader".equals(method.getName())) {
                            return name.equals(args[0]) ? value : null;
                        }
                        return null;
                    }
                });
        context.put(SoapServlet.SERVLET_REQUEST, servletRequest);
        return context;
    }

    private static final class TestAccount extends Account {

        private boolean persistentSessionEnabled;

        private long refreshTokenLifetime;

        private TestAccount() {
            super(null, null, new HashMap<String, Object>(), null, null);
        }

        @Override
        public boolean isNativeOAuthPersistentSessionEnabled() {
            return persistentSessionEnabled;
        }

        @Override
        public long getNativeOAuthRefreshTokenLifetime() {
            return refreshTokenLifetime;
        }

        private void setPersistentSessionEnabled(boolean enabled) {
            persistentSessionEnabled = enabled;
        }

        private void setRefreshTokenLifetime(long lifetime) {
            refreshTokenLifetime = lifetime;
        }
    }
}
