package com.zimbra.qa.unittest;

import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.qa.unittest.TestACL.TestViaGrant;

public class TestACLRight extends TestACL {

    private static final Right GET_ATTRS_RIGHT = AdminRight.R_getAccount;
    private static final Right SET_ATTRS_RIGHT = AdminRight.R_modifyAccount;
    private static final Right PRESET_RIGHT = AdminRight.R_modifyAccount;
    private static final Right COMBO_RIGHT = AdminRight.R_domainAdmin;
    
    public void testCanDoWithAttrRight() throws Exception {
        String testName = getTestName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = GET_ATTRS_RIGHT;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        TestViaGrant via;
        
        // call canDo with a getAttrs right
        verify(GA, TA, right, DENY, null);

    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLRight.class);
    }
    
}
