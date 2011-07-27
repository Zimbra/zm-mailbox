package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;

public class TestAC extends TestProv {
    
    protected static final AccessManager sAM = AccessManager.getInstance();
    
    protected static Right USER_RIGHT;
    protected static Right USER_RIGHT_DISTRIBUTION_LIST;
    protected static Right ADMIN_RIGHT_ACCOUNT;
    protected static Right ADMIN_RIGHT_CALENDAR_RESOURCE;
    protected static Right ADMIN_RIGHT_CONFIG;
    protected static Right ADMIN_RIGHT_COS;
    protected static Right ADMIN_RIGHT_DISTRIBUTION_LIST;
    protected static Right ADMIN_RIGHT_DOMAIN;
    protected static Right ADMIN_RIGHT_GLOBALGRANT;
    protected static Right ADMIN_RIGHT_SERVER;
    protected static Right ADMIN_RIGHT_XMPP_COMPONENT;
    protected static Right ADMIN_RIGHT_ZIMLET;
    protected static List<Right> sRights;
    
    private Account mGlobalAdminAcct;

    @BeforeClass
    public static void init() throws Exception {
        try {
            RightManager.getInstance(true);
            
            // setup rights
            USER_RIGHT                    = getRight("test-user");
            USER_RIGHT_DISTRIBUTION_LIST  = getRight("test-user-distributionlist");
            ADMIN_RIGHT_ACCOUNT           = getRight("test-preset-account");
            ADMIN_RIGHT_CALENDAR_RESOURCE = getRight("test-preset-calendarresource");
            ADMIN_RIGHT_CONFIG            = getRight("test-preset-globalconfig");
            ADMIN_RIGHT_COS               = getRight("test-preset-cos");
            ADMIN_RIGHT_DISTRIBUTION_LIST = getRight("test-preset-distributionlist");
            ADMIN_RIGHT_DOMAIN            = getRight("test-preset-domain");
            ADMIN_RIGHT_GLOBALGRANT       = getRight("test-preset-globalgrant");
            ADMIN_RIGHT_SERVER            = getRight("test-preset-server");
            ADMIN_RIGHT_XMPP_COMPONENT    = getRight("test-preset-xmppcomponent");
            ADMIN_RIGHT_ZIMLET            = getRight("test-preset-zimlet");
            
            sRights = new ArrayList<Right>();
            sRights.add(USER_RIGHT);
            sRights.add(USER_RIGHT_DISTRIBUTION_LIST);
            sRights.add(User.R_loginAs);
            sRights.add(ADMIN_RIGHT_ACCOUNT);
            sRights.add(Admin.R_adminLoginAs);
            sRights.add(ADMIN_RIGHT_CALENDAR_RESOURCE);
            sRights.add(ADMIN_RIGHT_CONFIG);
            sRights.add(ADMIN_RIGHT_COS);
            sRights.add(ADMIN_RIGHT_DISTRIBUTION_LIST);
            sRights.add(ADMIN_RIGHT_DOMAIN);
            sRights.add(ADMIN_RIGHT_GLOBALGRANT);
            sRights.add(ADMIN_RIGHT_SERVER);
            sRights.add(ADMIN_RIGHT_XMPP_COMPONENT);
            sRights.add(ADMIN_RIGHT_ZIMLET);
            
        
        } catch (ServiceException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }
    
    @Override
    protected Account getGlobalAdminAcct() throws ServiceException {
        if (mGlobalAdminAcct == null)
            mGlobalAdminAcct = super.getGlobalAdminAcct();
        return mGlobalAdminAcct;
    }
    

    /*
     *  Note: do *not* copy it to /Users/pshao/p4/main/ZimbraServer/conf
     *  that could accidently generate a RightDef.java with our test rights.
     *  
     *  cp -f /Users/pshao/p4/main/ZimbraServer/data/unittest/ldap/rights-unittest.xml /opt/zimbra/conf/rights
     */
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        TestUtil.runTest(TestAC.class);
        
        /*
        TestAC test = new TestAC();
        test.revokeAllGrantsOnGlobalGrant();
        */
    }

}
