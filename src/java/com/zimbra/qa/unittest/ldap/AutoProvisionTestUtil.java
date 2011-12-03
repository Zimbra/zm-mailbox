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
package com.zimbra.qa.unittest.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class AutoProvisionTestUtil {

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

    static void verifyAcctAutoProvisioned(Account acct) throws Exception {
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
