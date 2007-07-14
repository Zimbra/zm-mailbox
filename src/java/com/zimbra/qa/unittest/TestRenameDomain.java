package com.zimbra.qa.unittest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

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
    private String ACCOUNT_NAMEPREFIX;
    private String TOP_DL_NAMEPREFIX;
    private String NESTED_DL_NAMEPREFIX;
    private String ALIAS_NAMEPREFIX;
    
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
        ACCOUNT_NAMEPREFIX = "acct";
        TOP_DL_NAMEPREFIX = "top-dl";
        NESTED_DL_NAMEPREFIX = "nested-dl";
        ALIAS_NAMEPREFIX = "alias";
        
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
    
    private String accountAlias(int index, String domainName) {
        return ALIAS_NAMEPREFIX + "-" + accountName(index, domainName);
    }
    
    private String topDLName(int index, String domainName) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        int idx = index+1;
        return TOP_DL_NAMEPREFIX + "-" + idx + "@" + domainName;
    }
    
    private String topDLAlias(int index, String domainName) {
        return ALIAS_NAMEPREFIX + "-" + topDLName(index, domainName);
    }
    
    private String nestedDLName(int index, String domainName) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        int idx = index+1;
        return NESTED_DL_NAMEPREFIX + "-" + idx + "@" + domainName;
    }
    
    private String nestedDLAlias(int index, String domainName) {
        return ALIAS_NAMEPREFIX + "-" + nestedDLName(index, domainName);
    }
    
    private void createDomain(String domainName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = mProv.createDomain(domainName, attrs);
    }
    
    /*
     * create and setup entries in the domain
     */
    private void setupDomain(String domainName, String diffDomainName) throws Exception {
        
        // create accounts and their aliases
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Map<String, Object> acctAttrs = new HashMap<String, Object>();
            Account acct = mProv.createAccount(accountName(a, domainName), PASSWORD, acctAttrs);
            
            mProv.addAlias(acct, accountAlias(a, domainName));
            mProv.addAlias(acct, accountAlias(a, diffDomainName));
        }
        
        // create nested dls and their aliases, then add accounts to dls
        for (int nd = 0; nd < NUM_NESTED_DLS; nd++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(nestedDLName(nd, domainName), dlAttrs);
            
            mProv.addAlias(dl, nestedDLAlias(nd, domainName));
            mProv.addAlias(dl, nestedDLAlias(nd, diffDomainName));
            
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
            
            mProv.addAlias(dl, topDLAlias(td, domainName));
            mProv.addAlias(dl, topDLAlias(td, diffDomainName));
            
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
        TestUtil.runTest(new TestSuite(TestRenameDomain.class));
        
        /*
        System.out.println("\n===== hello =====");
        TestRenameDomain t = new TestRenameDomain();
        t.setUp();
        */
    }
}
