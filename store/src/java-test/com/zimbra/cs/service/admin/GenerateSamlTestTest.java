/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.SamlTestNonce;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.GenerateSamlTestRequest;
import com.zimbra.soap.admin.type.DomainSelector;

public class GenerateSamlTestTest {

    private static final String DOMAIN = "test.com";
    private static final String ADMIN = "admin@test.com";
    private static final String LOGIN_PATH = "/service/extension/samllogin";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain(DOMAIN, Maps.newHashMap());
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraIsAdminAccount, true);
        prov.createAccount(ADMIN, "secret", attrs);
        RightManager.getInstance().getAllAdminRights();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        // reset the SAML test-related attrs so each test is independent
        setDomainAttrs("", "", "", "", "");
        setConfigAttrs("", "");
    }

    private void setDomainAttrs(String loginUrl, String publicHost, String nonce, String ts, String err)
            throws ServiceException {
        Domain domain = Provisioning.getInstance().get(Key.DomainBy.name, DOMAIN);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURL, loginUrl);
        attrs.put(Provisioning.A_zimbraPublicServiceHostname, publicHost);
        attrs.put(Provisioning.A_zimbraSamlTestNonce, nonce);
        attrs.put(Provisioning.A_zimbraSamlTestTimestamp, ts);
        attrs.put(Provisioning.A_zimbraSamlTestErrorMessage, err);
        Provisioning.getInstance().modifyAttrs(domain, attrs);
    }

    private void setConfigAttrs(String loginUrl, String publicHost) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraWebClientLoginURL, loginUrl);
        attrs.put(Provisioning.A_zimbraPublicServiceHostname, publicHost);
        attrs.put(Provisioning.A_zimbraSamlTestNonce, "");
        Provisioning.getInstance().modifyAttrs(config, attrs);
    }

    private Element invoke(DomainSelector ds, Boolean force) throws Exception {
        GenerateSamlTestRequest request = new GenerateSamlTestRequest(ds, force);
        Element req = JaxbUtil.jaxbToElement(request);
        GenerateSamlTest handler = new GenerateSamlTest();
        handler.setResponseQName(AdminConstants.GENERATE_SAML_TEST_RESPONSE);
        Account admin = Provisioning.getInstance().get(Key.AccountBy.name, ADMIN);
        return handler.handle(req, ServiceTestUtil.getRequestContext(admin));
    }

    private String domainNonce() throws ServiceException {
        return Provisioning.getInstance().get(Key.DomainBy.name, DOMAIN)
                .getAttr(Provisioning.A_zimbraSamlTestNonce, null);
    }

    @Test
    public void domainTestSuccessBuildsLoginUrlAndPersistsNonce() throws Exception {
        // Active nonce + recorded failure — should fall through and start a fresh test (not throw).
        String staleNonce = SamlTestNonce.formatStoredValue("stale", System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/zimbra/", "", staleNonce,
                "20200101000000Z", "SSO:OLD_ERROR");

        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);

        String url = resp.getAttribute(AdminConstants.A_URL);
        assertTrue("url should target the login path on the login URL origin: " + url,
                url.startsWith("https://mail.test.com" + LOGIN_PATH + "?RelayState="));

        // new nonce persisted + active, and echoed in the RelayState
        String stored = domainNonce();
        assertFalse("stale nonce should have been replaced", staleNonce.equals(stored));
        assertTrue(SamlTestNonce.hasActiveNonce(stored, System.currentTimeMillis()));
        String relayState = resp.getAttribute(AdminConstants.A_RELAY_STATE);
        String nonceInRelay = SamlTestNonce.getNonceFromRelayState(relayState);
        assertTrue(SamlTestNonce.isValid(nonceInRelay, stored, System.currentTimeMillis()));

        // stale result cleared by the unconditional modifyAttrs
        Domain domain = Provisioning.getInstance().get(Key.DomainBy.name, DOMAIN);
        assertEquals("", domain.getAttr(Provisioning.A_zimbraSamlTestTimestamp, ""));
        assertEquals("", domain.getAttr(Provisioning.A_zimbraSamlTestErrorMessage, ""));
    }

    @Test
    public void activeNonceWithPartialResultStillThrows() throws Exception {
        // Only timestamp set, error empty — previousTestFailed is false, should still throw.
        String active = SamlTestNonce.formatStoredValue("existing", System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/", "", active, "20260806120000Z", "");
        try {
            invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
            fail("expected TEST_IN_PROGRESS");
        } catch (ServiceException e) {
            assertEquals(SamlServiceException.TEST_IN_PROGRESS, e.getCode());
        }
        // nonce untouched
        assertEquals(active, domainNonce());
    }

    @Test
    public void originFallsBackToPublicServiceHostname() throws Exception {
        setDomainAttrs("", "mail2.test.com", "", "", "");
        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
        String url = resp.getAttribute(AdminConstants.A_URL);
        assertTrue("url should use https + public hostname: " + url,
                url.startsWith("https://mail2.test.com" + LOGIN_PATH + "?RelayState="));
    }

    @Test
    public void missingOriginThrowsTestOriginMissing() throws Exception {
        setDomainAttrs("", "", "", "", "");
        setConfigAttrs("", "");
        try {
            invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
            fail("expected TEST_ORIGIN_MISSING");
        } catch (ServiceException e) {
            assertEquals(SamlServiceException.TEST_ORIGIN_MISSING, e.getCode());
        }
    }

    @Test
    public void conflictWithActiveNonceThrows() throws Exception {
        String active = SamlTestNonce.formatStoredValue("existing", System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/", "", active, "", "");
        try {
            invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
            fail("expected TEST_IN_PROGRESS");
        } catch (ServiceException e) {
            assertEquals(SamlServiceException.TEST_IN_PROGRESS, e.getCode());
        }
        // nonce untouched
        assertEquals(active, domainNonce());
    }

    @Test
    public void forceOverridesActiveNonce() throws Exception {
        String active = SamlTestNonce.formatStoredValue("existing", System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/", "", active, "", "");
        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), Boolean.TRUE);
        assertNotNull(resp.getAttribute(AdminConstants.A_URL));
        assertFalse("nonce should have been replaced", active.equals(domainNonce()));
    }

    @Test
    public void expiredNonceIsNotAConflict() throws Exception {
        String expired = SamlTestNonce.formatStoredValue("old", System.currentTimeMillis() - 60000);
        setDomainAttrs("https://mail.test.com/", "", expired, "", "");
        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
        assertNotNull(resp.getAttribute(AdminConstants.A_URL));
        assertFalse("expired nonce should have been replaced", expired.equals(domainNonce()));
    }

    @Test
    public void globalConfigTestPersistsNonceOnConfig() throws Exception {
        setConfigAttrs("https://global.test.com/", "");
        Element resp = invoke(null, null);
        String url = resp.getAttribute(AdminConstants.A_URL);
        assertTrue(url.startsWith("https://global.test.com" + LOGIN_PATH + "?RelayState="));
        String stored = Provisioning.getInstance().getConfig()
                .getAttr(Provisioning.A_zimbraSamlTestNonce, null);
        assertTrue(SamlTestNonce.hasActiveNonce(stored, System.currentTimeMillis()));
    }
}
