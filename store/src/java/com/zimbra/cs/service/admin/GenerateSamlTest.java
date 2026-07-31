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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.SamlTestNonce;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GenerateSamlTestRequest;
import com.zimbra.soap.admin.message.GenerateSamlTestResponse;
import com.zimbra.soap.admin.type.DomainSelector;

/**
 * Admin SOAP handler for {@code GenerateSamlTestRequest}. Begins a SAML configuration test: resolves
 * the test origin, generates a nonce (persisted in {@code zimbraSamlTestNonce}), and returns the
 * pop-up URL — carrying the nonce in {@code RelayState} — that drives the test SSO flow through the
 * SAML extension.
 */
public class GenerateSamlTest extends AdminDocumentHandler {

    private static final String SAMLLOGIN_PATH = "/service/extension/samllogin";

    /**
     * Domain admins may run the test against their own domain (the global-config variant is gated on
     * the global-config right below).
     */
    @Override
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        final ZimbraSoapContext zsc = getZimbraSoapContext(context);
        final Provisioning prov = Provisioning.getInstance();
        final GenerateSamlTestRequest req = JaxbUtil.elementToJaxb(request);

        // Resolve the target entry: a specific domain, or global config when no domain is given.
        final DomainSelector ds = req.getDomain();
        final Config config = prov.getConfig();
        final Entry entry;
        if (ds != null) {
            final Domain domain = prov.get(ds.getBy().toKeyDomainBy(), ds.getKey());
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(ds.getKey());
            }
            checkDomainRight(zsc, domain, Admin.R_modifyDomain);
            entry = domain;
        } else {
            checkRight(zsc, context, config, Admin.R_modifyGlobalConfig);
            entry = config;
        }

        final long now = System.currentTimeMillis();

        // Conflict: another (unexpired) test is in progress. Require force to overwrite.
        final boolean force = Boolean.TRUE.equals(req.getForce());
        final String currentNonce = entry.getAttr(Provisioning.A_zimbraSamlTestNonce, null);
        if (!force && SamlTestNonce.hasActiveNonce(currentNonce, now)) {
            final String existingTimestamp = entry.getAttr(Provisioning.A_zimbraSamlTestTimestamp, null);
            final String existingError = entry.getAttr(Provisioning.A_zimbraSamlTestErrorMessage, null);
            final boolean previousTestFailed = !StringUtil.isNullOrEmpty(existingTimestamp)
                    && !StringUtil.isNullOrEmpty(existingError);
            if (!previousTestFailed) {
                // No result recorded yet — test is genuinely still in progress.
                throw SamlServiceException.TEST_IN_PROGRESS(
                        "A SAML test is already in progress; retry with force=1 to override.");
            }
            // previous test closed with a failure — nonce is stale, fall through to start a fresh test.
            // the unconditional modifyAttrs below will overwrite nonce, timestamp(empty), and error(empty).
        }

        // Determine the test origin (scheme+host) to build the pop-up URL on.
        final String origin = resolveTestOrigin(entry, config);
        if (origin == null) {
            throw SamlServiceException.TEST_ORIGIN_MISSING(
                    "Unable to determine test origin: neither zimbraWebClientLoginURL nor "
                            + "zimbraPublicServiceHostname is configured.");
        }

        // Generate + persist the nonce, and clear any stale result from a prior test.
        final String nonce = SamlTestNonce.generate();
        final Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraSamlTestNonce,
                SamlTestNonce.formatStoredValue(nonce, now + SamlTestNonce.NONCE_VALIDITY_MS));
        attrs.put(Provisioning.A_zimbraSamlTestTimestamp, "");
        attrs.put(Provisioning.A_zimbraSamlTestErrorMessage, "");
        prov.modifyAttrs(entry, attrs);

        final String relayState = SamlTestNonce.encodeRelayState(nonce, null);
        final String url = buildLoginUrl(origin, relayState);

        final GenerateSamlTestResponse resp = new GenerateSamlTestResponse(url, relayState);
        return zsc.jaxbToElement(resp);
    }

    /**
     * Resolve the origin for the test pop-up: prefer {@code zimbraWebClientLoginURL} (scheme+host),
     * else fall back to {@code zimbraPublicServiceHostname} (assumed https). Domain values fall back
     * to global config.
     */
    private String resolveTestOrigin(Entry entry, Config config) {
        final String loginUrl = firstNonEmpty(entry, config, Provisioning.A_zimbraWebClientLoginURL);
        if (!StringUtil.isNullOrEmpty(loginUrl)) {
            try {
                final URI uri = new URI(loginUrl.trim());
                if (uri.getScheme() != null && uri.getAuthority() != null) {
                    return uri.getScheme() + "://" + uri.getAuthority();
                }
            } catch (final URISyntaxException e) {
                // fall through to the public service hostname
            }
        }
        final String publicHost = firstNonEmpty(entry, config, Provisioning.A_zimbraPublicServiceHostname);
        if (!StringUtil.isNullOrEmpty(publicHost)) {
            return "https://" + publicHost.trim();
        }
        return null;
    }

    /** Value from the entry, falling back to global config when the entry does not set it. */
    private String firstNonEmpty(Entry entry, Config config, String attr) {
        final String value = entry.getAttr(attr, null);
        if (!StringUtil.isNullOrEmpty(value)) {
            return value;
        }
        return (entry == config) ? null : config.getAttr(attr, null);
    }

    private String buildLoginUrl(String origin, String relayState) throws ServiceException {
        try {
            return new URI(origin).resolve(SAMLLOGIN_PATH).toString()
                    + "?" + SamlTestNonce.RELAY_STATE_PARAM + "=" + URLEncoder.encode(relayState, "UTF-8");
        } catch (final Exception e) {
            throw ServiceException.FAILURE("Unable to build SAML test login URL", e);
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_modifyDomain);
        relatedRights.add(Admin.R_modifyGlobalConfig);
        notes.add("Requires the right to modify the target domain, or the global config right when no "
                + "domain is specified.");
    }
}
