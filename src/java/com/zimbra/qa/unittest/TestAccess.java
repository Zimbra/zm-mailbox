/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.soap.SoapProvisioning;


public class TestAccess extends TestCase {
    private String TEST_ID;
    private static String TEST_NAME = "test-access";
    
    private SoapAdminUser mProvAdmin;
    private SoapUser mSoapUser1;
    
    private int mBrainDeadSingleThreadRandom = 0;
        
    private static String PASSWORD = "test123";
    private String DOMAIN_NAME;
    private String ACCT_1_EMAIL;
    private String ACCT_2_EMAIL;
    private String ACCT_1_ID;
    private String ACCT_2_ID;
    
    public void setUp() throws Exception {
        
        TEST_ID = TestProvisioningUtil.genTestId();
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        
        // mProvAdmin = Provisioning.getInstance();
        mProvAdmin = new SoapAdminUser("admin");
        mProvAdmin.auth();

        DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
        ACCT_1_EMAIL = "acct-1@" + DOMAIN_NAME;
        ACCT_2_EMAIL = "acct-2@" + DOMAIN_NAME;
        
        Domain domain = createDomain(DOMAIN_NAME);
        Account acct1 = createAccount(ACCT_1_EMAIL);
        Account acct2 = createAccount(ACCT_2_EMAIL);
        
        ACCT_1_ID = acct1.getId();
        ACCT_2_ID = acct2.getId();
        
        mSoapUser1 = new SoapUser(ACCT_1_EMAIL, ACCT_1_ID);
        mSoapUser1.auth();
    }
    
    private Domain createDomain(String domainName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = mProvAdmin.createDomain(domainName, attrs);
        assertNotNull(domain);
        return domain;
    }
    
    private Account createAccount(String accountEmail) throws Exception {
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        Account acct = mProvAdmin.createAccount(accountEmail, PASSWORD, acctAttrs);
        assertNotNull(acct);
        return acct;
    }
    
    private static class SoapUser extends SoapProvisioning {
        
        String mName;
        String mId;
        
        SoapUser(String name, String id) {
            mName = name;
            mId = id;
            
            setURL();
        }
        
        void setURL() {
            soapSetURI(TestUtil.getSoapUrl());
        }
        
        void auth() throws ServiceException {
            XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
            Element a = req.addElement(AccountConstants.E_ACCOUNT);
            a.addAttribute(AccountConstants.A_BY, "name");
            a.setText(mName);
            req.addElement(AccountConstants.E_PASSWORD).setText(PASSWORD);
            Element response = invoke(req);
            String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
            setAuthToken(authToken);
        }
 
        protected Element invokeOnTargetAccount(Element request, String targetId) throws ServiceException {
            return super.invokeOnTargetAccount(request, targetId);
        }

    }
    
    private static class SoapAdminUser extends SoapUser {
        
        SoapAdminUser(String name) {
            super(name, null);
        }
        
        void setURL() {
            soapSetURI(TestUtil.getAdminSoapUrl());
        }
        
        void auth() throws ServiceException {
            soapAdminAuthenticate(mName, PASSWORD);
        }
        
        DataSource setup_createDataSource(String acctName, String dataSourceName) {
            
            DataSource dataSource = null;
            try {
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
                attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
                attrs.put(Provisioning.A_zimbraDataSourceFolderId, "1");
                attrs.put(Provisioning.A_zimbraDataSourceHost, "pop3.google.com");
                attrs.put(Provisioning.A_zimbraDataSourcePassword, "my-pop3-password");
                attrs.put(Provisioning.A_zimbraDataSourcePort, "8888");
                attrs.put(Provisioning.A_zimbraDataSourceUsername, "my-pop3-name");
                attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "TRUE");
                dataSource = createDataSource(get(Provisioning.AccountBy.name, acctName), 
                                              DataSource.Type.pop3, dataSourceName, attrs);
            } catch (ServiceException e) {
                e.printStackTrace();
                fail();
            }
            return dataSource;
       }
    }
    
    private String random() {
        return "" + (mBrainDeadSingleThreadRandom++);
    }
    
    void accessTest(Role role, Perm perm, XMLElement req)  throws Exception {
        System.out.println(role.name() + ": " + req.getName());
        
        String expectedCode = perm.getByRole(role);
        String resultCode = null;
        
        try {
            switch (role) {
            case R_USER: 
                mSoapUser1.invoke(req);
                break;
            case R_USER_TARGET_SELF:
                mSoapUser1.invokeOnTargetAccount(req, ACCT_1_ID);
                break;
            case R_USER_TARGET_OTEHRUSER:
                mSoapUser1.invokeOnTargetAccount(req, ACCT_2_ID);
                break;
            default:
            	fail();
            }
            resultCode = Perm.OK;
        } catch (ServiceException e) {
            resultCode = e.getCode();
            
            if (!expectedCode.equals(resultCode))
                e.printStackTrace();
        }
        
        assertEquals(expectedCode, resultCode);
    }
    
    // ================= APIs ================
    public void Auth(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
        Element a = req.addElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        a.setText(ACCT_2_EMAIL);
        req.addElement(AccountConstants.E_PASSWORD).setText(PASSWORD);
        accessTest(role, perm, req);
    }
    
    public void AutoCompleteGal(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.AUTO_COMPLETE_GAL_REQUEST);
        req.addElement(AdminConstants.E_NAME).setText("phoebe");
        req.addAttribute(AdminConstants.A_TYPE, "all");
        req.addAttribute(AdminConstants.A_LIMIT, "10");
        accessTest(role, perm, req);
    }

    public void ChangePassword(Role role, Perm perm) throws Exception {
    	XMLElement req = new XMLElement(AccountConstants.CHANGE_PASSWORD_REQUEST);
        Element a = req.addElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        a.setText(ACCT_1_EMAIL);
        req.addElement(AccountConstants.E_OLD_PASSWORD).setText(PASSWORD);
        req.addElement(AccountConstants.E_PASSWORD).setText(PASSWORD); 
        accessTest(role, perm, req);
    }
    
    public void CreateDataSource(Role role, Perm perm) throws Exception {
        String dateSourceName = "datasource-create-"+random();
        
        XMLElement req = new XMLElement(MailConstants.CREATE_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_NAME, dateSourceName);
        dataSource.addAttribute(MailConstants.A_DS_IS_ENABLED, "true");
        dataSource.addAttribute(MailConstants.A_DS_HOST, "pop3.google.com");
        dataSource.addAttribute(MailConstants.A_DS_PORT, "8888");
        dataSource.addAttribute(MailConstants.A_DS_USERNAME, "my-pop3-name");
        dataSource.addAttribute(MailConstants.A_DS_PASSWORD, "my-pop3-password");
        dataSource.addAttribute(MailConstants.A_FOLDER, "1");
        dataSource.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, "ssl");
        accessTest(role, perm, req);
    }
    
    public void CreateIdentity(Role role, Perm perm) throws Exception {
        String identityName = "identity-create-"+random();
        
        XMLElement req = new XMLElement(AccountConstants.CREATE_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, identityName);
        accessTest(role, perm, req);
    }
    
    public void CreateSignature(Role role, Perm perm) throws Exception {
        String signatureName = "signature-create-"+random();
        
        XMLElement req = new XMLElement(AccountConstants.CREATE_SIGNATURE_REQUEST);
        Element signature = req.addElement(AccountConstants.E_SIGNATURE);
        signature.addAttribute(AccountConstants.A_NAME, signatureName);
        accessTest(role, perm, req);
    }
    
    public void DeleteDataSource(Role role, Perm perm) throws Exception {
        String dateSourceName = "datasource-delete-"+random();
        DataSource ds = mProvAdmin.setup_createDataSource(ACCT_1_EMAIL, dateSourceName);
        
        XMLElement req = new XMLElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_ID, ds.getId());
        accessTest(role, perm, req);
    }
    
    public void DeleteIdentity(Role role, Perm perm) throws Exception {
        String identityName = "identity-delete-"+random();
        mProvAdmin.createIdentity(mProvAdmin.get(Provisioning.AccountBy.id, ACCT_1_ID), identityName, new HashMap<String, Object>());
        
        XMLElement req = new XMLElement(AccountConstants.DELETE_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, identityName);
        accessTest(role, perm, req);
    }
    
    public void DeleteSignature(Role role, Perm perm) throws Exception {
        String signatureName = "signature-delete-"+random();
        Signature signature = mProvAdmin.createSignature(mProvAdmin.get(Provisioning.AccountBy.id, ACCT_1_ID), signatureName, new HashMap<String, Object>());
        
        XMLElement req = new XMLElement(AccountConstants.DELETE_SIGNATURE_REQUEST);
        Element identity = req.addElement(AccountConstants.E_SIGNATURE);
        identity.addAttribute(AccountConstants.A_ID, signature.getId());
        accessTest(role, perm, req);
    }

    public void GetAccountInfo(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_ACCOUNT_INFO_REQUEST);
        Element a = req.addElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        
        if (role == Role.R_USER || role == Role.R_USER_TARGET_SELF)
            a.setText(ACCT_1_EMAIL);
        else
            a.setText(ACCT_2_EMAIL);
        
        accessTest(role, perm, req);
    }
    
    public void GetAllLocales(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_ALL_LOCALES_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetAvailableLocales(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetAvailableSkins(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetDataSourcess(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(MailConstants.GET_DATA_SOURCES_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetIdentities(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_IDENTITIES_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetInfo(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetPrefs(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_PREFS_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void GetSignatures(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_SIGNATURES_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void ModifyDataSource(Role role, Perm perm) throws Exception {
        String dateSourceName = "datasource-modify-"+random();
        DataSource ds = mProvAdmin.setup_createDataSource(ACCT_1_EMAIL, dateSourceName);
        
        XMLElement req = new XMLElement(MailConstants.MODIFY_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_ID, ds.getId());
        dataSource.addAttribute(MailConstants.A_DS_IS_ENABLED, "false");
        accessTest(role, perm, req);
    }
    
    public void ModifyIdentity(Role role, Perm perm) throws Exception {
        String identityName = "identity-modify-"+random();
        mProvAdmin.createIdentity(mProvAdmin.get(Provisioning.AccountBy.id, ACCT_1_ID), identityName, new HashMap<String, Object>());
        
        XMLElement req = new XMLElement(AccountConstants.MODIFY_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, identityName);
        accessTest(role, perm, req);
    }
    
    public void ModifyPrefs(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.MODIFY_PREFS_REQUEST);
        Element p = req.addElement(AccountConstants.E_PREF);
        p.addAttribute(AccountConstants.A_NAME, Provisioning.A_zimbraPrefSkin);
        p.setText("beach");
        accessTest(role, perm, req);
    }
    
    public void ModifyProperties(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.MODIFY_PROPERTIES_REQUEST);
        accessTest(role, perm, req);
    }
    
    public void ModifySignature(Role role, Perm perm) throws Exception {
        String signatureName = "signature-modify-"+random();
        Signature signature = mProvAdmin.createSignature(mProvAdmin.get(Provisioning.AccountBy.id, ACCT_1_ID), signatureName, new HashMap<String, Object>());
        
        XMLElement req = new XMLElement(AccountConstants.MODIFY_SIGNATURE_REQUEST);
        Element identity = req.addElement(AccountConstants.E_SIGNATURE);
        identity.addAttribute(AccountConstants.A_ID, signature.getId());
        accessTest(role, perm, req);
    }
    
    public void SearchCalendarResources(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
        Element sf = req.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        Element cond = sf.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, Provisioning.A_zimbraCalResType);
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Equipment");
        accessTest(role, perm, req);
    }
    
    public void SearchGal(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.SEARCH_GAL_REQUEST);
        req.addElement(AdminConstants.E_NAME).setText("phoebe");
        req.addAttribute(AdminConstants.A_TYPE, "all");
        accessTest(role, perm, req);
    }
    
    public void SyncGal(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.SYNC_GAL_REQUEST);
        accessTest(role, perm, req);
    }

    public void TestDataSource(Role role, Perm perm) throws Exception {
        String dateSourceName = "datasource-test-"+random();
        DataSource ds = mProvAdmin.setup_createDataSource(ACCT_1_EMAIL, dateSourceName);
        
        XMLElement req = new XMLElement(MailConstants.TEST_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_ID, ds.getId());
        accessTest(role, perm, req);
    }
    
    private static class Perm {
        static final String OK = "OK";
        
        static final Perm Perm_1 = new Perm(OK, OK, ServiceException.PERM_DENIED);
        static final Perm Perm_2 = new Perm(OK, OK, OK);
        
        String mPerms[] = new String[Role.values().length];
        
        Perm(String user, String userTargetSelf, String userTargetOtherUser) {
            mPerms[Role.R_USER.ordinal()] = user;
            mPerms[Role.R_USER_TARGET_SELF.ordinal()] = userTargetSelf;
            mPerms[Role.R_USER_TARGET_OTEHRUSER.ordinal()] = userTargetOtherUser;
        }
        
        String getByRole(Role role) {
            return mPerms[role.ordinal()];
        }
    }
    
    private static enum Role {
        R_USER,
        R_USER_TARGET_SELF,
        R_USER_TARGET_OTEHRUSER,
        /*
        R_ADMIN_DELEGATE_USER,
        R_ADMIN_DELEGATE_DOMAINADMIN,
        R_DOMAINADMIN_DELEGATE_USER,
        R_ADMIN,
        R_DOMAIN_ADMIN,
        to be completed
        */
    }
    

    
    public void testAccess() throws Exception {
        for (Role role : Role.values()) {
            Auth(role, Perm.Perm_2);
            AutoCompleteGal(role, Perm.Perm_1);
            ChangePassword(role, Perm.Perm_2);
            CreateDataSource(role, Perm.Perm_1);
            CreateIdentity(role, Perm.Perm_1);
            CreateSignature(role, Perm.Perm_1);
            DeleteDataSource(role, Perm.Perm_1);
            DeleteIdentity(role, Perm.Perm_1);
            DeleteSignature(role, Perm.Perm_1);
            GetAccountInfo(role, Perm.Perm_2);
            GetAllLocales(role, Perm.Perm_2);
            GetAvailableLocales(role, Perm.Perm_1);
            GetAvailableSkins(role, Perm.Perm_1);
            GetDataSourcess(role, Perm.Perm_1);
            GetIdentities(role, Perm.Perm_1);
            GetInfo(role, Perm.Perm_1);
            GetPrefs(role, Perm.Perm_1);
            GetSignatures(role, Perm.Perm_1);
            ModifyDataSource(role, Perm.Perm_1);
            ModifyIdentity(role, Perm.Perm_1);
            ModifyPrefs(role, Perm.Perm_1);
            ModifyProperties(role, Perm.Perm_1);
            ModifySignature(role, Perm.Perm_1);
            SearchCalendarResources(role, Perm.Perm_1);
            SearchGal(role, Perm.Perm_1);
            SyncGal(role, Perm.Perm_1);
            TestDataSource(role, Perm.Perm_1);
        }
        System.out.println("\nTest " + TEST_ID + " done, all is well\n");
    }
 
    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestAccess.class));
    }

}
