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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.soap.SoapProvisioning;


public class TestIDN extends TestCase {
    private String TEST_ID;
    private static String TEST_NAME = "test-IDN";
    private static String PASSWORD = "test123";
   
    
    private Provisioning mProv;
    private String BASE_DOMAIN_NAME;
   
    public void setUp() throws Exception {
        
        TEST_ID = TestProvisioningUtil.genTestId();
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        mProv = Provisioning.getInstance();
        BASE_DOMAIN_NAME = "." + TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
    }
    
    private Domain createDomain(String domainName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = mProv.createDomain(domainName, attrs);
        assertNotNull(domain);
        return domain;
    }
    
    private Account createAccount(String accountEmail) throws Exception {
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        Account acct = mProv.createAccount(accountEmail, PASSWORD, acctAttrs);
        assertNotNull(acct);
        return acct;
    }
    
    public void testIDN() throws Exception {
        String domainName = "abc.\u5f35\u611b\u73b2" + BASE_DOMAIN_NAME;
        createDomain(domainName);
        createAccount("phoebe@" + domainName);
        
        System.out.println("\nTest " + TEST_ID + " done\n");
    }
 
    public static void main(String[] args) throws Exception {
        /*
         * 
         * cliSetup forces instanciating a SoapProvisioning, for now we want LdapProvisioning
         * SOAP: UTF-8 (multi bytes)
         * LDAP: UTF-8 (multi bytes)
         * Java: UTF-16 (2 bytes Unicode) 
         *       The primitive data type char in the Java programming language is an unsigned 16-bit 
         *       integer that can represent a Unicode code point in the range U+0000 to U+FFFF, 
         *       or the code units of UTF-16.
         */
        // TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestIDN.class));
        
    }

}
