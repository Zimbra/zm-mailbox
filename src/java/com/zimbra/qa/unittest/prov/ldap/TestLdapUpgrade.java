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

import org.junit.*;

import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.upgrade.LdapUpgrade;
import com.zimbra.cs.account.ldap.upgrade.UpgradeTask;

import static org.junit.Assert.*;

public class TestLdapUpgrade extends LdapTest {

    private static LdapProv prov;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = LdapProv.getInst();
    }
    
    private String[] getArgs(String bug) {
        return new String[] {"-b", bug};
    }
    
    @Test
    public void runAllUpgradeTasks() throws Exception {
        
        for (UpgradeTask task : UpgradeTask.values()) {
            String bug = task.getBug();
            
            String[] args;
            if ("27075".equals(bug)) {
                args = new String[] {"-b", "27075", "5.0.12"};
            } else {
                args = getArgs(bug);
            }
            
            LdapUpgrade.upgrade(args);
        }
    }
}
