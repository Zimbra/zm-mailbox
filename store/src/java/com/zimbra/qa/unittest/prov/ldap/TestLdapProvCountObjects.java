/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
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
        
        // accounts should NOT be counted as internalUserAccount
        Account externalAccount = provUtil.createExternalAccount(genAcctNameLocalPart("external"), domain);
        
        // archiving account
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_amavisArchiveQuarantineTo, "test@junk.com");
        Account archivingAcct = provUtil.createAccount(genAcctNameLocalPart("archiving"), domain, attrs);
        
        Domain otherDomain = provUtil.createDomain(genDomainName(domain.getName()));
        Account userAcctOtherDomain = provUtil.createAccount(genAcctNameLocalPart("user"), otherDomain);
        
        long num = prov.countObjects(CountObjectsType.userAccount, domain, null);
        assertEquals(4, num); // userAcct, systemAcct, externalAccount, archivingAcct
        
        num = prov.countObjects(CountObjectsType.account, domain, null);
        assertEquals(5, num); // all accounts
        
        num = prov.countObjects(CountObjectsType.internalUserAccount, domain, null);
        assertEquals(3, num);  // userAcct, systemAcct, archivingAcct
        
        num = prov.countObjects(CountObjectsType.internalArchivingAccount, domain, null);
        assertEquals(1, num);  // archivingAcct
        
        provUtil.deleteAccount(userAcct);
        provUtil.deleteAccount(systemAcct);
        provUtil.deleteAccount(systemResource);
        provUtil.deleteAccount(externalAccount);
        provUtil.deleteAccount(archivingAcct);
        provUtil.deleteAccount(userAcctOtherDomain);
        provUtil.deleteDomain(otherDomain);
    }
    
    @Test
    public void countAlias() throws Exception {
        // accounts should be counted as userAccount
        Account acct = provUtil.createAccount(genAcctNameLocalPart("user"), domain);
        prov.addAlias(acct, TestUtil.getAddress(genAcctNameLocalPart("alias-1"), domain.getName()));
        prov.addAlias(acct, TestUtil.getAddress(genAcctNameLocalPart("alias-2"), domain.getName()));
        
        long num = prov.countObjects(CountObjectsType.alias, domain, null);
        assertEquals(2, num);
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void countDL() throws Exception {
        Group staticGroup = provUtil.createGroup(genGroupNameLocalPart("static"), domain, false);
        Group dynamicGroup = provUtil.createGroup(genGroupNameLocalPart("dynamic"), domain, true);
        
        Domain subDomain = provUtil.createDomain(genDomainName(domain.getName()));
        Group staticGroup1Sub = provUtil.createGroup(genGroupNameLocalPart("static-1"), subDomain, false);
        Group dynamicGroup1Sub = provUtil.createGroup(genGroupNameLocalPart("dynamic-1"), subDomain, true);
        Group staticGroup2Sub = provUtil.createGroup(genGroupNameLocalPart("static-2"), subDomain, false);
        Group dynamicGroup2Sub = provUtil.createGroup(genGroupNameLocalPart("dynamic-2"), subDomain, true);
        
        long num;
        
        num = prov.countObjects(CountObjectsType.dl, domain, null);
        assertEquals(2, num);  // groups in sub domains should not be counted
        
        provUtil.deleteGroup(staticGroup);
        provUtil.deleteGroup(dynamicGroup);
        provUtil.deleteGroup(staticGroup1Sub);
        provUtil.deleteGroup(dynamicGroup1Sub);
        provUtil.deleteGroup(staticGroup2Sub);
        provUtil.deleteGroup(dynamicGroup2Sub);
        
        provUtil.deleteDomain(subDomain);
    }
    
    @Test 
    public void countDomain() throws Exception {
        
        Domain subDomain = provUtil.createDomain(genDomainName(domain.getName()));
        Domain subSubDomain = provUtil.createDomain(genDomainName(subDomain.getName()));
        long num = prov.countObjects(CountObjectsType.domain, domain, null);
        
        // count sub domains of a domain, the domain itself should be included in the count.
        assertEquals(3, num); // domain, subDomain, subSubDomain
        
        // count all domains
        num = prov.countObjects(CountObjectsType.domain, null, null);
        
        // fragile verification, assuming there are the two domains created by r-t-w 
        // and the domains created by this test.
        assertEquals(2+3, num); // example.com, phoebe.mbp + domain, subDomain, subSubDomain
        
        provUtil.deleteDomain(subSubDomain);
        provUtil.deleteDomain(subDomain);
    }
    
    @Test
    public void countCos() throws Exception {
        Cos cos = provUtil.createCos(genCosName());
        long num = prov.countObjects(CountObjectsType.cos, null, null);
        
        // fragile verification, assuming there are only the two cos created by r-t-w and 
        // the cos created in this test.
        assertEquals(3, num);
        
        provUtil.deleteCos(cos);
    }
    
    @Test
    public void countServer() throws Exception {
        Server server = provUtil.createServer(genServerName());
        long num = prov.countObjects(CountObjectsType.server, null, null);
        
        // fragile verification, assuming there are only the server created by r-t-w and 
        // the server created in this test.
        assertEquals(2, num);
        
        provUtil.deleteServer(server);
    }
}
