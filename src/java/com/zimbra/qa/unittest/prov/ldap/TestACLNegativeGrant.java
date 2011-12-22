/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.qa.unittest.prov.ldap.ACLTestUtil.AllowOrDeny;
import com.zimbra.qa.unittest.prov.ldap.ACLTestUtil.AsAdmin;
import com.zimbra.qa.unittest.prov.ldap.ACLTestUtil.TestViaGrant;
import com.zimbra.soap.type.TargetBy;

public class TestACLNegativeGrant extends LdapTest {

    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
    private static Domain baseDomain;
    private static String BASE_DOMAIN_NAME;
    private static Account globalAdmin;
    
    @BeforeClass
    public static void init() throws Exception {
        
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        baseDomain = provUtil.createDomain(baseDomainName());
        BASE_DOMAIN_NAME = baseDomain.getName();
        globalAdmin = provUtil.createGlobalAdmin("globaladmin", baseDomain);
        
        ACLTestUtil.initTestRights();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private void grantRight(Account authedAcct,
            TargetType targetType, NamedEntry target,
            GranteeType granteeType, NamedEntry grantee,
            Right right, AllowOrDeny grant) throws ServiceException {

        RightCommand.grantRight(prov, authedAcct,
                      targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                      granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), null,
                      right.getName(), grant.toRightModifier());
    }
    
    protected void revokeRight(Account authedAcct,
            TargetType targetType, NamedEntry target,
            GranteeType granteeType, NamedEntry grantee,
            Right right, AllowOrDeny grant) throws ServiceException {

        RightCommand.revokeRight(prov, authedAcct,
                      targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                      granteeType.getCode(), Key.GranteeBy.name, grantee.getName(),
                      right.getName(), grant.toRightModifier());
    }
    
    protected void verify(Account grantee, Entry target, Right right, AsAdmin asAdmin, 
            AllowOrDeny expected, TestViaGrant expectedVia) 
    throws Exception {
        AccessManager accessMgr = AccessManager.getInstance();
        
        boolean result;
        
        // Account interface
        ViaGrant via = (expectedVia==null)?null:new ViaGrant();
        result = accessMgr.canDo(grantee==null?null:grantee, target, right, asAdmin.yes(), via);
        assertEquals(expected.allow(), result);
        TestViaGrant.verifyEquals(expectedVia, via);
        
        // AuthToken interface
        via = (expectedVia==null)?null:new ViaGrant();
        result = accessMgr.canDo(grantee==null?null:AuthProvider.getAuthToken(grantee), 
                target, right, asAdmin.yes(), via);
        assertEquals(expected.allow(), result);
        TestViaGrant.verifyEquals(expectedVia, via);
        
        // String interface
        via = (expectedVia==null)?null:new ViaGrant();
        result = accessMgr.canDo(grantee==null?null:grantee.getName(), 
                target, right, asAdmin.yes(), via);
        if (grantee instanceof GuestAccount && ((GuestAccount)grantee).getAccessKey() != null) {
            // string interface always return denied for key grantee unless there is a pub grant
            // skip the test for now, unless we want to pass yet another parameter to this method
            // i.e. - if no pub grant: should always expect false
            //      - if there is a pub grant: should expect the expected
            return;
        }
        assertEquals(expected.allow(), result);
        TestViaGrant.verifyEquals(expectedVia, via);
    }


    /*
     * Verify denied takes precedence
     * 
     * Grant to two unrelated groups: one allowed, one denied
     * account is a member of both groups
     *
     * Expected: account should be denied
     */
    @Test
    public void groupGranteeTest1() throws Exception {
        Account authedAcct = globalAdmin;
        
        Right right = ACLTestUtil.ADMIN_PRESET_ACCOUNT;
        
        /*
         * setup grantees
         */
        Account account = provUtil.createDelegatedAdmin(genAcctNameLocalPart("acct"), baseDomain);
        Group group1 = provUtil.createAdminGroup(genAcctNameLocalPart("group1"), baseDomain);
        Group group2 = provUtil.createAdminGroup(genAcctNameLocalPart("group2"), baseDomain);
        prov.addGroupMembers(group1, new String[] {account.getName()});
        prov.addGroupMembers(group2, new String[] {account.getName()});
        
        /*
         * setup targets
         */
        Account target = provUtil.createAccount(genAcctNameLocalPart("target"), baseDomain);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, group1, right, AllowOrDeny.ALLOW);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, group2, right, AllowOrDeny.DENY);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, group2.getName(), right, TestViaGrant.NEGATIVE);
        verify(account, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.DENY, via);
    }
    
    
    /*
     * Verify denied takes precedence
     * 
     *
     * Membership:
     *          G1(A)                      G4(D)
     *          / \                        / \
     *         A  G2(D)                  A  G5(A)
     *             / \                        / \
     *            A  G3(A)                   A  G6(D)
     *                 |                          |
     *                 A                          A
     * 
     *
     * Grantee:
     *     GG1(allow), GG2(deny), GG3(allow), GG4(deny), GG5(allow), GG6(deny)
     *     
     * Target:
     *     granted on the same target entry - TA
     * 
     * Expected: 
     *     Denied via grants to G2 or G4 or G6
     * 
     */
    public void groupGranteeTest2() throws Exception {
        Domain domain = provUtil.createDomain(genDomainSegmentName() + "." + BASE_DOMAIN_NAME);
        
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
        
        Right right = ACLTestUtil.ADMIN_PRESET_ACCOUNT;
        
        /*
         * setup grantees
         */
        Account account = provUtil.createDelegatedAdmin(genAcctNameLocalPart("account"), domain);
        
        /*
         * setup groups
         */
        Group GG1 = provUtil.createAdminGroup(genGroupNameLocalPart("GG1"), domain);
        Group GG2 = provUtil.createAdminGroup(genGroupNameLocalPart("GG2"), domain);
        Group GG3 = provUtil.createAdminGroup(genGroupNameLocalPart("GG3"), domain);
        Group GG4 = provUtil.createAdminGroup(genGroupNameLocalPart("GG4"), domain);
        Group GG5 = provUtil.createAdminGroup(genGroupNameLocalPart("GG5"), domain);
        Group GG6 = provUtil.createAdminGroup(genGroupNameLocalPart("GG6"), domain);

        prov.addGroupMembers(GG1, new String[] {account.getName(), GG2.getName()});
        prov.addGroupMembers(GG2, new String[] {account.getName(), GG3.getName()});
        prov.addGroupMembers(GG3, new String[] {account.getName()});
        prov.addGroupMembers(GG4, new String[] {account.getName(), GG5.getName()});
        prov.addGroupMembers(GG5, new String[] {account.getName(), GG6.getName()});
        prov.addGroupMembers(GG6, new String[] {account.getName()});
        
        
        /*
         * setup targets
         */
        Account target = provUtil.createAccount(genAcctNameLocalPart("target"), domain);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, GG1, right, AllowOrDeny.ALLOW);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, GG2, right, AllowOrDeny.DENY);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, GG3, right, AllowOrDeny.ALLOW);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, GG4, right, AllowOrDeny.DENY);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, GG5, right, AllowOrDeny.ALLOW);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_GROUP, GG6, right, AllowOrDeny.DENY);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, GG2.getName(), right, TestViaGrant.NEGATIVE);
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, GG4.getName(), right, TestViaGrant.NEGATIVE));
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, GG6.getName(), right, TestViaGrant.NEGATIVE));
        verify(account, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.DENY, via);
    }
    
    /*
    Combining Target Scope and Grantee Scope: Grantee Relativity takes Precedence over Target Relativity
      For example, for this target hierarchy:
          domain D
              group G1 (allow right R to group GC)
                  group G2 (deny right R to group GB)
                      group G3 (deny right R to group GA)
                          user account U   
                      
      And this grantee hierarchy:
          group GA
              group GB
                  group GC
                      (admin) account A
                  
      Then A is *allowed* for right R on target account U, because GC is more specific to A than GA and GB.
      Even if on the target side, grant on G3(grant to GA) and G2(grant to GB) is more specific than the 
      grant on G1(grant to GC).
      
      The above is no longer true, it should be DENIED.          
    */
    @Test
    public void groupGranteeTest3() throws Exception {
        Domain domain = provUtil.createDomain(genDomainSegmentName() + "." + BASE_DOMAIN_NAME);
       
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
       
        Right right = ACLTestUtil.ADMIN_PRESET_ACCOUNT;
       
        /*
         * setup grantees
         */
        Account account = provUtil.createDelegatedAdmin(genAcctNameLocalPart("account"), domain);
       
        /*
         * setup grantee groups
         */
        Group GA = provUtil.createAdminGroup(genGroupNameLocalPart("GA"), domain);
        Group GB = provUtil.createAdminGroup(genGroupNameLocalPart("GB"), domain);
        Group GC = provUtil.createAdminGroup(genGroupNameLocalPart("GC"), domain);

        prov.addGroupMembers(GA, new String[] {GB.getName()});
        prov.addGroupMembers(GB, new String[] {GC.getName()});
        prov.addGroupMembers(GC, new String[] {account.getName()});
       
       
        /*
         * setup targets
         */
        TestViaGrant via;
       
        Account target = provUtil.createAccount(genAcctNameLocalPart("target"), domain);
       
        Group G1 = provUtil.createDistributionList(genGroupNameLocalPart("G1"), domain);
        Group G2 = provUtil.createDistributionList(genGroupNameLocalPart("G2"), domain);
        Group G3 = provUtil.createDistributionList(genGroupNameLocalPart("G3"), domain);
       
        prov.addGroupMembers(G1, new String[] {G2.getName()});
        prov.addGroupMembers(G2, new String[] {G3.getName()});
        prov.addGroupMembers(G3, new String[] {target.getName()});
       
        grantRight(authedAcct, TargetType.dl, G1, GranteeType.GT_GROUP, GC, right, AllowOrDeny.ALLOW);
        grantRight(authedAcct, TargetType.dl, G2, GranteeType.GT_GROUP, GB, right, AllowOrDeny.DENY);
        grantRight(authedAcct, TargetType.dl, G3, GranteeType.GT_GROUP, GA, right, AllowOrDeny.DENY);
       
 
        /* NO longer the case
        // the right should be allowed via the grant on G1, granted to group GC 
        via = new TestViaGrant(TargetType.dl, G1, GranteeType.GT_GROUP, GC.getName(), right, TestViaGrant.POSITIVE);
        verify(account, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.ALLOW, via);
        */
       
        via = new TestViaGrant(TargetType.dl, G2, GranteeType.GT_GROUP, GB.getName(), right, TestViaGrant.NEGATIVE);
        via.addCanAlsoVia(new TestViaGrant(TargetType.dl, G3, GranteeType.GT_GROUP, GA.getName(), right, TestViaGrant.NEGATIVE));
        verify(account, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.DENY, via);
       
    }
    
    /*
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
     */
    @Test
    public void targetPrecedence() throws Exception {
        Domain domain = provUtil.createDomain(genDomainSegmentName() + "." + BASE_DOMAIN_NAME);
        
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
        
        Right right = ACLTestUtil.ADMIN_PRESET_ACCOUNT;
        
        /*
         * setup grantees
         */
        Account grantee = provUtil.createDelegatedAdmin(genAcctNameLocalPart("grantee"), domain);

        /*
         * setup targets
         */
        // 1. target account itself
        Account target = provUtil.createAccount(genAcctNameLocalPart("target"), domain);
        grantRight(authedAcct, TargetType.account, target, GranteeType.GT_USER, grantee, right, AllowOrDeny.ALLOW);
        
        // 2. groups the target account is a member of
        DistributionList group1 = provUtil.createDistributionList(genGroupNameLocalPart("group1"), domain);
        DistributionList group2 = provUtil.createDistributionList(genGroupNameLocalPart("group2"), domain);
        prov.addMembers(group1, new String[] {group2.getName()});
        prov.addMembers(group2, new String[] {target.getName()});
        
        grantRight(authedAcct, TargetType.dl, group2, GranteeType.GT_USER, grantee, right, AllowOrDeny.DENY);
        grantRight(authedAcct, TargetType.dl, group1, GranteeType.GT_USER, grantee, right, AllowOrDeny.ALLOW);
        
        // 3. domain the target account is in
        grantRight(authedAcct, TargetType.domain, domain, GranteeType.GT_USER, grantee, right, AllowOrDeny.DENY);
        
        // 4. global grant
        GlobalGrant globalGrant = prov.getGlobalGrant();
        grantRight(authedAcct, TargetType.global, null, GranteeType.GT_USER, grantee, right, AllowOrDeny.ALLOW);
        
        /*
         * test targets
         */
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, grantee.getName(), right, TestViaGrant.POSITIVE);
        verify(grantee, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.ALLOW, via);
        
        // revoke the grant on target account, then grant on group2 should take effect
        revokeRight(authedAcct, TargetType.account, target, GranteeType.GT_USER, grantee, right, AllowOrDeny.ALLOW);
        via = new TestViaGrant(TargetType.dl, group2, GranteeType.GT_USER, grantee.getName(), right, TestViaGrant.NEGATIVE);
        verify(grantee, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.DENY, via);
        
        // revoke the grant on group2, then grant on group1 should take effect
        revokeRight(authedAcct, TargetType.dl, group2, GranteeType.GT_USER, grantee, right, AllowOrDeny.DENY);
        via = new TestViaGrant(TargetType.dl, group1, GranteeType.GT_USER, grantee.getName(), right, TestViaGrant.POSITIVE);
        verify(grantee, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.ALLOW, via);
        
        // revoke the grant on group1, then grant on domain should take effect
        revokeRight(authedAcct, TargetType.dl, group1, GranteeType.GT_USER, grantee, right, AllowOrDeny.ALLOW);
        via = new TestViaGrant(TargetType.domain, domain, GranteeType.GT_USER, grantee.getName(), right, TestViaGrant.NEGATIVE);
        verify(grantee, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.DENY, via);
        
        // revoke the grant on domain, then grant on globalgrant shuld take effect
        revokeRight(authedAcct, TargetType.domain, domain, GranteeType.GT_USER, grantee, right, AllowOrDeny.DENY);
        via = new TestViaGrant(TargetType.global, globalGrant, GranteeType.GT_USER, grantee.getName(), right, TestViaGrant.POSITIVE);
        verify(grantee, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.ALLOW, via);
        
        // revoke the grant on globalgrant, then there is no grant and callsite default should be honored 
        revokeRight(authedAcct, TargetType.global, null, GranteeType.GT_USER, grantee, right, AllowOrDeny.ALLOW);
        via = null;
        verify(grantee, target, right, AsAdmin.AS_ADMIN, AllowOrDeny.DENY, via);
        
    }
}
