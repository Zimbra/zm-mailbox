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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.account.message.GetInfoResponse;

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
    
    private String getAutoToken(String acctName, boolean isAdmin) throws Exception {
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
            super(null);
            this.isAdmin = isAdmin;
            this.authTokenForCookie = authTokenForCookie;
            setHttpDebugListener(new SoapDebugListener());
        }
        
        @Override
        public Element invoke(Element document, boolean raw, boolean noSession,
                String requestedAccountId, String changeToken, String tokenType)
                throws ServiceException, IOException {
            String uri = isAdmin ? TestUtil.getAdminSoapUrl() : TestUtil.getSoapUrl();
            HttpClient httpClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
            
            ZAuthToken zAuthToken = new ZAuthToken(authTokenForCookie);
            Map<String, String> cookieMap = zAuthToken.cookieMap(isAdmin);
            
            PostMethod method = new PostMethod(uri + "unittest");
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
                
                if (getHttpDebugListener() != null) {
                    getHttpDebugListener().sendSoapMessage(method, soapReq);
                }
                
                int respCode = httpClient.executeMethod(null, method, state);
                
                InputStreamReader reader = 
                    new InputStreamReader(method.getResponseBodyAsStream(), SoapProtocol.getCharset());
                String responseStr = ByteUtil.getContent(
                        reader, (int) method.getResponseContentLength(), false);
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
        String authToken = getAutoToken(USER_NAME, isAdmin);
        
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
        String authToken = getAutoToken(USER_NAME, isAdmin);
        
        Element req = Element.create(SoapProtocol.Soap12, AdminConstants.GET_CONFIG_REQUEST);
        req.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, Provisioning.A_cn);
        
        SoapTransport transport = new AuthTokenInCookieTransport(authToken, isAdmin);
        Element resp = transport.invoke(req);
        Element eA = resp.getElement(AdminConstants.E_A);
        String value = eA.getText();
        assertEquals("config", value);
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
}
