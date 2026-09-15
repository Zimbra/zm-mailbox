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

package com.zimbra.cs.account.auth.ropc.webhook;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.ropc.util.IRopcUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static com.zimbra.common.localconfig.LC.*;

public class IRopcWebhookServlet extends HttpServlet {

    private static final Cache<String, IRopcDomainConfig> DOMAIN_SECRET_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(mfa_idp_max_webhook_secret_cache_timeout_in_hours.intValue(), TimeUnit.HOURS)
            .build();

    private final Map<String, IRopcWebhookHandler> webhookHandlers = new HashMap<>();

    @Override
    public void init() throws ServletException {
        // register handlers. When Keycloak is added, simply add:
        // webhookHandlers.put("keyCloak", new KeyCloakWebhookHandler());
        webhookHandlers.put("okta", new OktaWebhookHandler());
    }

    /**
     * Handles one time verification Challenge (GET).
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        String detectedProvider = req.getParameter("provider");
        if (detectedProvider == null || detectedProvider.isEmpty()) {
            ZimbraLog.account.warn("IRopc Webhook(GET): Missing 'provider' query parameter in URL.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // fetch the exact handler for this provider
        IRopcWebhookHandler handler = webhookHandlers.get(detectedProvider.toLowerCase());
        if (handler == null) {
            ZimbraLog.account.warn("IRopc Webhook (GET): Unsupported provider requested : " + detectedProvider);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        handler.handleVerificationChallenge(req, response);
    }

    /**
     * Handles real time JSON Event Payload from IDP (POST).
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        try {
            // identify the provider from the URL
            String detectedProvider = req.getParameter("provider");

            if (detectedProvider == null || detectedProvider.isEmpty()) {
                ZimbraLog.account.warn("IRopc Webhook: Missing 'provider' query parameter in URL.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // fetch the exact handler for this provider
            IRopcWebhookHandler handler = webhookHandlers.get(detectedProvider.toLowerCase());

            if (handler == null) {
                ZimbraLog.account.warn("IRopc Webhook: Unsupported provider requested : " + detectedProvider);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // read the payload with strict mfa_idp_webhook_payload_max_size_limit
            final int MAX_PAYLOAD_SIZE = mfa_idp_webhook_payload_max_size_limit.intValue() * 1024;
            int charsRead = 0;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    charsRead += line.length();
                    // read the HTTP body with a strict limit to prevent memory/Dos attack
                    if (charsRead > MAX_PAYLOAD_SIZE) {
                        ZimbraLog.account.warn("IRopc Webhook: IDP WebHook payload exceeded limit. Rejecting.");
                        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                        return;
                    }
                    sb.append(line);
                }
            }
            String rawJsonString = sb.toString();

            // extract username from the registered handlers
            String username = handler.extractUsername(rawJsonString);

            if (username == null || !username.contains("@")) {
                ZimbraLog.account.warn("IRopc Webhook: Failed to extract valid username from the event payload for" +
                        "provider : " + detectedProvider);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String domainName = username.substring(username.indexOf('@') + 1);
            IRopcDomainConfig domainConfig = getCachedDomainConfig(domainName);

            if (domainConfig == null || domainConfig.getAuthSecret() == null) {
                ZimbraLog.account.error("IRopc Webhook: No webhook Secret configured for domain " + domainName);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // validate provider and secret matches domain config
            if (!detectedProvider.equalsIgnoreCase(domainConfig.getProvider())) {
                ZimbraLog.account.error("IRopc Webhook mismatch: Domain configured with provider as %s," +
                        " but request URL contains %s ",  domainConfig.getProvider(), detectedProvider);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String authHeader = req.getHeader(mfa_idp_webhook_secret_key.value());
            if (authHeader == null || !PasswordUtil.SSHA512.verifySSHA512(domainConfig.getAuthSecret(), authHeader)) {
                ZimbraLog.account.error("IRopc Webhook: Auth secret does not match. " +
                        "Session invalidation failed for %s ", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // lookup the account via LDAP
            Provisioning provisioning = Provisioning.getInstance();
            Account account = provisioning.get(AccountBy.name, username);

            if (account == null) {
                ZimbraLog.account.warn("IRopc Webhook: Account not found for " + username);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // check if the mailbox exists in the specific node
            if (Provisioning.onLocalServer(account)) {
                ZimbraLog.account.info("IRopc Webhook: Processing webhook event locally for " + username);
                IRopcUtil.clearCacheSession(account, username);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                // loop protection
                if (req.getHeader("X-Zimbra-Proxied") != null) {
                    ZimbraLog.account.warn("IRopc Webhook: Routing loop detected for webhook on " + username);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
                Server targetServer = account.getServer();
                if (targetServer == null) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
                // get the FQDN of target node
                String targetHost = targetServer.getServiceHostname();
                ZimbraLog.account.info("IRopc Webhook: Webhook event landed on wrong node. " +
                        "Proxying request to " + targetHost);
                boolean proxySuccessful = proxyToCorrectNode(targetHost, req.getRequestURI(),
                        rawJsonString, authHeader, detectedProvider);

                if (proxySuccessful) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (Exception e) {
            ZimbraLog.account.error("IRopc Webhook: Webhook event failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Forward the HTTP POST request to the correct node.
     *
     * @param targetHost
     * @param requestUri
     * @param jsonPayload
     * @param authHeader
     * @param provider
     *
     * @return boolean
     */
    protected boolean proxyToCorrectNode(String targetHost, String requestUri, String jsonPayload,
                                         String authHeader, String provider) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://" + targetHost + requestUri + "?provider=" + provider);
            conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // pass the original authentication header
            conn.setRequestProperty(mfa_idp_webhook_secret_key.value(), authHeader);


            // inject circuit-breaker header to prevent infinite loops
            conn.setRequestProperty("X-Zimbra-Proxied", "true");

            // write json payload to the target server
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = conn.getResponseCode();
            return responseCode == 200 || responseCode == 204;
        } catch (Exception e) {
            ZimbraLog.account.error("IRopc Webhook: Failed to proxy Webhook event to " + targetHost, e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Retrieves the secret from the memory cache. If missing, queries LDAP.
     *
     * @param domainName
     *
     * @return String
     */
    protected IRopcDomainConfig getCachedDomainConfig(String domainName) {
        try {
            // check guava cache first
            IRopcDomainConfig domainConfig = DOMAIN_SECRET_CACHE.getIfPresent(domainName);
            if (domainConfig != null) {
                return domainConfig;
            }

            // cache miss : query LDAP for the domain
            Provisioning prov = Provisioning.getInstance();
            Domain domain = prov.get(Key.DomainBy.name, domainName);

            if (domain != null) {
                String authMechString = domain.getAuthMech();
                String secret = parseAuthMechArgs(authMechString, "webhook_secret");
                String provider = parseAuthMechArgs(authMechString, "provider");
                if (secret != null && !secret.isEmpty() &&
                        provider != null && !provider.isEmpty()) {
                    String secretHash = PasswordUtil.SSHA512.generateSSHA512(secret, null);
                    domainConfig = new IRopcDomainConfig(provider, secretHash);
                    DOMAIN_SECRET_CACHE.put(domainName, domainConfig);
                    return domainConfig;
                }
            }
        } catch (Exception e) {
            ZimbraLog.account.error("IRopc Webhook: Failed to fetch secret from LDAP for domain: " + domainName, e);
            return null;
        }
        ZimbraLog.account.error("'Provider' or 'webhook_secret' is null or empty from LDAP " +
                "attribute for domain: " + domainName);
        return null;
    }

    /**
     * Extracts the specific idp config from the domain's zimbraAuthMech ARGS.
     *
     * @param authMech
     * @param key
     *
     * @return String
     */
    protected String parseAuthMechArgs(String authMech, String key) {
        if (authMech == null || key == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\"?" + key + "=([^\"]+)\"?");
        Matcher matcher = pattern.matcher(authMech);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
