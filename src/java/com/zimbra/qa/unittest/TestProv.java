package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.qa.unittest.prov.ldap.ACLTestUtil;
import com.zimbra.soap.admin.type.CacheEntryType;

public abstract class TestProv extends TestLdap {

    protected static final String TEST_ID = TestProvisioningUtil.genTestId();
    protected static final String BASE_DOMAIN_NAME = TestLdap.baseDomainName(TestProv.class); // TestProvisioningUtil.baseDomainName("test-prov", TEST_ID);
    protected static final String PASSWORD = "test123";
    
    protected static Provisioning sSoapProv;
    protected static Provisioning sLdapProv;
    
    private static int sSequence = 1;
    
    
    List<NamedEntry> mCreatedEntries = new ArrayList<NamedEntry>();
    
    // add domains in a separate list, so they are deleted, after all domain-ed 
    // entries are deleted, or else will get domain not empty exception
    // TODO: need to handle subdomains - those needed to be deleted before parent domains or
    //       else won't get deleted.  For now just go in LDAP and delete the test root directly.
    List<NamedEntry> mCreatedDomains = new ArrayList<NamedEntry>();
    
    protected Provisioning mProv = sLdapProv;  // use LdapProvisioning by default
    
    static {
        try {
            SocketFactories.registerProtocols();
            sSoapProv = TestProvisioningUtil.getSoapProvisioning();
            sLdapProv = TestProvisioningUtil.getLdapProvisioning();
        } catch (Exception e) {
            System.out.println("*** init failed ***");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    protected void useSoapProv() {
        mProv = sSoapProv;
    }
    
    protected void useLdapProv() {
        mProv = sLdapProv;
    }
   
    
    private static synchronized String nextSeq() {
        return "" + sSequence++;
    }
    
    private String genDomainName() {
        return nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genAccountName() {
        return "acct-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genCalendarResourceName() {
        return "cr-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genDistributionListName() {
        return "dl-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genDynamicGroupName() {
        return "group-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genCosName() {
        return "cos-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genServerName() {
        return "server-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genXMPPComponentName() {
        return "xmpp-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genZimletName() {
        return "zimlet-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    protected String getEmailLocalpart(Account acct) {
        return getEmailLocalpart(acct.getName());
    }
    
    protected String getEmailLocalpart(DistributionList dl) {
        return getEmailLocalpart(dl.getName());
    }
    
    protected String getEmailLocalpart(String email) {
        String[] parts = email.split("@");
        return parts[0];
    }
    
    protected Account getGlobalAdminAcct() throws ServiceException {
        return mProv.get(AccountBy.name, TestUtil.getAddress("admin"));
    }
    
    protected Domain createDomain() throws Exception {
        Domain domain = mProv.createDomain(genDomainName(), new HashMap<String, Object>());
        mCreatedDomains.add(domain);
        return domain;
    }
    
    protected Account createAccount(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        Account acct = mProv.createAccount(email, PASSWORD, attrs);
        mCreatedEntries.add(acct);
        return acct;
    }
    
    protected Account createUserAccount(String localpart, Domain domain) throws Exception {
        return createAccount(localpart, domain, null);
    }
    
    protected Account createUserAccount(Domain domain) throws Exception {
        String localpart = genAccountName();
        return createAccount(localpart, domain, null);
    }
    
    protected Account createDelegatedAdminAccount(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, ProvisioningConstants.TRUE);
        return createAccount(localpart, domain, attrs);
    }
    
    protected Account createDelegatedAdminAccount(Domain domain) throws Exception {
        String localpart = genAccountName();
        return createDelegatedAdminAccount(localpart, domain);
    }
    
    protected Account createGuestAccount(String email, String password) {
        return new GuestAccount(email, password);
    }
    
    protected Account createKeyAccount(String name, String accesKey) {
        AuthToken authToken = new ACLTestUtil.KeyAuthToken(name, accesKey);
        return new GuestAccount(authToken);
    }
    
    protected Account anonAccount() {
        return GuestAccount.ANONYMOUS_ACCT;
    }
    
    protected CalendarResource createCalendarResource(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, localpart);
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        
        String email = localpart + "@" + domain.getName();
        CalendarResource cr = mProv.createCalendarResource(email, PASSWORD, attrs);
        mCreatedEntries.add(cr);
        return cr;
    }
    
    protected CalendarResource createCalendarResource(Domain domain) throws Exception {
        String localpart = genCalendarResourceName();
        return createCalendarResource(localpart, domain);
    }
    
    protected Cos createCos() throws Exception {
        Cos cos = mProv.createCos(genCosName(), null);
        mCreatedEntries.add(cos);
        return cos;
    }
    
    private DistributionList createDistributionList(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        DistributionList dl = mProv.createDistributionList(email, attrs);
        mCreatedEntries.add(dl);
        return dl;
    }
    
    protected DistributionList createUserDistributionList(String localpart, Domain domain) throws Exception {
        return createDistributionList(localpart, domain, new HashMap<String, Object>());
    }
    
    protected DistributionList createUserDistributionList(Domain domain) throws Exception {
        String localpart = genDistributionListName();
        return createUserDistributionList(localpart, domain);
    }
    
    protected DistributionList createAdminDistributionList(String localpart, Domain domain) 
    throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, ProvisioningConstants.TRUE);
        return createDistributionList(localpart, domain, attrs);
    }
    
    protected DistributionList createAdminDistributionList(Domain domain) throws Exception {
        String localpart = genDistributionListName();
        return createAdminDistributionList(localpart, domain);
    }
    
    private DynamicGroup createDynamicGroup(String localpart, Domain domain, Map<String, Object> attrs) 
    throws Exception {
        if (domain == null) {
            domain = createDomain();
        }
        
        String email = localpart + "@" + domain.getName();
        DynamicGroup dynGroup = mProv.createDynamicGroup(email, attrs);
        mCreatedEntries.add(dynGroup);
        return dynGroup;
    }
    
    protected DynamicGroup createUserDynamicGroup(String localpart, Domain domain) throws Exception {
        return createDynamicGroup(localpart, domain, new HashMap<String, Object>());
    }
    
    protected DynamicGroup createUserDynamicGroup(Domain domain) throws Exception {
        String localpart = genDynamicGroupName();
        return createUserDynamicGroup(localpart, domain);
    }
    
    protected DynamicGroup createAdminDynamicGroup(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, ProvisioningConstants.TRUE);
        return createDynamicGroup(localpart, domain, attrs);
    }
    
    protected DynamicGroup createAdminDynamicGroup(Domain domain) throws Exception {
        String localpart = genDynamicGroupName();
        return createAdminDynamicGroup(localpart, domain);
    }
    
    protected Server createServer() throws Exception {
        Server server = mProv.createServer(genServerName(), new HashMap<String, Object>());
        mCreatedEntries.add(server);
        return server;
    }
    
    /*
    protected Server createXMPPComponent() throws Exception {
        return sProv.createXMPPComponent(genXMPPComponentName(), null);
    }
    */
    
    protected Zimlet createZimlet() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
        Zimlet zimlet = mProv.createZimlet(genZimletName(), attrs);
        mCreatedEntries.add(zimlet);
        return zimlet;
    }
    
    protected void flushCache(Account acct) throws Exception {
        CacheEntry[] entries = new CacheEntry[1];
        entries[0] = new CacheEntry(Key.CacheEntryBy.name, acct.getName());
        mProv.flushCache(CacheEntryType.account, entries);
    }
    
    private void deleteEntry(NamedEntry entry) throws Exception {
        if (entry instanceof Account)
            mProv.deleteAccount(entry.getId());
        else if (entry instanceof CalendarResource)
            mProv.deleteCalendarResource(entry.getId());
        else if (entry instanceof Cos)
            mProv.deleteCos(entry.getId());
        else if (entry instanceof Group)
            mProv.deleteGroup(entry.getId());
        else if (entry instanceof Domain)
            mProv.deleteDomain(entry.getId());
        else if (entry instanceof Server)
            mProv.deleteServer(entry.getId());
        else if (entry instanceof Zimlet)
            mProv.deleteZimlet(entry.getName());
        else
            throw new Exception("not yet implemented");
            
    }
    
    // delete all non-domained entries
    // for domained entries, it is faster to go in LDAP and just delete the domain root
    protected void deleteAllEntries() throws Exception {
        for (NamedEntry entry : mCreatedEntries)
            deleteEntry(entry);
        mCreatedEntries.clear();
        
        for (NamedEntry entry : mCreatedDomains)
            deleteEntry(entry);
        mCreatedDomains.clear();
    }
}
