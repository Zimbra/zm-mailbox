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
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AllowedAttrs;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;

public class TestACLAttrRight extends TestACL {
    
    
    static Right ATTR_RIGHT_GET_ALL;
    static Right ATTR_RIGHT_GET_SOME;
    
    static Right ATTR_RIGHT_SET_ALL;
    static Right ATTR_RIGHT_SET_SOME;
    
    // attrs covered by the ATTR_RIGHT_SOME right
    static final Map<String, Object> ATTRS_SOME;
    static final AllowedAttrs EXPECTED_SOME;
    static final AllowedAttrs EXPECTED_SOME_EMPTY;
    static final AllowedAttrs EXPECTED_ALL_MINUS_SOME;
    
    static final Map<String, Object> ATTRS_SOME_MORE;
    
    static {
        
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
        try {
            ALL_ACCOUNT_ATTRS = AttributeManager.getInstance().getAllAttrsInClass(AttributeClass.account);
            
            ATTR_RIGHT_GET_ALL   = TestACL.getRight("getAccount");
            ATTR_RIGHT_GET_SOME  = TestACL.getRight("test-getAttrs-account-2");
            ATTR_RIGHT_SET_ALL   = TestACL.getRight("modifyAccount");
            ATTR_RIGHT_SET_SOME  = TestACL.getRight("test-setAttrs-account-2");
            
        } catch (ServiceException e) {
            System.exit(1);
        }
        Set<String> ALL_ACCOUNT_ATTRS_MINUS_SOME = SetUtil.subtract(ALL_ACCOUNT_ATTRS, ATTRS_SOME.keySet());
        
        EXPECTED_SOME = AllowedAttrs.ALLOW_SOME_ATTRS(ATTRS_SOME.keySet());
        EXPECTED_SOME_EMPTY = AllowedAttrs.ALLOW_SOME_ATTRS(EMPTY_SET);
        EXPECTED_ALL_MINUS_SOME = AllowedAttrs.ALLOW_SOME_ATTRS(ALL_ACCOUNT_ATTRS_MINUS_SOME);
    }
    
    
    public void oneGrantSome(AllowOrDeny grant, GetOrSet getOrSet, AllowedAttrs expected) throws Exception {
        
        String testName = "oneGrantSome-" + grant.name() + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA = createAdminAccount(getEmailAddr(testName, "GA"));
        
        /*
         * grants
         */
        Right someRight;
        if (getOrSet.isGet())
            someRight = ATTR_RIGHT_GET_SOME;
        else
            someRight = ATTR_RIGHT_SET_SOME;
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, someRight, grant);
        
        verify(GA, TA, getOrSet, expected);
    }

    
    public void oneGrantAll(AllowOrDeny grant, GetOrSet getOrSet, AllowedAttrs expected) throws Exception {
        String testName = "oneGrantAll-" + grant.name() + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA = createAdminAccount(getEmailAddr(testName, "GA"));
        
        /*
         * grants
         */
        Right allRight;
        if (getOrSet.isGet())
            allRight = ATTR_RIGHT_GET_ALL;
        else
            allRight = ATTR_RIGHT_SET_ALL;
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, allRight, grant);
        
        verify(GA, TA, getOrSet, expected);
    }

    
    private void someAllSameLevel(AllowOrDeny some, AllowOrDeny all, GetOrSet getOrSet, AllowedAttrs expected) throws ServiceException {
        
        String testName = "someAllSameLevel-" + some.name() + "-some-" + all.name() + "-all-" + getOrSet.name();
       
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA = createAdminAccount(getEmailAddr(testName, "GA"));
        
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
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
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
                                 GetOrSet getOrSet,
                                 AllowedAttrs expected) throws Exception {

        String testName = "someAllDiffLevel-" + some.name() + "-some-" + all.name() + "-all-" + (someIsCloser?"someIsCloser":"allIsCloser") + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * setup authed account
         */
        Account authedAcct = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA = createAdminAccount(getEmailAddr(testName, "GA"));
        DistributionList GG = createAdminGroup(getEmailAddr(testName, "GG"));
        mProv.addMembers(GG, new String[] {GA.getName()});
        
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
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        
        if (someIsCloser) {
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, someRight, some);
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_GROUP, GG, allRight, all);
        } else {
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, allRight, all);
            grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_GROUP, GG, someRight, some);
        }

        
        verify(GA, TA, getOrSet, expected);
    }

    
    public void testOneGrantSome() throws Exception {
        oneGrantSome(ALLOW, SET, EXPECTED_SOME);
        oneGrantSome(DENY,  SET, EXPECTED_SOME_EMPTY);
        oneGrantSome(ALLOW, GET, EXPECTED_SOME);
        oneGrantSome(DENY,  GET, EXPECTED_SOME_EMPTY);
    }
    
    public void testOneGrantAll() throws Exception {
        oneGrantAll(ALLOW, SET, AllowedAttrs.ALLOW_ALL_ATTRS());
        oneGrantAll(DENY,  SET, AllowedAttrs.DENY_ALL_ATTRS());
        oneGrantAll(ALLOW, GET, AllowedAttrs.ALLOW_ALL_ATTRS());
        oneGrantAll(DENY,  GET, AllowedAttrs.DENY_ALL_ATTRS());
    }
    
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
    
    // TODO: add test for adding/substracting attrs between two allow/deny SOME rights 
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        TestUtil.runTest(TestACLAttrRight.class);
    }
    
}
