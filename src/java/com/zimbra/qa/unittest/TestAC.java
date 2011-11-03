/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.BeforeClass;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;

public class TestAC extends TestProv {
    
    static final AccessManager sAM = AccessManager.getInstance();
    
    // user rights
    protected static Right USER_LOGIN_AS;
    protected static Right USER_RIGHT;
    protected static Right USER_RIGHT_DISTRIBUTION_LIST;
    protected static Right USER_RIGHT_DOMAIN;
    protected static Right USER_RIGHT_RESTRICTED_GRANT_TARGET_TYPE;
    
    // admin preset rights
    protected static Right ADMIN_PRESET_LOGIN_AS;
    protected static Right ADMIN_PRESET_ACCOUNT;
    protected static Right ADMIN_PRESET_CALENDAR_RESOURCE;
    protected static Right ADMIN_PRESET_CONFIG;
    protected static Right ADMIN_PRESET_COS;
    protected static Right ADMIN_PRESET_DISTRIBUTION_LIST;
    protected static Right ADMIN_PRESET_DYNAMIC_GROUP;
    protected static Right ADMIN_PRESET_DOMAIN;
    protected static Right ADMIN_PRESET_GLOBALGRANT;
    protected static Right ADMIN_PRESET_SERVER;
    protected static Right ADMIN_PRESET_XMPP_COMPONENT;
    protected static Right ADMIN_PRESET_ZIMLET;
    
    // admin attrs rights
    protected static Right ADMIN_ATTR_GETALL_ACCOUNT;
    protected static Right ADMIN_ATTR_SETALL_ACCOUNT;
    protected static Right ADMIN_ATTR_GETSOME_ACCOUNT;
    protected static Right ADMIN_ATTR_SETSOME_ACCOUNT;
    protected static Right ADMIN_ATTR_GETALL_CALENDAR_RESOURCE;
    protected static Right ADMIN_ATTR_SETALL_CALENDAR_RESOURCE;
    protected static Right ADMIN_ATTR_GETSOME_CALENDAR_RESOURCE;
    protected static Right ADMIN_ATTR_SETSOME_CALENDAR_RESOURCE;
    protected static Right ADMIN_ATTR_GETALL_CONFIG;
    protected static Right ADMIN_ATTR_SETALL_CONFIG;
    protected static Right ADMIN_ATTR_GETSOME_CONFIG;
    protected static Right ADMIN_ATTR_SETSOME_CONFIG;
    protected static Right ADMIN_ATTR_GETALL_COS;
    protected static Right ADMIN_ATTR_SETALL_COS;
    protected static Right ADMIN_ATTR_GETSOME_COS;
    protected static Right ADMIN_ATTR_SETSOME_COS;
    protected static Right ADMIN_ATTR_GETALL_DISTRIBUTION_LIST;
    protected static Right ADMIN_ATTR_SETALL_DISTRIBUTION_LIST;
    protected static Right ADMIN_ATTR_GETSOME_DISTRIBUTION_LIST;
    protected static Right ADMIN_ATTR_SETSOME_DISTRIBUTION_LIST;
    protected static Right ADMIN_ATTR_GETALL_DYNAMIC_GROUP;
    protected static Right ADMIN_ATTR_SETALL_DYNAMIC_GROUP;
    protected static Right ADMIN_ATTR_GETSOME_DYNAMIC_GROUP;
    protected static Right ADMIN_ATTR_SETSOME_DYNAMIC_GROUP;
    protected static Right ADMIN_ATTR_GETALL_DOMAIN;
    protected static Right ADMIN_ATTR_SETALL_DOMAIN;
    protected static Right ADMIN_ATTR_GETSOME_DOMAIN;
    protected static Right ADMIN_ATTR_SETSOME_DOMAIN;
    protected static Right ADMIN_ATTR_GETALL_SERVER;
    protected static Right ADMIN_ATTR_SETALL_SERVER;
    protected static Right ADMIN_ATTR_GETSOME_SERVER;
    protected static Right ADMIN_ATTR_SETSOME_SERVER;
    protected static Right ADMIN_ATTR_GETALL_ZIMLET;
    protected static Right ADMIN_ATTR_SETALL_ZIMLET;
    protected static Right ADMIN_ATTR_GETSOME_ZIMLET;
    protected static Right ADMIN_ATTR_SETSOME_ZIMLET; 
    
    // admin combo rights
    protected static Right ADMIN_COMBO_ACCOUNT;
    protected static Right ADMIN_COMBO_CALENDAR_RESOURCE;
    protected static Right ADMIN_COMBO_CONFIG;
    protected static Right ADMIN_COMBO_COS;
    protected static Right ADMIN_COMBO_DISTRIBUTION_LIST;
    protected static Right ADMIN_COMBO_DYNAMIC_GROUP;
    protected static Right ADMIN_COMBO_DOMAIN;
    protected static Right ADMIN_COMBO_GLOBALGRANT;
    protected static Right ADMIN_COMBO_SERVER;
    protected static Right ADMIN_COMBO_XMPP_COMPONENT;
    protected static Right ADMIN_COMBO_ZIMLET;
    // protected static Right ADMIN_COMBO_ALL;
    
    protected static final String ATTR_ALLOWED_IN_THE_RIGHT = Provisioning.A_description;
    protected static final String ATTR_NOTALLOWED_IN_THE_RIGHT = Provisioning.A_objectClass;
    
    protected static List<Right> sRights = Lists.newArrayList();
    
    private Account mGlobalAdminAcct;

    @BeforeClass
    public static void init() throws Exception {
        try {
            RightManager.getInstance(true);
            
            // setup rights
            USER_LOGIN_AS                 = User.R_loginAs;
            USER_RIGHT                    = getRight("test-user");
            USER_RIGHT_DISTRIBUTION_LIST  = getRight("test-user-distributionlist");
            USER_RIGHT_DOMAIN             = User.R_createDistList;
            USER_RIGHT_RESTRICTED_GRANT_TARGET_TYPE = User.R_sendAs;
            
            ADMIN_PRESET_LOGIN_AS          = Admin.R_adminLoginAs;
            ADMIN_PRESET_ACCOUNT           = getRight("test-preset-account");
            ADMIN_PRESET_CALENDAR_RESOURCE = getRight("test-preset-calendarresource");
            ADMIN_PRESET_CONFIG            = getRight("test-preset-globalconfig");
            ADMIN_PRESET_COS               = getRight("test-preset-cos");
            ADMIN_PRESET_DISTRIBUTION_LIST = getRight("test-preset-distributionlist");
            ADMIN_PRESET_DYNAMIC_GROUP     = getRight("test-preset-dynamicgroup");
            ADMIN_PRESET_DOMAIN            = getRight("test-preset-domain");
            ADMIN_PRESET_GLOBALGRANT       = getRight("test-preset-globalgrant");
            ADMIN_PRESET_SERVER            = getRight("test-preset-server");
            ADMIN_PRESET_XMPP_COMPONENT    = getRight("test-preset-xmppcomponent");
            ADMIN_PRESET_ZIMLET            = getRight("test-preset-zimlet");
            
            ADMIN_ATTR_GETALL_ACCOUNT               = Admin.R_getAccount;
            ADMIN_ATTR_SETALL_ACCOUNT               = Admin.R_modifyAccount;
            ADMIN_ATTR_GETSOME_ACCOUNT              = getRight("test-getAttrs-account");
            ADMIN_ATTR_SETSOME_ACCOUNT              = getRight("test-setAttrs-account");
            ADMIN_ATTR_GETALL_CALENDAR_RESOURCE     = Admin.R_getCalendarResource;
            ADMIN_ATTR_SETALL_CALENDAR_RESOURCE     = Admin.R_modifyCalendarResource;
            ADMIN_ATTR_GETSOME_CALENDAR_RESOURCE    = getRight("test-getAttrs-calendarresource");
            ADMIN_ATTR_SETSOME_CALENDAR_RESOURCE    = getRight("test-setAttrs-calendarresource");
            ADMIN_ATTR_GETALL_CONFIG                = Admin.R_getGlobalConfig;
            ADMIN_ATTR_SETALL_CONFIG                = Admin.R_modifyGlobalConfig;
            ADMIN_ATTR_GETSOME_CONFIG               = getRight("test-getAttrs-globalconfig");
            ADMIN_ATTR_SETSOME_CONFIG               = getRight("test-setAttrs-globalconfig");
            ADMIN_ATTR_GETALL_COS                   = Admin.R_getCos;
            ADMIN_ATTR_SETALL_COS                   = Admin.R_modifyCos;
            ADMIN_ATTR_GETSOME_COS                  = getRight("test-getAttrs-cos");
            ADMIN_ATTR_SETSOME_COS                  = getRight("test-setAttrs-cos");
            ADMIN_ATTR_GETALL_DISTRIBUTION_LIST     = Admin.R_getDistributionList;
            ADMIN_ATTR_SETALL_DISTRIBUTION_LIST     = Admin.R_modifyDistributionList;
            ADMIN_ATTR_GETSOME_DISTRIBUTION_LIST    = getRight("test-getAttrs-distributionlist");
            ADMIN_ATTR_SETSOME_DISTRIBUTION_LIST    = getRight("test-setAttrs-distributionlist");
            ADMIN_ATTR_GETALL_DYNAMIC_GROUP         = Admin.R_getGroup;
            ADMIN_ATTR_SETALL_DYNAMIC_GROUP         = Admin.R_modifyGroup;
            ADMIN_ATTR_GETSOME_DYNAMIC_GROUP        = getRight("test-getAttrs-dynamicgroup");
            ADMIN_ATTR_SETSOME_DYNAMIC_GROUP        = getRight("test-setAttrs-dynamicgroup");
            ADMIN_ATTR_GETALL_DOMAIN                = Admin.R_getDomain;
            ADMIN_ATTR_SETALL_DOMAIN                = Admin.R_modifyDomain;
            ADMIN_ATTR_GETSOME_DOMAIN               = getRight("test-getAttrs-domain");
            ADMIN_ATTR_SETSOME_DOMAIN               = getRight("test-setAttrs-domain");
            ADMIN_ATTR_GETALL_SERVER                = Admin.R_getServer;
            ADMIN_ATTR_SETALL_SERVER                = Admin.R_modifyServer;
            ADMIN_ATTR_GETSOME_SERVER               = getRight("test-getAttrs-server");
            ADMIN_ATTR_SETSOME_SERVER               = getRight("test-setAttrs-server");
            ADMIN_ATTR_GETALL_ZIMLET                = Admin.R_getZimlet;
            ADMIN_ATTR_SETALL_ZIMLET                = Admin.R_modifyZimlet;
            ADMIN_ATTR_GETSOME_ZIMLET               = getRight("test-getAttrs-zimlet");
            ADMIN_ATTR_SETSOME_ZIMLET               = getRight("test-setAttrs-zimlet");           
            
            ADMIN_COMBO_ACCOUNT           = getRight("test-combo-account");
            ADMIN_COMBO_CALENDAR_RESOURCE = getRight("test-combo-calendarresource");
            ADMIN_COMBO_CONFIG            = getRight("test-combo-globalconfig");
            ADMIN_COMBO_COS               = getRight("test-combo-cos");
            ADMIN_COMBO_DISTRIBUTION_LIST = getRight("test-combo-distributionlist");
            ADMIN_COMBO_DYNAMIC_GROUP     = getRight("test-combo-dynamicgroup");
            ADMIN_COMBO_DOMAIN            = getRight("test-combo-domain");
            ADMIN_COMBO_GLOBALGRANT       = getRight("test-combo-globalgrant");
            ADMIN_COMBO_SERVER            = getRight("test-combo-server");
            ADMIN_COMBO_XMPP_COMPONENT    = getRight("test-combo-xmppcomponent");
            ADMIN_COMBO_ZIMLET            = getRight("test-combo-zimlet");
            // ADMIN_COMBO_ALL               = getRight("test-combo-all");
            
            sRights.add(USER_LOGIN_AS);              
            sRights.add(USER_RIGHT);                    
            sRights.add(USER_RIGHT_DISTRIBUTION_LIST);   
            sRights.add(USER_RIGHT_DOMAIN);
            sRights.add(USER_RIGHT_RESTRICTED_GRANT_TARGET_TYPE);
            
            sRights.add(ADMIN_PRESET_LOGIN_AS);          
            sRights.add(ADMIN_PRESET_ACCOUNT);
            sRights.add(ADMIN_PRESET_CALENDAR_RESOURCE);
            sRights.add(ADMIN_PRESET_CONFIG);
            sRights.add(ADMIN_PRESET_COS);
            sRights.add(ADMIN_PRESET_DISTRIBUTION_LIST);
            sRights.add(ADMIN_PRESET_DYNAMIC_GROUP);
            sRights.add(ADMIN_PRESET_DOMAIN);
            sRights.add(ADMIN_PRESET_GLOBALGRANT);
            sRights.add(ADMIN_PRESET_SERVER);
            sRights.add(ADMIN_PRESET_XMPP_COMPONENT);
            sRights.add(ADMIN_PRESET_ZIMLET);
            
            sRights.add(ADMIN_ATTR_GETALL_ACCOUNT);
            sRights.add(ADMIN_ATTR_SETALL_ACCOUNT);
            sRights.add(ADMIN_ATTR_GETSOME_ACCOUNT);
            sRights.add(ADMIN_ATTR_SETSOME_ACCOUNT);
            sRights.add(ADMIN_ATTR_GETALL_CALENDAR_RESOURCE);
            sRights.add(ADMIN_ATTR_SETALL_CALENDAR_RESOURCE);
            sRights.add(ADMIN_ATTR_GETSOME_CALENDAR_RESOURCE);
            sRights.add(ADMIN_ATTR_SETSOME_CALENDAR_RESOURCE);
            sRights.add(ADMIN_ATTR_GETALL_CONFIG);
            sRights.add(ADMIN_ATTR_SETALL_CONFIG);
            sRights.add(ADMIN_ATTR_GETSOME_CONFIG);
            sRights.add(ADMIN_ATTR_SETSOME_CONFIG);
            sRights.add(ADMIN_ATTR_GETALL_COS);
            sRights.add(ADMIN_ATTR_SETALL_COS);
            sRights.add(ADMIN_ATTR_GETSOME_COS);
            sRights.add(ADMIN_ATTR_SETSOME_COS);
            sRights.add(ADMIN_ATTR_GETALL_DISTRIBUTION_LIST);
            sRights.add(ADMIN_ATTR_SETALL_DISTRIBUTION_LIST);
            sRights.add(ADMIN_ATTR_GETSOME_DISTRIBUTION_LIST);
            sRights.add(ADMIN_ATTR_SETSOME_DISTRIBUTION_LIST);
            sRights.add(ADMIN_ATTR_GETALL_DYNAMIC_GROUP);
            sRights.add(ADMIN_ATTR_SETALL_DYNAMIC_GROUP);
            sRights.add(ADMIN_ATTR_GETSOME_DYNAMIC_GROUP);
            sRights.add(ADMIN_ATTR_SETSOME_DYNAMIC_GROUP);
            sRights.add(ADMIN_ATTR_GETALL_DOMAIN);
            sRights.add(ADMIN_ATTR_SETALL_DOMAIN);
            sRights.add(ADMIN_ATTR_GETSOME_DOMAIN);
            sRights.add(ADMIN_ATTR_SETSOME_DOMAIN);
            sRights.add(ADMIN_ATTR_GETALL_SERVER);
            sRights.add(ADMIN_ATTR_SETALL_SERVER);
            sRights.add(ADMIN_ATTR_GETSOME_SERVER);
            sRights.add(ADMIN_ATTR_SETSOME_SERVER);
            sRights.add(ADMIN_ATTR_GETALL_ZIMLET);
            sRights.add(ADMIN_ATTR_SETALL_ZIMLET);
            sRights.add(ADMIN_ATTR_GETSOME_ZIMLET);
            sRights.add(ADMIN_ATTR_SETSOME_ZIMLET);
            
            sRights.add(ADMIN_COMBO_ACCOUNT);
            sRights.add(ADMIN_COMBO_CALENDAR_RESOURCE);
            sRights.add(ADMIN_COMBO_CONFIG);
            sRights.add(ADMIN_COMBO_COS);
            sRights.add(ADMIN_COMBO_DISTRIBUTION_LIST);
            sRights.add(ADMIN_COMBO_DYNAMIC_GROUP);
            sRights.add(ADMIN_COMBO_DOMAIN);
            sRights.add(ADMIN_COMBO_GLOBALGRANT);
            sRights.add(ADMIN_COMBO_SERVER);
            sRights.add(ADMIN_COMBO_XMPP_COMPONENT);
            sRights.add(ADMIN_COMBO_ZIMLET);
            // sRights.add(ADMIN_COMBO_ALL);
            
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
        if (mGlobalAdminAcct == null) {
            mGlobalAdminAcct = super.getGlobalAdminAcct();
        }
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
