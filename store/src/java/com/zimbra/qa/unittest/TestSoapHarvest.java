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
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.TrustedStatus;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class TestSoapHarvest {
    @Rule
    public TestName testInfo = new TestName();
    private static String AUTH_USER_NAME = null;
    private static String TARGET_USER_NAME = null;
    private static String ADMIN_USER_NAME = null;
    private static final String NAME_PREFIX = TestSoapHarvest.class.getSimpleName();

    @Before
    public void setUp() throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        AUTH_USER_NAME = prefix + "user1";
        TARGET_USER_NAME = prefix + "user3";
        ADMIN_USER_NAME = prefix + "admin";
        cleanUp();
        TestUtil.createAccount(AUTH_USER_NAME);
        TestUtil.createAccount(TARGET_USER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception{
        TestUtil.deleteAccountIfExists(AUTH_USER_NAME);
        TestUtil.deleteAccountIfExists(TARGET_USER_NAME);
        TestUtil.deleteAccountIfExists(ADMIN_USER_NAME);
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
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost method = new HttpPost(TestUtil.getSoapUrl() + (useGetInfoReq ? "GetInfoRequest" : "NoOpRequest"));
        method.setEntity(new StringEntity(useGetInfoReq ? getInfoRequest(userId, authToken, byAccountId) : getNoOpRequest(userId, authToken, byAccountId), "application/soap+xml", "UTF-8"));
        HttpResponse response = HttpClientUtil.executeMethod(client, method);
        int respCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(expectedCode, respCode);
        return EntityUtils.toString(response.getEntity());
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
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        String adminPasswd = new BigInteger(130, new SecureRandom()).toString(32);
        TestUtil.createAccount(ADMIN_USER_NAME, adminPasswd, attrs);
        ZMailbox mbox = TestUtil.getZMailbox(ADMIN_USER_NAME, adminPasswd, null, TrustedStatus.not_trusted);
        String authToken = mbox.getAuthToken().getValue();
        String response = sendReq(ADMIN_USER_NAME, authToken, 200, false);
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
        String response = sendReq("admin", authToken, 200, false);
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
