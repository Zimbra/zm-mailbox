/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.soap.SoapServlet;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class EndSessionOAuthTest {

    private EndSession endSession;

    private OAuthProvider mockProvider;

    private HttpServletRequest mockHttpRequest;

    private Map<String, Object> context;

    private Element requestElement;

    @Before
    public void setUp() {
        endSession = new EndSession();
        mockProvider = Mockito.mock(OAuthProvider.class);
        mockHttpRequest = Mockito.mock(HttpServletRequest.class);
        requestElement = new Element.XMLElement(AccountConstants.END_SESSION_REQUEST);
        context = new HashMap<String, Object>();
        context.put(SoapServlet.SERVLET_REQUEST, mockHttpRequest);
    }

    @After
    public void tearDown() {
        OAuthRegistry.registerProvider(null);
    }

    @Test
    public void testHandleOAuthRequestedProviderNullThrowsInvalidRequest() {
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE))
                .thenReturn(OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
        OAuthRegistry.registerProvider(null);
        try {
            endSession.handle(requestElement, context);
            Assert.fail("Expected ServiceException when OAuthProvider is null");
        } catch (ServiceException e) {
            Assert.assertEquals("Missing OAuthProvider should produce INVALID_REQUEST",
                    ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void testHandleOAuthRequestedProviderRuntimeExceptionPropagates() throws Exception {
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE))
                .thenReturn(OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
        OAuthRegistry.registerProvider(mockProvider);
        RuntimeException expected = new RuntimeException("Simulated cred_store DB failure");
        Mockito.doThrow(expected).when(mockProvider).handleLogout(requestElement);
        try {
            endSession.handle(requestElement, context);
            Assert.fail("Expected the provider exception to propagate");
        } catch (RuntimeException actual) {
            Assert.assertSame("Provider exception should propagate without wrapping", expected, actual);
        }
        Mockito.verify(mockProvider).handleLogout(requestElement);
    }
}
