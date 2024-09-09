/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.ZimbraSoapContext;

public class ChangePassword extends AdminDocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        if (!checkPasswordSecurity(context)) {
            throw ServiceException.INVALID_REQUEST("clear text password is not allowed", null);
        }

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
        if (authTokenEl == null && zsc.getAuthToken() == null) {
            throw ServiceException.INVALID_REQUEST("invalid request parameter", null);
        }

        String namePassedIn = request.getAttribute(AccountConstants.E_ACCOUNT);
        String name = namePassedIn;

        Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
        String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();

        Provisioning prov = Provisioning.getInstance();
        if (virtualHost != null && name.indexOf('@') == -1) {
            Domain d = prov.get(Key.DomainBy.virtualHostname, virtualHost);
            if (d != null) {
                name = name + "@" + d.getName();
            }
        }

        String text = request.getAttribute(AccountConstants.E_DRYRUN, null);
        boolean dryRun = false;
        if (!StringUtil.isNullOrEmpty(text)) {
            if (text.equals("1") || text.equalsIgnoreCase("true")) {
                dryRun = true;
            }
        }

        AuthToken at = zsc.getAuthToken();
        Account acct = prov.get(Key.AccountBy.name, name, at);
        if (acct == null) {
            throw AccountServiceException.AuthFailedServiceException.AUTH_FAILED(name, namePassedIn, "account not found");
        }

        AuthToken.Usage usage = AuthToken.Usage.AUTH;
        if (authTokenEl != null) {
            try {
                at = AuthProvider.getAuthToken(authTokenEl, acct);
            } catch (AuthTokenException e) {
                throw ServiceException.AUTH_REQUIRED();
            }
            if (at == null) {
                throw ServiceException.AUTH_REQUIRED("invalid auth token");
            }
            usage = AuthToken.Usage.RESET_PASSWORD;
        } else if (!canAccessAccount(zsc, acct)) {
            throw ServiceException.PERM_DENIED("cannot access account");
        }

        acct = AuthProvider.validateAuthToken(prov, at, false, usage);
        if (acct == null) {
            throw AccountServiceException.AuthFailedServiceException.AUTH_FAILED(name, namePassedIn, "account not found");
        }
        String oldPassword = request.getAttribute(AccountConstants.E_OLD_PASSWORD);
        String newPassword = request.getAttribute(AccountConstants.E_PASSWORD);

        boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);
        if (mustChange && AuthToken.Usage.RESET_PASSWORD == at.getUsage()) {
            prov.changePassword(acct, oldPassword, newPassword, dryRun);
            try {
                at.deRegister();
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("cannot de-register reset password auth token", e);
            }
        } else if (acct.isIsExternalVirtualAccount() && StringUtil.isNullOrEmpty(oldPassword)
                && !acct.isVirtualAccountInitialPasswordSet() && acct.getId().equals(zsc.getAuthtokenAccountId())) {
            prov.setPassword(acct, newPassword, true);
            acct.setVirtualAccountInitialPasswordSet(true);
        } else {
            prov.changePassword(acct, oldPassword, newPassword, dryRun);
        }

        Element response = zsc.createElement(AdminConstants.CHANGE_PASSWORD_RESPONSE);
        if (!dryRun && Usage.AUTH == at.getUsage()) {
            at = AuthProvider.getAuthToken(acct);
            at.encodeAuthResp(response, true);
            response.addAttribute(AccountConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
        }

        return response;
    }

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}
