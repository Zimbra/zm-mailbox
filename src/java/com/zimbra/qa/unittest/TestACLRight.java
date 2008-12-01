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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightChecker;
import com.zimbra.cs.account.accesscontrol.RightChecker.AllowedAttrs;
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
            GET_SOME_ATTRS_RIGHT = RightManager.getInstance().getRight("viewDummy");
            SET_SOME_ATTRS_RIGHT = RightManager.getInstance().getRight("configureQuota");
            GET_ALL_ATTRS_RIGHT = RightManager.getInstance().getRight("getAccount");
            SET_ALL_ATTRS_RIGHT = RightManager.getInstance().getRight("modifyAccount");
            
            CONFIGURE_CONSTRAINT_RIGHT = RightManager.getInstance().getRight("configureCosConstraint");
            
            PRESET_RIGHT = RightManager.getInstance().getRight("modifyAccount");
            COMBO_RIGHT = RightManager.getInstance().getRight("domainAdmin");
            
            
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
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = GET_SOME_ATTRS_RIGHT;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        verify(GA, TA, GET_SOME_ATTRS_RIGHT, null, ALLOW);
        verify(GA, TA, GET_ALL_ATTRS_RIGHT, null, DENY);
    }
    
    /*
     * grant get ALL attrs
     */
    public void testGetAllAttrs() throws Exception {
        String testName = getTestName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = SET_SOME_ATTRS_RIGHT;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        verify(GA, TA, GET_SOME_ATTRS_RIGHT, null, ALLOW);
        verify(GA, TA, GET_ALL_ATTRS_RIGHT, null, DENY);
    }
    
    /*
     * grant set SOME attrs
     */
    public void testSetSomeAttrs() throws Exception {
        String testName = getTestName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = SET_SOME_ATTRS_RIGHT;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
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
        grants = makeUsrGrant(GA, CONFIGURE_CONSTRAINT_RIGHT, ALLOW);
        grantRight(TargetType.cos, cos, grants);
        
        // now can set attrs beyond constraint
        verify(GA, TA, SET_SOME_ATTRS_RIGHT, ATTRS_IN_SET_SOME_ATTRS_RIGHT_VIOLATE_CONSTRAINT, ALLOW);
    }
    
    
    /*
     * grant set ALL attrs
     */
    public void testSetAllAttrs() throws Exception {
        String testName = getTestName();
        
        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = SET_ALL_ATTRS_RIGHT;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
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
    
    public void testComboRight() throws Exception {
        String testName = getTestName();

        /*
         * grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * grants
         */
        Right right = COMBO_RIGHT;
        Set<ZimbraACE> grants = makeUsrGrant(GA, right, ALLOW);
        
        /*
         * targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        grantRight(TargetType.account, TA, grants);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, TA, GranteeType.GT_USER, GA.getName(), COMBO_RIGHT, POSITIVE);
        
        // createAcount is not applicable on account, thus the grant will be ignored, which is 
        // equivalent to no grant for the createAccount right, therefore default should be honored.
        // note: default is not used for admin rights in the product, this is just to complete the test
        verifyDefault(GA, TA, AdminRight.R_createAccount);
        
        // renameAccount right is applicable on account
        verify(GA, TA, AdminRight.R_renameAccount, ALLOW, via);
        
        Set<String> expectedAttrs = new HashSet<String>();
        expectedAttrs.add(Provisioning.A_zimbraMailQuota);
        expectedAttrs.add(Provisioning.A_zimbraQuotaWarnPercent);
        expectedAttrs.add(Provisioning.A_zimbraQuotaWarnInterval);
        expectedAttrs.add(Provisioning.A_zimbraQuotaWarnMessage);
        expectedAttrs.add(Provisioning.A_displayName);
        expectedAttrs.add(Provisioning.A_description);
        AllowedAttrs expected = RightChecker.ALLOW_SOME_ATTRS(expectedAttrs);
        verify(GA, TA, SET, expected);

    }

    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLRight.class);
    }
    
}
