/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.PreAuthKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.ldap.LdapConstants;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestPreAuthServlet extends TestCase {
    
    private static String PRE_AUTH_URL = "/service/preauth";

    String setUpDomain() throws Exception {
        String domainName = TestUtil.getDomain();
        Domain domain = Provisioning.getInstance().get(Key.DomainBy.name, domainName);
        String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        Provisioning.getInstance().modifyAttrs(domain, attrs);
        return preAuthKey;
    }
    
    public static String genPreAuthUrl(String preAuthKey, String user, boolean admin, boolean shouldFail) throws Exception {
        
        HashMap<String,String> params = new HashMap<String,String>();
        String acctName = TestUtil.getAddress(user);
        String authBy = "name";
        long timestamp = System.currentTimeMillis();
        long expires = 0;
        
        params.put("account", acctName);
        params.put("by", authBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        
        if (admin)
            params.put("admin", "1");
        
        String preAuth = PreAuthKey.computePreAuth(params, preAuthKey);
        
        StringBuffer url = new StringBuffer(PRE_AUTH_URL + "?");
        url.append("account=" + acctName);
        url.append("&by=" + authBy);
        if (shouldFail) {
            // if doing negative testing, mess up with the timestamp so
            // it won't be computed to the same value at the server and 
            // the preauth will fail
            long timestampBad = timestamp + 10;
            url.append("&timestamp=" + timestampBad);
        } else    
            url.append("&timestamp=" + timestamp);
        url.append("&expires=" + expires);
        url.append("&preauth=" + preAuth);
        
        if (admin)
            url.append("&admin=1");
        
        return url.toString();
    }
    
    void doPreAuthServletRequest(String preAuthUrl, boolean admin) throws Exception{
        Server localServer = Provisioning.getInstance().getLocalServer();
        
        String protoHostPort;
        if (admin)
            protoHostPort = "https://localhost:" + localServer.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        else
            protoHostPort = "http://localhost:" + localServer.getIntAttr(Provisioning.A_zimbraMailPort, 0);
        
        String url = protoHostPort + preAuthUrl;
        
        HttpClient client = HttpClientBuilder.create().build();
        HttpRequestBase method = new HttpGet(url);
        
        try {
            HttpResponse response = HttpClientUtil.executeMethod(client, method);
            int statusCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().getReasonPhrase();
            
            System.out.println("statusCode=" + statusCode);
            System.out.println("statusLine=" + statusLine);
            /*
            System.out.println("Headers");
            Header[] respHeaders = method.getResponseHeaders();
            for (int i=0; i < respHeaders.length; i++) {
                String header = respHeaders[i].toString();
                System.out.println(header);
            }
            
            String respBody = method.getResponseBodyAsString();
            // System.out.println("respBody=" + respBody);
            */
            
        } catch (HttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            method.releaseConnection();
        }
    }
    
    private void doPreAuth(String userLocalPart, boolean admin, boolean shouldFail) throws Exception {
        String preAuthKey = setUpDomain();
        
        String preAuthUrl = genPreAuthUrl(preAuthKey, userLocalPart, admin, shouldFail);
        
        System.out.println("preAuthKey=" + preAuthKey);
        System.out.println("preAuth=" + preAuthUrl);
        
        doPreAuthServletRequest(preAuthUrl, admin);
    }
    
    public void testPreAuthServlet() throws Exception {
        doPreAuth("user1", false, false);
        doPreAuth("admin", true, false);
        doPreAuth("domainadmin", true, false);
        
        /*
        // test refer mde == always
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailReferMode, "always");
        prov.modifyAttrs(server, attrs);
        
        doPreAuth("user1", false, false);
        doPreAuth("admin", true, false);
        doPreAuth("domainadmin", true, false);
        
        // set refer mode back
        attrs.put(Provisioning.A_zimbraMailReferMode, "wronghost");
        prov.modifyAttrs(server, attrs);
        */
    }
    
    private Account dumpLockoutAttrs(String user) throws Exception {
        Account acct = Provisioning.getInstance().get(AccountBy.name, user);
        
        System.out.println();
        System.out.println(Provisioning.A_zimbraAccountStatus + ": " + acct.getAttr(Provisioning.A_zimbraAccountStatus));
        System.out.println(Provisioning.A_zimbraPasswordLockoutLockedTime + ": " + acct.getAttr(Provisioning.A_zimbraPasswordLockoutLockedTime));

        System.out.println(Provisioning.A_zimbraPasswordLockoutFailureTime + ": ");
        String[] failureTime = acct.getMultiAttr(Provisioning.A_zimbraPasswordLockoutFailureTime);
        for (String ft : failureTime)
            System.out.println("    " + ft);
        
        return acct;
    }
    
    public void disable_testPreAuthLockout() throws Exception {
        String user = "user4";
        Account acct = TestUtil.getAccount(user);
        
        Provisioning prov = Provisioning.getInstance();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        int lockoutAfterNumFailures = 3;
        
        // setup lockout config attrs
        attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraPasswordLockoutDuration, "1m");
        attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, lockoutAfterNumFailures+"");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureLifetime, "30s");
        
        // put the account in active mode, clean all lockout attrs that might have been set 
        // in previous test
        attrs.put(Provisioning.A_zimbraAccountStatus, "active");
        attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "");
        
        prov.modifyAttrs(acct, attrs);
        
        System.out.println("Before the test:");
        dumpLockoutAttrs(user);
        System.out.println();
        
        // the account should be locked out at the last iteration
        for (int i=0; i<=lockoutAfterNumFailures; i++) {
            System.out.println("======================");
            System.out.println("Iteration: " + i);
            
            doPreAuth(user, false, true);
            Account a = dumpLockoutAttrs(user);
            System.out.println("\n\n");
            
            if (i >= lockoutAfterNumFailures-1)
                assertEquals("lockout", a.getAttr(Provisioning.A_zimbraAccountStatus));
            else
                assertEquals("active", a.getAttr(Provisioning.A_zimbraAccountStatus));
            
            // sleep two seconds
            Thread.sleep(2000);
        }
    }
    
    public void testPreAuthAccountNotActive() throws Exception {
        String user = "user1";
        Account acct = TestUtil.getAccount(user);
        
        Provisioning prov = Provisioning.getInstance();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, "maintenance");
        prov.modifyAttrs(acct, attrs);
        
        System.out.println("Before the test:");
        System.out.println(Provisioning.A_zimbraAccountStatus + ": " + acct.getAttr(Provisioning.A_zimbraAccountStatus));
        System.out.println();
        
        
        String preAuthKey = setUpDomain();
        String preAuthUrl = genPreAuthUrl(preAuthKey, user, false, false);
        
        System.out.println("preAuthKey=" + preAuthKey);
        System.out.println("preAuth=" + preAuthUrl);
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        String protoHostPort = "http://localhost:" + localServer.getIntAttr(Provisioning.A_zimbraMailPort, 0);
        String url = protoHostPort + preAuthUrl;
        
        HttpClient client =  HttpClientBuilder.create().build();
        HttpRequestBase method = new HttpGet(url);
        try {
            HttpResponse response = HttpClientUtil.executeMethod(client, method);
            int statusCode = response.getStatusLine().getStatusCode();
            String statusLine = response.getStatusLine().getReasonPhrase();
            System.out.println("statusCode=" + statusCode);
            System.out.println("statusLine=" + statusLine);
            assertEquals(400, statusCode);
           
            
        } catch (HttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            method.releaseConnection();
        }
        
        //revert account status back to active
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, "active");
        prov.modifyAttrs(acct, attrs);
        
        System.out.println("After the test:");
        System.out.println(Provisioning.A_zimbraAccountStatus + ": " + acct.getAttr(Provisioning.A_zimbraAccountStatus));
        System.out.println();
    }
    
    public void testShouldNotAllowPreAuthGetCookieReuse() throws Exception{
       Account account =  TestUtil.getAccount("user1");
       AuthToken authToken = new ZimbraAuthToken(account);
       System.out.println(authToken.isRegistered());
       HttpClient client =  HttpClientBuilder.create().build();
       Server localServer =  Provisioning.getInstance().getLocalServer();
       String protoHostPort = "http://localhost:" + localServer.getIntAttr(Provisioning.A_zimbraMailPort, 0);
       String url = protoHostPort + PRE_AUTH_URL;
       
       //allow first request
       
       List< NameValuePair> nvp = new ArrayList<NameValuePair>();
       nvp.add(new BasicNameValuePair("isredirect","1"));
       nvp.add(new BasicNameValuePair("authtoken",authToken.getEncoded()));
       
       HttpRequestBase method = new HttpGet(url + "?" + URLEncodedUtils.format(nvp, "utf-8"));
       
       HttpResponse response = HttpClientUtil.executeMethod(client, method);
       
       //reject second request
       method = new HttpGet(url + "?" + URLEncodedUtils.format(nvp, "utf-8"));
       response = HttpClientUtil.executeMethod(client, method);
       int respCode =response.getStatusLine().getStatusCode();
       Assert.assertEquals(400, respCode);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        TestUtil.cliSetup();
        try {
            TestUtil.runTest(TestPreAuthServlet.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
