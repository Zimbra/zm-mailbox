/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public class TestCommunityIntegration extends TestCase {
    private Provisioning mProv  = Provisioning.getInstance();
    private String DOMAIN_NAME = "testcommunity.domain.com";
    private String USER_NAME = "user1";
    private String COMMUNITY_BASE_URL = "http://zimbra.community.testcommunity.domain.com";
    private String COMMUNITY_HOME_URL = "/integration/zimbracollaboration";
    private String COMMUNITY_CLIENT_ID = "abcd";
    private String COMMUNITY_CLIENT_SECRET = "secret";
    Domain domain = null;
    Account testUser = null;

    @Override
    @Before
    public void setUp() throws Exception {
        cleanup();
        // create domain
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraCommunityAPIClientID, COMMUNITY_CLIENT_ID);
        attrs.put(Provisioning.A_zimbraCommunityAPIClientSecret, COMMUNITY_CLIENT_SECRET);
        attrs.put(Provisioning.A_zimbraCommunityUsernameMapping, "uid");
        attrs.put(Provisioning.A_zimbraCommunityBaseURL, COMMUNITY_BASE_URL);
        attrs.put(Provisioning.A_zimbraCommunityHomeURL, COMMUNITY_HOME_URL);
        domain = mProv.createDomain(DOMAIN_NAME, attrs);
        assertNotNull(domain);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraFeatureSocialExternalEnabled,ProvisioningConstants.TRUE);
        testUser = mProv.createAccount(USER_NAME.concat("@").concat(DOMAIN_NAME), TestUtil.DEFAULT_PASSWORD, attrs);
    }


    @Override
    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() throws Exception {
        Account a = mProv.getAccountByName(USER_NAME.concat("@").concat(DOMAIN_NAME));
        if(a != null) {
            mProv.deleteAccount(a.getId());
        }
        Domain d = mProv.getDomainByName(DOMAIN_NAME);
        if(d != null) {
            mProv.deleteDomain(d.getId());
        }
    }

    public void testGetInfo()
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(testUser.getName());
        request.addElement(AccountConstants.E_PASSWORD).setText(TestUtil.DEFAULT_PASSWORD);

        Element response = transport.invoke(request);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);

        request = Element.create(transport.getRequestProtocol(), AccountConstants.GET_INFO_REQUEST);
        response = transport.invoke(request);
        String communityURL = response.getAttribute(AccountConstants.E_COMMUNITY_URL);
        assertNotNull(communityURL);
        assertTrue("community URL begin with "+COMMUNITY_BASE_URL, communityURL.startsWith(COMMUNITY_BASE_URL));
        assertTrue("community URL should contain username="+testUser.getUid(),communityURL.contains(String.format("username=%s",testUser.getUid())));
        assertTrue("community URL should contain redirect_uri="+COMMUNITY_HOME_URL + " actual: "+communityURL, communityURL.contains(String.format("redirect_uri=%s",URLEncoder.encode(COMMUNITY_BASE_URL.concat(COMMUNITY_HOME_URL),"UTF8"))));
        assertTrue("community URL should contain configured client_id="+COMMUNITY_CLIENT_ID,communityURL.contains(String.format("client_id=%s&",COMMUNITY_CLIENT_ID)));
    }

}
