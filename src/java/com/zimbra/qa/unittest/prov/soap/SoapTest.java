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
package com.zimbra.qa.unittest.prov.soap;

import java.io.IOException;

import org.junit.BeforeClass;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.ProvTest;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;

public class SoapTest extends ProvTest {
    private static boolean JSON = false;
    
    private static final String SOAP_TEST_BASE_DOMAIN = "soaptest";
    
    private static String PASSWORD = "test123";
    private static HttpDebugListener soapDebugListener;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        CliUtil.toolSetup(); // init ssl stuff
        soapDebugListener = new SoapDebugListener();
        
        // init rights
        RightManager.getInstance();
    }
    
    static String baseDomainName() {
        StackTraceElement [] s = new RuntimeException().getStackTrace();
        return s[1].getClassName().toLowerCase() + "." + 
                SOAP_TEST_BASE_DOMAIN + "." + InMemoryLdapServer.UNITTEST_BASE_DOMAIN_SEGMENT;
    }
    
    static SoapTransport authUser(String acctName) throws Exception {
        com.zimbra.soap.type.AccountSelector acct = 
            new com.zimbra.soap.type.AccountSelector(com.zimbra.soap.type.AccountBy.name, acctName);
        
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
        SoapProtocol proto = JSON ? SoapProtocol.SoapJS : SoapProtocol.Soap12;
        Element req = JaxbUtil.jaxbToElement(jaxbObject, proto.getFactory());
        
        Element res = transport.invoke(req);
        return (T) JaxbUtil.elementToJaxb(res);
    }
    
    static <T> T invokeJaxbOnTargetAccount(SoapTransport transport, Object jaxbObject,
            String targetAcctId) 
    throws Exception {
        String oldTarget = transport.getTargetAcctId();
        try {
            transport.setTargetAcctId(targetAcctId);
            return invokeJaxb(transport, jaxbObject);
        } finally {
            transport.setTargetAcctId(oldTarget);
        }
    }
}
