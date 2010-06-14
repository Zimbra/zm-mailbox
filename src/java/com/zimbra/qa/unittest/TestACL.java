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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightChecker;
import com.zimbra.cs.account.accesscontrol.AllowedAttrs;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.ACLAccessManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.service.AuthProvider;


public abstract class TestACL extends TestCase {
    
    protected static boolean CHECK_LIMIT = false;  // todo: remove this and all limit related code after all tests pass. 
    
    protected static final AccessManager mAM = AccessManager.getInstance();
    protected static final Provisioning mProv = Provisioning.getInstance();
    protected static final String TEST_ID = TestProvisioningUtil.genTestId();
    protected static final String DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-ACL", TEST_ID);
    protected static final String PASSWORD = "test123";
    
    // user right
    protected static final Right USER_RIGHT = User.R_viewFreeBusy;
    
    // account right
    protected static Right ADMIN_RIGHT_ACCOUNT;
    protected static Right ADMIN_RIGHT_CALENDAR_RESOURCE;
    protected static Right ADMIN_RIGHT_CONFIG;
    protected static Right ADMIN_RIGHT_COS;
    protected static Right ADMIN_RIGHT_DISTRIBUTION_LIST;
    protected static Right ADMIN_RIGHT_DOMAIN;
    protected static Right ADMIN_RIGHT_GLOBALGRANT;
    protected static Right ADMIN_RIGHT_SERVER;
    protected static Right ADMIN_RIGHT_ZIMLET;
    
    protected static Account mSysAdminAcct;
    
    static {
        
        System.out.println();
        System.out.println("AccessManager: " + mAM.getClass().getName());
        System.out.println();
        
        try {
            // create a domain
            Domain domain = mProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
            
            // create a system admin account
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraIsAdminAccount, Provisioning.TRUE);
            String sysAdminEmail = getEmailAddr("sysadmin");
            mSysAdminAcct = mProv.createAccount(sysAdminEmail, PASSWORD, attrs);
                
            // setup rights
            ADMIN_RIGHT_ACCOUNT           = getRight("test-preset-account");
            ADMIN_RIGHT_CALENDAR_RESOURCE = getRight("test-preset-calendarresource");
            ADMIN_RIGHT_CONFIG            = getRight("test-preset-globalconfig");
            ADMIN_RIGHT_COS               = getRight("test-preset-cos");
            ADMIN_RIGHT_DISTRIBUTION_LIST = getRight("test-preset-distributionlist");
            ADMIN_RIGHT_DOMAIN            = getRight("test-preset-domain");
            ADMIN_RIGHT_GLOBALGRANT       = getRight("test-preset-globalgrant");
            ADMIN_RIGHT_SERVER            = getRight("test-preset-server");
            ADMIN_RIGHT_ZIMLET            = getRight("test-preset-zimlet");
            
        } catch (ServiceException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    String getTestName() {
        // if not run in the test framework(when we selectively run a test by new a test and 
        // invoke a method directly), testName will be null, just use some name, we should not 
        // run into name clash because only that test is run
        String testName = getName();
        if (testName == null)
            return "unknownTest";
        else
            return getName().substring(4);
    }
    
    static void logToConsole(String level) {
        ZimbraLog.toolSetupLog4j(level, "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
    }
    
    
    /*
     * ======================
     * ======================
     *     Util Methods
     * ======================
     * ======================
     */
    
    protected static String getEmailAddr(String localPart) {
        return localPart + "@" + DOMAIN_NAME;
    }
    
    protected static String getEmailAddr(String testCaseName, String localPartPostfix) {
        if (testCaseName == null)
            return localPartPostfix + "@" + DOMAIN_NAME;
        else
            return testCaseName + "-" + localPartPostfix + "@" + DOMAIN_NAME;
    }
    
    protected static String getSubDomainName(String testCaseName) {
        return testCaseName + "." + DOMAIN_NAME;
    }
    
    protected Account guestAccount(String email, String password) {
        return new GuestAccount(email, password);
    }
    
    protected Account keyAccount(String name, String accesKey) {
        AuthToken authToken = new TestACAccessKey.KeyAuthToken(name, accesKey);
        return new GuestAccount(authToken);
    }
    
    protected Account anonAccount() {
        return GuestAccount.ANONYMOUS_ACCT;
    }
    
    protected Account createAccount(String email) throws ServiceException {
        return mProv.createAccount(email, PASSWORD, null);
    }
    
    protected DistributionList createGroup(String email) throws ServiceException {
        return mProv.createDistributionList(email, new HashMap<String, Object>());
    }
    
    protected Account createAdminAccount(String email) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, Provisioning.TRUE);
        return mProv.createAccount(email, PASSWORD, attrs);
    }
    
    protected DistributionList createAdminGroup(String email) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
        return mProv.createDistributionList(email, attrs);
    }
    
    protected void flushAccountCache(Account acct) throws ServiceException {
        mProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(CacheEntryBy.id, acct.getId())});
    }
    
    protected void makeAccountAdmin(Account acct) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, Provisioning.TRUE);
        mProv.modifyAttrs(acct, attrs);
        flushAccountCache(acct);
    }
    
    protected void makeGroupAdmin(DistributionList group) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
        mProv.modifyAttrs(group, attrs);
        mProv.flushCache(CacheEntryType.group, null);
    }
    
    protected void makeGroupNonAdmin(DistributionList group) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.FALSE);
        mProv.modifyAttrs(group, attrs);
        mProv.flushCache(CacheEntryType.group, null);
    }
    
    /*
     * for now, just return the singleton mSysAdminAcct
     * if it becomes necessary then create a new one with email name for each callsite
     */
    protected Account getSystemAdminAccount(String email) throws ServiceException {
        return getSystemAdminAccount();
    }

    protected Account getSystemAdminAccount() throws ServiceException {
        return mSysAdminAcct;
    }

    
    /*
     * convenient notions so callsites don't have to deal with many various boolean values 
     * when passing args to the utility methods, also callsites code are more readable.
     */
    protected static enum AllowOrDeny {
        ALLOW(true, false),
        DELEGABLE(true, true),
        DENY(false, false);
        
        boolean mAllow;
        boolean mDelegable;
        
        AllowOrDeny(boolean allow, boolean delegable) {
            mAllow = allow;
            mDelegable = delegable;
        }
        
        boolean deny() {
            return !mAllow;
        }
        
        boolean allow() {
            return mAllow;
        }
        
        boolean delegable() {
            return mDelegable;
        }
        
        RightModifier toRightModifier() {
            if (deny())
                return RightModifier.RM_DENY;
            else if (delegable())
                return RightModifier.RM_CAN_DELEGATE;
            else
                return null;
        }
    }
    
    // shorthand notion so we don't have to refer to AllowOrDeny from callsites
    protected static final AllowOrDeny ALLOW = AllowOrDeny.ALLOW;
    protected static final AllowOrDeny DELEGABLE = AllowOrDeny.DELEGABLE;
    protected static final AllowOrDeny DENY = AllowOrDeny.DENY;
    
    protected static enum AsAdmin {
        AS_ADMIN(true),
        AS_USER(false);
        
        boolean mAsAdmin;
        
        AsAdmin(boolean asAdmin) {
            mAsAdmin = asAdmin;
        }
        
        boolean yes()  {
            return mAsAdmin;
        }
    }
    // shorthand notion so we don't have to refer to AllowOrDeny from callsites
    protected static final AsAdmin AS_ADMIN = AsAdmin.AS_ADMIN;
    protected static final AsAdmin AS_USER = AsAdmin.AS_USER;
    
    
    protected static enum LimitOrNoLimit {
        LIMIT(true),
        NOLIMIT(false),
        NULLLIMIT(false); // for tests in that limit doesn't matter
        
        boolean mLimit;
        
        LimitOrNoLimit(boolean limit) {
            mLimit = limit;
        }
        
        boolean limit() {
            // master key to turn of limit checking
            if (!CHECK_LIMIT)
                return false;
            
            // should never be called for NULLLIMIT
            if (this == NULLLIMIT)
                fail();
            
            return mLimit;
        }
    }
    protected static final LimitOrNoLimit LIMIT = LimitOrNoLimit.LIMIT;
    protected static final LimitOrNoLimit NOLIMIT = LimitOrNoLimit.NOLIMIT;
    protected static final LimitOrNoLimit NULLLIMIT = LimitOrNoLimit.NULLLIMIT;
    
    protected static enum GetOrSet {
        GET(true),
        SET(false);
        
        boolean mGet;
        
        GetOrSet(boolean get) {
            mGet = get;
        }
        
        boolean isGet() {
            return mGet;
        }
    }
    protected static final GetOrSet GET = GetOrSet.GET;
    protected static final GetOrSet SET = GetOrSet.SET;
    
    // construct a ACE with "pub" grantee type
    protected ZimbraACE newPubACE(Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(GuestAccount.GUID_PUBLIC, GranteeType.GT_PUBLIC, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "all" authuser grantee type
    protected ZimbraACE newAllACE(Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(GuestAccount.GUID_AUTHUSER, GranteeType.GT_AUTHUSER, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "usr" grantee type
    protected ZimbraACE newUsrACE(Account acct, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(acct.getId(), GranteeType.GT_USER, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "grp" grantee type
    protected ZimbraACE newGrpACE(DistributionList dl, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(dl.getId(), GranteeType.GT_GROUP, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "key" grantee type
    protected ZimbraACE newKeyACE(String nameOrEmail, String accessKey, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(nameOrEmail, GranteeType.GT_KEY, right, allowDeny.toRightModifier(), accessKey);
    }
    
    Set<ZimbraACE> makeUsrGrant(Account grantee, Right right, AllowOrDeny alloworDeny) throws ServiceException {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, alloworDeny));
        return aces;
    }
    
    Set<ZimbraACE> makeGrpGrant(DistributionList grantee, Right right, AllowOrDeny alloworDeny) throws ServiceException {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(grantee, right, alloworDeny));
        return aces;
    }
    
    
    // another shorthand so we don't have to remember true/false.
    static final boolean POSITIVE = false;
    static final boolean NEGATIVE = true;
    
    static class TestViaGrant extends ViaGrant {
        String mTargetType;
        String mTargetName;
        String mGranteeType;
        String mGranteeName;
        String mRight;
        boolean mIsNegativeGrant;
        
        Set<TestViaGrant> mCanAlsoVia;
        
        TestViaGrant(TargetType targetType,
                     Entry target,
                     GranteeType granteeType,
                     String granteeName,
                     Right right,
                     boolean isNegativeGrant) {
            mTargetType = targetType.getCode();
            mTargetName = target.getLabel();
            mGranteeType = granteeType.getCode();
            mGranteeName = granteeName;
            mRight = right.getName();
            mIsNegativeGrant = isNegativeGrant;
        }
        
        public String getTargetType() { 
            return mTargetType;
        } 
        
        public String getTargetName() {
            return mTargetName;
        }
        
        public String getGranteeType() {
            return mGranteeType;
        }
        
        public String getGranteeName() {
            return mGranteeName;
        }
        
        public String getRight() {
            return mRight;
        }
        
        public boolean isNegativeGrant() {
            return mIsNegativeGrant;
        }
        
        public void addCanAlsoVia(TestViaGrant canAlsoVia) {
            if (mCanAlsoVia == null)
                mCanAlsoVia = new HashSet<TestViaGrant>();
            mCanAlsoVia.add(canAlsoVia);
        }
        
        public void verify(ViaGrant actual) {
            try {
                assertEquals(getTargetType(),   actual.getTargetType());
                assertEquals(getTargetName(),   actual.getTargetName());
                assertEquals(getGranteeType(),  actual.getGranteeType());
                assertEquals(getGranteeName(),  actual.getGranteeName());
                assertEquals(getRight(),        actual.getRight());
                assertEquals(isNegativeGrant(), actual.isNegativeGrant());
            } catch (AssertionFailedError e) {
                if (mCanAlsoVia == null)
                    throw e;
                
                // see if any canAlsoVia matches
                for (TestViaGrant canAlsoVia : mCanAlsoVia) {
                    try {
                        canAlsoVia.verify(actual);
                        // good, at least one of the canAlsoVia matches
                        return;
                    } catch (AssertionFailedError     eAlso) {
                        // ignore, see if next one matches
                    }
                }
                // if we get here, none of the canAlsoVia matches
                // throw the assertion exception on the main via
                throw e;
            }
        }
    }
    
    static class AuthUserViaGrant extends TestViaGrant {
        AuthUserViaGrant(TargetType targetType,
                         Entry target,
                         Right right,
                         boolean isNegativeGrant) {
            super(targetType, target, GranteeType.GT_AUTHUSER, null, right, isNegativeGrant);
        }
    }
    
    static class PubViaGrant extends TestViaGrant {
        PubViaGrant(TargetType targetType,
                    Entry target,
                    Right right,
                    boolean isNegativeGrant) {
            super(targetType, target, GranteeType.GT_PUBLIC, null, right, isNegativeGrant);
        }
    }

    static class TodoViaGrant extends ViaGrant {

    }

    /*
     * verify we always get the expected result
     * This test does NOT use the admin privileges 
     * 
     * This is for testing target entry with some ACL.
     */
    protected void verify(Account grantee, Entry target, Right right, AllowOrDeny expected, ViaGrant expectedVia) throws Exception {
        verify(grantee, target, right, AS_USER, expected, expectedVia);
    }
    
    protected void verifyDefinedDefault(Account grantee, Entry target, Right right) throws Exception {
        if (right == User.R_invite || right == User.R_viewFreeBusy)
            verify(grantee, target, right, AS_USER, ALLOW, null);
        else
            verify(grantee, target, right, AS_USER, DENY, null);

    }
    
    /*
     * TODO: deprecate this thing.
     * 
     * verify that the result IS the default value
     * 
     * This is for testing target entry without any ACL.
     */
    protected void verifyDefault(Account grantee, Entry target, Right right) throws Exception {
        // TODO: enable the test again after default is fixed: move from rights.xml to global config
        return;
        
        
        /*
        
        AsAdmin asAdmin = AS_USER; // TODO: test admin case
        
        // 1. pass true as the default value, result should be true
        verify(grantee, target, right, asAdmin, ALLOW, ALLOW, null);
            
        // 2. pass false as the default value, result should be false
        verify(grantee, target, right, asAdmin, DENY, DENY, null);
        
        */
    }
    
    void assertEquals(ViaGrant expected, ViaGrant actual) {
        
        if (expected == null && actual == null)
            return;
        
        if (!(AccessManager.getInstance() instanceof ACLAccessManager))
            return;
        
        if (expected instanceof TodoViaGrant)
            return; // TODO
        
        ((TestViaGrant)expected).verify(actual);
    }
    
    /*
     * verify expected result
     */
    protected void verify(Account grantee, Entry target, Right right, AsAdmin asAdmin, AllowOrDeny expected, ViaGrant expectedVia) throws Exception {
        boolean result;
        
        // Account interface
        ViaGrant via = (expectedVia==null)?null:new ViaGrant();
        result = mAM.canDo(grantee==null?null:grantee, target, right, asAdmin.yes(), via);
        assertEquals(expected.allow(), result);
        assertEquals(expectedVia, via);
        
        // AuthToken interface
        via = (expectedVia==null)?null:new ViaGrant();
        result = mAM.canDo(grantee==null?null:AuthProvider.getAuthToken(grantee), target, right, asAdmin.yes(), via);
        assertEquals(expected.allow(), result);
        assertEquals(expectedVia, via);
        
        // String interface
        via = (expectedVia==null)?null:new ViaGrant();
        result = mAM.canDo(grantee==null?null:grantee.getName(), target, right, asAdmin.yes(), via);
        if (grantee instanceof GuestAccount && ((GuestAccount)grantee).getAccessKey() != null) {
            // string interface always return denied for key grantee unless there is a pub grant
            // skip the test for now, unless we want to pass yet another parameter to this method
            // i.e. - if no pub grant: should always expect false
            //      - if there is a pub grant: should expect the expected
            return;
        }
        assertEquals(expected.allow(), result);
        assertEquals(expectedVia, via);
    }
    
    void assertEquals(Set<String> expected, Set<String> actual) {
        assertEquals(expected.size(), actual.size());
        for (String s: expected)
            assertTrue(actual.contains(s));
    }
    
    void assertEquals(AllowedAttrs expected, AllowedAttrs actual) {
        assertEquals(expected.getResult(), actual.getResult());
        if (actual.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            assertEquals(expected.getAllowed(), actual.getAllowed());
        }
    }
    
    protected void verify(Account grantee, Entry target, GetOrSet getOrSet, AllowedAttrs expected) {
        try {
            // call RightChecker directly instead of mAM, we want to verify the interim result.
            AllowedAttrs allowedAttrs = getOrSet.isGet() ? 
                    RightChecker.accessibleAttrs(new Grantee(grantee), target, AdminRight.PR_GET_ATTRS, false):
                    RightChecker.accessibleAttrs(new Grantee(grantee), target, AdminRight.PR_SET_ATTRS, false);
            // System.out.println("========== Test result ==========\n" + allowedAttrs.dump());
            assertEquals(expected, allowedAttrs);
        } catch (ServiceException e) {
            fail();
        }
    }
        
    protected void verify(Account grantee, Entry target, Right right, Map<String, Object> attrs, AllowOrDeny expected) throws ServiceException {
        boolean actual = mAM.canPerform(grantee, target, right, false, attrs, true, null);
        assertEquals(expected.allow(), actual);
    }
    
       
    /*
     * utility methods to grant/revoke right
     * 
     * To simulate how grants are done in the real server/zmprov, we first call TargetType.lookupTarget to 
     * "look for" the taret, then use the returned entry instead of giving the target entry passed in 
     * directly to RightUtil.
     * 
     * This is for testing user rights, which goes to RightUtil directly (i.e. not through RightCommand)
     * 
     */
    protected List<ZimbraACE> grantRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
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
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.name, targetName);
        } else {
            String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.id, targetId);
        }
        return ACLUtil.grantRight(mProv, targetEntry, aces);
    }
    
    /*
     * for testing admin rights
     */
    protected void grantRight(Account authedAcct,
                              TargetType targetType, NamedEntry target,
                              GranteeType granteeType, NamedEntry grantee,
                              Right right, AllowOrDeny grant) throws ServiceException {
        
        RightCommand.grantRight(mProv, authedAcct,
                                targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                                granteeType.getCode(), GranteeBy.name, grantee.getName(), null,
                                right.getName(), grant.toRightModifier());
    }
    
    protected void grantDelegableRight(Account authedAcct,
                                       TargetType targetType, NamedEntry target,
                                       GranteeType granteeType, NamedEntry grantee,
                                       Right right) throws ServiceException {

        RightCommand.grantRight(mProv, authedAcct,
                      targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                      granteeType.getCode(), GranteeBy.name, grantee.getName(), null,
                      right.getName(), RightModifier.RM_CAN_DELEGATE);
    }
        
    protected List<ZimbraACE> revokeRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        // call TargetType.lookupTarget instead of passing the target entry directly for two reasons:
        // 1. to simulate how grants are done in the real server/zmprov
        // 2. convert DistributionList to AclGroup
        Entry targetEntry;
        if (target instanceof Zimlet) {
            // must be by name
            String targetName = ((Zimlet)target).getName();
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.name, targetName);
        } else {
        String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.id, targetId);
        }
        return ACLUtil.revokeRight(mProv, targetEntry, aces);
    }
    
    protected void revokeRight(Account authedAcct,
                               TargetType targetType, NamedEntry target,
                               GranteeType granteeType, NamedEntry grantee,
                               Right right, AllowOrDeny grant) throws ServiceException {
        
        RightCommand.revokeRight(mProv, authedAcct,
                                 targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                                 granteeType.getCode(), GranteeBy.name, grantee.getName(),
                                 right.getName(), grant.toRightModifier());
    }
    
    protected void revokeDelegableRight(Account authedAcct,
            TargetType targetType, NamedEntry target,
            GranteeType granteeType, NamedEntry grantee,
            Right right) throws ServiceException {

        RightCommand.revokeRight(mProv, authedAcct,
                      targetType.getCode(), TargetBy.name, target==null?null:target.getName(),
                      granteeType.getCode(), GranteeBy.name, grantee.getName(),
                      right.getName(), RightModifier.RM_CAN_DELEGATE);
    }
    
    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }
    
    static String inlineRightGet(TargetType targetType, String attrName) {
        return "get." + targetType.getCode() + "." + attrName;
    }
    
    static String inlineRightSet(TargetType targetType, String attrName) {
        return "set." + targetType.getCode() + "." + attrName;
    }
    
    /*
     * cleanup all "test-..." grants on global config and globalgrant entries
     */
    protected void cleanupGrants() throws ServiceException {
        Account sysAdmin = getSystemAdminAccount();
    }
    
/*
  Note: do *not* copy it to /Users/pshao/p4/main/ZimbraServer/conf
        that could accidently generate a RightDef.java with our test rights.
        
  cp /Users/pshao/p4/main/ZimbraServer/data/unittest/*.xml /opt/zimbra/conf/rights
  and
  uncomment sCoreRightDefFiles.add("rights-unittest.xml"); in RightManager
  
  zmlocalconfig -e zimbra_class_accessmanager=com.zimbra.cs.account.accesscontrol.ACLAccessManager
  then restart server
  
  or:
  <key name="zimbra_class_accessmanager">
    <value>com.zimbra.cs.account.accesscontrol.ACLAccessManager</value>
  </key>
*/
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        // run all ACL tests
        TestUtil.runTest(TestACLGrantee.class);    // all user rights for now
        TestUtil.runTest(TestACLPrecedence.class); // all user rights for now
        
        if (mAM instanceof ACLAccessManager) {
            TestUtil.runTest(TestACLTarget.class);
            TestUtil.runTest(TestACLAttrRight.class);
            TestUtil.runTest(TestACLRight.class);
            TestUtil.runTest(TestACLGrant.class);
        }
    }

}
