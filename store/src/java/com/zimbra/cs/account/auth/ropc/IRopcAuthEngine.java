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

package com.zimbra.cs.account.auth.ropc;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.ropc.store.DbRopcTokenStore;
import com.zimbra.cs.account.auth.ropc.store.IRopcSessionRecord;
import com.zimbra.cs.account.auth.ropc.store.IRopcTokenCleanupScheduler;
import com.zimbra.cs.account.auth.ropc.store.IRopcTokenStore;
import com.zimbra.cs.account.auth.ropc.util.IRopcUtil;
import java.util.Map;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.ACCESS_EXPIRY_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.FACTOR;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.INTERVAL;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.POLLING_INTERVAL_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.POLLING_TIMEOUT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.POLLING_TIMEOUT_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.PROVIDER;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REFRESH;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CLIENT_ID;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.TOKEN_ENDPOINT;

public final class IRopcAuthEngine {

    private static final MFAPollingService POLLER = MFAPollingService.getInstance();

    private static final IRopcTokenStore STORE = IRopcCredCache.STORE;

    private IRopcAuthEngine() {
    }

    public static Outcome authenticate(Account account, final String user, final String password, final String deviceId,
                                       AuthContext.Protocol protocol, AuthContext.SubProtocol subProtocol,
                                       String userAgent, String ip, Map<String, String> configs,
                                       boolean fullAuthRequired, boolean optionsReq) {
        return doAuthenticate(account, user, password, deviceId, protocol, subProtocol, userAgent, ip, configs,
                fullAuthRequired, optionsReq);
    }

    private static Outcome doAuthenticate(Account account, String user, String password, String deviceId,
                                          AuthContext.Protocol protocol, AuthContext.SubProtocol subProtocol,
                                          String userAgent, String ip, Map<String, String> configs,
                                          boolean fullAuthRequired, boolean optionsReq) {
        try {
            String proto = subProtocol == null ? protocol.name() : subProtocol.name();
            verifyMandatoryParamsforAuth(user, password, configs, proto);

            // Lazy initialization of token cleanup scheduler
            // need to start only if user is opting for idp-ropc auth mech
            // and only if store is SQL DB
            if (!IRopcTokenCleanupScheduler.isStarted() && STORE instanceof DbRopcTokenStore) {
                IRopcTokenCleanupScheduler.start();
            }
            IRopcHandler handler = IROPCHandlerRegistry.get(configs.get(PROVIDER));
            String passwordHash = PasswordUtil.SSHA512.generateSSHA512(password, null);

            IRopcSessionRecord rec = IRopcUtil.findInStore(account, user, userAgent, configs.get(PROVIDER),
                    proto, deviceId, ip, password, STORE);
            // first check if refresh token exists in store and session is not expired
            // then check if the request token is valid
            if (rec != null && !rec.isHardSessionExpired() && rec.getRefreshToken() != null && !fullAuthRequired) {
                String refreshToken = decrypt(user, rec.getRefreshToken());
                configs.put(FACTOR, REFRESH);
                IRopcAuthResult authResult = handler.authenticate(IRopcAuthRequest.builder()
                        .username(user).refreshToken(refreshToken).ip(ip).deviceId(deviceId)
                        .userAgent(userAgent).config(configs).build());
                switch (authResult.getStatus()) {
                    case SUCCESS:
                        if (authResult.getRefreshToken() != null
                                && !refreshToken.equals(authResult.getRefreshToken())) {
                            persist(account, rec.getId(), user, userAgent, rec.getDeviceId(), ip, configs.get(PROVIDER),
                                    authResult.getRefreshToken(), authResult.getIdToken(), rec.getPasswordHash(), proto,
                                    rec.getCreatedAt(), authResult.getAccessTokenExpiry());
                        }
                        return Outcome.SUCCESS;
                    case INVALID_GRANT:
                        STORE.delete(account, rec.getId(), user, userAgent, configs.get(PROVIDER), proto,
                                rec.getDeviceId());
                        IRopcCredCache.invalidate(user, userAgent, proto, rec.getProvider(), ip, rec.getDeviceId());
                        ZimbraLog.account.error("Refresh token validation failed as token is expired, revoked for " +
                                        "user %s: %s", user, authResult.getErrorDescription());
                        return Outcome.TOKEN_EXPIRED;
                    case POLICY_DENIED:
                        ZimbraLog.account.error("Refresh token validation failed, IDP policy denied for " +
                                        "user %s: %s", user, authResult.getErrorDescription());
                        return Outcome.POLICY_DENIED;
                    default:
                        ZimbraLog.account.error("ROPC failed due following error for user %s: %s", user,
                                authResult.getErrorDescription());
                        return Outcome.ERROR;
                }
            }

            return fullAuth(account, handler, user, password, deviceId, proto, userAgent, ip, configs,
                    passwordHash, fullAuthRequired, optionsReq);
        } catch (Exception e) {
            ZimbraLog.account.error("Authentication Failed : Error occurred in IDP based auth for %s", user, e);
            return Outcome.ERROR;
        }
    }

    private static Outcome fullAuth(Account account, IRopcHandler handler, String user, String password,
                                    String deviceId, String proto, String userAgent, String ip,
                                    Map<String, String> configs, String passwordHash, boolean fullAuthForceful,
                                    boolean optionsReq) throws ServiceException {
        IRopcAuthResult ar = handler.authenticate(IRopcAuthRequest.builder().username(user).password(password)
                .ip(ip).deviceId(deviceId).userAgent(userAgent).config(configs).build());

        switch (ar.getStatus()) {
            case SUCCESS:
                ZimbraLog.account.info("Authentication SUCCESSFUL for user :  %s", user);
                // passing null deviceId, as nativeIOS app do not share device id on the first OPTIONS request
                // and outlook changes the device ID once the Microsoft cloud server registers the user.
                // device id changes in subsequent calls. Will append device id in subsequent calls
                String deviceIdForDb = fullAuthForceful || !optionsReq ? deviceId : null;
                persist(account, null, user, userAgent, deviceIdForDb, ip, configs.get(PROVIDER),
                        ar.getRefreshToken(), ar.getIdToken(), passwordHash, proto, System.currentTimeMillis(),
                        ar.getAccessTokenExpiry());
                return Outcome.SUCCESS;
            case MFA_CHALLENGE:
                return awaitPush(account, handler, ar.getChallenge(), user, passwordHash, deviceId,
                        userAgent, ip, proto, fullAuthForceful, optionsReq);
            case INVALID_CREDENTIALS:
                ZimbraLog.account.error("ROPC failed due to invalid credentials for user %s: %s", user,
                        ar.getErrorDescription());
                return Outcome.INVALID;
            case POLICY_DENIED:
                ZimbraLog.account.error("ROPC policy denied for %s: %s", user, ar.getErrorDescription());
                return Outcome.POLICY_DENIED;
            default:
                ZimbraLog.account.error("ROPC error for %s: %s / %s", user, ar.getErrorCode(),
                        ar.getErrorDescription());
                return Outcome.ERROR;
        }
    }

    private static Outcome awaitPush(Account account, IRopcHandler handler, MFAChallenge challenge, String user,
                                     String passwordHash, String deviceId, String userAgent, String ip,
                                     String proto, boolean fullAuthForceful, boolean optionsReq)
            throws ServiceException {
        long pollingTimeout = IRopcUtil.parseToMillis(challenge.get(POLLING_TIMEOUT))
                .map(val -> Math.min(val, POLLING_TIMEOUT_DEFAULT))
                .orElse(POLLING_TIMEOUT_DEFAULT);
        long pollingInterval = IRopcUtil.parseToMillis(challenge.get(INTERVAL)).orElse(POLLING_INTERVAL_DEFAULT);

        MFAPollResult pollResult = POLLER.await(handler, challenge, pollingInterval, pollingTimeout);
        switch (pollResult) {
            case SUCCESS:
                ZimbraLog.account.info("MFA polling : SUCCESS. Push Challenge approved for user %s", user);
                // passing null deviceId, as nativeIOS app or gmail do not share device id on the first OPTIONS request
                // and outlook changes the device ID once the Microsoft cloud server registers the user.
                // device id changes in subsequent calls. Will append device id in subsequent calls
                String resolvedDeviceId = fullAuthForceful || !optionsReq ? deviceId : null;
                persist(account, null, user, userAgent, resolvedDeviceId, ip, handler.getName(),
                        challenge.get(MFAChallenge.REFRESH_TOKEN),
                        challenge.get(MFAChallenge.ID_TOKEN), passwordHash, proto, System.currentTimeMillis(),
                        parse(challenge.get(MFAChallenge.ACCESS_TOKEN_TIMEOUT)));
                return Outcome.SUCCESS;
            case REJECTED:
                ZimbraLog.account.error("MFA polling : REJECTED. Push Challenge denied by user %s", user);
                return Outcome.REJECTED;
            case EXPIRED:
                ZimbraLog.account.error("MFA polling : EXPIRED. PUSH challenge was not approved in " +
                        "defined approval window for user  %s", user);
                return Outcome.MFA_TIMEOUT;
            default:
                ZimbraLog.account.error("MFA polling : ERROR. Encountered unexpected polling status " +
                        "for user  %s", user);
                return Outcome.ERROR;
        }
    }

    private static void persist(Account account, Long id, String user, String userAgent, String deviceId, String ip,
                                String provider, String refreshToken, String idToken, String passwordHash,
                                String protocol, long createdAt, Long accessTokenExpiry) throws ServiceException {
        IRopcSessionRecord.Builder builder = new IRopcSessionRecord.Builder()
                .id(id)
                .username(user)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .ip(ip)
                .provider(provider)
                .idToken(encrypt(user, idToken))
                .passwordHash(passwordHash)
                .protocol(protocol)
                .createdAt(createdAt)
                .lastUpdatedAt(System.currentTimeMillis());
        if (refreshToken != null && !refreshToken.isEmpty()) {
            builder.refreshToken(encrypt(user, refreshToken));
        }

        IRopcSessionRecord rec = builder.build();

        STORE.upsert(account, rec);

        // update the cache
        IRopcCredCache.store(user, passwordHash, userAgent, protocol, provider, ip, deviceId, accessTokenExpiry);
    }

    private static void verifyMandatoryParamsforAuth(String username, String password, Map<String, String> configs,
                                                     String proto)
            throws ServiceException {
        if (username == null || username.trim().isEmpty() || !username.contains("@")) {
            throw ServiceException.FAILURE("Authentication Failed : Missing or invalid username", null);
        }
        if (password == null || password.trim().isEmpty()) {
            throw ServiceException.FAILURE("Authentication Failed : missing or invalid password provided", null);
        }

        if (proto == null || proto.isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'protocol'", null);
        }

        if (configs.get(TOKEN_ENDPOINT) == null || configs.get(TOKEN_ENDPOINT).isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'token_endpoint'", null);
        }

        if (configs.get(REQUEST_PARAM_CLIENT_ID) == null || configs.get(REQUEST_PARAM_CLIENT_ID).isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'client_id'", null);
        }

        if (configs.get(PROVIDER) == null || configs.get(PROVIDER).isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'provider'", null);
        }
    }

    private static String encrypt(String accountId, String plaintext) throws ServiceException {
        return plaintext == null ? null : DataSource.encryptData(accountId, plaintext);
    }

    private static String decrypt(String accountId, String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            return DataSource.decryptData(accountId, ciphertext);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ROPC token decrypt failed for %s; treating as absent", accountId, e);
            return null;
        }
    }

    private static Long parse(String expiry) {
        try {
            long maxExpiry = LC.mfa_idp_max_cred_cache_timeout_in_minutes.intValue() * 60L;
            return expiry == null ? maxExpiry :
                    Math.min(maxExpiry, Long.parseLong(expiry));
        } catch (Exception e) {
            return ACCESS_EXPIRY_DEFAULT;
        }
    }
}
