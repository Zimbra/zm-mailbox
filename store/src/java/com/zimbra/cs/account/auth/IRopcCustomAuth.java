/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.auth;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.ropc.IRopcAuthEngine;
import com.zimbra.cs.account.auth.ropc.Outcome;
import java.util.List;
import java.util.Map;

public class IRopcCustomAuth extends ZimbraCustomAuth {

    private static final String EXTENSION_NAME = "idp-ropc";

    @Override
    public void authenticate(Account acct, String password,
                             Map<String, Object> context, List<String> args) throws Exception {

        String user = acct.getName();
        Protocol proto = (Protocol) context.get(AuthContext.AC_PROTOCOL);
        String deviceId = (String) context.get(AuthContext.AC_DEVICE_ID);

        // protocol-aware caching — SKIP for zsync.
        boolean useCache = (proto != Protocol.zsync);
        if (useCache && checkInCache(user, password)) {
            return;
        }

        Outcome outcome = callAuthEngine(user, password, deviceId);

        switch (outcome) {
            case SUCCESS:
                break;
            case INVALID:
            case POLICY_DENIED:
                throw AuthFailedServiceException.AUTH_FAILED(user,
                        "Authentication failed : Invalid credentials provided");
            case MFA_TIMEOUT:
                throw AuthFailedServiceException.AUTH_FAILED(user, "MFA request timed out. Please try again");
            case ERROR:
            default:
                throw ServiceException.FAILURE("Authentication service temporarily unavailable.", null);
        }

        if (useCache) {
            // TODO : logic to store password in cache
        }
    }

    protected Outcome callAuthEngine(String user, String password, String deviceId) {
        return IRopcAuthEngine.authenticate(user, password, deviceId);
    }

    protected boolean checkInCache(String user, String password) {
        return false;
    }

    @Override
    public boolean checkPasswordAging() {
        return false;
    }
}