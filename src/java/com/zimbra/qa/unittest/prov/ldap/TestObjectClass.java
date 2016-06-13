/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
