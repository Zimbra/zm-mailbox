/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.zclient.ZMailbox;

public class TestSoap
extends TestCase {

    private static final String NAME_PREFIX = TestSoap.class.getSimpleName();
    
    private String mOriginalSoapRequestMaxSize;
    private String mOriginalSoapExposeVersion;
    
    public void setUp()
    throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        mOriginalSoapRequestMaxSize = server.getAttr(Provisioning.A_zimbraSoapRequestMaxSize, "");
        mOriginalSoapExposeVersion = server.getAttr(Provisioning.A_zimbraSoapExposeVersion, "");
        cleanUp();
    }
    
    public void testSoapRequestMaxSize()
    throws Exception {
        StringBuilder messageBody = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            messageBody.append("Morey Amsterdam was a great man.  Morey Amsterdam was not a sandwich.\r\n");
        }
        
        setSoapRequestMaxSize(100000);
        ZMailbox mbox = TestUtil.getZMailbox("user1");
        TestUtil.sendMessage(mbox, "user1", NAME_PREFIX + " 1", messageBody.toString());
        
        setSoapRequestMaxSize(1000);
        try {
            TestUtil.sendMessage(mbox, "user1", NAME_PREFIX + " 2", messageBody.toString());
            fail("SOAP request should not have succeeded.");
        } catch (SoapFaultException e) {
            assertTrue("Unexpected error: " + e.toString(), e.toString().contains("bytes set for zimbraSoapRequestMaxSize"));
        }
    }

    /**
     * Tests the AccountService version of GetInfoRequest (see bug 30010).
     */
    public void testAccountGetInfoRequest()
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.GET_VERSION_INFO_REQUEST);
        
        // Test with version exposed
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapExposeVersion, LdapUtil.LDAP_TRUE);
        Element response = transport.invoke(request);
        validateSoapVersionResponse(response);
        
        // Test with version not exposed
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapExposeVersion, LdapUtil.LDAP_FALSE);
        request = Element.create(transport.getRequestProtocol(), AccountConstants.GET_VERSION_INFO_REQUEST);
        try {
            response = transport.invoke(request);
            fail("GetInfoRequest should have failed");
        } catch (SoapFaultException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }
    
    /**
     * Tests the AdminService version of GetInfoRequest.
     */
    public void testAdminGetInfoRequest()
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        Element request = Element.create(transport.getRequestProtocol(), AdminConstants.GET_VERSION_INFO_REQUEST);
        Element response = transport.invoke(request);
        validateSoapVersionResponse(response);
    }
    
    private void validateSoapVersionResponse(Element response)
    throws ServiceException {
        assertEquals(AccountConstants.GET_VERSION_INFO_RESPONSE.getName(), response.getName());
        
        Element info = response.getElement(AccountConstants.E_VERSION_INFO_INFO);
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_DATE));
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_HOST));
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_RELEASE));
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_VERSION));
    }
    
    public void tearDown()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapRequestMaxSize, mOriginalSoapRequestMaxSize);
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapExposeVersion, mOriginalSoapExposeVersion);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData("user1", NAME_PREFIX);
    }
    
    private void setSoapRequestMaxSize(int numBytes)
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapRequestMaxSize, Integer.toString(numBytes));
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestSoap.class));
    }
}
