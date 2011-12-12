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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapObjectClassHierarchy;
import com.zimbra.cs.account.ldap.LdapProv;

public class TestObjectClass extends LdapTest {
    
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        // Cleanup.deleteAll(baseDomainName());
    }
    
    @Test
    public void getMostSpecificOC() throws Exception {
        LdapProv ldapProv = (LdapProv) prov;
        
        assertEquals("inetOrgPerson" , 
                LdapObjectClassHierarchy.getMostSpecificOC(ldapProv, 
                        new String[]{"zimbraAccount", "organizationalPerson", "person"}, "inetOrgPerson"));
        
        assertEquals("inetOrgPerson" , 
                LdapObjectClassHierarchy.getMostSpecificOC(ldapProv, 
                        new String[]{"inetOrgPerson"}, "organizationalPerson"));
        
        assertEquals("inetOrgPerson" , 
                LdapObjectClassHierarchy.getMostSpecificOC(ldapProv, 
                        new String[]{"organizationalPerson", "inetOrgPerson"}, "person"));
        
        assertEquals("bbb" , 
                LdapObjectClassHierarchy.getMostSpecificOC(ldapProv, 
                        new String[]{"inetOrgPerson"}, "bbb"));
        
        assertEquals("inetOrgPerson" , 
                LdapObjectClassHierarchy.getMostSpecificOC(ldapProv, 
                        new String[]{"aaa"}, "inetOrgPerson"));
        
        assertEquals("inetOrgPerson" , 
                LdapObjectClassHierarchy.getMostSpecificOC(ldapProv, 
                        new String[]{"person", "inetOrgPerson"}, "organizationalPerson"));
 
    }
    
    @Test
    public void getAttrsInOCs() throws Exception {
        LdapProv ldapProv = (LdapProv) prov;
        
        String[] ocs = { "amavisAccount" };
        Set<String> attrsInOCs = new HashSet<String>();
        ldapProv.getAttrsInOCs(ocs, attrsInOCs);
        
        assertEquals(48, attrsInOCs.size());
        assertTrue(attrsInOCs.contains("amavisBlacklistSender"));
        assertTrue(attrsInOCs.contains("amavisWhitelistSender"));
        
        /*
        int i = 1;
        for (String attr : attrsInOCs) {
            System.out.println(i++ + " " + attr);
        }
        */
    }
}
