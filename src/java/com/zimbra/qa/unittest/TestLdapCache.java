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

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class TestLdapCache {

    private final int ITERATIONS = 1000;
    private static final String DOMAIN_NAME = "ldap.cache.domain";
    private static final String USER_NAME = "ldapcacheacct@" + DOMAIN_NAME;
    private static final String COS_NAME = "testldapcachecos";

    private LdapProvisioning ldapProv = null;

    /* 2015 April 21.  Investigating the value of the LDAP cache with 1000 iterations before freshness checker changes.
     * With default settings (e.g. ldap_cache_account_maxsize=20000) get this logging:
     *      test - getAccount #ELAPSED_TIME=1ms (15:15:57.085-15:15:57.086)
     * With ldap_cache_account_maxsize=0) get this logging:
     *      test - getAccount #ELAPSED_TIME=735ms (15:19:26.069-15:19:26.804)
     */
    // @Ignore("Useful for performance testing") @Test
    @Test
    public void getAccount() throws Exception {
        long start = System.currentTimeMillis();
        for (int i=0;i < ITERATIONS;i++) {
            Account acct = ldapProv.get(AccountBy.name, USER_NAME);
        }
        ZimbraLog.test.info("getAccount %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    /* 2015 April 21.  Investigating the value of the LDAP cache with 1000 iterations before freshness checker changes.
     * With default settings (e.g. ldap_cache_domain_maxsize=500) get this logging:
     *      test - getDomain #ELAPSED_TIME=6ms (15:15:57.193-15:15:57.199)
     * With ldap_cache_domain_maxsize=0) get this logging:
     *      test - getDomain #ELAPSED_TIME=232ms (15:19:26.940-15:19:27.172)
     */
    // @Ignore("Useful for performance testing") @Test
    @Test
    public void getDomain() throws Exception {
        long start = System.currentTimeMillis();
        for (int i=0;i < ITERATIONS;i++) {
            Domain dom = ldapProv.get(DomainBy.name, DOMAIN_NAME);
        }
        ZimbraLog.test.info("getDomain %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    // @Ignore("Useful for performance testing") @Test
    @Test
    public void getCos() throws Exception {
        long start = System.currentTimeMillis();
        for (int i=0;i < ITERATIONS;i++) {
            ldapProv.get(CosBy.name, COS_NAME);
        }
        ZimbraLog.test.info("getCos %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    @Test
    public void accountTest() throws Exception {
        Account account = ldapProv.getAccountByName(USER_NAME);
        Account newAccount = ldapProv.getAccountByName(USER_NAME);
        Assert.assertEquals("account with no mods", account, newAccount);
        String origDesc[] = account.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in origDesc %s", Joiner.on(',').join(origDesc)),
                0, origDesc.length);
        account.addDescription("Pink");
        Thread.sleep(LC.ldap_cache_freshness_check_limit_ms.longValue() + 1);
        newAccount = ldapProv.getAccountByName(USER_NAME);
        Assert.assertNotSame("Changed Account and original", newAccount, account);
        String newDesc[] = newAccount.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in newDesc %s", Joiner.on(',').join(newDesc)),
                1, newDesc.length);
        Assert.assertEquals("new account description", "Pink", newDesc[0]);
        account = ldapProv.getAccountByName(USER_NAME);
        Assert.assertEquals("account get again after mod", account, newAccount);
        account = ldapProv.getAccountById(account.getId());
        Assert.assertEquals("account get again by ID after mod", account, newAccount);
        Cos acctCos = account.getCOS();
        acctCos.addDescription("Green");
        Thread.sleep(LC.ldap_cache_freshness_check_limit_ms.longValue() + 1);
        newAccount = ldapProv.getAccountByName(USER_NAME);
        Assert.assertNotSame("After Cos change Account and original should differ", newAccount, account);
        Domain acctDom = ldapProv.getDomain(newAccount);
        acctDom.addDescription("Gold");
        Thread.sleep(LC.ldap_cache_freshness_check_limit_ms.longValue() + 1);
        account = ldapProv.getAccountByName(USER_NAME);
        Assert.assertNotSame("After Domain change Account and original should differ", newAccount, account);
    }

    @Test
    public void cosTest() throws Exception {
        Cos cos = ldapProv.getCosByName(COS_NAME);
        Cos newCos = ldapProv.getCosByName(COS_NAME);
        Assert.assertEquals("cos with no mods", cos, newCos);
        String origDesc[] = cos.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in origDesc %s", Joiner.on(',').join(origDesc)),
                0, origDesc.length);
        cos.addDescription("Pink");
        Thread.sleep(LC.ldap_cache_freshness_check_limit_ms.longValue() + 1);
        newCos = ldapProv.getCosByName(COS_NAME);
        Assert.assertNotSame("Changed Cos and original", newCos, cos);
        String newDesc[] = newCos.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in newDesc %s", Joiner.on(',').join(newDesc)),
                1, newDesc.length);
        Assert.assertEquals("new COS description", "Pink", newDesc[0]);
        cos = ldapProv.getCosByName(COS_NAME);
        Assert.assertEquals("cos get again after mod", cos, newCos);
        cos = ldapProv.getCosById(cos.getId());
        Assert.assertEquals("cos get again by ID after mod", cos, newCos);
    }

    @Before
    public void setUp() throws Exception {
        ldapProv = (LdapProvisioning) Provisioning.getInstance();
        tearDown();
        Domain dom = ldapProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        Account acct = TestUtil.createAccount(USER_NAME);
        Cos acctCos = ldapProv.createCos("testldapcachecos", new HashMap<String, Object>());
        acct.setCOSId(acctCos.getId());
        acct.getDomainName();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
        Domain tDomain = ldapProv.getDomainByName(DOMAIN_NAME);
        if (tDomain != null) {
            ldapProv.deleteDomain(tDomain.getId());
        }
        Cos tCos = ldapProv.getCosByName(COS_NAME);
        if (tCos != null) {
            ldapProv.deleteCos(tCos.getId());
        }
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
