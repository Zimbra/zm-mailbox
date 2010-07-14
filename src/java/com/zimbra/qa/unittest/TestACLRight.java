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
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AllowedAttrs;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.qa.unittest.TestACL.AllowOrDeny;
import com.zimbra.qa.unittest.TestACL.TestViaGrant;

public class TestACLRight extends TestACL {

    private static Map<String, Object> ATTRS_IN_SET_SOME_ATTRS_RIGHT;
    private static Map<String, Object> ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT;
    private static Map<String, Object> ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT;
    
    private static Right GET_SOME_ATTRS_RIGHT;
    private static Right SET_SOME_ATTRS_RIGHT;
    private static Right GET_ALL_ATTRS_RIGHT;
    private static Right SET_ALL_ATTRS_RIGHT;
    private static Right CONFIGURE_CONSTRAINT_RIGHT;
    
    private static Right PRESET_RIGHT;
    private static Right COMBO_RIGHT;
    
    static int zimbraMailQuota_constraint_max = 1000;
    
    static {
        try {
            GET_SOME_ATTRS_RIGHT = TestACL.getRight("test-getAttrs-account-2");
            SET_SOME_ATTRS_RIGHT = TestACL.getRight("test-setAttrs-account-2");
            GET_ALL_ATTRS_RIGHT = TestACL.getRight("getAccount");
            SET_ALL_ATTRS_RIGHT = TestACL.getRight("modifyAccount");
            
            CONFIGURE_CONSTRAINT_RIGHT = RightManager.getInstance().getRight("configureCosConstraint");
            
            PRESET_RIGHT = TestACL.getRight("modifyAccount");
            COMBO_RIGHT = TestACL.getRight("test-combo-MultiTargetTypes-top");
            
            
            ATTRS_IN_SET_SOME_ATTRS_RIGHT = new HashMap<String, Object>();
            ATTRS_IN_SET_SOME_ATTRS_RIGHT.put("zimbraMailQuota", ""+(zimbraMailQuota_constraint_max-1));
            ATTRS_IN_SET_SOME_ATTRS_RIGHT.put("zimbraQuotaWarnPercent", "20");
            ATTRS_IN_SET_SOME_ATTRS_RIGHT.put("zimbraQuotaWarnInterval", "1d");
            ATTRS_IN_SET_SOME_ATTRS_RIGHT.put("zimbraQuotaWarnMessage", "foo");

            ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT = new HashMap<String, Object>();   
            ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT.put("zimbraMailQuota", ""+(zimbraMailQuota_constraint_max+1));
            
            ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT = new HashMap<String, Object>(); 
            ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT.put("zimbraMailQuota", "100");
            ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT.put("zimbraQuotaWarnPercent", "20");
            ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT.put("zimbraQuotaWarnInterval", "1d");
            ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT.put("zimbraQuotaWarnMessage", "foo");
            ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT.put("zimbraId", "blahblah");
            
        } catch (ServiceException e) {
            System.exit(1);
        }
    }
    
    /*
     * grant get SOME attrs
     */
    public void testGetSomeAttrs() throws Exception {
        String testName = getTestName();
        
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
        Right right = GET_SOME_ATTRS_RIGHT;
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, right, ALLOW);
        
        verify(GA, TA, GET_SOME_ATTRS_RIGHT, null, ALLOW);
        verify(GA, TA, GET_ALL_ATTRS_RIGHT, null, DENY);
    }
    
    /*
     * grant get ALL attrs
     */
    public void testGetAllAttrs() throws Exception {
        String testName = getTestName();
        
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
        Right right = SET_SOME_ATTRS_RIGHT;
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, right, ALLOW);
        
        verify(GA, TA, GET_SOME_ATTRS_RIGHT, null, ALLOW);
        verify(GA, TA, GET_ALL_ATTRS_RIGHT, null, DENY);
    }
    
    /*
     * grant set SOME attrs
     */
    public void testSetSomeAttrs() throws Exception {
        String testName = getTestName();
        
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
        Right right = SET_SOME_ATTRS_RIGHT;
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, right, ALLOW);
        
        // set constraint
        Cos cos = mProv.getCOS(TA);
        cos.unsetConstraint();
        Map<String, Object> cosConstraints = new HashMap<String,Object>();
        cos.addConstraint("zimbraMailQuota:max="+zimbraMailQuota_constraint_max, cosConstraints);
        mProv.modifyAttrs(cos, cosConstraints);
        
        
        // attrs in the right, yup
        verify(GA, TA, SET_SOME_ATTRS_RIGHT, ATTRS_IN_SET_SOME_ATTRS_RIGHT, ALLOW);
        
        // pass null for attr/value map, only attrs in the right are checked, constraints are not checked
        verify(GA, TA, SET_SOME_ATTRS_RIGHT, null, ALLOW);  
        
        // attrs not in the right, nope
        verify(GA, TA, SET_SOME_ATTRS_RIGHT, ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT, DENY);
        
        // attrs violates constraint, nope
        verify(GA, TA, SET_SOME_ATTRS_RIGHT, ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT, DENY);
        
        // get of the same attrs, yup
        verify(GA, TA, GET_SOME_ATTRS_RIGHT, null, ALLOW);
        
        // get all, nope!
        verify(GA, TA, GET_ALL_ATTRS_RIGHT, null, DENY);
        
        //
        // grant the configure constraint right on cos to the grantee
        //
        grantRight(authedAcct, TargetType.cos, cos, GranteeType.GT_USER, GA, CONFIGURE_CONSTRAINT_RIGHT, ALLOW);
        
        // now can set attrs beyond constraint
        verify(GA, TA, SET_SOME_ATTRS_RIGHT, ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT, ALLOW);
    }
    
    
    /*
     * grant set ALL attrs
     */
    public void testSetAllAttrs() throws Exception {
        String testName = getTestName();
        
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
        Right right = SET_ALL_ATTRS_RIGHT;
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(authedAcct, TargetType.account, TA, GranteeType.GT_USER, GA, right, ALLOW);
        
        // set constraint
        Cos cos = mProv.getCOS(TA);
        cos.unsetConstraint();
        Map<String, Object> cosConstraints = new HashMap<String,Object>();
        cos.addConstraint("zimbraMailQuota:max="+zimbraMailQuota_constraint_max, cosConstraints);
        mProv.modifyAttrs(cos, cosConstraints);
        
        
        // attrs in the right, yup
        verify(GA, TA, SET_ALL_ATTRS_RIGHT, ATTRS_IN_SET_SOME_ATTRS_RIGHT, ALLOW);
        
        // pass null for attr/value map, only attrs in the right are checked, constraints are not checked
        verify(GA, TA, SET_ALL_ATTRS_RIGHT, null, ALLOW);
        
        // attrs not in the right, yep
        verify(GA, TA, SET_ALL_ATTRS_RIGHT, ATTRS_NOT_IN_SET_SOME_ATTRS_RIGHT, ALLOW);
        
        // attrs violates constraint, nope
        verify(GA, TA, SET_ALL_ATTRS_RIGHT, ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT, DENY);
        
        // get of the same attrs, yup
        verify(GA, TA, GET_SOME_ATTRS_RIGHT, null, ALLOW);
        
        // get all, yep
        verify(GA, TA, GET_ALL_ATTRS_RIGHT, null, ALLOW);
    }
    
    public void testCheckComboRight() throws Exception {
        String testName = getTestName();

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
        Right right = getRight("test-combo-account-domain");
        
        /*
         * targets
         */
        String domainName = getSubDomainName(testName);
        Domain TD = mProv.createDomain(domainName, new HashMap<String, Object>());
        grantRight(authedAcct, TargetType.domain, TD, GranteeType.GT_USER, GA, right, ALLOW);
        
        // create an account in the domain
        Account TA = mProv.createAccount("user1@"+domainName, PASSWORD, null);
        
        boolean allowed;
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, TD, GranteeType.GT_USER, GA.getName(), right, POSITIVE);
        
        /*
         * check preset right
         */
        // 1. account right
        verify(GA, TD, getRight("test-preset-account"), null, DENY);
        verify(GA, TA, getRight("test-preset-account"), null, ALLOW);
        
        // 2. domain right
        verify(GA, TD, getRight("test-preset-domain"), null, ALLOW);
        verify(GA, TA, getRight("test-preset-domain"), null, DENY);
        
        // 3. not covered right
        verify(GA, TD, getRight("test-preset-cos"), null, DENY);
        
        /*
         * check setAttrs right
         */
        // 1. account right
        verify(GA, TD, getRight("test-setAttrs-account"), null, DENY);
        verify(GA, TA, getRight("test-setAttrs-account"), null, ALLOW);
        verify(GA, TD, getRight("test-setAttrs-distributionlist"), null, DENY);
        verify(GA, TA, getRight("test-setAttrs-distributionlist"), null, DENY);
        
        // 2. domain right
        verify(GA, TD, getRight("test-setAttrs-domain"), null, ALLOW);
        verify(GA, TA, getRight("test-setAttrs-domain"), null, DENY);
        
        // 3. account and domain right
        verify(GA, TD, getRight("test-setAttrs-accountDomain"), null, ALLOW);
        verify(GA, TA, getRight("test-setAttrs-accountDomain"), null, ALLOW);
        
        // 4. not covered right
        verify(GA, TD, getRight("modifyServer"), null, DENY);

        /*
         * check setAttrs right on each attr
         */
        Set<String> expectedAttrs;
        AllowedAttrs expected;
        
        // 1. account attrs
        expectedAttrs = new HashSet<String>();
        expectedAttrs.add(Provisioning.A_zimbraMailStatus);
        expectedAttrs.add(Provisioning.A_zimbraMailQuota);
        expectedAttrs.add(Provisioning.A_zimbraQuotaWarnPercent);
        expectedAttrs.add(Provisioning.A_zimbraQuotaWarnInterval);
        expectedAttrs.add(Provisioning.A_zimbraQuotaWarnMessage);
        expectedAttrs.add(Provisioning.A_displayName);
        expectedAttrs.add(Provisioning.A_description);
        expected = AllowedAttrs.ALLOW_SOME_ATTRS(expectedAttrs);
        verify(GA, TA, SET, expected);
        
        // 2. domain attrs
        expectedAttrs = new HashSet<String>();
        expectedAttrs.add(Provisioning.A_description);
        expectedAttrs.add(Provisioning.A_zimbraMailStatus);
        expectedAttrs.add(Provisioning.A_zimbraGalMode);
        expected = AllowedAttrs.ALLOW_SOME_ATTRS(expectedAttrs);
        verify(GA, TD, SET, expected);
    }
    
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        TestUtil.runTest(TestACLRight.class);
        
        /*
        TestACLRight test = new TestACLRight();
        test.testCanGrant();
        */
    }
    
}
