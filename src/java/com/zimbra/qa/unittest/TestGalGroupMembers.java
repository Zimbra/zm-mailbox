package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import junit.framework.TestCase;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.cs.ldap.LdapUtilCommon;

/*
 * To run this test:
 * zmsoap -v -z RunUnitTestsRequest/test=com.zimbra.qa.unittest.TestGalGroupMembers
 *
 */

public class TestGalGroupMembers extends TestCase {
    
    private static final String ZIMBRA_DOMAIN = "zimbra.galgrouptest";
    private static final String ZIMBRA_GROUP = "zimbra-group";
    
    private static final String EXTERNAL_DOMAIN = "external.galgrouptest";
    private static final String EXTERNAL_GROUP = "external-group";
    
    private static final String USER = "user";
        
    //////////////
    // TODO: remove once ZimbraSuite supports JUnit 4 annotations.
    private static boolean initialized = false;
    private static boolean allDone = false;
    
    public void setUp() throws Exception {
        if (!initialized) {
            init();
            initialized = true;
        }
    }
    
    public void tearDown() throws Exception {
        if (allDone) {
            cleanup();
            initialized = false;
        }
    }
    //////////////
    
    enum ZimbraGroupMembers {
        ZIMBRA_GROUP_MEMBER_ACCOUNT_1(true),
        ZIMBRA_GROUP_MEMBER_ACCOUNT_2(true),
        NON_ZIMBRA_ADDR_1(false),
        NON_ZIMBRA_ADDR_2(false);
        
        private boolean isZimbraAccount;
        
        private ZimbraGroupMembers(boolean isZimbraAccount) {
            this.isZimbraAccount = isZimbraAccount;
        }
        
        private boolean isZimbraAccount() {
            return isZimbraAccount;
        }
        
        private String getAddress() {
            String localPart = toString().toLowerCase();
            return isZimbraAccount ? TestUtil.getAddress(localPart, ZIMBRA_DOMAIN) :
                TestUtil.getAddress(localPart, "somedomain.com");
        }
        
        static String[] getAllMembersAsArray() {
            String[] members = new String[ZimbraGroupMembers.values().length];
            for (ZimbraGroupMembers member : ZimbraGroupMembers.values()) {
                members[member.ordinal()] = member.getAddress();
            }
            
            return members;
        }
        
        static void assertEquals(Set<String> addrs) {
            Assert.assertEquals(ZimbraGroupMembers.values().length, addrs.size());
            
            for (ZimbraGroupMembers member : ZimbraGroupMembers.values()) {
                Assert.assertTrue(addrs.contains(member.getAddress()));
                
                // verify that addrs should do case-insensitive comparison
                Assert.assertTrue(addrs.contains(member.getAddress().toUpperCase()));
                Assert.assertTrue(addrs.contains(member.getAddress().toLowerCase()));
            }
        }
    }

    enum ExternalGroupMembers {
        MEMBER_1,
        MEMBER_2,
        MEMBER_3,
        MEMBER_4;
        
        private String getAddress() {
            String localPart = toString().toLowerCase();
            return TestUtil.getAddress(localPart, EXTERNAL_DOMAIN);
        }
        
        static String[] getAllMembersAsArray() {
            String[] members = new String[ExternalGroupMembers.values().length];
            for (ExternalGroupMembers member : ExternalGroupMembers.values()) {
                members[member.ordinal()] = member.getAddress();
            }
            
            return members;
        }
        
        static void assertEquals(Set<String> addrs) {
            Assert.assertEquals(ExternalGroupMembers.values().length, addrs.size());
            
            for (ExternalGroupMembers member : ExternalGroupMembers.values()) {
                Assert.assertTrue(addrs.contains(member.getAddress()));
                
                // verify that addrs should do case-insensitive comparison
                Assert.assertTrue(addrs.contains(member.getAddress().toUpperCase()));
                Assert.assertTrue(addrs.contains(member.getAddress().toLowerCase()));
            }
        }
    }
    
    private static void setupZimbraDomain() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        // create the zimbra domain
        if (prov.get(Key.DomainBy.name, ZIMBRA_DOMAIN) == null) {
            ZimbraLog.test.info("Creating domain " + ZIMBRA_DOMAIN);
            Domain domain = prov.createDomain(ZIMBRA_DOMAIN, new HashMap<String, Object>());
            
            // configure external GAL
            Map<String, Object> attrs = new HashMap<String, Object>();
            domain.setGalMode(GalMode.both, attrs);
            domain.addGalLdapURL("ldap://localhost:389", attrs);
            domain.setGalLdapBindDn("cn=config", attrs);
            domain.setGalLdapBindPassword("zimbra");
            domain.setGalLdapSearchBase(LdapUtilCommon.domainToDN(EXTERNAL_DOMAIN));
            domain.setGalAutoCompleteLdapFilter("zimbraAccountAutoComplete");
            domain.setGalLdapFilter("zimbraAccounts");
            
            prov.modifyAttrs(domain, attrs);
        }
        
        // create the test user
        String userAddr = TestUtil.getAddress(USER, ZIMBRA_DOMAIN);
        if (prov.get(AccountBy.name, userAddr) == null) {
            prov.createAccount(userAddr, "test123", null);
        }
        
        // create accounts in the zimbra domain
        for (ZimbraGroupMembers member : ZimbraGroupMembers.values()) {
            if (member.isZimbraAccount()) {
                String addr = member.getAddress();
                if (prov.get(AccountBy.name, addr) == null) {
                    prov.createAccount(addr, "test123", null);
                }
            }
        }
        
        // create zimbra group and add members
        String groupAddr = TestUtil.getAddress(ZIMBRA_GROUP, ZIMBRA_DOMAIN);
        DistributionList group = prov.get(Key.DistributionListBy.name, groupAddr);
        if (group == null) {
            group = prov.createDistributionList(groupAddr, new HashMap<String, Object>());
            prov.addMembers(group, ZimbraGroupMembers.getAllMembersAsArray());
        }
        
    }
    
    private static void cleanupZimbraDomain() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        TestSearchGal.disableGalSyncAccount(prov, ZIMBRA_DOMAIN);
        
        // delete the test user
        String userAddr = TestUtil.getAddress(USER, ZIMBRA_DOMAIN);
        Account userAcct = prov.get(AccountBy.name, userAddr);
        if (prov.get(AccountBy.name, userAddr) != null) {
            prov.deleteAccount(userAcct.getId());
        }
        
        for (ZimbraGroupMembers member : ZimbraGroupMembers.values()) {
            if (member.isZimbraAccount()) {
                String addr = member.getAddress();
                Account acct = prov.get(AccountBy.name, addr);
                if (acct != null) {
                    prov.deleteAccount(acct.getId());
                }
            }
        }
        
        String groupAddr = TestUtil.getAddress(ZIMBRA_GROUP, ZIMBRA_DOMAIN);
        DistributionList group = prov.get(Key.DistributionListBy.name, groupAddr);
        if (group != null) {
            prov.deleteDistributionList(group.getId());
        }
        
        Domain domain = prov.get(Key.DomainBy.name, ZIMBRA_DOMAIN);
        if (domain != null) {
            ZimbraLog.test.info("Deleting domain " + ZIMBRA_DOMAIN);
            prov.deleteDomain(domain.getId());
        }
        
    }
    
    private static void setupExternalDomain() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        // create a domain to simulate entries in external GAL
        if (prov.get(Key.DomainBy.name, EXTERNAL_DOMAIN) == null) {
            ZimbraLog.test.info("Creating domain " + EXTERNAL_DOMAIN);
            prov.createDomain(EXTERNAL_DOMAIN, new HashMap<String, Object>());
        }
        
        // create groups in the external domain
        String groupAddr = TestUtil.getAddress(EXTERNAL_GROUP, EXTERNAL_DOMAIN);
        DistributionList group = prov.get(Key.DistributionListBy.name, groupAddr);
        if (group == null) {
            group = prov.createDistributionList(groupAddr, new HashMap<String, Object>());
            prov.addMembers(group, ExternalGroupMembers.getAllMembersAsArray());
        }
    }
    
    private static void cleanupExternalDomain() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        String groupAddr = TestUtil.getAddress(EXTERNAL_GROUP, EXTERNAL_DOMAIN);
        DistributionList group = prov.get(Key.DistributionListBy.name, groupAddr);
        if (group != null) {
            prov.deleteDistributionList(group.getId());
        }
        
        Domain domain = prov.get(Key.DomainBy.name, EXTERNAL_DOMAIN);
        if (domain != null) {
            ZimbraLog.test.info("Deleting domain " + EXTERNAL_DOMAIN);
            prov.deleteDomain(domain.getId());
        }
    }
    
    @BeforeClass
    public static void init() throws Exception {
        // TestUtil.cliSetup();
        // CliUtil.toolSetup();
        
        setupZimbraDomain();
        setupExternalDomain();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        cleanupZimbraDomain();
        cleanupExternalDomain();
    }
    
    private void doTest() throws Exception {
        String userAddr = TestUtil.getAddress(USER, ZIMBRA_DOMAIN);
        Account acct = Provisioning.getInstance().get(AccountBy.name, userAddr);
        
        Set<String> zimbraGroupMembers = GalGroupMembers.getGroupMembers(TestUtil.getAddress(ZIMBRA_GROUP, ZIMBRA_DOMAIN), acct);
        ZimbraGroupMembers.assertEquals(zimbraGroupMembers);
        
        Set<String> externalGroupMembers = GalGroupMembers.getGroupMembers(TestUtil.getAddress(EXTERNAL_GROUP, EXTERNAL_DOMAIN), acct);
        ExternalGroupMembers.assertEquals(externalGroupMembers);
    }
    
    @Test
    public void testLdapSearch() throws Exception {
        TestSearchGal.disableGalSyncAccount(Provisioning.getInstance(), ZIMBRA_DOMAIN);
        doTest();
    }
    
    @Test
    public void testGSASearch() throws Exception {
        TestSearchGal.enableGalSyncAccount(Provisioning.getInstance(), ZIMBRA_DOMAIN, TestSearchGal.GSAType.both);
        doTest();
    }
    
    // TODO: remove once ZimbraSuite supports JUnit 4 annotations. 
    @Test
    public void testLast() throws Exception {
        allDone = true;
    }

}
