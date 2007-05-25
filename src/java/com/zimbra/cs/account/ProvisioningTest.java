package com.zimbra.cs.account;

import java.lang.AssertionError;
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.PreAuthKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

public class ProvisioningTest {
    
    private Provisioning prov;
    private TestVisitor visitor;
    
    private String testId;
    
    private String preAuthKey;
    private String password;
    
    private String cosName;
    private String domainName;
    private String serverName;
    private String zimletName;
    
    private String newName;  // for testing rename
    private String newEmail; // for testing rename
    
    private String defaultAdminUser;
    private String adminUser;
    private String adminEmail;
    private String acctUser;
    private String acctEmail;
    private String acctAliasUser;
    private String acctAliasEmail;
    private String acctFullName;
    
    private String crUser;
    private String crEmail;
    private String crAliasUser;
    private String crAliasEmail;
    
    private String dlUser;
    private String dlEmail;
    private String dlAliasUser;
    private String dlAliasEmail;
    private String dlNestedUser;
    private String dlNestedEmail;
    private String dlNestedAliasUser;
    private String dlNestedAliasEmail;
    
    private String dataSourceName;
    private String identityName;

    
    ProvisioningTest(String tid) throws ServiceException {
        prov = Provisioning.getInstance();
        visitor = new TestVisitor();
        
        if (tid == null) {
            Date date = new Date();
            SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
            testId = fmt.format(date);
        } else
            testId = tid;
        
        preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        password = "test123";
        
        cosName = "cos-" + testId;
        domainName = "domain-" + testId + ".ldap-test-domain";
        serverName = "server-" + testId;
        zimletName = "zimlet-" + testId;
        
        newName = "newname";
        newEmail = newName + "-" + testId + "@" + domainName;
        
        defaultAdminUser = "zimbra";
        adminUser =  "admin";
        adminEmail = adminUser + "@" + domainName;
        acctUser = "acct-1";
        acctEmail = acctUser + "@" + domainName;
        acctAliasUser = "alias-of" + acctUser;
        acctAliasEmail = acctAliasUser + "@" + domainName;
        acctFullName = "Phoebe Shao";
        
        crUser = "cr-1";
        crEmail = "cr-1" + "@" + domainName;
        crAliasUser = "alias-of" + crUser;
        crAliasEmail = crAliasUser + "@" + domainName;
        
        dlUser = "dl-1";
        dlEmail = dlUser + "@" + domainName;
        dlAliasUser = "alias-of" + dlUser;
        dlAliasEmail = dlAliasUser + "@" + domainName;
        dlNestedUser = "dl-nested";
        dlNestedEmail = dlNestedUser + "@" + domainName;
        dlNestedAliasUser = "alias-of" + dlNestedUser;
        dlNestedAliasEmail = dlNestedAliasUser+ "@" + domainName;
        
        dataSourceName = "datasource-1";
        identityName ="identity-1";
        
    }
    
    private class TestVisitor implements NamedEntry.Visitor {
        public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
            // do nothing  
        }
    }
    
    // use ensure onstead of assert so the checking is independent of command-line switches and 
    // is always effective
    private void ensure(boolean ok) {
        if (!ok)
            throw new java.lang.AssertionError();
    }
    
    private void verifySameEntry(NamedEntry entry1, NamedEntry entry2) {
        try {
            ensure(entry1 != null && entry2 != null);
            ensure(entry1.getId().equals(entry2.getId()));
        } catch (java.lang.AssertionError e) {
            System.out.println("\n===== verifySameEntry failed =====");
            System.out.println((entry1 == null)? "entry is null":entry1.getId());
            System.out.println((entry2 == null)? "entry is null":entry2.getId());
            
            throw e;
        }
    }
    
    // verify list contains exactly entries, no more no less
    private void verifyEntries(List<NamedEntry> list, NamedEntry[] entries, boolean checkCount) {
        try {
            if (checkCount)
                ensure(list.size() == entries.length);
        
            Set<String> ids = new HashSet<String>();
            for (NamedEntry entry : list)
                ids.add(entry.getId());
            
            for (NamedEntry entry : entries)
                ensure(ids.contains(entry.getId()));
         
        } catch (java.lang.AssertionError e) {
            System.out.println("\n===== verifyEntries failed =====");
            System.out.println("list contains " + list.size() + " entries:");
            for (NamedEntry entry : list)
                System.out.println("    " + entry.getName());
            System.out.println("entries contains " + entries.length + " entries:");
            for (NamedEntry entry : entries)
                System.out.println("    " + entry.getName());
            
            System.out.println();
            throw e;
        }
    }
    
    private void testHealth() throws ServiceException {
        System.out.println("Testing health");
        
        prov.healthCheck();
    }

    private Config testConfig() throws ServiceException {
        System.out.println("Testing config");
        
        Config entry = prov.getConfig();
        ensure(entry != null);
        
        return entry;
    }
    
    private Cos testCos() throws ServiceException {
        System.out.println("Testing cos");
        
        Cos entry = prov.createCos(cosName, new HashMap<String, Object>());
        
        Cos entryGot = prov.get(Provisioning.CosBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.CosBy.name, cosName);
        verifySameEntry(entry, entryGot);
        Cos defaultCos = prov.get(Provisioning.CosBy.name, "default");
        ensure(defaultCos != null);
        
        List list = prov.getAllCos();
        verifyEntries(list, new NamedEntry[]{defaultCos, entry}, false);
        
        prov.renameCos(entry.getId(), newName);
        prov.renameCos(entry.getId(), cosName);
                        
        return entry;
    }
    
    private Domain testDomain() throws ServiceException {
        System.out.println("Testing domain");
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        Domain entry = prov.createDomain(domainName, attrs);
        
        Domain entryGot = prov.get(Provisioning.DomainBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.DomainBy.name, domainName);
        verifySameEntry(entry, entryGot);
        
        List list = prov.getAllDomains();
        verifyEntries(list, new NamedEntry[]{entry}, false);
        
        return entry;
    }
    
    private void testMime() throws ServiceException {
        System.out.println("Testing mime");
        
        MimeTypeInfo mime = prov.getMimeType("all"); 
        ensure(mime != null);
        
        mime = prov.getMimeTypeByExtension("text");
        ensure(mime != null);
    }

    
    private Server testServer() throws ServiceException {
        System.out.println("Testing server");
        
        Map<String, Object> serverAttrs = new HashMap<String, Object>();
        Server entry = prov.createServer(serverName, serverAttrs);
        
        Server entryGot = prov.get(Provisioning.ServerBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.ServerBy.name, serverName);
        verifySameEntry(entry, entryGot);
        
        Server localeServer = prov.getLocalServer();
        ensure(localeServer != null);
        
        List list = prov.getAllServers();
        verifyEntries(list, new NamedEntry[]{localeServer, entry}, false);
        
        list = prov.getAllServers("mailbox");
        verifyEntries(list, new NamedEntry[]{localeServer}, false);
        
        return entry;
    }
    
    private Zimlet testZimlet() throws ServiceException {
        System.out.println("Testing zimlet");
        
        Map<String, Object> zimletAttrs = new HashMap<String, Object>();
        zimletAttrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
        Zimlet entry = prov.createZimlet(zimletName, zimletAttrs);
        
        Zimlet entryGot = prov.getZimlet(zimletName);
        verifySameEntry(entry, entryGot);
        
        List list = prov.getObjectTypes();
        verifyEntries(list, new NamedEntry[]{entry}, false);
        
        list = prov.listAllZimlets();
        verifyEntries(list, new NamedEntry[]{entry}, false);
            
        return entry;
    }

    private void testAuth(Account account) throws ServiceException  {
        System.out.println("Testing auth");
        
        prov.authAccount(account, password, null);
         
        HashMap<String,String> params = new HashMap<String,String>();
        String authBy = "name";
        long timestamp = System.currentTimeMillis();
        long expires = 0;
        params.put("account", acctEmail);
        params.put("by", authBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        String preAuth = PreAuthKey.computePreAuth(params, preAuthKey);
        prov.preAuthAccount(account, 
                            acctEmail,     // account name 
                            authBy,        // by
                            timestamp,     // timestamp
                            0,             // expires
                            preAuth);   // preauth key
           
    }
    
    private Account testAdminAccount() throws ServiceException {
        System.out.println("Testing admin account");
        
        Account entry = prov.get(Provisioning.AccountBy.adminName, defaultAdminUser);
        ensure(entry != null);
        entry = prov.get(Provisioning.AccountBy.name, adminUser);
        ensure(entry != null);
        
        List list = prov.getAllAdminAccounts();
        verifyEntries(list, new NamedEntry[]{entry}, true);

        return entry;
    }
    
    // account and account aliases
    private Account testAccount(Cos cos, Domain domain) throws ServiceException {
        System.out.println("Testing account");
        
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        Account entry = prov.createAccount(acctEmail, password, acctAttrs);
        prov.addAlias(entry, acctAliasEmail);
        
        Account entryGot = prov.get(Provisioning.AccountBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.AccountBy.name, acctEmail);
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.AccountBy.name, acctAliasEmail);
        verifySameEntry(entry, entryGot);
                
        List list = prov.getAllAccounts(domain);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        prov.modifyAccountStatus(entry, "maintenance");
        prov.modifyAccountStatus(entry, "active");

        prov.removeAlias(entry, acctAliasEmail);

        prov.renameAccount(entry.getId(), newEmail);
        prov.renameAccount(entry.getId(), acctEmail);
        
        prov.setCOS(entry, cos);
                
        return entry;
    }
    
    private void testPassword(Account account) throws ServiceException {
        System.out.println("Testing password");
        
        prov.changePassword(account, password, password);
        prov.checkPasswordStrength(account, password);
        prov.setPassword(account, password);
    }
    
    // calendar resource and calendar resource alias
    private CalendarResource testCalendarResource(Cos cos, Domain domain) throws ServiceException {
        System.out.println("Testing calendar resource");
        
        Map<String, Object> crAttrs = new HashMap<String, Object>();
        crAttrs.put(Provisioning.A_displayName, crUser);
        crAttrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        crAttrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        CalendarResource entry = prov.createCalendarResource(crEmail, password, crAttrs);
        prov.addAlias(entry, crAliasEmail);
        
        CalendarResource entryGot = prov.get(Provisioning.CalendarResourceBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.CalendarResourceBy.name, crEmail);
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.CalendarResourceBy.name, crAliasEmail);
        verifySameEntry(entry, entryGot);
        
        List list = prov.getAllCalendarResources(domain);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        prov.getAllCalendarResources(domain, visitor);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        prov.renameCalendarResource(entry.getId(), newEmail);
        prov.renameCalendarResource(entry.getId(), crEmail);

        return entry;
    }
    
    // distribution list and distribution list alias
    private DistributionList[] testDistributionList(Domain domain) throws ServiceException {
        System.out.println("Testing distribution list");
        
        DistributionList entry = prov.createDistributionList(dlEmail, new HashMap<String, Object>());
        prov.addAlias(entry, dlAliasEmail);
                
        DistributionList entryGot = prov.get(Provisioning.DistributionListBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.DistributionListBy.name, dlEmail);
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(Provisioning.DistributionListBy.name, dlAliasEmail);
        verifySameEntry(entry, entryGot);
        
        DistributionList dlNested = prov.createDistributionList(dlNestedEmail, new HashMap<String, Object>());
        prov.addAlias(dlNested, dlNestedAliasEmail);
        
        prov.addMembers(entry, new String[]{dlNestedEmail});
        prov.addMembers(dlNested, new String[]{acctEmail});
        
        List list = prov.getAllDistributionLists(domain);
        verifyEntries(list, new NamedEntry[]{entry, dlNested}, true);
        
        Account account = prov.get(Provisioning.AccountBy.name, acctEmail);
        Set<String> set = prov.getDistributionLists(account);
        ensure(set.size() == 2);
        ensure(set.contains(entry.getId()));
        ensure(set.contains(dlNested.getId()));
        
        Map<String, String> via = new HashMap<String, String>();
        list = prov.getDistributionLists(account, false, via);
        verifyEntries(list, new NamedEntry[]{entry, dlNested}, true);
        ensure(via.size()==1);
        ensure(via.get(entry.getName()).equals(dlNested.getName()));
        
        list = prov.getDistributionLists(account, true, null);
        verifyEntries(list, new NamedEntry[]{dlNested}, true);
        
        boolean inList = prov.inDistributionList(account, entry.getId());
        ensure(inList);
                
        prov.removeAlias(entry, dlAliasEmail);
        
        prov.removeMembers(entry, new String[]{dlNested.getName()});

        prov.renameDistributionList(entry.getId(), newEmail);
        prov.renameDistributionList(entry.getId(), dlEmail);
                                
        return new DistributionList[]{entry, dlNested};
    }
    
    private DataSource testDataSource(Account account) throws ServiceException {
        System.out.println("Testing data source");
        
        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        dsAttrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
        dsAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId, "inbox");
        dsAttrs.put(Provisioning.A_zimbraDataSourceHost, "google.com");
        dsAttrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "TRUE");
        dsAttrs.put(Provisioning.A_zimbraDataSourcePassword, password);
        dsAttrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        dsAttrs.put(Provisioning.A_zimbraDataSourceUsername, "whatever");
        DataSource entry = prov.createDataSource(account, DataSource.Type.pop3, dataSourceName, dsAttrs);

        DataSource entryGot = prov.get(account, Provisioning.DataSourceBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(account, Provisioning.DataSourceBy.name, dataSourceName);
        verifySameEntry(entry, entryGot);
        
        List list = prov.getAllDataSources(account);
        verifyEntries(list, new NamedEntry[]{entry}, true);
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraDataSourcePollingInterval, "100");
        prov.modifyDataSource(account, entry.getId(), attrsToMod);
               
        return entry;
    }
    
    private Identity testIdentity(Account account) throws ServiceException {
        System.out.println("Testing identity");
        
        Map<String, Object> identityAttrs = new HashMap<String, Object>();
        Identity entry = prov.createIdentity(account, identityName, identityAttrs);
        
        Identity entryGot = prov.get(account, Provisioning.IdentityBy.id, entry.getId());
        verifySameEntry(entry, entryGot);
        entryGot = prov.get(account, Provisioning.IdentityBy.name, identityName);
        verifySameEntry(entry, entryGot);
        Identity defaultIdentity = prov.get(account, Provisioning.IdentityBy.name, Provisioning.DEFAULT_IDENTITY_NAME);
        verifySameEntry(account, defaultIdentity);
        
        List list = prov.getAllIdentities(account);
        verifyEntries(list, new NamedEntry[]{defaultIdentity, entry}, true);
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraPrefBccAddress, "whatever");
        prov.modifyIdentity(account, identityName, attrsToMod);

        return entry;
    }
    
    private void testEntry(NamedEntry entry) throws ServiceException  {
        System.out.println("Testing entry");
        
        Map attrsToMod = new HashMap<String, Object>();
        attrsToMod.put(Provisioning.A_zimbraId, "junk");
    
        try {
            prov.modifyAttrs(entry, attrsToMod, true);
            ensure(false); // should not come to here
        } catch (ServiceException e) {
            ensure(e.getCode().equals(ServiceException.INVALID_REQUEST));
        }
    
        attrsToMod.clear();
        attrsToMod.put(Provisioning.A_displayName, acctFullName);
        prov.modifyAttrs(entry, attrsToMod, true, true);
        
        prov.reload(entry);
    }
    
    private void testGal(Domain domain) throws ServiceException {
        System.out.println("Testing gal");
        
        String query = acctEmail.substring(0, 3);
        Account acct = prov.get(Provisioning.AccountBy.name, acctEmail);
        
        Provisioning.SearchGalResult galResult = prov.autoCompleteGal(domain, 
                                                                      query,
                                                                      Provisioning.GAL_SEARCH_TYPE.ALL, 
                                                                      100);

        List<GalContact> matches = galResult.matches;
        ensure(matches.size() == 1);
        ensure(matches.get(0).getAttrs().get("fullName").equals(acctFullName));
        
        galResult = prov.searchGal(domain, 
                                   query,
                                   Provisioning.GAL_SEARCH_TYPE.ALL, 
                                   null);
        matches = galResult.matches;
        ensure(matches.size() == 1);
        ensure(matches.get(0).getAttrs().get("fullName").equals(acctFullName));
    }
    
    private void testSearch(Domain domain) throws ServiceException {
        System.out.println("Testing search");
        
        Account acct = prov.get(Provisioning.AccountBy.name, acctEmail);
        Account cr = prov.get(Provisioning.AccountBy.name, crEmail);
        
        String query = "(" + Provisioning.A_zimbraMailDeliveryAddress + "=" + acctEmail + ")";
        List list = prov.searchAccounts(query, 
                                        new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                        Provisioning.A_zimbraMailDeliveryAddress, 
                                        true,
                                        Provisioning.SA_ACCOUNT_FLAG); 
        verifyEntries(list, new NamedEntry[]{acct}, true);
               
        list = prov.searchAccounts(domain, query, 
                                   new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                   Provisioning.A_zimbraMailDeliveryAddress, 
                                   true,
                                   Provisioning.SA_ACCOUNT_FLAG); 
        verifyEntries(list, new NamedEntry[]{acct}, true);
        
        EntrySearchFilter.Term term = new EntrySearchFilter.Single(false, 
                                                                   Provisioning.A_zimbraMailDeliveryAddress, 
                                                                   EntrySearchFilter.Operator.eq,
                                                                   crEmail);
        EntrySearchFilter filter = new EntrySearchFilter(term);
        list = prov.searchCalendarResources(filter,
                                            new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                            Provisioning.A_zimbraMailDeliveryAddress, 
                                            true);
        verifyEntries(list, new NamedEntry[]{cr}, true);       
        
        list = prov.searchCalendarResources(domain,
                                            filter,
                                            new String[]{Provisioning.A_zimbraMailDeliveryAddress}, 
                                            Provisioning.A_zimbraMailDeliveryAddress, 
                                            true);
        verifyEntries(list, new NamedEntry[]{cr}, true);
        
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();
        options.setDomain(domain);
        options.setQuery(query);
        list = prov.searchDirectory(options);
        verifyEntries(list, new NamedEntry[]{acct}, true);
        
    }
    
    public String execute() throws ServiceException, IOException {
        
        testHealth();
        Config config = testConfig();
        Cos cos = testCos();
        Domain domain = testDomain();
        testMime();
        Server server = testServer();
        Zimlet zimlet = testZimlet();    
        
        Account adminAccount = testAdminAccount();
        Account account = testAccount(cos, domain);
        testAuth(account);
        testPassword(account);
        CalendarResource calendarResource = testCalendarResource(cos, domain);
        DistributionList[] distributionLists = testDistributionList(domain);
        DataSource dataSource = testDataSource(account);
        Identity identity = testIdentity(account);
        
        testEntry(account);
        testGal(domain);
        testSearch(domain);

        // ========================================================================
        System.out.println("\nPress enter to delete entries created by the test");
        
        String line = (new BufferedReader(new InputStreamReader(System.in))).readLine();
        System.out.println("\nDeleting entries");
        
        prov.deleteZimlet(zimletName);
        prov.deleteServer(server.getId());
        prov.deleteIdentity(account, identityName);
        prov.deleteDataSource(account, dataSource.getId());
        prov.deleteDistributionList(distributionLists[1].getId());
        prov.deleteDistributionList(distributionLists[0].getId());
        prov.deleteCalendarResource(calendarResource.getId());
        prov.deleteAccount(account.getId());
        prov.deleteDomain(domain.getId());
        prov.deleteCos(cos.getId());
        
        return testId;
    }
    
    public static void main(String[] args) throws IOException {
        String testId = (args.length > 0)? args[0] : null;
        
        try {
            ProvisioningTest provTest = new ProvisioningTest(testId);
            testId = provTest.execute();
            System.out.println("\nTest " + testId + " done!");
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            System.out.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                    (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
            e.printStackTrace(System.out);
            System.out.println("\nTest " + testId + " failed!");
        } 
        
    }
}
