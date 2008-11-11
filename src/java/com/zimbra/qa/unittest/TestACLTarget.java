package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;

public class TestACLTarget extends TestACL {

/*
  <key name="zimbra_class_accessmanager">
    <value>com.zimbra.cs.account.accesscontrol.RoleAccessManager</value>
  </key>
*/
    
    /*
     * ======================
     * ======================
     *     Target Tests
     * ======================
     * ======================
     */    
    
    public void testTargetAccount() throws Exception {
        
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - target account
         *   - group the target account is a member of
         *   - domain the account is in
         *   - global grant
         */
        Right right = ADMIN_RIGHT_ACCOUNT;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target account itself 
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        grantRight(TargetType.account, target, aces);
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.account, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group1, aces);
        via = new TestViaGrant(TargetType.distributionlist, group1, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.distributionlist, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, aces);
        via = new TestViaGrant(TargetType.domain, domain, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.domain, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, null, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, null, aces);
    }
        
    public void testTargetCalendarResource() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - target calendar resource
         *   - group the target account is a member of
         *   - domain the account is in
         *   - global grant
         */
        Right right = ADMIN_RIGHT_CALENDAR_RESOURCE;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target calendar resource itself 
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "foo");
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        CalendarResource target = mProv.createCalendarResource(getEmailAddr(testName, "target"), PASSWORD, attrs);
        grantRight(TargetType.resource, target, aces);
        via = new TestViaGrant(TargetType.resource, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.resource, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group1, aces);
        via = new TestViaGrant(TargetType.distributionlist, group1, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.distributionlist, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, aces);
        via = new TestViaGrant(TargetType.domain, domain, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.domain, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, globalGrant, aces);
    }
    
    public void testTargetGroup() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - target group
         *   - group the target account is a member of
         *   - domain the account is in
         *   - global grant
         */
        Right right = ADMIN_RIGHT_DISTRIBUTION_LIST;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target group itself 
        DistributionList target = mProv.createDistributionList(getEmailAddr(testName, "target"), new HashMap<String, Object>());
        grantRight(TargetType.distributionlist, target, aces);
        via = new TestViaGrant(TargetType.distributionlist, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.distributionlist, target, aces);
        
        // grant on a group the target group is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group1, aces);
        via = new TestViaGrant(TargetType.distributionlist, group1, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.distributionlist, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, aces);
        via = new TestViaGrant(TargetType.domain, domain, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.domain, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, globalGrant, aces);
    }

    public void testTargetDomain() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - the target domain
         *   - global grant
         */
        Right right = ADMIN_RIGHT_DOMAIN;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target domain itself
        Domain target = mProv.get(DomainBy.name, DOMAIN_NAME);
        grantRight(TargetType.domain, target, aces);
        via = new TestViaGrant(TargetType.domain, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.domain, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, globalGrant, aces);
    }
    
    public void testTargetCos() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - the target cos
         *   - global grant
         */
        Right right = ADMIN_RIGHT_COS;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target cos itself
        Cos target = mProv.get(CosBy.name, "default");
        grantRight(TargetType.cos, target, aces);
        via = new TestViaGrant(TargetType.cos, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.cos, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, globalGrant, aces);
    }
    
    public void testTargetServer() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - the target server
         *   - global grant
         */
        Right right = ADMIN_RIGHT_SERVER;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target server itself
        Server target = mProv.getLocalServer();
        grantRight(TargetType.server, target, aces);
        via = new TestViaGrant(TargetType.server, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.server, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, globalGrant, aces);
    }
    
    public void testTargetConfig() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - the target config
         *   - global grant
         */
        Right right = ADMIN_RIGHT_CONFIG;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on target server itself
        Config target = mProv.getConfig();
        grantRight(TargetType.config, target, aces);
        via = new TestViaGrant(TargetType.config, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.config, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, globalGrant, aces);

    }
    
    public void testTargetGlobalGrant() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - global grant
         */
        Right right = ADMIN_RIGHT_GLOBALGRANT;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        // grant on the global grant
        GlobalGrant target = mProv.getGlobalGrant();
        grantRight(TargetType.global, target, aces);
        via = new TestViaGrant(TargetType.global, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        revokeRight(TargetType.global, target, aces);

    }
    
    /**
     * Original grants:
     *     global grant (allow)
     *         domain (deny)
     *             group1 (allow)
     *                 group2 (deny)
     *                     target account (allow)
     * => should allow
     * 
     * then revoke the grant on account, should deny
     * then revoke the grant on group2, should allow
     * then revoke the grant on group1, should deny
     * then revoke the grant on domain, should allow
     * then revoke the grant on global grant, should deny
     *                  
     * @throws Exception
     */
    public void testTargetPrecedence() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup grant, the grant will be granted/tested/revoked on the following targets in turn.
         *   - target account
         *   - group the target account is a member of
         *   - domain the account is in
         *   - global grant
         */
        Right right = ADMIN_RIGHT_ACCOUNT;
        Set<ZimbraACE> allowedGrants = new HashSet<ZimbraACE>();
        allowedGrants.add(newUsrACE(grantee, right, ALLOW));
        Set<ZimbraACE> deniedGrants = new HashSet<ZimbraACE>();
        deniedGrants.add(newUsrACE(grantee, right, DENY));
        
        /*
         * setup targets
         */
        // 1. target account itself
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        grantRight(TargetType.account, target, allowedGrants);
        
        // 2. groups the target account is a member of
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group2, deniedGrants);
        grantRight(TargetType.distributionlist, group1, allowedGrants);
        
        // 3. domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, deniedGrants);
        
        // 4. global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, allowedGrants);
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        
        // revoke the grant on target account, then grant on group2 should take effect
        revokeRight(TargetType.account, target, allowedGrants);
        via = new TestViaGrant(TargetType.distributionlist, group2, GranteeType.GT_USER, grantee.getName(), right, NEGATIVE);
        verify(grantee, target, right, DENY, via);
        
        // revoke the grant on group2, then grant on group1 should take effect
        revokeRight(TargetType.distributionlist, group2, deniedGrants);
        via = new TestViaGrant(TargetType.distributionlist, group1, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        
        // revoke the grant on group1, then grant on domain should take effect
        revokeRight(TargetType.distributionlist, group1, allowedGrants);
        via = new TestViaGrant(TargetType.domain, domain, GranteeType.GT_USER, grantee.getName(), right, NEGATIVE);
        verify(grantee, target, right, DENY, via);
        
        // revoke the grant on domain, then grant on globalgrant shuld take effect
        revokeRight(TargetType.domain, domain, deniedGrants);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        
        // revoke the grant on globalgrant, then there is no grant and callsite default should be honored 
        revokeRight(TargetType.global, globalGrant, allowedGrants);
        verifyDefault(grantee, target, right);
        
    }

    
    /*
     * Number in () is the shortest distance to the account
     *                     
     *                          G(2)     H(3)
     *                          |       /
     *                          E(1)   F(2)
     *                          |     /
     *                  C(2)    |    D(1) 
     *                   \      |   /  \    
     *                    A(1)  |  /    B(1)
     *                     \    | /    /
     *                      \   |/    /
     *                        account    
     *                     
     */
    public void testGroupExpansion() throws Exception {
        String testName = getName();
        
        Account acct = mProv.createAccount(getEmailAddr(testName, "account"), PASSWORD, null);
        DistributionList A = mProv.createDistributionList(getEmailAddr(testName, "A"), new HashMap<String, Object>());
        DistributionList B = mProv.createDistributionList(getEmailAddr(testName, "B"), new HashMap<String, Object>());
        DistributionList C = mProv.createDistributionList(getEmailAddr(testName, "C"), new HashMap<String, Object>());
        DistributionList D = mProv.createDistributionList(getEmailAddr(testName, "D"), new HashMap<String, Object>());
        DistributionList E = mProv.createDistributionList(getEmailAddr(testName, "E"), new HashMap<String, Object>());
        DistributionList F = mProv.createDistributionList(getEmailAddr(testName, "F"), new HashMap<String, Object>());
        DistributionList G = mProv.createDistributionList(getEmailAddr(testName, "G"), new HashMap<String, Object>());
        DistributionList H = mProv.createDistributionList(getEmailAddr(testName, "H"), new HashMap<String, Object>());
        
        mProv.addMembers(A, new String[] {acct.getName()});
        mProv.addMembers(B, new String[] {acct.getName()});
        mProv.addMembers(C, new String[] {A.getName()});
        mProv.addMembers(D, new String[] {acct.getName(), B.getName()});
        mProv.addMembers(E, new String[] {acct.getName(), D.getName()});
        mProv.addMembers(F, new String[] {D.getName()});
        mProv.addMembers(G, new String[] {E.getName()});
        mProv.addMembers(H, new String[] {F.getName()});
        
        Map<String, String> via = new HashMap<String, String>();
        List<DistributionList> dls = mProv.getDistributionLists(acct, false, via);
        
        /*
        for (DistributionList dl : dls) {
            System.out.println(dl.getName());
        }
        System.out.println();
        System.out.println("via:");
        for (Map.Entry dl : via.entrySet()) {
            System.out.println(dl.getKey() + ": " + dl.getValue());
        }
        System.out.println();
        */
        
        for (DistributionList dl : dls) {
            int dist = 0;
            String dlName = dl.getName();
            String viaName = dlName;
            do {
                viaName = via.get(viaName);
                dist++;
            } while (viaName != null);
            
            // System.out.println(dl.getName() + ": " + dist);
            
            if (dlName.equals(A.getName()))
                assertEquals(1, dist);
            else if (dlName.equals(B.getName()))
                assertEquals(1, dist);
            else if (dlName.equals(C.getName()))
                assertEquals(2, dist);
            else if (dlName.equals(D.getName()))
                assertEquals(1, dist);
            else if (dlName.equals(E.getName()))
                assertEquals(1, dist);
            else if (dlName.equals(F.getName()))
                assertEquals(2, dist); 
            else if (dlName.equals(G.getName()))
                assertEquals(2, dist);
            else if (dlName.equals(H.getName()))
                assertEquals(3, dist);
        }
        
    }    

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");

        TestUtil.runTest(TestACLTarget.class);
        
        /*
        TestACLTarget test = new TestACLTarget();
        test.testGroupExpansion();
        */
    }
}
