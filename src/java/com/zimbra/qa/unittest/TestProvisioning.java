/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.*;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.ZimbraCustomAuth;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.custom.CustomLdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

public class TestProvisioning extends TestCase {

    // whether to test start TLS for external GAL and auth
    private static final boolean TEST_STARTTLS = false;

    private Provisioning mProv;
    LdapProvisioning mLdapProv;
    CustomProvTester mCustomProvTester;
    SoapProvisioning mSoapProv;

    private String TEST_ID;

    private String PRE_AUTH_KEY;
    private String PASSWORD;

    private String SPECIAL_CHARS_ALLOWED_IN_DOMAIN;
    private String SPECIAL_CHARS_ALLOWED_IN_USER_PART;

    private String COS_NAME;
    private String DOMAIN_NAME;
    private String DOMAIN_NAME_SPECIAL_CHARS;
    private String OTHER_DOMAIN_NAME;
    private String SERVER_NAME;
    private String ZIMLET_NAME;

    private String BASE_DN_PSEUDO_ATTR = "ldap.baseDN";

    private String NEW_NAME;  // for testing rename
    private String NEW_EMAIL; // for testing rename
    private String NEW_EMAIL_IN_OTHER_DOMAIN; // for testing rename to different domain

    private String DEFAULT_LDAP_ADMIN_USER;
    private String ADMIN_USER;
    private String ADMIN_EMAIL;
    private String ACCT_USER;
    private String ACCT_EMAIL;
    private String ACCT_USER_SPECIAL_CHARS;
    private String ACCT_EMAIL_SPECIAL_CHARS;
    private String ACCT_ALIAS_USER;
    private String ACCT_ALIAS_EMAIL;
    private String ACCT_ALIAS_AFTER_ACCOUNT_RENAME_TO_OTHER_DMAIN_EMAIL;
    private String ACCT_ALIAS_IN_OTHER_DOMAIN_USER;
    private String ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL;
    private String ACCT_ALIAS_IN_OTHER_DOMAIN_AFTER_ACCOUNT_RENAME_TO_ORIG_DOMAIN_EMAIL;
    private String ACCT_FULL_NAME;

    /*
     * for testing CustomLdapProvisioning where naming rdn attr
     * for account is configured
     */
    private String ACCT_NAMING_ATTR;
    private String ACCT_NAMING_ATTR_VALUE;
    private String ACCT_BASE_DN;  // for testing pseudo attr ldap.baseDN

    private String CR_USER;
    private String CR_EMAIL;
    private String CR_ALIAS_USER;
    private String CR_ALIAS_EMAIL;

    private String DL_USER;
    private String DL_EMAIL;
    private String DL_USER_SPECIAL_CHARS;
    private String DL_EMAIL_SPECIAL_CHARS;
    private String DL_ALIAS_USER;
    private String DL_ALIAS_EMAIL;
    private String DL_NESTED_USER;
    private String DL_NESTED_EMAIL;
    private String DL_NESTED_ALIAS_USER;
    private String DL_NESTED_ALIAS_EMAIL;

    private String DATA_SOURCE_NAME;
    private String IDENTITY_NAME;
    private String SIGNATURE_NAME;
    private String SIGNATURE_VALUE;
    private String SIGNATURE_VALUE_MODIFIED;

    public void setUp() throws Exception {
        mProv = Provisioning.getInstance();
        assertTrue(mProv instanceof LdapProvisioning);
        mLdapProv = (LdapProvisioning)mProv;
        mCustomProvTester = new CustomProvTester(mProv);

        mSoapProv = new SoapProvisioning();
        mSoapProv.soapSetURI(TestUtil.getAdminSoapUrl());
        mSoapProv.soapZimbraAdminAuthenticate();


        TEST_ID = TestProvisioningUtil.genTestId();

        PRE_AUTH_KEY = PreAuthKey.generateRandomPreAuthKey();
        PASSWORD = "test123";

        SPECIAL_CHARS_ALLOWED_IN_DOMAIN = "/";
        SPECIAL_CHARS_ALLOWED_IN_USER_PART = "/";

        COS_NAME = "cos-" + TEST_ID;
        // DOMAIN_NAME = "domain-" + TEST_ID + ".ldap-test-domain";
        // DOMAIN_NAME_SPECIAL_CHARS = "domain-" + SPECIAL_CHARS_ALLOWED_IN_DOMAIN + "-" + TEST_ID + ".ldap-test-domain";
        DOMAIN_NAME = TestProvisioningUtil.baseDomainName("domain", TEST_ID);
        DOMAIN_NAME_SPECIAL_CHARS = TestProvisioningUtil.baseDomainName("domain-" + SPECIAL_CHARS_ALLOWED_IN_DOMAIN, TEST_ID);


        OTHER_DOMAIN_NAME = "other-" + DOMAIN_NAME;
        SERVER_NAME = "server-" + TEST_ID;
        ZIMLET_NAME = "zimlet-" + TEST_ID;

        NEW_NAME = "newname";
        NEW_EMAIL = NEW_NAME + "-" + TEST_ID + "@" + DOMAIN_NAME;
        NEW_EMAIL_IN_OTHER_DOMAIN = NEW_NAME + "-" + TEST_ID + "@" + OTHER_DOMAIN_NAME;

        DEFAULT_LDAP_ADMIN_USER = LC.zimbra_ldap_user.value();
        ADMIN_USER =  "admin";
        ADMIN_EMAIL = ADMIN_USER + "@" + DOMAIN_NAME;
        ACCT_USER = "acct-1";
        ACCT_EMAIL = ACCT_USER + "@" + DOMAIN_NAME;
        ACCT_USER_SPECIAL_CHARS = "acct-special-chars-" + SPECIAL_CHARS_ALLOWED_IN_USER_PART;
        ACCT_EMAIL_SPECIAL_CHARS = ACCT_USER_SPECIAL_CHARS + "@" + DOMAIN_NAME_SPECIAL_CHARS;
        ACCT_ALIAS_USER = "alias-of-" + ACCT_USER;
        ACCT_ALIAS_EMAIL = ACCT_ALIAS_USER + "@" + DOMAIN_NAME;
        ACCT_ALIAS_AFTER_ACCOUNT_RENAME_TO_OTHER_DMAIN_EMAIL = ACCT_ALIAS_USER + "@" + OTHER_DOMAIN_NAME;
        ACCT_ALIAS_IN_OTHER_DOMAIN_USER = ACCT_ALIAS_USER + "-in-other-domain";
        ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL = ACCT_ALIAS_IN_OTHER_DOMAIN_USER + "@" + OTHER_DOMAIN_NAME;
        ACCT_ALIAS_IN_OTHER_DOMAIN_AFTER_ACCOUNT_RENAME_TO_ORIG_DOMAIN_EMAIL = ACCT_ALIAS_IN_OTHER_DOMAIN_USER + "@" + DOMAIN_NAME;
        ACCT_FULL_NAME = "Phoebe Shao";

        ACCT_NAMING_ATTR = LC.get("ldap_dit_naming_rdn_attr_user");
        if (StringUtil.isNullOrEmpty(ACCT_NAMING_ATTR))
            ACCT_NAMING_ATTR = "uid";
        ACCT_NAMING_ATTR_VALUE = namingAttrValue(ACCT_USER);

        ACCT_BASE_DN = "ou=grp1,ou=mail,o=Comcast";

        CR_USER = "cr-1";
        CR_EMAIL = CR_USER + "@" + DOMAIN_NAME;
        CR_ALIAS_USER = "alias-of-" + CR_USER;
        CR_ALIAS_EMAIL = CR_ALIAS_USER + "@" + DOMAIN_NAME;

        DL_USER = "dl-1";
        DL_EMAIL = DL_USER + "@" + DOMAIN_NAME;
        DL_USER_SPECIAL_CHARS = "dl-special-chars-" + SPECIAL_CHARS_ALLOWED_IN_USER_PART;
        DL_EMAIL_SPECIAL_CHARS = DL_USER_SPECIAL_CHARS + "@" + DOMAIN_NAME_SPECIAL_CHARS;
        DL_ALIAS_USER = "alias-of" + DL_USER;
        DL_ALIAS_EMAIL = DL_ALIAS_USER + "@" + DOMAIN_NAME;
        DL_NESTED_USER = "dl-nested";
        DL_NESTED_EMAIL = DL_NESTED_USER + "@" + DOMAIN_NAME;
        DL_NESTED_ALIAS_USER = "alias-of-" + DL_NESTED_USER;
        DL_NESTED_ALIAS_EMAIL = DL_NESTED_ALIAS_USER+ "@" + DOMAIN_NAME;

        DATA_SOURCE_NAME = "datasource-1";
        IDENTITY_NAME ="identity-1";
        SIGNATURE_NAME ="signature-1";

        SIGNATURE_VALUE = "this is my signature";
        SIGNATURE_VALUE_MODIFIED = "this is my signature MODIFIED";
    }

    static class Flag {
        private static Set<String> sNeedLdapPaging = new HashSet<String>();

        static {
            sNeedLdapPaging.add("getAllAdminAccounts");
            sNeedLdapPaging.add("getAllAccounts_domain");
            sNeedLdapPaging.add("getAllAccounts_domain_visitor");
            sNeedLdapPaging.add("getAllAccounts_domain_server_visitor");
            sNeedLdapPaging.add("getAllCalendarResources_domain");
            sNeedLdapPaging.add("getAllCalendarResources_domain_visitor");
            sNeedLdapPaging.add("getAllDistributionLists");
            sNeedLdapPaging.add("getDistributionLists_account");
            sNeedLdapPaging.add("getDistributionLists_account_directonly_via");
            sNeedLdapPaging.add("inDistributionList");  // com.zimbra.cs.mailbox.ACL.Grant.matches
            sNeedLdapPaging.add("searchAccounts");      // com.zimbra.cs.backup.BackupManager.getAccountsOnServer, com.zimbra.cs.service.admin.FixCalendarTimeZone.getAccountsOnServer
            sNeedLdapPaging.add("searchAccounts_domain");
            sNeedLdapPaging.add("searchCalendarResources");
            sNeedLdapPaging.add("searchCalendarResources_domain");  // com.zimbra.cs.service.account.SearchCalendarResources
            sNeedLdapPaging.add("searchDirectory");
        }

        static boolean needLdapPaging(String methodName) {
            /*
             * turn on for checking if a call would end up in
             * LdapProvisioning.searchObjects(String query, String returnAttrs[], String base, int flags, NamedEntry.Visitor visitor, int maxResults)
             *
             * to do this, add
             * throw ServiceException.INVALID_REQUEST("paging NOT SUPPORTED", null);
             * as the only line in searchObjects and comment out all the code in searchObjects.
             */
             // return sNeedLdapPaging.contains(methodName);  .searchObject
            return false;
        }
    }

    class CustomProvTester {
        Provisioning mProv;
        boolean mIsCustomProv;

        CustomProvTester(Provisioning prov) {
            mProv = prov;
            mIsCustomProv = (prov instanceof CustomLdapProvisioning);
        }

        /*
         * Custom provisiong test would've created accounts under a specific dn, not under the domain hierarchy
         * In cases where the previous test didn't finish to the end, entries would still be there and
         * will fail this test run.  We clean everything under the specific dn
         */
        public void cleanup() throws Exception {
            System.out.println("Cleanup...");

            Provisioning.SearchOptions options = new Provisioning.SearchOptions();
            int flags = 0;
            flags = (Provisioning.SA_ACCOUNT_FLAG |
                     // Provisioning.SA_ALIAS_FLAG |
                     Provisioning.SA_CALENDAR_RESOURCE_FLAG |
                     Provisioning.SA_DISTRIBUTION_LIST_FLAG);
            options.setFlags(flags);
            if (isCustom())
                options.setBase(ACCT_BASE_DN);
            List<NamedEntry> list = mProv.searchDirectory(options);

            for (NamedEntry entry : list) {
                if (entry instanceof CalendarResource)
                    mProv.deleteCalendarResource(entry.getId());
                else if (entry instanceof Alias)
                    mProv.removeAlias((Account)null, entry.getName());
                else if (entry instanceof Account)
                    mProv.deleteAccount(entry.getId());
                else if (entry instanceof DistributionList)
                    mProv.deleteDistributionList(entry.getId());
                else
                    throw new Exception("unexpected entry type: " + entry.getClass().getCanonicalName());
            }

            // search again, this time we search for everything and it should not contain any entry
            flags = 0xFF;
            options.setFlags(flags);
            list = mProv.searchDirectory(options);
            assertEquals(0, list.size());

        }

        public boolean isCustom() {
            return mIsCustomProv;
        }

        public void addAttr(Map<String, Object> attrs, String pseudoAttr, String value) {
            if (!mIsCustomProv)
                return;

            attrs.put(pseudoAttr, value);
        }

        private void verifyDn(Entry entry, String dn) throws Exception {
            if (!mIsCustomProv)
                return;

            assertEquals(dn, mLdapProv.getDN(entry));
        }

        public boolean verifyAccountCountForDomainBasedSearch() {
            if (!mIsCustomProv)
                return true;
            else
                return true;
        }

        public boolean verifyDLCountForDomainBasedSearch() {
            if (!mIsCustomProv)
                return true;
            else
                return false;
        }

        public boolean verifyAliasCountForDomainBasedSearch() {
            if (!mIsCustomProv)
                return true;
            else
                return false;
        }
    }


    private class TestVisitor implements NamedEntry.Visitor {

        List<NamedEntry> mVisited = new ArrayList<NamedEntry>();

        public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
            mVisited.add(entry);
        }

        public List<NamedEntry> visited() {
            return mVisited;
        }
    }

    /*
     * util functions
     */
    private List<NamedEntry> searchAccountsInDomain(Domain domain) throws ServiceException {
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();

        int flags = 0;
        flags = Provisioning.SA_ACCOUNT_FLAG;
        options.setFlags(flags);
        options.setDomain(domain);
        return mProv.searchDirectory(options);
    }

    private List<NamedEntry> searchAliasesInDomain(Domain domain) throws ServiceException {
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();

        int flags = 0;
        flags = Provisioning.SA_ALIAS_FLAG;
        options.setFlags(flags);
        options.setDomain(domain);
        return mProv.searchDirectory(options);
    }

    public static class TestCustomAuth extends ZimbraCustomAuth {

        Account mTheOnlyAcctThatCanAuth;
        String  mTheOnlyPasswordIKnowAbout;

        TestCustomAuth(Account account, String password) {
            mTheOnlyAcctThatCanAuth = account;
            mTheOnlyPasswordIKnowAbout = password;
        }

        private boolean verifyArgs(List<String> args) {
            if (args.size() == 6 &&
                args.get(0).equals("http://blah.com:123") &&
                args.get(1).equals("green") &&
                args.get(2).equals(" ocean blue   ") &&
                args.get(3).equals("") &&
                args.get(4).equals("yelllow") &&
                args.get(5).equals(""))
                return true;
            else
                return false;
        }

        public void authenticate(Account acct, String password, Map<String, Object> context, List<String> args) throws Exception {
            String acOrigClientIp = (String)context.get(AuthContext.AC_ORIGINATING_CLIENT_IP);
            String acNamePassedIn = (String)context.get(AuthContext.AC_ACCOUNT_NAME_PASSEDIN);
            AuthContext.Protocol acProto = (AuthContext.Protocol)context.get(AuthContext.AC_PROTOCOL);


            if (acct.getName().equals(mTheOnlyAcctThatCanAuth.getName()) &&
                password.equals(mTheOnlyPasswordIKnowAbout) &&
                verifyArgs(args))
                return;
            else
                throw new Exception("auth failed by TestCustomAuth for " + acct.getName() + " password " + password);
        }
    }


    private void setDefaultDomain(String domain) throws Exception {
        Map<String, Object> confAttrs = new HashMap<String, Object>();
        confAttrs.put(Provisioning.A_zimbraDefaultDomainName, domain);
        mProv.modifyAttrs(mProv.getConfig(), confAttrs, true);
    }

    private String namingAttrValue(String name) {
        return name + ".mailuser";
    }

    /* =====================
     * end of util functions
     * =====================
     */


    private void healthTest() throws Exception {
        System.out.println("Testing health");

        mProv.healthCheck();
    }

    private Config configTest() throws Exception {
        System.out.println("Testing config");

        Config entry = mProv.getConfig();
        assertNotNull(entry != null);

        return entry;
    }

    private String cosTest() throws Exception {
        System.out.println("Testing cos");

        Cos entry = mProv.createCos(COS_NAME, new HashMap<String, Object>());

        Cos entryGot = mProv.get(Provisioning.CosBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.CosBy.name, COS_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        Cos defaultCos = mProv.get(Provisioning.CosBy.name, "default");
        assertNotNull(defaultCos);

        String destCosName = "cos2-" + TEST_ID;
        Cos destCos = mProv.copyCos(defaultCos.getId(), destCosName);

        List list = mProv.getAllCos();
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{defaultCos, entry, destCos}, false);

        mProv.renameCos(entry.getId(), NEW_NAME);
        mProv.renameCos(entry.getId(), COS_NAME);

        return entry.getName();
    }

    private String[] domainTest() throws Exception {
        System.out.println("Testing domain");

        int numVirtualHosts = 500;

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, PRE_AUTH_KEY);
        // test lots of zimbraVirtualHostname on a domain
        Set<String> virtualHosts = new HashSet<String>();
        for (int i=0; i<numVirtualHosts; i++) {
            String virtualHostName = "vhost-" + i + "-" + TEST_ID + ".com";
            virtualHosts.add(virtualHostName);
        }
        attrs.put(Provisioning.A_zimbraVirtualHostname, virtualHosts.toArray(new String[0]));
        Domain entry = mProv.createDomain(DOMAIN_NAME, attrs);

        Domain entryGot = mProv.get(Provisioning.DomainBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        for (int i=0; i<numVirtualHosts; i++) {
            String virtualHostName = "vhost-" + i + "-" + TEST_ID + ".com";
            entryGot = mProv.get(Provisioning.DomainBy.virtualHostname, virtualHostName);
            TestProvisioningUtil.verifySameEntry(entry, entryGot);
        }

        Domain otherEntry = mProv.createDomain(OTHER_DOMAIN_NAME, attrs);
        Domain specialCharEntry = mProv.createDomain(DOMAIN_NAME_SPECIAL_CHARS, attrs);

        List list = mProv.getAllDomains();
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry, otherEntry, specialCharEntry}, false);

        // set our domain as the default domain
        setDefaultDomain(DOMAIN_NAME);

        return new String[]{entry.getName(), otherEntry.getName(), specialCharEntry.getName()};
    }

    private void mimeTest() throws Exception {
        System.out.println("Testing mime");

        List<MimeTypeInfo> mime = mProv.getMimeTypes("all");
        assertEquals(1, mime.size());
    }

    private Server serverTest() throws Exception {
        System.out.println("Testing server");

        Map<String, Object> serverAttrs = new HashMap<String, Object>();
        Server entry = mProv.createServer(SERVER_NAME, serverAttrs);

        Server entryGot = mProv.get(Provisioning.ServerBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.ServerBy.name, SERVER_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // modify server
        /*
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, "blah");
        mProv.modifyAttrs(entry, attrs);
        */

        Server localServer = mProv.getLocalServer();
        assertNotNull(localServer);

        List list = mProv.getAllServers();
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{localServer, entry}, false);

        list = mProv.getAllServers(Provisioning.SERVICE_MAILBOX);
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{localServer}, false);

        return entry;
    }

    private Zimlet zimletTest() throws Exception {
        System.out.println("Testing zimlet");

        Map<String, Object> zimletAttrs = new HashMap<String, Object>();
        zimletAttrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
        Zimlet entry = mProv.createZimlet(ZIMLET_NAME, zimletAttrs);

        Zimlet entryGot = mProv.getZimlet(ZIMLET_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        /* weird, failed with OpenLDAP 2.4, subtree search returned only one zimlet
        List list = mProv.getObjectTypes();
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry}, false);

        list = mProv.listAllZimlets();
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry}, false);
        */

        return entry;
    }

    private void externalAuthTest(Account account, boolean startTLS) throws Exception {
        Domain domain = mProv.getDomain(account);
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_LDAP);
        attrsToMod.put(Provisioning.A_zimbraAuthLdapURL, "ldap://" + LC.zimbra_server_hostname.value() + ":389");
        attrsToMod.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
        attrsToMod.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrsToMod.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());

        if (startTLS)
            attrsToMod.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, "TRUE");

        mProv.modifyAttrs(domain, attrsToMod, true);
        mProv.authAccount(account, PASSWORD, AuthContext.Protocol.test);
    }

    private void authTest(Account account) throws Exception  {
        System.out.println("Testing auth");

        // zimbra auth
        mProv.authAccount(account, PASSWORD, AuthContext.Protocol.test);

        // external ldap auth, test using our own ldap
        externalAuthTest(account, false);
        if (TEST_STARTTLS)
            externalAuthTest(account, true);

        Domain domain = mProv.getDomain(account);
        Map attrsToMod = new HashMap<String, Object>();


        // ad auth
        // need an AD, can't test

        // kerberos5 auth
        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_KERBEROS5);
        attrsToMod.put(Provisioning.A_zimbraAuthKerberos5Realm, "PHOEBE.LOCAL");
        mProv.modifyAttrs(domain, attrsToMod, true);
        // by domain realm mapping    acct-1@PHOEBE.LOCAL has to be created (sudo /usr/local/sbin/kadmin.local addprinc command)
        // mProv.authAccount(account, PASSWORD, "unittest"); uncomment after krb5 server is fixed
        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_zimbraForeignPrincipal, "kerberos5:user1@PHOEBE.LOCAL");
        mProv.modifyAttrs(account, attrsToMod, true);
        // by specific foreignPrincipal   user1-1@PHOEBE.LOCAL has to be created (sudo /usr/local/sbin/kadmin.local addprinc command)
        // mProv.authAccount(account, PASSWORD, "unittest");  uncomment after krb5 server is fixed

        // skip these tests, as there could be multiple domain with PHOEBE.LOCAL in zimbraAuthKerberos5Realm  from previous test
        // to test, remove all previous test domains and uncomment the following.
        /*
        Account acctNonExist = mProv.get(Provisioning.AccountBy.krb5Principal, "bad@PHOEBE.LOCAL");
        assertNull(acctNonExist);

        Account acctByRealm = mProv.get(Provisioning.AccountBy.krb5Principal, ACCT_USER+"@PHOEBE.LOCAL");
        assertNotNull(acctByRealm);
        assertEquals(acctByRealm.getName(), ACCT_EMAIL);

        Account acctByFP = mProv.get(Provisioning.AccountBy.krb5Principal, "user1@PHOEBE.LOCAL");
        assertNotNull(acctByFP);
        assertEquals(acctByFP.getName(), ACCT_EMAIL);
        */

        // custom auth
        attrsToMod.clear();
        String customAuthHandlerName = "test";
        String args = "http://blah.com:123    green \" ocean blue   \"  \"\" yelllow \"\"";
        attrsToMod.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_CUSTOM + customAuthHandlerName + " " + args);
        mProv.modifyAttrs(domain, attrsToMod, true);
        ZimbraCustomAuth.register(customAuthHandlerName, new TestCustomAuth(account, PASSWORD));
        mProv.authAccount(account, PASSWORD, AuthContext.Protocol.test);

        // try an auth failure
        try {
            mProv.authAccount(account, PASSWORD + "-not", AuthContext.Protocol.test);
            fail("AccountServiceException.AUTH_FAILED not thrown"); // should not come to here
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.AUTH_FAILED, e.getCode());
        }

        // done testing auth mech, set auth meth back
        attrsToMod.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_ZIMBRA);
        mProv.modifyAttrs(domain, attrsToMod, true);

        // preauth
        HashMap<String,String> params = new HashMap<String,String>();
        String authBy = "name";
        long timestamp = System.currentTimeMillis();
        long expires = 0;
        params.put("account", ACCT_EMAIL);
        params.put("by", authBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        String preAuth = PreAuthKey.computePreAuth(params, PRE_AUTH_KEY);
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, "127.0.0.1");
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, ACCT_EMAIL);
        mProv.preAuthAccount(account,
                            ACCT_EMAIL, // account name
                            authBy,     // by
                            timestamp,  // timestamp
                            0,          // expires
                            preAuth,    // preauth key
                            authCtxt);

    }

    private Account adminAccountTest() throws Exception {
        System.out.println("Testing admin account");

        // LDAP bind admin user
        Account entry = mProv.get(Provisioning.AccountBy.adminName, DEFAULT_LDAP_ADMIN_USER);
        assertNotNull(entry);

        /*
         * for default provisioning, it will use the local part for the name so the
         * below assertion will pass.
         *
         * for custom provisionig, it appends the defaultdomain to form the address so the
         * below assertion will fail, which is fine.
         */
        // assertEquals(DEFAULT_LDAP_ADMIN_USER, entry.getName());

        // create an admin account
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(acctAttrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(acctAttrs, ACCT_NAMING_ATTR, namingAttrValue(ADMIN_USER));
        acctAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        entry = mProv.createAccount(ADMIN_EMAIL, PASSWORD, acctAttrs);

        Account entryGot = mProv.get(Provisioning.AccountBy.name, ADMIN_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        /*
         * admin can be retrieved without the domain, default domain will be used if domain is not supplied
         */
        Account entryGotByuser = mProv.get(Provisioning.AccountBy.name, ADMIN_USER);
        TestProvisioningUtil.verifySameEntry(entryGot, entryGotByuser);

        if (!Flag.needLdapPaging("getAllAdminAccounts")) {
            List list = mProv.getAllAdminAccounts();
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry}, false);
        }

        return entry;
    }


    // account and account aliases
    private Account[] accountTest(Account adminAcct, Cos cos, Domain domain, Domain otherDomain) throws Exception {
        System.out.println("Testing account");

        String krb5Principal1 = "fp1@FOO.COM";
        String krb5Principal2 = "fp2@BAR.COM";

        // create an account
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(acctAttrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(acctAttrs, ACCT_NAMING_ATTR, ACCT_NAMING_ATTR_VALUE);
        acctAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        acctAttrs.put(Provisioning.A_zimbraForeignPrincipal, new String[]{"kerberos5:"+krb5Principal1,"kerberos5:"+krb5Principal2});
        acctAttrs.put(Provisioning.A_zimbraPrefFromAddress, ACCT_EMAIL);
        Account entry = mProv.createAccount(ACCT_EMAIL, PASSWORD, acctAttrs);
        String entryId = entry.getId();
        String acctDn = ACCT_NAMING_ATTR + "=" + ACCT_NAMING_ATTR_VALUE + "," + ACCT_BASE_DN;
        mCustomProvTester.verifyDn(entry, acctDn);

        // create an account with special chars
        Map<String, Object> acctAttrsSpecialChars = new HashMap<String, Object>();
        mCustomProvTester.addAttr(acctAttrsSpecialChars, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(acctAttrsSpecialChars, ACCT_NAMING_ATTR, namingAttrValue(ACCT_USER_SPECIAL_CHARS));
        acctAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        Account entrySpecialChars = mProv.createAccount(ACCT_EMAIL_SPECIAL_CHARS, PASSWORD, acctAttrsSpecialChars);
        String acctSpecialCharsDn = ACCT_NAMING_ATTR + "=" + namingAttrValue(ACCT_USER_SPECIAL_CHARS) + "," + ACCT_BASE_DN;
        mCustomProvTester.verifyDn(entrySpecialChars, acctSpecialCharsDn);
        Account entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL_SPECIAL_CHARS);
        TestProvisioningUtil.verifySameEntry(entrySpecialChars, entryGot);

        // add an alias in the same domain
        mProv.addAlias(entry, ACCT_ALIAS_EMAIL);

        // add an alias in a different doamin
        boolean correct = false;
        try {
            mProv.addAlias(entry, ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL);
            correct = true;
        } catch (ServiceException e) {
            if (mCustomProvTester.isCustom())
                correct = true;
        }
        assertTrue(correct);

        // get account by id
        entryGot = mProv.get(Provisioning.AccountBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // get account by name(i.e. email)
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // get account by alias
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_ALIAS_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // get account by alias
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL);
        if (mCustomProvTester.isCustom())
            assertEquals(null, entryGot);
        else
            TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // get account by krb5Principal using account foreign principal
        // account has multiple kerberos principals
        entryGot = mProv.get(Provisioning.AccountBy.krb5Principal, krb5Principal1);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.AccountBy.krb5Principal, krb5Principal2);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // test dup zFP on multiple accounts - should fail
        String krb5PrincipalDup = "fp-dup@FOO.COM";
        acctAttrs.clear();
        acctAttrs.put(Provisioning.A_zimbraForeignPrincipal, new String[]{"kerberos5:"+krb5PrincipalDup});
        Account acctX = mProv.createAccount("acctx-dup-kerberos@" + DOMAIN_NAME, "test123", acctAttrs);
        Account acctY = mProv.createAccount("accty-dup-kerberos@" + DOMAIN_NAME, "test123", acctAttrs);
        try {
            mProv.get(Provisioning.AccountBy.krb5Principal, krb5PrincipalDup);
            fail();
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.MULTIPLE_ACCOUNTS_MATCHED, e.getCode());
        }

        // get account by krb5Principal using domain realm mapping
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        domainAttrs.put(Provisioning.A_zimbraAuthMech, "kerberos5"); // not necessary, put it to make sure if it is present things should still work
        domainAttrs.put(Provisioning.A_zimbraAuthKerberos5Realm, "JUNKREALM.COM");
        String krb5DomainName = "krb-test." + DOMAIN_NAME;
        Domain krb5TestDomain = mProv.createDomain(krb5DomainName, domainAttrs);
        Account krb5TestAcct = mProv.createAccount("user1@"+krb5DomainName, "test123", null);
        entryGot = mProv.get(Provisioning.AccountBy.krb5Principal, "user1@JUNKREALM.COM");
        TestProvisioningUtil.verifySameEntry(krb5TestAcct, entryGot);




        List list = null;

        // get all accounts in a domain
        if (!Flag.needLdapPaging("getAllAccounts_domain")) {
            list = mProv.getAllAccounts(domain);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry, adminAcct, acctX, acctY},
                                               mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        }

        // get all accounts in a domain, visitor version
        if (!Flag.needLdapPaging("getAllAccounts_domain_visitor")) {
            TestVisitor visitor = new TestVisitor();
            mProv.getAllAccounts(domain, visitor);
            TestProvisioningUtil.verifyEntries(visitor.visited(), new NamedEntry[]{entry, adminAcct, acctX, acctY},
                                               mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        }

        // get all accounts in a domain and on a server, visitor version
        if (!Flag.needLdapPaging("getAllAccounts_domain_server_visitor")) {
            TestVisitor visitor = new TestVisitor();
            Server server = mProv.getLocalServer();
            mProv.getAllAccounts(domain, server, visitor);
            TestProvisioningUtil.verifyEntries(visitor.visited(), new NamedEntry[]{entry, adminAcct, acctX, acctY},
                                               mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        }

        // modify account status
        mProv.modifyAccountStatus(entry, "maintenance");
        mProv.modifyAccountStatus(entry, "active");

        /*
         * rename account, same domain
         */
        mProv.renameAccount(entryId, NEW_EMAIL);
        if (!Flag.needLdapPaging("searchDirectory")) {
            // make sure the account is still in the same domain
            list = searchAccountsInDomain(domain);
            TestProvisioningUtil.verifyEntriesById(list, new String[]{entryId}, false);
            TestProvisioningUtil.verifyEntriesByName(list, new String[]{NEW_EMAIL}, false);
            // re-get the entry since it might've been changed after the rename
            entry = mProv.get(Provisioning.AccountBy.id, entryId);
            assertEquals(NEW_EMAIL,
                    entry.getAttr(Provisioning.A_zimbraPrefFromAddress));
            if (mCustomProvTester.isCustom()) {
                // make sure it is still in the same dn
                mCustomProvTester.verifyDn(entry, acctDn);
                // make sure both aliases are not moved or changed
                // actually the following just verifies that they are not changed,
                // for now can't verify if they are moved unless we improve the test
                list = searchAliasesInDomain(domain);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL}, mCustomProvTester.verifyAliasCountForDomainBasedSearch());
            } else {
                // make sure the alias is still in the same domain
                list = searchAliasesInDomain(domain);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL}, true);
                // make sure the alias in the other domain is still in the other domain
                list = searchAliasesInDomain(otherDomain);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL}, true);
            }
        }

        /*
         * rename account, different domain
         */
        correct = false;
        try {
            mProv.renameAccount(entryId, NEW_EMAIL_IN_OTHER_DOMAIN);
            correct = true;
        } catch (ServiceException e) {
            if (mCustomProvTester.isCustom())
                correct = true;
        }
        assertTrue(correct);
        // re-get the entry since it might've been changed after the rename
        entry = mProv.get(Provisioning.AccountBy.id, entryId);

        if (!mCustomProvTester.isCustom()) {
            if (!Flag.needLdapPaging("searchDirectory")) {
                // make sure the account is now in the other domain
                list = searchAccountsInDomain(otherDomain);
                TestProvisioningUtil.verifyEntriesById(list, new String[]{entryId}, true);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{NEW_EMAIL_IN_OTHER_DOMAIN}, true);
                // make sure the alias is now moved to the other domain and there shouldn't be any let in the old domain
                list = searchAliasesInDomain(domain);
                assertEquals(0, list.size());
                // make sure both aliases are now in the other doamin
                list = searchAliasesInDomain(otherDomain);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_ALIAS_AFTER_ACCOUNT_RENAME_TO_OTHER_DMAIN_EMAIL,
                                    ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL}, true);
            }
        }

        /*
         * rename it back
         */
        mProv.renameAccount(entryId, ACCT_EMAIL);
        if (!Flag.needLdapPaging("searchDirectory")) {
            // make sure the account is moved back to the orig domain
            list = searchAccountsInDomain(domain);
            TestProvisioningUtil.verifyEntriesById(list, new String[]{entryId}, false);
            TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_EMAIL}, false);
            // re-get the entry since it might've been changed after the rename
            entry = mProv.get(Provisioning.AccountBy.id, entryId);

            if (mCustomProvTester.isCustom()) {
                list = searchAliasesInDomain(domain);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL}, mCustomProvTester.verifyAliasCountForDomainBasedSearch());

            } else {
                // now, both aliases should be moved to the orig domain
                list = searchAliasesInDomain(otherDomain);
                assertEquals(0, list.size());
                list = searchAliasesInDomain(domain);
                TestProvisioningUtil.verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL,
                                    ACCT_ALIAS_IN_OTHER_DOMAIN_AFTER_ACCOUNT_RENAME_TO_ORIG_DOMAIN_EMAIL}, true);
            }
        }


        // remove alias
        mProv.removeAlias(entry, ACCT_ALIAS_EMAIL);

        if (!mCustomProvTester.isCustom()) {
            mProv.removeAlias(entry, ACCT_ALIAS_IN_OTHER_DOMAIN_AFTER_ACCOUNT_RENAME_TO_ORIG_DOMAIN_EMAIL);
        }
        if (!Flag.needLdapPaging("searchDirectory")) {
            /*
             * Note, for custon DIT, if this fails, delete the voice alias in grp0.
             * TODO, fix test.
             */

            // verify it
            list = searchAliasesInDomain(domain);
            assertEquals(0, list.size());
            list = searchAliasesInDomain(otherDomain);
            assertEquals(0, list.size());
        }

        // set cos
        entry = mProv.get(Provisioning.AccountBy.id, entryId);
        mProv.setCOS(entry, cos);

        return new Account[]{entry, entrySpecialChars, acctX, acctY};
    }

    private void passwordTest(Account account) throws Exception {
        System.out.println("Testing password");

        mProv.changePassword(account, PASSWORD, PASSWORD);
        mProv.checkPasswordStrength(account, PASSWORD);
        mProv.setPassword(account, PASSWORD);
    }

    private void doLocaleTest(Account acct, String locale) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefLocale, locale);
        mSoapProv.modifyAttrs(acct, attrs, true);

        String provLocale = mSoapProv.getLocale(acct).toString();
        assertEquals(locale, provLocale);

        String entryLocale = acct.getLocale().toString();
        assertEquals(locale, entryLocale);
    }

    private void localeTest() throws Exception {
        System.out.println("Testing locale"); // bug 23218: entry.getLocale() is not refreshed on modifying locale (zimbraPrefLocale/zimbraLocale)

        Account acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        assertNotNull(acct);

        doLocaleTest(acct, "xxx");
        doLocaleTest(acct, "yyy");
        doLocaleTest(acct, "zzz");
    }

    // calendar resource and calendar resource alias
    private CalendarResource calendarResourceTest(Cos cos, Domain domain) throws Exception {
        System.out.println("Testing calendar resource");

        Map<String, Object> crAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(crAttrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(crAttrs, ACCT_NAMING_ATTR, namingAttrValue(CR_USER));
        crAttrs.put(Provisioning.A_displayName, CR_USER);
        crAttrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        crAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        CalendarResource entry = mProv.createCalendarResource(CR_EMAIL, PASSWORD, crAttrs);
        mProv.addAlias(entry, CR_ALIAS_EMAIL);

        CalendarResource entryGot = mProv.get(Provisioning.CalendarResourceBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.CalendarResourceBy.name, CR_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.CalendarResourceBy.name, CR_ALIAS_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        List list = null;

        if (!Flag.needLdapPaging("getAllCalendarResources_domain")) {
            list = mProv.getAllCalendarResources(domain);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry}, mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        }

        if (!Flag.needLdapPaging("getAllCalendarResources_domain_visitor")) {
            TestVisitor visitor = new TestVisitor();
            mProv.getAllCalendarResources(domain, visitor);
            TestProvisioningUtil.verifyEntries(visitor.visited(), new NamedEntry[]{entry}, mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        }

        mProv.renameCalendarResource(entry.getId(), NEW_EMAIL);
        mProv.renameCalendarResource(entry.getId(), CR_EMAIL);

        return entry;
    }

    // distribution list and distribution list alias
    private DistributionList[] distributionListTest(Domain domain) throws Exception {
        System.out.println("Testing distribution list");

        Map<String, Object> dlAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(dlAttrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(dlAttrs, ACCT_NAMING_ATTR, namingAttrValue(DL_USER));
        DistributionList entry = mProv.createDistributionList(DL_EMAIL, dlAttrs);

        mProv.addAlias(entry, DL_ALIAS_EMAIL);

        DistributionList entryGot = mProv.get(Provisioning.DistributionListBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_ALIAS_EMAIL);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        Map<String, Object> dlNestedAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(dlNestedAttrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(dlNestedAttrs, ACCT_NAMING_ATTR, namingAttrValue(DL_NESTED_USER));
        DistributionList dlNested = mProv.createDistributionList(DL_NESTED_EMAIL, dlNestedAttrs);
        mProv.addAlias(dlNested, DL_NESTED_ALIAS_EMAIL);

        /*
         * dl with special chars
         */
        // first, temporarily set the default domain to the special char domain if we are
        // custom DIT.
        String curDefaultDomain = mProv.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName);
        if (mCustomProvTester.isCustom())
            setDefaultDomain(DOMAIN_NAME_SPECIAL_CHARS);
        Map<String, Object> dlAttrsSpecialChars = new HashMap<String, Object>();
        mCustomProvTester.addAttr(dlAttrsSpecialChars, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(dlAttrsSpecialChars, ACCT_NAMING_ATTR, namingAttrValue(DL_USER_SPECIAL_CHARS));
        DistributionList dlSpecilaChars = mProv.createDistributionList(DL_EMAIL_SPECIAL_CHARS, dlAttrsSpecialChars);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_EMAIL_SPECIAL_CHARS);
        TestProvisioningUtil.verifySameEntry(dlSpecilaChars, entryGot);
        // set back the orig default domain
        if (mCustomProvTester.isCustom())
            setDefaultDomain(curDefaultDomain);

        mProv.addMembers(entry, new String[]{DL_NESTED_EMAIL});
        mProv.addMembers(dlNested, new String[]{ACCT_EMAIL});

        List list = null;

        if (!Flag.needLdapPaging("getAllDistributionLists")) {
            list = mProv.getAllDistributionLists(domain);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry, dlNested}, mCustomProvTester.verifyDLCountForDomainBasedSearch());
        }

        Account account = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        Set<String> set = null;

        if (!Flag.needLdapPaging("getDistributionLists_account")) {
            set = mProv.getDistributionLists(account);
            assertEquals(2, set.size());
            assertTrue(set.contains(entry.getId()));
            assertTrue(set.contains(dlNested.getId()));
        }

        if (!Flag.needLdapPaging("getDistributionLists_account_directonly_via")) {
            Map<String, String> via = new HashMap<String, String>();
            list = mProv.getDistributionLists(account, false, via);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry, dlNested}, true);
            assertEquals(1, via.size());
            assertEquals(dlNested.getName(), via.get(entry.getName()));
        }

        if (!Flag.needLdapPaging("getDistributionLists_account_directonly_via")) {
            list = mProv.getDistributionLists(account, true, null);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{dlNested}, true);
        }

        if (!Flag.needLdapPaging("inDistributionList")) {
            boolean inList = mProv.inDistributionList(account, entry.getId());
            assertTrue(inList);
        }

        mProv.removeAlias(entry, DL_ALIAS_EMAIL);

        mProv.removeMembers(entry, new String[]{dlNested.getName()});

        mProv.renameDistributionList(entry.getId(), NEW_EMAIL);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, NEW_EMAIL);
        TestProvisioningUtil.verifySameId(entry, entryGot);
        if (!Flag.needLdapPaging("getAllDistributionLists")) {
            list = mProv.getAllDistributionLists(domain);
            TestProvisioningUtil.verifyEntriesByName(list, new String[]{NEW_EMAIL, DL_NESTED_EMAIL}, mCustomProvTester.verifyDLCountForDomainBasedSearch());
        }

        // refresh the entry, it might have been moved
        entry = mProv.get(Provisioning.DistributionListBy.name, NEW_EMAIL);

        // rename it back
        mProv.renameDistributionList(entry.getId(), DL_EMAIL);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_EMAIL);
        TestProvisioningUtil.verifySameId(entry, entryGot);
        if (!Flag.needLdapPaging("getAllDistributionLists")) {
            list = mProv.getAllDistributionLists(domain);
            TestProvisioningUtil.verifyEntriesByName(list, new String[]{DL_EMAIL, DL_NESTED_EMAIL}, mCustomProvTester.verifyDLCountForDomainBasedSearch());
        }

        return new DistributionList[]{entry, dlNested, dlSpecilaChars};
    }

    private DataSource dataSourceTest(Account account) throws Exception {
        System.out.println("Testing data source");

        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, "inbox");
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, "pop.google.com");
        dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "TRUE");
        dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, PASSWORD);
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, "mickymouse");
        dsAttrs.put(Provisioning.A_zimbraDataSourceEmailAddress, "micky@google.com");
        dsAttrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, LdapUtil.generateUUID()); // just some random id, not used anywhere
        dsAttrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
        dsAttrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
        dsAttrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");

        DataSource entry = mProv.createDataSource(account, DataSource.Type.pop3, DATA_SOURCE_NAME, dsAttrs);

        DataSource entryGot = mProv.get(account, Provisioning.DataSourceBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(account, Provisioning.DataSourceBy.name, DATA_SOURCE_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        List list = mProv.getAllDataSources(account);
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry}, true);

        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraDataSourcePollingInterval, "100");
        mProv.modifyDataSource(account, entry.getId(), attrsToMod);

        return entry;
    }

    private Identity identityTest(Account account) throws Exception {
        System.out.println("Testing identity");

        Map<String, Object> identityAttrs = new HashMap<String, Object>();
        identityAttrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, LdapUtil.generateUUID());  // just some random id, not used anywhere
        identityAttrs.put(Provisioning.A_zimbraPrefFromAddress, "micky.mouse@zimbra,com");
        identityAttrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
        identityAttrs.put(Provisioning.A_zimbraPrefReplyToEnabled, "TRUE");
        identityAttrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
        identityAttrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
        Identity entry = mProv.createIdentity(account, IDENTITY_NAME, identityAttrs);

        Identity entryGot = mProv.get(account, Provisioning.IdentityBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        entryGot = mProv.get(account, Provisioning.IdentityBy.name, IDENTITY_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);
        Identity defaultIdentity = mProv.get(account, Provisioning.IdentityBy.name, Provisioning.DEFAULT_IDENTITY_NAME);
        TestProvisioningUtil.verifySameId(account, defaultIdentity);
        assertEquals(Provisioning.DEFAULT_IDENTITY_NAME, defaultIdentity.getName());

        List list = mProv.getAllIdentities(account);
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{defaultIdentity, entry}, true);

        // modify
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraPrefReplyToDisplay, "MM");
        mProv.modifyIdentity(account, IDENTITY_NAME, attrsToMod);

        // rename
        String newName = "identity-new-name";
        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_zimbraPrefIdentityName, newName);
        mProv.modifyIdentity(account, IDENTITY_NAME, attrsToMod);

        // get by new name
        entryGot = mProv.get(account, Provisioning.IdentityBy.name, newName);
        entry = mProv.get(account, Provisioning.IdentityBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // rename back
        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_zimbraPrefIdentityName, IDENTITY_NAME);
        mProv.modifyIdentity(account, newName, attrsToMod);

        // refresh the entry to return
        entry = mProv.get(account, Provisioning.IdentityBy.id, entry.getId());

        return entry;
    }

    private void signatureTest(Account account) throws Exception {
        System.out.println("Testing signature");

        // create a signature
        Map<String, Object> signatureAttrs = new HashMap<String, Object>();
        signatureAttrs.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_VALUE);
        Signature entry = mProv.createSignature(account, SIGNATURE_NAME, signatureAttrs);

        // get the signature by id
        Signature entryGot = mProv.get(account, Provisioning.SignatureBy.id, entry.getId());
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // get the signature by name
        entryGot = mProv.get(account, Provisioning.SignatureBy.name, SIGNATURE_NAME);
        TestProvisioningUtil.verifySameEntry(entry, entryGot);

        // get all the signatures, there should be only one - the one we just created
        List list = mProv.getAllSignatures(account);
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{entry}, true);

        // since this is the only signature, it should be automatically set as the default signature of the account
        String defaultSigId = account.getAttr(Provisioning.A_zimbraPrefDefaultSignatureId);
        assertEquals(entry.getId(), defaultSigId);

        // modify the signature value
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_VALUE_MODIFIED);
        mProv.modifySignature(account, entry.getId(), attrsToMod);

        // make sure we get the modified value back
        entryGot = mProv.get(account, Provisioning.SignatureBy.id, entry.getId());
        assertEquals(SIGNATURE_VALUE_MODIFIED, entryGot.getAttr(Provisioning.A_zimbraPrefMailSignature));

        /*
        // try to delete the signature, since it is the default signature (because it is the only one)
        // it should not be allowed
        // -- the above is no longer true, we now allow deleting default signature
        try {
            mProv.deleteSignature(account, SIGNATURE_NAME);
            fail("ServiceException.INVALID_REQUEST not thrown"); // should not come to here
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
        */


        // create a second signature and set it as the default
        String secondSigName = "second-sig";
        signatureAttrs.clear();
        Signature secondEntry = mProv.createSignature(account, secondSigName, signatureAttrs);
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, secondEntry.getId());
        mProv.modifyAttrs(account, acctAttrs);

        // now we can delete the first signature
        mProv.deleteSignature(account, entry.getId());

        // rename the defaulf signature, default signature id should reman the same
        String secondSigNameNew = "second-sig-new-name";
        signatureAttrs.clear();
        signatureAttrs.put(Provisioning.A_zimbraSignatureName, secondSigNameNew);
        mProv.modifySignature(account, secondEntry.getId(), signatureAttrs);
        defaultSigId = account.getAttr(Provisioning.A_zimbraPrefDefaultSignatureId);
        assertEquals(secondEntry.getId(), defaultSigId);
        // refresh the entry, since it was moved
        secondEntry = mProv.get(account, Provisioning.SignatureBy.name, secondSigNameNew);

        // create a third signature, it should sit on the account entry
        String thirdSigName = "third-sig";
        signatureAttrs.clear();
        Signature thirdEntry = mProv.createSignature(account, thirdSigName, signatureAttrs);
        String acctSigName = account.getAttr(Provisioning.A_zimbraSignatureName);
        assertEquals(thirdSigName, acctSigName);

        list = mProv.getAllSignatures(account);
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{secondEntry, thirdEntry}, true);

        // now, verify that if A_zimbraPrefMailSignature is present on the account and if
        // A_zimbraPrefSignatureName is not present on the account, system should return
        // it as a signature using the account's name, and generate an id for it
        // first, we delete the third sig, just to clear the account signature entry
        mProv.deleteSignature(account, thirdEntry.getId());

        // manually set the A_zimbraPrefMailSignature on the aqccount
        String aSigValueOnAccount = "a signature value on account";
        String accountSigName = account.getName();
        acctAttrs.clear();
        acctAttrs.put(Provisioning.A_zimbraPrefMailSignature, aSigValueOnAccount);
        mProv.modifyAttrs(account, acctAttrs);

        // get the account signature, by its name, which is the account's name
        Signature acctSig = mProv.get(account, Provisioning.SignatureBy.name, accountSigName);
        assertEquals(account.getName(), acctSig.getName());
        assertNotSame(account.getId(), acctSig.getId());
        assertEquals(acctSig.getAttr(Provisioning.A_zimbraPrefMailSignature), aSigValueOnAccount);

        // get the account signature, by it's id
        acctSig = mProv.get(account, Provisioning.SignatureBy.id, acctSig.getId());
        assertNotSame(account.getId(), acctSig.getId());
        assertEquals(acctSig.getAttr(Provisioning.A_zimbraPrefMailSignature), aSigValueOnAccount);

        // get all signatures, the account entry should be included
        list = mProv.getAllSignatures(account);
        TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{acctSig, secondEntry}, true);

        // delete it
        mProv.deleteSignature(account, acctSig.getId());

        // set it(un-named account signature, with just a sig value) up again, for testing rename
        acctAttrs.clear();
        acctAttrs.put(Provisioning.A_zimbraPrefMailSignature, aSigValueOnAccount);
        mProv.modifyAttrs(account, acctAttrs);

        // get the account signature, by its name, which is the account's name
        acctSig = mProv.get(account, Provisioning.SignatureBy.name, accountSigName);
        // set it to the default signature
        acctAttrs.clear();
        acctAttrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, acctSig.getId());
        mProv.modifyAttrs(account, acctAttrs);
        assertEquals(acctSig.getAttr(Provisioning.A_zimbraPrefMailSignature), aSigValueOnAccount);

        // rename it!
        String accountSigNameNew = "account-sig-new-name";
        signatureAttrs.clear();
        signatureAttrs.put(Provisioning.A_zimbraSignatureName, accountSigNameNew);
        mProv.modifySignature(account, acctSig.getId(), signatureAttrs);

        // make sure we can get it by the new name
        Signature renamedAcctSig = mProv.get(account, Provisioning.SignatureBy.name, accountSigNameNew);
        assertEquals(renamedAcctSig.getId(), acctSig.getId());

        // make sure the default sig id is not changed
        defaultSigId = account.getAttr(Provisioning.A_zimbraPrefDefaultSignatureId);
        assertEquals(acctSig.getId(), defaultSigId);

        // change the default signature to something else
        acctAttrs.clear();
        acctAttrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, secondEntry.getName());
        mProv.modifyAttrs(account, acctAttrs);

        // now delete it!
        mProv.deleteSignature(account, renamedAcctSig.getId());

    }

    private void entryTest(NamedEntry entry) throws Exception  {
        System.out.println("Testing entry");

        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraId, "junk");

        try {
            mProv.modifyAttrs(entry, attrsToMod, true);
            fail("ServiceException.INVALID_REQUEST not thrown"); // should not come to here
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_displayName, ACCT_FULL_NAME);
        mProv.modifyAttrs(entry, attrsToMod, true, true);

        // make sure cn is also updated if cn is not the naing attribute
        if (mLdapProv != null) {
            String namingAttr = mLdapProv.getNamingRdnAttr(entry);
            if (!namingAttr.equals(Provisioning.A_cn)) {
                String cnValue = entry.getAttr(Provisioning.A_cn);
                assertEquals(ACCT_FULL_NAME, cnValue);
            }
        }

        mProv.reload(entry);
    }

    private void externalGalTest(Domain domain, boolean startTLS) throws Exception {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraGalMode, Provisioning.AM_LDAP);
        attrs.put(Provisioning.A_zimbraGalLdapURL, "ldap://" + LC.zimbra_server_hostname.value() + ":389"); // cannot be localhost for startTLS

        attrs.put(Provisioning.A_zimbraGalLdapBindDn, LC.zimbra_ldap_userdn.value());
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, "zimbra");
        attrs.put(Provisioning.A_zimbraGalLdapFilter, "(mail=*%s*)");

        // attrs.put(Provisioning.A_zimbraGalLdapAuthMech, Provisioning.LDAP_AM_KERBEROS5);
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Principal, "ldap/phoebe.local@PHOEBE.LOCAL");
        attrs.put(Provisioning.A_zimbraGalLdapKerberos5Keytab, "/etc/krb5.keytab");

        if (startTLS) {
            attrs.put(Provisioning.A_zimbraGalLdapStartTlsEnabled, "TRUE");
            attrs.put(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, "TRUE");
        }

        mProv.modifyAttrs(domain, attrs, true);
        Provisioning.SearchGalResult galResult = mProv.searchGal(domain,
                ACCT_EMAIL,
                Provisioning.GalSearchType.all,
                null);
        List<GalContact> matches = galResult.getMatches();
        assertEquals(1, galResult.getNumMatches());
        assertEquals(ACCT_FULL_NAME, matches.get(0).getAttrs().get("fullName"));
    }

    private void galTest(Domain domain) throws Exception {
        System.out.println("Testing gal");

        String query = ACCT_USER;
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);

        // auto complete Gal
        Provisioning.SearchGalResult galResult = mProv.autoCompleteGal(domain,
                                                                       query,
                                                                       Provisioning.GalSearchType.all,
                                                                       100);

        List<GalContact> matches = galResult.getMatches();
        assertEquals(1, galResult.getNumMatches());
        assertEquals(ACCT_FULL_NAME, matches.get(0).getAttrs().get("fullName"));

        // search gal
        galResult = mProv.searchGal(domain,
                                    query,
                                    Provisioning.GalSearchType.all,
                                    null);
        matches = galResult.getMatches();
        assertEquals(1, galResult.getNumMatches());
        assertEquals(ACCT_FULL_NAME, matches.get(0).getAttrs().get("fullName"));

        // search external gal
        externalGalTest(domain, false);

        if (TEST_STARTTLS)
            externalGalTest(domain, true);
    }

    private void searchTest(Domain domain) throws Exception {
        System.out.println("Testing search");

        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        Account cr = mProv.get(Provisioning.AccountBy.name, CR_EMAIL);

        String query = "(" + Provisioning.A_zimbraMailDeliveryAddress + "=" + ACCT_EMAIL + ")";
        List list = null;

        if (!Flag.needLdapPaging("searchAccounts")) {
            list = mProv.searchAccounts(query,
                                            new String[]{Provisioning.A_zimbraMailDeliveryAddress},
                                            Provisioning.A_zimbraMailDeliveryAddress,
                                            true,
                                            Provisioning.SA_ACCOUNT_FLAG);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{acct}, true);

            // testing get all accounts on local server, used by BackupManager
            String serverName = mProv.getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
            list = mProv.searchAccounts(
                        "(zimbraMailHost=" + serverName + ")",
                        new String[] { Provisioning.A_zimbraId }, null, false,
                        Provisioning.SA_ACCOUNT_FLAG | Provisioning.SA_CALENDAR_RESOURCE_FLAG);

        }

        if (!Flag.needLdapPaging("searchAccounts_domain")) {
            list = mProv.searchAccounts(domain, query,
                                       new String[]{Provisioning.A_zimbraMailDeliveryAddress},
                                       Provisioning.A_zimbraMailDeliveryAddress,
                                       true,
                                       Provisioning.SA_ACCOUNT_FLAG);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{acct}, true);
        }

        EntrySearchFilter.Term term = new EntrySearchFilter.Single(false,
                                                                   Provisioning.A_zimbraMailDeliveryAddress,
                                                                   EntrySearchFilter.Operator.eq,
                                                                   CR_EMAIL);
        EntrySearchFilter filter = new EntrySearchFilter(term);
        if (!Flag.needLdapPaging("searchCalendarResources")) {
            list = mProv.searchCalendarResources(filter,
                                                new String[]{Provisioning.A_zimbraMailDeliveryAddress},
                                                Provisioning.A_zimbraMailDeliveryAddress,
                                                true);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{cr}, true);
        }

        if (!Flag.needLdapPaging("searchCalendarResources_domain")) {
            list = mProv.searchCalendarResources(domain,
                                                filter,
                                                new String[]{Provisioning.A_zimbraMailDeliveryAddress},
                                                Provisioning.A_zimbraMailDeliveryAddress,
                                                true);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{cr}, true);
        }

        if (!Flag.needLdapPaging("searchDirectory")) {
            Provisioning.SearchOptions options = new Provisioning.SearchOptions();
            options.setDomain(domain);
            options.setQuery(query);
            list = mProv.searchDirectory(options);
            TestProvisioningUtil.verifyEntries(list, new NamedEntry[]{acct}, true);
        }
    }

    private Domain aliasTest() throws Exception {
        System.out.println("Testing alias");

        // create a new domain
        String domainName = "alias-test." + DOMAIN_NAME;
        Domain domain = mProv.createDomain(domainName, new HashMap<String, Object>());

        /*
        int numAccts = 10;
        int numAliases = 500;
        for (int a=0; a<numAccts; a++) {
            String acctName = "acct-" + (a+1) + "@" + domainName;
            Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());

            for (int i=0; i<numAliases; i++) {
                String aliasName = "a-" + (a+1) + "-" + (i+1) + "@" + domainName;
                System.out.println("Creating alias " + aliasName);
                mProv.addAlias(acct, aliasName);
            }
        }
        */

        return domain;
    }

    private void familyTest() throws Exception {
        System.out.println("Testing family");

        Set<String> visibleCids = new HashSet<String>();
        Set<String> invisibleCids = new HashSet<String>();
        Set<String> cids = new HashSet<String>();
        Map attrs = new HashMap<String, Object>();;
        String childNameLocal;
        String childName;
        for (int i=0; i<5; i++) {
            attrs.clear();
            childNameLocal = "v-child-" + i;
            childName =  childNameLocal + "@" + DOMAIN_NAME;
            mCustomProvTester.addAttr(attrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
            mCustomProvTester.addAttr(attrs, ACCT_NAMING_ATTR, namingAttrValue(childNameLocal));
            Account acct = mProv.createAccount(childName, PASSWORD, attrs);
            visibleCids.add(acct.getId());
            cids.add(acct.getId());

            attrs.clear();
            childNameLocal = "iv-child-" + i;
            childName = childNameLocal + "@" + DOMAIN_NAME;
            mCustomProvTester.addAttr(attrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
            mCustomProvTester.addAttr(attrs, ACCT_NAMING_ATTR, namingAttrValue(childNameLocal));
            acct = mProv.createAccount(childName, PASSWORD, attrs);
            invisibleCids.add(acct.getId());
            cids.add(acct.getId());
        }

        attrs.clear();
        childNameLocal = "not-child";
        childName = childNameLocal + "@" + DOMAIN_NAME;
        mCustomProvTester.addAttr(attrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(attrs, ACCT_NAMING_ATTR, namingAttrValue(childNameLocal));
        Account acctNotChild = mProv.createAccount(childName, PASSWORD, attrs);
        String idNotChild = acctNotChild.getId();
        Set<String> idsNotChild = new HashSet<String>();
        idsNotChild.add(idNotChild);

        Set<String> temp = new HashSet<String>();
        Account parent;

        String parentNameLocal = "parent";
        String parentName = parentNameLocal + "@" + DOMAIN_NAME;

        // should fail: adding an non child as visible child
        try {
            attrs.clear();
            mCustomProvTester.addAttr(attrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
            mCustomProvTester.addAttr(attrs, ACCT_NAMING_ATTR, namingAttrValue(parentNameLocal));
            attrs.put(Provisioning.A_zimbraChildAccount, cids);
            attrs.put(Provisioning.A_zimbraPrefChildVisibleAccount, SetUtil.union(temp, visibleCids, idsNotChild));
            parent = mProv.createAccount(parentName, PASSWORD, attrs);
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(ServiceException.INVALID_REQUEST))
                fail();
        }

        // should pass
        attrs.clear();
        mCustomProvTester.addAttr(attrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(attrs, ACCT_NAMING_ATTR, namingAttrValue(parentNameLocal));
        attrs.put(Provisioning.A_zimbraChildAccount, cids);
        attrs.put(Provisioning.A_zimbraPrefChildVisibleAccount, visibleCids);
        parent = mProv.createAccount(parentName, PASSWORD, attrs);

        // add a non child as visible
        try {
            attrs.clear();
            attrs.put("+" + Provisioning.A_zimbraPrefChildVisibleAccount, idsNotChild);

            mProv.modifyAttrs(parent, attrs);
            fail();
        } catch (ServiceException e) {
            if (!e.getCode().equals(ServiceException.INVALID_REQUEST))
                fail();
        }

        // add a child and set to visible in same request
        attrs.clear();
        attrs.put("+" + Provisioning.A_zimbraChildAccount, idsNotChild);
        attrs.put("+" + Provisioning.A_zimbraPrefChildVisibleAccount, idsNotChild);
        mProv.modifyAttrs(parent, attrs);

        // remove a child, it should be automatically removed from the visible children
        attrs.clear();
        attrs.put("-" + Provisioning.A_zimbraChildAccount, idsNotChild);
        mProv.modifyAttrs(parent, attrs);
        // verify it
        Set<String> curAttrs = parent.getMultiAttrSet(Provisioning.A_zimbraPrefChildVisibleAccount);
        assertFalse(curAttrs.contains(idsNotChild));

        // remove all visible children
        attrs.clear();
        attrs.put(Provisioning.A_zimbraChildAccount, "");
        mProv.modifyAttrs(parent, attrs);
        // verify it
        curAttrs = parent.getMultiAttrSet(Provisioning.A_zimbraPrefChildVisibleAccount);
        assertEquals(0, curAttrs.size());

        // delete all accounts
        for (String childId : cids)
            mProv.deleteAccount(childId);
        for (String childId : idsNotChild)
            mProv.deleteAccount(childId);
        mProv.deleteAccount(parent.getId());

    }

    private void flushCacheTest() throws Exception {
        System.out.println("Testing flush cache");
        // skin|locale|account|config|cos|domain|server|zimlet

        String value = null;
        String newVal = "new value";
        String oldVal = "old value";
        Map<String, Object> attrs = new HashMap<String, Object>();


        /*
         * account
         */
        String acctAttr = Provisioning.A_description;
        Account acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        assertNotNull(acct);

        // write the old value
        attrs.clear();
        attrs.put(acctAttr, oldVal);
        mSoapProv.modifyAttrs(acct, attrs); // modify via soap
        acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL); // get the entry
        value = acct.getAttr(acctAttr); // get the attr
        assertEquals(oldVal, value);  // make sure the attr is updated

        // update with the new values via ldap
        attrs.clear();
        attrs.put(acctAttr, newVal);
        acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        mProv.modifyAttrs(acct, attrs); // modify via ldap prov

        // ensure it is still the old value
        acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL); // get the entry via soap
        value = acct.getAttr(acctAttr); // get the attr
        assertEquals(oldVal, value); // the value should be still old value

        // flush the account
        mSoapProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(CacheEntryBy.id, acct.getId())});

        // ensure it is the new value
        acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL); // get he entry via soap
        value = acct.getAttr(acctAttr); // get the attr
        assertEquals(newVal, value); // now we should see the new value


        /*
         * cos
         */
        String cosAttr = Provisioning.A_zimbraPrefSkin;
        Cos cos = mSoapProv.get(Provisioning.CosBy.name, COS_NAME);
        assertNotNull(cos);

        // write the old value
        attrs.clear();
        attrs.put(cosAttr, oldVal);
        mSoapProv.modifyAttrs(cos, attrs); // modify via soap
        cos = mSoapProv.get(Provisioning.CosBy.name, COS_NAME); // get the entry
        value = cos.getAttr(cosAttr); // get the attr
        assertEquals(oldVal, value);  // make sure the attr is updated

        // update with the new values via ldap
        attrs.clear();
        attrs.put(cosAttr, newVal);
        cos = mProv.get(Provisioning.CosBy.name, COS_NAME);
        mProv.modifyAttrs(cos, attrs); // modify via ldap prov

        // ensure it is still the old value
        cos = mSoapProv.get(Provisioning.CosBy.name, COS_NAME); // get he entry via soap
        value = cos.getAttr(cosAttr); // get the attr
        assertEquals(oldVal, value); // the value should be still old value

        // ensure the account is also still the old value
        acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL); // get the entry via soap
        value = acct.getAttr(cosAttr); // get the attr
        assertEquals(oldVal, value); // the value should be still old value

        // flush the cos
        mSoapProv.flushCache(CacheEntryType.cos, new CacheEntry[]{new CacheEntry(CacheEntryBy.id, cos.getId())});

        // ensure it is the new value
        cos = mSoapProv.get(Provisioning.CosBy.name, COS_NAME); // get he entry via soap
        value = cos.getAttr(cosAttr); // get the attr
        assertEquals(newVal, value); // now we should see the new value

        // ensure the account also gets the new value
        acct = mSoapProv.get(Provisioning.AccountBy.name, ACCT_EMAIL); // get he entry via soap
        value = acct.getAttr(cosAttr); // get the attr
        assertEquals(newVal, value); // now we should see the new value

        /*
         * config
         */
        String configAttr = "zimbraWebClientLoginUrl";
        Config config = mSoapProv.getConfig();
        assertNotNull(config);

        // write the old value
        attrs.clear();
        attrs.put(configAttr, oldVal);
        mSoapProv.modifyAttrs(config, attrs); // modify via soap
        config = mSoapProv.getConfig(); // get the entry
        value = config.getAttr(configAttr); // get the attr
        assertEquals(oldVal, value);  // make sure the attr is updated

        // update with the new values via ldap
        attrs.clear();
        attrs.put(configAttr, newVal);
        config = mProv.getConfig();
        mProv.modifyAttrs(config, attrs); // modify via ldap prov

        // ensure it is still the old value
        config = mSoapProv.getConfig(); // get the entry via soap
        value = config.getAttr(configAttr); // get the attr
        assertEquals(oldVal, value); // the value should be still old value

        // flush the account
        mSoapProv.flushCache(CacheEntryType.config, null);

        // ensure it is the new value
        config = mSoapProv.getConfig(); // get he entry via soap
        value = config.getAttr(configAttr); // get the attr
        assertEquals(newVal, value); // now we should see the new value
    }

    private void attributeInheritanceTest() throws Exception {
        System.out.println("Testing attribute inheritance");

        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        assertNotNull(acct);

        Cos cos = mProv.getCOS(acct);
        assertNotNull(cos);

        Domain domain = mProv.getDomain(acct);
        assertNotNull(domain);

        String attr = Provisioning.A_zimbraPrefSkin;

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.clear();
        attrs.put(attr, "account-value");
        mProv.modifyAttrs(acct, attrs);

        attrs.clear();
        attrs.put(attr, "cos-value");
        mProv.modifyAttrs(cos, attrs);

        attrs.clear();
        attrs.put(attr, "domain-value");
        mProv.modifyAttrs(domain, attrs);

        // should get account value
        String val = acct.getAttr(attr);
        assertEquals("account-value", val);

        // delete account value, should get cos value
        attrs.clear();
        attrs.put(attr, "");
        mProv.modifyAttrs(acct, attrs);

        val = acct.getAttr(attr);
        assertEquals("cos-value", val);

        // delete cos value, should get domain value
        attrs.clear();
        attrs.put(attr, "");
        mProv.modifyAttrs(cos, attrs);

        val = acct.getAttr(attr);
        assertEquals("domain-value", val);

        // delete domain value, should get null
        attrs.clear();
        attrs.put(attr, "");
        mProv.modifyAttrs(domain, attrs);

        val = acct.getAttr(attr);
        assertEquals(null, val);
    }

    private Set<String> getAvailableSkins_prior_bug31596(Account acct) throws ServiceException {

        // 1) if set on account/cos, use it
        Set<String> skins = acct.getMultiAttrSet(Provisioning.A_zimbraAvailableSkin);
        if (skins.size() > 0)
            return skins;

        // 2) if set on Domain, use it
        Domain domain = Provisioning.getInstance().getDomain(acct);
        if (domain == null)
            return skins;
        return domain.getMultiAttrSet(Provisioning.A_zimbraAvailableSkin);
    }

    private void attributeInheritanceTestMultiValue_prior_bug31596() throws Exception {
        System.out.println("Testing attribute inheritance multi-value prior bug31596");

        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        assertNotNull(acct);

        Domain domain = mProv.getDomain(acct);
        assertNotNull(domain);

        String attr = Provisioning.A_zimbraAvailableSkin;

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.clear();
        attrs.put("+" + attr, new String[]{"domain-value-1", "domain-value-2"});
        mProv.modifyAttrs(domain, attrs);

        // should get domain value
        Set<String> val = getAvailableSkins_prior_bug31596(acct);
        TestProvisioningUtil.verifyEntries(val, new String[]{"domain-value-1", "domain-value-2"}, true);
    }

    /*
     * too bad Entry.getMultiAttrSet caches the value instead doing what Entry.getObject does:
     * check defaults.  Thus we can't do the same test as attributeInheritanceTest().
     *
     * This is an existing bug for any multi-value attrs, has nothing to do with the fix of bug31596
     */
    private void attributeInheritanceTestMultiValue() throws Exception {
        System.out.println("Testing attribute inheritance multi-value");

        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        assertNotNull(acct);

        Domain domain = mProv.getDomain(acct);
        assertNotNull(domain);

        String attr = Provisioning.A_zimbraAvailableSkin;

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.clear();
        attrs.put("+" + attr, new String[]{"domain-value-1", "domain-value-2"});
        mProv.modifyAttrs(domain, attrs);

        // should get domain value
        Set<String> val = acct.getMultiAttrSet(attr);
        TestProvisioningUtil.verifyEntries(val, new String[]{"domain-value-1", "domain-value-2"}, true);
    }

    private void loadTest() throws Exception {
        System.out.println("Testing load");

        // create a new domain
        String domainName = "load-test." + DOMAIN_NAME;
        Domain domain = mProv.createDomain(domainName, new HashMap<String, Object>());

        int numAccts = 5000;
        for (int a=0; a<numAccts; a++) {
            String acctName = "acct-" + (a+1) + "@" + domainName;
            System.out.println("creating account" + acctName);
            Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        }
    }

    private String execute() throws Exception {

        // mCustomProvTester.cleanup();

        healthTest();
        Config config = configTest();
        String cosName = cosTest();
        String[] domainNames = domainTest();
        String domainName = domainNames[0];
        String otherDomainName = domainNames[1];
        String specialCharDomainName = domainNames[2];
        mimeTest();
        Server server = serverTest();
        Zimlet zimlet = zimletTest();

        Account adminAccount = adminAccountTest();
        Account accounts[] = accountTest(adminAccount, mProv.get(Provisioning.CosBy.name, cosName), mProv.get(Provisioning.DomainBy.name, domainName), mProv.get(Provisioning.DomainBy.name, otherDomainName));
        Account account = accounts[0];
        authTest(account);
        passwordTest(account);
        localeTest();

        CalendarResource calendarResource = calendarResourceTest(mProv.get(Provisioning.CosBy.name, cosName), mProv.get(Provisioning.DomainBy.name, domainName));

        DistributionList[] distributionLists = distributionListTest(mProv.get(Provisioning.DomainBy.name, domainName));
        DataSource dataSource = dataSourceTest(account);
        Identity identity = identityTest(account);
        signatureTest(account);

        entryTest(account);
        galTest(mProv.get(Provisioning.DomainBy.name, domainName));
        searchTest(mProv.get(Provisioning.DomainBy.name, domainName));

        Domain aliasTestDomain = aliasTest();
        familyTest();
        flushCacheTest();
        attributeInheritanceTest();
        attributeInheritanceTestMultiValue_prior_bug31596();
        attributeInheritanceTestMultiValue();

        // loadTest();

        // ========================================================================
        System.out.println("\nPress enter to delete entries created by the test");

        String line = (new BufferedReader(new InputStreamReader(System.in))).readLine();
        System.out.println("\nDeleting entries");

        mProv.deleteZimlet(ZIMLET_NAME);
        mProv.deleteServer(server.getId());
        mProv.deleteIdentity(account, IDENTITY_NAME);
        mProv.deleteDataSource(account, dataSource.getId());

        // delete DLs
        for (DistributionList dl : distributionLists) {
            // so much trouble for DL if the domain is not the default doamin
            // we temporarily changed the default domain to the special char
            // domain  in diestributionListTest so that it can be created.
            // now we have to do the same hack to it can be removed!!!
            String defaultDomain = mProv.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName);
            if (mCustomProvTester.isCustom() && dl.getDomainName().equals(DOMAIN_NAME_SPECIAL_CHARS))
                setDefaultDomain(DOMAIN_NAME_SPECIAL_CHARS);

            mProv.deleteDistributionList(dl.getId());

            if (mCustomProvTester.isCustom() && dl.getDomainName().equals(DOMAIN_NAME_SPECIAL_CHARS))
                setDefaultDomain(defaultDomain);
        }

        mProv.deleteCalendarResource(calendarResource.getId());

        for (Account acct : accounts)
            mProv.deleteAccount(acct.getId());
        mProv.deleteAccount(adminAccount.getId());

        mProv.deleteDomain(mProv.get(Provisioning.DomainBy.name, domainName).getId());
        mProv.deleteDomain(mProv.get(Provisioning.DomainBy.name, otherDomainName).getId());
        mProv.deleteDomain(mProv.get(Provisioning.DomainBy.name, specialCharDomainName).getId());
        mProv.deleteDomain(aliasTestDomain.getId());
        mProv.deleteCos(mProv.get(Provisioning.CosBy.name, cosName).getId());

        System.out.println("\nAll done");
        return TEST_ID;
    }


    public void testProvisioning() throws Exception {
        try {
            System.out.println("\nTest " + TEST_ID + " starting\n");
            execute();
            System.out.println("\nTest " + TEST_ID + " done!");
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            System.out.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" +
                               (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
            e.printStackTrace(System.out);
            System.out.println("\nTest " + TEST_ID + " failed!");
        } catch (AssertionFailedError e) {
            System.out.println("\n===== assertion failed =====");
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
        }

    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("WARN");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/p4/main/ZimbraServer/conf/log4j.properties.zmprov-l");
        // TestUtil.runTest(new TestSuite(TestProvisioning.class));

        TestProvisioning t = new TestProvisioning();
        t.setUp();
        t.execute();

    }
}
