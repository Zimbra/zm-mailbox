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
package com.zimbra.qa.unittest.prov;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.AutoProvisionListener;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZMutableEntry;

public class AutoProvisionTestUtil {

    /**
     * A AutoProvisionListener that marks entry "provisioned" in the external directory
     *
     */
    public static class MarkEntryProvisionedListener implements AutoProvisionListener {
        private static final String PROVED_INDICATOR_ATTR = Provisioning.A_zimbraNotes;
        private static final String PROVED_NOTE = "PROVISIONED IN ZIMBRA";
        public static final String NOT_PROVED_FILTER = "(!(" + PROVED_INDICATOR_ATTR + "=" + PROVED_NOTE + "))";
        
        public MarkEntryProvisionedListener() {
        }
        
        @Override
        public void postCreate(Domain domain, Account acct, String externalDN) {
            Map<String, Object> attrs = Maps.newHashMap();
            attrs.put(PROVED_INDICATOR_ATTR, PROVED_NOTE);
            try {
                modifyExternalAcctEntry(externalDN, attrs);
            } catch (Exception e) {
                fail();
            }
        }
        
        private void modifyExternalAcctEntry(String externalDN, Map<String, Object> extAcctAttrs) 
        throws Exception {
            ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            try {
                ZMutableEntry entry = LdapClient.createMutableEntry();
                entry.mapToAttrs(extAcctAttrs);
                zlc.replaceAttributes(externalDN, entry.getAttributes());
            } finally {
                LdapClient.closeContext(zlc);
            }
        }
    }

    public static Map<String, Object> commonZimbraDomainAttrs() {
        Map<String, Object> zimbraDomainAttrs = new HashMap<String, Object>();
        
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvAuthMech, AutoProvAuthMech.LDAP.name());
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvMode, AutoProvMode.LAZY.name());
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvMode, AutoProvMode.MANUAL.name());
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvMode, AutoProvMode.EAGER.name());
        
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapURL, "ldap://localhost:389");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindDn, "cn=config");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapAdminBindPassword, "zimbra");
    
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvAttrMap, "sn=displayName");
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvAttrMap, "displayName=sn");
                
        return zimbraDomainAttrs;
    }

    public static void verifyAcctAutoProvisioned(Account acct) throws Exception {
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct, null);
    }

    public static void verifyAcctAutoProvisioned(Account acct, String expectedAcctName) 
    throws Exception {
        assertNotNull(acct);
        if (expectedAcctName != null) {
            assertEquals(expectedAcctName, acct.getName());
        }
        AutoProvisionTestUtil.verifyAttrMapping(acct);
    }

    static void verifyAttrMapping(Account acct) throws Exception {
        assertEquals("last name", acct.getAttr(Provisioning.A_displayName));
        assertEquals("display name", acct.getAttr(Provisioning.A_sn));
    }

}
