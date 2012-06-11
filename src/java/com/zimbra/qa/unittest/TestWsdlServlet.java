/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.IOException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

import junit.framework.TestCase;

public class TestWsdlServlet extends TestCase {

    private static final String wsdlUrlBase = "/service/wsdl/";

    String doWsdlServletRequest(String wsdlUrl, boolean admin, int expectedCode) throws Exception{
        Server localServer = Provisioning.getInstance().getLocalServer();

        String protoHostPort;
        if (admin)
            protoHostPort = "https://localhost:" + localServer.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        else
            protoHostPort = "http://localhost:" + localServer.getIntAttr(Provisioning.A_zimbraMailPort, 0);

        String url = protoHostPort + wsdlUrl;

        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);

        try {
            int respCode = HttpClientUtil.executeMethod(client, method);
            int statusCode = method.getStatusCode();
            String statusLine = method.getStatusLine().toString();

            ZimbraLog.test.debug("respCode=" + respCode);
            ZimbraLog.test.debug("statusCode=" + statusCode);
            ZimbraLog.test.debug("statusLine=" + statusLine);

            assertTrue("Response code", respCode == expectedCode);
            assertTrue("Status code", statusCode == expectedCode);

            Header[] respHeaders = method.getResponseHeaders();
            for (int i=0; i < respHeaders.length; i++) {
                String header = respHeaders[i].toString();
                ZimbraLog.test.debug("ResponseHeader:" + header);
            }

            String respBody = method.getResponseBodyAsString();
            // ZimbraLog.test.debug("Response Body:" + respBody);
            return respBody;

        } catch (HttpException e) {
            fail("Unexpected HttpException" + e);
            throw e;
        } catch (IOException e) {
            fail("Unexpected IOException" + e);
            throw e;
        } finally {
            method.releaseConnection();
        }
    }

    public void testWsdlServletZimbraServicesWsdl() throws Exception {
        String body = doWsdlServletRequest(wsdlUrlBase + "ZimbraService.wsdl", false, HttpStatus.SC_OK);
        assertTrue("Body contains expected string", body.contains("wsdl:service name="));
    }

    public void testWsdlServletZimbraUserServicesWsdl() throws Exception {
        String body = doWsdlServletRequest(wsdlUrlBase + "ZimbraUserService.wsdl", false, HttpStatus.SC_OK);
        assertTrue("Body contains expected string", body.contains("wsdl:service name="));
    }

    public void testWsdlServletZimbraAdminServicesWsdl() throws Exception {
        String body = doWsdlServletRequest(wsdlUrlBase + "ZimbraAdminService.wsdl", true, HttpStatus.SC_OK);
        assertTrue("Body contains expected string", body.contains("wsdl:service name="));
    }

    public void testWsdlServletXsd() throws Exception {
        String body = doWsdlServletRequest(wsdlUrlBase + "zimbraAccount.xsd", false, HttpStatus.SC_OK);
        assertTrue("Body contains expected string", body.contains(":schema>"));
    }

    public void testWsdlServletInvalidPathForWsdl() throws Exception {
        doWsdlServletRequest(wsdlUrlBase + "NonExistentService.wsdl", true, HttpStatus.SC_NOT_FOUND);
    }

    public void testWsdlServletInvalidPathForXsd() throws Exception {
        doWsdlServletRequest(wsdlUrlBase + "NonExistent.xsd", true, HttpStatus.SC_NOT_FOUND);
        doWsdlServletRequest(wsdlUrlBase + "fred/NonExistent.xsd", true, HttpStatus.SC_NOT_FOUND);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        TestUtil.cliSetup();
        try {
            TestUtil.runTest(TestWsdlServlet.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
