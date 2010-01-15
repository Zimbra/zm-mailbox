/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.ZimletBy;
import com.zimbra.cs.account.Provisioning.XMPPComponentBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;

public class TestACLGrant extends TestACL {

    private static String TEST_CASE_NAME = "TestACLGrant-";
    
    private static String ACCOUNT_NAME           = getEmailAddr(TEST_CASE_NAME + "account").toLowerCase();
    private static String CALENDAR_RESOURCE_NAME = getEmailAddr(TEST_CASE_NAME + "cr").toLowerCase();
    private static String COS_NAME               = TEST_CASE_NAME + "cos".toLowerCase();
    private static String DISTRIBUTION_LIST_NAME = getEmailAddr(TEST_CASE_NAME + "dl").toLowerCase();
    private static String SUBDOMAIN_NAME         = getSubDomainName(TEST_CASE_NAME +  "domain").toLowerCase();
    private static String SERVER_NAME            = TEST_CASE_NAME + "server".toLowerCase();
    private static String XMPP_COMPONENT_NAME    = TEST_CASE_NAME + "xmppcomponent".toLowerCase();
    private static String ZIMLET_NAME            = TEST_CASE_NAME + "zimlet".toLowerCase();
    
    private Account getAccount() throws ServiceException {
        Account acct = mProv.get(AccountBy.name,ACCOUNT_NAME);
        if (acct == null)
            acct = mProv.createAccount(ACCOUNT_NAME, PASSWORD, null);
        return acct;
    }
    
    private CalendarResource getCalendarResource() throws ServiceException {
        CalendarResource cr = mProv.get(CalendarResourceBy.name, CALENDAR_RESOURCE_NAME);
        if (cr == null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_displayName, "CALENDAR_RESOURCE_NAME");
            attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
            cr = mProv.createCalendarResource(CALENDAR_RESOURCE_NAME, PASSWORD, attrs);
        }
        return cr;
    }
    
    private Cos getCos() throws ServiceException {
        Cos cos = mProv.get(CosBy.name, COS_NAME);
        if (cos == null)
            cos = mProv.createCos(COS_NAME, null);
        return cos;
    }
    
    private DistributionList getDistributionList() throws ServiceException {
        DistributionList dl = mProv.get(DistributionListBy.name, DISTRIBUTION_LIST_NAME);
        if (dl == null)
            dl = mProv.createDistributionList(DISTRIBUTION_LIST_NAME, new HashMap<String, Object>());
        return dl;
    }
    
    private Domain getDomain() throws ServiceException {
        Domain domain = mProv.get(DomainBy.name, SUBDOMAIN_NAME);
        if (domain == null)
            domain = mProv.createDomain(SUBDOMAIN_NAME, new HashMap<String, Object>());
        return domain;
    }
    
    private Server getServer() throws ServiceException {
        Server server = mProv.get(ServerBy.name, SERVER_NAME);
        if (server == null)
            server = mProv.createServer(SERVER_NAME, new HashMap<String, Object>());
        return server;
    }
    
    private XMPPComponent getXMPPComponent() throws ServiceException {
        XMPPComponent xmppCpnt = mProv.get(XMPPComponentBy.name, XMPP_COMPONENT_NAME);
        if (xmppCpnt == null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraXMPPComponentCategory, "whatever");
            attrs.put(Provisioning.A_zimbraXMPPComponentClassName, "whatever");
            attrs.put(Provisioning.A_zimbraXMPPComponentType, "whatever");
            xmppCpnt = mProv.createXMPPComponent(XMPP_COMPONENT_NAME, getDomain(), getServer(), attrs);
        }
        return xmppCpnt;
    }
    
    private Zimlet getZimlet() throws ServiceException {
        Zimlet zimlet = mProv.getZimlet(ZIMLET_NAME);
        if (zimlet == null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
            zimlet = mProv.createZimlet(ZIMLET_NAME, attrs);
        }
        return zimlet;
    }
    
    private void doTargetTest(Account authedAcct, Account grantee, TargetType targetType, NamedEntry target, Right right, Set<TargetType> expected) 
        throws ServiceException {
        Boolean good = null;
        
        try {
            grantRight(authedAcct, targetType, target, GranteeType.GT_USER, grantee, right, ALLOW);
            revokeRight(authedAcct, targetType, target, GranteeType.GT_USER, grantee, right, ALLOW);
            good = Boolean.TRUE;
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.INVALID_REQUEST)) {
                good = Boolean.FALSE;  // yup, just what we want
                // System.out.println(e.getMessage());
            } else
                throw e; // rethrow
        }
        if (expected.contains(targetType))
            assertEquals(Boolean.TRUE, good);
        else
            assertEquals(Boolean.FALSE, good);
    }
    
    private void doTargetTest(Account authedAcct, Account grantee, Right right,
                        Set<TargetType> expected) throws ServiceException {
        
        doTargetTest(authedAcct, grantee, TargetType.account, getAccount(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.calresource, getCalendarResource(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.cos, getCos(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.dl, getDistributionList(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.domain, getDomain(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.server, getServer(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.xmppcomponent, getXMPPComponent(), right, expected);
        doTargetTest(authedAcct, grantee, TargetType.zimlet, getZimlet(), right, expected);

        doTargetTest(authedAcct, grantee, TargetType.config, null, right, expected);
        doTargetTest(authedAcct, grantee, TargetType.global, null, right, expected);
    }
    
    public void testAccountRight() throws Exception {
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
         * expected
         */
        Set<TargetType> expected = new HashSet<TargetType>();
        
        /*
         * single target rights
         */
        expected.add(TargetType.account);
        expected.add(TargetType.calresource);
        expected.add(TargetType.dl);
        expected.add(TargetType.domain);
        expected.add(TargetType.global);
        
        // preset right
        doTargetTest(authedAcct, GA, getRight("test-preset-account"), expected);
        
        // getAttrs right
        doTargetTest(authedAcct, GA, getRight("test-getAttrs-account"), expected);
        doTargetTest(authedAcct, GA, getRight(inlineRightGet(TargetType.account, "description")), expected);
       
        // setAttrs right
        doTargetTest(authedAcct, GA, getRight("test-setAttrs-account"), expected);
        doTargetTest(authedAcct, GA, getRight(inlineRightSet(TargetType.account, "description")), expected);
        
        // combo right
        doTargetTest(authedAcct, GA, getRight("test-combo-account"), expected);
        
        /*
         * multi targets rights
         */
        expected.clear();
        expected.add(TargetType.account);
        expected.add(TargetType.cos);
        expected.add(TargetType.calresource);
        expected.add(TargetType.dl);
        expected.add(TargetType.domain);
        expected.add(TargetType.global);

        doTargetTest(authedAcct, GA, getRight("test-getAttrs-accountCos"), expected);
        doTargetTest(authedAcct, GA, getRight("test-setAttrs-accountCos"), expected);
        
        expected.clear();
        expected.add(TargetType.global);
        doTargetTest(authedAcct, GA, getRight("test-combo-account-cos-accountCos"), expected);
    }
    
    /* todo
    public void testCalendarResourceRight() throws Exception {}
    public void testCosRight() throws Exception {}
    public void testDistributionListRight() throws Exception {}
    public void testDomainRight() throws Exception {}
    public void testServerRight() throws Exception {}
    public void testXMPPComponentRight() throws Exception {}
    public void testZimletRight() throws Exception {}
    public void testConfigRight() throws Exception {}
    public void testGlobalRight() throws Exception {}
    */
    
    enum Result {
        // the grant is good
        GOOD,               
        
        // the right cannot be granted on the target type
        INVALID_REQUEST,    
        
        // the right is OK for the target type, but the authed user 
        // does not have permission to delegate the right on the target entry.
        // if an attempt to grant is both INVALID_REQUEST and PERM_DENIED,
        // we shoild hit INVALID_REQUEST first.
        PERM_DENIED           
    }
    
    private void doTestGrant(Account delegator,
            TargetType targetType, NamedEntry target,
            GranteeType delegateeType, NamedEntry delegatee,
            Right right, AllowOrDeny allowOrDeny,
            Result expected) throws ServiceException {
        Result actual = null;
        
        // test grant
        try {
            grantRight(delegator, targetType, target, delegateeType, delegatee, right, allowOrDeny);
            actual = Result.GOOD;
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.INVALID_REQUEST))
                actual = Result.INVALID_REQUEST;
            else if (e.getCode().equals(ServiceException.PERM_DENIED))
                actual = Result.PERM_DENIED;
            else
                throw e;
        }
        assertEquals(expected, actual);
    }
    
    private void doTestRevoke(Account delegator,
            TargetType targetType, NamedEntry target,
            GranteeType delegateeType, NamedEntry delegatee,
            Right right, AllowOrDeny allowOrDeny,
            Result expected) throws ServiceException {
        
        Result actual = null;
        
        // test revoke
        actual = null;
        try {
            revokeRight(delegator, targetType, target, delegateeType, delegatee, right, allowOrDeny);
            actual = Result.GOOD;
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.INVALID_REQUEST))
                actual = Result.INVALID_REQUEST;
            else if (e.getCode().equals(ServiceException.PERM_DENIED))
                actual = Result.PERM_DENIED;
            else
                throw e;
        }
        assertEquals(expected, actual);
    }
    private void doTestDelegate(Account delegator,
            TargetType targetType, NamedEntry target,
            GranteeType delegateeType, NamedEntry delegatee,
            Right right, AllowOrDeny allowOrDeny,
            Result expected) throws ServiceException {
        doTestGrant(delegator, targetType, target, delegateeType, delegatee, right, allowOrDeny, expected);
        doTestRevoke(delegator, targetType, target, delegateeType, delegatee, right, allowOrDeny, expected);
    }
    
    private void doTestDelegate(Account delegator,
            TargetType targetType, NamedEntry target,
            GranteeType delegateeType, NamedEntry delegatee,
            Right right,
            Result expected) throws ServiceException {
        
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, right, ALLOW, expected);
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, right, DENY, expected);
    }
    
    
    /*
     * expectedHasDelegateRight is either GOOD or PERM_DENIED.  
     * INVALID_REQUEST is figured out in this method.
     * 
     * Callsites of this method all have the combo right granted.  
     * This method check each sub-right ofthe combo right.
     */
    private void doDelegatePartialRight(Account delegator,
                                        TargetType targetType, NamedEntry target,
                                        GranteeType delegateeType, NamedEntry delegatee, 
                                        Result expectedHasDelegateRight)
    throws ServiceException {
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, getRight("test-preset-account"), ALLOW, expectedHasDelegateRight);
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, getRight("test-getAttrs-account"), ALLOW, expectedHasDelegateRight);
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, getRight("test-setAttrs-account"), ALLOW, expectedHasDelegateRight);
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, getRight(inlineRightGet(TargetType.account, "description")), ALLOW, expectedHasDelegateRight);
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, getRight(inlineRightSet(TargetType.account, "description")), ALLOW, expectedHasDelegateRight);
        
        Result expected;
        if (targetType == TargetType.domain || targetType == TargetType.global)
            expected = expectedHasDelegateRight;
        else
            expected = Result.INVALID_REQUEST;
        doTestDelegate(delegator, targetType, target, delegateeType, delegatee, getRight("test-preset-domain"), ALLOW, expected);
    }
    
    public void testDelegate() throws Exception {
        String testName = getTestName();

        /*
         * sys admin
         */
        Account sysAdmin = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA_DELEGATOR = createAdminAccount(getEmailAddr(testName, "GA_DELEGATOR"));
        Account GA_DELEGATEE = createAdminAccount(getEmailAddr(testName, "GA_DELEGATEE"));
        DistributionList GG_DELEGATEE = createAdminGroup(getEmailAddr(testName, "GG_DELEGATEE"));
        
        /*
         * target
         */
        String domainName = getSubDomainName(testName).toLowerCase();
        Domain TD = mProv.createDomain(domainName, new HashMap<String, Object>());
        
        /*
         * right
         */
        Right right = getRight("test-combo-account-domain");
        
        // authed as sys admin, can always grant
        // grant a delegate right
        grantDelegableRight(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATOR, right);
        
        /*
         * setup other targets
         */
        DistributionList subTargetDl = createGroup("dl@"+domainName);
        Account subTargetAcct = createAccount("acct@"+domainName);
        Domain otherDomain = mProv.createDomain("other."+domainName, new HashMap<String, Object>());
        
        // authed as a regular admin
        
        /*
         * delegate the same right
         */ 
        // on the same target
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, right, Result.GOOD);
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_GROUP, GG_DELEGATEE, right, Result.GOOD);
        
        // on sub target
        doTestDelegate(GA_DELEGATOR, TargetType.dl, subTargetDl, GranteeType.GT_USER, GA_DELEGATEE, right, Result.INVALID_REQUEST);
        doTestDelegate(GA_DELEGATOR, TargetType.dl, subTargetDl, GranteeType.GT_GROUP, GG_DELEGATEE, right, Result.INVALID_REQUEST);
        doTestDelegate(GA_DELEGATOR, TargetType.account, subTargetAcct, GranteeType.GT_USER, GA_DELEGATEE, right, Result.INVALID_REQUEST);
        doTestDelegate(GA_DELEGATOR, TargetType.account, subTargetAcct, GranteeType.GT_GROUP, GG_DELEGATEE, right, Result.INVALID_REQUEST);
        
        // on unrelated target
        doTestDelegate(GA_DELEGATOR, TargetType.domain, otherDomain, GranteeType.GT_USER, GA_DELEGATEE, right, Result.PERM_DENIED);
        doTestDelegate(GA_DELEGATOR, TargetType.domain, otherDomain, GranteeType.GT_GROUP, GG_DELEGATEE, right, Result.PERM_DENIED);
        
        // on super target
        doTestDelegate(GA_DELEGATOR, TargetType.global, null, GranteeType.GT_USER, GA_DELEGATEE, right, Result.PERM_DENIED);
        doTestDelegate(GA_DELEGATOR, TargetType.global, null, GranteeType.GT_GROUP, GG_DELEGATEE, right, Result.PERM_DENIED);
        
        /*
         * delegate part of the right
         */ 
        // on the same target
        doDelegatePartialRight(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, Result.GOOD);
        doDelegatePartialRight(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_GROUP, GG_DELEGATEE, Result.GOOD);
        
        // on sub target
        doDelegatePartialRight(GA_DELEGATOR, TargetType.dl, subTargetDl, GranteeType.GT_USER, GA_DELEGATEE, Result.GOOD);
        doDelegatePartialRight(GA_DELEGATOR, TargetType.dl, subTargetDl, GranteeType.GT_GROUP, GG_DELEGATEE, Result.GOOD);
        doDelegatePartialRight(GA_DELEGATOR, TargetType.account, subTargetAcct, GranteeType.GT_USER, GA_DELEGATEE, Result.GOOD);
        doDelegatePartialRight(GA_DELEGATOR, TargetType.account, subTargetAcct, GranteeType.GT_GROUP, GG_DELEGATEE, Result.GOOD);
        
        // on unrelated target
        doDelegatePartialRight(GA_DELEGATOR, TargetType.domain, otherDomain, GranteeType.GT_USER, GA_DELEGATEE, Result.PERM_DENIED);
        doDelegatePartialRight(GA_DELEGATOR, TargetType.domain, otherDomain, GranteeType.GT_GROUP, GG_DELEGATEE, Result.PERM_DENIED);
        
        // on super target
        doDelegatePartialRight(GA_DELEGATOR, TargetType.global, otherDomain, GranteeType.GT_USER, GA_DELEGATEE, Result.PERM_DENIED);
        doDelegatePartialRight(GA_DELEGATOR, TargetType.global, otherDomain, GranteeType.GT_GROUP, GG_DELEGATEE, Result.PERM_DENIED);

    }
    
    public void testDelegateNonDelegableRight() throws Exception {
        String testName = getTestName();

        /*
         * sys admin
         */
        Account sysAdmin = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA_DELEGATOR = createAdminAccount(getEmailAddr(testName, "GA_DELEGATOR"));
        Account GA_DELEGATEE = createAdminAccount(getEmailAddr(testName, "GA_DELEGATEE"));
        
        /*
         * target
         */
        String domainName = getSubDomainName(testName).toLowerCase();
        Domain TD = mProv.createDomain(domainName, new HashMap<String, Object>());
        
        /*
         * right
         */
        Right right = getRight("test-combo-account-domain");
        Right subRight = getRight("test-preset-account");
        Right anotherRight = getRight("test-preset-distributionlist");
        
        // authed as sys admin
        // grant:
        //   - one delegable combo right
        //   - one non-delegable right that is a sub right of the combo right
        //   - one non-delegable right that is not a sub right of the cobo right
        grantDelegableRight(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATOR, right);
        grantRight(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATOR, subRight, ALLOW);
        grantRight(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATOR, anotherRight, ALLOW);
        
        // the combo right cannot be granted as a whole, because part if it is non-delegable
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, right, Result.PERM_DENIED);
        
        // the sub right is not delegable 
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, subRight, Result.PERM_DENIED);
        
        // another right is not delegable
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, anotherRight, Result.PERM_DENIED);
        
        // a subRight of the combo right that is still delegable
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, getRight("test-preset-domain"), Result.GOOD);
        doTestDelegate(GA_DELEGATOR, TargetType.domain, TD, GranteeType.GT_USER, GA_DELEGATEE, getRight(inlineRightGet(TargetType.account, "description")), Result.GOOD);
    }
    
    public void testDelegateToNonAdmin() throws Exception {
        String testName = getTestName();

        /*
         * sys admin
         */
        Account sysAdmin = getSystemAdminAccount(getEmailAddr(testName, "authed"));
        
        /*
         * grantees
         */
        Account GA = createAccount(getEmailAddr(testName, "GA"));
        DistributionList GG = createGroup(getEmailAddr(testName, "GG"));
        
        // add a member to the group
        Account member = createAccount(getEmailAddr(testName, "member"));
        mProv.addMembers(GG, new String[] {member.getName()});
        
        /*
         * target
         */
        String domainName = getSubDomainName(testName).toLowerCase();
        Domain TD = mProv.createDomain(domainName, new HashMap<String, Object>());
        Account TA = createAccount("acct@"+domainName); // a user in the domain
        
        /*
         * right
         */
        Right right = getRight("test-combo-account-domain");
        
        // authed as sys admin
        
        // cannot grant to a non-admin account/group
        doTestGrant(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA, right, DELEGABLE, Result.INVALID_REQUEST);
        doTestGrant(sysAdmin, TargetType.domain, TD, GranteeType.GT_GROUP, GG, right,DELEGABLE, Result.INVALID_REQUEST);
        
        // revoke should be OK though, the admin bit is not checked for revoking 
        doTestRevoke(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA, right, DELEGABLE, Result.GOOD);
        doTestRevoke(sysAdmin, TargetType.domain, TD, GranteeType.GT_GROUP, GG, right,DELEGABLE, Result.GOOD);
        
        // turn the account/group into admin
        makeAccountAdmin(GA);
        makeGroupAdmin(GG);
        
        // now can grant to the account/group
        grantDelegableRight(sysAdmin, TargetType.domain, TD, GranteeType.GT_USER, GA, right);
        grantDelegableRight(sysAdmin, TargetType.domain, TD, GranteeType.GT_GROUP, GG, right);

        // make sure the account do get the right, test it on an account in the target domain
        verify(GA, TA, getRight("test-preset-account"), null, ALLOW);
        
        // but the group member does not yet get the right, because it is not an admin account
        verify(member, TA, getRight("test-preset-account"), null, DENY);

        // make the member an admin account and then it should get the right
        makeAccountAdmin(member);
        verify(member, TA, getRight("test-preset-account"), null, ALLOW);
       
        // make the group grantee no longer an admin group, the member will automatically lose his right
        makeGroupNonAdmin(GG);
        // flush the cached account entry, because group info an account is a member of are cached on the account entr
        flushAccountCache(member);
        verify(member, TA, getRight("test-preset-account"), null, DENY);
       
        // make the group admin again, the right should come back
        makeGroupAdmin(GG);
        // flush the cached account entry, because group info an account is a member of are cached on the account entr
        flushAccountCache(member);
        verify(member, TA, getRight("test-preset-account"), null, ALLOW);
        
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");

        TestUtil.runTest(TestACLGrant.class);
        
        /*
        TestACLGrant test = new TestACLGrant();
        test.testCanGrant();
        */
    }
}
