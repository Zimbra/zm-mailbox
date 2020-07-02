/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

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

        HttpClient client = HttpClientBuilder.create().build();
        HttpRequestBase method = new HttpGet(url);

        try {
            HttpResponse response = HttpClientUtil.executeMethod(client, method);
            int statusCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().toString();

            ZimbraLog.test.debug("respCode=" + response.getStatusLine().getStatusCode());
            ZimbraLog.test.debug("statusCode=" + statusCode);
            ZimbraLog.test.debug("statusLine=" + statusLine);

            assertTrue("Response code", response.getStatusLine().getStatusCode() == expectedCode);
            assertTrue("Status code", statusCode == expectedCode);

            Header[] respHeaders = response.getAllHeaders();
            for (int i=0; i < respHeaders.length; i++) {
                String header = respHeaders[i].toString();
                ZimbraLog.test.debug("ResponseHeader:" + header);
            }

            String respBody = EntityUtils.toString(response.getEntity());
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
