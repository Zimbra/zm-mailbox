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
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
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
    protected ExternalVerificationOutcome verifyPasswordExternal(Account acct, String password,
            Map<String, Object> authCtxt) {
        if (Strings.isNullOrEmpty(LC.keycloak_base_url.value())) {
            // Not configured - this POC's "off switch". Registered but inert.
            return ExternalVerificationOutcome.NOT_SUPPORTED;
        }

        long openUntil = sCircuitOpenUntilMillis.get();
        if (System.currentTimeMillis() < openUntil) {
            logger().debug("Keycloak circuit breaker open for another %dms; skipping straight to fallback",
                    openUntil - System.currentTimeMillis());
            return new ExternalVerificationOutcome(ExternalVerificationResult.UNAVAILABLE, null);
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("username", acct.getName()));
        params.add(new BasicNameValuePair("password", password));

        try {
            TokenResult result = callKeycloakTokenEndpoint(params);
            // Reached Keycloak and got a definitive answer either way - the circuit stays closed.
            sCircuitOpenUntilMillis.set(0);
            return result.verified
                    ? new ExternalVerificationOutcome(ExternalVerificationResult.VERIFIED, result.refreshToken)
                    : new ExternalVerificationOutcome(ExternalVerificationResult.REJECTED, null);
        } catch (Exception e) {
            // Never log the password. acct.getName() only.
            logger().warn("Keycloak verification unavailable for %s, opening circuit breaker for %dms: %s",
                    acct.getName(), LC.keycloak_circuit_breaker_cooldown_ms.longValue(), e.getMessage());
            sCircuitOpenUntilMillis.set(System.currentTimeMillis() + LC.keycloak_circuit_breaker_cooldown_ms.longValue());
            return new ExternalVerificationOutcome(ExternalVerificationResult.UNAVAILABLE, null);
        }
    }

    @Override
    protected ExternalVerificationOutcome refreshExternal(Account acct, String refreshToken,
            Map<String, Object> authCtxt) {
        if (Strings.isNullOrEmpty(LC.keycloak_base_url.value())) {
            return ExternalVerificationOutcome.NOT_SUPPORTED;
        }

        long openUntil = sCircuitOpenUntilMillis.get();
        if (System.currentTimeMillis() < openUntil) {
            logger().debug("Keycloak circuit breaker open for another %dms; refresh for %s has no fallback",
                    openUntil - System.currentTimeMillis(), acct.getName());
            return new ExternalVerificationOutcome(ExternalVerificationResult.UNAVAILABLE, null);
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));

        try {
            TokenResult result = callKeycloakTokenEndpoint(params);
            sCircuitOpenUntilMillis.set(0);
            return result.verified
                    ? new ExternalVerificationOutcome(ExternalVerificationResult.VERIFIED, result.refreshToken)
                    : new ExternalVerificationOutcome(ExternalVerificationResult.REJECTED, null);
        } catch (Exception e) {
            logger().warn("Keycloak refresh unavailable for %s, opening circuit breaker for %dms: %s",
                    acct.getName(), LC.keycloak_circuit_breaker_cooldown_ms.longValue(), e.getMessage());
            sCircuitOpenUntilMillis.set(System.currentTimeMillis() + LC.keycloak_circuit_breaker_cooldown_ms.longValue());
            return new ExternalVerificationOutcome(ExternalVerificationResult.UNAVAILABLE, null);
        }
    }

    /** Outcome of one call to Keycloak's token endpoint, whichever grant type. */
    private static final class TokenResult {
        final boolean verified;
        final String refreshToken;

        TokenResult(boolean verified, String refreshToken) {
            this.verified = verified;
            this.refreshToken = refreshToken;
        }
    }

    /**
     * Calls Keycloak's token endpoint with the given grant-specific params (grant_type plus
     * either username/password or refresh_token) and the confidential client_id/client_secret
     * common to both grants. Both mailstore and this call are trusted, confidential-client,
     * server-to-server - the mobile app never talks to Keycloak directly (§1.1). The access_token
     * itself is discarded unread; only the refresh_token (if any) is kept, as an opaque value the
     * mobile app can hand back later without ever needing to understand it's a Keycloak concept.
     *
     * @throws IOException on anything other than a clean accept/reject - unreachable, timed out,
     *         or an unexpected response - callers must treat this as UNAVAILABLE, never as a
     *         rejected credential.
     */
    private TokenResult callKeycloakTokenEndpoint(List<NameValuePair> grantParams) throws IOException {
        String tokenUrl = LC.keycloak_base_url.value() + "/realms/" + LC.keycloak_realm.value()
                + "/protocol/openid-connect/token";

        List<NameValuePair> params = new ArrayList<NameValuePair>(grantParams);
        params.add(new BasicNameValuePair("client_id", LC.keycloak_client_id.value()));
        params.add(new BasicNameValuePair("client_secret", LC.keycloak_client_secret.value()));
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
            String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), "UTF-8");
            if (status == 200) {
                String refreshToken = null;
                try {
                    JSONObject json = new JSONObject(body);
                    refreshToken = json.optString("refresh_token", null);
                } catch (JSONException e) {
                    // Unparseable success body is unexpected enough to treat as unavailable
                    // rather than silently issuing a token with no refresh capability.
                    throw new IOException("could not parse Keycloak token response", e);
                }
                return new TokenResult(true, refreshToken);
            } else if (status == 400 || status == 401) {
                // Keycloak reached and it explicitly rejected the credential/refresh token - a
                // real auth failure, not an availability problem. Do not throw here (§3.5
                // false-fallback risk).
                return new TokenResult(false, null);
            } else {
                throw new IOException("unexpected Keycloak token endpoint status " + status);
            }
        } catch (Exception  e) {
            throw new IOException(e);
        } finally {
            post.releaseConnection();
        }
    }
}
