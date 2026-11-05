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

package com.zimbra.cs.service.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.objenesis.ObjenesisStd;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.SoapServlet;

public class OAuthUtilTest {

    @Test
    public void detectsOAuthFromAuthTypeHeaderCaseInsensitively() {
        assertTrue(OAuthUtil.isOAuthRequested(
                authRequest(),
                contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, "zm_oauth2_1.0"),
                enabledAccount()));
    }
    @Test
    public void detectsOAuthFromBooleanOAuthHeader() {
        assertTrue(OAuthUtil.isOAuthRequested(
                authRequest(),
                contextWithHeader(OAuthConstants.AUTH_TYPE_ZM_OAUTH2, "true"),
                enabledAccount()));
    }
    @Test
    public void doesNotDetectOAuthWithoutAnOAuthSignal() {
        assertFalse(OAuthUtil.isOAuthRequested(
                authRequest(),
                new HashMap<String, Object>(),
                enabledAccount()));
    }
    @Test
    public void disabledPersistentSessionsDoNotEnableOAuthEvenWithHeader() {
        TestAccount account = enabledAccount();
        account.setPersistentSessionEnabled(false);
        assertFalse(OAuthUtil.isOAuthRequested(
                authRequest(),
                contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, OAuthConstants.AUTH_TYPE_ZM_OAUTH2),
                account));
    }
    @Test
    public void nonPositiveRefreshLifetimeDoesNotEnableOAuthEvenWithHeader() {
        TestAccount account = enabledAccount();
        account.setRefreshTokenLifetime(0L);
        assertFalse(OAuthUtil.isOAuthRequested(
                authRequest(),
                contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, OAuthConstants.AUTH_TYPE_ZM_OAUTH2),
                account));
    }
    @Test
    public void negativeRefreshLifetimeDoesNotEnableOAuthEvenWithHeader() {
        TestAccount account = enabledAccount();
        account.setRefreshTokenLifetime(-1L);
        assertFalse(OAuthUtil.isOAuthRequested(
                authRequest(),
                contextWithHeader(OAuthConstants.HEADER_AUTH_TYPE, OAuthConstants.AUTH_TYPE_ZM_OAUTH2),
                account));
    }
    @Test
    public void nullAccountIsNotAnOAuthRequest() {
        assertFalse(OAuthUtil.isOAuthRequested(
                authRequest(),
                new HashMap<String, Object>(),
                null));
    }
    @Test
    public void noServletRequestInContextIsNotAnOAuthRequest() {
        assertFalse(OAuthUtil.isOAuthRequested(
                authRequest(),
                new HashMap<String, Object>(),
                enabledAccount()));
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
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getHeader".equals(method.getName()) && args != null && args.length > 0) {
                            return name.equalsIgnoreCase((String) args[0]) ? value : null;
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
            this.persistentSessionEnabled = enabled;
        }
        private void setRefreshTokenLifetime(long lifetime) {
            this.refreshTokenLifetime = lifetime;
        }
    }

}
