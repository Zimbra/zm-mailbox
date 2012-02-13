/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.PreAuthKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.entry.LdapDomain;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.qa.unittest.TestPreAuthServlet;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.AutoProvisionTestUtil;
import com.zimbra.qa.unittest.prov.LocalconfigTestUtil;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.admin.message.AutoProvTaskControlRequest;
import com.zimbra.soap.admin.message.AutoProvTaskControlResponse;
import com.zimbra.soap.admin.message.ReloadLocalConfigRequest;
import com.zimbra.soap.admin.message.ReloadLocalConfigResponse;
import com.zimbra.soap.admin.message.AutoProvTaskControlRequest.Action;
import com.zimbra.soap.admin.message.AutoProvTaskControlResponse.Status;
import com.zimbra.soap.admin.type.CountObjectsType;

public class TestAutoProvision extends SoapTest {
    
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain extDomain;
    private static String extDomainDn;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        extDomain = provUtil.createDomain("external." + baseDomainName());
        extDomainDn = LdapUtil.domainToDN(extDomain.getName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private String getZimbraDomainName(String testName) {
        return testName + "." + baseDomainName();
    }
    
    private Domain createZimbraDomain(String testName, Map<String, Object> zimbraDomainAttrs) 
    throws Exception {
        return provUtil.createDomain(getZimbraDomainName(testName), zimbraDomainAttrs);
    }
    
    private String createExternalAcctEntry(String localPart) throws Exception {
        return createExternalAcctEntry(localPart, null);
    }
    
    private String createExternalAcctEntry(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return createExternalAcctEntry(localPart, null, attrs);
    }
    
    private String createExternalAcctEntry(String localPart, String externalPassword, 
            Map<String, Object> attrs) throws Exception {
        String extAcctName = TestUtil.getAddress(localPart, extDomain.getName());
        
        Map<String, Object> extAcctAttrs = attrs == null ? new HashMap<String, Object>() : attrs;
        
        extAcctAttrs.put(Provisioning.A_displayName, "display name");
        extAcctAttrs.put(Provisioning.A_sn, "last name");
        Account extAcct = prov.createAccount(extAcctName, externalPassword, extAcctAttrs);
        return extAcctName;
    }
    
    private Map<String, Object> commonZimbraDomainAttrs() {
        return AutoProvisionTestUtil.commonZimbraDomainAttrs();
    }
    
    private void verifyAcctAutoProvisioned(Account acct, String expectedAcctName) 
    throws Exception {
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(
                acct, expectedAcctName);
    }
    
    private String getAuthTokenAcctId(String authToken) throws Exception {
        Map attrs = AuthToken.getInfo(authToken);
        String zimbraId = (String) attrs.get("id");  // hardcode id here, C_ID in ZimbraAuthToken is private
        assertNotNull(zimbraId);
        return zimbraId;
    }
    
    
    /* ========================
     * SOAP and servlets tests
     * ========================
     */
    
    /*
     * Note: need to restart server each time before re-run.  Otherwise server would still 
     * have the account created in previous run cached.
     */
    @Test
    public void authRequestByPassword() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        // setup external LDAP auth
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://localhost:389");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapBindDn, "uid=%u,ou=people," + extDomainDn);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        String loginName = extAcctLocalPart + "@" + zimbraDomain.getName();
        
        // make the soap request
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(loginName);
        request.addElement(AccountConstants.E_PASSWORD).setText(externalPassword);
        
        Element response = transport.invoke(request);
        
        String encodedAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        assertNotNull(encodedAuthToken);
        String acctId = getAuthTokenAcctId(encodedAuthToken);
        Account acct = prov.get(AccountBy.id, acctId);
        verifyAcctAutoProvisioned(acct, loginName.toLowerCase());
    }
  

    /*
     * Note: need to restart server each time before re-run.  Otherwise server would still 
     * have the account created in previous run cached.
     */
    @Test
    public void authRequestByPreauth() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = commonZimbraDomainAttrs();
        // setup auto prov
        // commonZimbraDomainAttrs added only LDAP, add preauth here
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvAuthMech, AutoProvAuthMech.PREAUTH.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        // setup external LDAP auth
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://localhost:389");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapBindDn, "uid=%u,ou=people," + extDomainDn);
        // setup preauth
        String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        zimbraDomainAttrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        String loginName = extAcctLocalPart + "@" + zimbraDomain.getName();
        
        // preauth data
        HashMap<String,String> params = new HashMap<String,String>();
        String authBy = "name";
        long timestamp = System.currentTimeMillis();
        long expires = 0;
        
        params.put("account", loginName);
        params.put("by", authBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");

        String preAuth = PreAuthKey.computePreAuth(params, preAuthKey);
        
            
        // make the soap request
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, authBy).setText(loginName);
        Element ePreAuth = request.addElement(AccountConstants.E_PREAUTH).setText(preAuth);
        ePreAuth.addAttribute(AccountConstants.A_TIMESTAMP, timestamp);
        ePreAuth.addAttribute(AccountConstants.A_EXPIRES, expires);
        
        Element response = transport.invoke(request);
        
        String encodedAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        assertNotNull(encodedAuthToken);
        String acctId = getAuthTokenAcctId(encodedAuthToken);
        Account acct = prov.get(AccountBy.id, acctId);
        verifyAcctAutoProvisioned(acct, loginName.toLowerCase());
    }
    
    /*
     * Note: need to restart server each time before re-run.  Otherwise server would still 
     * have the account created in previous run cached.
     */
    @Test
    @Ignore // need to setup KDC
    public void authRequestByKrb5() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, externalPassword, null);
        
        String krb5Realm = "MYREALM";
        
        Map<String, Object> zimbraDomainAttrs = commonZimbraDomainAttrs();
        // setup auto prov
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvAuthMech, AutoProvAuthMech.KRB5.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        // setup auth mech and krb5 realm on domain
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthMech, AuthMech.kerberos5.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthKerberos5Realm, krb5Realm);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthKerberos5Realm, krb5Realm);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        String loginName = extAcctLocalPart + "@" + krb5Realm;
        
        // make the soap request
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.krb5Principal.name()).setText(loginName);
        request.addElement(AccountConstants.E_PASSWORD).setText(externalPassword);
        
        Element response = transport.invoke(request);
        
        String encodedAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        assertNotNull(encodedAuthToken);
        String acctId = getAuthTokenAcctId(encodedAuthToken);
        Account acct = prov.get(AccountBy.id, acctId);
        verifyAcctAutoProvisioned(acct, loginName.toLowerCase());
    }
    
    /*
     * Note: need to restart server each time before re-run.  Otherwise server would still 
     * have the account created in previous run cached.
     */
    @Test
    public void preauthServlet() throws Exception {
        String testName = getTestName();
                
        String externalPassword = "test456";
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = commonZimbraDomainAttrs();
        // setup auto prov
        // commonZimbraDomainAttrs added only LDAP, add preauth here
        StringUtil.addToMultiMap(zimbraDomainAttrs, Provisioning.A_zimbraAutoProvAuthMech, AutoProvAuthMech.PREAUTH.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        // setup external LDAP auth
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://localhost:389");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapBindDn, "uid=%u,ou=people," + extDomainDn);
        // setup preauth
        String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        zimbraDomainAttrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        String loginName = extAcctLocalPart + "@" + zimbraDomain.getName();
        
        // preauth data
        String preAuthUrl = TestPreAuthServlet.genPreAuthUrl(preAuthKey, loginName, false, false);
        
        // do the preauth servlet request
        String url = TestUtil.getBaseUrl() + preAuthUrl;
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        
        boolean ok = false;
        try {
            int respCode = HttpClientUtil.executeMethod(client, method);
            int statusCode = method.getStatusCode();
            String statusLine = method.getStatusLine().toString();
            
            ok = (respCode == 200);
            
            /*
            System.out.println("respCode=" + respCode);
            System.out.println("statusCode=" + statusCode);
            System.out.println("statusLine=" + statusLine);
            
            System.out.println("Headers");
            Header[] respHeaders = method.getResponseHeaders();
            for (int i=0; i < respHeaders.length; i++) {
                String header = respHeaders[i].toString();
                System.out.println(header);
            }
            */
            
            /*
            String respBody = method.getResponseBodyAsString();
            System.out.println(respBody);
            */
            
        } catch (HttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            method.releaseConnection();
        }
        
        assertTrue(ok);
        
        /*
        String encodedAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        assertNotNull(encodedAuthToken);
        AuthToken authToken = AuthToken.getAuthToken(encodedAuthToken);
        String acctId = authToken.getAccountId();
        Account acct = prov.get(AccountBy.id, acctId);
        verifyAcctAutoProvisioned(acct, loginName.toLowerCase());
        */
    }
    
    private void verifyAutoProvTask(SoapTransport transport, 
            Action action, Status expectedSatus) 
    throws Exception {
        AutoProvTaskControlRequest req = 
            new AutoProvTaskControlRequest(action);
        AutoProvTaskControlResponse resp = invokeJaxb(transport, req);
        Status status = resp.getStatus();
        
        assertEquals(expectedSatus.name(), status.name());
    }
    
    @Test
    public void attributeCallbackAutoProvMode() throws Exception {
        
        Domain domain1 = createZimbraDomain(genDomainSegmentName("1"), null);
        domain1.setAutoProvMode(Provisioning.AutoProvMode.EAGER);
        
        Domain domain2 = createZimbraDomain(genDomainSegmentName("2"), null);
        domain2.setAutoProvMode(Provisioning.AutoProvMode.EAGER);
        
        Account admin = provUtil.createGlobalAdmin(genAcctNameLocalPart(), domain1);
        SoapTransport transport = authAdmin(admin.getName());
        
        /*
         * verify auto prov thread is not running at the beginning
         */
        verifyAutoProvTask(transport, Action.status, Status.idle);
        
        /*
         * schedule eager prov for domain1 and domain2 on servers 
         */
        Server localServer = prov.getLocalServer();
        localServer.setAutoProvScheduledDomains(new String[]{domain1.getName(), domain2.getName()});
        
        /*
         * verify auto prov thread is now running, because at last one domain is scheduled
         */
        verifyAutoProvTask(transport, Action.status, Status.running);
        
        Server otherServer = provUtil.createServer(genServerName("other"));
        otherServer.setAutoProvScheduledDomains(new String[]{domain1.getName(), domain2.getName()});
        
        /*
         * change zimbraAutoProvMode on domain1 away from EAGER, verify domain1 is 
         * removed from zimbraAutoProvScheduledDomains on all servers
         */
        domain1.setAutoProvMode(Provisioning.AutoProvMode.MANUAL);
        
        // refresh server object instances, we are on SOAP
        prov.reload(localServer);
        prov.reload(otherServer);
        
        Verify.verifyEquals(Sets.newHashSet(domain2.getName()),
                localServer.getAutoProvScheduledDomains());
        Verify.verifyEquals(Sets.newHashSet(domain2.getName()),
                otherServer.getAutoProvScheduledDomains());
        
        /*
         * change zimbraAutoProvMode on domain2 away from EAGER, verify domain2 is 
         * removed from zimbraAutoProvScheduledDomains on all servers,
         * since domain2 is the last scheduled domain on the server, verify the 
         * auto provision thread is idle(i.e. not running).
         */
        domain2.setAutoProvMode(Provisioning.AutoProvMode.MANUAL);
        // refresh server object instances, we are on SOAP
        prov.reload(localServer);
        prov.reload(otherServer);
        
        Verify.verifyEquals(new HashSet<String>(), 
                localServer.getAutoProvScheduledDomains());
        Verify.verifyEquals(new HashSet<String>(),
                otherServer.getAutoProvScheduledDomains());
        
        
        verifyAutoProvTask(transport, Action.status, Status.idle);
    }
    
    @Test
    public void attributeCallbackAutoProvScheduledDomains() throws Exception {
        Domain domain1 = createZimbraDomain(genDomainSegmentName("1"), null);
        domain1.setAutoProvMode(Provisioning.AutoProvMode.EAGER);
        
        Domain domain2 = createZimbraDomain(genDomainSegmentName("2"), null);
        domain2.setAutoProvMode(Provisioning.AutoProvMode.EAGER);
        
        Account admin = provUtil.createGlobalAdmin(genAcctNameLocalPart(), domain1);
        SoapTransport transport = authAdmin(admin.getName());
        
        /*
         * verify auto prov thread is not running at the beginning
         */
        verifyAutoProvTask(transport, Action.status, Status.idle);
        
        /*
         * schedule eager prov for domain1 and domain2 on server 
         */
        Server localServer = prov.getLocalServer();
        localServer.setAutoProvScheduledDomains(new String[]{domain1.getName(), domain2.getName()});
        
        /*
         * verify auto prov thread is now running, because at last one domain is scheduled
         */
        verifyAutoProvTask(transport, Action.status, Status.running);
        
        /*
         * unschedule domain1, verify the auto prov thread is still running
         */
        localServer.removeAutoProvScheduledDomains(domain1.getName());
        verifyAutoProvTask(transport, Action.status, Status.running);
        
        /*
         * unschedule domain2, verify the auto prov thread is not running, because there 
         * is no scheduled domain.
         */
        localServer.removeAutoProvScheduledDomains(domain2.getName());
        verifyAutoProvTask(transport, Action.status, Status.idle);
    }
    
    @Test
    public void attributeCallbackAutoProvPollingInterval() throws Exception {
        Domain domain1 = createZimbraDomain(genDomainSegmentName("1"), null);
        domain1.setAutoProvMode(Provisioning.AutoProvMode.EAGER);
        
        Domain domain2 = createZimbraDomain(genDomainSegmentName("2"), null);
        domain2.setAutoProvMode(Provisioning.AutoProvMode.EAGER);
        
        Account admin = provUtil.createGlobalAdmin(genAcctNameLocalPart(), domain1);
        SoapTransport transport = authAdmin(admin.getName());
        
        /*
         * verify auto prov thread is not running at the beginning
         */
        verifyAutoProvTask(transport, Action.status, Status.idle);
        
        /*
         * schedule eager prov for domain1 and domain2 on server 
         */
        Server localServer = prov.getLocalServer();
        localServer.setAutoProvScheduledDomains(new String[]{domain1.getName(), domain2.getName()});
        
        /*
         * verify auto prov thread is now running, because at last one domain is scheduled
         */
        verifyAutoProvTask(transport, Action.status, Status.running);
        
        
        /*
         * set polling interval to 0, verify the auth prov thread is not running
         */
        // remember cur value and set it back after the test
        String curPollingInterval = localServer.getAutoProvPollingIntervalAsString();
        localServer.setAutoProvPollingInterval("0");
        verifyAutoProvTask(transport, Action.status, Status.idle);
        
        /*
         * done test, clean all data on the server
         */
        localServer.unsetAutoProvScheduledDomains();
        localServer.setAutoProvPollingInterval(curPollingInterval);
        Verify.verifyEquals(new HashSet<String>(), localServer.getAutoProvScheduledDomains());
        assertEquals(curPollingInterval, localServer.getAutoProvPollingIntervalAsString());
    }
    
    @Test
    public void autoProvTaskControl() throws Exception {
        Domain domain = createZimbraDomain(genDomainSegmentName(), null);
        Account admin = provUtil.createGlobalAdmin(genAcctNameLocalPart(), domain);
        SoapTransport transport = authAdmin(admin.getName());
        
        /*
         * verify auto prov thread is not running at the beginning
         */
        verifyAutoProvTask(transport, Action.status, Status.idle);
        
        /*
         * force start the auto prov thread, verify it is started
         */
        verifyAutoProvTask(transport, Action.start, Status.started);
        verifyAutoProvTask(transport, Action.status, Status.running);
        
        /*
         * force stop the auot prov thread, verify it is stopped
         */
        verifyAutoProvTask(transport, Action.stop, Status.stopped);
        verifyAutoProvTask(transport, Action.status, Status.idle);
    }
    
    public static class TestDomainLockListener extends AutoProvisionTestUtil.MarkEntryProvisionedListener {
        private static final long LONGTIME = Constants.MILLIS_PER_DAY;
        private static final int HOLD_IT_AT_THIS_ENTRY = 2;
        
        private int numEntriesAutoProvisioned = 0;
        
        @Override
        public void postCreate(Domain domain, Account acct, String externalDN) {
            super.postCreate(domain, acct, externalDN);
            
            numEntriesAutoProvisioned++;
            if (HOLD_IT_AT_THIS_ENTRY == numEntriesAutoProvisioned) {
                // sleep for long time to hold the auto prov thread.
                // doing this will keep the auto prov thread in the server to have 
                // the domain locked so the eagerModeDomainUnlockedWhenThreadIsStopped 
                // test can verify domain unlocked at the right timing
                try {
                    Thread.sleep(LONGTIME);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Test
    public void eagerModeDomainUnlockedWhenThreadStopped() throws Exception {
        // must be > TestDomainLockListener.HOLD_IT_AT_THIS_ENTRY
        int numAccts = TestDomainLockListener.HOLD_IT_AT_THIS_ENTRY + 2; 
        
        for (int i = 1; i <= numAccts; i++) {
            createExternalAcctEntry("eagerMode-" + i, "test123", null);
        }
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, 
                "(&(uid=%u)(mail=eagerMode*)" + AutoProvisionTestUtil.MarkEntryProvisionedListener.NOT_PROVED_FILTER + ")");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, Provisioning.A_uid);
        
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvListenerClass, 
                TestDomainLockListener.class.getName());
        Domain zimbraDomain = createZimbraDomain(genDomainSegmentName(), zimbraDomainAttrs);
        
        // create a domain for the admin so the admin account won't interfere with our account counting
        Domain domain = provUtil.createDomain(getZimbraDomainName("admin-domain"));
        Account admin = provUtil.createGlobalAdmin(genAcctNameLocalPart(), domain);
        SoapTransport transport = authAdmin(admin.getName());
        
        /*
         * verify the auto prov thread is not running
         */
        verifyAutoProvTask(transport, Action.status, Status.idle);
        
        /*
         * change LC key autpprov_initial_sleep_ms to 0, so we don't need to wait that long
         * (default is 5 mins)
         */
        String cur_autpprov_initial_sleep_ms = LC.autpprov_initial_sleep_ms.value();
        modifyLocalconfigAndReload(transport, LC.autpprov_initial_sleep_ms, "0");
        
                
        // schedule the domain on local server
        Server localServer = prov.getLocalServer();
        localServer.addAutoProvScheduledDomains(zimbraDomain.getName());

        
        /*
         * verify the auto prov thread is running
         */
        verifyAutoProvTask(transport, Action.status, Status.running);
        
        /*
         * let the auto prov thread run for a while, until after the 
         * TestDomainLockListener.HOLD_IT_AT_THIS_ENTRYth account is auto provisioned
         */
        // use LdapProvisioning to count accounts, because countObjects is not yet 
        // supported in SoapProvisioning
        LdapProvisioning ldapProv = (LdapProvisioning) Provisioning.getInstance();
        LdapDomain zimbraDomainLdap = (LdapDomain) ldapProv.get(DomainBy.name, zimbraDomain.getName());
        while (true) {
            long numAcctsAutoProvisioned = ldapProv.countObjects(CountObjectsType.account, zimbraDomainLdap);
            if (numAcctsAutoProvisioned == TestDomainLockListener.HOLD_IT_AT_THIS_ENTRY) {
                break;
            }
            System.out.println(getTestName() + " waiting for 1 second");
            Thread.sleep(Constants.MILLIS_PER_SECOND);
        }
        
        
        /*
         * verify the domain is locked, since the eager auto prov thread should be at work
         */
        prov.reload(zimbraDomain);
        assertEquals(localServer.getId(), zimbraDomain.getAutoProvLock());
        
        
        /*
         * un-schedule the domain
         */
        localServer.unsetAutoProvScheduledDomains();
        
        /*
         * verify the thread is stopped, and the domain is unlocked
         */
        verifyAutoProvTask(transport, Action.status, Status.idle);
        prov.reload(zimbraDomain);
        assertNull(zimbraDomain.getAutoProvLock());
        
        /*
         * done test, set the LC key back
         */
        modifyLocalconfigAndReload(transport, LC.autpprov_initial_sleep_ms, cur_autpprov_initial_sleep_ms);
        
    }
}
