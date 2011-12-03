/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.soap;

import java.io.IOException;

import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.BeforeClass;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;

public class SoapTest {
    private static final String TEST_SOAP_BASE_DOMAIN = "testsoap";
    
    private static String PASSWORD = "test123";
    
    static private boolean verbose = false;
    private static HttpDebugListener soapDebugListener;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        // use soap provisioning
        TestUtil.cliSetup();
        
        soapDebugListener = new SoapTest.DebugListener(verbose);
    }
    
    static String baseDomainName() {
        StackTraceElement [] s = new RuntimeException().getStackTrace();
        return s[1].getClassName().toLowerCase() + "." + TEST_SOAP_BASE_DOMAIN;
    }
    
    static SoapTransport authUser(String acctName) throws Exception {
        com.zimbra.soap.account.type.Account acct = 
            new com.zimbra.soap.account.type.Account(
                    com.zimbra.soap.account.type.Account.By.NAME, acctName);
        
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setHttpDebugListener(soapDebugListener);
        
        AuthRequest req = new AuthRequest(acct, PASSWORD);
        AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }
    
    static SoapTransport authAdmin(String acctName) throws Exception {
        
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        transport.setHttpDebugListener(soapDebugListener);
        
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(acctName, PASSWORD);
        com.zimbra.soap.admin.message.AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }
    
    static <T> T invokeJaxb(SoapTransport transport, Object jaxbObject)
    throws ServiceException, IOException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject);
        Element res = transport.invoke(req);
        return (T) JaxbUtil.elementToJaxb(res);
    }

    static class DebugListener implements HttpDebugListener {
        private boolean verbose;
        DebugListener(boolean verbose) {
            this.verbose = verbose;
        }
        
        @Override
        public void receiveSoapMessage(PostMethod postMethod, Element envelope) {
            if (!verbose) {
                return;
            }
            
            System.out.println();
            System.out.println("=== Response ===");
            System.out.println(envelope.prettyPrint());
        }
    
        @Override
        public void sendSoapMessage(PostMethod postMethod, Element envelope) {
            if (!verbose) {
                return;
            }
            
            System.out.println();
            System.out.println("=== Request ===");
            System.out.println(envelope.prettyPrint());
        }
    }

}
