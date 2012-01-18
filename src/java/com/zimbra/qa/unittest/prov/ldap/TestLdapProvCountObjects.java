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
package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.admin.type.CountObjectsType;

public class TestLdapProvCountObjects extends LdapTest {

    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
        
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
        
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    @Test
    public void countAccount() throws Exception {
        // accounts should be counted as userAccount
        Account userAcct = provUtil.createAccount(genAcctNameLocalPart("user"), domain);
        Account systemAcct = provUtil.createSystemAccount(genAcctNameLocalPart("system-acct"), domain);
        
        // accounts should NOT be counted as userAccount
        Account systemResource = provUtil.createSystemResource(genAcctNameLocalPart("system-resource"), domain);
        
        Domain otherDomain = provUtil.createDomain(genDomainName(domain.getName()));
        Account userAcctOtherDomain = provUtil.createAccount(genAcctNameLocalPart("user"), otherDomain);
        
        long num = prov.countObjects(CountObjectsType.userAccount, domain);
        assertEquals(2, num);
        
        num = prov.countObjects(CountObjectsType.account, domain);
        assertEquals(3, num);
        
        provUtil.deleteAccount(userAcct);
        provUtil.deleteAccount(systemAcct);
        provUtil.deleteAccount(systemResource);
    }
    
    @Test
    public void countAlias() throws Exception {
        // accounts should be counted as userAccount
        Account acct = provUtil.createAccount(genAcctNameLocalPart("user"), domain);
        prov.addAlias(acct, TestUtil.getAddress(genAcctNameLocalPart("alias-1"), domain.getName()));
        prov.addAlias(acct, TestUtil.getAddress(genAcctNameLocalPart("alias-2"), domain.getName()));
        
        long num = prov.countObjects(CountObjectsType.alias, domain);
        assertEquals(2, num);
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void countDL() throws Exception {
        Group staticGroup = provUtil.createGroup(genGroupNameLocalPart("static"), domain, false);
        Group dynamicGroup = provUtil.createGroup(genGroupNameLocalPart("dynamic"), domain, true);
        
        long num = prov.countObjects(CountObjectsType.dl, domain);
        assertEquals(2, num);
        
        provUtil.deleteGroup(staticGroup);
        provUtil.deleteGroup(dynamicGroup);
    }
    
    @Test 
    public void countDomain() throws Exception {
        long num = prov.countObjects(CountObjectsType.domain, null);
        
        // fragile verification, assuming there are only the two domains created by r-t-w
        assertEquals(2, num); 
    }
    
    @Test
    public void countCos() throws Exception {
        Cos cos = provUtil.createCos(genCosName());
        long num = prov.countObjects(CountObjectsType.cos, null);
        
        // fragile verification, assuming there are only the two cos created by r-t-w and 
        // the cos created in this test.
        assertEquals(3, num);
        
        provUtil.deleteCos(cos);
    }
    
    @Test
    public void countServer() throws Exception {
        Server server = provUtil.createServer(genServerName());
        long num = prov.countObjects(CountObjectsType.server, null);
        
        // fragile verification, assuming there are only the server created by r-t-w and 
        // the server created in this test.
        assertEquals(2, num);
        
        provUtil.deleteServer(server);
    }
}
