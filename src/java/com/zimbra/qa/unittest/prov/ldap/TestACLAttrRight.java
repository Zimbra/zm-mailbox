/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AllowedAttrs;
import com.zimbra.cs.account.accesscontrol.CheckAttrRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.qa.unittest.prov.ldap.ACLTestUtil.AllowOrDeny;
import com.zimbra.qa.unittest.prov.ldap.ACLTestUtil.GetOrSet;
import com.zimbra.soap.type.TargetBy;

public class TestACLAttrRight extends LdapTest {
    
    private static Right ATTR_RIGHT_GET_ALL;
    private static Right ATTR_RIGHT_GET_SOME;
    
    private static Right ATTR_RIGHT_SET_ALL;
    private static Right ATTR_RIGHT_SET_SOME;
    
    // attrs covered by the ATTR_RIGHT_SOME right
    private static Map<String, Object> ATTRS_SOME;
    private static AllowedAttrs EXPECTED_SOME;
    private static AllowedAttrs EXPECTED_SOME_EMPTY;
    private static AllowedAttrs EXPECTED_ALL_MINUS_SOME;
    
    private static Map<String, Object> ATTRS_SOME_MORE;
    
    private static final AllowOrDeny ALLOW = AllowOrDeny.ALLOW;
    private static final AllowOrDeny DENY = AllowOrDeny.DENY;
    protected static final GetOrSet GET = GetOrSet.GET;
    protected static final GetOrSet SET = GetOrSet.SET;
    
    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
    private static Domain domain;
    private static String DOMAIN_NAME;
    private static Account globalAdmin;
    
    @BeforeClass
    public static void init() throws Exception {
        
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
        DOMAIN_NAME = domain.getName();
        globalAdmin = provUtil.createGlobalAdmin("globaladmin", domain);
        
        Set<String> EMPTY_SET = new HashSet<String>();
        
        ATTRS_SOME = new HashMap<String, Object>();
        ATTRS_SOME.put(Provisioning.A_zimbraMailQuota, "123");
        ATTRS_SOME.put(Provisioning.A_zimbraQuotaWarnPercent, "123");
        ATTRS_SOME.put(Provisioning.A_zimbraQuotaWarnInterval, "123");
        ATTRS_SOME.put(Provisioning.A_zimbraQuotaWarnMessage, "123");
        
        ATTRS_SOME_MORE = new HashMap<String, Object>(ATTRS_SOME);
        ATTRS_SOME_MORE.put(Provisioning.A_zimbraFeatureMailEnabled, "TRUE");
        ATTRS_SOME_MORE.put(Provisioning.A_zimbraFeatureCalendarEnabled, "TRUE");
        ATTRS_SOME_MORE.put(Provisioning.A_zimbraPrefLocale, "en-us");
        
        Set<String> ALL_ACCOUNT_ATTRS = null;
        ALL_ACCOUNT_ATTRS = AttributeManager.getInstance().getAllAttrsInClass(AttributeClass.account);
            
        ATTR_RIGHT_GET_ALL   = ACLTestUtil.getRight("getAccount");
        ATTR_RIGHT_GET_SOME  = ACLTestUtil.getRight("test-getAttrs-account-2");
        ATTR_RIGHT_SET_ALL   = ACLTestUtil.getRight("modifyAccount");
        ATTR_RIGHT_SET_SOME  = ACLTestUtil.getRight("test-setAttrs-account-2");

        Set<String> ALL_ACCOUNT_ATTRS_MINUS_SOME = SetUtil.subtract(ALL_ACCOUNT_ATTRS, ATTRS_SOME.keySet());
        
        EXPECTED_SOME = AllowedAttrs.ALLOW_SOME_ATTRS(ATTRS_SOME.keySet());
        EXPECTED_SOME_EMPTY = AllowedAttrs.ALLOW_SOME_ATTRS(EMPTY_SET);
        EXPECTED_ALL_MINUS_SOME = AllowedAttrs.ALLOW_SOME_ATTRS(ALL_ACCOUNT_ATTRS_MINUS_SOME);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private String getAddress(String localPart) {
        return TestUtil.getAddress(localPart, DOMAIN_NAME);
    }
    
    private String getAddress(String testCaseName, String localPartSuffix) {
        return getAddress(testCaseName + "-" + localPartSuffix);
    }
    
    private Account createAccount(String acctName) throws Exception {
        return provUtil.createAccount(acctName);
    }
    
    
    /*
     * TODO: following methods (grantRight and verify) copied from legacy 
     *       com.zimbra.qa.unittest.TestACL.  
     *       Move to ACLTestUtil if used in other classes as we continue to renovate ACL 
     *       unit tests.
     * 
     * 
     * utility methods to grant/revoke right
     * 
     * To simulate how grants are done in the real server/zmprov, we first call TargetType.lookupTarget to 
     * "look for" the taret, then use the returned entry instead of giving the target entry passed in 
     * directly to RightUtil.
     * 
     * This is for testing user rights, which goes to RightUtil directly (i.e. not through RightCommand)
     * 
     */
    private List<ZimbraACE> grantRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        /*
         * make sure all rights are user right, tests written earlier could still be using 
         * this to grant
         */
        for (ZimbraACE ace : aces) {
            assertTrue(ace.getRight().isUserRight());
        }
        
        Entry targetEntry;
        if (target instanceof Zimlet) {
            // must be by name
            String targetName = ((Zimlet)target).getName();
            targetEntry = TargetType.lookupTarget(prov, targetType, TargetBy.name, targetName);
        } else {
            String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
            targetEntry = TargetType.lookupTarget(prov, targetType, TargetBy.id, targetId);
        }
        return ACLUtil.grantRight(prov, targetEntry, aces);
    }
    
    /*
     * for testing admin rights
     */
    private void grantRight(Account authedAcct,
                              TargetType targetType, NamedEntry target,
                              GranteeType granteeType, NamedEntry grantee,
                              Right right, AllowOrDeny grant) throws ServiceException {
        
        RightCommand.grantRight(prov, authedAcct,
                                targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), null,
                                right.getName(), grant.toRightModifier());
    }
    
    private void verify(Account grantee, Entry target, GetOrSet getOrSet, AllowedAttrs expected) 
    throws Exception {
        // call RightChecker directly instead of mAM, we want to verify the interim result.
        AllowedAttrs allowedAttrs = getOrSet.isGet() ? 
                CheckAttrRight.accessibleAttrs(new Grantee(grantee), target, AdminRight.PR_GET_ATTRS, false):
                CheckAttrRight.accessibleAttrs(new Grantee(grantee), target, AdminRight.PR_SET_ATTRS, false);
        // System.out.println("========== Test result ==========\n" + allowedAttrs.dump());
        verifyEquals(expected, allowedAttrs);
    }
    
    void verifyEquals(AllowedAttrs expected, AllowedAttrs actual) throws Exception {
        assertEquals(expected.getResult(), actual.getResult());
        if (actual.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            Verify.verifyEquals(expected.getAllowed(), actual.getAllowed());
        }
    }
    
    private void oneGrantSome(AllowOrDeny grant, GetOrSet getOrSet, AllowedAttrs expected) 
    throws Exception {
        
        String testName = "oneGrantSome-" + grant.name() + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
        
        /*
         * grantees
         */
        Account GA = provUtil.createDelegatedAdmin(getAddress(testName, "GA"));
        
        /*
         * grants
         */
        Right someRight;
        if (getOrSet.isGet()) {
            someRight = ATTR_RIGHT_GET_SOME;
        } else {
            someRight = ATTR_RIGHT_SET_SOME;
        }
        
        /*
         * targets
         */
        Account TA = createAccount(getAddress(testName, "TA"));
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, someRight, grant);
        
        verify(GA, TA, getOrSet, expected);
    }
    
    public void oneGrantAll(AllowOrDeny grant, GetOrSet getOrSet, AllowedAttrs expected) 
    throws Exception {
        String testName = "oneGrantAll-" + grant.name() + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
        
        /*
         * grantees
         */
        Account GA = provUtil.createDelegatedAdmin(getAddress(testName, "GA"));
        
        /*
         * grants
         */
        Right allRight;
        if (getOrSet.isGet()) {
            allRight = ATTR_RIGHT_GET_ALL;
        } else {
            allRight = ATTR_RIGHT_SET_ALL;
        }
        
        /*
         * targets
         */
        Account TA = createAccount(getAddress(testName, "TA"));
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, allRight, grant);
        
        verify(GA, TA, getOrSet, expected);
    }

    
    private void someAllSameLevel(AllowOrDeny some, AllowOrDeny all, 
            GetOrSet getOrSet, AllowedAttrs expected) 
    throws Exception {
        
        String testName = "someAllSameLevel-" + some.name() + "-some-" + 
                all.name() + "-all-" + getOrSet.name();
       
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
        
        /*
         * grantees
         */
        Account GA = provUtil.createDelegatedAdmin(getAddress(testName, "GA"));
        
        /*
         * grants
         */
        Right someRight;
        Right allRight;
        if (getOrSet.isGet()) {
            someRight = ATTR_RIGHT_GET_SOME;
            allRight = ATTR_RIGHT_GET_ALL;
        } else {
            someRight = ATTR_RIGHT_SET_SOME;
            allRight = ATTR_RIGHT_SET_ALL;
        }
        
        /*
         * targets
         */
        Account TA = createAccount(getAddress(testName, "TA"));
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, someRight, some);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, allRight, all);
        
        verify(GA, TA, getOrSet, expected);
    }
    
    /*
     * 2 grants
     * allow some at closer level, deny all at farther level
     * => should allow some
     */
    public void someAllDiffLevel(AllowOrDeny some, AllowOrDeny all, 
            boolean someIsCloser, // whether some or all is the closer grant
            GetOrSet getOrSet, AllowedAttrs expected) 
    throws Exception {

        String testName = "someAllDiffLevel-" + some.name() + "-some-" + 
            all.name() + "-all-" + (someIsCloser?"someIsCloser":"allIsCloser") + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = globalAdmin;
        
        /*
         * grantees
         */
        Account GA = provUtil.createDelegatedAdmin(getAddress(testName, "GA"));
        Group GG = provUtil.createAdminGroup(getAddress(testName, "GG"));
        prov.addGroupMembers(GG, new String[] {GA.getName()});
        
        /*
         * grants
         */
        Right someRight;
        Right allRight;
        if (getOrSet.isGet()) {
            someRight = ATTR_RIGHT_GET_SOME;
            allRight = ATTR_RIGHT_GET_ALL;
        } else {
            someRight = ATTR_RIGHT_SET_SOME;
            allRight = ATTR_RIGHT_SET_ALL;
        }
        
        /*
         * targets
         */
        Account TA = createAccount(getAddress(testName, "TA"));
        
        if (someIsCloser) {
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, someRight, some);
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_GROUP, GG, allRight, all);
        } else {
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, allRight, all);
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_GROUP, GG, someRight, some);
        }

        
        verify(GA, TA, getOrSet, expected);
    }

    @Test
    public void testOneGrantSome() throws Exception {
        oneGrantSome(ALLOW, SET, EXPECTED_SOME);
        oneGrantSome(DENY,  SET, EXPECTED_SOME_EMPTY);
        oneGrantSome(ALLOW, GET, EXPECTED_SOME);
        oneGrantSome(DENY,  GET, EXPECTED_SOME_EMPTY);
    }
    
    @Test
    public void testOneGrantAll() throws Exception {
        oneGrantAll(ALLOW, SET, AllowedAttrs.ALLOW_ALL_ATTRS());
        oneGrantAll(DENY,  SET, AllowedAttrs.DENY_ALL_ATTRS());
        oneGrantAll(ALLOW, GET, AllowedAttrs.ALLOW_ALL_ATTRS());
        oneGrantAll(DENY,  GET, AllowedAttrs.DENY_ALL_ATTRS());
    }
    
    @Test
    public void testTwoGrantsSameLevel() throws Exception {
        someAllSameLevel(ALLOW, ALLOW, SET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllSameLevel(DENY,  ALLOW, SET, EXPECTED_ALL_MINUS_SOME);
        someAllSameLevel(ALLOW, DENY,  SET, AllowedAttrs.DENY_ALL_ATTRS());
        someAllSameLevel(DENY,  DENY,  SET, AllowedAttrs.DENY_ALL_ATTRS());

        someAllSameLevel(ALLOW, ALLOW, GET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllSameLevel(DENY,  ALLOW, GET, EXPECTED_ALL_MINUS_SOME);
        someAllSameLevel(ALLOW, DENY,  GET, AllowedAttrs.DENY_ALL_ATTRS());
        someAllSameLevel(DENY,  DENY,  GET, AllowedAttrs.DENY_ALL_ATTRS());
    }

    @Test
    public void testTwoGrantsDiffLevel() throws Exception {
        //               some   all    some-is-closer
        someAllDiffLevel(ALLOW, ALLOW, true, SET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllDiffLevel(DENY,  ALLOW, true, SET, EXPECTED_ALL_MINUS_SOME);
        someAllDiffLevel(ALLOW, DENY,  true, SET, EXPECTED_SOME);
        someAllDiffLevel(DENY,  DENY,  true, SET, AllowedAttrs.DENY_ALL_ATTRS());
        
        someAllDiffLevel(ALLOW, ALLOW, false, SET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllDiffLevel(DENY,  ALLOW, false, SET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllDiffLevel(ALLOW, DENY,  false, SET, AllowedAttrs.DENY_ALL_ATTRS());
        someAllDiffLevel(DENY,  DENY,  false, SET, AllowedAttrs.DENY_ALL_ATTRS());
        
        someAllDiffLevel(ALLOW, ALLOW, true, GET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllDiffLevel(DENY,  ALLOW, true, GET, EXPECTED_ALL_MINUS_SOME);
        someAllDiffLevel(ALLOW, DENY,  true, GET, EXPECTED_SOME);
        someAllDiffLevel(DENY,  DENY,  true, GET, AllowedAttrs.DENY_ALL_ATTRS());
        
        someAllDiffLevel(ALLOW, ALLOW, false, GET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllDiffLevel(DENY,  ALLOW, false, GET, AllowedAttrs.ALLOW_ALL_ATTRS());
        someAllDiffLevel(ALLOW, DENY,  false, GET, AllowedAttrs.DENY_ALL_ATTRS());
        someAllDiffLevel(DENY,  DENY,  false, GET, AllowedAttrs.DENY_ALL_ATTRS());
    }
    
}
