/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AutoCompleteGalRequest;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.account.message.CreateIdentityRequest;
import com.zimbra.soap.account.message.CreateSignatureRequest;
import com.zimbra.soap.account.type.Identity;
import com.zimbra.soap.account.type.Signature;
import com.zimbra.soap.mail.message.CreateDataSourceRequest;
import com.zimbra.soap.mail.message.CreateDataSourceResponse;
import com.zimbra.soap.mail.message.CreateFolderRequest;
import com.zimbra.soap.mail.message.CreateFolderResponse;
import com.zimbra.soap.mail.message.DeleteDataSourceRequest;
import com.zimbra.soap.mail.type.MailPop3DataSource;
import com.zimbra.soap.mail.type.MdsConnectionType;
import com.zimbra.soap.mail.type.NewFolderSpec;
import com.zimbra.soap.mail.type.Pop3DataSourceNameOrId;
import com.zimbra.soap.type.AccountSelector;

public class TestAccess extends SoapTest {
    private static final String PASSWORD = "test123";

    private static String ACCT_NAME;
    private static String ACCT_ID;
    private static SoapTransport ACCT;

    private static String OTHER_ACCT_NAME;
    private static String OTHER_ACCT_ID;
    private static SoapTransport OTHER_ACCT;

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;

    private static Sequencer seq = new Sequencer();

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());

        ACCT_NAME = TestUtil.getAddress("acct", domain.getName());
        Account acct = provUtil.createAccount(ACCT_NAME);
        ACCT_ID = acct.getId();
        ACCT = authUser(ACCT_NAME);

        OTHER_ACCT_NAME = TestUtil.getAddress("other-acct", domain.getName());
        Account otherAcct = provUtil.createAccount(OTHER_ACCT_NAME);
        OTHER_ACCT_ID = otherAcct.getId();
        OTHER_ACCT = authUser(OTHER_ACCT_NAME);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }

    private void accessTest(Perm perm, Object jaxbObject) throws Exception {
        for (Role role : Role.values()) {
            accessTest(role, perm, jaxbObject);
        }
    }

    private void accessTest(Role role, Perm perm, Object jaxbObject) throws Exception {

        String expectedCode = perm.getByRole(role);
        String resultCode = null;

        try {
            switch (role) {
            case R_USER:
                invokeJaxb(ACCT, jaxbObject);
                break;
            case R_USER_TARGET_SELF:
                invokeJaxbOnTargetAccount(ACCT, jaxbObject, ACCT_ID);
                break;
            case R_USER_TARGET_OTEHRUSER:
                invokeJaxbOnTargetAccount(ACCT, jaxbObject, OTHER_ACCT_ID);
                break;
            default:
                fail();
            }
            resultCode = Perm.OK;
        } catch (ServiceException e) {
            resultCode = e.getCode();

            if (!expectedCode.equals(resultCode)) {
                e.printStackTrace();
            }
        }

        assertEquals(expectedCode, resultCode);
    }

    // ================= APIs ================

    @Test
    public void Auth() throws Exception {
        AccountSelector acct =
            new AccountSelector(com.zimbra.soap.type.AccountBy.name, OTHER_ACCT_NAME);
        AuthRequest req = new AuthRequest(acct, PASSWORD);

        accessTest(Perm.PERM_AUTH_TOKEN_IGNORED, req);
    }

    @Test
    public void AutoCompleteGal() throws Exception {
        AutoCompleteGalRequest req = AutoCompleteGalRequest.createForName("zimbra");
        accessTest(Perm.PERM_SELF_ONLY, req);
    }

    @Test
    public void ChangePassword() throws Exception {
        AccountSelector acct =
            new AccountSelector(com.zimbra.soap.type.AccountBy.name, ACCT_NAME);
        ChangePasswordRequest req = new ChangePasswordRequest(acct, PASSWORD, PASSWORD);
        accessTest(Perm.PERM_AUTH_TOKEN_IGNORED, req);

        // urg, need to re-auth after changing password, because we now
        // invalidate auth token after password change.
        ACCT = authUser(ACCT_NAME);
    }

    private String createFolderAndReturnFolderId() throws Exception {
        String folderName = "/folder-" + seq.next();

        SoapTransport transport = authUser(ACCT_NAME);
        NewFolderSpec folterSpec = new NewFolderSpec(folderName);
        CreateFolderRequest req = new CreateFolderRequest(folterSpec);
        CreateFolderResponse resp = invokeJaxb(transport, req);

        return resp.getFolder().getId();
    }

    private CreateDataSourceRequest buildCeateDataSourceRequest(String dateSourceName) throws Exception {

        MailPop3DataSource dataSource = new MailPop3DataSource();
        dataSource.setName(dateSourceName);
        dataSource.setEnabled(Boolean.TRUE);
        dataSource.setMdsConnectionType(MdsConnectionType.ssl);
        dataSource.setHost("pop3.google.com");
        dataSource.setPort(8888);
        dataSource.setUsername("my-pop3-name");
        dataSource.setPassword("my-pop3-password");
        dataSource.setFolderId(createFolderAndReturnFolderId());

        CreateDataSourceRequest req = new CreateDataSourceRequest();
        req.setDataSource(dataSource);

        return req;
    }

    @Test
    public void CreateDataSource() throws Exception {
        String dateSourceName = genDataSourceName(seq);

        CreateDataSourceRequest req = buildCeateDataSourceRequest(dateSourceName);

        /*
        XMLElement req = new XMLElement(MailConstants.CREATE_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_NAME, dateSourceName);
        dataSource.addAttribute(MailConstants.A_DS_IS_ENABLED, "true");
        dataSource.addAttribute(MailConstants.A_DS_HOST, "pop3.google.com");
        dataSource.addAttribute(MailConstants.A_DS_PORT, "pop3.google.com");
        dataSource.addAttribute(MailConstants.A_DS_USERNAME, "my-pop3-name");
        dataSource.addAttribute(MailConstants.A_DS_PASSWORD, "my-pop3-password");
        dataSource.addAttribute(MailConstants.A_FOLDER, createFolderAndReturnFolderId(role, perm));
        dataSource.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, "ssl");
        */

        accessTest(Perm.PERM_SELF_ONLY, req);
    }

    @Test
    public void CreateIdentity() throws Exception {
        String identityName = genIdentityName(seq);
        Identity identity = Identity.fromName(identityName);
        CreateIdentityRequest req = new CreateIdentityRequest(identity);
        /*
        XMLElement req = new XMLElement(AccountConstants.CREATE_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, identityName);
        */
        accessTest(Perm.PERM_SELF_ONLY, req);
    }

    @Test
    public void CreateSignature() throws Exception {
        String signatureName = genSignatureName(seq);

        Signature sig = new Signature((String) null, signatureName, (String) null, (String) null);
        CreateSignatureRequest req = new CreateSignatureRequest(sig);

        /*
        XMLElement req = new XMLElement(AccountConstants.CREATE_SIGNATURE_REQUEST);
        Element signature = req.addElement(AccountConstants.E_SIGNATURE);
        signature.addAttribute(AccountConstants.A_NAME, signatureName);
        */
        accessTest(Perm.PERM_SELF_ONLY, req);
    }

    @Test
    public void DeleteDataSource() throws Exception {
        String dateSourceName = genDataSourceName(seq);

        // create a data cource
        SoapTransport transport = authUser(ACCT_NAME);
        CreateDataSourceRequest createReq = buildCeateDataSourceRequest(dateSourceName);
        CreateDataSourceResponse createResp = invokeJaxb(transport, createReq);
        assertNotNull("CreateDataSourceResponse", createResp);

        Pop3DataSourceNameOrId name = new Pop3DataSourceNameOrId();
        name.setName(dateSourceName);
        DeleteDataSourceRequest req = new DeleteDataSourceRequest();
        req.addDataSource(name);

        /*
        DataSource ds = mProvAdmin.setup_createDataSource(this, role, perm, ACCT_1_EMAIL, dateSourceName);

        XMLElement req = new XMLElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_ID, ds.getId());
        */

        accessTest(Perm.PERM_SELF_ONLY, req);
    }

    /*
    private void DeleteIdentity(Role role, Perm perm) throws Exception {
        String identityName = "identity-delete-"+random();
        mProvAdmin.createIdentity(mProvAdmin.get(Key.AccountBy.id, ACCT_1_ID), identityName, new HashMap<String, Object>());

        XMLElement req = new XMLElement(AccountConstants.DELETE_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, identityName);
        accessTest(role, perm, req);
    }

    private void DeleteSignature(Role role, Perm perm) throws Exception {
        String signatureName = "signature-delete-"+random();
        Signature signature = mProvAdmin.createSignature(mProvAdmin.get(Key.AccountBy.id, ACCT_1_ID), signatureName, new HashMap<String, Object>());

        XMLElement req = new XMLElement(AccountConstants.DELETE_SIGNATURE_REQUEST);
        Element identity = req.addElement(AccountConstants.E_SIGNATURE);
        identity.addAttribute(AccountConstants.A_ID, signature.getId());
        accessTest(role, perm, req);
    }

    private void GetAccountInfo(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_ACCOUNT_INFO_REQUEST);
        Element a = req.addElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");

        if (role == Role.R_USER || role == Role.R_USER_TARGET_SELF)
            a.setText(ACCT_1_EMAIL);
        else
            a.setText(ACCT_2_EMAIL);

        accessTest(role, perm, req);
    }

    private void GetAllLocales(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_ALL_LOCALES_REQUEST);
        accessTest(role, perm, req);
    }

    private void GetAvailableLocales(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST);
        accessTest(role, perm, req);
    }

    private void GetAvailableSkins(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
        accessTest(role, perm, req);
    }

    private void GetDataSourcess(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(MailConstants.GET_DATA_SOURCES_REQUEST);
        accessTest(role, perm, req);
    }

    public void GetIdentities(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_IDENTITIES_REQUEST);
        accessTest(role, perm, req);
    }

    private void GetInfo(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        accessTest(role, perm, req);
    }

    private void GetPrefs(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_PREFS_REQUEST);
        accessTest(role, perm, req);
    }

    private void GetSignatures(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.GET_SIGNATURES_REQUEST);
        accessTest(role, perm, req);
    }

    private void ModifyDataSource(Role role, Perm perm) throws Exception {
        String dateSourceName = "datasource-modify-"+random();
        DataSource ds = mProvAdmin.setup_createDataSource(this, role, perm, ACCT_1_EMAIL, dateSourceName);

        XMLElement req = new XMLElement(MailConstants.MODIFY_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_ID, ds.getId());
        dataSource.addAttribute(MailConstants.A_DS_IS_ENABLED, "false");
        accessTest(role, perm, req);
    }

    private void ModifyIdentity(Role role, Perm perm) throws Exception {
        String identityName = "identity-modify-"+random();
        mProvAdmin.createIdentity(mProvAdmin.get(Key.AccountBy.id, ACCT_1_ID), identityName, new HashMap<String, Object>());

        XMLElement req = new XMLElement(AccountConstants.MODIFY_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountConstants.E_IDENTITY);
        identity.addAttribute(AccountConstants.A_NAME, identityName);
        accessTest(role, perm, req);
    }

    private void ModifyPrefs(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.MODIFY_PREFS_REQUEST);
        Element p = req.addElement(AccountConstants.E_PREF);
        p.addAttribute(AccountConstants.A_NAME, Provisioning.A_zimbraPrefSkin);
        p.setText("beach");
        accessTest(role, perm, req);
    }

    private void ModifyProperties(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.MODIFY_PROPERTIES_REQUEST);
        accessTest(role, perm, req);
    }

    private void ModifySignature(Role role, Perm perm) throws Exception {
        String signatureName = "signature-modify-"+random();
        Signature signature = mProvAdmin.createSignature(mProvAdmin.get(Key.AccountBy.id, ACCT_1_ID), signatureName, new HashMap<String, Object>());

        XMLElement req = new XMLElement(AccountConstants.MODIFY_SIGNATURE_REQUEST);
        Element identity = req.addElement(AccountConstants.E_SIGNATURE);
        identity.addAttribute(AccountConstants.A_ID, signature.getId());
        accessTest(role, perm, req);
    }

    private void SearchCalendarResources(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.SEARCH_CALENDAR_RESOURCES_REQUEST);
        Element sf = req.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        Element cond = sf.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, Provisioning.A_zimbraCalResType);
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Equipment");
        accessTest(role, perm, req);
    }

    private void SearchGal(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.SEARCH_GAL_REQUEST);
        req.addElement(AdminConstants.E_NAME).setText("phoebe");
        req.addAttribute(AdminConstants.A_TYPE, "all");
        accessTest(role, perm, req);
    }

    private void SyncGal(Role role, Perm perm) throws Exception {
        XMLElement req = new XMLElement(AccountConstants.SYNC_GAL_REQUEST);
        accessTest(role, perm, req);
    }

    private void TestDataSource(Role role, Perm perm) throws Exception {
        String dateSourceName = "datasource-test-"+random();
        DataSource ds = mProvAdmin.setup_createDataSource(this, role, perm, ACCT_1_EMAIL, dateSourceName);

        XMLElement req = new XMLElement(MailConstants.TEST_DATA_SOURCE_REQUEST);
        Element dataSource = req.addElement(MailConstants.E_DS_POP3);
        dataSource.addAttribute(MailConstants.A_ID, ds.getId());
        accessTest(role, perm, req);
    }
    */

    private static class Perm {
        static final String OK = "OK";

        static final Perm PERM_SELF_ONLY = new Perm(OK, OK, ServiceException.PERM_DENIED);
        static final Perm PERM_AUTH_TOKEN_IGNORED = new Perm(OK, OK, OK);

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

    // @Test
    public void testAccess() throws Exception {
        for (Role role : Role.values()) {
            /*
            Auth(role, Perm.PERM_ALLOW_OTHERS);
            AutoCompleteGal(role, Perm.PERM_SELF_ONLY);

            ChangePassword(role, Perm.PERM_ALLOW_OTHERS);

            // urg, need to re-auth after changing password, because we now
            // invalidate auth token after password change.
            ACCT = authUser(ACCT_NAME);

            CreateDataSource(role, Perm.PERM_SELF_ONLY);
            CreateIdentity(role, Perm.PERM_SELF_ONLY);
            CreateSignature(role, Perm.PERM_SELF_ONLY);
            DeleteDataSource(role, Perm.PERM_SELF_ONLY);
            */

            /*
            DeleteIdentity(role, Perm.PERM_SELF_ONLY);
            DeleteSignature(role, Perm.PERM_SELF_ONLY);
            GetAccountInfo(role, Perm.PERM_ALLOW_OTHERS);
            GetAllLocales(role, Perm.PERM_ALLOW_OTHERS);
            GetAvailableLocales(role, Perm.PERM_SELF_ONLY);
            GetAvailableSkins(role, Perm.PERM_SELF_ONLY);
            GetDataSourcess(role, Perm.PERM_SELF_ONLY);
            GetIdentities(role, Perm.PERM_SELF_ONLY);
            GetInfo(role, Perm.PERM_SELF_ONLY);
            GetPrefs(role, Perm.PERM_SELF_ONLY);
            GetSignatures(role, Perm.PERM_SELF_ONLY);
            ModifyDataSource(role, Perm.PERM_SELF_ONLY);
            ModifyIdentity(role, Perm.PERM_SELF_ONLY);
            ModifyPrefs(role, Perm.PERM_SELF_ONLY);
            ModifyProperties(role, Perm.PERM_SELF_ONLY);
            ModifySignature(role, Perm.PERM_SELF_ONLY);
            SearchCalendarResources(role, Perm.PERM_SELF_ONLY);
            SearchGal(role, Perm.PERM_SELF_ONLY);
            SyncGal(role, Perm.PERM_SELF_ONLY);
            TestDataSource(role, Perm.PERM_SELF_ONLY);
            */
        }
    }

}

