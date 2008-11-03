package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.Right;
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
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr("testTargetAccount-grantee"), PASSWORD, null);
        
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
         * setup targets
         */
        
        // grant on target account itself 
        Account target = mProv.createAccount(getEmailAddr("testTargetAccount-target"), PASSWORD, null);
        PermUtil.grantAccess(mProv, target, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr("testTargetAccount-group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr("testTargetAccount-group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        PermUtil.grantAccess(mProv, group1, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        PermUtil.grantAccess(mProv, domain, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        PermUtil.grantAccess(mProv, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, globalGrant, aces);
    }
        
    public void testTargetCalendarResource() throws Exception {
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr("testTargetCalendarResource-grantee"), PASSWORD, null);
        
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
         * setup targets
         */
        
        // grant on target calendar resource itself 
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "foo");
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        Account target = mProv.createCalendarResource(getEmailAddr("testTargetCalendarResource-target"), PASSWORD, attrs);
        PermUtil.grantAccess(mProv, target, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr("testTargetCalendarResource-group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr("testTargetCalendarResource-group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        PermUtil.grantAccess(mProv, group1, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        PermUtil.grantAccess(mProv, domain, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        PermUtil.grantAccess(mProv, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, globalGrant, aces);
    }
    
    public void testTargetGroup() throws Exception {
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr("testTargetGroup-grantee"), PASSWORD, null);
        
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
         * setup targets
         */
        
        // grant on target group itself 
        DistributionList target = mProv.createDistributionList(getEmailAddr("testTargetGroup-target"), new HashMap<String, Object>());
        PermUtil.grantAccess(mProv, target, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, target, aces);
        
        // grant on a group the target account is in
        DistributionList group1 = mProv.createDistributionList(getEmailAddr("testTargetGroup-group1"), new HashMap<String, Object>());
        DistributionList group2 = mProv.createDistributionList(getEmailAddr("testTargetGroup-group2"), new HashMap<String, Object>());
        mProv.addMembers(group1, new String[] {group2.getName()});
        mProv.addMembers(group2, new String[] {target.getName()});
        PermUtil.grantAccess(mProv, group1, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, group1, aces);
        
        // grant on the domain the target account is in
        Domain domain = mProv.getDomain(target);
        PermUtil.grantAccess(mProv, domain, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, domain, aces);
        
        // grant on the global grant
        GlobalGrant globalGrant = mProv.getGlobalGrant();
        PermUtil.grantAccess(mProv, globalGrant, aces);
        verify(grantee, target, right, ALLOW);
        PermUtil.revokeAccess(mProv, globalGrant, aces);
    }

    public void testTargetDomain() throws Exception {
        
    }
    
    public void testTargetCos() throws Exception {
        
    }
    
    public void testTargetServer() throws Exception {
        
    }
    
    public void testTargetConfig() throws Exception {
        
    }
    
    public void testTargetGlobalGrant() throws Exception {
        
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
    public void testGranteeGroupConflict() throws Exception {
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr("testGranteeGroup-grantee"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList GA = mProv.createDistributionList(getEmailAddr("testGranteeGroupConflict-GA"), new HashMap<String, Object>());
        DistributionList GB = mProv.createDistributionList(getEmailAddr("testGranteeGroupConflict-GB"), new HashMap<String, Object>());
        DistributionList GC = mProv.createDistributionList(getEmailAddr("testGranteeGroupConflict-GC"), new HashMap<String, Object>());

        mProv.addMembers(GA, new String[] {GB.getName()});
        mProv.addMembers(GB, new String[] {GC.getName()});
        mProv.addMembers(GC, new String[] {grantee.getName()});
        
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr("testGranteeGroupConflict-target"), PASSWORD, null);
        
        DistributionList G1 = mProv.createDistributionList(getEmailAddr("testGranteeGroupConflict-G1"), new HashMap<String, Object>());
        DistributionList G2 = mProv.createDistributionList(getEmailAddr("testGranteeGroupConflict-G2"), new HashMap<String, Object>());
        DistributionList G3 = mProv.createDistributionList(getEmailAddr("testGranteeGroupConflict-G3"), new HashMap<String, Object>());
        
        mProv.addMembers(G1, new String[] {G2.getName()});
        mProv.addMembers(G2, new String[] {G3.getName()});
        mProv.addMembers(G3, new String[] {target.getName()});
        
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(GA, UserRight.RT_viewFreeBusy, ALLOW));
        PermUtil.grantAccess(mProv, G1, aces);
        
        aces.clear();
        aces.add(newGrpACE(GB, UserRight.RT_viewFreeBusy, DENY));
        PermUtil.grantAccess(mProv, G2, aces);
        
        aces.clear();
        aces.add(newGrpACE(GC, UserRight.RT_viewFreeBusy, ALLOW));
        PermUtil.grantAccess(mProv, G3, aces);
        
        verify(grantee, target, UserRight.RT_viewFreeBusy, ALLOW);
    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");

        TestUtil.runTest(TestACLTarget.class);
    }
}
