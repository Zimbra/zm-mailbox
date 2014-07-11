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
import java.util.List;

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
import com.zimbra.soap.mail.message.BrowseRequest;
import com.zimbra.soap.mail.message.BrowseResponse;
import com.zimbra.soap.mail.type.BrowseData;

public class TestJaxb extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestJaxb.class.getSimpleName();

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
        "<authTokenControl voidOnExpired=\"1\"/>" +
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

    private Element sendReqExpectingFail(String requestBody, String requestCommand, int expectedRespCode)
    throws HttpException, IOException, ServiceException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(TestUtil.getSoapUrl() + requestCommand);
        post.setRequestEntity(new StringRequestEntity(requestBody, "application/soap+xml", "UTF-8"));
        int respCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals(expectedRespCode, respCode);
        return W3cDomUtil.parseXML(post.getResponseBodyAsStream());
    }

    public BrowseResponse doBrowseRequest(BrowseRequest browseRequest) throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(browseRequest, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.info(doc.getRootElement().asXML());
        return (BrowseResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "BrowseRequest");
    }

    @Test
    public void testBrowseRequestDomains() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("domains" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
    }

    @Test
    public void testBrowseRequestAttachments() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("attachments" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
    }

    @Test
    public void testBrowseRequestObjects() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("objects" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
    }

    public Element doBadBrowseRequest(BrowseRequest browseRequest) throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(browseRequest, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.info(doc.getRootElement().asXML());
        return sendReqExpectingFail(envelope(authToken, doc.getRootElement().asXML()), "BrowseRequest", 500);
    }

    static String BAD_REGEX = ".*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*822";
    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestDomainsBadRegex() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("domains" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }

    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestObjectsBadRegex() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("objects" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }

    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestAttachmentsBadRegex() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("attachments" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }
}
