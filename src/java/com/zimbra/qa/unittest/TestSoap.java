/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.client.ZAuthResult;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;

public class TestSoap
extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestSoap.class.getSimpleName();
    
    private String mOriginalSoapRequestMaxSize;
    private String mOriginalSoapExposeVersion;
    
    @Override
    public void setUp()
    throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        mOriginalSoapRequestMaxSize = server.getAttr(Provisioning.A_zimbraSoapRequestMaxSize, "");
        mOriginalSoapExposeVersion = server.getAttr(Provisioning.A_zimbraSoapExposeVersion, "");
        cleanUp();
    }
    
    public void testSoapRequestMaxSize()
    throws Exception {
        StringBuilder messageBody = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            messageBody.append("Morey Amsterdam was a great man.  Morey Amsterdam was not a sandwich.\r\n");
        }
        
        setSoapRequestMaxSize(100000);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.sendMessage(mbox, USER_NAME, NAME_PREFIX + " 1", messageBody.toString());
        
        setSoapRequestMaxSize(1000);
        try {
            TestUtil.sendMessage(mbox, USER_NAME, NAME_PREFIX + " 2", messageBody.toString());
            fail("SOAP request should not have succeeded.");
        } catch (SoapFaultException e) {
            assertTrue("Unexpected error: " + e.toString(), e.toString().contains("bytes set for zimbraSoapRequestMaxSize"));
        }
    }

    /**
     * Tests the AccountService version of GetInfoRequest (see bug 30010).
     */
    public void testAccountGetInfoRequest()
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.GET_VERSION_INFO_REQUEST);
        
        // Test with version exposed
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapExposeVersion, LdapConstants.LDAP_TRUE);
        Element response = transport.invoke(request);
        validateSoapVersionResponse(response);
        
        // Test with version not exposed
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapExposeVersion, LdapConstants.LDAP_FALSE);
        request = Element.create(transport.getRequestProtocol(), AccountConstants.GET_VERSION_INFO_REQUEST);
        try {
            response = transport.invoke(request);
            fail("GetInfoRequest should have failed");
        } catch (SoapFaultException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }
    
    /**
     * Tests the AdminService version of GetInfoRequest.
     */
    public void testAdminGetInfoRequest()
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        Element request = Element.create(transport.getRequestProtocol(), AdminConstants.GET_VERSION_INFO_REQUEST);
        Element response = transport.invoke(request);
        validateSoapVersionResponse(response);
    }
    
    private void validateSoapVersionResponse(Element response)
    throws ServiceException {
        assertEquals(AccountConstants.GET_VERSION_INFO_RESPONSE.getName(), response.getName());
        
        Element info = response.getElement(AccountConstants.E_VERSION_INFO_INFO);
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_DATE));
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_HOST));
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_RELEASE));
        assertNotNull(info.getAttribute(AccountConstants.A_VERSION_INFO_VERSION));
    }
    
    /**
     * Confirms that attrs and prefs are selected when specified in {@link ZMailbox} options.
     */
    public void testAuthRequest()
    throws Exception {
        // Test password auth.
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(USER_NAME);
        options.setAccountBy(AccountBy.name);
        options.setPassword(TestUtil.DEFAULT_PASSWORD);
        options.setUri(TestUtil.getSoapUrl());
        ZMailbox mbox = runAuthTest(options);
        ZAuthToken authToken = mbox.getAuthToken();
        
        // Test auth token auth.
        options = new ZMailbox.Options();
        options.setAuthToken(authToken);
        options.setAuthAuthToken(true);
        options.setUri(TestUtil.getSoapUrl());
        runAuthTest(options);
    }
    
    private ZMailbox runAuthTest(ZMailbox.Options options)
    throws Exception {
        List<String> attrNames = Arrays.asList(
            Provisioning.A_zimbraFeatureImportExportFolderEnabled,
            Provisioning.A_zimbraFeatureOutOfOfficeReplyEnabled);
        List<String> prefNames = Arrays.asList(
            Provisioning.A_zimbraPrefComposeFormat,
            Provisioning.A_zimbraPrefAutoSaveDraftInterval);
        
        options.setAttrs(attrNames);
        options.setPrefs(prefNames);
        ZMailbox mbox = ZMailbox.getMailbox(options);
        
        ZAuthResult auth = mbox.getAuthResult();
        Map<String, List<String>> attrs = auth.getAttrs();
        Map<String, List<String>> prefs = auth.getPrefs();
        
        assertEquals(attrNames.size(), attrs.size());
        assertEquals(prefNames.size(), prefs.size());
        
        for (String attrName : attrNames) {
            assertTrue(attrs.containsKey(attrName));
        }
        for (String prefName : prefNames) {
            assertTrue(prefs.containsKey(prefName));
        }
        return mbox;
    }
    
    public void testGetFolders()
    throws Exception {
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(USER_NAME);
        options.setAccountBy(AccountBy.name);
        options.setPassword(TestUtil.DEFAULT_PASSWORD);
        options.setUri(TestUtil.getSoapUrl());
        options.setNoSession(true);
        ZMailbox mbox = ZMailbox.getMailbox(options);
        
        ZFolder inbox = mbox.getFolderByPath("/Inbox");
        assertEquals("Inbox", inbox.getName());
    }

    /*
     * Useful for sending invalid XML.
     * @return Text of response - which could be either for a successful request or could be error text
     */
    private String doLowLevelRequest(URL url, String request)
    throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("content-type", "text/xml");
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.getOutputStream().write(request.getBytes("UTF-8"));

        conn.connect(); /* Send the request to the server */
        InputStream is;
        if (conn.getResponseCode() <= 400) {
            is = conn.getInputStream();
        } else { /* error from server */
            is = conn.getErrorStream();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder resp = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            resp.append(inputLine);
        }
        in.close();
        return resp.toString();
    }

    public void testBadXmlReqWantJSResp()
    throws IOException {
        StringBuilder req = new StringBuilder();
        req.append("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n")
            .append("<soap:Header>\n")
            .append("<context xmlns=\"urn:zimbra\">\n")
            .append("<format xmlns=\"\" type=\"js\"/>\n")
            .append("</context>\n")
            .append("</soap:Header>\n")
            .append("<soap:Body>\n")
            .append("<NoOpRequest session=<> xmlns=\"urn:zimbraMail\"/>\n") // invalid XML in this line
            .append("</soap:Body>\n")
            .append("</soap:Envelope>\n");
        String responseString = doLowLevelRequest(new URL(TestUtil.getSoapUrl() + "/WibbleRequest"), req.toString());
        assertTrue("Response should be a JSON fault", responseString.startsWith("{\"Body\":{\"Fault\":{\"Code\":") );
    }

    public void testBadXmlReqWantXmlResp()
    throws IOException {
        StringBuilder req = new StringBuilder();
        req.append("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n")
            .append("<soap:Header>\n")
            .append("<context xmlns=\"urn:zimbra\">\n")
            .append("<format xmlns=\"\" type=\"xml\"/>\n")   // Only diff from previous
            .append("</context>\n")
            .append("</soap:Header>\n")
            .append("<soap:Body>\n")
            .append("<NoOpRequest session=<> xmlns=\"urn:zimbraMail\"/>\n") // invalid XML in this line
            .append("</soap:Body>\n")
            .append("</soap:Envelope>\n");
        String responseString = doLowLevelRequest(new URL(TestUtil.getSoapUrl() + "/WibbleRequest"), req.toString());
        assertTrue("Response should be a SOAP 1.2 fault", responseString.startsWith(
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body><soap:Fault>") );
    }

    public void testBadSoap11XmlReq()
    throws IOException {
        StringBuilder req = new StringBuilder();
        req.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n")
            .append("  SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n")
            .append("<SOAO-ENV:Body>\n")
            .append("<comment>Missing end tag for body</comment>")
            .append("</SOAO-ENV:Envelope>\n");
        String responseString = doLowLevelRequest(new URL(TestUtil.getSoapUrl() + "/WibbleRequest"), req.toString());
        assertTrue("Response should be a SOAP 1.1 fault", responseString.startsWith(
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
                "<soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>parse error:"));
    }

    @Override
    public void tearDown()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapRequestMaxSize, mOriginalSoapRequestMaxSize);
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapExposeVersion, mOriginalSoapExposeVersion);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    private void setSoapRequestMaxSize(int numBytes)
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraSoapRequestMaxSize, Integer.toString(numBytes));
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSoap.class);
    }
}
