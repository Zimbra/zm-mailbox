/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.zclient.ZMailbox;

public class TestSoap
extends TestCase {

    private static final String NAME_PREFIX = TestSoap.class.getSimpleName();
    
    private String mOriginalSoapRequestMaxSize;
    
    public void setUp()
    throws Exception {
        mOriginalSoapRequestMaxSize = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraSoapRequestMaxSize, null);
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
    
    public void tearDown()
    throws Exception {
        setSoapRequestMaxSize(mOriginalSoapRequestMaxSize);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData("user1", NAME_PREFIX);
    }
    
    private void setSoapRequestMaxSize(int numBytes)
    throws Exception {
        setSoapRequestMaxSize(Integer.toString(numBytes));
    }
    
    private void setSoapRequestMaxSize(String numBytes)
    throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraSoapRequestMaxSize, numBytes);
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        prov.modifyAttrs(server, attrs);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestSoap.class));
    }
}
