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
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.ExternalGroup;
import com.zimbra.cs.account.ldap.LdapCache;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.gal.ZimbraGalGroupHandler;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

public class TestLdapCache {

    private final int ITERATIONS = 1000;
    private static final String EXT_DOMAIN_NAME = "ldap.cache.extern";
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
        Assert.assertSame("account with no mods", account, newAccount);
        String origDesc[] = account.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in origDesc %s", Joiner.on(',').join(origDesc)),
                0, origDesc.length);
        account.addDescription("Pink");
        Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);

        newAccount = ldapProv.getAccountByName(USER_NAME);
        Assert.assertNotSame("Changed Account and original", newAccount, account);
        String newDesc[] = newAccount.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in newDesc %s", Joiner.on(',').join(newDesc)),
                1, newDesc.length);
        Assert.assertEquals("new account description", "Pink", newDesc[0]);
        account = ldapProv.getAccountByName(USER_NAME);
        Assert.assertSame("account get again after mod", account, newAccount);
        account = ldapProv.getAccountById(account.getId());
        Assert.assertSame("account get again by ID after mod", account, newAccount);
        Cos acctCos = account.getCOS();
        acctCos.addDescription("Green");
        Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
        newAccount = ldapProv.getAccountByName(USER_NAME);
        Assert.assertNotSame("After Cos change Account and original should differ", newAccount, account);
        Domain acctDom = ldapProv.getDomain(newAccount);
        acctDom.addDescription("Gold");
        Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
        account = ldapProv.getAccountByName(USER_NAME);
        Assert.assertNotSame("After Domain change Account and original should differ", newAccount, account);
    }

    @Test
    public void serverTest() throws Exception {
        String SERVER_NAME = "ldap.cache";
        String serverId = null;
        Config config = ldapProv.getConfig();
        int spamTagPercent = config.getSpamTagPercent();
        try {
            Server server = ldapProv.getServerByName(SERVER_NAME);
            if (server == null) {
                server = ldapProv.createServer(SERVER_NAME, new HashMap<String, Object>());
            }
            Assert.assertNotNull("Server object", server);
            serverId = server.getId();
            Server newServer = ldapProv.getServerByName(SERVER_NAME);
            Assert.assertSame("Server with no mods", server, newServer);
            String origDesc[] = server.getDescription();
            Assert.assertEquals(String.format("Number of descriptions in origDesc %s", Joiner.on(',').join(origDesc)),
                    0, origDesc.length);
            server.addDescription("Pink");
            Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
            newServer = ldapProv.getServerByName(SERVER_NAME);
            Assert.assertNotSame("Changed Server and original", newServer, server);
            String newDesc[] = newServer.getDescription();
            Assert.assertEquals(String.format("Number of descriptions in newDesc %s", Joiner.on(',').join(newDesc)),
                    1, newDesc.length);
            Assert.assertEquals("new server description", "Pink", newDesc[0]);
            server = ldapProv.getServerByName(SERVER_NAME);
            Assert.assertSame("server get again after mod", server, newServer);
            server = ldapProv.getServerById(newServer.getId());
            Assert.assertSame("server get again by ID after mod", server, newServer);
            config.setSpamTagPercent(spamTagPercent + 2);
            Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
            newServer = ldapProv.getServerByName(SERVER_NAME);
            Assert.assertNotSame("After Config change Server and original should differ", newServer, server);
        } finally {
            if (null != serverId) {
                ldapProv.deleteServer(serverId);
            }
            config.setSpamTagPercent(spamTagPercent);
        }
    }

    @Test
    public void domainTest() throws Exception {
        Config config = ldapProv.getConfig();
        int spamTagPercent = config.getSpamTagPercent();
        Domain domain = ldapProv.getDomainByName(DOMAIN_NAME);
        Assert.assertNotNull("Domain object", domain);
        String domDesc[] = domain.getDescription();
        try {
            Domain newDomain = ldapProv.getDomainByName(DOMAIN_NAME);
            Assert.assertSame("Domain with no mods", domain, newDomain);
            String origDesc[] = domain.getDescription();
            Assert.assertEquals(String.format("Number of descriptions in origDesc %s", Joiner.on(',').join(origDesc)),
                    0, origDesc.length);
            domain.addDescription("Pink");
            Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
            newDomain = ldapProv.getDomainByName(DOMAIN_NAME);
            Assert.assertNotSame("Changed Domain and original", newDomain, domain);
            String newDesc[] = newDomain.getDescription();
            Assert.assertEquals(String.format("Number of descriptions in newDesc %s", Joiner.on(',').join(newDesc)),
                    1, newDesc.length);
            Assert.assertEquals("new Domain description", "Pink", newDesc[0]);
            domain = ldapProv.getDomainByName(DOMAIN_NAME);
            Assert.assertSame("Domain get again after mod", domain, newDomain);
            domain = ldapProv.getDomainById(newDomain.getId());
            Assert.assertSame("Domain get again by ID after mod", domain, newDomain);
            config.setSpamTagPercent(spamTagPercent + 2);
            Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
            newDomain = ldapProv.getDomainByName(DOMAIN_NAME);
            Assert.assertNotSame("After Config change Domain and original should differ", newDomain, domain);
        } finally {
            config.setSpamTagPercent(spamTagPercent);
            domain.setDescription(domDesc);
        }
    }

    @Test
    public void cosTest() throws Exception {
        Cos cos = ldapProv.getCosByName(COS_NAME);
        Cos newCos = ldapProv.getCosByName(COS_NAME);
        Assert.assertSame("cos with no mods", cos, newCos);
        String origDesc[] = cos.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in origDesc %s", Joiner.on(',').join(origDesc)),
                0, origDesc.length);
        cos.addDescription("Pink");
        Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
        newCos = ldapProv.getCosByName(COS_NAME);
        Assert.assertNotSame("Changed Cos and original", newCos, cos);
        String newDesc[] = newCos.getDescription();
        Assert.assertEquals(String.format("Number of descriptions in newDesc %s", Joiner.on(',').join(newDesc)),
                1, newDesc.length);
        Assert.assertEquals("new COS description", "Pink", newDesc[0]);
        cos = ldapProv.getCosByName(COS_NAME);
        Assert.assertSame("cos get again after mod", cos, newCos);
        cos = ldapProv.getCosById(cos.getId());
        Assert.assertSame("cos get again by ID after mod", cos, newCos);
    }

    private static final String ext_machine = "example.com";
    private static final String ext_password = "changeme";
    private static final String ext_dl_name = "testdl@example.com";
    private static final String ext_group_grantee = String.format("%s:%S", EXT_DOMAIN_NAME, ext_dl_name);

    @Ignore("Relies on external Zimbra server - tailor above strings.") @Test
    public void externalGroupTest() throws Exception {
        Domain edom = ldapProv.createDomain(EXT_DOMAIN_NAME, new HashMap<String, Object>());
        edom.addAuthLdapURL(String.format("ldap://%s:389", ext_machine));
        edom.setAuthLdapBindDn("uid=zimbra,cn=admins,cn=zimbra");
        edom.setAuthLdapSearchBindDn("uid=zimbra,cn=admins,cn=zimbra");
        edom.setAuthLdapSearchBindPassword(ext_password);
        edom.setExternalGroupLdapSearchBase("");
        edom.setExternalGroupLdapSearchFilter(ZLdapFilterFactory.getInstance().groupByName("%n").toFilterString());
        edom.setExternalGroupHandlerClass(ZimbraGalGroupHandler.class.getName());
        edom.setAuthMech("zimbra");
        edom.setAuthMechAdmin("zimbra");
        ExternalGroup egroup = ExternalGroup.get(DomainBy.name, ext_group_grantee, false);
        Assert.assertNotNull("egroup initial get", egroup);
        ExternalGroup newEgroup = ExternalGroup.get(DomainBy.id, egroup.getId(), false);
        Assert.assertSame("ExternalGroup get again", newEgroup, egroup);
        edom.addDescription("Maroon");
        Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
        newEgroup = ExternalGroup.get(DomainBy.id, egroup.getId(), false);
        Assert.assertNotSame("ExternalGroup get again", newEgroup, egroup);
    }

    @Before
    public void setUp() throws Exception {
        ldapProv = (LdapProvisioning) Provisioning.getInstance();
        tearDown();
        ldapProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
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
        tDomain = ldapProv.getDomainByName(EXT_DOMAIN_NAME);
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
