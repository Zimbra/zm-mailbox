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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.Marshaller;

import junit.framework.TestCase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.CreateWaitSetRequest;
import com.zimbra.soap.mail.message.CreateWaitSetResponse;
import com.zimbra.soap.mail.message.WaitSetRequest;
import com.zimbra.soap.mail.message.WaitSetResponse;
import com.zimbra.soap.type.WaitSetAddSpec;

public class TestWaitSetRequest extends TestCase {

    private static final String NAME_PREFIX = TestWaitSetRequest.class.getSimpleName();
    private static final String USER_NAME = NAME_PREFIX +"_user1";
    private boolean success = false;
    private boolean cbCalled = false;
    private String seqNo = "";
    private Marshaller marshaller;

    @Override
    public void setUp() throws Exception {
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        marshaller = JaxbUtil.createMarshaller();
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
        seqNo = "";
        cbCalled = false;
        success = false;
    }

    private String envelope(String authToken, String requestBody) {
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<soap:Header>"+
        "<context xmlns=\"urn:zimbra\">"+
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

    private Object sendReq(String requestBody, String requestCommand) throws IOException, ServiceException {
        CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        HttpPost post = new HttpPost(TestUtil.getSoapUrl() + requestCommand);
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

    @Test
    public void testWaitSetRequest() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        String waitSetId = createWaitSet(mbox.getAccountInfo(false).getId(), authToken);
        Assert.assertNotNull(waitSetId);

        WaitSetRequest waitSet = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, "0");
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(waitSet, dr);
        Document doc = dr.getDocument();
        WaitSetResponse wsResp = (WaitSetResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "WaitSetRequest");
        Assert.assertEquals("0", wsResp.getSeqNo());
        
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, USER_NAME, "user999@example.com");
        try { Thread.sleep(500); } catch (Exception e) {}
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "WaitSetRequest");
        Assert.assertFalse(wsResp.getSeqNo().equals("0"));
    }

    private String createWaitSet(String accountId, String authToken) throws Exception {
        CreateWaitSetRequest req = new CreateWaitSetRequest("all");
        WaitSetAddSpec add = new WaitSetAddSpec();
        add.setId(accountId);
        req.addAccount(add);
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(req, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.info(doc.getRootElement().asXML());
        CreateWaitSetResponse resp = (CreateWaitSetResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "CreateWaitSetRequest");
        String waitSetId = resp.getWaitSetId();
        return waitSetId;
    }

    @Test
    public void testBlockingWaitSetRequest() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        String waitSetId = createWaitSet(mbox.getAccountInfo(false).getId(), authToken);
        Assert.assertNotNull(waitSetId);
        
        WaitSetRequest waitSetReq = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, "0");
        waitSetReq.setBlock(true);
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(waitSetReq, dr);
        Document doc = dr.getDocument();
        String requestBody = envelope(authToken, doc.getRootElement().asXML());
        CloseableHttpAsyncClient httpClient = ZimbraHttpClientManager.getInstance().getInternalAsyncHttpClient();
        HttpPost post = new HttpPost(TestUtil.getSoapUrl() + "WaitSetRequest");
        post.setHeader("Content-Type", "application/soap+xml");
        HttpEntity reqEntity = new ByteArrayEntity(requestBody.getBytes("UTF-8"));
        post.setEntity(reqEntity);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        httpClient.execute(post, new FutureCallback<HttpResponse>() {
            public void completed(final HttpResponse response) {
                cbCalled = true;
                int respCode = response.getStatusLine().getStatusCode();
                Assert.assertEquals(200, respCode);
                Element envelope;
                try {
                    envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                    SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                    Element doc = proto.getBodyElement(envelope);
                    WaitSetResponse wsResp = (WaitSetResponse) JaxbUtil.elementToJaxb(doc);
                    seqNo = wsResp.getSeqNo();
                    seqNo = wsResp.getSeqNo();
                } catch (UnsupportedOperationException | IOException | ServiceException e) {
                    Assert.fail(e.getMessage());
                }
                try { Thread.sleep(500); } catch (Exception e) {}
                doneSignal.countDown();
            }

            public void failed(final Exception ex) {
                cbCalled = true;
                doneSignal.countDown();
            }

            public void cancelled() {
                cbCalled = true;
                doneSignal.countDown();
            }
        });
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessage(mbox, subject);
        try {
            doneSignal.await(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted. Sequence # " + seqNo);
        }
        Assert.assertTrue("callback was not triggered. Sequence #" + seqNo, cbCalled);
        Assert.assertTrue("wrong squence number. Sequence #" + seqNo, Integer.parseInt(seqNo) > 0);
    }
}