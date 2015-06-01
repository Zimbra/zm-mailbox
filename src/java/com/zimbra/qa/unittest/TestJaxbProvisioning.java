/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AlwaysOnCluster;
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
import com.zimbra.cs.account.ldap.LdapCache;
import com.zimbra.cs.account.soap.SoapAccountInfo;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.MailboxInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.QuotaUsage;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.VerifyIndexResult;
import com.zimbra.soap.admin.message.CreateAlwaysOnClusterRequest;
import com.zimbra.soap.admin.message.CreateAlwaysOnClusterResponse;
import com.zimbra.soap.admin.message.CreateServerRequest;
import com.zimbra.soap.admin.message.CreateServerResponse;
import com.zimbra.soap.admin.message.GetLicenseInfoRequest;
import com.zimbra.soap.admin.message.GetLicenseInfoResponse;
import com.zimbra.soap.admin.message.GetServerNIfsRequest;
import com.zimbra.soap.admin.message.GetServerNIfsResponse;
import com.zimbra.soap.admin.message.GetServerRequest;
import com.zimbra.soap.admin.message.GetServerResponse;
import com.zimbra.soap.admin.message.GetVersionInfoRequest;
import com.zimbra.soap.admin.message.GetVersionInfoResponse;
import com.zimbra.soap.admin.message.ModifyAlwaysOnClusterRequest;
import com.zimbra.soap.admin.message.ModifyAlwaysOnClusterResponse;
import com.zimbra.soap.admin.message.ModifyServerRequest;
import com.zimbra.soap.admin.message.ModifyServerResponse;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.LicenseExpirationInfo;
import com.zimbra.soap.admin.type.NetworkInformation;
import com.zimbra.soap.admin.type.ServerInfo;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.admin.type.VersionInfo;
import com.zimbra.soap.type.TargetBy;

/**
 * Primary focus of these tests is to ensure that Jaxb objects work, in
 * particular where SoapProvisioning uses them
 */
public class TestJaxbProvisioning {

    private SoapProvisioning prov = null;

    private final static String testAcctDomainName =
            "jaxb.acct.domain.example.test";
    private final static String testServer = "jaxb.server.example.test";
    private final static String testAcctEmail = "jaxb1@" + testAcctDomainName;
    private final static String testAcctAlias = "alias_4_jaxb1@" + testAcctDomainName;
    private final static String testAcctIdentity = "identity_4_jaxb1@" + testAcctDomainName;
    private final static String testNewAcctEmail = "new_jaxb1@" + testAcctDomainName;
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
    private final static String testAlwaysOnCluster = "testjaxbalwaysoncluster";

    public void init() throws Exception {
        oneTimeTearDown();
    }

    public void oneTimeTearDown() {
        ZimbraLog.test.info("in TestJaxbProvisioning oneTimeTearDown");
    }

    @Before
    public void setUp() throws Exception {
        prov = TestUtil.newSoapProvisioning();
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        ZimbraLog.test.debug("in TestJaxbProvisioning tearDown");
        if (prov == null)
            prov = TestUtil.newSoapProvisioning();
        TestUtil.deleteAccount(testAcctEmail);
        TestUtil.deleteAccount(testNewAcctEmail);
        deleteCalendarResourceIfExists(testCalRes);
        deleteDlIfExists(testDl);
        deleteDlIfExists(parentDl);
        deleteDlIfExists(testDlNewName);
        deleteCosIfExists(testCos);
        deleteCosIfExists(testNewCos);
        deleteCosIfExists(testCosCopy);
        deleteDomainIfExists(testAcctDomainName);
        deleteDomainIfExists(testCalResDomain);
        deleteDomainIfExists(testDlDomain);
        deleteDomainIfExists(testCosDomain);
        deleteServerIfExists(testServer);
        deleteAlwaysOnClusterIfExists(testAlwaysOnCluster);
    }

    public void deleteDomainIfExists(String name) {
        try {
            ZimbraLog.test.debug(
                    "Deleting domain " + name);
            Domain res = prov.get(Key.DomainBy.name, name);
            if (res != null)
                prov.deleteDomain(res.getId());
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting domain " + name, ex);
        }
    }

    public void deleteServerIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting server %s", name);
            Server res = prov.get(Key.ServerBy.name, name);
            if (res != null) {
                prov.deleteServer(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting server %s", name, ex);
        }
    }

    public void deleteAlwaysOnClusterIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting AlwaysOnCluster %s", name);
            AlwaysOnCluster res = prov.get(Key.AlwaysOnClusterBy.name, name);
            if (res != null) {
                prov.deleteAlwaysOnCluster(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting AlwaysOnCluster %s", name, ex);
        }
    }

    public void deleteCalendarResourceIfExists(String name) {
        try {
            ZimbraLog.test.debug(
                    "Deleting CalendarResource " + name);
            CalendarResource res = prov.get(Key.CalendarResourceBy.name, name);
            if (res != null)
                prov.deleteDomain(res.getId());
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Calendar Resource " +
                    name, ex);
        }
    }

    public void deleteDlIfExists(String name) {
        try {
            ZimbraLog.test.debug(
                    "Deleting DL " + name);
            DistributionList res = prov.get(Key.DistributionListBy.name, name);
            if (res != null)
                prov.deleteDistributionList(res.getId());
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Distribution List " + name, ex);
        }
    }

    public void deleteCosIfExists(String name) {
        try {
            ZimbraLog.test.debug("Deleting COS " + name);
            Cos res = prov.get(Key.CosBy.name, name);
            if (res != null)
                prov.deleteCos(res.getId());
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Cos " + name, ex);
        }
    }

    public Domain ensureDomainExists(String name)
    throws Exception {
        Domain dom = prov.get(Key.DomainBy.name, name);
        if (dom == null) {
            ZimbraLog.test.debug("ensureDomainExists didn't exist - creating new domain=" + name);
            dom = prov.createDomain(name, null);
        }
        if (dom == null) {
            ZimbraLog.test.debug("ensureDomainExists returning null!!!");
        } else {
            ZimbraLog.test.debug("ensureDomainExists Returning=" + dom.getName() + " Id=" + dom.getId());
        }
        return dom;
    }

    public Account ensureAccountExists(String name)
    throws Exception {
        String domainName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(domainName);
        Account acct = prov.get(AccountBy.name, name);
        if (acct == null)
            acct = TestUtil.createAccount(name);
        if (acct == null) {
            ZimbraLog.test.debug("ensureAccountExists returning null!!!");
        } else {
            ZimbraLog.test.debug("ensureAccountExists Returning Account=" + acct.getName() + " Id=" + acct.getId());
        }
        return acct;
    }

    public Account ensureMailboxExists(String name)
    throws Exception {
        Account acct = ensureAccountExists(name);
        if (acct == null) {
            ZimbraLog.test.debug("ensureMailboxExists returning null!!!");
        } else {
            // The act of getting a mailbox is sufficient to create it if the associated account exists.
            // Note that prov.getAccount() USED TO implicitly created a mailbox even though it was not really
            // supposed to and this routine used to rely on that.
            MailboxInfo mboxInfo = prov.getMailbox(acct);
            ZimbraLog.test.debug("ensureMailboxExists Returning Mailbox=" + mboxInfo.getMailboxId() +
                    " Account=" + acct.getName() + " Id=" + acct.getId());
        }
        return acct;
    }

    public DistributionList ensureDlExists(String name)
    throws Exception {
        String domainName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(domainName);
        DistributionList dl = prov.get(Key.DistributionListBy.name, name);
        if (dl == null)
            dl = prov.createDistributionList(name, null);
        return dl;
    }

    public Cos ensureCosExists(String name)
    throws Exception {
        String domainName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(domainName);
        Cos cos = prov.get(Key.CosBy.name, name);
        if (cos == null)
            cos = prov.createCos(name, null);
        return cos;
    }

    @Test
    public void getConfig() throws Exception {
        ZimbraLog.test.debug("Starting testConfig");
        Config cfg = prov.getConfig();
        Assert.assertNotNull("Config" , cfg);
        cfg = prov.getConfig("zimbra_user");
        Assert.assertNotNull("Config" , cfg);
    }

    @Test
    public void server() throws Exception {
        ZimbraLog.test.debug("Starting testServer");
        Domain dom = ensureDomainExists(testServer);
        Assert.assertNotNull("Domain for " + testAcctDomainName, dom);
        Server svr = prov.createServer(testServer, null);
        Assert.assertNotNull("Server for " + testServer, svr);
        svr = prov.get(Key.ServerBy.id, svr.getId());
        List <Server> svrs = prov.getAllServers();
        Assert.assertNotNull("All Servers" , svrs);
        Assert.assertTrue("Number of Servers objects=" + svrs.size() +
                " should be >=1", svrs.size() >= 1);
        prov.deleteServer(svr.getId());
   }

    /**
     * Check how serverPreferAlwaysOn affects attributes for a server.
     * At present using these 2 attributes:
     * attr id="1156" name="zimbraImapMaxConnections" type="integer" cardinality="single"
     *      optionalIn="globalConfig,server,alwaysOnCluster" flags="serverInherited,serverPreferAlwaysOn"
     * attr id="1834" name="zimbraReindexBatchSize" type="integer" cardinality="single"
     *      optionalIn="globalConfig,server,alwaysOnCluster" flags="serverInherited"
     * @throws ServiceException
     */
    @Test
    public void serverPreferAlwaysOn() throws ServiceException {
        // Create always on cluster with one attr that over-rides a server setting and one that doesn't
        CreateAlwaysOnClusterRequest createAlwaysOnClusterReq = new CreateAlwaysOnClusterRequest(testAlwaysOnCluster);
        createAlwaysOnClusterReq.addAttr(new Attr(Provisioning.A_zimbraImapMaxConnections, "171"));
        createAlwaysOnClusterReq.addAttr(new Attr(Provisioning.A_zimbraReindexBatchSize, "21"));
        CreateAlwaysOnClusterResponse createAlwaysOnClusterResp = prov.invokeJaxb(createAlwaysOnClusterReq);
        Assert.assertNotNull("CreateAlwaysOnClusterResponse" , createAlwaysOnClusterResp);
        String alwaysOnClusterId = createAlwaysOnClusterResp.getAlwaysOnCluster().getId();
        Assert.assertNotNull("AlwaysOnClusterId" , alwaysOnClusterId);

        // Create server with one attr that gets over-ridden by always on cluster setting and one that doesn't
        // Associate it with a cluster
        CreateServerRequest createServerReq = new CreateServerRequest(testServer);
        createServerReq.addAttr(new Attr(Provisioning.A_zimbraImapMaxConnections, "172"));
        createServerReq.addAttr(new Attr(Provisioning.A_zimbraReindexBatchSize, "22"));
        CreateServerResponse createServerResp = prov.invokeJaxb(createServerReq);
        Assert.assertNotNull("CreateServerResponse" , createServerResp);
        String serverId = createServerResp.getServer().getId();
        Assert.assertNotNull("ServerId" , serverId);
        ModifyServerRequest modifyServerReq = new ModifyServerRequest(serverId);
        modifyServerReq.addAttr(Provisioning.A_zimbraAlwaysOnClusterId, alwaysOnClusterId);
        ModifyServerResponse modifyServerResp = prov.invokeJaxb(modifyServerReq);
        Assert.assertNotNull("ModifyServerResponse" , modifyServerResp);
        checkServerAttrs(serverId, alwaysOnClusterId, "171", "22");

        // modify settings at the always on cluster level
        ModifyAlwaysOnClusterRequest modifyAlwaysOnClusterReq = new ModifyAlwaysOnClusterRequest(alwaysOnClusterId);
        modifyAlwaysOnClusterReq.addAttr(Provisioning.A_zimbraImapMaxConnections, "173");
        modifyAlwaysOnClusterReq.addAttr(Provisioning.A_zimbraReindexBatchSize, "23");
        ModifyAlwaysOnClusterResponse modifyAlwaysOnClusterResp = prov.invokeJaxb(modifyAlwaysOnClusterReq);
        Assert.assertNotNull("ModifyAlwaysOnClusterResponse" , modifyAlwaysOnClusterResp);
        List<Attr> aocAttrs = modifyAlwaysOnClusterResp.getAlwaysOnCluster().getAttrList();
        Assert.assertNotNull("Modified AlwaysOnCluster attrs" , aocAttrs);
        try {
            Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
        } catch (InterruptedException e) {
            ZimbraLog.test.info("Sleep interrupted", e);
        }
        checkServerAttrs(serverId, alwaysOnClusterId, "173", "22");

        // modify settings at the always on cluster level to be unset
        modifyAlwaysOnClusterReq = new ModifyAlwaysOnClusterRequest(alwaysOnClusterId);
        modifyAlwaysOnClusterReq.addAttr(Provisioning.A_zimbraImapMaxConnections, "");
        modifyAlwaysOnClusterReq.addAttr(Provisioning.A_zimbraReindexBatchSize, "");
        modifyAlwaysOnClusterResp = prov.invokeJaxb(modifyAlwaysOnClusterReq);
        Assert.assertNotNull("ModifyAlwaysOnClusterResponse" , modifyAlwaysOnClusterResp);
        aocAttrs = modifyAlwaysOnClusterResp.getAlwaysOnCluster().getAttrList();
        Assert.assertNotNull("Modified AlwaysOnCluster attrs" , aocAttrs);
        try {
            Thread.sleep(LdapCache.ldapCacheFreshnessCheckLimitMs() + 1);
        } catch (InterruptedException e) {
            ZimbraLog.test.info("Sleep interrupted", e);
        }
        checkServerAttrs(serverId, alwaysOnClusterId, "172", "22");

        prov.deleteServer(serverId);
        prov.deleteAlwaysOnCluster(alwaysOnClusterId);
    }

    private void checkServerAttrs(String serverId, String alwaysOnClusterId, String expectedImapMaxConnections,
            String expectedReindexBatchSize)
    throws ServiceException {
        // Check settings gotten via SOAP provisioning
        Server svr = prov.get(Key.ServerBy.id, serverId);
        String clusterIdForServer = svr.getAttr(Provisioning.A_zimbraAlwaysOnClusterId, "rubbish-default");
        Assert.assertEquals("ClusterId from server" , alwaysOnClusterId, clusterIdForServer);
        String imapMaxConnections = svr.getAttr(Provisioning.A_zimbraImapMaxConnections, "rubbish-default");
        Assert.assertEquals("imapMaxConnections from server" , expectedImapMaxConnections, imapMaxConnections);
        String reindexBatchSize = svr.getAttr(Provisioning.A_zimbraReindexBatchSize, "rubbish-default");
        Assert.assertEquals("reindexBatchSize from server" , expectedReindexBatchSize, reindexBatchSize);

        // Check settings gotten via JAXB objects and SOAP
        GetServerRequest getServerReq = new GetServerRequest(ServerSelector.fromId(serverId), true);
        getServerReq.addAttrs(Provisioning.A_zimbraAlwaysOnClusterId, Provisioning.A_zimbraImapMaxConnections,
                Provisioning.A_zimbraReindexBatchSize);
        GetServerResponse getServerResp = prov.invokeJaxb(getServerReq);
        Assert.assertNotNull("GetServerResponse" , getServerResp);
        ServerInfo serverInfo = getServerResp.getServer();
        Assert.assertNotNull("ServerInfo" , serverInfo);
        List<Attr> serverAttrs = serverInfo.getAttrList();
        Assert.assertNotNull("ServerAttrs" , serverAttrs);
        Assert.assertEquals("ServerAttrs size", 3, serverAttrs.size());
        for (Attr svrAttr : serverAttrs) {
            if (Provisioning.A_zimbraAlwaysOnClusterId.equals(svrAttr.getKey())) {
                Assert.assertEquals("ClusterId from SOAP get of server" , alwaysOnClusterId, svrAttr.getValue());
            } else if (Provisioning.A_zimbraImapMaxConnections.equals(svrAttr.getKey())) {
                Assert.assertEquals("imapMaxConnections from SOAP get of server",
                        expectedImapMaxConnections, svrAttr.getValue());
            } else if (Provisioning.A_zimbraReindexBatchSize.equals(svrAttr.getKey())) {
                Assert.assertEquals("reindexbatchsize from SOAP get of server",
                        expectedReindexBatchSize, svrAttr.getValue());
            }
        }
        // Check settings gotten via LDAP provisioning - Bug 97515 this used to return any setting at the server level
        // even if it should have been over-ridden by a value on the always on cluster
        Provisioning ldapProvisioning = Provisioning.getInstance();
        svr = ldapProvisioning.getServerByName(testServer);
        imapMaxConnections = svr.getAttr(Provisioning.A_zimbraImapMaxConnections, "rubbish-default");
        Assert.assertEquals("imapMaxConnections from LDAP get of server",
                expectedImapMaxConnections, imapMaxConnections);
        reindexBatchSize = svr.getAttr(Provisioning.A_zimbraReindexBatchSize, "rubbish-default");
        Assert.assertEquals("reindexBatchSize from LDAP get of server" , expectedReindexBatchSize, reindexBatchSize);
    }

    @Test
    public void account() throws Exception {
        ZimbraLog.test.debug("Starting testAccount");
        Domain dom = ensureDomainExists(testAcctDomainName);
        Assert.assertNotNull("Domain for " + testAcctDomainName, dom);
        Account acct = prov.createAccount(testAcctEmail,
                TestUtil.DEFAULT_PASSWORD, null);
        prov.authAccount(acct, TestUtil.DEFAULT_PASSWORD,
                AuthContext.Protocol.test);
        Assert.assertNotNull("Account for " + testAcctEmail, acct);
        prov.changePassword(acct, TestUtil.DEFAULT_PASSWORD, "DelTA4Pa555");
        prov.checkPasswordStrength(acct, "2ndDelTA4Pa555");
        prov.setPassword(acct, "2ndDelTA4Pa555");
        prov.renameAccount(acct.getId(), testNewAcctEmail);
        prov.addAlias(acct, testAcctAlias);
        prov.removeAlias(acct, testAcctAlias);
        acct = prov.get(AccountBy.name, testNewAcctEmail);
        Assert.assertNotNull("Account for " + testNewAcctEmail, acct);
        SoapAccountInfo acctInfo = prov.getAccountInfo(AccountBy.id, acct.getId());
        Assert.assertNotNull("SoapAccountInfo for " + testNewAcctEmail, acctInfo);
        acct = prov.get(AccountBy.name, testNewAcctEmail, true);
        Assert.assertNotNull("2nd Account for " + testNewAcctEmail, acct);
        List <Account> adminAccts = prov.getAllAdminAccounts(true);
        Assert.assertNotNull("Admin Accounts" , adminAccts);
        Assert.assertTrue("Number of Admin Account objects=" + adminAccts.size() +
                " should be >=1", adminAccts.size() >= 1);
        List <Account> accts = prov.getAllAccounts(dom);
        Assert.assertNotNull("All Accounts" , accts);
        Assert.assertTrue("Number of Account objects=" + accts.size() +
                " should be >=1", accts.size() >= 1);
        List <Domain> domains = prov.getAllDomains(false);
        Assert.assertNotNull("All Domains" , domains);
        Assert.assertTrue("Number of Domain objects=" + domains.size() +
                " should be >=1", domains.size() >= 1);
        dom = prov.get(Key.DomainBy.id, dom.getId());
        Assert.assertNotNull("Domain got by id" , dom);
        CountAccountResult res = prov.countAccount(dom);
        Assert.assertNotNull("CountAccountResult", res);
        dom = prov.getDomainInfo(Key.DomainBy.id, dom.getId());
        Assert.assertNotNull("DomainInfo got by id" , dom);

        prov.deleteAccount(acct.getId());
   }

    @Test
    public void mailbox() throws Exception {
        ZimbraLog.test.debug("Starting testMailbox");
        Domain dom = ensureDomainExists(testAcctDomainName);
        Assert.assertNotNull("Domain for " + testAcctDomainName, dom);
        Account acct = prov.createAccount(testAcctEmail,
                TestUtil.DEFAULT_PASSWORD, null);
        acct = ensureMailboxExists(testAcctEmail);
        Assert.assertNotNull("Account for " + testAcctEmail, acct);
        MailboxInfo mbx = prov.getMailbox(acct);
        Assert.assertNotNull("MailboxInfo for Account=" + testAcctEmail, mbx);
        prov.deleteAccount(acct.getId());
   }

    @Test
    public void cos() throws Exception {
        ZimbraLog.test.debug("Starting testCos");
        Domain dom = ensureDomainExists(testCosDomain);
        Assert.assertNotNull("Domain for " + testCosDomain, dom);
        Cos cos = prov.createCos(testCos, null);
        Assert.assertNotNull("Cos for " + testCos, cos);
        prov.renameCos(cos.getId(), testNewCos);
        prov.copyCos(cos.getId(), testCosCopy);
        List <Cos> cosList = prov.getAllCos();
        Assert.assertNotNull("All Cos" , cosList);
        Assert.assertTrue("Number of Cos objects=" + cosList.size() +
                " should be >=1", cosList.size() >= 1);
        prov.deleteCos(cos.getId());
        cos = prov.get(Key.CosBy.name, testCosCopy);
        prov.deleteCos(cos.getId());
   }

    @Test
    public void distributionList() throws Exception {
        ZimbraLog.test.debug("Starting distributionList");
        Domain dom = ensureDomainExists(testDlDomain);
        Assert.assertNotNull("Domain for " + testDlDomain, dom);
        deleteDlIfExists(testDl);
        deleteDlIfExists(parentDl);
        DistributionList dl = prov.createDistributionList(testDl, null);
        Assert.assertNotNull("DistributionList for " + testDl, dl);
        prov.renameDistributionList(dl.getId(), testDlNewName);
        prov.addAlias(dl, testDlAlias);
        dl = prov.get(Key.DistributionListBy.name, testDlAlias);
        prov.removeAlias(dl, testDlAlias);
        String[] members = { "one@example.com",
                "two@example.test", "three@example.net" };
        String[] rmMembers = { "two@example.test", "three@example.net" };
        prov.addMembers(dl, members);
        prov.removeMembers(dl, rmMembers);

        // DL Membership test
        DistributionList dadDl = prov.createDistributionList(parentDl, null);
        Assert.assertNotNull("DistributionList for " + parentDl, dadDl);
        String[] dlMembers = { "one@example.com", testDlNewName };
        prov.addMembers(dadDl, dlMembers);
        Map <String, String> via = Maps.newHashMap();
        List <DistributionList> containingDls =
            prov.getDistributionLists(dl, false, via);
        Assert.assertEquals("Number of DLs a DL is a member of", 1,
                containingDls.size());

        // Account Membership test
        Account acct = ensureMailboxExists(testAcctEmail);
        String[] dlAcctMembers = { testAcctEmail };
        prov.addMembers(dadDl, dlAcctMembers);
        containingDls =
            prov.getDistributionLists(acct, false, via);
        Assert.assertEquals("Number of DLs an acct is a member of", 1,
                containingDls.size());

        List <DistributionList> dls = prov.getAllDistributionLists(dom);
        Assert.assertNotNull("All DLs" , dls);
        Assert.assertTrue("Number of DL objects=" + dls.size() +
                " should be >=2", dls.size() >= 2);
        prov.deleteDistributionList(dadDl.getId());
        prov.deleteDistributionList(dl.getId());
   }

    @Test
    public void testCalendarResource() throws Exception {
        ZimbraLog.test.debug("Starting testCalendarResource");
        deleteCalendarResourceIfExists(testCalRes);
        deleteDomainIfExists(testCalResDomain);
        Domain dom = prov.createDomain(testCalResDomain, null);
        Assert.assertNotNull("Domain for " + testAcctDomainName, dom);
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("displayName", testCalResDisplayName);
        attrs.put ("zimbraCalResType", "Location");
        attrs.put("zimbraCalResLocationDisplayName", "Harare");
        CalendarResource calRes = prov.createCalendarResource(
                testCalRes, TestUtil.DEFAULT_PASSWORD, attrs);
        Assert.assertNotNull("CalendarResource on create", calRes);
        prov.renameCalendarResource(calRes.getId(), testNewCalRes);
        List <CalendarResource> resources = prov.getAllCalendarResources(
                dom, Provisioning.getInstance().getLocalServer());
        Assert.assertNotNull("CalendarResource List for getAll", resources);
        Assert.assertEquals("CalendarResource list size", 1, resources.size());
        calRes = prov.get(Key.CalendarResourceBy.id, calRes.getId());
        prov.deleteCalendarResource(calRes.getId());
    }

    @Test
    public void testQuotaUsage() throws Exception {
        ZimbraLog.test.debug("Starting testQuotaUsage");
        List <QuotaUsage> quotaUsages = prov.getQuotaUsage(
                Provisioning.getInstance().getLocalServer().getName());
        Assert.assertNotNull("QuotaUsage List", quotaUsages);
        Assert.assertTrue("Number of QuotaUsage objects=" + quotaUsages.size() +
                " should be >1", quotaUsages.size() > 1);
    }

    // Disabled - getting :
    // SoapFaultException: system failure: server
    //    gren-elliots-macbook-pro.local zimbraRemoteManagementPrivateKeyPath
    //    (/opt/zimbra/.ssh/zimbra_identity) does not exist
    public void DISABLED_testGetServerNIfs() throws Exception {
        ZimbraLog.test.debug("Starting testGetServerNIfs");
        Server svr = Provisioning.getInstance().getLocalServer();
        GetServerNIfsRequest req = new GetServerNIfsRequest(
                null, ServerSelector.fromId(svr.getId()));
        GetServerNIfsResponse resp = prov.invokeJaxb(req);
        Assert.assertNotNull("GetServerNIfsResponse", resp);
        List <NetworkInformation> nisList = resp.getNetworkInterfaces();
        Assert.assertNotNull("NetworkInfomation List", nisList);
    }

    @Test
    public void testLicenseInfo() throws Exception {
        ZimbraLog.test.debug("Starting testLicenseInfo");
        GetLicenseInfoRequest req = new GetLicenseInfoRequest();
        GetLicenseInfoResponse resp = prov.invokeJaxb(req);
        Assert.assertNotNull("GetLicensInfoResponse", resp);
        LicenseExpirationInfo expires = resp.getExpiration();
        Assert.assertNotNull("Expiration Info", expires);
        Assert.assertNotNull("getDate result", expires.getDate());
    }

    @Test
    public void testVersionInfo() throws Exception {
        ZimbraLog.test.debug("Starting testVersionInfo");
        GetVersionInfoRequest req = new GetVersionInfoRequest();
        GetVersionInfoResponse resp = prov.invokeJaxb(req);
        Assert.assertNotNull("GetLicensInfoResponse", resp);
        VersionInfo info = resp.getInfo();
        Assert.assertNotNull("VersionInfo", info);
        info.getType();  // Don't care whether null or not
        Assert.assertNotNull("getVersion result", info.getVersion());
        Assert.assertNotNull("getRelease result", info.getRelease());
        Assert.assertNotNull("getBuildDate result", info.getBuildDate());
        Assert.assertNotNull("getHost result", info.getHost());
        Assert.assertNotNull("getMajorVersion result", info.getMajorVersion());
        Assert.assertNotNull("getMinorVersion result", info.getMinorVersion());
        Assert.assertNotNull("getMicroVersion result", info.getMicroVersion());
        Assert.assertNotNull("getPlatform result", info.getPlatform());
        Assert.assertNotNull("getBuildDate result", info.getBuildDate());
    }

    @Test
    public void testIndex() throws Exception {
        ZimbraLog.test.debug("Starting testIndex");
        Account acct = ensureMailboxExists(testAcctEmail);
        ReIndexInfo info = prov.reIndex(acct, "start", null, null);
        Assert.assertNotNull("ReIndexInfo", info);
        Assert.assertNotNull("getStatus result", info.getStatus());
        // Progress can be null.
        // Progress prog = info.getProgress();
        VerifyIndexResult ndxRes = prov.verifyIndex(acct);
        Assert.assertNotNull("VerifyIndexResult", ndxRes);
        prov.deleteMailbox(acct.getId());
    }

    @Test
    public void testMboxCounts() throws Exception {
        ZimbraLog.test.debug("Starting testMboxCounts");
        Account acct = ensureMailboxExists(testAcctEmail);
        long quotaUsed = prov.recalculateMailboxCounts(acct);
        Assert.assertTrue("quota used=" + quotaUsed + " should be >= =", quotaUsed >= 0);
    }

    @Test
    public void testFlushCache() throws Exception {
        ZimbraLog.test.debug("Starting testFlushCache");
        ensureDomainExists(testAcctDomainName);
        prov.flushCache(CacheEntryType.domain, null);
    }

    @Test
    public void testGetAllRights() throws Exception {
        ZimbraLog.test.debug("Starting testGetAllRights");
        List<Right> rights = prov.getAllRights("account" /* targetType */,
                true /* expandAllAttrs */, "USER" /* rightClass */);
        Assert.assertNotNull("getAllRight returned list", rights);
        Assert.assertTrue("Number of rights objects=" + rights.size() +
                " should be > 3", rights.size() > 3);
    }

    @Test
    public void testGetAllEffectiveRights() throws Exception {
        ZimbraLog.test.debug("Starting testGetAllEffectiveRights");
        AllEffectiveRights aer = prov.getAllEffectiveRights(null, null, null,
                false /* expandSetAttrs */, true /* expandGetAttrs */);
        Assert.assertNotNull("AllEffectiveRights", aer);
    }

    @Test
    public void testGetEffectiveRights() throws Exception {
        ZimbraLog.test.debug("Starting testGetEffectiveRights");
        EffectiveRights er = prov.getEffectiveRights("account" /* targetType */,
                TargetBy.name /* targetBy */, "admin" /* target */,
                GranteeBy.name /* granteeBy */, "admin" /* grantee */,
                true /* expandSetAttrs */, true /* expandGetAttrs */);
        Assert.assertNotNull("EffectiveRights", er);
    }

    @Test
    public void testGetRightsDoc() throws Exception {
        ZimbraLog.test.debug("Starting testGetRightsDoc");
        Map<String, List<RightsDoc>> map = prov.getRightsDoc(null);
        Assert.assertTrue("Map size=" + map.size() +
                " should be >= 1", map.size() >= 1);
        String[] pkgs = { "com.zimbra.cs.service.admin" };
        map = prov.getRightsDoc(pkgs);
        Assert.assertEquals("Map for specified set of pkgs", 1, map.size());
        boolean seenTstRight = false;
        for (String key : map.keySet()) {
            Assert.assertEquals("key to map", pkgs[0], key);
            for (RightsDoc rightsDoc : map.get(key)) {
                Assert.assertNotNull("rightsDoc cmd name", rightsDoc.getCmd());
                if (rightsDoc.getCmd().equals("AddAccountAliasRequest")) {
                    seenTstRight = true;
                    Assert.assertEquals("Notes number", 3, rightsDoc.getNotes().size());
                    Assert.assertEquals("Rights number", 3, rightsDoc.getRights().size());
                }
            }
        }
        Assert.assertTrue("AddAccountAliasRequest right in report", seenTstRight);
    }

    @Test
    public void testGetRight() throws Exception {
        ZimbraLog.test.debug("Starting testGetRight");
        Right right = prov.getRight("adminConsoleAccountRights", true);
        Assert.assertNotNull("Right", right);
        RightClass rightClass = right.getRightClass();
        Assert.assertEquals("right RightClass", rightClass, RightClass.ADMIN);
        Assert.assertEquals("right Name", "adminConsoleAccountRights", right.getName());
    }

    @Test
    public void testHealth() throws Exception {
        ZimbraLog.test.debug("Starting testHealth");
        Assert.assertTrue(prov.healthCheck());
    }

    @Test
    public void testIdentities() throws Exception {
        ZimbraLog.test.debug("Starting testIdentities");
        Account acct = ensureAccountExists(testAcctEmail);
        List<Identity> identities = prov.getAllIdentities(acct);
        Assert.assertEquals("Number of identities for new acct", 1, identities.size());
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("zimbraPrefFromAddress", testAcctIdentity);
        Identity newId = prov.createIdentity(acct, "altIdentity", attrs);
        Assert.assertNotNull("New identity", newId);
        identities = prov.getAllIdentities(acct);
        Assert.assertEquals("Number of identities after add", 2, identities.size());
        prov.deleteIdentity(acct, "altIdentity");
        identities = prov.getAllIdentities(acct);
        Assert.assertEquals("Number of identities after delete", 1, identities.size());
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestJaxbProvisioning.class);
    }
}
