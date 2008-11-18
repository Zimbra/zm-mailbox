package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AllowedAttrs;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.qa.unittest.TestACL.TestViaGrant;


public class TestACLComboRight extends TestACL {

    // attrs covered by the ATTR_RIGHT_SOME right
    static final Map<String, Object> ATTRS_SOME;
    static final AllowedAttrs EXPECTED_SOME_NO_LIMIT;
    static final AllowedAttrs EXPECTED_SOME_WITH_LIMIT;
    
    static {
        Set<String> EMPTY_SET = new HashSet<String>();
        
        Map<String, Object> ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT = new HashMap<String, Object>();
        ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT = new HashMap<String, Object>();
        ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT.put(Provisioning.A_zimbraMailQuota, "123");
        ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT.put(Provisioning.A_zimbraQuotaWarnPercent, "123");
        ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT.put(Provisioning.A_zimbraQuotaWarnInterval, "123");
        ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT.put(Provisioning.A_zimbraQuotaWarnMessage, "123");
        
        Map<String, Object> ATTRS_DOMAIN_ADMIN_MODIFIABLE = new HashMap<String, Object>();
        ATTRS_DOMAIN_ADMIN_MODIFIABLE.put(Provisioning.A_displayName, "123");
        ATTRS_DOMAIN_ADMIN_MODIFIABLE.put(Provisioning.A_description, "123");
        
        ATTRS_SOME = new HashMap<String, Object>();
        ATTRS_SOME.putAll(ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT);
        ATTRS_SOME.putAll(ATTRS_DOMAIN_ADMIN_MODIFIABLE);
        
        EXPECTED_SOME_NO_LIMIT = AccessManager.ALLOW_SOME_ATTRS(ATTRS_SOME.keySet(), EMPTY_SET);
        EXPECTED_SOME_WITH_LIMIT =  AccessManager.ALLOW_SOME_ATTRS(ATTRS_SOME.keySet(), ATTRS_CONFIGURE_QUOTA_WITHIN_LIMIT.keySet());
    }
    
    public void testComboRight() throws Exception {
        String testName = getTestName();
        
        System.out.println("Testing " + testName);
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = AdminRight.R_domainAdmin;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, TA, GranteeType.GT_USER, GA.getName(), AdminRight.R_domainAdmin, POSITIVE);
        
        // createAcount is not applicable on account, thus the grant will be ignored, which is 
        // equivalent to no grant for the createAccount right, therefore default should be honored.
        verifyDefault(GA, TA, AdminRight.R_createAccount);
        
        // renameAccount right is applicable on acount
        verify(GA, TA, AdminRight.R_renameAccount, ALLOW, via);
        
        verify(GA, TA, ATTRS_SOME, SET, EXPECTED_SOME_WITH_LIMIT);
    }

    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLComboRight.class);
    }
    
}
