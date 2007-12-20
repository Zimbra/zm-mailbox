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

import java.util.HashMap;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.servlet.ZimbraServlet;

public class TestCreateAccount extends TestCase {
    private String TEST_ID = TestProvisioningUtil.genTestId();;
    private static String TEST_NAME = "test-soap-prov";
    private static String PASSWORD = "test123";
    private static int NUM_THREADS = 10;
    private static int NUM_ACCTS_PER_THREAD = 500;

    private String DOMAIN_NAME = null;
   
    /*
     * for testCreateAccount
     * 
     * call either setUpDomain or setUpAccounts
     */
    private void setUpDomain() throws Exception {
        
        Provisioning prov = Provisioning.getInstance();
        assertTrue(prov instanceof LdapProvisioning);
            
        DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
        Domain domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
    }
    
    /*
     * for testGetAccount, create only one and sue for subsequent tests
     * 
     * call either setUpDomain or setUpAccounts
     */
    private void setUpAccounts() throws Exception {
        
        Provisioning prov = Provisioning.getInstance();
        assertTrue(prov instanceof LdapProvisioning);
            
        DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, null);
        Domain domain = prov.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        if (domain != null)
            return;
        
        domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        
        // simulate accounts created by the concurrent create account test
        for (int i=0; i<NUM_THREADS; i++) {
            createAccount(prov, i);
        }
    }
    
    private SoapProvisioning getSoapProv() throws Exception {
        SoapProvisioning sp = new SoapProvisioning();    
        String server = LC.zimbra_zmprov_default_soap_server.value();
        int port = LC.zimbra_admin_service_port.intValue();
        
        sp.soapSetURI(LC.zimbra_admin_service_scheme.value()+server+":"+port+ZimbraServlet.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
        return sp;            
    }
    
    private String makeAcctName(int threadIdx, int idx) {
        return "a-" + idx + "-thread-"+ threadIdx + "@" + DOMAIN_NAME;
    }
    
    private void createAccount(Provisioning prov, int threadIdx) throws Exception {
        for (int i=0; i<NUM_ACCTS_PER_THREAD; i++) {
            System.out.println("createAccount: " + threadIdx + ", " + i);
            String acctName = makeAcctName(threadIdx, i);
            try {
                Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
                assertNotNull(acct);
            } catch (Exception e) {
                System.out.println("createAccount caught exception: " + threadIdx + ", " + i);
                throw e;
            }
        }
    }
    
    private void getAccount(int threadIdx) throws Exception {
        SoapProvisioning prov = getSoapProv();
        for (int i=0; i<NUM_ACCTS_PER_THREAD; i++) {
            System.out.println("getAccount: " + threadIdx + ", " + i);
            String acctName = makeAcctName(threadIdx, i);
            Account acct = prov.get(Provisioning.AccountBy.name, acctName);
            assertNotNull(acct);
        }
    }
    
    abstract class TestThread extends Thread {
        
        protected int mThreadIdx;
        
        TestThread(int threadIdx) {
            mThreadIdx = threadIdx;
        }
        
        abstract void doRun() throws Exception ; 
        
        public void run() {
            try {
                System.out.println("thread " + mThreadIdx + " started");
                doRun();
            } catch (Exception e) {
                System.out.println("thread " + mThreadIdx + " caught exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
    
    class TestCreateAccountThread extends TestThread {
        
        TestCreateAccountThread(int threadIdx) {
            super(threadIdx);
        }
        
        public void doRun() throws Exception {
            SoapProvisioning soapProv = getSoapProv();
            createAccount(soapProv, mThreadIdx);
        }
    }

    class TestGetAccountThread extends TestThread {
        
        TestGetAccountThread(int threadIdx) {
            super(threadIdx);
        }
        
        public void doRun() throws Exception {
            getAccount(mThreadIdx);
        }
    }
    
    public void testCreateAccount() throws Exception {
        
        setUpDomain();
        CliUtil.toolSetup(); // setup cert
        
        for (int i=0; i<NUM_THREADS; i++) {
            TestThread t = new TestCreateAccountThread(i);
            t.start();
        }
    }
    
    public void xxxtestGetAccount() throws Exception {
        
        setUpAccounts();
        CliUtil.toolSetup(); // setup cert
        
        for (int i=0; i<NUM_THREADS; i++) {
            TestThread t = new TestGetAccountThread(i);
            t.start();
        }
    }
 
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        try {
            TestUtil.runTest(new TestSuite(TestCreateAccount.class));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

