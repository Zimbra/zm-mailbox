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
package com.zimbra.qa.unittest;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.ldap.LdapProv;

public class TestLdapProvDIT extends TestLdap {
    private static LdapProv prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = LdapProv.getInst();
        domain = TestLdapProvDomain.createDomain(prov, baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestLdapProvDIT.class.getName().toLowerCase();
    }
    
    
    // TODO, test all DIT methods and asserts
    
    @Test
    public void domainNameToDN() throws Exception {
        String DOMAIN_NAME = domain.getName();
        String domainDN = prov.getDIT().domainNameToDN(DOMAIN_NAME);
    }
    
}
