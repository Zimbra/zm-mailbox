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

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.qa.unittest.prov.Names;

public class TestLdapProvDynamicGroup extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private DynamicGroup createDynamicGroup(String localPart) throws Exception {
        return provUtil.createDynamicGroup(localPart, domain);
    }
    
    private void deleteDynamicGroup(DynamicGroup dg) throws Exception {
        provUtil.deleteDynamicGroup(dg);
    }
    
    @Test
    public void createDynamicGroup() throws Exception {
        String DG_NAME_LOCALPART = Names.makeDLNameLocalPart("createDynamicGroup");
        
        DynamicGroup dg = createDynamicGroup(DG_NAME_LOCALPART);
        
        // make sure the group has a home server
        Server homeServer = dg.getServer();
        assertNotNull(homeServer);
        
        deleteDynamicGroup(dg);
    }

}
