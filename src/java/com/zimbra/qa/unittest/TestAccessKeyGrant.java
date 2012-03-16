/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.AuthProviderException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGrant;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.client.ZSearchParams;

/*
 * To test key grant:
 * 
 * 1. In com.zimbra.cs.service.AuthProvider, uncomment 
 *    // register(new com.zimbra.qa.unittest.TestAccessKeyGrant.DummyAuthProvider());
 *    
 * 2. In /opt/zimbra/conf/localconfig.xml, set: 
<key name="zimbra_auth_provider">
    <value>DUMMY_AUTH_PROVIDER</value>
</key>

 *
*/

public class TestAccessKeyGrant extends TestCase {

    private static final String DUMMY_AUTH_PROVIDER = "DUMMY_AUTH_PROVIDER";
    private static final String OWNER_NAME = "user1";
    private static final String USER_GRANTEE_NAME = "user3";
    private static final String ACCESS_KEY = "b931d99fc5dc7e8061a97d90e05e3256";
    
    private static final String AUTH_K_ATTR = "K";
    private static final String AUTH_H_ATTR = "H";
    
    private static final String FOLDER_PATH = "Calendar";
    
    public static class DummyAuthProvider extends AuthProvider {

        public DummyAuthProvider() {
            super(DUMMY_AUTH_PROVIDER);
        }
        
        @Override
        protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq)
               throws AuthProviderException, AuthTokenException {

            if (isAdminReq)
                return null;

            String accessKey = (String)req.getParameter("k");
            String hostYid   = (String)req.getParameter("h");
            
            if (accessKey != null && hostYid != null) {
                return new DummyAuthToken(accessKey, hostYid);
            } else {
                throw AuthProviderException.NO_AUTH_DATA();
            }
        }


        @Override
        protected AuthToken authToken(Element soapCtxt, Map engineCtxt)
                throws AuthProviderException, AuthTokenException {
            
            if (soapCtxt == null)
                throw AuthProviderException.NO_AUTH_DATA();

            try {
                Element eAuthToken = soapCtxt.getElement("authToken");
                String type = eAuthToken.getAttribute("type");
                if (type == null || !type.equals(DUMMY_AUTH_PROVIDER))
                    throw AuthProviderException.NOT_SUPPORTED();

                String ownerId = null;
                String accessKey = null;
                
                for (Element authAttr : eAuthToken.listElements(AccountConstants.E_A)) {
                    String name = authAttr.getAttribute(AccountConstants.A_N);

                    if (name != null && name.equals(AUTH_K_ATTR))
                        accessKey = authAttr.getText();
                    else if (name != null && name.equals(AUTH_H_ATTR))
                        ownerId = authAttr.getText();
                }

                if (accessKey != null && ownerId != null)
                    return new DummyAuthToken(accessKey, ownerId);
                
            } catch (ServiceException x) {
                throw AuthProviderException.NO_AUTH_DATA();
            }

            throw AuthProviderException.NO_AUTH_DATA();
        }
        
        protected boolean allowURLAccessKeyAuth(HttpServletRequest req, ZimbraServlet servlet) {
            return true;
        }

    }
    
    private static class DummyAuthToken extends AuthToken {
        
        private String mAccessKey;
        private String mOwnerId;
        
        DummyAuthToken(String accessKey, String ownerId) {
            mAccessKey = accessKey;
            mOwnerId = ownerId;
        }

        @Override
        public void encode(HttpClient client, HttpMethod method,
                boolean isAdminReq, String cookieDomain) throws ServiceException {
            // TODO Auto-generated method stub
            throw ServiceException.FAILURE("Not implemented", null);
        }

        @Override
        public void encode(HttpState state, boolean isAdminReq, String cookieDomain)
                throws ServiceException {
            // TODO Auto-generated method stub
            throw ServiceException.FAILURE("Not implemented", null);
        }

        @Override
        public void encode(HttpServletResponse resp, boolean isAdminReq, 
                boolean secureCookie, boolean rememberMe)
                throws ServiceException {
            // TODO Auto-generated method stub
            throw ServiceException.FAILURE("Not implemented", null);
        }

        @Override
        public void encodeAuthResp(Element parent, boolean isAdmin)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public String getAccountId() {
            // TODO Auto-generated method stub
            return mOwnerId;
        }

        @Override
        public String getAdminAccountId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCrumb() throws AuthTokenException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDigest() {
            // TODO Auto-generated method stub
            return null;
        }
        
        public String getAccessKey() {
            return mAccessKey;
        }

        @Override
        public String getEncoded() throws AuthTokenException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getExpires() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getExternalUserEmail() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isAdmin() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDomainAdmin() {
            // TODO Auto-generated method stub
            return false;
        }
        
        @Override
        public boolean isDelegatedAdmin() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isExpired() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isZimbraUser() {
            // TODO Auto-generated method stub
            return mAccessKey == null;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ZAuthToken toZAuthToken() throws ServiceException {
            Map<String,String> attrs = new HashMap<String, String>();
            attrs.put(AUTH_K_ATTR, mAccessKey);
            attrs.put(AUTH_H_ATTR, mOwnerId);
            return new ZAuthToken(DUMMY_AUTH_PROVIDER, null, attrs);
        }

    }
    
    /*
     * ====================
     *     util methods
     * ====================
     */
    private Account getAccount(String acctName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.name, acctName);
        assertNotNull(acct);
        return acct;
    }
    
    private String getAccountId(String acctName) throws ServiceException {
        return getAccount(acctName).getId();
    }
    
    private String getRestUrl(String acctName) throws ServiceException {
        return UserServlet.getRestUrl(getAccount(acctName));
    }
    
    private String getRestCalendarUrl(String acctName) throws ServiceException {
        return UserServlet.getRestUrl(getAccount(acctName)) + "/Calendar";
    }
    
    
    private ZMailbox getZMailboxByKey() throws Exception {
        Map<String,String> authAttrs = new HashMap<String, String>();
        authAttrs.put(AUTH_K_ATTR, ACCESS_KEY);
        authAttrs.put(AUTH_H_ATTR, getAccountId(OWNER_NAME));
        
        ZAuthToken zat = new ZAuthToken(DUMMY_AUTH_PROVIDER, null, authAttrs);
        ZMailbox.Options options = new ZMailbox.Options(zat, TestUtil.getSoapUrl());
        return ZMailbox.getMailbox(options);
    }
    
    private void dumpGrants(ZMailbox mbox, String folderId) throws Exception {
        ZFolder folder = mbox.getFolderRequestById(folderId);
        
        System.out.println("--------------------");
        System.out.println(mbox.getName() + ", folder=" + folderId);
        List<ZGrant> grants = folder.getGrants();
        for (ZGrant grant : grants) {
            System.out.println("    type: " + grant.getGranteeType().toString());
            System.out.println("    id: " + grant.getGranteeId());
            System.out.println("    name: " + grant.getGranteeName());
            System.out.println("    rights: " + grant.getPermissions());
            
            if (grant.getGranteeType() == ZGrant.GranteeType.key)
                System.out.println("    accesskey: " + grant.getArgs());
            else if (grant.getGranteeType() == ZGrant.GranteeType.guest)
                System.out.println("    password: " + grant.getArgs());
            else
                assertNull(grant.getArgs());
            
            System.out.println();
        }
    }
    
    public void disable_testKeyGrant() throws Exception {
        ZMailbox mbox = getZMailboxByKey();
        // ZFolder folder = mbox.getFolderByPath(FOLDER_PATH);
        
        StringBuffer query = new StringBuffer();
        query.append("(inid:");
        // query.append(folder.getId());
        query.append("10");
        query.append(")");
        ZSearchParams sp = new ZSearchParams(query.toString());
        sp.setTypes(ZSearchParams.TYPE_APPOINTMENT);
        mbox.search(sp);
    }
    
    /*
     * ensure the accesskey is return in getFolderRequest
     */
    public void disable_testGetFolderRequest() throws Exception {
        ZMailbox ownerMbox = TestUtil.getZMailbox(OWNER_NAME);
        ZMailbox granteeMbox = TestUtil.getZMailbox(USER_GRANTEE_NAME);
        
        // grant USER_GRANTEE_NAME full access to a folder
        String mointpointPath = OWNER_NAME + "'s "  + FOLDER_PATH;
        
        /*
        ZMountpoint mt = TestUtil.createMountpoint(ownerMbox, FOLDER_PATH,
                                                   granteeMbox, mointpointPath);
        */
        
        // grant key access to the mountpoint
        /*
        ZFolder mtFolder = granteeMbox.getFolderByPath(mointpointPath);
        granteeMbox.modifyFolderGrant(mtFolder.getId(), ZGrant.GranteeType.key, "a key grantee", "r", null);
        */
        
        // TO BE CLEANED UP...
        
        // dumpGrants(ownerMbox, FOLDER_PATH);
        // dumpGrants(granteeMbox, mointpointPath);
        // dumpGrants(granteeMbox, "Demo User One's Calendar (admin)");
        // dumpGrants(granteeMbox, "/");
        dumpGrants(TestUtil.getZMailbox("user2"), "259");
        dumpGrants(TestUtil.getZMailbox("user4"), "258");
    }
    
    private void executeHttpMethod(HttpClient client, HttpMethod method) throws Exception {
        try {
            
            int respCode = HttpClientUtil.executeMethod(client, method);

            if (respCode != HttpStatus.SC_OK ) {
                 System.out.println("failed, respCode=" + respCode);
            } else {
                 
                 boolean chunked = false;
                 boolean textContent = false;
                 
                 System.out.println("Headers:");
                 System.out.println("--------");
                 for (Header header : method.getRequestHeaders()) {
                     System.out.print("    " + header.toString());
                 }
                 System.out.println();
                 
                 System.out.println("Body:");
                 System.out.println("-----");
                 String respBody = method.getResponseBodyAsString();
                 System.out.println(respBody);
             }
         } finally {
             // Release the connection.
             method.releaseConnection();
         }
    }
    
    /*
     * setup:
     * 
     * 1. use zmmailbox to grant key access: zmmailbox -z -d -r soap12
     *        -d: debug, so it shows soap trace
     *        -z: default admin auth
     *        -r soap12: use soap12 protocol, it's easier to read than json
     *    
     *    mbox> sm user1
     *    mbox user1@phoebe.mac> mfg Calendar key k1@key.com r
     *    
     *    grab the access key from the FolderActionResponse:
     *        <FolderActionResponse xmlns="urn:zimbraMail">
     *            <action d="k1@key.com" key="3c4877ed3948511cee39379debbf968d" op="grant" zid="k1@key.com" id="10"/>
     *        </FolderActionResponse>
     *        
     * 2. paste the access key to the test (TODO, automate it)        
     *    
     * 3. In com.zimbra.cs.service.AuthProvider, uncomment // register(new com.zimbra.qa.unittest.TestAccessKeyGrant.DummyAuthProvider());
     * 
     * 4. ant deploy-war
     * 
     * 5. modify localconfig.xml, add:
     *    <key name="zimbra_auth_provider">
     *        <value>DUMMY_AUTH_PROVIDER</value>
     *    </key>
     *
     * 6. retstart server
     * 
     * ready to run the test
     */
    public void disable_testCalendarGet_Yahoo_accesskey() throws Exception {
        
        HttpClient client = new HttpClient();
        
        String accessKey = "3c4877ed3948511cee39379debbf968d-bogus";
        String url = getRestCalendarUrl(OWNER_NAME);
        
        /* 
         * the Yahoo accesskey URL is:
         * /home/yid/Calendar/Folder.ics?k=accesskey&h=yid
         */
        url = url + "?k=" + accessKey + "&h=" + getAccountId(OWNER_NAME);
        
        System.out.println("REST URL: " + url);
        HttpMethod method = new GetMethod(url);
        
        executeHttpMethod(client, method);
    }
    
    /*
     * use zmmailbox to grant guest access:
     * zmmailbox -z -m user1@phoebe.mac mfg Calendar guest g1@guest.com zzz r
     */
    public void testCalendarGet_guest() throws Exception {
        
        HttpState initialState = new HttpState();
        
        /*
        Cookie authCookie = new Cookie(restURL.getURL().getHost(), "ZM_AUTH_TOKEN", mAuthToken, "/", null, false);
        Cookie sessionCookie = new Cookie(restURL.getURL().getHost(), "JSESSIONID", mSessionId, "/zimbra", null, false);
        initialState.addCookie(authCookie);
        initialState.addCookie(sessionCookie);
        */
        
        String guestName = "g1@guest.com";
        String guestPassword = "zzz";
        Credentials loginCredentials = new UsernamePasswordCredentials(guestName, guestPassword);
        initialState.setCredentials(AuthScope.ANY, loginCredentials);
        
        HttpClient client = new HttpClient();
        client.setState(initialState);
        
        String url = getRestCalendarUrl(OWNER_NAME);
        System.out.println("REST URL: " + url);
        HttpMethod method = new GetMethod(url);
        
        executeHttpMethod(client, method);
    }
    
    
    public static void main(String[] args) throws Exception {
        // TestUtil.cliSetup();
        CliUtil.toolSetup();
        TestUtil.runTest(TestAccessKeyGrant.class);
    }
}
