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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.ropc.IRopcAuthEngine;
import com.zimbra.cs.account.auth.ropc.IRopcConstants;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.cs.account.auth.ropc.Outcome;
import com.zimbra.cs.account.auth.ropc.util.IRopcUtil;
import java.util.List;
import java.util.Map;

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
        String deviceId = (String) context.get(AuthContext.AC_DEVICE_ID);
        String userAgent = (String) context.get(AuthContext.AC_USER_AGENT);
        String ipAddress = (String) context.get(AuthContext.AC_ORIGINATING_CLIENT_IP);
        Map<String, String> configs = IRopcUtil.extractConfigsFromArgs(args);
        //Extract configs to get the 'provider' for the composite cache key
        String provider = configs.get(IRopcConstants.PROVIDER);
        String protocolStr = (proto != null) ? proto.name() : null;

        ZimbraLog.account.info("IRopcCustomAuth: auth request for %s "
                        + "[protocol=%s, ua=%s, deviceId=%s, ip=%s, provider=%s]",
                user, protocolStr, userAgent, deviceId, ipAddress, provider);

        // Implementation of the cache check using composite keys
        if (checkInCache(user, password, userAgent, protocolStr, provider, ipAddress, deviceId)) {
            ZimbraLog.account.info("IRopcCustomAuth: cache hit for %s [protocol=%s]", user, protocolStr);
            return;
        }
        IRopcCredCache.checkRejectionLimit(user, userAgent, protocolStr, provider, ipAddress, deviceId);

        Outcome outcome = callAuthEngine(user, password, deviceId, proto, userAgent, ipAddress, configs);

        switch (outcome) {
            case SUCCESS:
                break;
            case REJECTED:
                IRopcCredCache.storeRejection(user, userAgent, protocolStr, provider, ipAddress, deviceId);
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Challenge denied by user");
            case INVALID:
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Invalid credentials provided");
            case POLICY_DENIED:
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Policy Denied");
            case MFA_TIMEOUT:
                throw AuthFailedServiceException.AUTH_FAILED(user, "MFA request timed out. Please try again");
            case ERROR:
                throw AuthFailedServiceException.AUTH_FAILED(user, "Error while authentication");
            default:
                throw ServiceException.FAILURE("Authentication service temporarily unavailable.", null);
        }
        long expires_in = 3600000L;

        IRopcCredCache.store(user, password, userAgent, protocolStr, provider, ipAddress, deviceId, expires_in);
        ZimbraLog.account.info("IRopcCustomAuth: IdP auth successful for %s [protocol=%s]", user, protocolStr);

    }

    protected Outcome callAuthEngine(String userName, String password, String deviceId, Protocol protocol,
                                     String userAgent, String ip, Map<String, String> configs) {
        return IRopcAuthEngine.authenticate(userName, password, deviceId, protocol, userAgent, ip, configs);
    }

    protected boolean checkInCache(String user, String password, String ua, String proto,
            String prov, String ip, String did) {
        return IRopcCredCache.isValid(user, password, ua, proto, prov, ip, did);
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

    public static boolean shouldSkipEasPassCache(Account account) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Domain domain = prov.getDomain(account);
            if (domain != null) {
                String authMech = domain.getAuthMech();
                if (authMech != null && authMech.contains(EXTENSION_NAME)) {
                    return true;
                }
            }
        } catch (Exception e) {
            ZimbraLog.account.warn("IRopcCustomAuth: failed to check authMech for %s", account.getName(), e);
        }
        return false;
    }
}
