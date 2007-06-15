package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.*;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.ZimbraCustomAuth;
import com.zimbra.cs.account.ldap.custom.CustomLdapProvisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

public class TestProvisioning extends TestCase {
    
    private Provisioning mProv;
    LdapProvisioning mLdapProv;
    CustomProvTester mCustomProvTester;
    
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
        
        // if we are a ldapProvisioning, cast the prov obj and save it once here
        // ldapProv is used for several LDAP specific tests
        if (mProv instanceof LdapProvisioning)
            mLdapProv = (LdapProvisioning)mProv;
        
        mCustomProvTester = new CustomProvTester(mProv);
        
        Date date = new Date();
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
        TEST_ID = fmt.format(date);
        
        PRE_AUTH_KEY = PreAuthKey.generateRandomPreAuthKey();
        PASSWORD = "test123";
        
        SPECIAL_CHARS_ALLOWED_IN_DOMAIN = "/";
        SPECIAL_CHARS_ALLOWED_IN_USER_PART = "/";
        
        COS_NAME = "cos-" + TEST_ID;
        DOMAIN_NAME = "domain-" + TEST_ID + ".ldap-test-domain";
        DOMAIN_NAME_SPECIAL_CHARS = "domain-" + SPECIAL_CHARS_ALLOWED_IN_DOMAIN + "-" + TEST_ID + ".ldap-test-domain";
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
        
        ACCT_NAMING_ATTR = LC.get("ldap_user_naming_rdn_attr");
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
        
        public void authenticate(Account acct, String password) throws Exception {
            if (acct.getName().equals(mTheOnlyAcctThatCanAuth.getName()) && 
                password.equals(mTheOnlyPasswordIKnowAbout))
                return;
            else
                throw new Exception("auth failed by TestCustomAuth for " + acct.getName() + " password " + password);
        }
    }
    
    private void verifySameId(NamedEntry entry1, NamedEntry entry2) throws Exception {
        assertNotNull(entry1);
        assertNotNull(entry2);
        assertEquals(entry1.getId(), entry2.getId());
    }
    
    
    private void verifySameEntry(NamedEntry entry1, NamedEntry entry2) throws Exception {
        verifySameId(entry1, entry2);
        assertEquals(entry1.getName(), entry2.getName());
    }
    
    // verify list contains all the entries
    // if checkCount == true, verify the count matches too
    private void verifyEntries(List<NamedEntry> list, NamedEntry[] entries, boolean checkCount) throws Exception {
        try {
            if (checkCount)
                assertEquals(list.size(), entries.length);
        
            Set<String> ids = new HashSet<String>();
            for (NamedEntry entry : list)
                ids.add(entry.getId());
            
            for (NamedEntry entry : entries)
                assertTrue(ids.contains(entry.getId()));
         
        } catch (AssertionFailedError e) {
            System.out.println("\n===== verifyEntries failed =====");
            System.out.println("Message:" + e.getMessage());
            
            System.out.println("\nlist contains " + list.size() + " entries:");
            for (NamedEntry entry : list)
                System.out.println("    " + entry.getName());
            System.out.println("entries contains " + entries.length + " entries:");
            for (NamedEntry entry : entries)
                System.out.println("    " + entry.getName());
            
            System.out.println();
            throw e;
        }
    }
    
    // verify list of NamedEntry contains all the ids
    // if checkCount == true, verify the count matches too
    private void verifyEntriesById(List<NamedEntry> list, String[] names, boolean checkCount) throws Exception {
        Set<String> idsInList = new HashSet<String>();
        for (NamedEntry entry : list)
            idsInList.add(entry.getId());
        
        verifyEntries(idsInList, names, checkCount);
    }
    
    // verify list of NamedEntry contains all the names
    // if checkCount == true, verify the count matches too
    private void verifyEntriesByName(List<NamedEntry> list, String[] names, boolean checkCount) throws Exception {
        Set<String> namesInList = new HashSet<String>();
        for (NamedEntry entry : list)
            namesInList.add(entry.getName());
        
        verifyEntries(namesInList, names, checkCount);
    }
    
    // verify list contains all the names
    // if checkCount == true, verify the count matches too
    private void verifyEntries(Set<String> list, String[] names, boolean checkCount) throws Exception {
        try {
            if (checkCount)
                assertEquals(names.length, list.size());
            
            for (String name : names)
                assertTrue(list.contains(name));
         
        } catch (AssertionFailedError e) {
            System.out.println("\n===== verifyEntries failed =====");
            System.out.println("Message:" + e.getMessage());
            
            System.out.println("\nlist contains " + list.size() + " entries:");
            for (String name : list)
                System.out.println("    " + name);
            System.out.println("entries contains " + names.length + " entries:");
            for (String name : names)
                System.out.println("    " + name);
            
            System.out.println();
            throw e;
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
    
    private Cos cosTest() throws Exception {
        System.out.println("Testing cos");
        
        Cos entry = mProv.createCos(COS_NAME, new HashMap<String, Object>());
        
        Cos entryGot = mProv.get(Provisioning.CosBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.CosBy.name, COS_NAME);
        verifySameEntry(entry, entryGot);
        Cos defaultCos = mProv.get(Provisioning.CosBy.name, "default");
        assertNotNull(defaultCos);
        
        List list = mProv.getAllCos();
        verifyEntries(list, new NamedEntry[]{defaultCos, entry}, false);
        
        mProv.renameCos(entry.getId(), NEW_NAME);
        mProv.renameCos(entry.getId(), COS_NAME);
                        
        return entry;
    }
    
    private Domain[] domainTest() throws Exception {
        System.out.println("Testing domain");
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, PRE_AUTH_KEY);
        Domain entry = mProv.createDomain(DOMAIN_NAME, attrs);
        
        Domain entryGot = mProv.get(Provisioning.DomainBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        verifySameEntry(entry, entryGot);
        
        Domain otherEntry = mProv.createDomain(OTHER_DOMAIN_NAME, attrs);
        Domain specialCharEntry = mProv.createDomain(DOMAIN_NAME_SPECIAL_CHARS, attrs);
                
        List list = mProv.getAllDomains();
        verifyEntries(list, new NamedEntry[]{entry, otherEntry, specialCharEntry}, false);
        
        // set our domain as the default domain
        setDefaultDomain(DOMAIN_NAME);
                
        return new Domain[]{entry, otherEntry, specialCharEntry};
    }
    
    private void mimeTest() throws Exception {
        System.out.println("Testing mime");
        
        MimeTypeInfo mime = mProv.getMimeType("all"); 
        assertNotNull(mime);
        
        mime = mProv.getMimeTypeByExtension("text");
        assertNotNull(mime);
    }

    
    private Server serverTest() throws Exception {
        System.out.println("Testing server");
        
        Map<String, Object> serverAttrs = new HashMap<String, Object>();
        Server entry = mProv.createServer(SERVER_NAME, serverAttrs);
        
        Server entryGot = mProv.get(Provisioning.ServerBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.ServerBy.name, SERVER_NAME);
        verifySameEntry(entry, entryGot);
        
        Server localeServer = mProv.getLocalServer();
        assertNotNull(localeServer);
        
        List list = mProv.getAllServers();
        verifyEntries(list, new NamedEntry[]{localeServer, entry}, false);
        
        list = mProv.getAllServers("mailbox");
        verifyEntries(list, new NamedEntry[]{localeServer}, false);
        
        return entry;
    }
    
    private Zimlet zimletTest() throws Exception {
        System.out.println("Testing zimlet");
        
        Map<String, Object> zimletAttrs = new HashMap<String, Object>();
        zimletAttrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
        Zimlet entry = mProv.createZimlet(ZIMLET_NAME, zimletAttrs);
        
        Zimlet entryGot = mProv.getZimlet(ZIMLET_NAME);
        verifySameEntry(entry, entryGot);
        
        List list = mProv.getObjectTypes();
        verifyEntries(list, new NamedEntry[]{entry}, false);
        
        list = mProv.listAllZimlets();
        verifyEntries(list, new NamedEntry[]{entry}, false);
            
        return entry;
    }

    private void authTest(Account account) throws Exception  {
        System.out.println("Testing auth");
        
        // zimbra auth
        mProv.authAccount(account, PASSWORD, "unittest");
        
        // ldap auth, test using our own ldap
        Domain domain = mProv.getDomain(account);
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_LDAP);
        attrsToMod.put(Provisioning.A_zimbraAuthLdapURL, "ldap://localhost:389");
        attrsToMod.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
        attrsToMod.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrsToMod.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        mProv.modifyAttrs(domain, attrsToMod, true);
        mProv.authAccount(account, PASSWORD, "unittest");
                
        // ad auth
        // need an AD, can't test
        
        // custom auth
        attrsToMod.clear();
        String customAuthHandlerName = "test";
        attrsToMod.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_CUSTOM + customAuthHandlerName);
        mProv.modifyAttrs(domain, attrsToMod, true);
        ZimbraCustomAuth.register(customAuthHandlerName, new TestCustomAuth(account, PASSWORD));
        mProv.authAccount(account, PASSWORD, "unittest");
        
        // try an auth failure
        try {
            mProv.authAccount(account, PASSWORD + "-not", "unittest");
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
        mProv.preAuthAccount(account, 
                            ACCT_EMAIL, // account name 
                            authBy,     // by
                            timestamp,  // timestamp
                            0,          // expires
                            preAuth);   // preauth key
        
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
        verifySameEntry(entry, entryGot);
        
        /*
         * admin can be retrieved without the domain, default domain will be used if domain is not supplied
         */
        Account entryGotByuser = mProv.get(Provisioning.AccountBy.name, ADMIN_USER);
        verifySameEntry(entryGot, entryGotByuser);
        
        
        List list = mProv.getAllAdminAccounts();
        verifyEntries(list, new NamedEntry[]{entry}, false);

        return entry;
    }
    
    
    // account and account aliases
    private Account[] accountTest(Account adminAcct, Cos cos, Domain domain, Domain otherDomain) throws Exception {
        System.out.println("Testing account");
        
        // create an account
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(acctAttrs, BASE_DN_PSEUDO_ATTR, ACCT_BASE_DN);
        mCustomProvTester.addAttr(acctAttrs, ACCT_NAMING_ATTR, ACCT_NAMING_ATTR_VALUE);
        acctAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
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
        verifySameEntry(entrySpecialChars, entryGot);
        
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
        verifySameEntry(entry, entryGot);
        
        // get account by name(i.e. email)
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        verifySameEntry(entry, entryGot);
        
        // get account by alias
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_ALIAS_EMAIL);
        verifySameEntry(entry, entryGot);
        
        // get account by alias
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL);
        if (mCustomProvTester.isCustom())
            assertEquals(null, entryGot);
        else
            verifySameEntry(entry, entryGot);        
                
        // get all accounts in a domain
        List list = mProv.getAllAccounts(domain);
        verifyEntries(list, new NamedEntry[]{entry, adminAcct}, mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        
        // get all accounts in a domain, visitor version
        TestVisitor visitor = new TestVisitor();
        mProv.getAllAccounts(domain, visitor);
        verifyEntries(visitor.visited(), new NamedEntry[]{entry, adminAcct}, mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        
        // modify account status
        mProv.modifyAccountStatus(entry, "maintenance");
        mProv.modifyAccountStatus(entry, "active");

        /*
         * rename account, same domain
         */ 
        mProv.renameAccount(entryId, NEW_EMAIL);
        // make sure the account is still in the same domain
        list = searchAccountsInDomain(domain);
        verifyEntriesById(list, new String[]{entryId}, false);
        verifyEntriesByName(list, new String[]{NEW_EMAIL}, false);
        // re-get the entry since it might've been changed after the rename
        entry = mProv.get(Provisioning.AccountBy.id, entryId);
        if (mCustomProvTester.isCustom()) {
            // make sure it is still in the same dn
            mCustomProvTester.verifyDn(entry, acctDn);
            // make sure both aliases are not moved or changed
            // actually the following just verifies that they are not changed, 
            // for now can't verify if they are moved unless we improve the test
            list = searchAliasesInDomain(domain);
            verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL}, true);
        } else {
            // make sure the alias is still in the same domain
            list = searchAliasesInDomain(domain);
            verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL}, true);
            // make sure the alias in the other domain is still in the other domain
            list = searchAliasesInDomain(otherDomain);
            verifyEntriesByName(list, new String[]{ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL}, true);
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
            // make sure the account is now in the other domain
            list = searchAccountsInDomain(otherDomain);
            verifyEntriesById(list, new String[]{entryId}, true);
            verifyEntriesByName(list, new String[]{NEW_EMAIL_IN_OTHER_DOMAIN}, true);
            // make sure the alias is now moved to the other domain and there shouldn't be any let in the old domain
            list = searchAliasesInDomain(domain);
            assertEquals(0, list.size());
            // make sure both aliases are now in the other doamin
            list = searchAliasesInDomain(otherDomain);
            verifyEntriesByName(list, new String[]{ACCT_ALIAS_AFTER_ACCOUNT_RENAME_TO_OTHER_DMAIN_EMAIL,
                                ACCT_ALIAS_IN_OTHER_DOMAIN_EMAIL}, true);
        }
        
        /*
         * rename it back
         */
        mProv.renameAccount(entryId, ACCT_EMAIL);
        // make sure the account is moved back to the orig domain
        list = searchAccountsInDomain(domain);
        verifyEntriesById(list, new String[]{entryId}, false);
        verifyEntriesByName(list, new String[]{ACCT_EMAIL}, false);
        // re-get the entry since it might've been changed after the rename
        entry = mProv.get(Provisioning.AccountBy.id, entryId);

        if (mCustomProvTester.isCustom()) {
            list = searchAliasesInDomain(domain);
            verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL}, true);

        } else {
            // now, both aliases should be moved to the orig domain
            list = searchAliasesInDomain(otherDomain);
            assertEquals(0, list.size());
            list = searchAliasesInDomain(domain);
            verifyEntriesByName(list, new String[]{ACCT_ALIAS_EMAIL,
                                ACCT_ALIAS_IN_OTHER_DOMAIN_AFTER_ACCOUNT_RENAME_TO_ORIG_DOMAIN_EMAIL}, true);
        }
        
        
        // remove alias
        mProv.removeAlias(entry, ACCT_ALIAS_EMAIL);
        
        if (!mCustomProvTester.isCustom()) {
            mProv.removeAlias(entry, ACCT_ALIAS_IN_OTHER_DOMAIN_AFTER_ACCOUNT_RENAME_TO_ORIG_DOMAIN_EMAIL);
        }
        list = searchAliasesInDomain(domain);
        assertEquals(0, list.size());
        list = searchAliasesInDomain(otherDomain);
        assertEquals(0, list.size());
        
        // set cos
        mProv.setCOS(entry, cos);
                
        return new Account[]{entry,entrySpecialChars};
    }
    
    private void passwordTest(Account account) throws Exception {
        System.out.println("Testing password");
        
        mProv.changePassword(account, PASSWORD, PASSWORD);
        mProv.checkPasswordStrength(account, PASSWORD);
        mProv.setPassword(account, PASSWORD);
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
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.CalendarResourceBy.name, CR_EMAIL);
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.CalendarResourceBy.name, CR_ALIAS_EMAIL);
        verifySameEntry(entry, entryGot);
        
        List list = mProv.getAllCalendarResources(domain); 
        verifyEntries(list, new NamedEntry[]{entry}, mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        
        TestVisitor visitor = new TestVisitor();
        mProv.getAllCalendarResources(domain, visitor);
        verifyEntries(visitor.visited(), new NamedEntry[]{entry}, mCustomProvTester.verifyAccountCountForDomainBasedSearch());
        
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
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_EMAIL);
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_ALIAS_EMAIL);
        verifySameEntry(entry, entryGot);
        
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
        verifySameEntry(dlSpecilaChars, entryGot);
        // set back the orig default domain
        if (mCustomProvTester.isCustom())
            setDefaultDomain(curDefaultDomain);
        
        mProv.addMembers(entry, new String[]{DL_NESTED_EMAIL});
        mProv.addMembers(dlNested, new String[]{ACCT_EMAIL});
        
        List list = mProv.getAllDistributionLists(domain);
        verifyEntries(list, new NamedEntry[]{entry, dlNested}, mCustomProvTester.verifyDLCountForDomainBasedSearch());
        
        Account account = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        Set<String> set = mProv.getDistributionLists(account);
        assertEquals(2, set.size());
        assertTrue(set.contains(entry.getId()));
        assertTrue(set.contains(dlNested.getId()));
        
        Map<String, String> via = new HashMap<String, String>();
        list = mProv.getDistributionLists(account, false, via);
        verifyEntries(list, new NamedEntry[]{entry, dlNested}, true);
        assertEquals(1, via.size());
        assertEquals(dlNested.getName(), via.get(entry.getName()));
        
        list = mProv.getDistributionLists(account, true, null);
        verifyEntries(list, new NamedEntry[]{dlNested}, true);
        
        boolean inList = mProv.inDistributionList(account, entry.getId());
        assertTrue(inList);
                
        mProv.removeAlias(entry, DL_ALIAS_EMAIL);
        
        mProv.removeMembers(entry, new String[]{dlNested.getName()});

        mProv.renameDistributionList(entry.getId(), NEW_EMAIL);
        mProv.renameDistributionList(entry.getId(), DL_EMAIL);
                                
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
        dsAttrs.put(Provisioning.A_zimbraPrefSignatureName, "sig-business");
        dsAttrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
        dsAttrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
        dsAttrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
        
        DataSource entry = mProv.createDataSource(account, DataSource.Type.pop3, DATA_SOURCE_NAME, dsAttrs);

        DataSource entryGot = mProv.get(account, Provisioning.DataSourceBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(account, Provisioning.DataSourceBy.name, DATA_SOURCE_NAME);
        verifySameEntry(entry, entryGot);
        
        List list = mProv.getAllDataSources(account);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraDataSourcePollingInterval, "100");
        mProv.modifyDataSource(account, entry.getId(), attrsToMod);
               
        return entry;
    }
    
    private Identity identityTest(Account account) throws Exception {
        System.out.println("Testing identity");
        
        Map<String, Object> identityAttrs = new HashMap<String, Object>();
        identityAttrs.put(Provisioning.A_zimbraPrefSignatureName, "sig-business");
        identityAttrs.put(Provisioning.A_zimbraPrefFromAddress, "micky.mouse@zimbra,com");
        identityAttrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
        identityAttrs.put(Provisioning.A_zimbraPrefReplyToEnabled, "TRUE");
        identityAttrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
        identityAttrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
        Identity entry = mProv.createIdentity(account, IDENTITY_NAME, identityAttrs);
        
        Identity entryGot = mProv.get(account, Provisioning.IdentityBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(account, Provisioning.IdentityBy.name, IDENTITY_NAME);
        verifySameEntry(entry, entryGot);
        Identity defaultIdentity = mProv.get(account, Provisioning.IdentityBy.name, Provisioning.DEFAULT_IDENTITY_NAME);
        verifySameId(account, defaultIdentity);
        assertEquals(Provisioning.DEFAULT_IDENTITY_NAME, defaultIdentity.getName());
        
        List list = mProv.getAllIdentities(account);
        verifyEntries(list, new NamedEntry[]{defaultIdentity, entry}, true);
        
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
        verifySameEntry(entry, entryGot);
        
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
        
        Map<String, Object> signatureAttrs = new HashMap<String, Object>();
        signatureAttrs.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_VALUE);
        Signature entry = mProv.createSignature(account, SIGNATURE_NAME, signatureAttrs);
        
        Signature entryGot = mProv.get(account, Provisioning.SignatureBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(account, Provisioning.SignatureBy.name, SIGNATURE_NAME);
        verifySameEntry(entry, entryGot);
        
        List list = mProv.getAllSignatures(account);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        // since this is the only signature, it should be automatically set as the default signature of the account
        String defaultSigName = account.getAttr(Provisioning.A_zimbraPrefDefaultSignature);
        assertEquals(SIGNATURE_NAME, defaultSigName);
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_VALUE_MODIFIED);
        mProv.modifySignature(account, SIGNATURE_NAME, attrsToMod);
        
        // make sure we get the modified value back
        entryGot = mProv.get(account, Provisioning.SignatureBy.id, entry.getId());
        assertEquals(SIGNATURE_VALUE_MODIFIED, entryGot.getAttr(Provisioning.A_zimbraPrefMailSignature));
        
        // try to delete the signature, since it is the default signature (because it is the only one)
        // it should not be allowed
        try {
            mProv.deleteSignature(account, SIGNATURE_NAME);
            fail("ServiceException.INVALID_REQUEST not thrown"); // should not come to here
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
        
        // create a second signature and set it as the default 
        String secondSigName = "second-sig";
        signatureAttrs.clear();
        Signature secondEntry = mProv.createSignature(account, secondSigName, signatureAttrs);
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraPrefDefaultSignature, secondSigName);
        mProv.modifyAttrs(account, acctAttrs);
        
        // now we can delete the first sig
        mProv.deleteSignature(account, SIGNATURE_NAME);
        
        // rename the defaulf sig, make sure A_zimbraPrefDefaultSignature is changed accordingly
        String secondSigNameNew = "second-sig-new-name";
        signatureAttrs.clear();
        signatureAttrs.put(Provisioning.A_zimbraPrefSignatureName, secondSigNameNew);
        mProv.modifySignature(account, secondSigName, signatureAttrs);
        defaultSigName = account.getAttr(Provisioning.A_zimbraPrefDefaultSignature);
        assertEquals(secondSigNameNew, defaultSigName);
        // refresh the entry, since it was moved
        secondEntry = mProv.get(account, Provisioning.SignatureBy.name, secondSigNameNew);
        
        // create a third signature, it should sit on the account entry
        String thirdSigName = "third-sig";
        signatureAttrs.clear();
        Signature thirdEntry = mProv.createSignature(account, thirdSigName, signatureAttrs);
        String acctSigName = account.getAttr(Provisioning.A_zimbraPrefSignatureName);
        assertEquals(thirdSigName, acctSigName);
        
        list = mProv.getAllSignatures(account);
        verifyEntries(list, new NamedEntry[]{secondEntry, thirdEntry}, true);
        
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
    
    private void galTest(Domain domain) throws Exception {
        System.out.println("Testing gal");
        
        String query = ACCT_USER;
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        
        // auto complete Gal
        Provisioning.SearchGalResult galResult = mProv.autoCompleteGal(domain, 
                                                                       query,
                                                                       Provisioning.GAL_SEARCH_TYPE.ALL, 
                                                                       100);

        List<GalContact> matches = galResult.matches;
        assertEquals(1, matches.size());
        assertEquals(ACCT_FULL_NAME, matches.get(0).getAttrs().get("fullName"));
        
        // search  gal
        galResult = mProv.searchGal(domain, 
                                    query,
                                    Provisioning.GAL_SEARCH_TYPE.ALL, 
                                    null);
        matches = galResult.matches;
        assertEquals(1, matches.size());
        assertEquals(ACCT_FULL_NAME, matches.get(0).getAttrs().get("fullName"));
    }
    
    private void searchTest(Domain domain) throws Exception {
        System.out.println("Testing search");
        
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        Account cr = mProv.get(Provisioning.AccountBy.name, CR_EMAIL);
        
        String query = "(" + Provisioning.A_zimbraMailDeliveryAddress + "=" + ACCT_EMAIL + ")";
        List list = mProv.searchAccounts(query, 
                                        new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                        Provisioning.A_zimbraMailDeliveryAddress, 
                                        true,
                                        Provisioning.SA_ACCOUNT_FLAG); 
        verifyEntries(list, new NamedEntry[]{acct}, true);
               
        list = mProv.searchAccounts(domain, query, 
                                   new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                   Provisioning.A_zimbraMailDeliveryAddress, 
                                   true,
                                   Provisioning.SA_ACCOUNT_FLAG); 
        verifyEntries(list, new NamedEntry[]{acct}, true);
        
        EntrySearchFilter.Term term = new EntrySearchFilter.Single(false, 
                                                                   Provisioning.A_zimbraMailDeliveryAddress, 
                                                                   EntrySearchFilter.Operator.eq,
                                                                   CR_EMAIL);
        EntrySearchFilter filter = new EntrySearchFilter(term);
        list = mProv.searchCalendarResources(filter,
                                            new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                            Provisioning.A_zimbraMailDeliveryAddress, 
                                            true);
        verifyEntries(list, new NamedEntry[]{cr}, true);       
        
        list = mProv.searchCalendarResources(domain,
                                            filter,
                                            new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                            Provisioning.A_zimbraMailDeliveryAddress, 
                                            true);
        verifyEntries(list, new NamedEntry[]{cr}, true);
        
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();
        options.setDomain(domain);
        options.setQuery(query);
        list = mProv.searchDirectory(options);
        verifyEntries(list, new NamedEntry[]{acct}, true);
    }
    
    private String execute() throws Exception {
        
        mCustomProvTester.cleanup();
        
        healthTest();
        Config config = configTest();
        Cos cos = cosTest();
        Domain[] domains = domainTest();
        Domain domain = domains[0];
        Domain otherDomain = domains[1];
        Domain specialCharDomain = domains[2];
        mimeTest();
        Server server = serverTest();
        Zimlet zimlet = zimletTest();    
        
        Account adminAccount = adminAccountTest();
        Account accounts[] = accountTest(adminAccount, cos, domain, otherDomain);
        Account account = accounts[0];
        authTest(account);
        passwordTest(account);
        CalendarResource calendarResource = calendarResourceTest(cos, domain);
        
        DistributionList[] distributionLists = distributionListTest(domain);
        DataSource dataSource = dataSourceTest(account);
        Identity identity = identityTest(account);
        signatureTest(account);
        
        entryTest(account);
        galTest(domain);
        searchTest(domain);

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
        
        mProv.deleteDomain(domain.getId());
        mProv.deleteDomain(otherDomain.getId());
        mProv.deleteDomain(specialCharDomain.getId());
        mProv.deleteCos(cos.getId());
        
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
        TestUtil.runTest(new TestSuite(TestProvisioning.class));
    }
}
