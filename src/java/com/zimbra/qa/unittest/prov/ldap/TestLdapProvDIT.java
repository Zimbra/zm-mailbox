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
import static org.junit.Assert.*;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.ldap.LdapProv;

public class TestLdapProvDIT extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
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
    
    // TODO, test all DIT methods and asserts
    
    @Test
    public void domainNameToDN() throws Exception {
        String DOMAIN_NAME = domain.getName();
        String domainDN = prov.getDIT().domainNameToDN(DOMAIN_NAME);
    }
    
}
