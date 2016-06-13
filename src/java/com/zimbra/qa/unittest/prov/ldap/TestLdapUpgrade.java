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
