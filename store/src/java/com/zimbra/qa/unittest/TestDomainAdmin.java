/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.soap.admin.message.AddAccountAliasRequest;
import com.zimbra.soap.admin.message.AddAccountAliasResponse;
import com.zimbra.soap.admin.message.AddDistributionListAliasRequest;
import com.zimbra.soap.admin.message.AddDistributionListAliasResponse;
import com.zimbra.soap.admin.message.AddDistributionListMemberRequest;
import com.zimbra.soap.admin.message.AddDistributionListMemberResponse;
import com.zimbra.soap.admin.message.CreateAccountRequest;
import com.zimbra.soap.admin.message.CreateAccountResponse;
import com.zimbra.soap.admin.message.CreateCalendarResourceRequest;
import com.zimbra.soap.admin.message.CreateCalendarResourceResponse;
import com.zimbra.soap.admin.message.CreateDistributionListRequest;
import com.zimbra.soap.admin.message.CreateDistributionListResponse;
import com.zimbra.soap.admin.message.DeleteAccountRequest;
import com.zimbra.soap.admin.message.DeleteAccountResponse;
import com.zimbra.soap.admin.message.DeleteCalendarResourceRequest;
import com.zimbra.soap.admin.message.DeleteCalendarResourceResponse;
import com.zimbra.soap.admin.message.DeleteDistributionListRequest;
import com.zimbra.soap.admin.message.DeleteDistributionListResponse;
import com.zimbra.soap.admin.message.GetAccountRequest;
import com.zimbra.soap.admin.message.GetAccountResponse;
import com.zimbra.soap.admin.message.GetCalendarResourceRequest;
import com.zimbra.soap.admin.message.GetCalendarResourceResponse;
import com.zimbra.soap.admin.message.GetDistributionListRequest;
import com.zimbra.soap.admin.message.GetDistributionListResponse;
import com.zimbra.soap.admin.message.GetMailboxRequest;
import com.zimbra.soap.admin.message.GetMailboxResponse;
import com.zimbra.soap.admin.message.GrantRightRequest;
import com.zimbra.soap.admin.message.GrantRightResponse;
import com.zimbra.soap.admin.message.ModifyAccountRequest;
import com.zimbra.soap.admin.message.ModifyAccountResponse;
import com.zimbra.soap.admin.message.ModifyCalendarResourceRequest;
import com.zimbra.soap.admin.message.ModifyCalendarResourceResponse;
import com.zimbra.soap.admin.message.ModifyDistributionListRequest;
import com.zimbra.soap.admin.message.ModifyDistributionListResponse;
import com.zimbra.soap.admin.message.RemoveAccountAliasRequest;
import com.zimbra.soap.admin.message.RemoveAccountAliasResponse;
import com.zimbra.soap.admin.message.RemoveDistributionListAliasRequest;
import com.zimbra.soap.admin.message.RemoveDistributionListAliasResponse;
import com.zimbra.soap.admin.message.RenameAccountRequest;
import com.zimbra.soap.admin.message.RenameAccountResponse;
import com.zimbra.soap.admin.message.RenameCalendarResourceRequest;
import com.zimbra.soap.admin.message.RenameCalendarResourceResponse;
import com.zimbra.soap.admin.message.RenameDistributionListRequest;
import com.zimbra.soap.admin.message.RenameDistributionListResponse;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CalendarResourceSelector;
import com.zimbra.soap.admin.type.DistributionListSelector;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;
import com.zimbra.soap.admin.type.RightModifierInfo;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.GranteeType;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

/**
 * Primary focus of these tests is to ensure that Jaxb objects work, in
 * particular where SoapProvisioning uses them
 */
public class TestDomainAdmin extends TestCase {

    private SoapProvisioning adminSoapProv = null;

    private final static String ADMINISTRATOR_DOMAIN = "testadmin.domain";
    private final static String ADMINISTERED_DOMAIN = "administered.domain";
    private final static String DIFFERENT_DOMAIN = "diff.domain";
    private final static String DOMADMIN = "domadmin@" + ADMINISTRATOR_DOMAIN;
    private final static String TARGET_ACCT = "targetacct@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_ACCT_RENAMED = "targetacctrenamed@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_ACCT2 = "targetacct2@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_CALRES = "targetroom@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_CALRES2 = "targetroom2@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_CALRES_RENAMED = "targetroomrenamed@" + ADMINISTERED_DOMAIN;
    private final static String DOMADMINGROUP = "domadmingroup@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_DL = "targetdl@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_DL2 = "targetdl2@" + ADMINISTERED_DOMAIN;
    private final static String TARGET_DL_RENAMED = "targetdlrenamed@" + ADMINISTERED_DOMAIN;
    private final static String DIFF_ACCT = "diffacct@" + DIFFERENT_DOMAIN;
    private final static String DIFF_ACCT2 = "diffacct2@" + DIFFERENT_DOMAIN;
    private final static String DIFF_CALRES = "diffroom@" + DIFFERENT_DOMAIN;
    private final static String DIFF_CALRES2 = "diffroom2@" + DIFFERENT_DOMAIN;
    private final static String DIFF_DL = "diffdl@" + DIFFERENT_DOMAIN;
    private final static String DIFF_DL2 = "diffdl2@" + DIFFERENT_DOMAIN;
    private final static String ALIAS_FOR_TARGET_DL = "alias_4_targetdl@" + ADMINISTERED_DOMAIN;
    private final static String ALIAS_FOR_TARGET_ACCT = "alias_4_targetacct@" + ADMINISTERED_DOMAIN;
    private final static String ALIAS_FOR_TARGET_ACCT2 = "alias_4_targetacct2@" + ADMINISTERED_DOMAIN;

    public void init() throws Exception {
        oneTimeTearDown();
    }

    public void oneTimeTearDown() {
        ZimbraLog.test.info("in TestDomainAdmin oneTimeTearDown");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        if (!TestUtil.fromRunUnitTests) {
            TestUtil.cliSetup();
        }
        tearDown();
        TestJaxbProvisioning.ensureDomainExists(ADMINISTERED_DOMAIN);
        TestJaxbProvisioning.ensureDomainExists(ADMINISTRATOR_DOMAIN);
        TestJaxbProvisioning.ensureDomainExists(DIFFERENT_DOMAIN);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        ZimbraLog.test.debug("in TestDomainAdmin tearDown");
        if (adminSoapProv == null) {
            adminSoapProv = TestUtil.newSoapProvisioning();
        }
        TestUtil.deleteAccount(TARGET_ACCT);
        TestUtil.deleteAccount(TARGET_ACCT_RENAMED);
        TestUtil.deleteAccount(TARGET_ACCT2);
        TestUtil.deleteAccount(TARGET_CALRES);
        TestUtil.deleteAccount(TARGET_CALRES2);
        TestUtil.deleteAccount(TARGET_CALRES_RENAMED);
        TestJaxbProvisioning.deleteDlIfExists(TARGET_DL);
        TestJaxbProvisioning.deleteDlIfExists(TARGET_DL2);
        TestJaxbProvisioning.deleteDlIfExists(TARGET_DL_RENAMED);
        TestUtil.deleteAccount(DOMADMIN);
        TestUtil.deleteAccount(DOMADMIN);
        TestUtil.deleteAccount(DIFF_ACCT);
        TestUtil.deleteAccount(DIFF_ACCT2);
        TestUtil.deleteAccount(DIFF_CALRES);
        TestUtil.deleteAccount(DIFF_CALRES2);
        TestJaxbProvisioning.deleteDlIfExists(DOMADMINGROUP);
        TestJaxbProvisioning.deleteDlIfExists(DIFF_DL);
        TestJaxbProvisioning.deleteDlIfExists(DIFF_DL2);
        TestJaxbProvisioning.deleteDomainIfExists(ADMINISTERED_DOMAIN);
        TestJaxbProvisioning.deleteDomainIfExists(ADMINISTRATOR_DOMAIN);
        TestJaxbProvisioning.deleteDomainIfExists(DIFFERENT_DOMAIN);
    }

    private void grantRight(SoapProvisioning soapProv, TargetType targetType, String targetName,
            GranteeType granteeType, String granteeName, String rightName)
    throws ServiceException {
        GranteeSelector grantee;
        EffectiveRightsTargetSelector target;
        RightModifierInfo right;
        GrantRightResponse grResp;

        grantee = new GranteeSelector(granteeType, GranteeBy.name, granteeName);
        target = new EffectiveRightsTargetSelector(targetType, TargetBy.name, targetName);
        right = new RightModifierInfo(rightName);
        grResp = soapProv.invokeJaxb(new GrantRightRequest(target, grantee, right));
        assertNotNull("GrantRightResponse for " + right.getValue(), grResp);
    }

    private void grantRight(SoapProvisioning soapProv, TargetType targetType, String targetName, String granteeName,
            String rightName)
    throws ServiceException {
        grantRight(soapProv, targetType, targetName, GranteeType.usr, granteeName, rightName);
    }

    private void failToGrantRight(SoapProvisioning soapProv, TargetType targetType, String targetName,
            String granteeName, String rightName, String expectedFailureReason)
    throws ServiceException {
        GranteeSelector grantee;
        EffectiveRightsTargetSelector target;
        RightModifierInfo right;

        grantee = new GranteeSelector(GranteeType.usr, GranteeBy.name, granteeName);
        target = new EffectiveRightsTargetSelector(targetType, TargetBy.name, targetName);
        right = new RightModifierInfo(rightName);
        try {
            soapProv.invokeJaxb(new GrantRightRequest(target, grantee, right));
            fail(String.format("granting %s right succeeded when it shouldn't have", rightName));
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, expectedFailureReason);
        }
    }

    public String createAdminConsoleStyleDomainAdminGroup(String domAdminGrp) throws ServiceException {
        CreateDistributionListResponse cdlResp;
        CreateDistributionListRequest cdlReq = new CreateDistributionListRequest(domAdminGrp);
        cdlReq.addAttr(new Attr(Provisioning.A_zimbraIsAdminGroup, "TRUE"));
        cdlResp = adminSoapProv.invokeJaxb(cdlReq);
        assertNotNull("CreateDistributionListResponse for " + cdlReq.getName() + " as Admin", cdlResp);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "accountListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "downloadsView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "DLListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "aliasListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "resourceListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "saveSearch"));
        ModifyDistributionListResponse mdlResp = adminSoapProv.invokeJaxb(
                new ModifyDistributionListRequest(cdlResp.getDl().getId(), attrs));
        assertNotNull("ModifyDistributionListResponse for " + cdlReq.getName() + " as Admin", mdlResp);


        grantRight(adminSoapProv, TargetType.domain, ADMINISTERED_DOMAIN, GranteeType.grp, domAdminGrp,
                RightConsts.RT_domainAdminConsoleRights);
        grantRight(adminSoapProv, TargetType.global, "globalacltarget", GranteeType.grp, domAdminGrp,
                RightConsts.RT_domainAdminZimletRights);
        grantRight(adminSoapProv, TargetType.global, "globalacltarget", GranteeType.grp, domAdminGrp,
                RightConsts.RT_adminLoginCalendarResourceAs);

        adminSoapProv.flushCache(CacheEntryType.acl, null);
        return cdlResp.getDl().getId();
    }

    public String createAdminConsoleStyleDomainAdmin(String domAdminName) throws ServiceException {
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "accountListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "downloadsView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "DLListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "aliasListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "resourceListView"));
        attrs.add(new Attr(Provisioning.A_zimbraAdminConsoleUIComponents, "saveSearch"));
        CreateAccountRequest caReq = new CreateAccountRequest(domAdminName, TestUtil.DEFAULT_PASSWORD, attrs);
        CreateAccountResponse caResp = adminSoapProv.invokeJaxb(caReq);
        assertNotNull("CreateAccountResponse for " + domAdminName, caResp);

        grantRight(adminSoapProv, TargetType.domain, ADMINISTERED_DOMAIN, domAdminName,
                RightConsts.RT_domainAdminConsoleRights);
        grantRight(adminSoapProv, TargetType.global, "globalacltarget", domAdminName,
                RightConsts.RT_domainAdminZimletRights);
        grantRight(adminSoapProv, TargetType.global, "globalacltarget", domAdminName,
                RightConsts.RT_adminLoginCalendarResourceAs);

        adminSoapProv.flushCache(CacheEntryType.acl, null);
        return caResp.getAccount().getId();
    }

    public static SoapProvisioning getSoapProvisioning(String account, String password) throws ServiceException {
        // SoapProvisioning sp = new SoapProvisioning();
        SoapProvisioning sp = SoapProvisioning.getAdminInstance(true);
        if (account != null && password != null) {
            sp.soapAdminAuthenticate(account, password);
        } else {
            sp.soapZimbraAdminAuthenticate();
        }
        return sp;
    }

    public static String soapReason(SoapFaultException sfe) {
        Element fault = sfe.getFault();
        if (null != fault) {
            try {
                Element reason = fault.getElement("Reason");
                if (null != reason) {
                    Element text = reason.getElement("Text");
                    if (null != text) {
                        return text.getText();
                    }
                    return reason.toString();
                }
            } catch (ServiceException e) {
                return sfe.getMessage();
            }
            return fault.toString();
        }
        return sfe.getMessage();
    }

    public static void checkSoapReason(SoapFaultException sfe, String expectedSubstring) {
        String soapReason = soapReason(sfe);
        assertTrue("SoapFault reason '" + soapReason + "' should contain '" + expectedSubstring + "'",
                soapReason.contains(expectedSubstring));
    }

    @Test
    public void testAccountPassword() throws Exception {
        Account acct = adminSoapProv.createAccount(TARGET_ACCT, TestUtil.DEFAULT_PASSWORD, null);
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        adminSoapProv.authAccount(acct, TestUtil.DEFAULT_PASSWORD, AuthContext.Protocol.test);
        assertNotNull("Account for " + TARGET_ACCT, acct);

        // first as admin
        adminSoapProv.changePassword(acct, TestUtil.DEFAULT_PASSWORD, "DelTA4Pa555");
        adminSoapProv.checkPasswordStrength(acct, "2ndDelTA4Pa555");
        adminSoapProv.setPassword(acct, "2ndDelTA4Pa555");

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        try {
            domAdminSoapProv.changePassword(acct, "DelTA4Pa555", TestUtil.DEFAULT_PASSWORD);
            fail("changePassword succeeded when shouldn't");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "authentication failed for");
        }
        try {
            domAdminSoapProv.checkPasswordStrength(acct, "2ndDelTA4Pa555");
            fail("checkPasswordStrength succeeded in spite of not having checkPasswordStrength right!!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: need right: checkPasswordStrength for account");
        }
        domAdminSoapProv.setPassword(acct, TestUtil.DEFAULT_PASSWORD);
    }

    /**
     * Test that delegated admin with adminConsoleDLACLTabRights can grant sendToDistList right
     * @throws Exception
     */
    @Test
    public void testDelegatedAdminAssignSendToDistList() throws Exception {
        createAdminConsoleStyleDomainAdmin(DOMADMIN);
        adminSoapProv.createAccount(TARGET_ACCT, TestUtil.DEFAULT_PASSWORD, null);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        CreateDistributionListResponse caResp;
        caResp = domAdminSoapProv.invokeJaxb(new CreateDistributionListRequest(TARGET_DL));
        assertNotNull("CreateDistributionListResponse for " + TARGET_DL + " simple as domAdmin", caResp);
        caResp = adminSoapProv.invokeJaxb(new CreateDistributionListRequest(DIFF_DL));
        assertNotNull("CreateDistributionListResponse for " + TARGET_DL + " simple as domAdmin", caResp);
        failToGrantRight(domAdminSoapProv, TargetType.dl, TARGET_DL, TARGET_ACCT, RightConsts.RT_sendToDistList,
            "permission denied: insufficient right to grant 'sendToDistList' right");
        grantRight(adminSoapProv, TargetType.domain, ADMINISTERED_DOMAIN, DOMADMIN /* grantee */,
                RightConsts.RT_adminConsoleDLACLTabRights);
        grantRight(domAdminSoapProv, TargetType.dl, TARGET_DL, TARGET_ACCT, RightConsts.RT_sendToDistList);
        /* Check that doesn't allow it for a dl in a different domain */
        failToGrantRight(domAdminSoapProv, TargetType.dl, DIFF_DL, TARGET_ACCT, RightConsts.RT_sendToDistList,
            "permission denied: insufficient right to grant 'sendToDistList' right");
    }

    /**
     * Test that delegated admin with adminConsoleAccountsACLTabRights can grant sendAs right
     * @throws Exception
     */
    @Test
    public void testDelegatedAdminAssignSendAs() throws Exception {
        createAdminConsoleStyleDomainAdmin(DOMADMIN);
        adminSoapProv.createAccount(TARGET_ACCT, TestUtil.DEFAULT_PASSWORD, null);
        adminSoapProv.createAccount(TARGET_ACCT2, TestUtil.DEFAULT_PASSWORD, null);
        adminSoapProv.createAccount(DIFF_ACCT, TestUtil.DEFAULT_PASSWORD, null);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        failToGrantRight(domAdminSoapProv, TargetType.account, TARGET_ACCT2, TARGET_ACCT, RightConsts.RT_sendAs,
            "permission denied: insufficient right to grant 'sendAs' right");
        grantRight(adminSoapProv, TargetType.domain, ADMINISTERED_DOMAIN, DOMADMIN /* grantee */,
                RightConsts.RT_adminConsoleAccountsACLTabRights);
        grantRight(domAdminSoapProv, TargetType.account, TARGET_ACCT2, TARGET_ACCT, RightConsts.RT_sendAs);
        /* Check that doesn't allow it for an account in a different domain */
        failToGrantRight(domAdminSoapProv, TargetType.account, DIFF_ACCT, TARGET_ACCT, RightConsts.RT_sendAs,
            "permission denied: insufficient right to grant 'sendAs' right");
    }

    /**
     * Test that delegated admin with adminConsoleDLACLTabRights can grant sendToDistList right
     * @throws Exception
     */
    @Test
    public void testViaGroupDelegatedAdminAssignSendToDistList() throws Exception {
        String domAdminGroupId = createAdminConsoleStyleDomainAdminGroup(DOMADMINGROUP);
        CreateAccountResponse caResp;
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE"));
        CreateAccountRequest caReq = new CreateAccountRequest(DOMADMIN, TestUtil.DEFAULT_PASSWORD, attrs);
        caResp = adminSoapProv.invokeJaxb(caReq);
        assertNotNull("CreateAccountResponse for " + DOMADMIN + " Admin", caResp);
        AddDistributionListMemberResponse adlmResp;
        adlmResp = adminSoapProv.invokeJaxb(
                new AddDistributionListMemberRequest(domAdminGroupId, Lists.newArrayList(DOMADMIN)));
        assertNotNull("AddDistributionListMemberResponse for " + DOMADMIN + " Admin", adlmResp);
        adminSoapProv.createAccount(TARGET_ACCT, TestUtil.DEFAULT_PASSWORD, null);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        CreateDistributionListResponse cdlResp;
        cdlResp = domAdminSoapProv.invokeJaxb(new CreateDistributionListRequest(TARGET_DL));
        assertNotNull("CreateDistributionListResponse for " + TARGET_DL + " simple as domAdmin", cdlResp);
        cdlResp = adminSoapProv.invokeJaxb(new CreateDistributionListRequest(DIFF_DL));
        assertNotNull("CreateDistributionListResponse for " + TARGET_DL + " simple as domAdmin", cdlResp);
        failToGrantRight(domAdminSoapProv, TargetType.dl, TARGET_DL, TARGET_ACCT, RightConsts.RT_sendToDistList,
            "permission denied: insufficient right to grant 'sendToDistList' right");
        grantRight(adminSoapProv, TargetType.domain, ADMINISTERED_DOMAIN, GranteeType.grp, DOMADMINGROUP,
                RightConsts.RT_adminConsoleDLACLTabRights);
        grantRight(domAdminSoapProv, TargetType.dl, TARGET_DL, TARGET_ACCT, RightConsts.RT_sendToDistList);
        /* Check that doesn't allow it for a dl in a different domain */
        failToGrantRight(domAdminSoapProv, TargetType.dl, DIFF_DL, TARGET_ACCT, RightConsts.RT_sendToDistList,
            "permission denied: insufficient right to grant 'sendToDistList' right");
    }


    @Test
    public void testGetAccountInDomAdminDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        CreateAccountResponse caResp;
        caResp = domAdminSoapProv.invokeJaxb(new CreateAccountRequest(TARGET_ACCT, TestUtil.DEFAULT_PASSWORD));
        assertNotNull("CreateAccountResponse for " + TARGET_ACCT + " simple as domAdmin", caResp);
        String acctId = caResp.getAccount().getId();

        GetAccountRequest getAcctReq = new GetAccountRequest(AccountSelector.fromName(TARGET_ACCT), true /*applyCos*/);
        GetAccountResponse getAcctResp = domAdminSoapProv.invokeJaxb(getAcctReq);
        assertNotNull("GetAccountResponse for " + TARGET_ACCT + " simple as domAdmin", getAcctResp);
        getAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(getAcctReq, acctId);
        assertNotNull("GetAccountResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", getAcctResp);

        GetMailboxRequest gmReq = new GetMailboxRequest(new MailboxByAccountIdSelector(acctId));
        GetMailboxResponse gmResp = domAdminSoapProv.invokeJaxb(gmReq);
        assertNotNull("GetMailboxResponse for " + TARGET_ACCT + " simple as domAdmin", gmResp);
        gmResp = domAdminSoapProv.invokeJaxbOnTargetAccount(gmReq, acctId);
        assertNotNull("GetMailboxResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", gmResp);

        AddAccountAliasResponse aaaResp;
        aaaResp = domAdminSoapProv.invokeJaxb( new AddAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT, false));
        assertNotNull("AddAccountAliasResponse for " + TARGET_ACCT + " simple as domAdmin", aaaResp);
        aaaResp = domAdminSoapProv.invokeJaxbOnTargetAccount(
                new AddAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT2, false), acctId);
        assertNotNull("AddAccountAliasResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", aaaResp);

        RemoveAccountAliasResponse daaResp;
        daaResp = domAdminSoapProv.invokeJaxb(new RemoveAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT));
        assertNotNull("RemoveAccountAliasResponse for " + TARGET_ACCT + " simple as domAdmin", daaResp);
        daaResp = domAdminSoapProv.invokeJaxbOnTargetAccount(
                new RemoveAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT2), acctId);
        assertNotNull("RemoveAccountAliasResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", daaResp);

        RenameAccountResponse renAResp;
        renAResp = domAdminSoapProv.invokeJaxb(new RenameAccountRequest(acctId, TARGET_ACCT_RENAMED));
        assertNotNull("RenameAccountResponse for " + TARGET_ACCT + " simple as domAdmin", renAResp);

        renAResp = domAdminSoapProv.invokeJaxb(new RenameAccountRequest(acctId, TARGET_ACCT));
        assertNotNull("RenameAccountResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", renAResp);

        DeleteAccountRequest delAcctReq;
        DeleteAccountResponse delAcctResp;
        delAcctReq = new DeleteAccountRequest(null);
        try {
            delAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
            fail("DeleteAccountRequest succeeded in spite of having no 'id' specified!!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "invalid request: missing required attribute: id");
        }
        delAcctReq = new DeleteAccountRequest(acctId);
        delAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
        assertNotNull("DeleteAccountResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", delAcctResp);
        try {
            getAcctResp = domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "no such account: ");
        }
        try {
            getAcctResp = domAdminSoapProv.invokeJaxb(
                    new GetAccountRequest(AccountSelector.fromId(acctId), true /*applyCos*/));
            fail("GetAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");  // because by id not name
        }
        try {
            getAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(getAcctReq, acctId);
            fail("GetAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        try {
            gmResp = domAdminSoapProv.invokeJaxb(gmReq);
            fail("GetMailboxRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account "); // because by id
        }
        try {
            gmResp = domAdminSoapProv.invokeJaxbOnTargetAccount(gmReq, acctId);
            fail("GetMailboxRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
    }

    @Test
    public void testGetAccountInDiffDomain() throws Exception {
        Account acct = TestJaxbProvisioning.ensureAccountExists(DIFF_ACCT);
        String acctId = acct.getId();
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        GetAccountRequest getAcctReq = new GetAccountRequest(AccountSelector.fromName(DIFF_ACCT), true /*applyCos*/);
        try {
            domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetAccountRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(getAcctReq, acctId);
            fail("GetAccountRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        // try non-existent acct
        getAcctReq = new GetAccountRequest(AccountSelector.fromName(DIFF_ACCT2), true /*applyCos*/);
        try {
            domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetAccountRequest succeeded for non-existent account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        GetMailboxRequest gmReq = new GetMailboxRequest(new MailboxByAccountIdSelector(acctId));
        try {
            domAdminSoapProv.invokeJaxb(gmReq);
            fail("GetMailboxRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(gmReq, acctId);
            fail("GetMailboxRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        AddAccountAliasResponse aaaResp;
        try {
            aaaResp = domAdminSoapProv.invokeJaxb(new AddAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT, false));
            fail("AddAccountAliasRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        try {
            aaaResp = domAdminSoapProv.invokeJaxbOnTargetAccount(
                    new AddAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT2, false), acctId);
            fail("AddAccountAliasRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        aaaResp = adminSoapProv.invokeJaxb(new AddAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT, false));
        assertNotNull("AddAccountAliasResponse for " + TARGET_ACCT + " as FULL ADMIN", aaaResp);

        try {
            domAdminSoapProv.invokeJaxb( new RemoveAccountAliasRequest(acctId, ALIAS_FOR_TARGET_ACCT));
            fail("RemoveAccountAliasRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        try {
            domAdminSoapProv.invokeJaxb(new RenameAccountRequest(acctId, TARGET_ACCT_RENAMED));
            fail("RenameAccountRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        try {
            domAdminSoapProv.invokeJaxb(new DeleteAccountRequest(acctId));
            fail("DeleteAccountRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
    }

    @Test
    public void testModifyAccountInDomAdminDomain() throws Exception {
        Account acct = TestJaxbProvisioning.ensureAccountExists(TARGET_ACCT);
        String acctId = acct.getId();
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        ModifyAccountRequest modAcctReq;
        ModifyAccountResponse modAcctResp;
        modAcctReq = new ModifyAccountRequest(null);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        try {
            modAcctResp = domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyAccountRequest succeeded without 'id'!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "invalid request: missing required attribute: id");
        }

        modAcctReq = new ModifyAccountRequest(acctId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        modAcctResp = domAdminSoapProv.invokeJaxb(modAcctReq);
        assertNotNull("ModifyAccountResponse for " + TARGET_ACCT + " simple as domAdmin", modAcctResp);
        modAcctReq = new ModifyAccountRequest(acctId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "another dummy description"));
        modAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
        assertNotNull("ModifyAccountResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", modAcctResp);
        DeleteAccountRequest delAcctReq = new DeleteAccountRequest(acctId);
        DeleteAccountResponse delAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
        assertNotNull("DeleteAccountResponse for " + TARGET_ACCT + " as domAdmin specifying target acct", delAcctResp);
        try {
            modAcctResp = domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Get this instead of "no such account: " because modify by ID (not name) and for domain admin
            // cannot know whether that ID was for a domain we administered or not.
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        try {
            modAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
            fail("ModifyAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }

        // as full admin
        try {
            modAcctResp = adminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Full admin gets "no such account: " instead of "permission denied"
            checkSoapReason(sfe, "no such account: ");
        }
    }

    @Test
    public void testModifyAccountInDiffDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        Account acct = TestJaxbProvisioning.ensureAccountExists(DIFF_ACCT);
        String acctId = acct.getId();

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        ModifyAccountRequest modAcctReq = new ModifyAccountRequest(acctId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        try {
            domAdminSoapProv.invokeJaxb(modAcctReq);
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        DeleteAccountRequest delAcctReq = new DeleteAccountRequest(acctId);
        DeleteAccountResponse delAcctResp = adminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
        assertNotNull("DeleteAccountResponse for " + DIFF_ACCT + " as ADMIN specifying target acct",
                delAcctResp);
        try {
            domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Get this instead of "no such account: " because modify by ID (not name) and for domain admin
            // cannot know whether that ID was for a domain we administered or not.
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
            fail("ModifyAccountRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
    }

    @Test
    public void testGetCalendarResourceInDomAdminDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_displayName, "testGetCalendarResourceInDomAdminDomain Room 101"));
        attrs.add(new Attr(Provisioning.A_description, "Room 101 for 50 seats"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResType, "Location"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoAcceptDecline, "TRUE"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoDeclineIfBusy, "TRUE"));
        CreateCalendarResourceResponse caResp;
        caResp = domAdminSoapProv.invokeJaxb(
                new CreateCalendarResourceRequest(TARGET_CALRES, TestUtil.DEFAULT_PASSWORD, attrs));
        assertNotNull("CreateCalendarResourceResponse for " + TARGET_CALRES + " simple as domAdmin", caResp);
        String acctId = caResp.getCalResource().getId();

        GetCalendarResourceResponse getAcctResp = domAdminSoapProv.invokeJaxb(
                new GetCalendarResourceRequest(CalendarResourceSelector.fromName(TARGET_CALRES), true /*applyCos*/));
        assertNotNull("GetCalendarResourceResponse for " + TARGET_CALRES + " simple as domAdmin", getAcctResp);
        getAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(
                new GetCalendarResourceRequest(CalendarResourceSelector.fromName(TARGET_CALRES), true /*applyCos*/),
                acctId);
        assertNotNull("GetCalendarResourceResponse for " + TARGET_CALRES + " as domAdmin specifying target acct",
                getAcctResp);

        RenameCalendarResourceResponse renAResp;
        renAResp = domAdminSoapProv.invokeJaxb(new RenameCalendarResourceRequest(acctId, TARGET_CALRES_RENAMED));
        assertNotNull("RenameCalendarResourceResponse for " + TARGET_CALRES + " simple as domAdmin", renAResp);

        renAResp = domAdminSoapProv.invokeJaxb(new RenameCalendarResourceRequest(acctId, TARGET_CALRES));
        assertNotNull("RenameCalendarResourceResponse for " + TARGET_CALRES + " as domAdmin specifying target acct",
                renAResp);

        DeleteCalendarResourceRequest delAcctReq;
        DeleteCalendarResourceResponse delAcctResp;
        delAcctReq = new DeleteCalendarResourceRequest(null);
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
            fail("DeleteCalendarResouceRequest succeeded in spite of having no 'id' specified!!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "invalid request: missing required attribute: id");
        }
        delAcctReq = new DeleteCalendarResourceRequest(acctId);
        delAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
        assertNotNull("DeleteCalendarResourceResponse for " + TARGET_CALRES + " as domAdmin specifying target acct",
                delAcctResp);
        try {
            getAcctResp = domAdminSoapProv.invokeJaxb(
                new GetCalendarResourceRequest(CalendarResourceSelector.fromName(TARGET_CALRES), true /*applyCos*/));
            fail("GetCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "no such calendar resource: ");
        }
        try {
            getAcctResp = domAdminSoapProv.invokeJaxb(
                    new GetCalendarResourceRequest(CalendarResourceSelector.fromId(acctId), true /*applyCos*/));
            fail("GetCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");  // because by id not name
        }
        try {
            getAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(
                new GetCalendarResourceRequest(CalendarResourceSelector.fromName(TARGET_CALRES), true /*applyCos*/),
                acctId);
            fail("GetCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
    }

    @Test
    public void testGetCalendarResourceInDiffDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_displayName, "testGetCalendarResourceInDiffDomain Room 101"));
        attrs.add(new Attr(Provisioning.A_description, "Room 101 for 50 seats"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResType, "Location"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoAcceptDecline, "TRUE"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoDeclineIfBusy, "TRUE"));
        CreateCalendarResourceResponse caResp;
        try {
            caResp = domAdminSoapProv.invokeJaxb(
                    new CreateCalendarResourceRequest(DIFF_CALRES, TestUtil.DEFAULT_PASSWORD, attrs));
            fail("CreateCalendarResourceRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: need right: createCalendarResource for domain");
        }
        caResp = adminSoapProv.invokeJaxb(
                new CreateCalendarResourceRequest(DIFF_CALRES, TestUtil.DEFAULT_PASSWORD, attrs));
        assertNotNull("CreateCalendarResourceResponse for " + DIFF_CALRES + " simple as domAdmin", caResp);
        String acctId = caResp.getCalResource().getId();

        GetCalendarResourceRequest getAcctReq =
                new GetCalendarResourceRequest(CalendarResourceSelector.fromName(DIFF_CALRES), true /*applyCos*/);
        try {
            domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetCalendarResourceRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(getAcctReq, acctId);
            fail("GetCalendarResourceRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }

        // try non-existent
        getAcctReq = new GetCalendarResourceRequest(CalendarResourceSelector.fromName(DIFF_CALRES2), true /*applyCos*/);
        try {
            domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetCalendarResourceRequest succeeded for non-existent account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }

        try {
            domAdminSoapProv.invokeJaxb(new RenameCalendarResourceRequest(acctId, TARGET_CALRES_RENAMED));
            fail("RenameCalendarResourceRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }

        try {
            domAdminSoapProv.invokeJaxb(new DeleteCalendarResourceRequest(acctId));
            fail("DeleteCalendarResourceRequest succeeded for account in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }
    }

    @Test
    public void testModifyCalendarResourceInDomAdminDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_displayName, "testModifyCalendarResourceInDomAdminDomain Room 101"));
        attrs.add(new Attr(Provisioning.A_description, "Room 101 for 50 seats"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResType, "Location"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoAcceptDecline, "TRUE"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoDeclineIfBusy, "TRUE"));
        CreateCalendarResourceResponse caResp;
        caResp = domAdminSoapProv.invokeJaxb(
                new CreateCalendarResourceRequest(TARGET_CALRES, TestUtil.DEFAULT_PASSWORD, attrs));
        assertNotNull("CreateCalendarResourceResponse for " + TARGET_CALRES + " simple as domAdmin", caResp);
        String acctId = caResp.getCalResource().getId();

        ModifyCalendarResourceRequest modAcctReq;
        ModifyCalendarResourceResponse modAcctResp;
        modAcctReq = new ModifyCalendarResourceRequest(null);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        try {
            modAcctResp = domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyCalendarResourceRequest succeeded without 'id'!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "invalid request: missing required attribute: id");
        }
        modAcctReq = new ModifyCalendarResourceRequest(acctId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        modAcctResp = domAdminSoapProv.invokeJaxb(modAcctReq);
        assertNotNull("ModifyCalendarResourceResponse for " + TARGET_CALRES2 + " simple as domAdmin", modAcctResp);
        modAcctReq = new ModifyCalendarResourceRequest(acctId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "another dummy description"));
        modAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
        assertNotNull("ModifyCalendarResourceResponse for " + TARGET_CALRES2 + " as domAdmin specifying target acct",
                modAcctResp);
        DeleteCalendarResourceRequest delAcctReq = new DeleteCalendarResourceRequest(acctId);
        DeleteCalendarResourceResponse delAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
        assertNotNull("DeleteCalendarResourceResponse for " + TARGET_CALRES2 + " as domAdmin specifying target acct",
                delAcctResp);
        try {
            modAcctResp = domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Get this instead of "no such calendar resource: " because modify by ID (not name) and for domain admin
            // cannot know whether that ID was for a domain we administered or not.
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }
        try {
            modAcctResp = domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
            fail("ModifyCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account");
        }

        // as full admin
        try {
            modAcctResp = adminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Full admin gets "no such calendar resource: " instead of "permission denied"
            checkSoapReason(sfe, "no such calendar resource: ");
        }
    }

    @Test
    public void testModifyCalendarResourceInDiffDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_displayName, "testModifyCalendarResourceInDiffDomain Room 101"));
        attrs.add(new Attr(Provisioning.A_description, "Room 101 for 50 seats"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResType, "Location"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoAcceptDecline, "TRUE"));
        attrs.add(new Attr(Provisioning.A_zimbraCalResAutoDeclineIfBusy, "TRUE"));
        CreateCalendarResourceResponse caResp;
        caResp = adminSoapProv.invokeJaxb(
                new CreateCalendarResourceRequest(DIFF_CALRES, TestUtil.DEFAULT_PASSWORD, attrs));
        assertNotNull("CreateCalendarResourceResponse for " + DIFF_CALRES + " as FULL ADMIN", caResp);
        String acctId = caResp.getCalResource().getId();

        ModifyCalendarResourceRequest modAcctReq = new ModifyCalendarResourceRequest(acctId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        try {
            domAdminSoapProv.invokeJaxb(modAcctReq);
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }
        DeleteCalendarResourceRequest delAcctReq = new DeleteCalendarResourceRequest(acctId);
        DeleteCalendarResourceResponse delAcctResp = adminSoapProv.invokeJaxbOnTargetAccount(delAcctReq, acctId);
        assertNotNull("DeleteCalendarResourceResponse for " + DIFF_CALRES + " as ADMIN specifying target acct",
                delAcctResp);
        try {
            domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Get this instead of "no such account: " because modify by ID (not name) and for domain admin
            // cannot know whether that ID was for a domain we administered or not.
            checkSoapReason(sfe, "permission denied: can not access calendar resource ");
        }
        try {
            domAdminSoapProv.invokeJaxbOnTargetAccount(modAcctReq, acctId);
            fail("ModifyCalendarResourceRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access account ");
        }
    }

    @Test
    public void testGetDistributionListInDomAdminDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        CreateDistributionListResponse caResp;
        caResp = domAdminSoapProv.invokeJaxb(new CreateDistributionListRequest(TARGET_DL));
        assertNotNull("CreateDistributionListResponse for " + TARGET_DL + " simple as domAdmin", caResp);
        String dlId = caResp.getDl().getId();

        GetDistributionListRequest getDlReq =
                new GetDistributionListRequest(DistributionListSelector.fromName(TARGET_DL));
        GetDistributionListResponse getDlResp = domAdminSoapProv.invokeJaxb(getDlReq);
        assertNotNull("GetDistributionListResponse for " + TARGET_DL + " simple as domAdmin", getDlResp);

        AddDistributionListAliasResponse aaaResp;
        aaaResp = domAdminSoapProv.invokeJaxb( new AddDistributionListAliasRequest(dlId, ALIAS_FOR_TARGET_DL));
        assertNotNull("AddDistributionListAliasResponse for " + TARGET_DL + " simple as domAdmin", aaaResp);

        RemoveDistributionListAliasResponse daaResp;
        daaResp = domAdminSoapProv.invokeJaxb(new RemoveDistributionListAliasRequest(dlId, ALIAS_FOR_TARGET_DL));
        assertNotNull("RemoveDistributionListAliasResponse for " + TARGET_DL + " simple as domAdmin", daaResp);

        RenameDistributionListResponse renAResp;
        renAResp = domAdminSoapProv.invokeJaxb(new RenameDistributionListRequest(dlId, TARGET_DL_RENAMED));
        assertNotNull("RenameDistributionListResponse for " + TARGET_DL + " simple as domAdmin", renAResp);

        DeleteDistributionListRequest delDLReq;
        DeleteDistributionListResponse delDlResp;
        delDLReq = new DeleteDistributionListRequest(null);
        try {
            delDlResp = domAdminSoapProv.invokeJaxb(delDLReq);
            fail("DeleteDistributionListRequest succeeded in spite of having no 'id' specified!!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "invalid request: missing required attribute: id");
        }
        delDLReq = new DeleteDistributionListRequest(dlId);
        delDlResp = domAdminSoapProv.invokeJaxb(delDLReq);
        assertNotNull("DeleteDistributionListResponse for " + TARGET_DL + " as domAdmin", delDlResp);
        try {
            getDlResp = domAdminSoapProv.invokeJaxb(getDlReq);
            fail("GetDistributionListRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "no such distribution list:");
        }
        try {
            getDlResp = domAdminSoapProv.invokeJaxb(
                    new GetDistributionListRequest(DistributionListSelector.fromId(dlId)));
            fail("GetDistributionListRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");  // because by id not name
        }
    }

    @Test
    public void testGetDistributionListInDiffDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        CreateDistributionListResponse caResp;
        caResp = adminSoapProv.invokeJaxb(new CreateDistributionListRequest(DIFF_DL));
        assertNotNull("CreateDistributionListResponse for " + DIFF_DL + " as FULL ADMIN", caResp);
        String dlId = caResp.getDl().getId();

        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        GetDistributionListRequest getAcctReq =
                new GetDistributionListRequest(DistributionListSelector.fromName(DIFF_DL));
        try {
            domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetDistributionListRequest succeeded for DistributionList in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");
        }

        // try non-existent acct
        getAcctReq = new GetDistributionListRequest(DistributionListSelector.fromName(DIFF_DL2));
        try {
            domAdminSoapProv.invokeJaxb(getAcctReq);
            fail("GetDistributionListRequest succeeded for non-existent DistributionList in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");
        }

        AddDistributionListAliasResponse aaaResp;
        try {
            aaaResp = domAdminSoapProv.invokeJaxb(new AddDistributionListAliasRequest(dlId, ALIAS_FOR_TARGET_DL));
            fail("AddDistributionListAliasRequest succeeded for DistributionList in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");
        }

        aaaResp = adminSoapProv.invokeJaxb(new AddDistributionListAliasRequest(dlId, ALIAS_FOR_TARGET_DL));
        assertNotNull("AddDistributionListAliasResponse for " + TARGET_DL + " as FULL ADMIN", aaaResp);

        try {
            domAdminSoapProv.invokeJaxb( new RemoveDistributionListAliasRequest(dlId, ALIAS_FOR_TARGET_DL));
            fail("RemoveDistributionListAliasRequest succeeded for DistributionList in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");
        }

        try {
            domAdminSoapProv.invokeJaxb(new RenameDistributionListRequest(dlId, TARGET_DL_RENAMED));
            fail("RenameDistributionListRequest succeeded for DistributionList in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");
        }

        try {
            domAdminSoapProv.invokeJaxb(new DeleteDistributionListRequest(dlId));
            fail("DeleteDistributionListRequest succeeded for DistributionList in other domain!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list");
        }
    }

    @Test
    public void testModifyDistributionListInDomAdminDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_displayName, "testModifyDistributionListInDomAdminDomain"));
        CreateDistributionListResponse caResp;
        caResp = domAdminSoapProv.invokeJaxb(
                new CreateDistributionListRequest(TARGET_DL, attrs, false /* dynamic */));
        assertNotNull("CreateDistributionListResponse for " + TARGET_DL + " simple as domAdmin", caResp);
        String dlId = caResp.getDl().getId();

        ModifyDistributionListRequest modDlReq;
        ModifyDistributionListResponse modDlResp;
        modDlReq = new ModifyDistributionListRequest(null);
        modDlReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        try {
            modDlResp = domAdminSoapProv.invokeJaxb(modDlReq);
            fail("ModifyDistributionListRequest succeeded without specifying id!");
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "invalid request: missing required attribute: id");
        }
        modDlReq = new ModifyDistributionListRequest(dlId);
        modDlReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        modDlResp = domAdminSoapProv.invokeJaxb(modDlReq);
        assertNotNull("ModifyDistributionListResponse for " + TARGET_DL2 + " simple as domAdmin", modDlResp);
        DeleteDistributionListRequest delDlReq = new DeleteDistributionListRequest(dlId);
        DeleteDistributionListResponse delDlResp = domAdminSoapProv.invokeJaxb(delDlReq);
        assertNotNull("DeleteDistributionListResponse for " + TARGET_DL2 + " as domAdmin", delDlResp);
        try {
            modDlResp = domAdminSoapProv.invokeJaxb(modDlReq);
            fail("ModifyDistributionListRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Get this instead of "no such calendar resource: " because modify by ID (not name) and for domain admin
            // cannot know whether that ID was for a domain we administered or not.
            checkSoapReason(sfe, "permission denied: can not access distribution list ");
        }

        // as full admin
        try {
            modDlResp = adminSoapProv.invokeJaxb(modDlReq);
            fail("ModifyDistributionListRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Full admin gets "no such distribution list: " instead of "permission denied"
            checkSoapReason(sfe, "no such distribution list: ");
        }
    }

    @Test
    public void testModifyDistributionListInDiffDomain() throws Exception {
        String domAdminId = createAdminConsoleStyleDomainAdmin(DOMADMIN);
        SoapProvisioning domAdminSoapProv = getSoapProvisioning(DOMADMIN, TestUtil.DEFAULT_PASSWORD);
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr(Provisioning.A_displayName, "testGetDistributionListInDiffDomain Room 101"));
        CreateDistributionListResponse caResp;
        caResp = adminSoapProv.invokeJaxb(
                new CreateDistributionListRequest(DIFF_DL, attrs, false /* dynamic */));
        assertNotNull("CreateDistributionListResponse for " + DIFF_DL + " as FULL ADMIN", caResp);
        String dlId = caResp.getDl().getId();

        ModifyDistributionListRequest modAcctReq = new ModifyDistributionListRequest(dlId);
        modAcctReq.addAttr(new Attr(Provisioning.A_description, "dummy description"));
        try {
            domAdminSoapProv.invokeJaxb(modAcctReq);
        } catch (SoapFaultException sfe) {
            checkSoapReason(sfe, "permission denied: can not access distribution list ");
        }
        DeleteDistributionListRequest delAcctReq = new DeleteDistributionListRequest(dlId);
        DeleteDistributionListResponse delAcctResp = adminSoapProv.invokeJaxb(delAcctReq);
        assertNotNull("DeleteDistributionListResponse for " + DIFF_DL + " as ADMIN", delAcctResp);
        try {
            domAdminSoapProv.invokeJaxb(modAcctReq);
            fail("ModifyDistributionListRequest succeeded after delete!");
        } catch (SoapFaultException sfe) {
            // Get this instead of "no such distribution list" because modify by ID (not name) and for domain admin
            // cannot know whether that ID was for a domain we administered or not.
            checkSoapReason(sfe, "permission denied: can not access distribution list ");
        }
    }

    public static void main(String[] args)
            throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestDomainAdmin.class);
    }
}
