/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetResponse;
import com.zimbra.soap.admin.message.QueryWaitSetRequest;
import com.zimbra.soap.admin.message.QueryWaitSetResponse;
import com.zimbra.soap.admin.type.SessionForWaitSet;
import com.zimbra.soap.admin.type.WaitSetInfo;
import com.zimbra.soap.admin.type.WaitSetSessionInfo;
import com.zimbra.soap.mail.message.CreateWaitSetRequest;
import com.zimbra.soap.mail.message.CreateWaitSetResponse;
import com.zimbra.soap.mail.message.WaitSetRequest;
import com.zimbra.soap.mail.message.WaitSetResponse;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.WaitSetAddSpec;

public class TestWaitSetRequest {

    private static final String NAME_PREFIX = TestWaitSetRequest.class.getSimpleName();
    Account acc1 = null;
    Account acc2 = null;
    private boolean cbCalled = false;
    private Marshaller marshaller;
    private String waitSetId = null;
    private String failureMessage = null;
    private boolean success = false;
    private AtomicInteger numSignalledAccounts = new AtomicInteger(0);
    private AtomicInteger lastSeqNum = new AtomicInteger(0);
    private final SoapProvisioning soapProv = new SoapProvisioning();

    @Before
    public void setUp() throws Exception {
        cleanUp();
        marshaller = JaxbUtil.createMarshaller();
        soapProv.soapSetURI(TestUtil.getAdminSoapUrl());
        soapProv.soapZimbraAdminAuthenticate();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        try {
            soapProv.soapLogOut();
        } catch (ServiceException e) {
            //ignore
        }
    }

    private void cleanUp() throws Exception {
        if(acc1 != null) {
            acc1.deleteAccount();
            acc1 = null;
        }
        if(acc2 != null) {
            acc2.deleteAccount();
            acc2 = null;
        }
        cbCalled = false;
        numSignalledAccounts.set(0);
        lastSeqNum.set(0);
        if(waitSetId != null) {
            WaitSetMgr.destroy(null, null, waitSetId);
            waitSetId = null;
        }
        failureMessage = null;
        success = false;
    }

    private String envelope(String authToken, String requestBody, String urn) {
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<soap:Header>"+
        "<context xmlns=\"" + urn + "\">"+
        "<userAgent name=\"Zimbra Junit\" version=\"0.0\"/>"+
        "<authToken>" + authToken + "</authToken>" +
        "<nosession/>"+
        "</context>"+
        "</soap:Header>"+
        "<soap:Body>"+
        requestBody +
        "</soap:Body>"+
        "</soap:Envelope>";
    }

    private String jaxbToString(Object obj) throws JAXBException {
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(obj, dr);
        Document doc = dr.getDocument();
        return doc.getRootElement().asXML();
    }

    private Object sendReq(String requestBody, String url) throws IOException, ServiceException {
        CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/soap+xml");
        HttpEntity reqEntity = new ByteArrayEntity(requestBody.getBytes("UTF-8"));
        post.setEntity(reqEntity);
        HttpResponse response = client.execute(post);
        int respCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(200, respCode);
        Element envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
        Element doc = proto.getBodyElement(envelope);
        return JaxbUtil.elementToJaxb(doc);
    }

    private Object sendReq(Object obj, String authToken, String urlBase)
    throws IOException, ServiceException, JAXBException {
        return sendReq(envelope(authToken, jaxbToString(obj), "urn:zimbra"), urlBase + obj.getClass().getSimpleName());
    }

    @Test
    public void testSyncWaitSetRequest() throws Exception {
        String user1Name = "testSyncWaitSetRequest_user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        String authToken = mbox.getAuthToken().getValue();
        CreateWaitSetResponse resp = createWaitSet(mbox.getAccountInfo(false).getId(), authToken);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        WaitSetRequest waitSet = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, Integer.toString(seq));
        WaitSetResponse wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertEquals("0", wsResp.getSeqNo());

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        // try { Thread.sleep(500); } catch (Exception e) {}
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertFalse(wsResp.getSeqNo().equals("0"));
        List<AccountWithModifications> accounts =  wsResp.getSignalledAccounts();
        Assert.assertEquals("should have signaled 1 account", 1, accounts.size());
        Assert.assertEquals(String.format("Shold have signaled account %s", acc1.getId()), acc1.getId(), accounts.get(0).getId());
        Assert.assertNull("Should not return folder notifications unless 'expand' is set to 'true'", accounts.get(0).getPendingFolderModifications());
    }

    @Test
    public void testFolderInterestSyncWaitSetRequest() throws Exception {
        String user1Name = "testFISyncWaitSetRequest_user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        String acctId = mbox.getAccountId();
        String authToken = mbox.getAuthToken().getValue();
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        ZFolder myFolder = TestUtil.createFolder(mbox, "funFolder");
        Set<Integer> folderInterest = Sets.newHashSet();
        folderInterest.add(myFolder.getFolderIdInOwnerMailbox());

        /* initially only interested in funFolder */
        CreateWaitSetResponse resp = createWaitSet(acctId, authToken, folderInterest);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        WaitSetRequest waitSet = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, Integer.toString(seq));
        WaitSetResponse wsResp = (WaitSetResponse) sendReq(waitSet, authToken, TestUtil.getSoapUrl());
        Assert.assertEquals("0", wsResp.getSeqNo());

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertTrue(wsResp.getSeqNo().equals("0"));
        Assert.assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());

        QueryWaitSetResponse qwsResp;
        QueryWaitSetRequest qwsReq = new QueryWaitSetRequest(waitSetId);
        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acctId, folderInterest, null);

        /* interested in funFolder AND inbox */
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acctId, folderInterest));
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertTrue(wsResp.getSeqNo().equals("0"));
        Assert.assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());

        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acctId, folderInterest, null);

        subject = NAME_PREFIX + " test wait set request 2";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);

        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acctId, folderInterest, null);

        waitSet = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertFalse(wsResp.getSeqNo().equals("0"));
        Assert.assertEquals("Number of signalled accounts", 1, wsResp.getSignalledAccounts().size());
        AccountWithModifications acctInfo = wsResp.getSignalledAccounts().get(0);
        Assert.assertEquals("Signaled account id", mbox.getAccountId(), acctInfo.getId());
        Collection<PendingFolderModifications> mods = acctInfo.getPendingFolderModifications();
        Assert.assertNotNull("'mod' field should not be null", mods);
        Assert.assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        Integer foldInt = mods.iterator().next().getFolderId();
        Assert.assertEquals(String.format("Folder ID should be %d (Inbox). Getting %d instead", Mailbox.ID_FOLDER_INBOX, foldInt),
                foldInt.intValue(), Mailbox.ID_FOLDER_INBOX);
    }

    @Test
    public void testFolderInterestSyncAdminWaitSetRequest() throws Exception {
        String user1Name = "testFISyncAdminWaitSetRequest_user1";
        String user2Name = "testFISyncAdminWaitSetRequest_user2";
        acc1 = TestUtil.createAccount(user1Name);
        acc2 = TestUtil.createAccount(user2Name);
        ZMailbox zMbox1 = TestUtil.getZMailbox(user1Name);
        ZMailbox zMbox2 = TestUtil.getZMailbox(user2Name);
        Mailbox mbox1 = TestUtil.getMailbox(user1Name);
        Mailbox mbox2 = TestUtil.getMailbox(user2Name);
        Set<String> accountIds = new HashSet<String>();
        String acct1Id = zMbox1.getAccountId();
        String acct2Id = zMbox2.getAccountId();
        accountIds.add(acct1Id);
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        ZFolder user1FunFolder = TestUtil.createFolder(zMbox1, "funFolder");
        ZFolder user2FunFolder = TestUtil.createFolder(zMbox2, "funFolder");
        ZFolder user2FunFolder2 = TestUtil.createFolder(zMbox2, "funFolder2");
        Set<Integer> folderInterest = Sets.newHashSet();
        folderInterest.add(user1FunFolder.getFolderIdInOwnerMailbox());

        //initially only interested in user1::funFolder
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acct1Id, folderInterest));
        AdminWaitSetResponse wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals(0, seq);

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox1, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        Assert.assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());
        seq = Integer.parseInt(wsResp.getSeqNo());

        //now interested in user1::funFolder AND user1::inbox
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acct1Id, folderInterest));
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        //nothing happened, so should not trigger any accounts
        Assert.assertEquals("Number of signalled accounts (test 1)", 0, wsResp.getSignalledAccounts().size());
        seq = Integer.parseInt(wsResp.getSeqNo());

        subject = NAME_PREFIX + " test wait set request 2";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox1, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);

        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 2)", 1, wsResp.getSignalledAccounts().size());
        AccountWithModifications acctInfo = wsResp.getSignalledAccounts().get(0);
        Assert.assertEquals("Signaled account id (should signal user1)", acct1Id, acctInfo.getId());
        Collection<PendingFolderModifications> mods = acctInfo.getPendingFolderModifications();
        Assert.assertNotNull("'mod' field should not be null", mods);
        Assert.assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        Integer foldInt = mods.iterator().next().getFolderId();
        Assert.assertEquals(String.format("Folder ID should be %d (Inbox). Getting %d instead", Mailbox.ID_FOLDER_INBOX, foldInt),
                foldInt.intValue(), Mailbox.ID_FOLDER_INBOX);

        //Add message to user2 (should not trigger this waitset, because this waitset is not subscribed to user2 yet)
        subject = NAME_PREFIX + " test wait set request 3";
        TestUtil.addMessageLmtp(subject, user2Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox2, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 3)", 0, wsResp.getSignalledAccounts().size());

        //subscribe to user2::funFolder2
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        folderInterest = Sets.newHashSet();
        folderInterest.add(user2FunFolder2.getFolderIdInOwnerMailbox());
        waitSet.addAddAccount(createWaitSetAddSpec(acct2Id, folderInterest));
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 4)", 0, wsResp.getSignalledAccounts().size());

        //Add message to user2 (should NOT trigger this waitset yet, because WaitSet is subscribed to user2:funFolder2, user1:funFolder and user1:INBOX
        subject = NAME_PREFIX + " test wait set request 4";
        TestUtil.addMessageLmtp(subject, user2Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox2, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 5)", 0, wsResp.getSignalledAccounts().size());

        //add interest in user2:INBOX
        folderInterest = Sets.newHashSet();
        folderInterest.add(user2FunFolder2.getFolderIdInOwnerMailbox());
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acct2Id, folderInterest));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        //nothing happened, so should not trigger any accounts
        Assert.assertEquals("Number of signalled accounts (test 6)", 0, wsResp.getSignalledAccounts().size());

        //Add message to user2:INBOX (should trigger this WatSet now)
        subject = NAME_PREFIX + " test wait set request 5";
        TestUtil.addMessageLmtp(subject, user2Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox2, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());

        //now user2 should be triggered
        Assert.assertEquals("Number of signalled accounts (test 7)", 1, wsResp.getSignalledAccounts().size());
        acctInfo = wsResp.getSignalledAccounts().get(0);
        Assert.assertEquals("Signaled account id (should signal user2)", acct2Id, acctInfo.getId());
        mods = acctInfo.getPendingFolderModifications();
        Assert.assertNotNull("'mod' field should not be null", mods);
        Assert.assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        foldInt = mods.iterator().next().getFolderId();
        Assert.assertEquals(String.format("Folder ID should be %d (Inbox). Getting %d instead", Mailbox.ID_FOLDER_INBOX, foldInt),
                foldInt.intValue(), Mailbox.ID_FOLDER_INBOX);

        //Add message to user1:funFolder (should trigger this WatSet)
        subject = NAME_PREFIX + " test wait set request 6";
        TestUtil.addMessage(mbox1, user1FunFolder.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox1, String.format("in:%s is:unread \"%s\"", user1FunFolder.getName(), subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 8)", 1, wsResp.getSignalledAccounts().size());
        acctInfo = wsResp.getSignalledAccounts().get(0);
        Assert.assertEquals("Signaled account id (should signal user1)", acct1Id, acctInfo.getId());
        mods = acctInfo.getPendingFolderModifications();
        Assert.assertNotNull("'mod' field should not be null", mods);
        Assert.assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        foldInt = mods.iterator().next().getFolderId();
        Assert.assertEquals(String.format("Folder ID should be %d (%s). Getting %d instead", user1FunFolder.getFolderIdInOwnerMailbox(), user1FunFolder.getName(), foldInt),
                foldInt.intValue(), user1FunFolder.getFolderIdInOwnerMailbox());

        //Add message to user2:funFolder (should NOT trigger this WatSet, because it is subscribed to INBOX and funFolder2 on user2)
        subject = NAME_PREFIX + " test wait set request 7";
        TestUtil.addMessage(mbox2, user2FunFolder.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox2, String.format("in:%s is:unread \"%s\"", user2FunFolder.getName(), subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 9)", 0, wsResp.getSignalledAccounts().size());

        //Add message to user2:funFolder2 (should trigger this WatSet)
        subject = NAME_PREFIX + " test wait set request 8";
        TestUtil.addMessage(mbox2, user2FunFolder2.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox2, String.format("in:%s is:unread \"%s\"", user2FunFolder2.getName(), subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 10)", 1, wsResp.getSignalledAccounts().size());
        acctInfo = wsResp.getSignalledAccounts().get(0);
        Assert.assertEquals("Signaled account id (should signal user2)", acct2Id, acctInfo.getId());
        mods = acctInfo.getPendingFolderModifications();
        Assert.assertNotNull("'mod' field should not be null", mods);
        Assert.assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        foldInt = mods.iterator().next().getFolderId();
        Assert.assertEquals(String.format("Folder ID should be %d (%s). Getting %d instead", user2FunFolder2.getFolderIdInOwnerMailbox(), user2FunFolder2.getName(), foldInt),
                user2FunFolder2.getFolderIdInOwnerMailbox(), foldInt.intValue());

        //Add message to user2:funFolder2 and user1:INBOX (should trigger this WatSet)
        subject = NAME_PREFIX + " test wait set request 9";
        TestUtil.addMessage(mbox2, user2FunFolder2.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox2, String.format("in:%s is:unread \"%s\"", user2FunFolder2.getName(), subject), 1, 1000);
        subject = NAME_PREFIX + " test wait set request 10";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox1, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new com.zimbra.soap.admin.message.AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        Assert.assertEquals("Number of signalled accounts (test 11)", 2, wsResp.getSignalledAccounts().size());
        boolean user1Triggered = false;
        boolean user2Triggered = false;
        List<AccountWithModifications> accnts = wsResp.getSignalledAccounts();
        for(AccountWithModifications info : accnts) {
            if(info.getId().equalsIgnoreCase(acct1Id)) {
                user1Triggered = true;
                mods = info.getPendingFolderModifications();
                PendingFolderModifications fm = ((ArrayList<PendingFolderModifications>)mods).get(0);
                foldInt = fm.getFolderId();
                Assert.assertNotNull("'mods' field should not be null", mods);
                Assert.assertEquals("Should have 1 folder object with modifications for user1", 1, mods.size());
                Assert.assertEquals(String.format("Folder ID should be %d (INBOX). Getting %d instead. Account %s", Mailbox.ID_FOLDER_INBOX, foldInt, acct1Id),
                        Mailbox.ID_FOLDER_INBOX, foldInt.intValue());
            }
            if(info.getId().equalsIgnoreCase(acct2Id)) {
                user2Triggered = true;
                mods = info.getPendingFolderModifications();
                Assert.assertNotNull("'mods' field should not be null", mods);
                Assert.assertEquals("Should have 1 folder object with modifications for user2", 1, mods.size());
                PendingFolderModifications fm = ((ArrayList<PendingFolderModifications>)mods).get(0);
                foldInt = fm.getFolderId();
                Assert.assertEquals(String.format("Folder ID should be %d (%s). Getting %d instead. Account %s", user2FunFolder2.getFolderIdInOwnerMailbox(), user2FunFolder2.getName(), foldInt, acct2Id),
                        user2FunFolder2.getFolderIdInOwnerMailbox(), foldInt.intValue());
            }
        }
        Assert.assertTrue("Should have signalled user2", user2Triggered);
        Assert.assertTrue("Should have signalled user1", user1Triggered);
    }

    private void validateQueryWaitSetResponse(QueryWaitSetResponse qwsResp, String acctId,
            Set<Integer>folderInterests, Set<Integer> expectedChangedFolders) {
        Assert.assertEquals("Number of Waitsets in response", 1, qwsResp.getWaitsets().size());
        WaitSetInfo wsInfo = qwsResp.getWaitsets().get(0);
        Assert.assertEquals("waitSet owner", acctId, wsInfo.getOwner());
        Assert.assertEquals("Number of sessions in WaitSetResponse/waitSet", 1, wsInfo.getSessions().size());
        SessionForWaitSet session = wsInfo.getSessions().get(0);
        Assert.assertEquals("WaitSetResponse/waitSet/session@account", acctId, session.getAccount());
        WaitSetSessionInfo sessInfo = session.getWaitSetSession();
        if (!folderInterests.isEmpty()) {
            Set<Integer> respFolderInt = sessInfo.getFolderInterestsAsSet();
            for (Integer folderInterest : folderInterests) {
                Assert.assertTrue(String.format("Query reported folderInterests=%s should contain %s",
                        respFolderInt, folderInterest), respFolderInt.contains(folderInterest));
            }
        }
        if ((null != expectedChangedFolders) && !expectedChangedFolders.isEmpty()) {
            Set<Integer> respChangedFolders = sessInfo.getChangedFoldersAsSet();
            for (Integer changedFldr : expectedChangedFolders) {
                Assert.assertTrue(String.format("Query reported respChangedFolders=%s should contain %s",
                        respChangedFolders, changedFldr), respChangedFolders.contains(changedFldr));
            }
            String signalledAccts = wsInfo.getSignalledAccounts().getAccounts();
            Assert.assertTrue(String.format("ready accts '%s' contains '%s'", signalledAccts, acctId),
                    signalledAccts.contains(acctId));
        }
    }

    private CreateWaitSetResponse createWaitSet(String accountId, String authToken) throws Exception {
        return createWaitSet(accountId, authToken, null);
    }

    private CreateWaitSetResponse createWaitSet(String accountId, String authToken, Set<Integer>folderInterest)
    throws Exception {
        CreateWaitSetRequest req = new CreateWaitSetRequest("all", (Boolean) null);
        req.addAccount(createWaitSetAddSpec(accountId, folderInterest));
        return (CreateWaitSetResponse) sendReq(envelope(authToken, jaxbToString(req), "urn:zimbra"),
                TestUtil.getSoapUrl() + "CreateWaitSetRequest");
    }

    private WaitSetAddSpec createWaitSetAddSpec(String accountId, Set<Integer>folderInterest) {
        WaitSetAddSpec add = new WaitSetAddSpec();
        add.setId(accountId);
        add.setFolderInterests(folderInterest);
        return add;
    }

    private AdminCreateWaitSetResponse createAdminWaitSet(Set<String> accountIds, String authToken) throws Exception {
        AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
        for(String accountId : accountIds) {
            WaitSetAddSpec add = new WaitSetAddSpec();
            add.setId(accountId);
            req.addAccount(add);
        }

        DocumentResult dr = new DocumentResult();
        marshaller.marshal(req, dr);
        Document doc = dr.getDocument();
        return (AdminCreateWaitSetResponse)sendReq(envelope(authToken, doc.getRootElement().asXML(), "urn:zimbra"), TestUtil.getAdminSoapUrl() + "AdminCreateWaitSetRequest");
    }

    @Test
    public void testBlockingAdminWait2Accounts() throws Exception {
        ZimbraLog.test.info("Starting testBlockingAdminWait2Accounts");
        Set<String> accountIds = new HashSet<String>();
        String user1Name = "testBlockingAdminWait2Accounts_user1";
        String user2Name = "testBlockingAdminWait2Accounts_user2";
        acc1 = TestUtil.createAccount(user1Name);
        acc2 = TestUtil.createAccount(user2Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        accountIds.add(mbox.getAccountId());
        ZMailbox mbox2 = TestUtil.getZMailbox(user2Name);
        accountIds.add(mbox2.getAccountId());
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        Assert.assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        waitForAccounts(Arrays.asList(mbox.getAccountId(), mbox2.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminWait2Accounts");
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        TestUtil.addMessage(mbox2, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);
        if(numSignalledAccounts.intValue() < 2) {
            cbCalled = false;
            success = false;
            failureMessage = null;
            //the waitset may get return both accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum.toString());
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox.getAccountId(), mbox2.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminWait2Accounts");
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Assert.fail("Wait interrupted.");
            }
            Assert.assertTrue("callback was not triggered.", cbCalled);
            Assert.assertTrue(failureMessage, success);
            Assert.assertEquals("If WaitSet was triggered again, it should have returned only one account", 1, numSignalledAccounts.intValue());
        }
    }

    @Test
    public void testBlockingAdminWait1Account() throws Exception {
        ZimbraLog.test.info("Starting testBlockingAdminWait1Account");
        Set<String> accountIds = new HashSet<String>();
        String user1Name = "testBlockingAdminWait1Account_user1";
        String user2Name = "testBlockingAdminWait1Account_user2";
        acc1 = TestUtil.createAccount(user1Name);
        acc2 = TestUtil.createAccount(user2Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        accountIds.add(mbox.getAccountId());
        ZMailbox mbox2 = TestUtil.getZMailbox(user2Name);
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        Assert.assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        waitForAccounts(Arrays.asList(mbox.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminWait1Account");
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        TestUtil.addMessage(mbox2, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);
    }

    @Test
    public void testBlockingAdminAddAccount() throws Exception {
        ZimbraLog.test.info("Starting testBlockingAdminAddAccount");
        Set<String> accountIds = new HashSet<String>();
        String user1Name = "testBlockingAdminAddAccount_user1";
        String user2Name = "testBlockingAdminAddAccount_user2";
        acc1 = TestUtil.createAccount(user1Name);
        acc2 = TestUtil.createAccount(user2Name);
        ZMailbox mbox1 = TestUtil.getZMailbox(user1Name);
        accountIds.add(mbox1.getAccountId());
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        Assert.assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        ZimbraLog.test.info("Should signal only account %s", mbox1.getAccountId());
        waitForAccounts(Arrays.asList(mbox1.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminAddAccount - 1"); //should catch only update for user1
        String subject = NAME_PREFIX + " test wait set request 1";
        ZMailbox mbox2 = TestUtil.getZMailbox(user2Name);
        TestUtil.addMessage(mbox1, subject); //this event will notify waitset
        TestUtil.addMessage(mbox2, subject); //this event will NOT notify waitset
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);

        //test 2, add user2 to the existing waitset
        ZimbraLog.test.info("Sending 2d AdminWaitSetRequest");
        success = false;
        failureMessage = null;
        cbCalled = false;
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum.toString());
        waitSetReq.setBlock(true);

        //add user2 to the same waitset
        WaitSetAddSpec add = new WaitSetAddSpec();
        add.setId(mbox2.getAccountId());
        waitSetReq.addAddAccount(add);

        final CountDownLatch doneSignal2 = new CountDownLatch(1);

        ZimbraLog.test.info("Should signal accounts %s and %s", mbox1.getAccountId(), mbox2.getAccountId());
        subject = NAME_PREFIX + " test wait set request 2";

        waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal2, waitSetReq, "testBlockingAdminAddAccount - 2");
        TestUtil.addMessage(mbox2, subject);
        TestUtil.addMessage(mbox1, subject);

        try {
            doneSignal2.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback2 was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);

        if(numSignalledAccounts.intValue() < 2) {
            cbCalled = false;
            success = false;
            failureMessage = null;
            ZimbraLog.test.info("Sending followup to 2d AdminWaitSetRequest");
            //the waitset may return both accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum.toString());
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminAddAccount - 2.5");
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Assert.fail("Wait interrupted.");
            }
            Assert.assertTrue("callback2.5 was not triggered.", cbCalled);
            Assert.assertTrue(failureMessage, success);
            Assert.assertEquals("If WaitSet was triggered again, it should have returned only one account", 1, numSignalledAccounts.intValue());
        }

        //3rd request
        ZimbraLog.test.info("Sending 3rd AdminWaitSetRequest");
        success = false;
        failureMessage = null;
        cbCalled = false;
        final CountDownLatch doneSignal3 = new CountDownLatch(1);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum.toString());
        waitSetReq.setBlock(true);

        //both accounts should get signaled at this time
        waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal3, waitSetReq, "testBlockingAdminAddAccount - 3");
        TestUtil.addMessage(mbox2, subject);
        TestUtil.addMessage(mbox1, subject);
        try {
            doneSignal3.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback3 was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);

        if(numSignalledAccounts.intValue() < 2) {
            cbCalled = false;
            success = false;
            failureMessage = null;
            ZimbraLog.test.info("Sending followup to 3rd AdminWaitSetRequest");
            //the waitset may get return both accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum.toString());
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminAddAccount - 3.5");
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Assert.fail("Wait interrupted.");
            }
            Assert.assertTrue("callback3.5 was not triggered.", cbCalled);
            Assert.assertTrue(failureMessage, success);
            Assert.assertEquals("If WaitSet was triggered again, it should have returned only one account", 1, numSignalledAccounts.intValue());
        }

        //4th request
        ZimbraLog.test.info("Sending 4th AdminWaitSetRequest");
        success = false;
        failureMessage = null;
        cbCalled = false;
        final CountDownLatch doneSignal4 = new CountDownLatch(1);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum.toString());
        waitSetReq.setBlock(true);

        //only second account should get signaled this time
        waitForAccounts(Arrays.asList(mbox2.getAccountId()), doneSignal4, waitSetReq, "testBlockingAdminAddAccount - 4");
        TestUtil.addMessage(mbox2, subject);
        try {
            doneSignal4.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback4 was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);
    }

    @Test
    public void testBlockingWaitSetRequest() throws Exception {
        ZimbraLog.test.info("Starting testBlockingWaitSetRequest");
        String user1Name = "testBlockingWaitSetRequest_user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        String authToken = mbox.getAuthToken().getValue();
        String accId = mbox.getAccountId();
        CreateWaitSetResponse resp = createWaitSet(accId, authToken);
        Assert.assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();
        Assert.assertNotNull(waitSetId);

        WaitSetRequest waitSetReq = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);

        final CountDownLatch doneSignal = new CountDownLatch(1);
        mbox.getTransport().invokeAsync(JaxbUtil.jaxbToElement(waitSetReq), new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                cbCalled = true;
                int respCode = response.getStatusLine().getStatusCode();
                success = (respCode == 200);
                if(!success) {
                    failureMessage = "Response code " + respCode;
                }
                if(success) {
                    Element envelope;
                    try {
                        envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                        Element doc = proto.getBodyElement(envelope);
                        WaitSetResponse wsResp = (WaitSetResponse) JaxbUtil.elementToJaxb(doc);
                        success = (Integer.parseInt(wsResp.getSeqNo()) > 0);
                        if(!success) {
                            failureMessage = "wrong squence number. Sequence #" + wsResp.getSeqNo();
                        }
                        if(success) {
                            success = (wsResp.getSignalledAccounts().size() == 1);
                            if(!success) {
                                failureMessage = "wrong number of signaled accounts " + wsResp.getSignalledAccounts().size();
                            }
                        }
                        if(success) {
                            success = wsResp.getSignalledAccounts().get(0).getId().equalsIgnoreCase(accId);
                            if(!success) {
                                failureMessage = "signaled wrong account " +  wsResp.getSignalledAccounts().get(0).getId();
                            }
                        }
                    } catch (UnsupportedOperationException | IOException | ServiceException e) {
                        Assert.fail(e.getMessage());
                    }
                    try { Thread.sleep(100); } catch (Exception e) {}
                }
                doneSignal.countDown();
            }

            @Override
            public void failed(final Exception ex) {
                ZimbraLog.test.error("request :: failed ", ex);
                success = false;
                failureMessage = ex.getMessage();
                doneSignal.countDown();
            }

            @Override
            public void cancelled() {
                ZimbraLog.test.info("request :: cancelled");
                success = false;
                failureMessage = "request :: cancelled";
                doneSignal.countDown();
            }
        });

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted. ");
        }
        Assert.assertTrue("callback was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);
    }

    private void waitForAccounts(List<String> accountIds, CountDownLatch doneSignal, Object req, String caller) throws Exception {
        soapProv.invokeJaxbAsync(req,  new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                ZimbraLog.test.info("waitForAccounts %s :: completed", caller);
                cbCalled = true;
                int respCode = response.getStatusLine().getStatusCode();
                success = (respCode == 200);
                if(!success) {
                    failureMessage = "response code " + respCode;
                }
                if(success) {
                    Element envelope;
                    try {
                        envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                        Element doc = proto.getBodyElement(envelope);
                        AdminWaitSetResponse wsResp = (AdminWaitSetResponse) JaxbUtil.elementToJaxb(doc);
                        ZimbraLog.test.info(new String(doc.toUTF8(), "UTF-8"));
                        for(int i=0; i < wsResp.getSignalledAccounts().size(); i++) {
                            ZimbraLog.test.info("signalled account " + wsResp.getSignalledAccounts().get(i).getId());
                        }
                        success = (Integer.parseInt(wsResp.getSeqNo()) > 0);
                        if(!success) {
                            failureMessage = "wrong squence number. Sequence #" + wsResp.getSeqNo();
                        }
                        lastSeqNum.set(Integer.parseInt(wsResp.getSeqNo()));
                        if(success) {
                            success = (wsResp.getSignalledAccounts().size() <= accountIds.size());
                            if(!success) {
                                failureMessage = "wrong number of signaled accounts " + wsResp.getSignalledAccounts().size();
                            }
                            numSignalledAccounts.set(wsResp.getSignalledAccounts().size());
                        }
                        if(success) {
                            for(int i=0; i < wsResp.getSignalledAccounts().size(); i++) {
                                success = accountIds.contains(wsResp.getSignalledAccounts().get(i).getId());
                                if(!success) {
                                    failureMessage = "received notificaiton for an unexpected account " + wsResp.getSignalledAccounts().get(i).getId();
                                    break;
                                }
                            }
                        }
                    } catch (UnsupportedOperationException | IOException | ServiceException e) {
                        success = false;
                        failureMessage = e.getMessage();
                        ZimbraLog.test.error(e);
                    }
                }
                try { Thread.sleep(100); } catch (Exception e) {}
                doneSignal.countDown();
            }

            @Override
            public void failed(final Exception ex) {
                ZimbraLog.test.error("waitForAccounts :: failed ", ex);
                success = false;
                failureMessage = ex.getMessage();
                doneSignal.countDown();
            }

            @Override
            public void cancelled() {
                ZimbraLog.test.info("waitForAccounts :: cancelled");
                success = false;
                failureMessage = "waitForAccounts :: cancelled";
                doneSignal.countDown();
            }
        });
    }
}
