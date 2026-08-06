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

package com.zimbra.cs.account.auth;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.AuthContext.SubProtocol;
import com.zimbra.cs.account.auth.ropc.CacheResponse;
import com.zimbra.cs.account.auth.ropc.IRopcAuthEngine;
import com.zimbra.cs.account.auth.ropc.IRopcConstants;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.cs.account.auth.ropc.Outcome;
import com.zimbra.cs.account.auth.ropc.util.IRopcUtil;
import java.util.List;
import java.util.Map;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.AUTH_REQUEST_TYPE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.FULL_AUTH;

public class IRopcCustomAuth extends ZimbraCustomAuth {

    private static final String EXTENSION_NAME = "idp-ropc";

    @Override
    public void authenticate(Account acct, String password,
                             Map<String, Object> context, List<String> args) throws Exception {

        if (acct == null) {
            ZimbraLog.account.error("Authentication failed : Account is null");
            throw ServiceException.FAILURE("Authentication failed : Account is null.", null);
        }
        String user = acct.getName();
        Protocol proto = (Protocol) context.get(AuthContext.AC_PROTOCOL);
        SubProtocol subProtocol = (SubProtocol) context.get(AuthContext.AC_SUB_PROTOCOL);
        String deviceId = (String) context.get(AuthContext.AC_DEVICE_ID);
        String userAgent = (String) context.get(AuthContext.AC_USER_AGENT);
        String ipAddress = (String) context.get(AuthContext.AC_ORIGINATING_CLIENT_IP);
        boolean optionsReq = AUTH_REQUEST_TYPE.equalsIgnoreCase(String.valueOf
                (context.get(AuthContext.AC_AUTH_REQUEST)));
        Map<String, String> configs = IRopcUtil.extractConfigsFromArgs(args);
        //extract configs to get the 'provider' for the composite cache key
        String provider = configs.get(IRopcConstants.PROVIDER);
        String protocolStr = subProtocol == null ? proto.name() : subProtocol.name();

        ZimbraLog.account.debug("IRopcCustomAuth: auth request for %s "
                        + "[protocol=%s, ua=%s, deviceId=%s, ip=%s, provider=%s]",
                user, protocolStr, userAgent, deviceId, ipAddress, provider);

        CacheResponse cacheResponse = null;
        try {
            cacheResponse = checkInCache(user, password, userAgent, protocolStr, provider,
                    ipAddress, deviceId, acct, optionsReq);

            if (cacheResponse == null) {
                cacheResponse = new CacheResponse(false);
            }

        } catch (Exception e) {
            ZimbraLog.account.error("Error while caching operation for %s ", user, e);
            cacheResponse = new CacheResponse(false);
        }

        if (cacheResponse.isCacheHit()) {
            ZimbraLog.account.debug("IRopcCustomAuth: cache hit for %s [protocol=%s]", user, protocolStr);
            return;
        }

        if (!cacheResponse.getRejectionSkip()) {
            IRopcCredCache.checkRejectionLimit(user);
        }

        Outcome outcome = callAuthEngine(acct, user, password, deviceId, proto, subProtocol,
                userAgent, ipAddress, configs, FULL_AUTH.equals(cacheResponse.getAuthType()));


        switch (outcome) {
            case SUCCESS:
                break;
            case REJECTED:
                IRopcCredCache.storeRejection(user);
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Challenge denied by user");
            case INVALID:
                IRopcCredCache.storeRejection(user);
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Invalid credentials provided");
            case POLICY_DENIED:
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Policy Denied");
            case TOKEN_EXPIRED:
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Token Expired");
            case MFA_TIMEOUT:
                IRopcCredCache.storeRejection(user);
                throw AuthFailedServiceException.AUTH_FAILED(user, "MFA request timed out. Please try again");
            case ERROR:
                throw ServiceException.TEMPORARILY_UNAVAILABLE();
            default:
                throw ServiceException.TEMPORARILY_UNAVAILABLE();
        }
    }

    protected Outcome callAuthEngine(Account account, String userName, String password, String deviceId,
                                     Protocol protocol, AuthContext.SubProtocol subProtocol,
                                     String userAgent, String ip, Map<String, String> configs,
                                     boolean fullAuthRequired) {
        return IRopcAuthEngine.authenticate(account, userName, password, deviceId, protocol, subProtocol,
                userAgent, ip, configs, fullAuthRequired);
    }

    protected CacheResponse checkInCache(String user, String password, String ua, String proto,
                                         String prov, String ip, String did, Account account, boolean optionsReq)
            throws ServiceException {
        return IRopcCredCache.isValid(user, password, ua, proto, prov, ip, did, account, optionsReq);
    }

    @Override
    public boolean checkPasswordAging() {
        return false;
    }

    public static boolean isSupported(Map<String, Object> context) {
        try {
            Protocol protocol = (Protocol) context.get(AuthContext.AC_PROTOCOL);

            if (protocol == null) {
                return false;
            }

            switch (protocol) {
                case zsync:
                    return context.get(AuthContext.AC_SUB_PROTOCOL) == AuthContext.SubProtocol.eas;
                default:
                    return false;
            }
        } catch (Exception e) {
            ZimbraLog.account.warn("Failure while checking the protocol isSupported.", e);
            return false;
        }
    }

    public String getName() {
        return EXTENSION_NAME;
    }
}
