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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.junit.Assert;
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
import com.zimbra.soap.mail.message.CreateWaitSetRequest;
import com.zimbra.soap.mail.message.CreateWaitSetResponse;
import com.zimbra.soap.mail.message.WaitSetRequest;
import com.zimbra.soap.mail.message.WaitSetResponse;
import com.zimbra.soap.type.WaitSetAddSpec;

import junit.framework.TestCase;

public class TestWaitSetRequest extends TestCase {

    private static final String NAME_PREFIX = TestWaitSetRequest.class.getSimpleName();
    Account acc1 = null;
    Account acc2 = null;
    private boolean cbCalled = false;
    private String lastKnownSeq = "0";
    private Marshaller marshaller;
    private String waitSetId = null;
    private String failureMessage = null;
    private boolean success = false;
    private final SoapProvisioning soapProv = new SoapProvisioning();
    @Override
    public void setUp() throws Exception {
        cleanUp();
        marshaller = JaxbUtil.createMarshaller();
        soapProv.soapSetURI(TestUtil.getAdminSoapUrl());
        soapProv.soapZimbraAdminAuthenticate();
    }

    @Override
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
        lastKnownSeq = "0";
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
        ZimbraLog.test.info("Sending request " + requestBody);
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
    }

    @Test
    public void testFolderInterestSyncWaitSetRequest() throws Exception {
        String user1Name = "testFISyncWaitSetRequest_user1";
        acc1 = TestUtil.createAccount(user1Name);
        ZMailbox mbox = TestUtil.getZMailbox(user1Name);
        String authToken = mbox.getAuthToken().getValue();
        String adminAuthToken = TestUtil.getAdminSoapTransport().getAuthToken().getValue();
        ZFolder myFolder = TestUtil.createFolder(mbox, "funFolder");
        Set<Integer> folderInterest = Sets.newHashSet();
        folderInterest.add(myFolder.getFolderIdInOwnerMailbox());

        /* initially only interested in funFolder */
        CreateWaitSetResponse resp = createWaitSet(mbox.getAccountInfo(false).getId(), authToken, folderInterest);
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

        /* interested in funFolder AND inbox */
        folderInterest.add(Integer.valueOf(Mailbox.ID_FOLDER_INBOX));
        waitSet.addUpdateAccount(createWaitSetAddSpec(mbox.getAccountId(), authToken, folderInterest));
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertTrue(wsResp.getSeqNo().equals("0"));
        Assert.assertEquals("Number of signalled accounts", 0, wsResp.getSignalledAccounts().size());

        qwsResp = (QueryWaitSetResponse) sendReq(qwsReq, adminAuthToken, TestUtil.getAdminSoapUrl());

        subject = NAME_PREFIX + " test wait set request 2";
        TestUtil.addMessageLmtp(subject, user1Name, "user999@example.com");
        TestUtil.waitForMessages(mbox, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        waitSet = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, Integer.toString(seq));
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, jaxbToString(waitSet),
                "urn:zimbra"), TestUtil.getSoapUrl() + "WaitSetRequest");
        Assert.assertFalse(wsResp.getSeqNo().equals("0"));
        Assert.assertEquals("Number of signalled accounts", 1, wsResp.getSignalledAccounts().size());
    }

    private CreateWaitSetResponse createWaitSet(String accountId, String authToken) throws Exception {
        return createWaitSet(accountId, authToken, null);
    }

    private CreateWaitSetResponse createWaitSet(String accountId, String authToken, Set<Integer>folderInterest)
    throws Exception {
        CreateWaitSetRequest req = new CreateWaitSetRequest("all", (Boolean) null);
        req.addAccount(createWaitSetAddSpec(accountId, authToken, folderInterest));
        return (CreateWaitSetResponse) sendReq(envelope(authToken, jaxbToString(req), "urn:zimbra"),
                TestUtil.getSoapUrl() + "CreateWaitSetRequest");
    }

    private WaitSetAddSpec createWaitSetAddSpec(String accountId, String authToken, Set<Integer>folderInterest) {
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
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastKnownSeq);
        waitSetReq.setBlock(true);

        //add user2 to the same waitset
        WaitSetAddSpec add = new WaitSetAddSpec();
        add.setId(mbox2.getAccountId());
        waitSetReq.addAddAccount(add);

        final CountDownLatch doneSignal2 = new CountDownLatch(1);

        ZimbraLog.test.info("Should signal accounts %s and %s", mbox1.getAccountId(), mbox2.getAccountId());
        subject = NAME_PREFIX + " test wait set request 2";

        //only one account will be signaled this time, because user2 will be added to WS after the WS gets notified
        waitForAccounts(Arrays.asList(mbox1.getAccountId()), doneSignal2, waitSetReq, "testBlockingAdminAddAccount - 2");
        TestUtil.addMessage(mbox2, subject);
        TestUtil.addMessage(mbox1, subject);

        try {
            doneSignal2.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        Assert.assertTrue("callback2 was not triggered.", cbCalled);
        Assert.assertTrue(failureMessage, success);

        //3rd request
        ZimbraLog.test.info("Sending 3rd AdminWaitSetRequest");
        success = false;
        failureMessage = null;
        cbCalled = false;
        final CountDownLatch doneSignal3 = new CountDownLatch(1);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastKnownSeq);
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

        //4th request
        ZimbraLog.test.info("Sending 4rd AdminWaitSetRequest");
        success = false;
        failureMessage = null;
        cbCalled = false;
        final CountDownLatch doneSignal4 = new CountDownLatch(1);
        waitSetReq = new AdminWaitSetRequest(waitSetId, lastKnownSeq);
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
                        for(int i=0; i < wsResp.getSignalledAccounts().size(); i++) {
                            ZimbraLog.test.info("signalled account " + wsResp.getSignalledAccounts().get(i).getId());
                        }
                        success = (Integer.parseInt(wsResp.getSeqNo()) > 0);
                        if(!success) {
                            failureMessage = "wrong squence number. Sequence #" + wsResp.getSeqNo();
                        }
                        if(success) {
                            success = (wsResp.getSignalledAccounts().size() == accountIds.size());
                            if(!success) {
                                failureMessage = "wrong number of signaled accounts " + wsResp.getSignalledAccounts().size();
                            }
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
                        lastKnownSeq = wsResp.getSeqNo();
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