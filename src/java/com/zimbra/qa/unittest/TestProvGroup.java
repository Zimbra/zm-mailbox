package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GroupedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;

import java.util.HashSet;

import junit.framework.TestCase;

public class TestProvGroup extends TestProv {

    public void tearDown() throws Exception {
        useSoapProv();
        deleteAllEntries();
    }
    
    
    /**
     * 
     * Create an "upward" group hierarchy for the given members.
     * 
     * @param memberNames
     * @param parentOfNode name of the node for which we are creating parent nodes for
     *                     - if parentOfNode is null, we are creating the bottom most level
     *                     - appending parentOfNode to each each node name guarantees a unique group name
     *                     
     * @param domain
     * @param directWidth   width for the bottom-most level
     * @param indirectWidth width for all non-bottom-most levels
     * @param depth
     * @param allGroups
     * @param directGroups
     * @throws Exception
     */
    private void createUpwardGroupHierarchy(String[] memberNames, String parentOfNode,
            Domain domain, int directWidth, int indirectWidth, int depth, 
            Set<String> allGroups, Set<String> directGroups) throws Exception {
        if (depth == 0)
            return;
        
        boolean isBottomLevel = (parentOfNode == null);  // is the bottom level
        int width = isBottomLevel ? directWidth : indirectWidth;
        
        for (int w = 1; w <= width; w++) {
            String groupName = depth + "-" + w;
            if (parentOfNode != null)
                groupName = groupName + "." + parentOfNode;
            System.out.println("Creating group " + groupName);
            DistributionList group = createUserGroup(groupName, domain);
            group.addMembers(memberNames);
            
            // huge bummer! SoapDistributionList.getId() does not work for the getDistributionLists call!!!  FIXIT!
            // have to use getName() here
            allGroups.add(group.getName());
            if (isBottomLevel)
                directGroups.add(group.getName());
            
            createUpwardGroupHierarchy(new String[]{group.getName()}, groupName, domain, 
                    directWidth, indirectWidth, depth-1, allGroups, directGroups);
        }
        
    }
    
    private void verifyEquals(Set<String> expectedNames, List<DistributionList> actual) throws Exception {
        assertEquals(expectedNames.size(), actual.size());
        Set<String> actualIds = new HashSet<String>();
        for (DistributionList group : actual) {
            actualIds.add(group.getName());
        }
        TestProvisioningUtil.verifyEquals(expectedNames, actualIds);
    }
    
    static class Timer {
        private long mBeginTime;
        
        Timer() {
            mBeginTime = System.currentTimeMillis();
        }
        
        long elapsed() {
            long now = System.currentTimeMillis();
            return (now - mBeginTime);
        }
        
        void reportElapsed(String note) {
            System.out.println(note + "(" + elapsed() + "ms)");
        }
        
    }
    
    private void verifyMembership(String acctId, Set<String> expectedAllGroups, Set<String> expectedDirectGroups) throws Exception {
        
        // test twice, so we hit all paths in LdapProvisioning.getGroups
        
        for (int i = 0; i < 2; i++) {
            Account acct = mProv.get(AccountBy.id, acctId);
            
            Timer timer = new Timer();
            
            // bummer, this API is broken for SoapProvisioning
            // The getId() call gets the id from attrs, but attrs is not populated by GetAccountMembership
            // Set<String> allGroups = mProv.getDistributionLists(acct);
            
            List<DistributionList> allGroups = mProv.getDistributionLists(acct, false, null);
            verifyEquals(expectedAllGroups, allGroups);
            
            List<DistributionList> directGroups = mProv.getDistributionLists(acct, true, null);
            verifyEquals(expectedDirectGroups, directGroups);
            
            timer.reportElapsed("Testing " + acct.getName());
        }
    }
    

    /*
     * Test both:
     * zmlocalconfig -e disable_compute_group_membership_optimization=true  (old way, before the perf fix)
     * zmlocalconfig -e disable_compute_group_membership_optimization=false (new way, after the perf fix, this is the default)
     */
    public void xxxtestPerf() throws Exception {
        useLdapProv();
        
        Domain domain = createDomain();
        Account acct1 = createUserAccount("acct1", domain);
        Account acct2 = createUserAccount("acct2", domain);
        
        String acct1Id = acct1.getId();
        String acct2Id = acct2.getId();
        
        String[] members = new String[2];
        members[0] = acct1.getName();
        members[1] = acct2.getName();
        
        Set<String> expectedAllGroups = new HashSet<String>();
        Set<String> expectedDirectGroups = new HashSet<String>();
        
        createUpwardGroupHierarchy(members, null, domain, 10, 2, 3, expectedAllGroups, expectedDirectGroups);
        
        int numIters = 10;
        
        useSoapProv();
        // flushCache(acct1);
        Timer soapTimer = new Timer();
        for (int i = 0; i < numIters; i++) {
            verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
            verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
        }
        soapTimer.reportElapsed("soap");
        
        useLdapProv();
        Timer ldapTimer = new Timer();
        for (int i = 0; i < numIters; i++) {
            verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
            verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
        }
        ldapTimer.reportElapsed("ldap");
    }

    public void testCircular_1() throws Exception {
        useLdapProv();
        
        Domain domain = createDomain();
        DistributionList group1 = createUserGroup("group1", domain);
        DistributionList group2 = createUserGroup("group2", domain);
        Account acct1 = createUserAccount("acct1", domain);
        Account acct2 = createUserAccount("acct2", domain);
        String acct1Id = acct1.getId();
        String acct2Id = acct2.getId();
        
        group1.addMembers(new String[]{group2.getName(), acct1.getName(), acct2.getName()});
        group2.addMembers(new String[]{group1.getName(), acct1.getName(), acct2.getName()});
        
        Set<String> expectedAllGroups = new HashSet<String>();
        Set<String> expectedDirectGroups = new HashSet<String>();
        expectedAllGroups.add(group1.getName());
        expectedAllGroups.add(group2.getName());
        expectedDirectGroups.add(group1.getName());
        expectedDirectGroups.add(group2.getName());
        
        useSoapProv();
        verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
        verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
        
        useLdapProv();
        verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
        verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
    }
    
    public void testCircular_2() throws Exception {
        useLdapProv();
        
        Domain domain = createDomain();
        DistributionList group1 = createUserGroup("group1", domain);
        DistributionList group2 = createUserGroup("group2", domain);
        DistributionList group3 = createUserGroup("group3", domain);
        Account acct1 = createUserAccount("acct1", domain);
        Account acct2 = createUserAccount("acct2", domain);
        String acct1Id = acct1.getId();
        String acct2Id = acct2.getId();
        
        group1.addMembers(new String[]{group2.getName(), acct1.getName(), acct2.getName()});
        group2.addMembers(new String[]{group3.getName()});
        group3.addMembers(new String[]{group1.getName()});
        
        Set<String> expectedAllGroups = new HashSet<String>();
        Set<String> expectedDirectGroups = new HashSet<String>();
        expectedAllGroups.add(group1.getName());
        expectedAllGroups.add(group2.getName());
        expectedAllGroups.add(group3.getName());
        expectedDirectGroups.add(group1.getName());
        
        useSoapProv();
        verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
        verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
        
        useLdapProv();
        verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
        verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
    }

        
    public void testBug42132() throws Exception {
        useLdapProv();
        
        Domain domain = createDomain();
        DistributionList group1 = createUserGroup("group1", domain);
        DistributionList group2 = createUserGroup("group2", domain);
        Account acct1 = createUserAccount("acct1", domain);
        Account acct2 = createUserAccount("acct2", domain);
        String acct1Id = acct1.getId();
        String acct2Id = acct2.getId();
        
        group2.addMembers(new String[]{group1.getName()});
        group2.addMembers(new String[]{group2.getName()});
        group1.addMembers(new String[]{acct1.getName(), acct2.getName()});
        
        Set<String> expectedAllGroups = new HashSet<String>();
        Set<String> expectedDirectGroups = new HashSet<String>();
        expectedAllGroups.add(group1.getName());
        expectedAllGroups.add(group2.getName());
        expectedDirectGroups.add(group1.getName());
        
        useSoapProv();
        verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
        verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
        
        useLdapProv();
        verifyMembership(acct1Id, expectedAllGroups, expectedDirectGroups);
        verifyMembership(acct2Id, expectedAllGroups, expectedDirectGroups);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        TestUtil.runTest(TestProvGroup.class);
    }

}
