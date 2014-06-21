/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import java.io.IOException;

import javax.xml.bind.Marshaller;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
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

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestWaitSetRequest.class.getSimpleName();

    private Marshaller marshaller;

    @Override
    public void setUp() throws Exception {
        cleanUp();
        marshaller = JaxbUtil.createMarshaller();
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
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

    private Object sendReq(String requestBody, String requestCommand) throws HttpException, IOException, ServiceException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(TestUtil.getSoapUrl() + requestCommand);
        post.setRequestEntity(new StringRequestEntity(requestBody, "application/soap+xml", "UTF-8"));
        int respCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals(200, respCode);
        Element envelope = W3cDomUtil.parseXML(post.getResponseBodyAsStream());
        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
        Element doc = proto.getBodyElement(envelope);
        return JaxbUtil.elementToJaxb(doc);
    }

    @Test
    public void testWaitSetRequest() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        String authToken = mbox.getAuthToken().getValue();
        CreateWaitSetRequest req = new CreateWaitSetRequest("all");
        WaitSetAddSpec add = new WaitSetAddSpec();
        add.setId(mbox.getAccountInfo(false).getId());
        req.addAccount(add);
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(req, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.info(doc.getRootElement().asXML());
        CreateWaitSetResponse createResp = (CreateWaitSetResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "CreateWaitSetRequest");

        String waitSetId = createResp.getWaitSetId();
        Assert.assertNotNull(waitSetId);

        WaitSetRequest waitSet = new com.zimbra.soap.mail.message.WaitSetRequest(waitSetId, "0");
        dr = new DocumentResult();
        marshaller.marshal(waitSet, dr);
        doc = dr.getDocument();

        WaitSetResponse wsResp = (WaitSetResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "WaitSetRequest");
        Assert.assertEquals("0", wsResp.getSeqNo());
        String subject = NAME_PREFIX + " test wait set request 1";
        TestUtil.addMessageLmtp(subject, USER_NAME, "user999@example.com");
        try { Thread.sleep(500); } catch (Exception e) {}
        wsResp = (WaitSetResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "WaitSetRequest");
        Assert.assertFalse(wsResp.getSeqNo().equals("0"));

    }
}
