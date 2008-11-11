package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;

public class TestACLAttrAccess extends TestACL {
    
    
    protected static final Right ATTR_RIGHT_ALL  = AdminRight.R_modifyAccount;
    protected static final Right ATTR_RIGHT_SOME = AdminRight.R_configureQuota;
    
    // attrs covered by the ATTR_RIGHT_SOME right
    private static final Map<String, Object> ATTRS_SOME = new HashMap<String, Object>();
    
    static {
        ATTRS_SOME.put(Provisioning.A_zimbraMailQuota, "123");
        ATTRS_SOME.put(Provisioning.A_zimbraQuotaWarnPercent, "123");
        ATTRS_SOME.put(Provisioning.A_zimbraQuotaWarnInterval, "123");
        ATTRS_SOME.put(Provisioning.A_zimbraQuotaWarnMessage, "123");
    }
    
    Set<ZimbraACE> makeUsrGrant(Account grantee, Right right, AllowOrDeny alloworDeny) throws ServiceException {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, alloworDeny));
        return aces;
    }
    
    Map<String, Object> wantMoreAttrs(Map<String, Object> addTo) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        attrs.put(Provisioning.A_zimbraFeatureMailEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraFeatureCalendarEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraPrefLocale, "en-us");
        
        return attrs;
    }
    
    /*
     * simple, allow some
     */
    public void test1() throws Exception {
        String testName = getName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Set<ZimbraACE> grants = makeUsrGrant(GA, ATTR_RIGHT_SOME, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        /*
         * attrs wanted
         */
        Map<String, Object> attrs = ATTRS_SOME;
        
        verify(GA, TA, attrs, ALLOW);
    }
    
    
    /*
     * allow some and allow all on same level
     */
    public void test2() throws Exception {
        String testName = getName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Set<ZimbraACE> grants = makeUsrGrant(GA, ATTR_RIGHT_SOME, ALLOW);
        grants.add(newUsrACE(GA, ATTR_RIGHT_ALL, ALLOW));
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        /*
         * attrs wanted
         */
        // want more covered by the right
        Map<String, Object> attrs = wantMoreAttrs(ATTRS_SOME);
        
        verify(GA, TA, attrs, ALLOW);
    }
    
    
    /*
     * allow some and deny all on same level
     */
    public void test3() throws Exception {
        String testName = getName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Set<ZimbraACE> grants = makeUsrGrant(GA, ATTR_RIGHT_SOME, ALLOW);
        grants.add(newUsrACE(GA, ATTR_RIGHT_ALL, DENY));
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        /*
         * attrs wanted
         */
        Map<String, Object> attrs = ATTRS_SOME;
        
        verify(GA, TA, attrs, DENY);
    }
    

    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLAttrAccess.class);
    }
    
}
