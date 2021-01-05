/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ResetPasswordUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.ResetPasswordRequest;

public class ResetPassword extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        if (!checkPasswordSecurity(context)) {
            throw ServiceException.INVALID_REQUEST("clear text password is not allowed", null);
        }
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ResetPasswordRequest req = JaxbUtil.elementToJaxb(request);
        req.validateResetPasswordRequest();

        AuthToken at = zsc.getAuthToken();
        if(at.getUsage() == Usage.RESET_PASSWORD) {
            AuthProvider.validateAuthToken(prov, at, false, Usage.RESET_PASSWORD);
        } else {
            AuthProvider.validateAuthToken(prov, at, false);
        }

        boolean dryRun = request.getAttributeBool(AccountConstants.E_DRYRUN, false);

        Account acct = at.getAccount();
        boolean locked = acct.getBooleanAttr(Provisioning.A_zimbraPasswordLocked, false);
        if (locked) {
            throw AccountServiceException.PASSWORD_LOCKED();
        }
        ResetPasswordUtil.validateFeatureResetPasswordStatus(acct);
        String newPassword = req.getPassword();
        checkPasswordStrength(prov, acct, newPassword);

        // proxy if required
        if (!Provisioning.onLocalServer(acct)) {
            try {
                return proxyRequest(request, context, acct.getId());
            } catch (ServiceException e) {
                // if something went wrong proxying the request, just execute it locally
                if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                    ZimbraLog.account.warn("encountered proxy error", e);
                } else {
                    // but if it's a real error, it's a real error
                    throw e;
                }
            }
        }

        setPasswordAndPurgeAuthTokens(prov, acct, newPassword, dryRun);

        Element response = zsc.createElement(AccountConstants.E_RESET_PASSWORD_RESPONSE);
        acct.unsetResetPasswordRecoveryCode();
        return response;
    }

    protected void setPasswordAndPurgeAuthTokens(Provisioning prov, Account acct, 
            String newPassword, boolean dryRun) throws ServiceException {
        // set new password
        if (dryRun) {
            prov.resetPassword(acct, newPassword, dryRun);
        } else {
            prov.setPassword(acct, newPassword);
        }
        // purge old auth tokens to invalidate existing sessions
        acct.purgeAuthTokens();
    }

    protected void checkPasswordStrength(Provisioning prov, Account acct, String newPassword) throws ServiceException {
        prov.checkPasswordStrength(acct, newPassword);
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        AuthToken at = zsc.getAuthToken();
        if(at.getUsage() == Usage.RESET_PASSWORD) {
            return false;
        }
        return true;
    }
}
