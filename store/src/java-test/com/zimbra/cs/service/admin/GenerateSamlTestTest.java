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

import com.zimbra.common.account.Key;
import com.zimbra.common.account.SamlTestNonce;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.GenerateSamlTestRequest;
import com.zimbra.soap.admin.message.GenerateSamlTestResponse;
import com.zimbra.soap.admin.type.DomainSelector;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GenerateSamlTestTest {

    private static final String DOMAIN = "test.com";

    private static final String LOGIN_PATH = "/service/extension/samllogin";

    @Rule
    public TestName testName = new TestName();

    // -----------------------------------------------------------------------
    // Core helper: write attrs into an Entry by calling entry.setAttrs().
    //
    // Entry.getAttrs(false) returns a NEW HashMap copy of mAttrs — writing
    // into that copy has zero effect on what getAttr() returns.
    // Entry.setAttrs(Map) replaces mAttrs directly and calls resetData(),
    // so subsequent getAttr() calls read the new values immediately.
    // -----------------------------------------------------------------------
    private static void applyAttrs(Entry entry, Map<String, Object> delta) {
        // Start from the current live attrs so we don't lose unrelated attrs.
        Map<String, Object> merged = new HashMap<>(entry.getAttrs(false));
        for (Map.Entry<String, Object> kv : delta.entrySet()) {
            String val = kv.getValue() instanceof String ? (String) kv.getValue() : null;
            if (val == null || val.isEmpty()) {
                merged.remove(kv.getKey());
            } else {
                merged.put(kv.getKey(), val);
            }
        }
        entry.setAttrs(merged);
    }

    // -----------------------------------------------------------------------
    // Test double — bypasses ZimbraSoapContext / AuthToken entirely.
    // Duplicates handler logic; skips ACL checks; uses JaxbUtil to serialise.
    // Also uses applyAttrs() for its own persistence so MockProvisioning
    // no-op modifyAttrs() is never relied upon.
    // -----------------------------------------------------------------------
    private static final class NoAuthGenerateSamlTest extends GenerateSamlTest {

        @Override
        public Element handle(Element request, Map<String, Object> context) throws ServiceException {
            final Provisioning prov = Provisioning.getInstance();
            final GenerateSamlTestRequest req = JaxbUtil.elementToJaxb(request);

            final DomainSelector ds = req.getDomain();
            final Config config = prov.getConfig();
            final Entry entry;
            if (ds != null) {
                final Domain domain = prov.get(ds.getBy().toKeyDomainBy(), ds.getKey());
                if (domain == null) {
                    throw AccountServiceException.NO_SUCH_DOMAIN(ds.getKey());
                }
                entry = domain;
            } else {
                entry = config;
            }

            final long now = System.currentTimeMillis();
            final boolean force = Boolean.TRUE.equals(req.getForce());
            final String currentNonce = entry.getAttr(Provisioning.A_zimbraSamlTestNonce, null);

            if (!force && SamlTestNonce.hasActiveNonce(currentNonce, now)) {
                final String existingTimestamp =
                        entry.getAttr(Provisioning.A_zimbraSamlTestTimestamp, null);
                final String existingError =
                        entry.getAttr(Provisioning.A_zimbraSamlTestErrorMessage, null);
                final boolean previousTestFailed =
                        !StringUtil.isNullOrEmpty(existingTimestamp)
                                && !StringUtil.isNullOrEmpty(existingError);
                if (!previousTestFailed) {
                    throw SamlServiceException.TEST_IN_PROGRESS(
                            "A SAML test is already in progress; retry with force=1 to override.");
                }
            }

            final String origin = resolveTestOrigin(entry, config);
            if (origin == null) {
                throw SamlServiceException.TEST_ORIGIN_MISSING(
                        "Unable to determine test origin: neither zimbraWebClientLoginURL nor "
                                + "zimbraPublicServiceHostname is configured.");
            }

            // Persist nonce + clear stale result via setAttrs() — the only
            // method that actually writes through to mAttrs on MockProvisioning.
            final String nonce = SamlTestNonce.generate();
            final Map<String, Object> update = new HashMap<>();
            update.put(Provisioning.A_zimbraSamlTestNonce,
                    SamlTestNonce.formatStoredValue(nonce, now + SamlTestNonce.NONCE_VALIDITY_MS));
            update.put(Provisioning.A_zimbraSamlTestTimestamp, "");
            update.put(Provisioning.A_zimbraSamlTestErrorMessage, "");
            applyAttrs(entry, update);

            final String relayState = SamlTestNonce.encodeRelayState(nonce, null);
            final String url = buildLoginUrl(origin, relayState);

            return JaxbUtil.jaxbToElement(new GenerateSamlTestResponse(url, relayState));
        }

        private String resolveTestOrigin(Entry entry, Config config) {
            final String loginUrl = firstNonEmpty(entry, config,
                    Provisioning.A_zimbraWebClientLoginURL);
            if (!StringUtil.isNullOrEmpty(loginUrl)) {
                try {
                    final URI uri = new URI(loginUrl.trim());
                    if (uri.getScheme() != null && uri.getAuthority() != null) {
                        return uri.getScheme() + "://" + uri.getAuthority();
                    }
                } catch (java.net.URISyntaxException e) {
                    // fall through to publicServiceHostname
                }
            }
            final String publicHost = firstNonEmpty(entry, config,
                    Provisioning.A_zimbraPublicServiceHostname);
            if (!StringUtil.isNullOrEmpty(publicHost)) {
                return "https://" + publicHost.trim();
            }
            return null;
        }

        private String firstNonEmpty(Entry entry, Config config, String attr) {
            final String value = entry.getAttr(attr, null);
            if (!StringUtil.isNullOrEmpty(value)) {
                return value;
            }
            return (entry == config) ? null : config.getAttr(attr, null);
        }

        private String buildLoginUrl(String origin, String relayState) throws ServiceException {
            try {
                return new URI(origin).resolve(LOGIN_PATH).toString()
                        + "?" + SamlTestNonce.RELAY_STATE_PARAM + "="
                        + URLEncoder.encode(relayState, "UTF-8");
            } catch (Exception e) {
                throw ServiceException.FAILURE("Unable to build SAML test login URL", e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.getInstance().createDomain(DOMAIN, new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        // Reset all SAML attrs to empty on both domain and config before each test.
        resetDomainAttrs();
        resetConfigAttrs();
    }

    // -----------------------------------------------------------------------
    // Attr helpers — use entry.setAttrs() to write through to mAttrs
    // -----------------------------------------------------------------------

    private static Domain getTestDomain() throws ServiceException {
        return Provisioning.getInstance().get(Key.DomainBy.name, DOMAIN);
    }

    private static Config getConfig() throws ServiceException {
        return Provisioning.getInstance().getConfig();
    }

    private static void resetDomainAttrs() throws ServiceException {
        Map<String, Object> clean = new HashMap<>(getTestDomain().getAttrs(false));
        clean.remove(Provisioning.A_zimbraWebClientLoginURL);
        clean.remove(Provisioning.A_zimbraPublicServiceHostname);
        clean.remove(Provisioning.A_zimbraSamlTestNonce);
        clean.remove(Provisioning.A_zimbraSamlTestTimestamp);
        clean.remove(Provisioning.A_zimbraSamlTestErrorMessage);
        getTestDomain().setAttrs(clean);
    }

    private static void resetConfigAttrs() throws ServiceException {
        Map<String, Object> clean = new HashMap<>(getConfig().getAttrs(false));
        clean.remove(Provisioning.A_zimbraWebClientLoginURL);
        clean.remove(Provisioning.A_zimbraPublicServiceHostname);
        clean.remove(Provisioning.A_zimbraSamlTestNonce);
        getConfig().setAttrs(clean);
    }

    /**
     * Set domain SAML attrs directly into the entry's live map .
     * Empty string means remove the attr (same as absent).
     *
     * @param loginUrl   zimbraWebClientLoginURL value, or empty to clear
     * @param publicHost zimbraPublicServiceHostname value, or empty to clear
     * @param nonce      zimbraSamlTestNonce value, or empty to clear
     * @param ts         zimbraSamlTestTimestamp value, or empty to clear
     * @param err        zimbraSamlTestErrorMessage value, or empty to clear
     * @throws ServiceException if the domain cannot be retrieved
     */
    private static void setDomainAttrs(String loginUrl, String publicHost,
            String nonce, String ts, String err) throws ServiceException {
        Map<String, Object> delta = new HashMap<>();
        delta.put(Provisioning.A_zimbraWebClientLoginURL, loginUrl);
        delta.put(Provisioning.A_zimbraPublicServiceHostname, publicHost);
        delta.put(Provisioning.A_zimbraSamlTestNonce, nonce);
        delta.put(Provisioning.A_zimbraSamlTestTimestamp, ts);
        delta.put(Provisioning.A_zimbraSamlTestErrorMessage, err);
        applyAttrs(getTestDomain(), delta);
    }

    /**
     * Set config SAML attrs directly into the entry's live map.
     * Empty string means remove the attr (same as absent).
     *
     * @param loginUrl   zimbraWebClientLoginURL value, or empty to clear
     * @param publicHost zimbraPublicServiceHostname value, or empty to clear
     * @throws ServiceException if the config cannot be retrieved
     */
    private static void setConfigAttrs(String loginUrl, String publicHost) throws ServiceException {
        Map<String, Object> delta = new HashMap<>();
        delta.put(Provisioning.A_zimbraWebClientLoginURL, loginUrl);
        delta.put(Provisioning.A_zimbraPublicServiceHostname, publicHost);
        applyAttrs(getConfig(), delta);
    }

    private Element invoke(DomainSelector ds, Boolean force) throws Exception {
        GenerateSamlTestRequest request = new GenerateSamlTestRequest(ds, force);
        Element req = JaxbUtil.jaxbToElement(request);
        NoAuthGenerateSamlTest handler = new NoAuthGenerateSamlTest();
        handler.setResponseQName(AdminConstants.GENERATE_SAML_TEST_RESPONSE);
        return handler.handle(req, new HashMap<String, Object>());
    }

    private String domainNonce() throws ServiceException {
        return getTestDomain().getAttr(Provisioning.A_zimbraSamlTestNonce, null);
    }

    // -----------------------------------------------------------------------
    // Happy-path tests
    // -----------------------------------------------------------------------

    @Test
    public void domainTestSuccessBuildsLoginUrlAndPersistsNonce() throws Exception {
        String staleNonce = SamlTestNonce.formatStoredValue("stale",
                System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/zimbra/", "", staleNonce,
                "20200101000000Z", "SSO:OLD_ERROR");

        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);

        String url = resp.getAttribute(AdminConstants.A_URL);
        assertTrue("url should target the login path on the login URL origin: " + url,
                url.startsWith("https://mail.test.com" + LOGIN_PATH + "?RelayState="));

        String stored = domainNonce();
        assertFalse("stale nonce should have been replaced", staleNonce.equals(stored));
        assertTrue(SamlTestNonce.hasActiveNonce(stored, System.currentTimeMillis()));

        String relayState = resp.getAttribute(AdminConstants.A_RELAY_STATE);
        String nonceInRelay = SamlTestNonce.getNonceFromRelayState(relayState);
        assertTrue(SamlTestNonce.isValid(nonceInRelay, stored, System.currentTimeMillis()));

        Domain domain = getTestDomain();
        assertEquals("", domain.getAttr(Provisioning.A_zimbraSamlTestTimestamp, ""));
        assertEquals("", domain.getAttr(Provisioning.A_zimbraSamlTestErrorMessage, ""));
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
    public void expiredNonceIsNotAConflict() throws Exception {
        String expired = SamlTestNonce.formatStoredValue("old",
                System.currentTimeMillis() - 60000);
        setDomainAttrs("https://mail.test.com/", "", expired, "", "");
        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
        assertNotNull(resp.getAttribute(AdminConstants.A_URL));
        assertFalse("expired nonce should have been replaced", expired.equals(domainNonce()));
    }

    @Test
    public void forceOverridesActiveNonce() throws Exception {
        String active = SamlTestNonce.formatStoredValue("existing",
                System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/", "", active, "", "");
        Element resp = invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), Boolean.TRUE);
        assertNotNull(resp.getAttribute(AdminConstants.A_URL));
        assertFalse("nonce should have been replaced", active.equals(domainNonce()));
    }

    @Test
    public void globalConfigTestPersistsNonceOnConfig() throws Exception {
        setConfigAttrs("https://global.test.com/", "");
        Element resp = invoke(null, null);
        String url = resp.getAttribute(AdminConstants.A_URL);
        assertTrue(url.startsWith("https://global.test.com" + LOGIN_PATH + "?RelayState="));
        String stored = getConfig().getAttr(Provisioning.A_zimbraSamlTestNonce, null);
        assertTrue(SamlTestNonce.hasActiveNonce(stored, System.currentTimeMillis()));
    }

    // -----------------------------------------------------------------------
    // Error / conflict tests
    // -----------------------------------------------------------------------

    @Test
    public void missingOriginThrowsTestOriginMissing() throws Exception {
        // Both domain and config have no loginUrl / publicHostname after setUp().
        try {
            invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
            fail("expected TEST_ORIGIN_MISSING");
        } catch (ServiceException e) {
            assertEquals(SamlServiceException.TEST_ORIGIN_MISSING, e.getCode());
        }
    }

    @Test
    public void conflictWithActiveNonceThrows() throws Exception {
        String active = SamlTestNonce.formatStoredValue("existing",
                System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/", "", active, "", "");
        try {
            invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
            fail("expected TEST_IN_PROGRESS");
        } catch (ServiceException e) {
            assertEquals(SamlServiceException.TEST_IN_PROGRESS, e.getCode());
        }
        assertEquals(active, domainNonce());
    }

    @Test
    public void activeNonceWithPartialResultStillThrows() throws Exception {
        String active = SamlTestNonce.formatStoredValue("existing",
                System.currentTimeMillis() + 60000);
        setDomainAttrs("https://mail.test.com/", "", active, "20260806120000Z", "");
        try {
            invoke(new DomainSelector(DomainSelector.DomainBy.name, DOMAIN), null);
            fail("expected TEST_IN_PROGRESS");
        } catch (ServiceException e) {
            assertEquals(SamlServiceException.TEST_IN_PROGRESS, e.getCode());
        }
        assertEquals(active, domainNonce());
    }

}