/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;

public class TestAuth {
    
    private String getAutoToken(String acctName, boolean isAdmin) throws Exception {
        String uri = isAdmin ? TestUtil.getAdminSoapUrl() : TestUtil.getSoapUrl();
        SoapHttpTransport transport = new SoapHttpTransport(uri);
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).
                addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText("test123");
        
        Element response = transport.invoke(request);
        return response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
    }
    
    /**
     * a SoapTransport that put autoken in cookie, not SOAP header
     */
    private static class TestSoapTransport extends SoapTransport {

        private boolean isAdmin;
        private String authTokenForCookie;
        
        private TestSoapTransport(String authTokenForCookie, boolean isAdmin) {
            this.isAdmin = isAdmin;
            this.authTokenForCookie = authTokenForCookie;
        }
        
        @Override
        public Element invoke(Element document, boolean raw, boolean noSession,
                String requestedAccountId, String changeToken, String tokenType)
                throws ServiceException, IOException {
            String uri = isAdmin ? TestUtil.getAdminSoapUrl() : TestUtil.getSoapUrl();
            HttpClient httpClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
            
            ZAuthToken zAuthToken = new ZAuthToken(authTokenForCookie);
            Map<String, String> cookieMap = zAuthToken.cookieMap(isAdmin);
            
            PostMethod method = new PostMethod(uri + "/unittest");
            try {
                Element soapReq = generateSoapMessage(document, raw, noSession, 
                        requestedAccountId, changeToken, tokenType);
                String soapMessage = SoapProtocol.toString(soapReq, getPrettyPrint());
                method.setRequestEntity(new StringRequestEntity(soapMessage, null, "UTF-8"));
                
                HttpState state = null;
                if (cookieMap != null) {
                    for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                        if (state == null) {
                            state = new HttpState();
                        }
                        state.addCookie(new Cookie(method.getURI().getHost(), ck.getKey(), ck.getValue(), "/", null, false));
                    }
                }
                
                HttpMethodParams params = method.getParams();
                params.setCookiePolicy(state == null ? CookiePolicy.IGNORE_COOKIES : CookiePolicy.BROWSER_COMPATIBILITY);
                
                int respCode = httpClient.executeMethod(null, method, state);
                
                InputStreamReader reader = 
                    new InputStreamReader(method.getResponseBodyAsStream(), SoapProtocol.getCharset());
                String responseStr = ByteUtil.getContent(
                        reader, (int) method.getResponseContentLength(), false);
                Element soapResp = parseSoapResponse(responseStr, false);
                return soapResp;
            } finally {
                method.releaseConnection();
            }
        }
    }
    
    @BeforeClass
    public static void init() throws Exception {
        CliUtil.toolSetup();
    }
    
    @Test
    public void soapByCookie() throws Exception {
        String USER_NAME = TestUtil.getAddress("user1");
        String authToken = getAutoToken(USER_NAME, false);
        
        Element req = Element.create(SoapProtocol.Soap12, AccountConstants.GET_VERSION_INFO_REQUEST);
        
        SoapTransport transport = new TestSoapTransport(authToken, false);
        Element resp = transport.invoke(req);
        Element eInfo = resp.getElement(AccountConstants.E_VERSION_INFO_INFO);
        // <info host="phoebe.mbp" buildDate="20111112-1806" release="pshao" version="7.0.0_BETA1_1111"/>
        String host = eInfo.getAttribute(AccountConstants.A_VERSION_INFO_HOST);
        assertEquals(LC.zimbra_server_hostname.value(), host);
    }
    
    @Test
    public void soapByCookieAdmin() throws Exception {
        String USER_NAME = TestUtil.getAddress("admin");
        String authToken = getAutoToken(USER_NAME, false);
        
        Element req = Element.create(SoapProtocol.Soap12, AdminConstants.GET_VERSION_INFO_REQUEST);
        
        SoapTransport transport = new TestSoapTransport(authToken, true);
        Element resp = transport.invoke(req);
        Element eInfo = resp.getElement(AccountConstants.E_VERSION_INFO_INFO);
        // <info host="phoebe.mbp" buildDate="20111112-1806" release="pshao" version="7.0.0_BETA1_1111"/>
        String host = eInfo.getAttribute(AccountConstants.A_VERSION_INFO_HOST);
        assertEquals(LC.zimbra_server_hostname.value(), host);
        
    }


}
