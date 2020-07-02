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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.http.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Sets;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.service.admin.AdminServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetResponse;
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

    @Rule
    public TestName testInfo = new TestName();

    private static final String NAME_PREFIX = TestWaitSetRequest.class.getSimpleName();
    private Account acc1 = null;
    private Account acc2 = null;
    private Account acc3 = null;
    private AtomicBoolean cbCalled = new AtomicBoolean(false);
    private static Marshaller marshaller;
    private String waitSetId = null;
    private String failureMessage = null;
    private AtomicBoolean success = new AtomicBoolean(false);
    private AtomicInteger numSignalledAccounts = new AtomicInteger(0);
    private String lastSeqNum = null;
    private final SoapProvisioning soapProv = new SoapProvisioning();

    @BeforeClass
    public static void beforeClass() throws Exception {
        marshaller = JaxbUtil.createMarshaller();
    }

    @Before
    public void setUp() throws Exception {
        cleanUp();
        soapProv.soapSetURI(TestUtil.getAdminSoapUrl());
        soapProv.soapZimbraAdminAuthenticate();
        // Actually, shouldn't be using any. TestUtil.getAdminSoapTransport used to create them
        // un-necessarily
        ZimbraLog.test.debug("Number of Admin SOAP sessions before test %d", countActiveSessionsForAdmin());
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        try {
            ZimbraLog.test.debug("Number of Admin SOAP sessions after test (before soapLogOut) %d",
                    countActiveSessionsForAdmin());
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
        if(acc3 != null) {
            acc3.deleteAccount();
            acc3 = null;
        }
        cbCalled.set(false);
        numSignalledAccounts.set(0);
        lastSeqNum = null;
        if (waitSetId != null) {
            try {
                WaitSetMgr.destroy(null, null, waitSetId);
                ZimbraLog.test.debug("TestWaitSetRequest.cleanUp Destroyed waitSetId %s", waitSetId);
            } catch (ServiceException ex) {
                if (!ex.getCode().equalsIgnoreCase(MailServiceException.NO_SUCH_WAITSET)) {
                    ZimbraLog.test.warn("TestWaitSetRequest.cleanUp - Problem Destroying waitSetId %s",
                            waitSetId, ex);
                }
            }
            waitSetId = null;
        }
        failureMessage = null;
        success.set(false);
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

    private <T> T sendReq(String requestBody, String url) throws IOException, ServiceException {
        Element envelope = sendReqGetEnvelope(requestBody, url, HttpStatus.SC_OK);
        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
        Element doc = proto.getBodyElement(envelope);
        return JaxbUtil.elementToJaxb(doc);
    }

    private <T> T sendReq(Object obj, String authToken, String urlBase)
            throws IOException, ServiceException, JAXBException  {
        return sendReq(envelope(authToken, jaxbToString(obj), "urn:zimbra"),
                urlBase + obj.getClass().getSimpleName());
    }

    private Element sendReqExpectedToFail(Object obj, String authToken, String urlBase, int expectedCode)
    throws IOException, ServiceException, JAXBException {
        return sendReqGetEnvelope(envelope(authToken, jaxbToString(obj), "urn:zimbra"),
                urlBase + obj.getClass().getSimpleName(), expectedCode);
    }

    private Element sendReqGetEnvelope(String requestBody, String url, int expectedCode)
            throws IOException, ServiceException {
        CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/soap+xml");
        HttpEntity reqEntity = new ByteArrayEntity(requestBody.getBytes("UTF-8"));
        post.setEntity(reqEntity);
        HttpResponse response = client.execute(post);
        int respCode = response.getStatusLine().getStatusCode();
        assertEquals("Expected HTTP status code", expectedCode, respCode);
        return W3cDomUtil.parseXML(response.getEntity().getContent());
    }

    @Test
    public void testSyncWaitSetRequest() throws Exception {
        String user1Name = "testSyncWaitSetRequest_user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        String authToken = mbox.getAuthToken().getValue();
        CreateWaitSetResponse resp = createWaitSet(mbox.getAccountInfo(false).getId(), authToken);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        WaitSetRequest waitSet = new WaitSetRequest(waitSetId, Integer.toString(seq));
        WaitSetResponse wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        assertEquals("0", wsResp.getSeqNo());

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        // try { Thread.sleep(500); } catch (Exception e) {}
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getSoapUrl() + "WaitSetRequest");
        assertFalse(wsResp.getSeqNo().equals("0"));
        List<AccountWithModifications> accounts =  wsResp.getSignalledAccounts();
        assertEquals("should have signaled 1 account", 1, accounts.size());
        assertEquals(String.format("Shold have signaled account %s", acc1.getId()), acc1.getId(), accounts.get(0).getId());
        assertNull("Should not return folder notifications unless 'expand' is set to 'true'", accounts.get(0).getPendingFolderModifications());
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
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        WaitSetRequest waitSet = new WaitSetRequest(waitSetId, Integer.toString(seq));
        WaitSetResponse wsResp = (WaitSetResponse) sendReq(waitSet, authToken, TestUtil.getSoapUrl());
        assertEquals("0", wsResp.getSeqNo());

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getSoapUrl() + "WaitSetRequest");
        assertTrue(wsResp.getSeqNo().equals("0"));
        assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());

        QueryWaitSetResponse qwsResp;
        QueryWaitSetRequest qwsReq = new QueryWaitSetRequest(waitSetId);
        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acctId, folderInterest, null);

        /* interested in funFolder AND inbox */
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acctId, folderInterest));
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        assertTrue(wsResp.getSeqNo().equals("0"));
        assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());

        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acctId, folderInterest, null);

        subject = NAME_PREFIX + " test wait set request 2";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);

        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acctId, folderInterest, null);

        waitSet = new WaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        assertFalse(wsResp.getSeqNo().equals("0"));
        assertEquals("Number of signalled accounts", 1, wsResp.getSignalledAccounts().size());
        AccountWithModifications acctInfo = wsResp.getSignalledAccounts().get(0);
        assertEquals("Signaled account id", mbox.getAccountId(), acctInfo.getId());
        Collection<PendingFolderModifications> mods = acctInfo.getPendingFolderModifications();
        assertNotNull("'mod' field should not be null", mods);
        assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        Integer foldInt = mods.iterator().next().getFolderId();
        assertEquals(String.format("Folder ID should be %d (Inbox). Getting %d instead", Mailbox.ID_FOLDER_INBOX, foldInt),
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
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken, false);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acct1Id, folderInterest));
        AdminWaitSetResponse wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals(0, seq);

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox1, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());
        seq = Integer.parseInt(wsResp.getSeqNo());

        //now interested in user1::funFolder AND user1::inbox
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acct1Id, folderInterest));
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        //nothing happened, so should not trigger any accounts
        assertEquals("Number of signalled accounts (test 1)", 0, wsResp.getSignalledAccounts().size());
        seq = Integer.parseInt(wsResp.getSeqNo());

        subject = NAME_PREFIX + " test wait set request 2";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox1, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);

        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 2)", 1, wsResp.getSignalledAccounts().size());
        AccountWithModifications acctInfo = wsResp.getSignalledAccounts().get(0);
        assertEquals("Signaled account id (should signal user1)", acct1Id, acctInfo.getId());
        Collection<PendingFolderModifications> mods = acctInfo.getPendingFolderModifications();
        assertNotNull("'mod' field should not be null", mods);
        assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        Integer foldInt = mods.iterator().next().getFolderId();
        assertEquals(String.format("Folder ID should be %d (Inbox). Getting %d instead", Mailbox.ID_FOLDER_INBOX, foldInt),
                foldInt.intValue(), Mailbox.ID_FOLDER_INBOX);

        //Add message to user2 (should not trigger this waitset, because this waitset is not subscribed to user2 yet)
        subject = NAME_PREFIX + " test wait set request 3";
        TestUtil.addMessageLmtp(subject, user2Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox2, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 3)", 0, wsResp.getSignalledAccounts().size());

        //subscribe to user2::funFolder2
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        folderInterest = Sets.newHashSet();
        folderInterest.add(user2FunFolder2.getFolderIdInOwnerMailbox());
        waitSet.addAddAccount(createWaitSetAddSpec(acct2Id, folderInterest));
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 4)", 0, wsResp.getSignalledAccounts().size());

        //Add message to user2 (should NOT trigger this waitset yet, because WaitSet is subscribed to user2:funFolder2, user1:funFolder and user1:INBOX
        subject = NAME_PREFIX + " test wait set request 4";
        TestUtil.addMessageLmtp(subject, user2Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox2, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 5)", 0, wsResp.getSignalledAccounts().size());

        //add interest in user2:INBOX
        folderInterest = Sets.newHashSet();
        folderInterest.add(user2FunFolder2.getFolderIdInOwnerMailbox());
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.addUpdateAccount(createWaitSetAddSpec(acct2Id, folderInterest));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        //nothing happened, so should not trigger any accounts
        assertEquals("Number of signalled accounts (test 6)", 0, wsResp.getSignalledAccounts().size());

        //Add message to user2:INBOX (should trigger this WatSet now)
        subject = NAME_PREFIX + " test wait set request 5";
        TestUtil.addMessageLmtp(subject, user2Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox2, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());

        //now user2 should be triggered
        assertEquals("Number of signalled accounts (test 7)", 1, wsResp.getSignalledAccounts().size());
        acctInfo = wsResp.getSignalledAccounts().get(0);
        assertEquals("Signaled account id (should signal user2)", acct2Id, acctInfo.getId());
        mods = acctInfo.getPendingFolderModifications();
        assertNotNull("'mod' field should not be null", mods);
        assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        foldInt = mods.iterator().next().getFolderId();
        assertEquals(String.format("Folder ID should be %d (Inbox). Getting %d instead", Mailbox.ID_FOLDER_INBOX, foldInt),
                foldInt.intValue(), Mailbox.ID_FOLDER_INBOX);

        //Add message to user1:funFolder (should trigger this WatSet)
        subject = NAME_PREFIX + " test wait set request 6";
        TestUtil.addMessage(mbox1, user1FunFolder.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox1, String.format("in:%s is:unread \"%s\"", user1FunFolder.getName(), subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 8)", 1, wsResp.getSignalledAccounts().size());
        acctInfo = wsResp.getSignalledAccounts().get(0);
        assertEquals("Signaled account id (should signal user1)", acct1Id, acctInfo.getId());
        mods = acctInfo.getPendingFolderModifications();
        assertNotNull("'mod' field should not be null", mods);
        assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        foldInt = mods.iterator().next().getFolderId();
        assertEquals(String.format("Folder ID should be %d (%s). Getting %d instead", user1FunFolder.getFolderIdInOwnerMailbox(), user1FunFolder.getName(), foldInt),
                foldInt.intValue(), user1FunFolder.getFolderIdInOwnerMailbox());

        //Add message to user2:funFolder (should NOT trigger this WatSet, because it is subscribed to INBOX and funFolder2 on user2)
        subject = NAME_PREFIX + " test wait set request 7";
        TestUtil.addMessage(mbox2, user2FunFolder.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox2, String.format("in:%s is:unread \"%s\"", user2FunFolder.getName(), subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 9)", 0, wsResp.getSignalledAccounts().size());

        //Add message to user2:funFolder2 (should trigger this WatSet)
        subject = NAME_PREFIX + " test wait set request 8";
        TestUtil.addMessage(mbox2, user2FunFolder2.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox2, String.format("in:%s is:unread \"%s\"", user2FunFolder2.getName(), subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 10)", 1, wsResp.getSignalledAccounts().size());
        acctInfo = wsResp.getSignalledAccounts().get(0);
        assertEquals("Signaled account id (should signal user2)", acct2Id, acctInfo.getId());
        mods = acctInfo.getPendingFolderModifications();
        assertNotNull("'mod' field should not be null", mods);
        assertEquals("Should have 1 folder object with modifications", 1, mods.size());
        foldInt = mods.iterator().next().getFolderId();
        assertEquals(String.format("Folder ID should be %d (%s). Getting %d instead", user2FunFolder2.getFolderIdInOwnerMailbox(), user2FunFolder2.getName(), foldInt),
                user2FunFolder2.getFolderIdInOwnerMailbox(), foldInt.intValue());

        //Add message to user2:funFolder2 and user1:INBOX (should trigger this WatSet)
        subject = NAME_PREFIX + " test wait set request 9";
        TestUtil.addMessage(mbox2, user2FunFolder2.getFolderIdInOwnerMailbox(), subject, System.currentTimeMillis());
        TestUtil.waitForMessages(zMbox2, String.format("in:%s is:unread \"%s\"", user2FunFolder2.getName(), subject), 1, 1000);
        subject = NAME_PREFIX + " test wait set request 10";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(zMbox1, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSet.setExpand(true);
        wsResp = (AdminWaitSetResponse) sendReq(envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
        seq = Integer.parseInt(wsResp.getSeqNo());
        assertEquals("Number of signalled accounts (test 11)", 2, wsResp.getSignalledAccounts().size());
        boolean user1Triggered = false;
        boolean user2Triggered = false;
        List<AccountWithModifications> accnts = wsResp.getSignalledAccounts();
        for(AccountWithModifications info : accnts) {
            if(info.getId().equalsIgnoreCase(acct1Id)) {
                user1Triggered = true;
                mods = info.getPendingFolderModifications();
                PendingFolderModifications fm = ((ArrayList<PendingFolderModifications>)mods).get(0);
                foldInt = fm.getFolderId();
                assertNotNull("'mods' field should not be null", mods);
                assertEquals("Should have 1 folder object with modifications for user1", 1, mods.size());
                assertEquals(String.format("Folder ID should be %d (INBOX). Getting %d instead. Account %s", Mailbox.ID_FOLDER_INBOX, foldInt, acct1Id),
                        Mailbox.ID_FOLDER_INBOX, foldInt.intValue());
            }
            if(info.getId().equalsIgnoreCase(acct2Id)) {
                user2Triggered = true;
                mods = info.getPendingFolderModifications();
                assertNotNull("'mods' field should not be null", mods);
                assertEquals("Should have 1 folder object with modifications for user2", 1, mods.size());
                PendingFolderModifications fm = ((ArrayList<PendingFolderModifications>)mods).get(0);
                foldInt = fm.getFolderId();
                assertEquals(String.format("Folder ID should be %d (%s). Getting %d instead. Account %s", user2FunFolder2.getFolderIdInOwnerMailbox(), user2FunFolder2.getName(), foldInt, acct2Id),
                        user2FunFolder2.getFolderIdInOwnerMailbox(), foldInt.intValue());
            }
        }
        assertTrue("Should have signalled user2", user2Triggered);
        assertTrue("Should have signalled user1", user1Triggered);
    }

    private int countActiveSessionsForAdmin() throws ServiceException {
        Account adminAcct = soapProv.get(AccountBy.adminName, LC.zimbra_ldap_user.value());
        return SessionCache.countActiveSessionsForAccount(adminAcct.getId(), Session.Type.ADMIN);
    }

    /**
     * Test that can change the info on which folders we're interested in several times.
     * Introduced for ZCS-2220 although discovered that shouldn't need ADMIN SOAP sessions
     * for IMAP AdminWaitSet code, so counting them is a bit redundant.  Kept test
     * as some assurance that lots of folder interest changes works OK.
     */
    @Test
    public void adminWSfolderInterestChanges() throws Exception {
        int numFolders = LC.zimbra_session_limit_admin.intValue() + 15;
        String user1Name = testInfo.getMethodName().toLowerCase() + "user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox zMbox1 = TestUtil.getZMailbox(user1Name);
        Set<String> accountIds = new HashSet<String>();
        String acct1Id = zMbox1.getAccountId();
        accountIds.add(acct1Id);
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        List<ZFolder> folders = new ArrayList<>(numFolders);
        for (int fnum = 1; fnum <= numFolders; fnum++) {
            folders.add(TestUtil.createFolder(zMbox1, String.format("FOLDER_%s", fnum)));
        }

        int numSess = countActiveSessionsForAdmin();
        SessionCache.getAllSessions(acct1Id);
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken, false);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();

        for (int fnum = 0; fnum < numFolders; fnum++) {
            AdminWaitSetRequest waitSet = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
            Set<Integer> folderInterest = Sets.newHashSet();
            folderInterest.add(folders.get(fnum).getFolderIdInOwnerMailbox());
            waitSet.addUpdateAccount(createWaitSetAddSpec(acct1Id, folderInterest));
            AdminWaitSetResponse wsResp = sendReq(
                    envelope(adminAuthToken, jaxbToString(waitSet), "urn:zimbra"),
                    TestUtil.getAdminSoapUrl() + "AdminWaitSetRequest");
            seq = Integer.parseInt(wsResp.getSeqNo());
            assertEquals("Number of ADMIN sessions (should not have increased)", numSess,
                    countActiveSessionsForAdmin());
        }
    }

    private void validateQueryWaitSetResponse(QueryWaitSetResponse qwsResp, String acctId,
            Set<Integer>folderInterests, Set<Integer> expectedChangedFolders) {
        validateQueryWaitSetResponse(qwsResp, acctId,
                folderInterests, expectedChangedFolders, true);
    }

    private void validateQueryWaitSetResponse(QueryWaitSetResponse qwsResp, String acctId,
            Set<Integer> folderInterests, Set<Integer> expectedChangedFolders, boolean checkOwner) {
        assertEquals("Number of Waitsets in response", 1, qwsResp.getWaitsets().size());
        WaitSetInfo wsInfo = qwsResp.getWaitsets().get(0);
        if(checkOwner) {
            assertEquals("waitSet owner", acctId, wsInfo.getOwner());
        }
        assertEquals("Number of sessions in WaitSetResponse/waitSet", 1, wsInfo.getSessions().size());
        SessionForWaitSet session = wsInfo.getSessions().get(0);
        assertEquals("WaitSetResponse/waitSet/session@account", acctId, session.getAccount());
        WaitSetSessionInfo sessInfo = session.getWaitSetSession();
        if ((null != folderInterests) && !folderInterests.isEmpty()) {
            Set<Integer> respFolderInt = sessInfo.getFolderInterestsAsSet();
            for (Integer folderInterest : folderInterests) {
                assertTrue(String.format("Query reported folderInterests=%s should contain %s",
                        respFolderInt, folderInterest), respFolderInt.contains(folderInterest));
            }
        }
        if ((null != expectedChangedFolders) && !expectedChangedFolders.isEmpty()) {
            Set<Integer> respChangedFolders = sessInfo.getChangedFoldersAsSet();
            for (Integer changedFldr : expectedChangedFolders) {
                assertTrue(String.format("Query reported respChangedFolders=%s should contain %s",
                        respChangedFolders, changedFldr), respChangedFolders.contains(changedFldr));
            }
            String signalledAccts = wsInfo.getSignalledAccounts().getAccounts();
            assertTrue(String.format("ready accts '%s' contains '%s'", signalledAccts, acctId),
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

    private AdminCreateWaitSetResponse createAdminWaitSet(
            Set<String> accountIds, String authToken, boolean all) throws Exception {
        AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", all);
        if(accountIds != null) {
            for(String accountId : accountIds) {
                WaitSetAddSpec add = new WaitSetAddSpec();
                add.setId(accountId);
                req.addAccount(add);
            }
        }

        DocumentResult dr = new DocumentResult();
        marshaller.marshal(req, dr);
        Document doc = dr.getDocument();
        AdminCreateWaitSetResponse acwsResp =
                sendReq(envelope(authToken, doc.getRootElement().asXML(), "urn:zimbra"),
                        TestUtil.getAdminSoapUrl() + "AdminCreateWaitSetRequest");
        return acwsResp;
    }

    @Test
    public void testDestroyWaitset() throws Exception {
        ZimbraLog.test.info("Starting testDestroyWaitset");
        String user1Name = "testDestroyWaitset_user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        Set<String> accountIds = new HashSet<String>();
        accountIds.add(mbox.getAccountId());
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken, false);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        assertNotNull(waitSetId);
        QueryWaitSetRequest qwsReq = new QueryWaitSetRequest(waitSetId);
        QueryWaitSetResponse qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        validateQueryWaitSetResponse(qwsResp, acc1.getId(), null, null, false);

        AdminDestroyWaitSetRequest destroyReq = new AdminDestroyWaitSetRequest(waitSetId);
        AdminDestroyWaitSetResponse destroyResp = (AdminDestroyWaitSetResponse) sendReq(destroyReq, adminAuthToken, TestUtil.getAdminSoapUrl());
        assertNotNull("AdminDestroyWaitSetResponse should not be null", destroyResp);
        assertNotNull("AdminDestroyWaitSetResponse::waitSetId should not be null", destroyResp.getWaitSetId());
        assertEquals("AdminDestroyWaitSetResponse has wrong waitSetId", waitSetId, destroyResp.getWaitSetId());
        qwsReq = new QueryWaitSetRequest(waitSetId);
        Element faultResp = sendReqExpectedToFail(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertNotNull("should return Element", faultResp);
        try {
            TestUtil.getAdminSoapTransport().extractBodyElement(faultResp);
            fail("Should thrown SoapFaultException");
        } catch (SoapFaultException sfe) {
            assertEquals("Expecting admin.NO_SUCH_WAITSET", AdminServiceException.NO_SUCH_WAITSET, sfe.getCode());
        } catch (SoapParseException spe) {
            fail("Should not be throwing SoapParseException. " + spe.getMessage());
        }
        waitSetId = null;
    }

    @Test
    public void testBlockingAdminWaitAllAccounts() throws Exception {
        ZimbraLog.test.info("Starting testBlockingAdminWaitAllAccounts");
        String user1Name = "testBlockingAdminWaitAllAccounts_user1";
        String user2Name = "testBlockingAdminWaitAllAccounts_user2";
        String user3Name = "testBlockingAdminWaitAllAccounts_user3";
        acc1 = TestUtil.createAccount(user1Name);
        acc2 = TestUtil.createAccount(user2Name);
        acc3 = TestUtil.createAccount(user3Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        ZMailbox mbox2 = TestUtil.getZMailbox(user2Name);
        ZMailbox mbox3 = TestUtil.getZMailbox(user3Name);
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        AdminCreateWaitSetResponse resp = createAdminWaitSet(null, adminAuthToken, true);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        waitForAccounts(Arrays.asList(mbox.getAccountId(), mbox2.getAccountId(), mbox3.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminWaitAllAccounts", true);
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        TestUtil.addMessage(mbox2, subject);
        TestUtil.addMessage(mbox3, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
        int signalled = numSignalledAccounts.intValue();
        while(signalled < 3) {
            cbCalled.set(false);
            success.set(false);
            failureMessage = null;
            //the waitset may be triggered for all accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox.getAccountId(), mbox2.getAccountId(), mbox3.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminWaitAllAccounts", true);
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Wait interrupted.");
            }
            signalled += numSignalledAccounts.intValue();
            ZimbraLog.test.debug("Signalled %d accounts", signalled);
            assertTrue("callback was not triggered.", cbCalled.get());
            assertTrue(failureMessage, success.get());
        }
        assertEquals("This waitset has to signal 3 accounts", 3, signalled);
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
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken, false);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        waitForAccounts(Arrays.asList(mbox.getAccountId(), mbox2.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminWait2Accounts", false);
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        TestUtil.addMessage(mbox2, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
        if(numSignalledAccounts.intValue() < 2) {
            cbCalled.set(false);
            success.set(false);
            failureMessage = null;
            //the waitset may be triggered for both accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox.getAccountId(), mbox2.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminWait2Accounts", false);
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Wait interrupted.");
            }
            assertTrue("callback was not triggered.", cbCalled.get());
            assertTrue(failureMessage, success.get());
            assertEquals("If WaitSet was triggered again, it should have returned only one account", 1, numSignalledAccounts.intValue());
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
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken, false);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        waitForAccounts(Arrays.asList(mbox.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminWait1Account", false);
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        TestUtil.addMessage(mbox2, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
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
        AdminCreateWaitSetResponse resp = createAdminWaitSet(accountIds, adminAuthToken, false);
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        assertNotNull(waitSetId);
        int seq = resp.getSequence();

        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        ZimbraLog.test.info("Should signal only account %s", mbox1.getAccountId());
        waitForAccounts(Arrays.asList(mbox1.getAccountId()), doneSignal, waitSetReq, "testBlockingAdminAddAccount - 1", false); //should catch only update for user1
        String subject = NAME_PREFIX + " test wait set request 1";
        ZMailbox mbox2 = TestUtil.getZMailbox(user2Name);
        TestUtil.addMessage(mbox1, subject); //this event will notify waitset
        TestUtil.addMessage(mbox2, subject); //this event will NOT notify waitset
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());

        //test 2, add user2 to the existing waitset
        ZimbraLog.test.info("Sending 2d AdminWaitSetRequest");
        success.set(false);
        failureMessage = null;
        cbCalled.set(false);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
        waitSetReq.setBlock(true);

        //add user2 to the same waitset
        WaitSetAddSpec add = new WaitSetAddSpec();
        add.setId(mbox2.getAccountId());
        waitSetReq.addAddAccount(add);

        final CountDownLatch doneSignal2 = new CountDownLatch(1);

        ZimbraLog.test.info("Should signal accounts %s and %s", mbox1.getAccountId(), mbox2.getAccountId());
        subject = NAME_PREFIX + " test wait set request 2";

        waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal2, waitSetReq, "testBlockingAdminAddAccount - 2", false);
        TestUtil.addMessage(mbox1, subject);
        TestUtil.addMessage(mbox2, subject);

        try {
            doneSignal2.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback2 was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
        int signalled = numSignalledAccounts.intValue();
        while(signalled < 2) {
            cbCalled.set(false);
            success.set(false);
            failureMessage = null;
            ZimbraLog.test.info("Sending followup to 2d AdminWaitSetRequest");
            //the waitset may return both accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminAddAccount - 2.5", false);
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Wait interrupted.");
            }
            assertTrue("callback2.5 was not triggered.", cbCalled.get());
            assertTrue(failureMessage, success.get());
            signalled+=numSignalledAccounts.intValue();
        }
        assertEquals("Should signal 2 accounts in total", 2, signalled);
        //3rd request
        ZimbraLog.test.info("Sending 3rd AdminWaitSetRequest");
        success.set(false);
        failureMessage = null;
        cbCalled.set(false);
        final CountDownLatch doneSignal3 = new CountDownLatch(1);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
        waitSetReq.setBlock(true);

        //both accounts should get signaled at this time
        waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal3, waitSetReq, "testBlockingAdminAddAccount - 3", false);
        TestUtil.addMessage(mbox2, subject);
        TestUtil.addMessage(mbox1, subject);
        try {
            doneSignal3.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback3 was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
        signalled = numSignalledAccounts.intValue();
        while(signalled < 2) {
            cbCalled.set(false);
            success.set(false);
            failureMessage = null;
            ZimbraLog.test.info("Sending followup to 3rd AdminWaitSetRequest");
            //the waitset may get return both accounts at once or be triggered for each account separately
            waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
            waitSetReq.setBlock(true);
            final CountDownLatch doneSignal1 = new CountDownLatch(1);
            waitForAccounts(Arrays.asList(mbox1.getAccountId(), mbox2.getAccountId()), doneSignal1, waitSetReq, "testBlockingAdminAddAccount - 3.5", false);
            try {
                doneSignal1.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Wait interrupted.");
            }
            assertTrue("callback3.5 was not triggered.", cbCalled.get());
            assertTrue(failureMessage, success.get());
            signalled+=numSignalledAccounts.intValue();
        }
        assertEquals("Should signal 2 accounts in total", 2, signalled);
        //4th request
        ZimbraLog.test.info("Sending 4th AdminWaitSetRequest");
        success.set(false);
        failureMessage = null;
        cbCalled.set(false);
        final CountDownLatch doneSignal4 = new CountDownLatch(1);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastSeqNum);
        waitSetReq.setBlock(true);

        //only second account should get signaled this time
        waitForAccounts(Arrays.asList(mbox2.getAccountId()), doneSignal4, waitSetReq, "testBlockingAdminAddAccount - 4", false);
        TestUtil.addMessage(mbox2, subject);
        try {
            doneSignal4.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted.");
        }
        assertTrue("callback4 was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
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
        assertNotNull(resp);
        waitSetId = resp.getWaitSetId();
        int seq = resp.getSequence();
        assertNotNull(waitSetId);

        WaitSetRequest waitSetReq = new WaitSetRequest(waitSetId, Integer.toString(seq));
        waitSetReq.setBlock(true);

        final CountDownLatch doneSignal = new CountDownLatch(1);
        mbox.getTransport().invokeAsync(JaxbUtil.jaxbToElement(waitSetReq), new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                cbCalled.set(true);
                int respCode = response.getStatusLine().getStatusCode();
                success.set((respCode == 200));
                if(!success.get()) {
                    failureMessage = "Response code " + respCode;
                }
                if(success.get()) {
                    Element envelope;
                    try {
                        envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                        Element doc = proto.getBodyElement(envelope);
                        ZimbraLog.test.info(new String(doc.toUTF8(), "UTF-8"));
                        WaitSetResponse wsResp = (WaitSetResponse) JaxbUtil.elementToJaxb(doc);
                        success.set((Integer.parseInt(wsResp.getSeqNo()) > 0));
                        if(!success.get()) {
                            failureMessage = "wrong squence number. Sequence #" + wsResp.getSeqNo();
                        }
                        if(success.get()) {
                            success.set((wsResp.getSignalledAccounts().size() == 1));
                            if(!success.get()) {
                                failureMessage = "wrong number of signaled accounts " + wsResp.getSignalledAccounts().size();
                            }
                        }
                        if(success.get()) {
                            success.set(wsResp.getSignalledAccounts().get(0).getId().equalsIgnoreCase(accId));
                            if(!success.get()) {
                                failureMessage = "signaled wrong account " +  wsResp.getSignalledAccounts().get(0).getId();
                            }
                        }
                    } catch (UnsupportedOperationException | IOException | ServiceException e) {
                        fail(e.getMessage());
                    }
                    try { Thread.sleep(100); } catch (Exception e) {}
                }
                doneSignal.countDown();
            }

            @Override
            public void failed(final Exception ex) {
                ZimbraLog.test.error("request :: failed ", ex);
                success.set(false);
                failureMessage = ex.getMessage();
                doneSignal.countDown();
            }

            @Override
            public void cancelled() {
                ZimbraLog.test.info("request :: cancelled");
                success.set(false);
                failureMessage = "request :: cancelled";
                doneSignal.countDown();
            }
        });

        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted. ");
        }
        assertTrue("callback was not triggered.", cbCalled.get());
        assertTrue(failureMessage, success.get());
    }

    private void waitForAccounts(List<String> accountIds, CountDownLatch doneSignal, Object req, String caller, boolean allAccounts) throws Exception {
        soapProv.invokeJaxbAsync(req,  new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                ZimbraLog.test.info("waitForAccounts %s :: completed", caller);
                cbCalled.set(true);
                int respCode = response.getStatusLine().getStatusCode();
                success.set((respCode == 200));
                if(!success.get()) {
                    failureMessage = "response code " + respCode;
                } else {
                    Element envelope;
                    try {
                        envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                        Element doc = proto.getBodyElement(envelope);
                        AdminWaitSetResponse wsResp = (AdminWaitSetResponse) JaxbUtil.elementToJaxb(doc);
                        ZimbraLog.test.info(new String(doc.toUTF8(), "UTF-8"));
                        ZimbraLog.test.info("Setting numSignalledAccounts to %d", wsResp.getSignalledAccounts().size());
                        numSignalledAccounts.set(wsResp.getSignalledAccounts().size());
                        lastSeqNum = wsResp.getSeqNo();

                        for(int i=0; i < wsResp.getSignalledAccounts().size(); i++) {
                            ZimbraLog.test.info("signalled account " + wsResp.getSignalledAccounts().get(i).getId());
                        }

                        //check that we are getting expected signals
                        if(allAccounts) {
                            //AllAccountsWaitSet uses encoded redolog commit sequence instead of linear sequence number
                            CommitId commitSeq = CommitId.decodeFromString(wsResp.getSeqNo());
                            success.set(commitSeq.getRedoSeq() > 0);
                        } else {
                            success.set((Integer.parseInt(wsResp.getSeqNo()) > 0));
                        }
                        if(!success.get()) {
                            failureMessage = "wrong squence number. Sequence #" + wsResp.getSeqNo();
                        } else {
                            success.set((wsResp.getSignalledAccounts().size() <= accountIds.size()));
                            if(!success.get()) {
                                failureMessage = "wrong number of signaled accounts " + wsResp.getSignalledAccounts().size();
                            } else if(!allAccounts) {
                                //AllAccountsWaitSet may receive notifications for other accounts that are being changed outside of this test run
                                //e.g.: admin account receiving system notifications
                                for(int i=0; i < wsResp.getSignalledAccounts().size(); i++) {
                                    success.set(accountIds.contains(wsResp.getSignalledAccounts().get(i).getId()));
                                    if(!success.get()) {
                                        failureMessage = "received notificaiton for an unexpected account " + wsResp.getSignalledAccounts().get(i).getId();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (UnsupportedOperationException | IOException | ServiceException e) {
                        success.set(false);
                        failureMessage = e.getMessage();
                        ZimbraLog.test.error(e);
                    }
                }
                if(!failureMessage.isEmpty()) {
                    ZimbraLog.test.error(failureMessage);
                }
                try { Thread.sleep(100); } catch (Exception e) {}
                doneSignal.countDown();
            }

            @Override
            public void failed(final Exception ex) {
                ZimbraLog.test.error("waitForAccounts :: failed ", ex);
                success.set(false);
                failureMessage = ex.getMessage();
                doneSignal.countDown();
            }

            @Override
            public void cancelled() {
                ZimbraLog.test.info("waitForAccounts :: cancelled");
                success.set(false);
                failureMessage = "waitForAccounts :: cancelled";
                doneSignal.countDown();
            }
        });
    }
}
