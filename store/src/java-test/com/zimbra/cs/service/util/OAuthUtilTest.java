/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.util;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.soap.SoapServlet;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OAuthUtilTest {

    private HttpServletRequest mockHttpRequest;

    private Map<String, Object> context;

    @Before
    public void setUp() {
        mockHttpRequest = Mockito.mock(HttpServletRequest.class);
        context = new HashMap<>();
        context.put(SoapServlet.SERVLET_REQUEST, mockHttpRequest);
    }

    @Test
    public void testIsOAuthRequestedViaAuthTypeHeaderReturnsTrue() {
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE))
                .thenReturn(OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
        boolean result = OAuthUtil.isOAuthRequested(null, context);
        Assert.assertTrue(
                "Should detect OAuth when Auth-Type header is ZM_OAUTH2_1.0",
                result);
    }

    @Test
    public void testIsOAuthRequestedViaAuthTypeHeaderCaseInsensitiveReturnsTrue() {
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE))
                .thenReturn("zm_oauth2_1.0");
        boolean result = OAuthUtil.isOAuthRequested(null, context);
        Assert.assertTrue(
                "Should match Auth-Type header case-insensitively",
                result);
    }

    @Test
    public void testIsOAuthRequestedViaLegacyBooleanHeaderReturnsTrue() {
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE))
                .thenReturn(null);
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.AUTH_TYPE_ZM_OAUTH2))
                .thenReturn("true");
        boolean result = OAuthUtil.isOAuthRequested(null, context);
        Assert.assertTrue(
                "Should detect OAuth when the legacy OAuth header is true",
                result);
    }

    @Test
    public void testIsOAuthRequestedViaXmlAttributeReturnsTrue() {
        Element requestElement = new Element.XMLElement("EndSessionRequest");
        requestElement.addAttribute(
                OAuthConstants.HEADER_AUTH_TYPE,
                OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
        boolean result = OAuthUtil.isOAuthRequested(requestElement, context);
        Assert.assertTrue(
                "Should detect OAuth when the OAuth XML attribute is ZM_OAUTH2_1.0",
                result);
    }

    @Test
    public void testIsOAuthRequestedViaXmlAttributeCaseInsensitiveReturnsTrue() {
        Element requestElement = new Element.XMLElement("EndSessionRequest");
        requestElement.addAttribute(
                OAuthConstants.HEADER_AUTH_TYPE,
                "zm_oauth2_1.0");
        boolean result = OAuthUtil.isOAuthRequested(requestElement, null);
        Assert.assertTrue(
                "Should match the OAuth XML attribute case-insensitively with null context",
                result);
    }

    @Test
    public void testIsOAuthRequestedNoOAuthIndicatorsReturnsFalse() {
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE))
                .thenReturn("BASIC");
        Mockito.when(mockHttpRequest.getHeader(OAuthConstants.AUTH_TYPE_ZM_OAUTH2))
                .thenReturn("false");
        Element requestElement = new Element.XMLElement("EndSessionRequest");
        requestElement.addAttribute(
                OAuthConstants.HEADER_AUTH_TYPE,
                "PASSWORD");
        boolean result = OAuthUtil.isOAuthRequested(requestElement, context);
        Assert.assertFalse(
                "Should return false when no OAuth indicator is present",
                result);
    }

    @Test
    public void testIsOAuthRequestedNullContextWithValidXmlAttributeReturnsTrue() {
        Element requestElement = new Element.XMLElement("EndSessionRequest");
        requestElement.addAttribute(
                OAuthConstants.HEADER_AUTH_TYPE,
                OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
        boolean result = OAuthUtil.isOAuthRequested(requestElement, null);
        Assert.assertTrue(
                "Should detect the OAuth XML attribute even when context is null",
                result);
    }

    @Test
    public void testIsOAuthRequestedNullRequestNullContextReturnsFalse() {
        boolean result = OAuthUtil.isOAuthRequested(null, null);
        Assert.assertFalse(
                "Should return false when request and context are both null",
                result);
    }

    @Test
    public void testIsOAuthRequestedContextWithoutServletRequestFallbackToElement() {
        Map<String, Object> emptyContext = new HashMap<>();
        Element requestElement = new Element.XMLElement("EndSessionRequest");
        requestElement.addAttribute(
                OAuthConstants.HEADER_AUTH_TYPE,
                OAuthConstants.AUTH_TYPE_ZM_OAUTH2);
        boolean result = OAuthUtil.isOAuthRequested(requestElement, emptyContext);
        Assert.assertTrue(
                "Should fall back to the XML attribute when the servlet request is missing",
                result);
    }
}
