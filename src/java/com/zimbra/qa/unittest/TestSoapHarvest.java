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

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;

public class TestSoapHarvest extends TestCase {

    private static final String USER_NAME = "user1";

    @Override
    public void setUp()
    throws Exception {
    }

    private String getRequest(String userId, String authToken) {
        //no password or authtoken, so this will fail
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<soap:Header>"+
        "<context xmlns=\"urn:zimbra\">"+
        "<userAgent name=\"Zimbra Junit\" version=\"0.0\"/>"+
        (authToken != null ? "<authToken>" + authToken + "</authToken>" : "") +
        "<nosession/>"+
        "<account by=\"name\">"+
        userId+
        "</account>"+
        "</context>"+
        "</soap:Header>"+
        "<soap:Body>"+
        "<NoOpRequest xmlns=\"urn:zimbraMail\" />"+
        "</soap:Body>"+
        "</soap:Envelope>";
    }

    private String sendReq(String userId, String authToken, int expectedCode) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(TestUtil.getSoapUrl() + "NoOpRequest");
        method.setRequestEntity(new StringRequestEntity(getRequest(userId, authToken), "application/soap+xml", "UTF-8"));
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals(expectedCode, respCode);
        return method.getResponseBodyAsString();
    }

    @Test
    public void testHarvest() throws Exception {

        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        //make sure user1 exists
        Assert.assertNotNull(mbox);

        String response = sendReq(USER_NAME, null, 500);
        Assert.assertTrue(response.indexOf("<Code>service.AUTH_REQUIRED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>no valid authtoken present</soap:Text>") > -1);


        //now with non-existing account
        String bogusUserId = "bogus";
        try {
            TestUtil.getZMailbox(bogusUserId);
            Assert.fail("user "+bogusUserId+" should not exist");
        } catch (Exception e) {
            //expected
        }

        response = sendReq(bogusUserId, null, 500);
        Assert.assertTrue(response.indexOf("<Code>service.AUTH_REQUIRED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>no valid authtoken present</soap:Text>") > -1);

    }

    @Test
    public void testHarvestDelegated() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        //make sure user1 exists
        Assert.assertNotNull(mbox);

        String authToken = mbox.getAuthToken().getValue();
        String response = sendReq(USER_NAME, authToken, 200);
        //make sure auth token works for normal request

        String userId = "user4";
        mbox = TestUtil.getZMailbox(userId);
        //make sure user4 exists
        Assert.assertNotNull(mbox);

        //note this fails if you've shared anything from user1 to user4. works fine in clean setup
        response = sendReq(userId, authToken, 500);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account " + userId + "</soap:Text>") > -1);


        //make sure bogus does *not* exist
        userId = "bogus";
        try {
            TestUtil.getZMailbox(userId);
            Assert.fail("user "+userId+" should not exist");
        } catch (Exception e) {
            //expected
        }

        response = sendReq(userId, authToken, 500);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account " + userId + "</soap:Text>") > -1);
    }

}
