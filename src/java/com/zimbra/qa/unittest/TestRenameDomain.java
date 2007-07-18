package com.zimbra.qa.unittest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestRenameDomain  extends TestCase {
    private Provisioning mProv;
    private String TEST_ID;
    
    private String PASSWORD;
    private int NUM_ACCOUNTS;
    private int NUM_TOP_DLS;
    private int NUM_NESTED_DLS;
    private int NUM_SIGNATURES;
    private int NUM_IDENTITIES;
    private int NUM_DATASOURCES;
    
    private String ACCOUNT_NAMEPREFIX;
    private String TOP_DL_NAMEPREFIX;
    private String NESTED_DL_NAMEPREFIX;
    private String ALIAS_NAMEPREFIX;
    private String SIGNATURE_NAMEPREFIX;
    private String IDENTITY_NAMEPREFIX;
    private String DATASOURCE_NAMEPREFIX;
    
     
    private String OLD_DOMAIN_NAME;
    private String NEW_DOMAIN_NAME;
    private String OTHER_DOMAIN_NAME;
    
    public void setUp() throws Exception {
        
        Date date = new Date();
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
        TEST_ID = fmt.format(date);
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        
        mProv = Provisioning.getInstance();
        assertTrue(mProv instanceof LdapProvisioning);
        
        PASSWORD = "test123";
        NUM_ACCOUNTS = 3;
        NUM_TOP_DLS = 2;
        NUM_NESTED_DLS = 2;
        
        /*
         * NUM_IDENTITIES and NUM_DATASOURCES must be >= NUM_SIGNATURES, each identity/datasource get one signature of the same index.
         * identities and datasources that do not have corresponding signature will get no signature. 
         * e.g. 
         *     identity-1 -> signature-1
         *     identity-2 -> signature-2 
         *     identity-3 -> (no signature)
         * 
         */ 
        NUM_SIGNATURES = 2; 
        NUM_IDENTITIES = 2;
        NUM_DATASOURCES = 2;
        
        ACCOUNT_NAMEPREFIX = "acct";
        TOP_DL_NAMEPREFIX = "top-dl";
        NESTED_DL_NAMEPREFIX = "nested-dl";
        ALIAS_NAMEPREFIX = "alias";
        SIGNATURE_NAMEPREFIX = "signature";
        IDENTITY_NAMEPREFIX = "identity";
        DATASOURCE_NAMEPREFIX = "datasource";
                
        OLD_DOMAIN_NAME   = "old-domain" + ".test-" + TEST_ID + ".ldap-test-domain";
        NEW_DOMAIN_NAME   = "new-domain" + ".test-" + TEST_ID + ".ldap-test-domain";
        OTHER_DOMAIN_NAME = "other-domain" + ".test-" + TEST_ID + ".ldap-test-domain";
        
        
        /*
         * Create 2 domains: domain to be renamed, and one other domain.  
         *     - Each domain:
         *           - has NUM_ACCOUNTS accounts
         *           - has NUM_TOP_DLS top level dls (dl that is not a nested DL)
         *           - has NUM_NESTED_DLS nested dls (dl under another DL)
         * 
         *     - Each account:
         *           - has two aliases, one in the same domain, one in diff domain
         *           - is a member of all DLs in all domains
         *           
         *     - Each top dl:
         *           - has two aliases, one in the same domain, one in diff domain
         *                 
         *     - Each nested dl:
         *           - has two aliases, one in the same domain, one in diff domain
         *           - is a member of all DLs in all domains
         */
        
        createDomain(OLD_DOMAIN_NAME);
        createDomain(OTHER_DOMAIN_NAME);
        
        // TODO, 3 domains, identity, signature, data source, cross link
        
        setupDomain(OLD_DOMAIN_NAME, OTHER_DOMAIN_NAME);
        setupDomain(OTHER_DOMAIN_NAME, OLD_DOMAIN_NAME);
        
        crossLinkDomain(OLD_DOMAIN_NAME, OTHER_DOMAIN_NAME);
        crossLinkDomain(OTHER_DOMAIN_NAME, OLD_DOMAIN_NAME);
    }
    
    private String accountName(int index, String domainName) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        int idx = index+1;
        return ACCOUNT_NAMEPREFIX + "-" + idx + "@" + domainName;
    }
    
    private String accountAlias(int index, String aliasInDomain, String targetInDomain) {
        if (aliasInDomain.equals(targetInDomain))
            return ALIAS_NAMEPREFIX + "-" + accountName(index, aliasInDomain);
        else
            return ALIAS_NAMEPREFIX + "-" + targetInDomain + accountName(index, aliasInDomain);
    }
    
    private String topDLName(int index, String domainName) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        int idx = index+1;
        return TOP_DL_NAMEPREFIX + "-" + idx + "@" + domainName;
    }
    
    private String topDLAlias(int index, String aliasInDomain, String targetInDomain) {
        if (aliasInDomain.equals(targetInDomain))
            return ALIAS_NAMEPREFIX + "-" + topDLName(index, aliasInDomain);
        else
            return ALIAS_NAMEPREFIX + "-" + targetInDomain + topDLName(index, aliasInDomain);
    }
    
    private String nestedDLName(int index, String domainName) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        int idx = index+1;
        return NESTED_DL_NAMEPREFIX + "-" + idx + "@" + domainName;
    }
    
    private String nestedDLAlias(int index, String aliasInDomain, String targetInDomain) {
        if (aliasInDomain.equals(targetInDomain))
            return ALIAS_NAMEPREFIX + "-" + nestedDLName(index, aliasInDomain);
        else
            return ALIAS_NAMEPREFIX + "-" + targetInDomain + nestedDLName(index, aliasInDomain);
    }
    
    private String signatureName(Account acct, int index) {
        int idx = index+1;
        return SIGNATURE_NAMEPREFIX + "-" + idx + "of-acct-" + acct.getName();
    }
    
    private String signatureContent(Account acct, int index) {
        int idx = index+1;
        return "signature content of " + SIGNATURE_NAMEPREFIX + "-" + idx + "of-acct-" + acct.getName();
    }
    
    private String identityName(Account acct, int index) {
        int idx = index+1;
        return IDENTITY_NAMEPREFIX + "-" + idx + "of-acct-" + acct.getName();
    }
    
    private String dataSourceName(Account acct, int index) {
        int idx = index+1;
        return DATASOURCE_NAMEPREFIX + "-" + idx + "of-acct-" + acct.getName();
    }
    
    private void createDomain(String domainName) throws Exception {
        System.out.println("createDomain: " + domainName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = mProv.createDomain(domainName, attrs);
    }
    
    private String[] createSignatures(Account acct) throws Exception {
        String[] sigIds = new String[NUM_SIGNATURES];
        
        for (int i = 0; i < NUM_SIGNATURES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraPrefMailSignature, signatureContent(acct, i));
            Signature entry = mProv.createSignature(acct, signatureName(acct, i), attrs);
        }
        
        return sigIds;
    }
    
    private void createIdentities(Account acct, String[] sigIds) throws Exception {
        for (int i = 0; i < NUM_IDENTITIES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, LdapUtil.generateUUID());  // just some random id, not used anywhere
            attrs.put(Provisioning.A_zimbraPrefFromAddress, "micky.mouse@zimbra,com");
            attrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
            attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, "TRUE");
            attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
            attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
            if (i < NUM_SIGNATURES)
                attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, sigIds[i]);
            Identity entry = mProv.createIdentity(acct, identityName(acct, i), attrs);
        }
    }
    
    private void createDataSources(Account acct, String[] sigIds) throws Exception {
        for (int i = 0; i < NUM_DATASOURCES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
            attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
            attrs.put(Provisioning.A_zimbraDataSourceFolderId, "inbox");
            attrs.put(Provisioning.A_zimbraDataSourceHost, "pop.google.com");
            attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "TRUE");
            attrs.put(Provisioning.A_zimbraDataSourcePassword, PASSWORD);
            attrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
            attrs.put(Provisioning.A_zimbraDataSourceUsername, "mickymouse");
            attrs.put(Provisioning.A_zimbraDataSourceEmailAddress, "micky@google.com");
            attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, LdapUtil.generateUUID()); // just some random id, not used anywhere
            attrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
            attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
            attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
            if (i < NUM_SIGNATURES)
                attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, sigIds[i]);
            DataSource entry = mProv.createDataSource(acct, DataSource.Type.pop3, dataSourceName(acct, i), attrs);
        }
    }
    
    /*
     * create and setup entries in the domain
     */
    private void setupDomain(String domainName, String diffDomainName) throws Exception {
        System.out.println("setupDomain: " + domainName + ", " + diffDomainName);
        
        // create accounts and their aliases
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Map<String, Object> acctAttrs = new HashMap<String, Object>();
            Account acct = mProv.createAccount(accountName(a, domainName), PASSWORD, acctAttrs);
            
            mProv.addAlias(acct, accountAlias(a, domainName, domainName));
            mProv.addAlias(acct, accountAlias(a, diffDomainName, domainName));
            
            String[] signatureIds = createSignatures(acct);
            createIdentities(acct, signatureIds);
            createDataSources(acct, signatureIds);
            
        }
        
        // create nested dls and their aliases, then add accounts to dls
        for (int nd = 0; nd < NUM_NESTED_DLS; nd++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(nestedDLName(nd, domainName), dlAttrs);
            
            mProv.addAlias(dl, nestedDLAlias(nd, domainName, domainName));
            mProv.addAlias(dl, nestedDLAlias(nd, diffDomainName, domainName));
            
            String[] members = new String[NUM_ACCOUNTS];
            for (int a = 0; a < NUM_ACCOUNTS; a++) {
                members[a] = accountName(a, domainName);
            }
            
            // add members to the dl
            mProv.addMembers(dl, members);
        }
        
        // create top dls and their aliases, then add accounts and nested dls to top dls
        for (int td = 0; td < NUM_TOP_DLS; td++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(topDLName(td, domainName), dlAttrs);
            
            mProv.addAlias(dl, topDLAlias(td, domainName, domainName));
            mProv.addAlias(dl, topDLAlias(td, diffDomainName, domainName));
            
            // setup the member array
            String[] members = new String[NUM_ACCOUNTS + NUM_NESTED_DLS];
            for (int a = 0; a < NUM_ACCOUNTS; a++) {
                members[a] = accountName(a, domainName);
            }
            
            for (int nd = 0; nd < NUM_NESTED_DLS; nd++) {
                members[NUM_ACCOUNTS+nd] = nestedDLName(nd, domainName);
            }
            
            // add members to the dl
            mProv.addMembers(dl, members);
        }
    }
    
    /*
     * add all accounts and dls in sourceDomain to targetDomain
     */
    private void crossLinkDomain(String sourceDomainName, String targetDomainName) throws Exception {
        System.out.println("crossLinkDomain: " + sourceDomainName + ", " + targetDomainName);
        
        Domain sourceDomain = mProv.get(Provisioning.DomainBy.name, sourceDomainName);
        
        
        /*
        List dls = mProv.getAllDistributionLists(sourceDomain);
        for (Iterator it = dls.iterator(); it.hasNext();) {
            DistributionList dl = (DistributionList)it.next();
            parts = EmailUtil.getLocalPartAndDomain(dl.getName());
            if (parts == null)
                throw ServiceException.FAILURE("encountered invalid dl name " + dl.getName(), null);
            newLocal = parts[0];
            newEmail = newLocal + "@" + newName;   
            renameDistributionList(dl.getId(), newEmail);
         }
         */
    }
    
    private String execute() throws Exception {
        
        Domain oldDomain = mProv.get(Provisioning.DomainBy.name, OLD_DOMAIN_NAME);
        ((LdapProvisioning)mProv).renameDomain(oldDomain.getId(), NEW_DOMAIN_NAME);
        
        return TEST_ID;
    }
    
    public void testRenameDomain() throws Exception {
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
        CliUtil.toolSetup("DEBUG");
        
        TestUtil.runTest(new TestSuite(TestRenameDomain.class));
        
        /*
        System.out.println("\n===== hello =====");
        TestRenameDomain t = new TestRenameDomain();
        t.setUp();
        */
    }
}
