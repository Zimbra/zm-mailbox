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

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Provisioning;

public class TestLdapProvDynamicGroup extends TestLdap {
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = Provisioning.getInstance();
        domain = TestLdapProvDomain.createDomain(prov, baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestLdapProvDynamicGroup.class.getName().toLowerCase();
    }
    
    static DynamicGroup createDynamicGroup(Provisioning prov, String localPart, 
            Domain domain, Map<String, Object> attrs) throws Exception {
        return (DynamicGroup) TestLdapProvDistributionList.createGroup(
                prov, localPart, domain, attrs, true);
    }
    
    static DynamicGroup createDynamicGroup(String localPart) throws Exception {
        return createDynamicGroup(prov, localPart, domain, null);
    }
    
    static void deleteDynamicGroup(Provisioning prov, DynamicGroup group) throws Exception {
        TestLdapProvDistributionList.deleteGroup(prov, group);
    }

}
