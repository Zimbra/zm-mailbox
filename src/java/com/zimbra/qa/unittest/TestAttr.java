/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;


public class TestAttr extends TestCase {
    private String TEST_ID = TestProvisioningUtil.genTestId();;
    private static String TEST_NAME = "test-attr";
    private static String PASSWORD = "test123";

    private String DOMAIN_NAME;
    private String ACCT_EMAIL;
    private String SERVER_NAME;
    private String COS_NAME;
    
    private Provisioning mProv;
    
    public void setUp() throws Exception {
        DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
        ACCT_EMAIL = "user1" + "@" + DOMAIN_NAME;
        SERVER_NAME = "server-" + TEST_ID + "-" + TEST_NAME;
        COS_NAME = "cos-" + TEST_ID + "-" + TEST_NAME;
        
        mProv = Provisioning.getInstance();        
    }
    
    private Account createAccount() throws Exception {
        Domain domain = getDomain();
        Cos cos = getCos();
        
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        Account acct = mProv.createAccount(ACCT_EMAIL, PASSWORD, attrs);
        assertNotNull(acct);
        return acct;
    }
    
    private Cos createCos() throws Exception{
        Cos cos = mProv.createCos(COS_NAME, new HashMap<String, Object>());
        assertNotNull(cos);
        return cos;
    }
    
    private Domain createDomain() throws Exception{
        Domain domain = mProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        return domain;
    }
    
    private Server createServer() throws Exception {
        Server server = mProv.createServer(SERVER_NAME, new HashMap<String, Object>());
        assertNotNull(server);
        return server;
    }
    
    private Account getAccount() throws Exception {
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        if (acct == null)
            acct = createAccount();
        assertNotNull(acct);
        return acct;        
    }
    
    private Cos getCos() throws Exception {
        Cos cos = mProv.get(Provisioning.CosBy.name, COS_NAME);
        if (cos == null)
            cos = createCos();
        assertNotNull(cos);
        return cos;        
    }
    
    private Domain getDomain() throws Exception {
        Domain domain = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        if (domain == null)
            domain = createDomain();
        assertNotNull(domain);
        return domain;        
    }
    
    private Server getServer() throws Exception {
        Server server = mProv.get(Provisioning.ServerBy.name, SERVER_NAME);
        if (server == null)
            server = createServer();
        assertNotNull(server);
        return server;        
    }
    
    private void cannotUnset(Entry entry, String attrName) {
        boolean good = false;
        
        try {
            unsetByEmptyString(entry, attrName);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode()) && 
                e.getMessage().contains("is a required attribute"))
                good = true;
        }
        assertTrue(good);        
        
        try {
            unsetByNull(entry, attrName);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode()) && 
                e.getMessage().contains("is a required attribute"))
                good = true;
        }
        assertTrue(good); 
    }
    
    private void unsetByEmptyString(Entry entry, String attrName) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, "");
        mProv.modifyAttrs(entry, attrs);
        String newValue = entry.getAttr(attrName, false);
        assertNull(newValue);
    }
    
    private void unsetByNull(Entry entry, String attrName) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, null);
        mProv.modifyAttrs(entry, attrs);
        String newValue = entry.getAttr(attrName, false);
        assertNull(newValue);
    }
        
    private void unsetTest(Entry entry, String attrName) throws ServiceException {
        unsetByEmptyString(entry, attrName);
        unsetByNull(entry, attrName);
    }
    
    private void setAttr(Entry entry, String attrName, String value) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, value);
        mProv.modifyAttrs(entry, attrs);
        String newValue = entry.getAttr(attrName, false);
        assertEquals(value, newValue);
    }
    
    public void testSingleValuedAttr() throws Exception {
        Account acct = getAccount();
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        String attrName = Provisioning.A_zimbraInterceptAddress;
        String V = "test@example.com";
        
        String value = acct.getAttr(attrName);
        assertNull(value);
        
        // set a value
        attrs.clear();
        attrs.put(attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertEquals(V, value);
        
        // delete the value by passing ""
        unsetByEmptyString(acct, attrName);
        
        // set a value
        attrs.clear();
        attrs.put(attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertEquals(V, value);
        
        // delete the value by passing null
        unsetByNull(acct, attrName);
        
        // set a value
        attrs.clear();
        attrs.put(attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertEquals(V, value);
        
        // delete the value by passing -{attr anme}
        attrs.clear();
        attrs.put("-" + attrName, V);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertNull(value);
    }
    
    public void testMultiValuedAttr() throws Exception {
        Account acct = getAccount();
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        String attrName = Provisioning.A_zimbraMailForwardingAddress;
        String V1 = "test-1@example.com";
        String V2 = "test-2@example.com";
        
        String value;
        Set<String> values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 0);
      
        // set a value
        attrs.clear();
        attrs.put(attrName, V1);
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 1 && values.contains(V1));
        
        // add a value
        attrs.clear();
        attrs.put("+" + attrName, V2);
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 2 && values.contains(V1) && values.contains(V2));

        // removing a value
        attrs.clear();
        attrs.put("-" + attrName, V1);
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 1 && values.contains(V2));
        
        // delete all values by passing ""
        attrs.clear();
        attrs.put(attrName, "");
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertNull(value);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 0);
        
        // set 2 values
        attrs.clear();
        attrs.put(attrName, new String[] {V1, V2});
        mProv.modifyAttrs(acct, attrs);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 2 && values.contains(V1) && values.contains(V2));
        
        // delete all values by passing null
        attrs.clear();
        attrs.put(attrName, null);
        mProv.modifyAttrs(acct, attrs);
        value = acct.getAttr(attrName);
        assertNull(value);
        values = acct.getMultiAttrSet(attrName);
        assertTrue(values != null && values.size() == 0);
    }
    
    public void testDurationAttr() throws Exception {
        Server server = getServer();  
        String attrName = Provisioning.A_zimbraHsmAge;
        
        String strValue;
        long msValue;
        long secValue;
        Map<String, Object> attrs = new HashMap<String, Object>();
                
        // nnnnn([hmsd]|ms)
        
        // d (day)
        attrs.put(attrName, "1d");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1d", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(Constants.MILLIS_PER_DAY, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(Constants.SECONDS_PER_DAY, secValue);
        
        // h (hour)
        attrs.put(attrName, "1h");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1h", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(Constants.MILLIS_PER_HOUR, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(Constants.SECONDS_PER_HOUR, secValue);
        
        // m (minute)
        attrs.put(attrName, "1m");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1m", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(Constants.MILLIS_PER_MINUTE, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(Constants.SECONDS_PER_MINUTE, secValue);
        
        // s (second)
        attrs.put(attrName, "1s");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1s", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(Constants.MILLIS_PER_SECOND, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(1, secValue);
        
        // default (== s)
        attrs.put(attrName, "1");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(Constants.MILLIS_PER_SECOND, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(1, secValue);
        
        // ms (milli second)
        attrs.put(attrName, "1ms");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1ms", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(1, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(0, secValue);
        
        attrs.put(attrName, "500ms");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("500ms", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(500, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(1, secValue);
        
        attrs.put(attrName, "1000ms");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1000ms", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(1000, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(1, secValue);
        
        attrs.put(attrName, "1001ms");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("1001ms", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(1001, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(1, secValue);
        
        attrs.put(attrName, "999ms");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("999ms", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(999, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(1, secValue);
        
        attrs.put(attrName, "0ms");
        mProv.modifyAttrs(server, attrs);
        strValue = server.getAttr(attrName);
        assertEquals("0ms", strValue);
        msValue = server.getTimeInterval(attrName, 0);
        assertEquals(0, msValue);
        secValue = server.getTimeIntervalSecs(attrName, 0);
        assertEquals(0, secValue);
        
        
        // invalid unit
        boolean good = false;
        attrs.put(attrName, "1y");
        try {
            mProv.modifyAttrs(server, attrs);
        } catch (AccountServiceException e) {
            if (AccountServiceException.INVALID_ATTR_VALUE.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        good = false;
        attrs.put(attrName, "1mm");
        try {
            mProv.modifyAttrs(server, attrs);
        } catch (AccountServiceException e) {
            if (AccountServiceException.INVALID_ATTR_VALUE.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
    }
    
    public void testCallbackAccountStatus() throws Exception {
        Account acct = getAccount();
        String attrName = Provisioning.A_zimbraAccountStatus;
        
        String value = acct.getAttr(attrName);
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE, value);
        
        cannotUnset(acct, attrName);       
        
        setAttr(acct, attrName, Provisioning.ACCOUNT_STATUS_CLOSED);
        assertEquals(acct.getAttr(Provisioning.A_zimbraMailStatus), Provisioning.MAIL_STATUS_DISABLED);
    }
    
    public void testCallbackCheckPortConflict() throws Exception {
        Server server = getServer();    
        String attrName = Provisioning.A_zimbraLmtpBindPort;
        
        unsetTest(server, attrName);
    }
    
    public void testCallbackDataSource() throws Exception {
        Account acct = getAccount();        
        String attrName = Provisioning.A_zimbraDataSourcePollingInterval;
        
        unsetTest(acct, attrName);
    }
    
    public void testCallbackDisplayName() throws Exception {
        Account acct = getAccount();
        String attrName = Provisioning.A_displayName;
        
        unsetTest(acct, attrName);
    }
    
    public void testCallbackDomainStatus() throws Exception {
        Domain domain = getDomain();
        String attrName = Provisioning.A_zimbraDomainStatus;
        
        // TODO
        // unsetTest(domain, attrName);
    }
    
    public void testCallbackMailSignature() throws Exception {
        Account acct = getAccount();
        String attrName = Provisioning.A_zimbraPrefMailSignature;
        
        unsetTest(acct, attrName);
        
        // set a limit on cos
        Cos cos = getCos();
        setAttr(cos, Provisioning.A_zimbraMailSignatureMaxLength, "10");
        
        // cannot have signature longer than the max len
        boolean good = false;
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, "12345678901");
        try {
            mProv.modifyAttrs(acct, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode()) && 
                e.getMessage().contains("is longer than the limited value"))
                good = true;
        }
        assertTrue(good); 
    }
 
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        try {
            TestUtil.runTest(TestAttr.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

