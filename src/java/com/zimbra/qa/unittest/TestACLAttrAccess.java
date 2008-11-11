package com.zimbra.qa.unittest;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;

public class TestACLAttrAccess extends TestACL {
    
    public void testGranteeUser() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        Right right = AdminRight.R_configureQuota;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(GA, right, ALLOW));
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLAttrAccess.class);
    }
    
}
