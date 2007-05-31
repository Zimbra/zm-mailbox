package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
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
    
    private TestVisitor mVisitor;
    
    private String TEST_ID;
    
    private String PRE_AUTH_KEY;
    private String PASSWORD;
    
    private String COS_NAME;
    private String DOMAIN_NAME;
    private String SERVER_NAME;
    private String ZIMLET_NAME;
    
    private String NEW_NAME;  // for testing rename
    private String NEW_EMAIL; // for testing rename
    
    private String DEFAULT_ADMIN_USER;
    private String ADMIN_USER;
    private String ADMIN_EMAIL;
    private String ACCT_USER;
    private String ACCT_EMAIL;
    private String ACCT_ALIAS_USER;
    private String ACCT_ALIAS_EMAIL;
    private String ACCT_FULL_NAME;
    
    /*
     * for testing CustomLdapProvisioning where naming rdn attr
     * for account is configured
     */ 
    private String ACCT_NAMING_ATTR;
    private String ACCT_NAMING_ATTR_VALUE;
    private String ACCT_BASE_DN;  // for testing pseudo attr ldap.baseDn
    
    private String CR_USER;
    private String CR_EMAIL;
    private String CR_ALIAS_USER;
    private String CR_ALIAS_EMAIL;
    
    private String DL_USER;
    private String DL_EMAIL;
    private String DL_ALIAS_USER;
    private String DL_ALIAS_EMAIL;
    private String DL_NESTED_USER;
    private String DL_NESTED_EMAIL;
    private String DL_NESTED_ALIAS_USER;
    private String DL_NESTED_ALIAS_EMAIL;
    
    private String DATA_SOURCE_NAME;
    private String IDENTITY_NAME;
    
    class CustomProvTester {
        boolean mIsCustomProv;
        
        CustomProvTester(Provisioning prov) {
            mIsCustomProv = (prov instanceof CustomLdapProvisioning);
        }
        
        public void addAttr(Map<String, Object> attrs, String pseudoAttr, String value) {
            if (!mIsCustomProv)
                return;
            
            attrs.put(pseudoAttr, value);
        }
        
        
        private void verifyDn(Entry entry, String dn) throws Exception {
            if (!mIsCustomProv)
                return;
            
            assertEquals(mLdapProv.getDN(entry), dn);
        }
    }

    public void setUp() throws Exception {
        mProv = Provisioning.getInstance();
        
        // if we are a ldapProvisioning, cast the prov obj and save it once here
        // ldapProv is used for several LDAP specific tests
        if (mProv instanceof LdapProvisioning)
            mLdapProv = (LdapProvisioning)mProv;
        
        mCustomProvTester = new CustomProvTester(mProv);
        
        mVisitor = new TestVisitor();
        
        Date date = new Date();
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
        TEST_ID = fmt.format(date);
        
        PRE_AUTH_KEY = PreAuthKey.generateRandomPreAuthKey();
        PASSWORD = "test123";
        
        COS_NAME = "cos-" + TEST_ID;
        DOMAIN_NAME = "domain-" + TEST_ID + ".ldap-test-domain";
        SERVER_NAME = "server-" + TEST_ID;
        ZIMLET_NAME = "zimlet-" + TEST_ID;
        
        NEW_NAME = "newname";
        NEW_EMAIL = NEW_NAME + "-" + TEST_ID + "@" + DOMAIN_NAME;
        
        DEFAULT_ADMIN_USER = LC.zimbra_ldap_user.value();
        ADMIN_USER =  "admin";
        ADMIN_EMAIL = ADMIN_USER + "@" + DOMAIN_NAME;
        ACCT_USER = "acct-1";
        ACCT_EMAIL = ACCT_USER + "@" + DOMAIN_NAME;
        ACCT_ALIAS_USER = "alias-of" + ACCT_USER;
        ACCT_ALIAS_EMAIL = ACCT_ALIAS_USER + "@" + DOMAIN_NAME;
        ACCT_FULL_NAME = "Phoebe Shao";
        
        ACCT_NAMING_ATTR = LC.get("ldap_account_naming_rdn_attr");
        if (StringUtil.isNullOrEmpty(ACCT_NAMING_ATTR))
            ACCT_NAMING_ATTR = "uid";
        ACCT_NAMING_ATTR_VALUE = ACCT_USER + ".mailuser";
        
        ACCT_BASE_DN = "ou=grp1,ou=mail,o=Comcast";
        
        CR_USER = "cr-1";
        CR_EMAIL = CR_USER + "@" + DOMAIN_NAME;
        CR_ALIAS_USER = "alias-of" + CR_USER;
        CR_ALIAS_EMAIL = CR_ALIAS_USER + "@" + DOMAIN_NAME;
        
        DL_USER = "dl-1";
        DL_EMAIL = DL_USER + "@" + DOMAIN_NAME;
        DL_ALIAS_USER = "alias-of" + DL_USER;
        DL_ALIAS_EMAIL = DL_ALIAS_USER + "@" + DOMAIN_NAME;
        DL_NESTED_USER = "dl-nested";
        DL_NESTED_EMAIL = DL_NESTED_USER + "@" + DOMAIN_NAME;
        DL_NESTED_ALIAS_USER = "alias-of" + DL_NESTED_USER;
        DL_NESTED_ALIAS_EMAIL = DL_NESTED_ALIAS_USER+ "@" + DOMAIN_NAME;
        
        DATA_SOURCE_NAME = "datasource-1";
        IDENTITY_NAME ="identity-1";
        
    }
    
    private class TestVisitor implements NamedEntry.Visitor {
        public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
            // do nothing  
        }
    }
    
    public static class TestCustomAuth extends ZimbraCustomAuth{
        
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
    
    
    private void verifySameEntry(NamedEntry entry1, NamedEntry entry2) throws Exception {
        assertNotNull(entry1);
        assertNotNull(entry2);
        assertEquals(entry1.getId(), entry2.getId());
    }
    
    // verify list contains exactly entries, no more no less
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
            System.out.println(e.getMessage());
            
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
    
    private Domain domainTest() throws Exception {
        System.out.println("Testing domain");
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, PRE_AUTH_KEY);
        Domain entry = mProv.createDomain(DOMAIN_NAME, attrs);
        
        Domain entryGot = mProv.get(Provisioning.DomainBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME);
        verifySameEntry(entry, entryGot);
        
        List list = mProv.getAllDomains();
        verifyEntries(list, new NamedEntry[]{entry}, false);
        
        return entry;
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
            assertEquals(e.getCode(), AccountServiceException.AUTH_FAILED);
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
        
        Account entry = mProv.get(Provisioning.AccountBy.adminName, DEFAULT_ADMIN_USER);
        assertNotNull(entry);
        entry = mProv.get(Provisioning.AccountBy.name, ADMIN_USER);
        assertNotNull(entry);
        
        List list = mProv.getAllAdminAccounts();
        verifyEntries(list, new NamedEntry[]{entry}, true);

        return entry;
    }
    
    
    // account and account aliases
    private Account accountTest(Cos cos, Domain domain) throws Exception {
        System.out.println("Testing account");
        
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        mCustomProvTester.addAttr(acctAttrs, "ldap.baseDn", ACCT_BASE_DN);
        mCustomProvTester.addAttr(acctAttrs, ACCT_NAMING_ATTR, ACCT_NAMING_ATTR_VALUE);

        acctAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        Account entry = mProv.createAccount(ACCT_EMAIL, PASSWORD, acctAttrs);
        mCustomProvTester.verifyDn(entry, ACCT_NAMING_ATTR + "=" + ACCT_NAMING_ATTR_VALUE + "," + ACCT_BASE_DN);
        
        mProv.addAlias(entry, ACCT_ALIAS_EMAIL);
        
        Account entryGot = mProv.get(Provisioning.AccountBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.AccountBy.name, ACCT_ALIAS_EMAIL);
        verifySameEntry(entry, entryGot);
                
        List list = mProv.getAllAccounts(domain);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        mProv.modifyAccountStatus(entry, "maintenance");
        mProv.modifyAccountStatus(entry, "active");

        mProv.removeAlias(entry, ACCT_ALIAS_EMAIL);

        mProv.renameAccount(entry.getId(), NEW_EMAIL);
        mProv.renameAccount(entry.getId(), ACCT_EMAIL);
        
        mProv.setCOS(entry, cos);
                
        return entry;
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
        verifyEntries(list, new NamedEntry[]{entry}, true);
        mProv.getAllCalendarResources(domain, mVisitor);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        mProv.renameCalendarResource(entry.getId(), NEW_EMAIL);
        mProv.renameCalendarResource(entry.getId(), CR_EMAIL);

        return entry;
    }
    
    // distribution list and distribution list alias
    private DistributionList[] distributionListTest(Domain domain) throws Exception {
        System.out.println("Testing distribution list");
        
        DistributionList entry = mProv.createDistributionList(DL_EMAIL, new HashMap<String, Object>());
        mProv.addAlias(entry, DL_ALIAS_EMAIL);
                
        DistributionList entryGot = mProv.get(Provisioning.DistributionListBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_EMAIL);
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(Provisioning.DistributionListBy.name, DL_ALIAS_EMAIL);
        verifySameEntry(entry, entryGot);
        
        DistributionList dlNested = mProv.createDistributionList(DL_NESTED_EMAIL, new HashMap<String, Object>());
        mProv.addAlias(dlNested, DL_NESTED_ALIAS_EMAIL);
        
        mProv.addMembers(entry, new String[]{DL_NESTED_EMAIL});
        mProv.addMembers(dlNested, new String[]{ACCT_EMAIL});
        
        List list = mProv.getAllDistributionLists(domain);
        verifyEntries(list, new NamedEntry[]{entry, dlNested}, true);
        
        Account account = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        Set<String> set = mProv.getDistributionLists(account);
        assertEquals(set.size(), 2);
        assertTrue(set.contains(entry.getId()));
        assertTrue(set.contains(dlNested.getId()));
        
        Map<String, String> via = new HashMap<String, String>();
        list = mProv.getDistributionLists(account, false, via);
        verifyEntries(list, new NamedEntry[]{entry, dlNested}, true);
        assertEquals(via.size(), 1);
        assertEquals(via.get(entry.getName()), dlNested.getName());
        
        list = mProv.getDistributionLists(account, true, null);
        verifyEntries(list, new NamedEntry[]{dlNested}, true);
        
        boolean inList = mProv.inDistributionList(account, entry.getId());
        assertTrue(inList);
                
        mProv.removeAlias(entry, DL_ALIAS_EMAIL);
        
        mProv.removeMembers(entry, new String[]{dlNested.getName()});

        mProv.renameDistributionList(entry.getId(), NEW_EMAIL);
        mProv.renameDistributionList(entry.getId(), DL_EMAIL);
                                
        return new DistributionList[]{entry, dlNested};
    }
    
    private DataSource dataSourceTest(Account account) throws Exception {
        System.out.println("Testing data source");
        
        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, "inbox");
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, "google.com");
        dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "TRUE");
        dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, PASSWORD);
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, "whatever");
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
        Identity entry = mProv.createIdentity(account, IDENTITY_NAME, identityAttrs);
        
        Identity entryGot = mProv.get(account, Provisioning.IdentityBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = mProv.get(account, Provisioning.IdentityBy.name, IDENTITY_NAME);
        verifySameEntry(entry, entryGot);
        Identity defaultIdentity = mProv.get(account, Provisioning.IdentityBy.name, Provisioning.DEFAULT_IDENTITY_NAME);
        verifySameEntry(account, defaultIdentity);
        
        List list = mProv.getAllIdentities(account);
        verifyEntries(list, new NamedEntry[]{defaultIdentity, entry}, true);
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraPrefBccAddress, "whatever");
        mProv.modifyIdentity(account, IDENTITY_NAME, attrsToMod);

        return entry;
    }
    
    private void entryTest(NamedEntry entry) throws Exception  {
        System.out.println("Testing entry");
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraId, "junk");
    
        try {
            mProv.modifyAttrs(entry, attrsToMod, true);
            fail("ServiceException.INVALID_REQUEST not thrown"); // should not come to here
        } catch (ServiceException e) {
            assertEquals(e.getCode(), ServiceException.INVALID_REQUEST);
        }
    
        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_displayName, ACCT_FULL_NAME);
        mProv.modifyAttrs(entry, attrsToMod, true, true);
        
        // make sure cn is also updated if cn is not the naing attribute
        if (mLdapProv != null) {
            String namingAttr = mLdapProv.getNamingRdnAttr(entry);
            if (!namingAttr.equals(Provisioning.A_cn)) {
                String cnValue = entry.getAttr(Provisioning.A_cn);
                assertEquals(cnValue, ACCT_FULL_NAME);
            }
        }
        
        mProv.reload(entry);
    }
    
    private void galTest(Domain domain) throws Exception {
        System.out.println("Testing gal");
        
        String query = ACCT_EMAIL.substring(0, 3);
        Account acct = mProv.get(Provisioning.AccountBy.name, ACCT_EMAIL);
        
        Provisioning.SearchGalResult galResult = mProv.autoCompleteGal(domain, 
                                                                      query,
                                                                      Provisioning.GAL_SEARCH_TYPE.ALL, 
                                                                      100);

        List<GalContact> matches = galResult.matches;
        assertEquals(matches.size(), 1);
        assertEquals(matches.get(0).getAttrs().get("fullName"), ACCT_FULL_NAME);
        
        galResult = mProv.searchGal(domain, 
                                   query,
                                   Provisioning.GAL_SEARCH_TYPE.ALL, 
                                   null);
        matches = galResult.matches;
        assertEquals(matches.size(), 1);
        assertEquals(matches.get(0).getAttrs().get("fullName"), ACCT_FULL_NAME);
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
        
        healthTest();
        Config config = configTest();
        Cos cos = cosTest();
        Domain domain = domainTest();
        mimeTest();
        Server server = serverTest();
        Zimlet zimlet = zimletTest();    
        
        Account adminAccount = adminAccountTest();
        Account account = accountTest(cos, domain);
        authTest(account);
        passwordTest(account);
        CalendarResource calendarResource = calendarResourceTest(cos, domain);
        DistributionList[] distributionLists = distributionListTest(domain);
        DataSource dataSource = dataSourceTest(account);
        Identity identity = identityTest(account);
        
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
        mProv.deleteDistributionList(distributionLists[1].getId());
        mProv.deleteDistributionList(distributionLists[0].getId());
        mProv.deleteCalendarResource(calendarResource.getId());
        mProv.deleteAccount(account.getId());
        mProv.deleteDomain(domain.getId());
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
