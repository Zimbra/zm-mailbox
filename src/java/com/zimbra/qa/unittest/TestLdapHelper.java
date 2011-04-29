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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.prov.ldap.LdapHelper;
import com.zimbra.cs.prov.ldap.LdapProv;

public class TestLdapHelper {

    private static LdapHelper ldapHelper;
    
    @BeforeClass
    public static void init() throws Exception {
        TestLdap.manualInit();
        
        ldapHelper = ((LdapProv) Provisioning.getInstance()).getHelper();
    }
    
    @Test
    public void searchForEntry() throws Exception {
        String base = "cn=zimbra";
        String query = "(cn=config)";
        
        ZSearchResultEntry sr = ldapHelper.searchForEntry(
                base, query, null, false);
        assertNotNull(sr);
        assertEquals("cn=config,cn=zimbra", sr.getDN());
    }
    
    @Test
    public void searchForEntryMultipleMatchedEntries() throws Exception {
        String base = "cn=zimbra";
        String query = "(objectClass=zimbraAccount)";
        
        boolean caughtException = false;
        try {
            ZSearchResultEntry entry = ldapHelper.searchForEntry(
                    base, query, null, false);
            assertNotNull(entry);
        } catch (LdapMultipleEntriesMatchedException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
    }
}
