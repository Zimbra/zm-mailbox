package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
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
import com.zimbra.cs.account.accesscontrol.PermUtil;
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
        Right right = AdminRight.RT_renameAccount;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target account itself 
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        grantRight(TargetType.account, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.account, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group1, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.distributionlist, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.domain, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, null, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_renameCalendarResource;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target calendar resource itself 
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "foo");
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        Account target = mProv.createCalendarResource(getEmailAddr(testName, "target"), PASSWORD, attrs);
        grantRight(TargetType.resource, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.resource, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group1, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.distributionlist, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.domain, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_renameDistributionList;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target group itself 
        DistributionList target = mProv.createDistributionList(getEmailAddr(testName, "target"), new HashMap<String, Object>());
        grantRight(TargetType.distributionlist, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.distributionlist, target, aces);
        
        // grant on a group the target group is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr(testName, "group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr(testName, "group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        grantRight(TargetType.distributionlist, group1, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.distributionlist, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        grantRight(TargetType.domain, domain, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.domain, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_createAccount;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target domain itself
        Domain target = mProv.get(DomainBy.name, DOMAIN_NAME);
        grantRight(TargetType.domain, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.domain, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_renameCos;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target cos itself
        Cos target = mProv.get(CosBy.name, "default");
        grantRight(TargetType.cos, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.cos, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_renameServer;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target server itself
        Server target = mProv.getLocalServer();
        grantRight(TargetType.server, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.server, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_testGlobalConfigRemoveMe;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on target server itself
        Config target = mProv.getConfig();
        grantRight(TargetType.config, target, aces);
        verify(grantee, target, right, ALLOW);
        revokeRight(TargetType.config, target, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        grantRight(TargetType.global, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_testGlobalGrantRemoveMe;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, ALLOW));
        
        /*
         * test targets
         */
        
        // grant on the global grant
        GlobalGrant target = mProv.getGlobalGrant();
        grantRight(TargetType.global, target, aces);
        verify(grantee, target, right, ALLOW);
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
        Right right = AdminRight.RT_renameAccount;
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
        verify(grantee, target, right, ALLOW);
        
        // revoke the grant on target account, then grant on group2 should take effect
        revokeRight(TargetType.account, target, allowedGrants);
        verify(grantee, target, right, DENY);
        
        // revoke the grant on group2, then grant on group1 should take effect
        revokeRight(TargetType.distributionlist, group2, deniedGrants);
        verify(grantee, target, right, ALLOW);
        
        // revoke the grant on group1, then grant on domain should take effect
        revokeRight(TargetType.distributionlist, group1, allowedGrants);
        verify(grantee, target, right, DENY);
        
        // revoke the grant on domain, then grant on globalgrant shuld take effect
        revokeRight(TargetType.domain, domain, deniedGrants);
        verify(grantee, target, right, ALLOW);
        
        // revoke the grant on globalgrant, then there is no grant and should be denied.
        revokeRight(TargetType.global, globalGrant, allowedGrants);
        verify(grantee, target, right, DENY);
        
    }
    
    /*
     * Test this scenario:
     *
       For this target hierarchy:
           group G1 (allow right R for group GA)
               group G2 (deny right R for group GB)
                   group G3 (allow right R for group GC)
                       account target 
                       
       And this grantee hierarchy:
           group GA
               group GB
                   group GC
                       account grantee
                   
       The A is *allowed* for right R on target account A1, because GC is more specific to A than GA and GB.
       Even if on the target side, grant on G3(grant to GC) and G2(grant to GB) is more specific than the 
       grant on G1(grant to GA).                
                   
     */
    public void testTargetGranteeGroupConflict() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList GA = mProv.createDistributionList(getEmailAddr(testName, "GA"), new HashMap<String, Object>());
        DistributionList GB = mProv.createDistributionList(getEmailAddr(testName, "GB"), new HashMap<String, Object>());
        DistributionList GC = mProv.createDistributionList(getEmailAddr(testName, "GC"), new HashMap<String, Object>());

        mProv.addMembers(GA, new String[] {GB.getName()});
        mProv.addMembers(GB, new String[] {GC.getName()});
        mProv.addMembers(GC, new String[] {grantee.getName()});
        
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        
        DistributionList G1 = mProv.createDistributionList(getEmailAddr(testName, "G1"), new HashMap<String, Object>());
        DistributionList G2 = mProv.createDistributionList(getEmailAddr(testName, "G2"), new HashMap<String, Object>());
        DistributionList G3 = mProv.createDistributionList(getEmailAddr(testName, "G3"), new HashMap<String, Object>());
        
        mProv.addMembers(G1, new String[] {G2.getName()});
        mProv.addMembers(G2, new String[] {G3.getName()});
        mProv.addMembers(G3, new String[] {target.getName()});
        
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(GA, UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.distributionlist, G1, aces);
        
        aces.clear();
        aces.add(newGrpACE(GB, UserRight.RT_viewFreeBusy, DENY));
        grantRight(TargetType.distributionlist, G2, aces);
        
        aces.clear();
        aces.add(newGrpACE(GC, UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.distributionlist, G3, aces);
        
        verify(grantee, target, UserRight.RT_viewFreeBusy, ALLOW);
    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");

        TestUtil.runTest(TestACLTarget.class);
    }
}
