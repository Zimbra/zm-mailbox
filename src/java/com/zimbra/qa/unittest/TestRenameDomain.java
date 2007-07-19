package com.zimbra.qa.unittest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.NamedEntry;
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
    
    private String PASSWORD = "test123";
    
    private int NUM_ACCOUNTS    = 3;
    private int NUM_CAS         = 3;  // calendar resources, TODO
    private int NUM_DLS_NESTED  = 2;
    private int NUM_DLS_TOP     = 2;
    private int NUM_DOMAINS     = 2;
    private int NUM_SUB_DOMAINS = 2;  // number of sub domains under the old domain(domain to be renamed)
     
    /*
     * NUM_IDENTITIES and NUM_DATASOURCES must be >= NUM_SIGNATURES, each identity/datasource get one signature of the same index.
     * identities and datasources that do not have corresponding signature will get no signature. 
     * e.g. 
     *     identity-1 -> signature-1
     *     identity-2 -> signature-2 
     *     identity-3 -> (no signature)
     * 
     */ 
    private int NUM_SIGNATURES  = 2; 
    private int NUM_IDENTITIES  = 2;
    private int NUM_DATASOURCES = 2;
    
    private String NAME_ROOT_DOMAIN     = "ldap-test-domain";
    private String NAME_LEAF_OLD_DOMAIN = "olddomain";
    private String NAME_LEAF_NEW_DOMAIN = "newdomain";
    
    private String NAMEPREFIX_ACCOUNT     = "acct-";
    private String NAMEPREFIX_ALIAS       = "alias-";
    private String NAMEPREFIX_DATASOURCE  = "datasource-";
    private String NAMEPREFIX_DL_NESTED   = "nesteddl-";
    private String NAMEPREFIX_DL_TOP      = "topdl-";
    private String NAMEPREFIX_IDENTITY    = "identity-";
    private String NAMEPREFIX_OTHERDOMAIN = "otherdomain-";
    private String NAMEPREFIX_SIGNATURE   = "signature-";
    private String NAMEPREFIX_SUB_DOMAIN  = "subdomian-";
    
    // pseudo domain index for the old domain and new domain, so that we can use the unified interfaces
    private int OLD_DOMAIN  = 0;
    private int NEW_DOMAIN  = -1;
    
    
    public void setUp() throws Exception {
        
        Date date = new Date();
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
        TEST_ID = fmt.format(date);
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        
        mProv = Provisioning.getInstance();
        assertTrue(mProv instanceof LdapProvisioning);
        
         
        /*
         * Create NUM_DOMAINS domains: one domain(the first one) to be renamed, and NUM_DOMAINS-1 other domains.  
         *     - Each domain:
         *           - has NUM_ACCOUNTS accounts
         *           - has NUM_DLS_TOP top level dls (dl that is not a nested DL)
         *           - has NUM_DLS_NESTED nested dls (dl under another DL)
         * 
         *     - Each account:
         *           - has NUM_DOMAINS aliases, one in each domain
         *           - is a member of all DLs in all domains
         *           
         *     - Each top dl:
         *           - has NUM_DOMAINS aliases, one in each domain
         *                 
         *     - Each nested dl:
         *           - has NUM_DOMAINS aliases, one in each domain
         *           - is a member of all top DLs in all domains
         */
        
        // create domains
        for (int i = 0; i < NUM_DOMAINS; i++)
            createDomain(DOMAIN_NAME(i));
        
        // create sub domains under the domain-to-rename
        for (int i = 0; i < NUM_SUB_DOMAINS; i++) 
            createDomain(SUB_DOMAIN_NAME(i, OLD_DOMAIN));
        
        // setup entries in domains
        for (int i = 0; i < NUM_DOMAINS; i++)     
            setupDomain(i);
        
        for (int i = 0; i < NUM_DOMAINS; i++)     
            crossLinkDomain(i);

    }
        
    private String DOMAIN_NAME(String leafDomainName) {
        return leafDomainName + ".test-" + TEST_ID + "." + NAME_ROOT_DOMAIN;
    }
    
    private String SUB_DOMAIN_NAME(int index, int parentDomain) {
        int idx = index + 1;
        String parentDomainName = DOMAIN_NAME(parentDomain);
        return NAMEPREFIX_SUB_DOMAIN + idx + "." + parentDomainName;
    }
    
    private String LEAF_DOMAIN_NAME(int index) {
        if (index == OLD_DOMAIN)
            return NAME_LEAF_OLD_DOMAIN;
        else if (index == NEW_DOMAIN)
            return NAME_LEAF_NEW_DOMAIN;
        else
            return NAMEPREFIX_OTHERDOMAIN + index;
    }
        
    private String DOMAIN_NAME(int index) {
        return DOMAIN_NAME(LEAF_DOMAIN_NAME(index));
    }
    
    private String ACCOUNT_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_ACCOUNT + String.valueOf(index+1);
    }
    
    private String ACCOUNT_NAME(int index, int domainIdx) {
        return ACCOUNT_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    private String ACCOUNT_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + ACCOUNT_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String TOP_DL_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_DL_TOP + String.valueOf(index+1);
    }
    
    private String TOP_DL_NAME(int index, int domainIdx) {
        return TOP_DL_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    private String TOP_DL_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + TOP_DL_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String NESTED_DL_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_DL_NESTED + String.valueOf(index+1);
    }
    
    private String NESTED_DL_NAME(int index, int domainIdx) {
        return NESTED_DL_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    private String NESTED_DL_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + NESTED_DL_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String SIGNATURE_NAME(Account acct, int index) {
        int idx = index+1;
        return NAMEPREFIX_SIGNATURE + idx + "of-acct-" + acct.getName();
    }
    
    private String SIGNATURE_CONTENT(Account acct, int index) {
        int idx = index+1;
        return "signature content of " + NAMEPREFIX_SIGNATURE + idx + "of-acct-" + acct.getName();
    }
    
    private String IDENTITY_NAME(Account acct, int index) {
        int idx = index+1;
        return NAMEPREFIX_IDENTITY + idx + "of-acct-" + acct.getName();
    }
    
    private String DATASOURCE_NAME(Account acct, int index) {
        int idx = index+1;
        return NAMEPREFIX_DATASOURCE + idx + "of-acct-" + acct.getName();
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
            attrs.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_CONTENT(acct, i));
            Signature entry = mProv.createSignature(acct, SIGNATURE_NAME(acct, i), attrs);
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
            Identity entry = mProv.createIdentity(acct, IDENTITY_NAME(acct, i), attrs);
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
            DataSource entry = mProv.createDataSource(acct, DataSource.Type.pop3, DATASOURCE_NAME(acct, i), attrs);
        }
    }
    
    /*
     * create and setup entries in the domain
     */
    private void setupDomain(int domainIdx) throws Exception {
        
        String domainName = DOMAIN_NAME(domainIdx);
        System.out.println("setupDomain: " + domainName);
        
        // create accounts and their aliases
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Map<String, Object> acctAttrs = new HashMap<String, Object>();
            Account acct = mProv.createAccount(ACCOUNT_NAME(a, domainIdx), PASSWORD, acctAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                mProv.addAlias(acct, ACCOUNT_ALIAS_NAME(a, domainIdx, d));
            
            String[] signatureIds = createSignatures(acct);
            createIdentities(acct, signatureIds);
            createDataSources(acct, signatureIds);
        }
        
        // create nested dls and their aliases, then add accounts to dls
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(NESTED_DL_NAME(nd, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                mProv.addAlias(dl, NESTED_DL_ALIAS_NAME(nd, domainIdx, d));
            
            String[] members = new String[NUM_ACCOUNTS];
            for (int a = 0; a < NUM_ACCOUNTS; a++) {
                members[a] = ACCOUNT_NAME(a, domainIdx);
            }
            
            // add members to the dl
            mProv.addMembers(dl, members);
        }
        
        // create top dls and their aliases, then add accounts and nested dls to top dls
        for (int td = 0; td < NUM_DLS_TOP; td++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(TOP_DL_NAME(td, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                mProv.addAlias(dl, TOP_DL_ALIAS_NAME(td, domainIdx, d));
            
            // setup the member array
            String[] members = new String[NUM_ACCOUNTS + NUM_DLS_NESTED];
            for (int a = 0; a < NUM_ACCOUNTS; a++) {
                members[a] = ACCOUNT_NAME(a, domainIdx);
            }
            
            for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
                members[NUM_ACCOUNTS+nd] = NESTED_DL_NAME(nd, domainIdx);
            }
            
            // add members to the dl
            mProv.addMembers(dl, members);
        }
    }
    
    /*
     * add all accounts and dls of the domain to all other domains
     */
    private void crossLinkDomain(int domainIdx) throws Exception {
        
        String domainName = DOMAIN_NAME(domainIdx);
        System.out.println("crossLinkDomain: " + domainName);
        
        Domain sourceDomain = mProv.get(Provisioning.DomainBy.name, domainName);
        
        List<String>[][] nestedDLMembers = new ArrayList[NUM_DOMAINS][NUM_DLS_NESTED];
        List<String>[][] topDLMembers = new ArrayList[NUM_DOMAINS][NUM_DLS_TOP];        
        
        for (int d = 0; d < NUM_DOMAINS; d++) {
            for (int nd = 0; nd < NUM_DLS_TOP; nd++) {
                nestedDLMembers[d][nd] = new ArrayList();
            }
            for (int td = 0; td < NUM_DLS_TOP; td++) {
                topDLMembers[d][td] = new ArrayList();
            }
        }
        
        // add accounts to top and nested dls
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            String acctName = ACCOUNT_NAME(a, domainIdx);
            for (int d = 0; d < NUM_DOMAINS; d++) {
                if (d != domainIdx) {
                    for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
                        nestedDLMembers[d][nd].add(acctName);
                    }
                    for (int td = 0; td < NUM_DLS_TOP; td++) {
                        topDLMembers[d][td].add(acctName);
                    }
                }
            }
        }
        
        // add nested dls to top dls
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            String nestedDLName = NESTED_DL_NAME(nd, domainIdx);
            for (int d = 0; d < NUM_DOMAINS; d++) {
                if (d != domainIdx) {
                    for (int td = 0; td < NUM_DLS_TOP; td++) {
                        topDLMembers[d][td].add(nestedDLName);
                    }
                }
            }
        }
        
        // now add them
        for (int d = 0; d < NUM_DOMAINS; d++) {
            if (d != domainIdx) {
                for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
                    DistributionList dl = mProv.get(Provisioning.DistributionListBy.name, NESTED_DL_NAME(nd, d));
                    mProv.addMembers(dl, nestedDLMembers[d][nd].toArray(new String[0]));
                }
                for (int td = 0; td < NUM_DLS_TOP; td++) {
                    DistributionList dl = mProv.get(Provisioning.DistributionListBy.name, TOP_DL_NAME(td, d));
                    mProv.addMembers(dl, topDLMembers[d][td].toArray(new String[0]));
                }
            }
        }
    }
    
    
    private void verifyOldDomain() throws Exception {
        String oldDomainName = DOMAIN_NAME(OLD_DOMAIN);
        Domain oldDomain = mProv.get(Provisioning.DomainBy.name, oldDomainName);
        assertTrue(oldDomain == null);
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn, Set<String> specificAttrs) {
        
        System.out.println();
        
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            if (specificAttrs == null || specificAttrs.contains(name.toLowerCase())) {
                Object value = entry.getValue();
                if (value instanceof String[]) {
                    String sv[] = (String[]) value;
                    for (String aSv : sv) {
                        System.out.println(name + ": " + aSv);
                    }
                } else if (value instanceof String){
                    System.out.println(name+": "+value);
                }
            }
        }
        
        System.out.println();
    }
    
    private void dumpNames(String desc, List<NamedEntry> entries) {
        System.out.println();
        
        System.out.println("===== " + ((desc==null)?"":desc) + " =====");
        for (NamedEntry entry : entries)
            System.out.println(entry.getName());
        
        System.out.println();
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn) {
        dumpAttrs(attrsIn, null);
    }
    
    private Domain verifyNewDomainBasic(String domainId) throws Exception {
        // get by name
        Domain domainByName = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME(NEW_DOMAIN));
        assertTrue(domainByName != null);
        
        // get by id
        Domain domainById = mProv.get(Provisioning.DomainBy.id, domainId);
        assertTrue(domainById != null);
        
        TestProvisioningUtil.verifySameEntry(domainByName, domainById);
        
        return domainById;
    }
    
    /*
     * ensure all the attrs are carried over
     */ 
    private void verifyNewDomainAttrs(Domain newDomain, Map<String, Object> oldDomainAttrs) {
        Map<String, Object> newDomainAttrs = newDomain.getAttrs(false);

        // make a copy of the two attrs maps, becase we are deleting from them
        Map<String, Object> oldAttrs = new HashMap<String, Object>(oldDomainAttrs);
        Map<String, Object> newAttrs = new HashMap<String, Object>(newDomainAttrs);
                
        // dumpAttrs(oldAttrs);
        // dumpAttrs(newAttrs);
        
        oldAttrs.remove(Provisioning.A_dc);
        oldAttrs.remove(Provisioning.A_o);
        oldAttrs.remove(Provisioning.A_zimbraDomainName);

        newAttrs.remove(Provisioning.A_dc);
        newAttrs.remove(Provisioning.A_o);
        newAttrs.remove(Provisioning.A_zimbraDomainName);
        
        for (Map.Entry<String, Object> oldAttr : oldAttrs.entrySet()) {
            String oldKey = oldAttr.getKey();
            Object oldValue = oldAttr.getValue();
            
            Object newValue = newAttrs.get(oldKey);
            if (oldValue instanceof String[]) {
                assertTrue(newValue instanceof String[]);
                Set<String> oldV = new HashSet(Arrays.asList((String[])oldValue));
                Set<String> newV = new HashSet(Arrays.asList((String[])newValue));
                assertEquals(oldV.size(), newV.size());
                assertEquals(SetUtil.subtract(oldV, newV).size(), 0);
                
            } else if (oldValue instanceof String){
                assertEquals(oldValue, newValue);
            }
        }
    }
    
    /*
     * verify that the DL list contains the all the DLs in domain domainIdx an new domain account is supposed to be in
     */
    private void verifyNewDomainAccountInDLsOfDomain(List<String> dlNames, int domainIdx) {
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            String dlName = NESTED_DL_NAME(nd, domainIdx);
            assertTrue(dlNames.contains(dlName));
        }
        for (int td = 0; td < NUM_DLS_TOP; td++) {
            String dlName = TOP_DL_NAME(td, domainIdx);
            assertTrue(dlNames.contains(dlName));
        }
    }
    
    private void verifyNewDomainAccounts(Domain newDomain) throws Exception {
        
        /*
         * verify the account is in correct DLs in all domains
         */
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            String acctName = ACCOUNT_NAME(a, NEW_DOMAIN);
            Account acct = mProv.get(Provisioning.AccountBy.name, acctName);
            assertNotNull(acct);
            
            HashMap<String,String> via = new HashMap<String, String>();
            List dls = mProv.getDistributionLists(acct, false, via);
            // dumpNames("DLs account " + acctName + " is member of", dls);
            assertEquals((NUM_DLS_NESTED + NUM_DLS_TOP)*(NUM_DOMAINS), dls.size());
            
            List<String> dlNames = new ArrayList<String>();
            for (Object dl : dls)
                dlNames.add(((DistributionList)dl).getName());
            
            verifyNewDomainAccountInDLsOfDomain(dlNames, NEW_DOMAIN);
            for (int d = 0; d < NUM_DOMAINS; d++) {
                if (d != OLD_DOMAIN) {
                    verifyNewDomainAccountInDLsOfDomain(dlNames, d);
                }
            }
        }
       
    }
    
    private void verifyNewDomain(String domainId,  Map<String, Object> oldDomainAttrs) throws Exception {
        Domain newDomain = verifyNewDomainBasic(domainId);
        verifyNewDomainAttrs(newDomain, oldDomainAttrs);
        verifyNewDomainAccounts(newDomain);
        
    }
    
    private void verifyOtherDomains() throws Exception {
        
    }
    
    // TODO: 
    //  - stop the rename at different stages and test the restart
    //  - sub domain under old domain 

    private String execute() throws Exception {
        
        Domain oldDomain = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME(OLD_DOMAIN));
        String oldDomainId = oldDomain.getId();
        Map<String, Object> oldDomainAttrs = oldDomain.getAttrs(false);
        ((LdapProvisioning)mProv).renameDomain(oldDomain.getId(), DOMAIN_NAME(NEW_DOMAIN));
        
        verifyOldDomain();
        verifyNewDomain(oldDomainId, oldDomainAttrs);
        verifyOtherDomains();
        
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
