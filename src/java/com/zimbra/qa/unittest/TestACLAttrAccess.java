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
import com.zimbra.cs.account.AccessManager.AllowedAttrs;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;

public class TestACLAttrAccess extends TestACL {
    
    
    static final Right ATTR_RIGHT_GET_ALL   = AdminRight.R_getAccount;
    static final Right ATTR_RIGHT_GET_SOME  = AdminRight.R_viewDummy;
    
    static final Right ATTR_RIGHT_SET_ALL  = AdminRight.R_modifyAccount;
    static final Right ATTR_RIGHT_SET_SOME = AdminRight.R_configureQuota;
    static final Right ATTR_RIGHT_SET_SOME_WITH_LIMIT = AdminRight.R_configureQuotaWithinLimit;
    
    // attrs covered by the ATTR_RIGHT_SOME right
    static final Map<String, Object> ATTRS_SOME;
    static final AllowedAttrs EXPECTED_SOME_NO_LIMIT;
    static final AllowedAttrs EXPECTED_SOME_WITH_LIMIT;
    static final AllowedAttrs EXPECTED_SOME_EMPTY;
    static final AllowedAttrs EXPECTED_ALL_MINUS_SOME_NO_LIMIT;
    static final AllowedAttrs EXPECTED_ALL_SOME_WITH_LIMIT;
    
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
            ALL_ACCOUNT_ATTRS = AttributeManager.getInstance().getAttrsInClass(AttributeClass.account);
        } catch (ServiceException e) {
            System.exit(1);
        }
        Set<String> ALL_ACCOUNT_ATTRS_MINUS_SOME = SetUtil.subtract(ALL_ACCOUNT_ATTRS, ATTRS_SOME.keySet());
        
        EXPECTED_SOME_NO_LIMIT = AccessManager.ALLOW_SOME_ATTRS(ATTRS_SOME.keySet(), EMPTY_SET);
        EXPECTED_SOME_WITH_LIMIT = AccessManager.ALLOW_SOME_ATTRS(ATTRS_SOME.keySet(), ATTRS_SOME.keySet());
        EXPECTED_SOME_EMPTY = AccessManager.ALLOW_SOME_ATTRS(EMPTY_SET, EMPTY_SET);
        EXPECTED_ALL_MINUS_SOME_NO_LIMIT = AccessManager.ALLOW_SOME_ATTRS(ALL_ACCOUNT_ATTRS_MINUS_SOME, EMPTY_SET);
        EXPECTED_ALL_SOME_WITH_LIMIT = AccessManager.ALLOW_SOME_ATTRS(ALL_ACCOUNT_ATTRS_MINUS_SOME, ATTRS_SOME.keySet());
    }
    
    
    public void oneGrantSome(AllowOrDeny grant, LimitOrNoLimit limit, GetOrSet getOrSet, AllowedAttrs expected) throws Exception {
        String testName = "oneGrantSome-" + grant.name() + "-" + limit.name() + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right someRight;
        if (getOrSet.isGet())
            someRight = ATTR_RIGHT_GET_SOME;
        else
            someRight = limit.limit()? ATTR_RIGHT_SET_SOME_WITH_LIMIT : ATTR_RIGHT_SET_SOME;
        Set<ZimbraACE> grants = makeUsrGrant(GA, someRight, grant);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        Map<String, Object> attrs = ATTRS_SOME;
        
        verify(GA, TA, attrs, getOrSet, expected);
    }

    
    public void oneGrantAll(AllowOrDeny grant, GetOrSet getOrSet, AllowedAttrs expected) throws Exception {
        String testName = "oneGrantAll-" + grant.name() + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right allRight;
        if (getOrSet.isGet())
            allRight = ATTR_RIGHT_GET_ALL;
        else
            allRight = ATTR_RIGHT_SET_ALL;
        Set<ZimbraACE> grants = makeUsrGrant(GA, allRight, grant);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        Map<String, Object> attrs = ATTRS_SOME;
        
        verify(GA, TA, attrs, getOrSet, expected);
    }

    
    private void someAllSameLevel(AllowOrDeny some, AllowOrDeny all, LimitOrNoLimit limit, GetOrSet getOrSet, AllowedAttrs expected) throws ServiceException {
        String testName = "someAllSameLevel-" + some.name() + "-some-" + all.name() + "-all-" + limit.name() + "-" + getOrSet.name();
       
        System.out.println("Testing " + testName);
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right someRight;
        Right allRight;
        if (getOrSet.isGet()) {
            someRight = ATTR_RIGHT_GET_SOME;
            allRight = ATTR_RIGHT_GET_ALL;
        } else {
            someRight = limit.limit()? ATTR_RIGHT_SET_SOME_WITH_LIMIT : ATTR_RIGHT_SET_SOME;
            allRight = ATTR_RIGHT_SET_ALL;
        }
        Set<ZimbraACE> grants = makeUsrGrant(GA, someRight, some);
        grants.add(newUsrACE(GA, allRight, all));
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        /*
         * attrs wanted
         */
        Map<String, Object> attrs = ATTRS_SOME;
        
        verify(GA, TA, attrs, getOrSet, expected);
    }
    

    
    /*
     * 2 grants
     * allow some at closer level, deny all at farther level
     * => should allow some
     */
    public void someAllDiffLevel(AllowOrDeny some, AllowOrDeny all, LimitOrNoLimit limit, 
                                 boolean someIsCloser, // whether some or all is the closer grant
                                 GetOrSet getOrSet,
                                 AllowedAttrs expected) throws Exception {
        
        String testName = "someAllDiffLevel-" + some.name() + "-some-" + all.name() + "-all-" + limit.name() + "-" + (someIsCloser?"someIsCloser":"allIsCloser") + "-" + getOrSet.name();
        
        System.out.println("Testing " + testName);
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        DistributionList GG = mProv.createDistributionList(getEmailAddr(testName, "GG"), new HashMap<String, Object>());
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
            someRight = limit.limit()? ATTR_RIGHT_SET_SOME_WITH_LIMIT : ATTR_RIGHT_SET_SOME;
            allRight = ATTR_RIGHT_SET_ALL;
        }
        Set<ZimbraACE> grants = makeUsrGrant(GA, someRight, some);
        grants.add(newUsrACE(GA, allRight, all));
        
        Set<ZimbraACE> closerGrant = someIsCloser? makeUsrGrant(GA, someRight, some) : makeUsrGrant(GA, allRight, all);
        Set<ZimbraACE> fartherGrant = someIsCloser? makeGrpGrant(GG, allRight, all) : makeGrpGrant(GG, someRight, some);
       
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, closerGrant);
        grantRight(TargetType.account, TA, fartherGrant);
        
        /*
         * attrs wanted
         */
        Map<String, Object> attrs = ATTRS_SOME;
        
        verify(GA, TA, attrs, getOrSet, expected);
    }

    
    public void testOneGrantSome() throws Exception {
        oneGrantSome(ALLOW, NOLIMIT, SET, EXPECTED_SOME_NO_LIMIT);
        oneGrantSome(DENY,  NOLIMIT, SET, EXPECTED_SOME_EMPTY);
        oneGrantSome(ALLOW, LIMIT,   SET, EXPECTED_SOME_WITH_LIMIT);
        oneGrantSome(DENY,  LIMIT,   SET, EXPECTED_SOME_EMPTY);
        
        // limit doesn't matter, result should the same as SET with no NOLIMIT
        oneGrantSome(ALLOW, NULLLIMIT, GET, EXPECTED_SOME_NO_LIMIT);
        oneGrantSome(DENY,  NULLLIMIT, GET, EXPECTED_SOME_EMPTY);
    }
    
    public void testOneGrantAll() throws Exception {
        oneGrantAll(ALLOW, SET, AccessManager.ALLOW_ALL_ATTRS);
        oneGrantAll(DENY,  SET, AccessManager.DENY_ALL_ATTRS);
        
        // result should the same as SET
        oneGrantAll(ALLOW, GET, AccessManager.ALLOW_ALL_ATTRS);
        oneGrantAll(DENY,  GET, AccessManager.DENY_ALL_ATTRS);
    }
    
    public void testTwoGrantsSameLevel() throws Exception {
        someAllSameLevel(ALLOW, ALLOW, NOLIMIT, SET, AccessManager.ALLOW_ALL_ATTRS);
        someAllSameLevel(DENY,  ALLOW, NOLIMIT, SET, EXPECTED_ALL_MINUS_SOME_NO_LIMIT);
        someAllSameLevel(ALLOW, DENY,  NOLIMIT, SET, AccessManager.DENY_ALL_ATTRS);
        someAllSameLevel(DENY,  DENY,  NOLIMIT, SET, AccessManager.DENY_ALL_ATTRS);
        someAllSameLevel(ALLOW, ALLOW, LIMIT,   SET, EXPECTED_ALL_SOME_WITH_LIMIT);
        someAllSameLevel(DENY,  ALLOW, LIMIT,   SET, EXPECTED_ALL_MINUS_SOME_NO_LIMIT);
        someAllSameLevel(ALLOW, DENY,  LIMIT,   SET, AccessManager.DENY_ALL_ATTRS);
        someAllSameLevel(DENY,  DENY,  LIMIT,   SET, AccessManager.DENY_ALL_ATTRS);
        
        // limit doesn't matter, result should the same as SET with no NOLIMIT
        someAllSameLevel(ALLOW, ALLOW, NULLLIMIT, GET, AccessManager.ALLOW_ALL_ATTRS);
        someAllSameLevel(DENY,  ALLOW, NULLLIMIT, GET, EXPECTED_ALL_MINUS_SOME_NO_LIMIT);
        someAllSameLevel(ALLOW, DENY,  NULLLIMIT, GET, AccessManager.DENY_ALL_ATTRS);
        someAllSameLevel(DENY,  DENY,  NULLLIMIT, GET, AccessManager.DENY_ALL_ATTRS);
        
    }

    public void testTwoGrantsDiffLevel() throws Exception {
        //               some   all    limit  some-is-closer
        someAllDiffLevel(ALLOW, ALLOW, NOLIMIT, true, SET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(DENY,  ALLOW, NOLIMIT, true, SET, EXPECTED_ALL_MINUS_SOME_NO_LIMIT);
        someAllDiffLevel(ALLOW, DENY,  NOLIMIT, true, SET, EXPECTED_SOME_NO_LIMIT);
        someAllDiffLevel(DENY,  DENY,  NOLIMIT, true, SET, AccessManager.DENY_ALL_ATTRS);
        someAllDiffLevel(ALLOW, ALLOW, LIMIT,   true, SET, EXPECTED_ALL_SOME_WITH_LIMIT);
        someAllDiffLevel(DENY,  ALLOW, LIMIT,   true, SET, EXPECTED_ALL_MINUS_SOME_NO_LIMIT);
        someAllDiffLevel(ALLOW, DENY,  LIMIT,   true, SET, EXPECTED_SOME_WITH_LIMIT);
        someAllDiffLevel(DENY,  DENY,  LIMIT,   true, SET, AccessManager.DENY_ALL_ATTRS);
        
        someAllDiffLevel(ALLOW, ALLOW, NOLIMIT, false, SET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(DENY,  ALLOW, NOLIMIT, false, SET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(ALLOW, DENY,  NOLIMIT, false, SET, AccessManager.DENY_ALL_ATTRS);
        someAllDiffLevel(DENY,  DENY,  NOLIMIT, false, SET, AccessManager.DENY_ALL_ATTRS);
        someAllDiffLevel(ALLOW, ALLOW, LIMIT,   false, SET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(DENY,  ALLOW, LIMIT,   false, SET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(ALLOW, DENY,  LIMIT,   false, SET, AccessManager.DENY_ALL_ATTRS);
        someAllDiffLevel(DENY,  DENY,  LIMIT,   false, SET, AccessManager.DENY_ALL_ATTRS);
        
        // limit doesn't matter, result should the same as SET with no NOLIMIT
        someAllDiffLevel(ALLOW, ALLOW, NULLLIMIT, true,  GET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(DENY,  ALLOW, NULLLIMIT, true,  GET, EXPECTED_ALL_MINUS_SOME_NO_LIMIT);
        someAllDiffLevel(ALLOW, DENY,  NULLLIMIT, true,  GET, EXPECTED_SOME_NO_LIMIT);
        someAllDiffLevel(DENY,  DENY,  NULLLIMIT, true,  GET, AccessManager.DENY_ALL_ATTRS);
        someAllDiffLevel(ALLOW, ALLOW, NULLLIMIT, false, GET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(DENY,  ALLOW, NULLLIMIT, false, GET, AccessManager.ALLOW_ALL_ATTRS);
        someAllDiffLevel(ALLOW, DENY,  NULLLIMIT, false, GET, AccessManager.DENY_ALL_ATTRS);
        someAllDiffLevel(DENY,  DENY,  NULLLIMIT, false, GET, AccessManager.DENY_ALL_ATTRS);
        
    }
    
    // TODO: add test for adding/substracting attrs between two allow/deny SOME rights 
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLAttrAccess.class);
    }
    
}
