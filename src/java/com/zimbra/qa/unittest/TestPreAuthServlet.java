package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.PreAuthKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;


import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestPreAuthServlet extends TestCase {

    String setUpDomain() throws Exception {
        String domainName = TestUtil.getDomain();
        Domain domain = Provisioning.getInstance().get(DomainBy.name, domainName);
        String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        Provisioning.getInstance().modifyAttrs(domain, attrs);
        return preAuthKey;
    }
    
    String genPreAuthUrl(String preAuthKey, String user) throws Exception {
        
        HashMap<String,String> params = new HashMap<String,String>();
        String acctName = TestUtil.getAddress(user);
        String authBy = "name";
        long timestamp = System.currentTimeMillis();
        long expires = 0;
        
        params.put("account", acctName);
        params.put("by", authBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        String preAuth = PreAuthKey.computePreAuth(params, preAuthKey);
        
        StringBuffer url = new StringBuffer("/service/preauth?");
        url.append("account=" + acctName);
        url.append("&by=" + authBy);
        url.append("&timestamp=" + timestamp);
        url.append("&expires=" + expires);
        url.append("&preauth=" + preAuth);
        
        return url.toString();
    }
    
    void doPreAuthServletRequest(String preAuthUrl) throws Exception{
        int port = port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailPort, 0);
        String url = "http://localhost:" + port + preAuthUrl;
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        
        try {
            int respCode = client.executeMethod(method);
            int statusCode = method.getStatusCode();
            String statusLine = method.getStatusLine().toString();
            
            System.out.println("respCode=" + respCode);
            System.out.println("statusCode=" + statusCode);
            System.out.println("statusLine=" + statusLine);
            
            System.out.println("Headers");
            Header[] respHeaders = method.getResponseHeaders();
            for (int i=0; i < respHeaders.length; i++) {
                String header = respHeaders[i].toString();
                System.out.println(header);
            }
            
            String respBody = method.getResponseBodyAsString();
            // System.out.println("respBody=" + respBody);
            
        } catch (HttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            method.releaseConnection();
        }
    }
    
    public void testPreAuthServlet() throws Exception {
        String preAuthKey = setUpDomain();
        String preAuthUrl = genPreAuthUrl(preAuthKey, "user1");
        
        System.out.println("preAuthKey=" + preAuthKey);
        System.out.println("preAuth=" + preAuthUrl);
        
        doPreAuthServletRequest(preAuthUrl);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        TestUtil.cliSetup();
        try {
            TestUtil.runTest(new TestSuite(TestPreAuthServlet.class));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
