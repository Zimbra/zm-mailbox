package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.common.net.SocketFactories;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.mailbox.ACL;

public abstract class TestProv extends TestCase {

    protected static final String TEST_ID = TestProvisioningUtil.genTestId();
    protected static final String BASE_DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-prov", TEST_ID);
    protected static final String PASSWORD = "test123";
    
    protected static Provisioning sSoapProv;
    protected static Provisioning sLdapProv;
    
    private static int sSequence = 1;
    
    
    List<NamedEntry> mCreatedEntries = new ArrayList<NamedEntry>();
    
    // add domains in a seperate list, so they are deleted, after all domained 
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
    
    protected Account createDelegatedAdminAccount(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        return createAccount(localpart, domain, attrs);
    }
    
    protected Account createGuestAccount(String email, String password) {
        return new ACL.GuestAccount(email, password);
    }
    
    protected Account createKeyAccount(String name, String accesKey) {
        AuthToken authToken = new TestACAccessKey.KeyAuthToken(name, accesKey);
        return new ACL.GuestAccount(authToken);
    }
    
    protected Account anonAccount() {
        return ACL.ANONYMOUS_ACCT;
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
    
    protected Cos createCos() throws Exception {
        Cos cos = mProv.createCos(genCosName(), null);
        mCreatedEntries.add(cos);
        return cos;
    }
    
    private DistributionList createGroup(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        DistributionList dl = mProv.createDistributionList(email, attrs);
        mCreatedEntries.add(dl);
        return dl;
    }
    
    protected DistributionList createUserGroup(String localpart, Domain domain) throws Exception {
        return createGroup(localpart, domain, new HashMap<String, Object>());
    }
    
    protected DistributionList createAdminGroup(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
        return createGroup(localpart, domain, attrs);
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
        entries[0] = new CacheEntry(CacheEntryBy.name, acct.getName());
        mProv.flushCache(CacheEntryType.account, entries);
    }
    
    private void deleteEntry(NamedEntry entry) throws Exception {
        if (entry instanceof Account)
            mProv.deleteAccount(entry.getId());
        else if (entry instanceof CalendarResource)
            mProv.deleteCalendarResource(entry.getId());
        else if (entry instanceof Cos)
            mProv.deleteCos(entry.getId());
        else if (entry instanceof DistributionList)
            mProv.deleteDistributionList(entry.getId());
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
