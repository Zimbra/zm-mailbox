/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

@Ignore
public class TestLdapCache {

    private static final String USER_NAME = "ldapcacheacct";
    private static String domainName;

    private LdapProvisioning ldapProv = null;

    /* 2015 April 21.  Investigating the value of the LDAP cache.
     * With default settings (e.g. ldap_cache_account_maxsize=20000) get this logging:
     *      test - getAccount #ELAPSED_TIME=1ms (15:15:57.085-15:15:57.086)
     * With ldap_cache_account_maxsize=0) get this logging:
     *      test - getAccount #ELAPSED_TIME=735ms (15:19:26.069-15:19:26.804)
     */
    @Test
    public void getAccount() throws Exception {
        long start = System.currentTimeMillis();
        for (int i=0;i < 1000;i++) {
            Account acct = ldapProv.get(AccountBy.name, USER_NAME);
        }
        ZimbraLog.test.info("getAccount %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    /* 2015 April 21.  Investigating the value of the LDAP cache.
     * With default settings (e.g. ldap_cache_domain_maxsize=500) get this logging:
     *      test - getDomain #ELAPSED_TIME=6ms (15:15:57.193-15:15:57.199)
     * With ldap_cache_domain_maxsize=0) get this logging:
     *      test - getDomain #ELAPSED_TIME=232ms (15:19:26.940-15:19:27.172)
     */
    @Test
    public void getDomain() throws Exception {
        long start = System.currentTimeMillis();
        for (int i=0;i < 1000;i++) {
            Domain dom = ldapProv.get(DomainBy.name, domainName);
        }
        ZimbraLog.test.info("getDomain %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    @Before
    public void setUp() throws Exception {
        ldapProv = (LdapProvisioning) Provisioning.getInstance();
        tearDown();
        Account acct = TestUtil.createAccount(USER_NAME);
        domainName = acct.getDomainName();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        TestUtil.cliSetup();
        try {
            TestUtil.runTest(TestLdapCache.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
