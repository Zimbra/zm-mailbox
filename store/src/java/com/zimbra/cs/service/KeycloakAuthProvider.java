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

// ZCS-20285: Keycloak-backed password verification with a fallback to the existing direct
// LDAP bind. See poc-implementation-guide.md §3.5 for the full design and the health-check /
// fallback rules this class implements. Registered in AuthProvider's static initializer;
// inert (NOT_SUPPORTED) until an operator opts in by setting keycloak_base_url and adding
// "keycloak" to zimbra_auth_provider (e.g. `zmlocalconfig -e zimbra_auth_provider=zimbra,keycloak`).
package com.zimbra.cs.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Strings;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;


public class KeycloakAuthProvider extends AuthProvider {

    public static final String KEYCLOAK_AUTH_PROVIDER = "keycloak";

    // Circuit breaker: 0 means closed (Keycloak assumed reachable). While
    // System.currentTimeMillis() < circuitOpenUntilMillis, verifyPasswordExternal skips the
    // network call entirely and returns UNAVAILABLE immediately - no repeated timeouts on every
    // login while Keycloak is down. Cleared on any call that reaches Keycloak, VERIFIED or
    // REJECTED alike; only a failure to reach it at all re-opens the circuit.
    private static final AtomicLong sCircuitOpenUntilMillis = new AtomicLong(0);

    public KeycloakAuthProvider() {
        super(KEYCLOAK_AUTH_PROVIDER);
    }

    // This provider exists only for verifyPasswordExternal - it doesn't parse or mint tokens.
    @Override
    protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq)
    throws AuthProviderException, AuthTokenException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    @Override
    protected AuthToken authToken(Element soapCtxt, Map engineCtxt)
    throws AuthProviderException, AuthTokenException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    @Override
    protected ExternalVerificationResult verifyPasswordExternal(Account acct, String password,
            Map<String, Object> authCtxt) {
        if (Strings.isNullOrEmpty(LC.keycloak_base_url.value())) {
            // Not configured - this POC's "off switch". Registered but inert.
            return ExternalVerificationResult.NOT_SUPPORTED;
        }

        long openUntil = sCircuitOpenUntilMillis.get();
        if (System.currentTimeMillis() < openUntil) {
            logger().debug("Keycloak circuit breaker open for another %dms; skipping straight to fallback",
                    openUntil - System.currentTimeMillis());
            return ExternalVerificationResult.UNAVAILABLE;
        }

        try {
            boolean verified = callKeycloakTokenEndpoint(acct, password);
            // Reached Keycloak and got a definitive answer either way - the circuit stays closed.
            sCircuitOpenUntilMillis.set(0);
            return verified ? ExternalVerificationResult.VERIFIED : ExternalVerificationResult.REJECTED;
        } catch (Exception e) {
            // Never log the password. acct.getName() only.
            logger().warn("Keycloak verification unavailable for %s, opening circuit breaker for %dms: %s",
                    acct.getName(), LC.keycloak_circuit_breaker_cooldown_ms.longValue(), e.getMessage());
            sCircuitOpenUntilMillis.set(System.currentTimeMillis() + LC.keycloak_circuit_breaker_cooldown_ms.longValue());
            return ExternalVerificationResult.UNAVAILABLE;
        }
    }

    /**
     * Direct Access Grant (OAuth2 "password" grant) against Keycloak's token endpoint.
     * Both mailstore and this call are trusted, confidential-client, server-to-server - the
     * mobile app never sees this credential flow (§1.1). We only need a yes/no; the returned
     * access token itself is discarded unread, not stored or forwarded anywhere.
     *
     * @return true if Keycloak accepted the credential, false if it explicitly rejected it
     * @throws IOException on anything else - unreachable, timed out, or an unexpected response -
     *         callers must treat this as UNAVAILABLE, never as a rejected credential.
     */
    private boolean callKeycloakTokenEndpoint(Account acct, String password) throws IOException {
        String tokenUrl = LC.keycloak_base_url.value() + "/realms/" + LC.keycloak_realm.value()
                + "/protocol/openid-connect/token";

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_id", LC.keycloak_client_id.value()));
        params.add(new BasicNameValuePair("client_secret", LC.keycloak_client_secret.value()));
        params.add(new BasicNameValuePair("username", acct.getName()));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("scope", "openid"));

        HttpPost post = new HttpPost(tokenUrl);
        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        RequestConfig reqConfig = RequestConfig.custom()
                .setConnectTimeout(LC.keycloak_connect_timeout_ms.intValue())
                .setSocketTimeout(LC.keycloak_socket_timeout_ms.intValue())
                .setConnectionRequestTimeout(LC.keycloak_connect_timeout_ms.intValue())
                .build();
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        clientBuilder.setDefaultRequestConfig(reqConfig);

        try {
            HttpResponse response = HttpClientUtil.executeMethod(clientBuilder.build(), post);
            int status = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());
            if (status == 200) {
                return true;
            } else if (status == 400 || status == 401) {
                // Keycloak reached and it explicitly rejected the credential - a real auth
                // failure, not an availability problem. Do not throw here (§3.5 false-fallback risk).
                return false;
            } else {
                throw new IOException("unexpected Keycloak token endpoint status " + status);
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            post.releaseConnection();
        }
    }
}
