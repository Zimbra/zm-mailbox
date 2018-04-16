/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CountAccountResult;
import com.zimbra.cs.account.Provisioning.RightsDoc;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightClass;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.soap.SoapAccountInfo;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.MailboxInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.QuotaUsage;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.VerifyIndexResult;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.account.message.GetShareInfoRequest;
import com.zimbra.soap.account.message.GetShareInfoResponse;
import com.zimbra.soap.admin.message.CheckBlobConsistencyRequest;
import com.zimbra.soap.admin.message.CheckBlobConsistencyResponse;
import com.zimbra.soap.admin.message.CreateDistributionListRequest;
import com.zimbra.soap.admin.message.CreateDistributionListResponse;
import com.zimbra.soap.admin.message.GetLicenseInfoRequest;
import com.zimbra.soap.admin.message.GetLicenseInfoResponse;
import com.zimbra.soap.admin.message.GetServerNIfsRequest;
import com.zimbra.soap.admin.message.GetServerNIfsResponse;
import com.zimbra.soap.admin.message.GetVersionInfoRequest;
import com.zimbra.soap.admin.message.GetVersionInfoResponse;
import com.zimbra.soap.admin.message.ModifyCosRequest;
import com.zimbra.soap.admin.message.ModifyCosResponse;
import com.zimbra.soap.admin.message.RenameDistributionListRequest;
import com.zimbra.soap.admin.message.RenameDistributionListResponse;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.LicenseExpirationInfo;
import com.zimbra.soap.admin.type.MailboxBlobConsistency;
import com.zimbra.soap.admin.type.NetworkInformation;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.admin.type.VersionInfo;
import com.zimbra.soap.mail.message.CreateFolderRequest;
import com.zimbra.soap.mail.message.CreateFolderResponse;
import com.zimbra.soap.mail.message.CreateMountpointRequest;
import com.zimbra.soap.mail.message.CreateMountpointResponse;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.message.FolderActionResponse;
import com.zimbra.soap.mail.type.ActionGrantSelector;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.FolderActionResult;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.NewFolderSpec;
import com.zimbra.soap.mail.type.NewMountpointSpec;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.GranteeChooser;
import com.zimbra.soap.type.ShareInfo;
import com.zimbra.soap.type.TargetBy;

/**
 * Primary focus of these tests is to ensure that Jaxb objects work, in particular where SoapProvisioning uses them
 */
public class TestJaxbProvisioning {
    @Rule
    public TestName testName = new TestName();

    private SoapProvisioning prov = null;

    private String USER_NAME = null;
    private final static String domain1 = "jaxb.domain1";
    private final static String domain2 = "jaxb.domain2";
    private final static String domain3 = "jaxb.domain3";
    private final static String sharer = "sharer@" + domain1;
    private final static String sharee = "sharee@" + domain1;
    private final static String other = "other@" + domain1;
    private final static String dom2acct = "dom2acct@" + domain2;
    private final static String dom3acct = "dom3acct@" + domain3;

    private final static String testServer = "jaxb.server.example.test";
    private final static String testAcctEmail = "jaxb1@" + domain2;
    private final static String dom1acct = "dom1acct@" + domain2;
    private final static String testAcctAlias = "alias_4_jaxb1@" + domain2;
    private final static String testAcctIdentity = "identity_4_jaxb1@" + domain2;
    private final static String testNewAcctEmail = "new_jaxb1@" + domain2;
    private final static String testCalResDomain = "jaxb.calr.domain.example.test";
    private final static String testCalRes = "jaxb1@" + testCalResDomain;
    private final static String testNewCalRes = "new_jaxb1@" + testCalResDomain;
    private final static String testCalResDisplayName = "JAXB Test CalResource";
    private final static String testDlDomain = "jaxb.dl.domain.example.test";
    private final static String testDl = "jaxb_dl1@" + testDlDomain;
    private final static String parentDl = "jaxb_parentdl@" + testDlDomain;
    private final static String testDlAlias = "alias_4_jaxb_dl1@" + testDlDomain;
    private final static String testDlNewName = "new_jaxb_dl1@" + testDlDomain;
    private final static String testCosDomain = "jaxb.cos.domain.example.test";
    private final static String testCos = "jaxb_cos@" + testDlDomain;
    private final static String testCosCopy = "jaxb_cos_copy@" + testDlDomain;
    private final static String testNewCos = "new_jaxb_cos@" + testDlDomain;

    public void init() throws Exception {
        oneTimeTearDown();
    }

    public void oneTimeTearDown() {
        ZimbraLog.test.info("in TestJaxbProvisioning oneTimeTearDown");
    }

    @Before
    public void setUp() throws Exception {
        if (!TestUtil.fromRunUnitTests) {
            TestUtil.cliSetup();
        }
        prov = TestUtil.newSoapProvisioning();
        USER_NAME = testName.getMethodName() + "-user";
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        ZimbraLog.test.debug("in TestJaxbProvisioning tearDown");
        if (prov == null) {
            prov = TestUtil.newSoapProvisioning();
        }
        TestUtil.setLCValue(LC.public_share_advertising_scope, null);
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(testAcctEmail);
        TestUtil.deleteAccountIfExists(testNewAcctEmail);
        TestUtil.deleteAccountIfExists(sharer);
        TestUtil.deleteAccountIfExists(sharee);
        TestUtil.deleteAccountIfExists(other);
        TestUtil.deleteAccountIfExists(dom1acct);
        TestUtil.deleteAccountIfExists(dom2acct);
        TestUtil.deleteAccountIfExists(dom3acct);
        deleteCalendarResourceIfExists(testCalRes);
        deleteDlIfExists(testDl);
        deleteDlIfExists(parentDl);
        deleteDlIfExists(testDlNewName);
        deleteCosIfExists(testCos);
        deleteCosIfExists(testNewCos);
        deleteCosIfExists(testCosCopy);
        deleteDomainIfExists(domain1);
        deleteDomainIfExists(testCalResDomain);
        deleteDomainIfExists(testDlDomain);
        deleteDomainIfExists(testCosDomain);
        deleteDomainIfExists(domain2);
        deleteDomainIfExists(domain3);
        deleteServerIfExists(testServer);
    }

    private String testName() {
        return testName.getMethodName();
    }

    public static void deleteDomainIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting domain %s", name);
            Provisioning prov = TestUtil.newSoapProvisioning();
            Domain res = prov.get(Key.DomainBy.name, name);
            if (res != null) {
                prov.deleteDomain(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting domain %s", name, ex);
        }
    }

    public static void deleteServerIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting server %s", name);
            Provisioning prov = TestUtil.newSoapProvisioning();
            Server res = prov.get(Key.ServerBy.name, name);
            if (res != null) {
                prov.deleteServer(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting server %s", name, ex);
        }
    }

    public static void deleteAccountIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting Account %s", name);
            Provisioning prov = TestUtil.newSoapProvisioning();
            Account acc = prov.get(Key.AccountBy.name, name);
            if (acc != null) {
                prov.deleteAccount(acc.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Account %s", name, ex);
        }
    }

    public static void deleteCalendarResourceIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting CalendarResource %s", name);
            Provisioning prov = TestUtil.newSoapProvisioning();
            CalendarResource res = prov.get(Key.CalendarResourceBy.name, name);
            if (res != null) {
                prov.deleteCalendarResource(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Calendar Resource %s", name, ex);
        }
    }

    public static void deleteDlIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting DL %s", name);
            Provisioning prov = TestUtil.newSoapProvisioning();
            DistributionList res = prov.get(Key.DistributionListBy.name, name);
            if (res != null) {
                prov.deleteDistributionList(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Distribution List %s", name, ex);
        }
    }

    public static void deleteCosIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting COS %s", name);
            Provisioning prov = TestUtil.newSoapProvisioning();
            Cos res = prov.get(Key.CosBy.name, name);
            if (res != null)
                prov.deleteCos(res.getId());
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Cos %s", name, ex);
        }
    }

    public static Domain ensureDomainExists(String name) throws ServiceException {
        Provisioning prov = TestUtil.newSoapProvisioning();
        Domain dom = prov.get(Key.DomainBy.name, name);
        if (dom == null) {
            ZimbraLog.test.debug("ensureDomainExists didn't exist - creating new domain=%s", name);
            dom = prov.createDomain(name, null);
        }
        if (dom == null) {
            ZimbraLog.test.debug("ensureDomainExists returning null!!!");
        } else {
            ZimbraLog.test.debug("ensureDomainExists Returning=%s Id=%s", dom.getName(), dom.getId());
        }
        return dom;
    }

    public static Account ensureAccountExists(String name) throws ServiceException {
        String domainName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(domainName);
        Provisioning prov = TestUtil.newSoapProvisioning();
        Account acct = prov.get(AccountBy.name, name);
        if (acct == null)
            acct = TestUtil.createAccount(name);
        if (acct == null) {
            ZimbraLog.test.debug("ensureAccountExists returning null!!!");
        } else {
            ZimbraLog.test.debug("ensureAccountExists Returning Account=%s Id=%s", acct.getName(), acct.getId());
        }
        return acct;
    }

    public static Account ensureMailboxExists(String name) throws ServiceException {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account acct = ensureAccountExists(name);
        if (acct == null) {
            ZimbraLog.test.debug("ensureMailboxExists returning null!!!");
        } else {
            // The act of getting a mailbox is sufficient to create it if the associated account exists.
            // Note that prov.getAccount() USED TO implicitly created a mailbox even though it was not really
            // supposed to and this routine used to rely on that.
            MailboxInfo mboxInfo = prov.getMailbox(acct);
            ZimbraLog.test.debug("ensureMailboxExists Returning Mailbox=%s Account=%s Id=%s", mboxInfo.getMailboxId(),
                    acct.getName(), acct.getId());
        }
        return acct;
    }

    public static DistributionList ensureDlExists(String name) throws ServiceException {
        Provisioning prov = TestUtil.newSoapProvisioning();
        String domainName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(domainName);
        DistributionList dl = prov.get(Key.DistributionListBy.name, name);
        if (dl == null) {
            dl = prov.createDistributionList(name, null);
        }
        return dl;
    }

    public static Cos ensureCosExists(String name) throws ServiceException {
        Provisioning prov = TestUtil.newSoapProvisioning();
        String domainName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(domainName);
        Cos cos = prov.get(Key.CosBy.name, name);
        if (cos == null) {
            cos = prov.createCos(name, null);
        }
        return cos;
    }

    @Test
    public void testGetConfig() throws Exception {
        ZimbraLog.test.debug("Starting test %s", testName());
        Config cfg = prov.getConfig();
        assertNotNull("Config", cfg);
        cfg = prov.getConfig("zimbra_user");
        assertNotNull("Config", cfg);
    }

    @Test
    public void testServer() throws Exception {
        ZimbraLog.test.debug("Starting test %s", testName());
        Domain dom = ensureDomainExists(testServer);
        assertNotNull("Domain for " + domain1, dom);
        Server svr = prov.createServer(testServer, null);
        assertNotNull("Server for " + testServer, svr);
        svr = prov.get(Key.ServerBy.id, svr.getId());
        List<Server> svrs = prov.getAllServers();
        assertNotNull("All Servers", svrs);
        assertTrue("Number of Servers objects=" + svrs.size() + " should be >=1", svrs.size() >= 1);
        prov.deleteServer(svr.getId());
    }

    @Test
    public void testAccount() throws Exception {
        ZimbraLog.test.debug("Starting testAccount");
        Domain dom = ensureDomainExists(domain2);
        assertNotNull("Domain for " + domain2, dom);
        Account acct = prov.createAccount(testAcctEmail, TestUtil.DEFAULT_PASSWORD, null);
        prov.authAccount(acct, TestUtil.DEFAULT_PASSWORD, AuthContext.Protocol.test);
        assertNotNull("Account for " + testAcctEmail, acct);
        prov.changePassword(acct, TestUtil.DEFAULT_PASSWORD, "DelTA4Pa555");
        prov.checkPasswordStrength(acct, "2ndDelTA4Pa555");
        prov.setPassword(acct, "2ndDelTA4Pa555");
        prov.renameAccount(acct.getId(), testNewAcctEmail);
        prov.addAlias(acct, testAcctAlias);
        prov.removeAlias(acct, testAcctAlias);
        acct = prov.get(AccountBy.name, testNewAcctEmail);
        assertNotNull("Account for " + testNewAcctEmail, acct);
        SoapAccountInfo acctInfo = prov.getAccountInfo(AccountBy.id, acct.getId());
        assertNotNull("SoapAccountInfo for " + testNewAcctEmail, acctInfo);
        acct = prov.get(AccountBy.name, testNewAcctEmail, true);
        assertNotNull("2nd Account for " + testNewAcctEmail, acct);
        List<Account> adminAccts = prov.getAllAdminAccounts(true);
        assertNotNull("Admin Accounts", adminAccts);
        assertTrue("Number of Admin Account objects=" + adminAccts.size() + " should be >=1", adminAccts.size() >= 1);
        List<Account> accts = prov.getAllAccounts(dom);
        assertNotNull("All Accounts", accts);
        assertTrue("Number of Account objects=" + accts.size() + " should be >=1", accts.size() >= 1);
        List<Domain> domains = prov.getAllDomains(false);
        assertNotNull("All Domains", domains);
        assertTrue("Number of Domain objects=" + domains.size() + " should be >=1", domains.size() >= 1);
        dom = prov.get(Key.DomainBy.id, dom.getId());
        assertNotNull("Domain got by id", dom);
        CountAccountResult res = prov.countAccount(dom);
        assertNotNull("CountAccountResult", res);
        dom = prov.getDomainInfo(Key.DomainBy.id, dom.getId());
        assertNotNull("DomainInfo got by id", dom);

        prov.deleteAccount(acct.getId());
    }

    public static class CutDownShareInfo {
        String folderPath;
        String ownerEmail;
        String granteeType;

        public CutDownShareInfo(String folderPath, String ownerEmail, String granteeType) {
            this.folderPath = folderPath;
            this.ownerEmail = ownerEmail;
            this.granteeType = granteeType;
        }

        public CutDownShareInfo(ShareInfo sInfo) {
            this.folderPath = sInfo.getFolderPath();
            this.ownerEmail = sInfo.getOwnerEmail();
            this.granteeType = sInfo.getGranteeType();
        }

        public boolean matches(ShareInfo shareInfo) {
            return folderPath.equals(shareInfo.getFolderPath()) && ownerEmail.equals(shareInfo.getOwnerEmail())
                    && granteeType.equals(shareInfo.getGranteeType());
        }

        @Override
        public String toString() {
            return String.format("path=%s ownerEmail=%s granteeType=%s", folderPath, ownerEmail, granteeType);
        }
    }

    public static class ShareSet {
        Set<CutDownShareInfo> shares = Sets.newHashSet();

        public ShareSet() {
        }

        public ShareSet(CutDownShareInfo share) {
            add(share);
        }

        public ShareSet(Set<CutDownShareInfo> shares) {
            addAll(shares);
        }

        public ShareSet add(CutDownShareInfo share) {
            this.shares.add(share);
            return this;
        }

        public ShareSet addAll(Set<CutDownShareInfo> shars) {
            this.shares.addAll(shars);
            return this;
        }

        public ShareSet addAll(List<ShareInfo> shars) {
            for (ShareInfo sInfo : shars) {
                this.shares.add(new CutDownShareInfo(sInfo));
            }
            return this;
        }
    }

    public CutDownShareInfo setupShare(Account sharerAccount, String sharerFolderName, byte color, String granteeType,
            Account shareeAccount, Account mountingAccount, String mountFolderName) {
        setupShare(sharerAccount, sharerFolderName, color, "appointment", granteeType, "r", shareeAccount,
                mountingAccount, mountFolderName);
        return new CutDownShareInfo("/" + sharerFolderName, sharerAccount.getName(), granteeType);
    }

    public void setupShare(Account sharerAccount, String sharerFolderName, byte color, String defaultView,
            String granteeType, String perm, Account shareeAccount, Account mountingAccount, String mountFolderName) {
        ShareInfo fred = new ShareInfo();
        fred.getGranteeType();
        String theSharee = (shareeAccount != null) ? shareeAccount.getName() : null;
        String folderId = createFolder(sharerAccount, sharerFolderName, Byte.valueOf(color), defaultView);
        grantAccess(sharerAccount, folderId, granteeType, theSharee, perm);
        if (mountingAccount != null) {
            createMountpoint(mountingAccount, mountFolderName, defaultView, sharerAccount.getId(), folderId);
        }
    }

    /**
     * Setup: no public shares in DomA; UserM@DomZ has a public share(s) 1. UserA@DomA w/GetShareInfoRequest
     * (unqualified) 2. UserA@DomA w/GetShareInfoRequest owner by=name UserL@DomZ - valid owner - *no* public shares 3.
     * UserA@DomA w/GetShareInfoRequest owner by=name UserM@DomZ - valid owner - with public shares ONLY 4. UserA@DomA
     * w/GetShareInfoRequest owner by=name UserN@DomZ - invalid owner (non-existent account) 5. UserA@DomA
     * w/GetShareInfoRequest owner by=name UserB@DomA - valid owner - *no* public shares (yet)
     *
     * Setup: UserB@DomA creates a public share(s)... - Rerun Tests 1-5
     *
     * => I believe responses for 1-5 (both rounds) should be ~identical if public_share_advertising_scope=none and
     * there are no explicit shares to UserA@DomA
     *
     * # sanity tests on explicit sharing...
     *
     * 6. UserC@DomA w/GetShareInfoRequest owner by=name UserO@DomZ - valid owner - with ONLY an *explicit* (non-public)
     * share with UserB@DomA 7. UserC@DomA w/GetShareInfoRequest owner by=name UserP@DomB - valid owner - with public
     * shares AND an *explicit* (non-public) share with UserB@DomA
     *
     * => I believe responses for 7-8 should be ~identical (different owners of course) if
     * public_share_advertising_scope=none and there are no explicit shares to UserC@DomA
     *
     * - for completeness, we should also repeat 2-7 with by=id, although the likelihood of harvesting using by=id is
     * probably quite a bit lower than by=name.
     *
     * I don't know how this aligns or overlaps with existing test cases but hopefully we end up with a similar set of
     * test cases for each of none|all|samePrimaryDomain as makes sense.
     */
    @Test
    public void testVisibilityNonePublicSharesConsistentlyIgnored() throws Exception {
        ZimbraLog.test.debug("Starting test %s", testName());

        ensureDomainExists(domain1); // DomA (sameDom) "jaxb.domain.test"
        ensureDomainExists(domain2); // DomZ (diffDom) "jaxb.acct.domain.example.test"
        ensureDomainExists(domain3); // DomB
        Cos cos = prov.createCos(testCos, null);
        assertNotNull("Cos for " + testCos, cos);
        Account sameDomAcct = ensureMailboxExists(sharer); // UserB@DomA
        Account shareeAcct = ensureMailboxExists(sharee); // UserA@DomA
        Account diffDomAcct = ensureMailboxExists(testAcctEmail); // UserM@DomZ
        Account diffDomAcct2 = ensureMailboxExists(testNewAcctEmail); // UserL@DomZ
        Account sameDomAcct3 = ensureMailboxExists(dom1acct); // UserC@DomA
        Account diffDomAcct3 = ensureMailboxExists(dom2acct); // UserO@DomZ
        Account dom3acct1 = ensureMailboxExists(dom3acct); // UserP@DomB new_jaxb1@jaxb.server.example.test
        // anticipate that LC.public_share_advertising_scope config will be done by account/cos/domain in the future.
        shareeAcct.setCOSId(cos.getId());
        TestUtil.setLCValue(LC.public_share_advertising_scope, LC.PUBLIC_SHARE_VISIBILITY.none.toString());
        CutDownShareInfo pubDiffDom = setupShare(diffDomAcct, "PUB_DiffDomain", (byte) 5, "pub", null, null, null);
        visibilityNoneRepeats(shareeAcct, diffDomAcct, diffDomAcct2, sameDomAcct);
        CutDownShareInfo pubSameDom = setupShare(sameDomAcct, "PUB_SameDomain", (byte) 5, "pub", null, null, null);
        visibilityNoneRepeats(shareeAcct, diffDomAcct, diffDomAcct2, sameDomAcct);

        setupShare(diffDomAcct3, "USR_DiffDomain", (byte) 3, "usr", sameDomAcct, null, null);
        accountGetShareInfo(sameDomAcct3, AccountSelector.fromName(diffDomAcct3.getName()), false, new ShareSet());
        accountGetShareInfo(sameDomAcct3, AccountSelector.fromId(diffDomAcct3.getId()), false, new ShareSet());
        setupShare(dom3acct1, "PUB_DiffDomain2", (byte) 3, "pub", null, null, null);
        setupShare(dom3acct1, "USR_DiffDomain2", (byte) 3, "usr", sameDomAcct, null, null);
        accountGetShareInfo(sameDomAcct3, AccountSelector.fromName(diffDomAcct3.getName()), false, new ShareSet());
        accountGetShareInfo(sameDomAcct3, AccountSelector.fromId(diffDomAcct3.getId()), false, new ShareSet());
        visibilityNoneRepeats(shareeAcct, diffDomAcct, diffDomAcct2, sameDomAcct);
    }

    private void visibilityNoneRepeats(Account shareeAcct, Account diffDomAcct, Account diffDomAcct2,
            Account sameDomAcct) {
        // unqualified
        accountGetShareInfo(shareeAcct, null /* owner account */, false, new ShareSet());
        // valid owner, diff domain - no public shares
        accountGetShareInfo(shareeAcct, AccountSelector.fromName(diffDomAcct2.getName()), false, new ShareSet());
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(diffDomAcct2.getId()), false, new ShareSet());
        // valid owner, diff domain - with public shares only
        accountGetShareInfo(shareeAcct, AccountSelector.fromName(diffDomAcct.getName()), false, new ShareSet());
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(diffDomAcct.getId()), false, new ShareSet());
        // invalid owner, diff domain - non-existent account
        accountGetShareInfo(shareeAcct, AccountSelector.fromName("nonexistent@" + domain1), false, new ShareSet());
        // valid owner, same domain - no public shares 1st time , some the 2nd time
        accountGetShareInfo(shareeAcct, AccountSelector.fromName(sameDomAcct.getName()), false, new ShareSet());
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(sameDomAcct.getId()), false, new ShareSet());
    }

    @Test
    public void testPublicSharesVisibility() throws Exception {
        ZimbraLog.test.debug("Starting test %s", testName());
        ensureDomainExists(domain1);
        ensureDomainExists(domain2);
        Cos cos = prov.createCos(testCos, null);
        assertNotNull("Cos for " + testCos, cos);
        Account sameDomAcct = TestUtil.createAccount(sharer);
        Account shareeAcct = ensureMailboxExists(sharee);
        Account otherAcct = ensureMailboxExists(other);
        Account diffDomAcct = TestUtil.createAccount(testAcctEmail);
        // anticipate that LC.public_share_advertising_scope config will be done by account/cos/domain in the future.
        shareeAcct.setCOSId(cos.getId());
        TestUtil.setLCValue(LC.public_share_advertising_scope, LC.PUBLIC_SHARE_VISIBILITY.all.toString());
        GetShareInfoResponse gsiResp = accountGetShareInfo(shareeAcct, null /* owner account */, false, null);
        // Remember how many baseline public shares there are before we start.
        ShareSet baselinePublicShares = new ShareSet().addAll(gsiResp.getShares());

        CutDownShareInfo mountedUsrSameDom = setupShare(sameDomAcct, "USER_Mounted", (byte) 2, "usr", shareeAcct,
                shareeAcct, "USR_SAME_DOM");
        CutDownShareInfo notMountedUsrSameDom = setupShare(sameDomAcct, "USER_NOT_Mounted", (byte) 3, "usr",
                shareeAcct, null, null);
        CutDownShareInfo mountedPubSameDom = setupShare(sameDomAcct, "PUB_Mounted", (byte) 4, "pub", null, shareeAcct,
                "PUB_SAME_DOM");
        CutDownShareInfo notMountedPubSameDom = setupShare(sameDomAcct, "PUB_NOT_Mounted", (byte) 5, "pub", null, null,
                null);
        /* never visible to sharee */
        CutDownShareInfo usrUnrelatedSameDom = setupShare(sameDomAcct, "USER_Unrelated", (byte) 2, "usr", otherAcct,
                null, null);

        CutDownShareInfo mountedUsrDiffDom = setupShare(diffDomAcct, "USER_MountedDiffDom", (byte) 2, "usr",
                shareeAcct, shareeAcct, "USR_DIFF_DOM");
        CutDownShareInfo notMountedUsrDiffDom = setupShare(diffDomAcct, "USER_NOT_MountedDiffDom", (byte) 3, "usr",
                shareeAcct, null, null);
        CutDownShareInfo mountedPubDiffDom = setupShare(diffDomAcct, "PUB_MountedDiffDom", (byte) 4, "pub", null,
                shareeAcct, "PUB_DIFF_DOM");
        CutDownShareInfo notMountedPubDiffDom = setupShare(diffDomAcct, "PUB_NOT_MountedDiffDom", (byte) 5, "pub",
                null, null, null);
        /* never visible to sharee */
        CutDownShareInfo usrUnrelatedDiffDom = setupShare(diffDomAcct, "USER_UnrelatedDiffDom", (byte) 2, "usr",
                otherAcct, null, null);

        ShareSet allMounted = new ShareSet(mountedUsrSameDom).add(mountedPubSameDom).add(mountedUsrDiffDom)
                .add(mountedPubDiffDom);
        ShareSet allSharedSameDom = new ShareSet(mountedUsrSameDom).add(mountedPubSameDom).add(notMountedUsrSameDom)
                .add(notMountedPubSameDom);
        ShareSet allSharedDiffDom = new ShareSet(mountedUsrDiffDom).add(mountedPubDiffDom).add(notMountedUsrDiffDom)
                .add(notMountedPubDiffDom);
        ShareSet allShares = new ShareSet(allSharedSameDom.shares).addAll(allSharedDiffDom.shares);

        TestUtil.setLCValue(LC.public_share_advertising_scope, LC.PUBLIC_SHARE_VISIBILITY.samePrimaryDomain.toString());
        prov.flushCache(CacheEntryType.all, null);
        accountGetShareInfo(
                shareeAcct,
                null /* owner account */,
                false,
                new ShareSet(allMounted.shares).add(notMountedUsrSameDom).add(notMountedPubSameDom)
                        .add(notMountedUsrDiffDom));
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(sameDomAcct.getId()), false, new ShareSet(
                mountedUsrSameDom).add(mountedPubSameDom).add(notMountedPubSameDom).add(notMountedUsrSameDom));
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(diffDomAcct.getId()), false, new ShareSet(
                mountedUsrDiffDom).add(mountedPubDiffDom).add(notMountedUsrDiffDom));
        ShareSet adminSet = new ShareSet(allSharedSameDom.shares).add(usrUnrelatedSameDom);
        adminGetShareInfo(sameDomAcct, adminSet);

        TestUtil.setLCValue(LC.public_share_advertising_scope, LC.PUBLIC_SHARE_VISIBILITY.all.toString());
        prov.flushCache(CacheEntryType.account, null);
        accountGetShareInfo(shareeAcct, null /* owner account */, false,
                new ShareSet(baselinePublicShares.shares).addAll(allShares.shares));
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(sameDomAcct.getId()), false, allSharedSameDom);
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(diffDomAcct.getId()), false, allSharedDiffDom);
        adminGetShareInfo(sameDomAcct, adminSet);

        TestUtil.setLCValue(LC.public_share_advertising_scope, LC.PUBLIC_SHARE_VISIBILITY.none.toString());
        prov.flushCache(CacheEntryType.account, null);
        accountGetShareInfo(shareeAcct, null /* owner account */, false,
                new ShareSet(allMounted.shares).add(notMountedUsrSameDom).add(notMountedUsrDiffDom));
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(sameDomAcct.getId()), false, new ShareSet(
                mountedUsrSameDom).add(mountedPubSameDom).add(notMountedUsrSameDom));
        accountGetShareInfo(shareeAcct, AccountSelector.fromId(diffDomAcct.getId()), false, new ShareSet(
                mountedUsrDiffDom).add(mountedPubDiffDom).add(notMountedUsrDiffDom));
        adminGetShareInfo(sameDomAcct, adminSet);
        // Confirm that can't use GetShareInfoRequest with self as owner. If could, may want to alter things
        // so that public shares are always included in results if owner==self...
        accountGetShareInfoExpectFail(shareeAcct, AccountSelector.fromId(shareeAcct.getId()), true,
                "invalid request: cannot discover shares on self");
    }

    public void modifyCos(Cos cos, String attr, String value) throws ServiceException {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(attr, value);
        ModifyCosRequest modCosReq = new ModifyCosRequest();
        modCosReq.setId(cos.getId());
        modCosReq.setAttrs(attrs);
        ModifyCosResponse modCosResp = prov.invokeJaxb(modCosReq);
        assertNotNull(String.format("ModifyCosResponse object when setting %s to %s", attr, value), modCosResp);
    }

    public void checkGetShareInfo(ShareSet expected, List<ShareInfo> shares) {
        if (expected != null) {
            assertEquals("Number of shares", expected.shares.size(), shares.size());
            for (CutDownShareInfo share : expected.shares) {
                boolean found = false;
                for (ShareInfo aShare : shares) {
                    if (share.matches(aShare)) {
                        found = true;
                        break;
                    }
                }
                assertTrue(String.format("no match for '%s' in: %s", share, shares), found);
            }
        }
    }

    public com.zimbra.soap.admin.message.GetShareInfoResponse adminGetShareInfo(Account acct, ShareSet shareSet) {
        com.zimbra.soap.admin.message.GetShareInfoRequest req = new com.zimbra.soap.admin.message.GetShareInfoRequest(
                AccountSelector.fromId(acct.getId()));
        com.zimbra.soap.admin.message.GetShareInfoResponse resp = null;
        try {
            resp = prov.invokeJaxb(req);
            assertNotNull(String.format("GetShareInfoRequest for account %s", acct.getName()), resp);
            checkGetShareInfo(shareSet, resp.getShares());
        } catch (ServiceException e) {
            fail("Unexpected exception while creating mountpoint " + e);
        }
        return resp;
    }

    public void accountGetShareInfoExpectFail(Account acct, AccountSelector owner, Boolean includeSelf, String reason) {
        GranteeChooser grantee = null;
        try {
            prov.invokeJaxbOnTargetAccount(GetShareInfoRequest.create(owner, grantee, includeSelf), acct.getId());
            fail("Unexpected success for GetShareInfoRequest");
        } catch (ServiceException e) {
            assertTrue(String.format("Unexpected exception %s (expected msg '%s')", e.getMessage(), reason), e
                    .getMessage().contains(reason));
        }
    }

    public GetShareInfoResponse accountGetShareInfo(Account acct, AccountSelector owner, Boolean includeSelf,
            ShareSet shareSet) {
        GetShareInfoResponse resp = null;
        GranteeChooser grantee = null;
        try {
            resp = prov
                    .invokeJaxbOnTargetAccount(GetShareInfoRequest.create(owner, grantee, includeSelf), acct.getId());
            assertNotNull(String.format("GetShareInfoRequest for account %s", acct.getName()), resp);
            checkGetShareInfo(shareSet, resp.getShares());
        } catch (ServiceException e) {
            fail("Unexpected exception while creating mountpoint " + e);
        }
        return resp;
    }

    public void createMountpoint(Account acct, String localFolderName, String defaultView, String remoteZimbraId,
            String remoteFolderId) {
        NewMountpointSpec folderSpec = new NewMountpointSpec(localFolderName);
        folderSpec.setFolderId("1");
        folderSpec.setDefaultView(defaultView);
        folderSpec.setOwnerId(remoteZimbraId);
        folderSpec.setRemoteId(Integer.parseInt(remoteFolderId.substring(remoteFolderId.indexOf(':') + 1)));
        CreateMountpointRequest req = new CreateMountpointRequest(folderSpec);
        try {
            CreateMountpointResponse resp = prov.invokeJaxbOnTargetAccount(req, acct.getId());
            assertNotNull(
                    String.format("CreateMountpointResponse for account %s folder %s", acct.getName(), localFolderName),
                    resp);
        } catch (ServiceException e) {
            fail("Unexpected exception while creating mountpoint " + e);
        }
    }

    public void grantAccess(Account acct, String id, String granteeType, String granteeName, String perm) {
        FolderActionSelector selector = new FolderActionSelector(id, "grant");
        ActionGrantSelector grant = new ActionGrantSelector(perm, granteeType);
        grant.setDisplayName(granteeName);
        selector.setGrant(grant);
        FolderActionRequest req = new FolderActionRequest(selector);
        try {
            FolderActionResponse resp = prov.invokeJaxbOnTargetAccount(req, acct.getId());
            assertNotNull(String.format("FolderActionResponse for account %s", acct.getName()), resp);
        } catch (ServiceException e) {
            fail("Unexpected exception while granting access " + e);
        }
    }

    public String createFolder(Account acct, String name, Byte color, String defaultView) {
        NewFolderSpec newFolderSpec = new NewFolderSpec(name);
        newFolderSpec.setParentFolderId(Integer.toString(Mailbox.ID_FOLDER_USER_ROOT));
        newFolderSpec.setColor(color);
        newFolderSpec.setFlags("#");
        newFolderSpec.setDefaultView(defaultView);
        CreateFolderRequest createFolderReq = new CreateFolderRequest(newFolderSpec);
        CreateFolderResponse createFolderResp;
        try {
            createFolderResp = prov.invokeJaxbOnTargetAccount(createFolderReq, acct.getId());
            assertNotNull(String.format("CreateFolderResponse %s", newFolderSpec.getName()), createFolderResp);
            return createFolderResp.getFolder().getId();
        } catch (ServiceException e) {
            fail("Unexpected exception while creating folder" + e);
        }
        return null;
    }

    @Test
    public void testMailbox() throws Exception {
        ZimbraLog.test.debug("Starting test %s", testName());
        Domain dom = ensureDomainExists(domain2);
        assertNotNull("Domain for " + domain2, dom);
        Account acct = prov.createAccount(testAcctEmail, TestUtil.DEFAULT_PASSWORD, null);
        acct = ensureMailboxExists(testAcctEmail);
        assertNotNull("Account for " + testAcctEmail, acct);
        MailboxInfo mbx = prov.getMailbox(acct);
        assertNotNull("MailboxInfo for Account=" + testAcctEmail, mbx);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void testCos() throws Exception {
        ZimbraLog.test.debug("Starting testCos");
        Domain dom = ensureDomainExists(testCosDomain);
        assertNotNull("Domain for " + testCosDomain, dom);
        Cos cos = prov.createCos(testCos, null);
        assertNotNull("Cos for " + testCos, cos);
        prov.renameCos(cos.getId(), testNewCos);
        prov.copyCos(cos.getId(), testCosCopy);
        List<Cos> cosList = prov.getAllCos();
        assertNotNull("All Cos", cosList);
        assertTrue("Number of Cos objects=" + cosList.size() + " should be >=1", cosList.size() >= 1);
        prov.deleteCos(cos.getId());
        cos = prov.get(Key.CosBy.name, testCosCopy);
        prov.deleteCos(cos.getId());
    }

    @Test
    public void testDistributionList() throws Exception {
        ZimbraLog.test.debug("Starting testDistributionList");
        Domain dom = ensureDomainExists(testDlDomain);
        assertNotNull("Domain for " + testDlDomain, dom);
        deleteDlIfExists(testDl);
        deleteDlIfExists(parentDl);
        DistributionList dl = prov.createDistributionList(testDl, null);
        assertNotNull("DistributionList for " + testDl, dl);
        prov.renameDistributionList(dl.getId(), testDlNewName);
        prov.addAlias(dl, testDlAlias);
        dl = prov.get(Key.DistributionListBy.name, testDlAlias);
        prov.removeAlias(dl, testDlAlias);
        String[] members = { "one@example.com", "two@example.test", "three@example.net" };
        String[] rmMembers = { "two@example.test", "three@example.net" };
        prov.addMembers(dl, members);
        prov.removeMembers(dl, rmMembers);

        // DL Membership test
        DistributionList dadDl = prov.createDistributionList(parentDl, null);
        assertNotNull("DistributionList for " + parentDl, dadDl);
        String[] dlMembers = { "one@example.com", testDlNewName };
        prov.addMembers(dadDl, dlMembers);
        Map<String, String> via = Maps.newHashMap();
        List<DistributionList> containingDls = prov.getDistributionLists(dl, false, via);
        assertEquals("Number of DLs a DL is a member of", 1, containingDls.size());

        // Account Membership test
        Account acct = ensureMailboxExists(testAcctEmail);
        String[] dlAcctMembers = { testAcctEmail };
        prov.addMembers(dadDl, dlAcctMembers);
        containingDls = prov.getDistributionLists(acct, false, via);
        assertEquals("Number of DLs an acct is a member of", 1, containingDls.size());

        List<DistributionList> dls = prov.getAllDistributionLists(dom);
        assertNotNull("All DLs", dls);
        assertTrue("Number of DL objects=" + dls.size() + " should be >=2", dls.size() >= 2);
        prov.deleteDistributionList(dadDl.getId());
        prov.deleteDistributionList(dl.getId());
    }

    private void doRenameDynamicGroupTest(boolean isACLGroup, String displayName, String memberURL)
            throws ServiceException {
        List<Attr> attrs = Lists.newArrayList();
        attrs.add(new Attr("zimbraIsACLGroup", isACLGroup ? "TRUE" : "FALSE"));
        attrs.add(new Attr("zimbraMailStatus", "enabled"));
        attrs.add(new Attr("displayName", displayName));
        if (memberURL != null) {
            attrs.add(new Attr("memberURL", memberURL));
        }
        Domain dom = ensureDomainExists(testDlDomain);
        assertNotNull(String.format("Domain for %s", testDlDomain), dom);
        deleteDlIfExists(testDl);
        CreateDistributionListResponse cdlResp = prov
                .invokeJaxb(new CreateDistributionListRequest(testDl, attrs, true /* dynamic */));
        assertNotNull("CreateDistributionListResponse", cdlResp);
        RenameDistributionListResponse rdlResp = prov.invokeJaxb(new RenameDistributionListRequest(cdlResp.getDl()
                .getId(), testDlNewName));
        assertNotNull("RenameDistributionListResponse", rdlResp);
    }

    @Test
    public void testRenameStdDynamicGroup() throws ServiceException {
        // Note that if memberURL is not specified, zimbraIsACLGroup cannot be set to FALSE (currently)
        doRenameDynamicGroupTest(true, "Standard Dynamic ACLGroup", null);
    }

    @Test
    public void testRenameDynamicGroupWithMemberURL() throws ServiceException {
        doRenameDynamicGroupTest(false, "Dynamic With MemberURL non ACLGroup",
                "ldap:///??sub?(objectClass=zimbraAccount)");
    }

    @Test
    public void testRenameDynamicACLGroupWithMemberURL() throws ServiceException {
        doRenameDynamicGroupTest(true, "Dynamic With MemberURL ACLGroup", "ldap:///??sub?(objectClass=zimbraAccount)");
    }

    @Test
    public void testCalendarResource() throws Exception {
        ZimbraLog.test.debug("Starting testCalendarResource");
        deleteCalendarResourceIfExists(testCalRes);
        deleteDomainIfExists(testCalResDomain);
        Domain dom = prov.createDomain(testCalResDomain, null);
        assertNotNull("Domain for " + domain1, dom);
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("displayName", testCalResDisplayName);
        attrs.put("zimbraCalResType", "Location");
        attrs.put("zimbraCalResLocationDisplayName", "Harare");
        CalendarResource calRes = prov.createCalendarResource(testCalRes, TestUtil.DEFAULT_PASSWORD, attrs);
        assertNotNull("CalendarResource on create", calRes);
        prov.renameCalendarResource(calRes.getId(), testNewCalRes);
        List<CalendarResource> resources = prov.getAllCalendarResources(dom, Provisioning.getInstance()
                .getLocalServer());
        assertNotNull("CalendarResource List for getAll", resources);
        assertEquals("CalendarResource list size", 1, resources.size());
        calRes = prov.get(Key.CalendarResourceBy.id, calRes.getId());
        prov.deleteCalendarResource(calRes.getId());
    }

    @Test
    public void testQuotaUsage() throws Exception {
        ZimbraLog.test.debug("Starting testQuotaUsage");
        List<QuotaUsage> quotaUsages = prov.getQuotaUsage(Provisioning.getInstance().getLocalServer().getName());
        assertNotNull("QuotaUsage List", quotaUsages);
        assertTrue("Number of QuotaUsage objects=" + quotaUsages.size() + " should be >1", quotaUsages.size() > 1);
    }

    // Disabled - getting :
    // SoapFaultException: system failure: server
    // gren-elliots-macbook-pro.local zimbraRemoteManagementPrivateKeyPath
    // (/opt/zimbra/.ssh/zimbra_identity) does not exist
    public void DISABLED_testGetServerNIfs() throws Exception {
        ZimbraLog.test.debug("Starting testGetServerNIfs");
        Server svr = Provisioning.getInstance().getLocalServer();
        GetServerNIfsRequest req = new GetServerNIfsRequest(null, ServerSelector.fromId(svr.getId()));
        GetServerNIfsResponse resp = prov.invokeJaxb(req);
        assertNotNull("GetServerNIfsResponse", resp);
        List<NetworkInformation> nisList = resp.getNetworkInterfaces();
        assertNotNull("NetworkInfomation List", nisList);
    }

    @Test
    public void testLicenseInfo() throws Exception {
        ZimbraLog.test.debug("Starting testLicenseInfo");
        GetLicenseInfoRequest req = new GetLicenseInfoRequest();
        GetLicenseInfoResponse resp = prov.invokeJaxb(req);
        assertNotNull("GetLicensInfoResponse", resp);
        LicenseExpirationInfo expires = resp.getExpiration();
        assertNotNull("Expiration Info", expires);
        assertNotNull("getDate result", expires.getDate());
    }

    @Test
    public void testVersionInfo() throws Exception {
        ZimbraLog.test.debug("Starting testVersionInfo");
        GetVersionInfoRequest req = new GetVersionInfoRequest();
        GetVersionInfoResponse resp = prov.invokeJaxb(req);
        assertNotNull("GetLicensInfoResponse", resp);
        VersionInfo info = resp.getInfo();
        assertNotNull("VersionInfo", info);
        info.getType(); // Don't care whether null or not
        assertNotNull("getVersion result", info.getVersion());
        assertNotNull("getRelease result", info.getRelease());
        assertNotNull("getBuildDate result", info.getBuildDate());
        assertNotNull("getHost result", info.getHost());
        assertNotNull("getMajorVersion result", info.getMajorVersion());
        assertNotNull("getMinorVersion result", info.getMinorVersion());
        assertNotNull("getMicroVersion result", info.getMicroVersion());
        assertNotNull("getPlatform result", info.getPlatform());
        assertNotNull("getBuildDate result", info.getBuildDate());
    }

    @Test
    public void testIndex() throws Exception {
        ZimbraLog.test.debug("Starting testIndex");
        Account acct = ensureMailboxExists(testAcctEmail);
        ReIndexInfo info = prov.reIndex(acct, "start", null, null);
        assertNotNull("ReIndexInfo", info);
        assertNotNull("getStatus result", info.getStatus());
        // Progress can be null.
        // Progress prog = info.getProgress();
        VerifyIndexResult ndxRes = prov.verifyIndex(acct);
        assertNotNull("VerifyIndexResult", ndxRes);
        prov.deleteMailbox(acct.getId());
    }

    @Test
    public void testMboxCounts() throws Exception {
        ZimbraLog.test.debug("Starting testMboxCounts");
        Account acct = ensureMailboxExists(testAcctEmail);
        long quotaUsed = prov.recalculateMailboxCounts(acct);
        assertTrue("quota used=" + quotaUsed + " should be >= =", quotaUsed >= 0);
    }

    @Test
    public void testFlushCache() throws Exception {
        ZimbraLog.test.debug("Starting testFlushCache");
        ensureDomainExists(domain1);
        prov.flushCache(CacheEntryType.domain, null);
    }

    @Test
    public void testGetAllRights() throws Exception {
        ZimbraLog.test.debug("Starting testGetAllRights");
        List<Right> rights = prov
                .getAllRights("account" /* targetType */, true /* expandAllAttrs */, "USER" /* rightClass */);
        assertNotNull("getAllRight returned list", rights);
        assertTrue("Number of rights objects=" + rights.size() + " should be > 3", rights.size() > 3);
    }

    @Test
    public void testGetAllEffectiveRights() throws Exception {
        ZimbraLog.test.debug("Starting testGetAllEffectiveRights");
        AllEffectiveRights aer = prov
                .getAllEffectiveRights(null, null, null, false /* expandSetAttrs */, true /* expandGetAttrs */);
        assertNotNull("AllEffectiveRights", aer);
    }

    @Test
    public void testGetEffectiveRights() throws Exception {
        ZimbraLog.test.debug("Starting testGetEffectiveRights");
        EffectiveRights er = prov.getEffectiveRights("account" /* targetType */, TargetBy.name /* targetBy */,
                "admin" /* target */, GranteeBy.name /* granteeBy */, "admin" /* grantee */, true /* expandSetAttrs */,
                true /* expandGetAttrs */);
        assertNotNull("EffectiveRights", er);
    }

    @Test
    public void testGetRightsDoc() throws Exception {
        ZimbraLog.test.debug("Starting testGetRightsDoc");
        Map<String, List<RightsDoc>> map = prov.getRightsDoc(null);
        assertTrue("Map size=" + map.size() + " should be >= 1", map.size() >= 1);
        String[] pkgs = { "com.zimbra.cs.service.admin" };
        map = prov.getRightsDoc(pkgs);
        assertEquals("Map for specified set of pkgs", 1, map.size());
        boolean seenTstRight = false;
        for (String key : map.keySet()) {
            assertEquals("key to map", pkgs[0], key);
            for (RightsDoc rightsDoc : map.get(key)) {
                assertNotNull("rightsDoc cmd name", rightsDoc.getCmd());
                if (rightsDoc.getCmd().equals("AddAccountAliasRequest")) {
                    seenTstRight = true;
                    assertEquals("Notes number", 3, rightsDoc.getNotes().size());
                    assertEquals("Rights number", 3, rightsDoc.getRights().size());
                }
            }
        }
        assertTrue("AddAccountAliasRequest right in report", seenTstRight);
    }

    @Test
    public void testGetRight() throws Exception {
        ZimbraLog.test.debug("Starting testGetRight");
        Right right = prov.getRight("adminConsoleAccountRights", true);
        assertNotNull("Right", right);
        RightClass rightClass = right.getRightClass();
        assertEquals("right RightClass", rightClass, RightClass.ADMIN);
        assertEquals("right Name", "adminConsoleAccountRights", right.getName());
    }

    @Test
    public void testHealth() throws Exception {
        ZimbraLog.test.debug("Starting testHealth");
        assertTrue(prov.healthCheck());
    }

    @Test
    public void testIdentities() throws Exception {
        ZimbraLog.test.debug("Starting testIdentities");
        Account acct = ensureAccountExists(testAcctEmail);
        List<Identity> identities = prov.getAllIdentities(acct);
        assertEquals("Number of identities for new acct", 1, identities.size());
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("zimbraPrefFromAddress", testAcctIdentity);
        Identity newId = prov.createIdentity(acct, "altIdentity", attrs);
        assertNotNull("New identity", newId);
        identities = prov.getAllIdentities(acct);
        assertEquals("Number of identities after add", 2, identities.size());
        prov.deleteIdentity(acct, "altIdentity");
        identities = prov.getAllIdentities(acct);
        assertEquals("Number of identities after delete", 1, identities.size());
    }

    @Test
    public void folderActionTrash() throws Exception {
        Account acct = TestUtil.createAccount(USER_NAME);
        TestUtil.getZMailbox(USER_NAME);
        CreateFolderRequest cfReq = new CreateFolderRequest(new NewFolderSpec("trashMe"));
        String folderId = null;
        try {
            CreateFolderResponse resp = prov.invokeJaxbOnTargetAccount(cfReq, acct.getId());
            assertNotNull(String.format("CreateFolderResponse for %s account %s", cfReq, acct.getName()), resp);
            Folder folder = resp.getFolder();
            assertNotNull(String.format("CreateFolder Folder for %s account %s", cfReq, acct.getName()), folder);
            folderId = folder.getId();
        } catch (ServiceException e) {
            fail("Unexpected exception while creating folder" + e);
        }
        FolderActionRequest req = new FolderActionRequest(new FolderActionSelector(folderId, "trash"));
        try {
            FolderActionResponse resp = prov.invokeJaxbOnTargetAccount(req, acct.getId());
            assertNotNull(String.format("FolderActionResponse for req=%s account %s", req, acct.getName()), resp);
            FolderActionResult result = resp.getAction();
            assertNotNull(String.format("FolderActionResult for req=%s account %s", req, acct.getName()), result);
            assertEquals("Result folder ID", folderId, result.getId());
        } catch (ServiceException e) {
            fail("Unexpected exception while trashing folder" + e);
        }
    }

    @Test
    public void checkBlobConsistency() throws Exception {
        Account acct = ensureMailboxExists(testAcctEmail);
        assertNotNull("Account for " + testAcctEmail, acct);
        CheckBlobConsistencyRequest req = new CheckBlobConsistencyRequest();
        try {
            CheckBlobConsistencyResponse resp = prov.invokeJaxb(req);
            assertNotNull("CheckBlobConsistencyResponse", resp);
            List<MailboxBlobConsistency> mboxes = resp.getMailboxes();
            assertNotNull("mboxes in response", mboxes);
            assertNotNull("at least 1 mbox in response", mboxes.size() > 0);
        } catch (ServiceException e) {
            fail("Unexpected exception while checking blob consistency " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestJaxbProvisioning.class);
    }
}
