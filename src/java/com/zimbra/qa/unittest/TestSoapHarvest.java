/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
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
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class TestSoapHarvest extends TestCase {

    private static final String AUTH_USER_NAME = "user1";
    private static final String TARGET_USER_NAME = "user3";


    @Override
    public void setUp()
    throws Exception {
    }

    private String getNoOpRequest(String userId, String authToken, boolean byAccountId) {
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<soap:Header>"+
        "<context xmlns=\"urn:zimbra\">"+
        "<userAgent name=\"Zimbra Junit\" version=\"0.0\"/>"+
        (authToken != null ? "<authToken>" + authToken + "</authToken>" : "") +
        "<nosession/>"+
        "<account by=\"" + (byAccountId ? "id" : "name") + "\">"+
        userId+
        "</account>"+
        "</context>"+
        "</soap:Header>"+
        "<soap:Body>"+
        "<NoOpRequest xmlns=\"urn:zimbraMail\" />"+
        "</soap:Body>"+
        "</soap:Envelope>";
    }

    private String getInfoRequest(String userId, String authToken, boolean byAccountId) {
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<soap:Header>"+
        "<context xmlns=\"urn:zimbra\">"+
        "<userAgent name=\"Zimbra Junit\" version=\"0.0\"/>"+
        (authToken != null ? "<authToken>" + authToken + "</authToken>" : "") +
        "<nosession/>"+
        "<account by=\"" + (byAccountId ? "id" : "name") + "\">"+
        userId+
        "</account>"+
        "</context>"+
        "</soap:Header>"+
        "<soap:Body>"+
        "<GetInfoRequest xmlns=\"urn:zimbraAccount\" sections=\"mbox\"/>"+
        "</soap:Body>"+
        "</soap:Envelope>";
    }

    private String sendReq(String userId, String authToken, int expectedCode, boolean useGetInfoReq) throws HttpException, IOException {
        return sendReq(userId, authToken, expectedCode, useGetInfoReq, false);
    }

    private String sendReq(String userId, String authToken, int expectedCode, boolean useGetInfoReq, boolean byAccountId) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(TestUtil.getSoapUrl() + (useGetInfoReq ? "GetInfoRequest" : "NoOpRequest"));
        method.setRequestEntity(new StringRequestEntity(useGetInfoReq ? getInfoRequest(userId, authToken, byAccountId) : getNoOpRequest(userId, authToken, byAccountId), "application/soap+xml", "UTF-8"));
        int respCode = HttpClientUtil.executeMethod(client, method);
        Assert.assertEquals(expectedCode, respCode);
        return method.getResponseBodyAsString();
    }

    @Test
    public void testHarvestNoAuth() throws Exception {

        ZMailbox mbox = TestUtil.getZMailbox(AUTH_USER_NAME);
        //make sure user1 exists
        Assert.assertNotNull(mbox);

        String response = sendReq(AUTH_USER_NAME, null, 500, false);
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

        response = sendReq(bogusUserId, null, 500, false);
        Assert.assertTrue(response.indexOf("<Code>service.AUTH_REQUIRED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>no valid authtoken present</soap:Text>") > -1);

    }

    @Test
    public void testHarvestDelegated() throws Exception {
        //test an operation that implements delegation
        ZMailbox mbox = TestUtil.getZMailbox(AUTH_USER_NAME);
        //make sure user1 exists
        Assert.assertNotNull(mbox);

        String authToken = mbox.getAuthToken().getValue();
        String response = sendReq(AUTH_USER_NAME, authToken, 200, true);
        //make sure auth token works for normal request

        String userId = TARGET_USER_NAME;
        mbox = TestUtil.getZMailbox(userId);
        //make sure TARGET_USER_NAME exists
        Assert.assertNotNull(mbox);

        //note this fails if you've shared anything from TARGET_USER_NAME to user1. works fine in clean setup
        response = sendReq(userId, authToken, 500, true);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(Provisioning.getInstance().get(AccountBy.name, userId).getId()));

        response = sendReq(userId, authToken, 500, false);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(Provisioning.getInstance().get(AccountBy.name, userId).getId()));

        //make sure bogus does *not* exist
        userId = "bogus";
        try {
            TestUtil.getZMailbox(userId);
            Assert.fail("user "+userId+" should not exist");
        } catch (Exception e) {
            //expected
        }

        response = sendReq(userId, authToken, 500, true);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
    }

    @Test
    public void testErrorResponses() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(AUTH_USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        String userId = TARGET_USER_NAME;
        Account account = Provisioning.getInstance().get(AccountBy.name, userId);

        //note this fails if you've shared anything from TARGET_USER_NAME to user1. works fine in clean setup
        String response = sendReq(userId, authToken, 500, true, false);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(account.getId()));

        response = sendReq(account.getId(), authToken, 500, true, true);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account name
        Assert.assertTrue(!response.contains(userId));

    }

    @Test
    public void testHarvestDelegatedNoOp() throws Exception {
        //test an operation that does not implement delegation
        ZMailbox mbox = TestUtil.getZMailbox(AUTH_USER_NAME);
        //make sure user1 exists
        Assert.assertNotNull(mbox);

        String authToken = mbox.getAuthToken().getValue();
        String response = sendReq(AUTH_USER_NAME, authToken, 200, false);
        //make sure auth token works for normal request

        String userId = TARGET_USER_NAME;
        mbox = TestUtil.getZMailbox(userId);
        //make sure TARGET_USER_NAME exists
        Assert.assertNotNull(mbox);

        //note this fails if you've shared anything from TARGET_USER_NAME to user1.
        response = sendReq(userId, authToken, 500, true);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(Provisioning.getInstance().get(AccountBy.name, userId).getId()));

        response = sendReq(userId, authToken, 500, false);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(Provisioning.getInstance().get(AccountBy.name, userId).getId()));

        //make sure bogus does *not* exist
        userId = "bogus";
        try {
            TestUtil.getZMailbox(userId);
            Assert.fail("user "+userId+" should not exist");
        } catch (Exception e) {
            //expected
        }

        response = sendReq(userId, authToken, 500, false);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
    }

    @Test
    public void testAdminDelegation() throws Exception {
        //admin account non-admin auth token; effectively no rights
        ZMailbox mbox = TestUtil.getZMailbox("admin");

        String authToken = mbox.getAuthToken().getValue();
        String response = sendReq(AUTH_USER_NAME, authToken, 500, false);

        String userId = TARGET_USER_NAME;
        mbox = TestUtil.getZMailbox(userId);
        //make sure TARGET_USER_NAME exists
        Assert.assertNotNull(mbox);

        //send both NoOp and GetInfo; neither should work here
        response = sendReq(userId, authToken, 500, true);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(Provisioning.getInstance().get(AccountBy.name, userId).getId()));

        response = sendReq(userId, authToken, 500, false);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
        //make sure we're not returning account id
        Assert.assertTrue(!response.contains(Provisioning.getInstance().get(AccountBy.name, userId).getId()));

        //make sure bogus does *not* exist
        userId = "bogus";
        try {
            TestUtil.getZMailbox(userId);
            Assert.fail("user "+userId+" should not exist");
        } catch (Exception e) {
            //expected
        }

        response = sendReq(userId, authToken, 500, false);
        Assert.assertTrue(response.indexOf("<Code>service.PERM_DENIED</Code>") > -1);
        Assert.assertTrue(response.indexOf("<soap:Text>permission denied: can not access account") > -1);
    }

    @Test
    public void testAdminAuthToken() throws Exception {
        //admin account admin auth token
        ZMailbox mbox = TestUtil.getZMailboxAsAdmin("admin");

        String authToken = mbox.getAuthToken().getValue();
        String response = sendReq(AUTH_USER_NAME, authToken, 200, false);
        //make sure auth token works for normal request

        String userId = TARGET_USER_NAME;
        mbox = TestUtil.getZMailbox(userId);
        //make sure TARGET_USER_NAME exists
        Assert.assertNotNull(mbox);

        //send both NoOp and GetInfo; both should work for admin
        response = sendReq(userId, authToken, 200, false);
        response = sendReq(userId, authToken, 200, true);

        //make sure bogus does *not* exist
        userId = "bogus";
        try {
            TestUtil.getZMailbox(userId);
            Assert.fail("user "+userId+" should not exist");
        } catch (Exception e) {
            //expected
        }

        response = sendReq(userId, authToken, 500, false);
        //admin is correctly allowed to receive NO_SUCH_ACCOUNT
        Assert.assertTrue(response.indexOf("<Code>account.NO_SUCH_ACCOUNT</Code>") > -1);
    }
}
