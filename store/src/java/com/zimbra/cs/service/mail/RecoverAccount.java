/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
 * (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EmailRecoveryCode;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SendRecoveryCode;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecoverAccountRequest;
import com.zimbra.soap.mail.message.RecoverAccountResponse;
import com.zimbra.soap.type.Channel;

public final class RecoverAccount extends MailDocumentHandler {
    public static final String LOG_OPERATION = "RecoverAccount:";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        RecoverAccountRequest req = zsc.elementToJaxb(request);
        req.validateRecoverAccountRequest();
        String email = req.getEmail();
        Account user = Provisioning.getInstance().getAccountByName(email);
        Channel channel = req.getChannel();
        if (user == null) {
            ZimbraLog.account.debug("%s Account not found for %s", LOG_OPERATION, email);
            throw ServiceException.FAILURE("Something went wrong. Please contact your administrator.", null);
        }
        if (!user.isFeatureResetPasswordEnabled()) {
            throw ServiceException.PERM_DENIED("Reset password feature is disabled");
        }
        if (user.getPrefPasswordRecoveryAddressStatus() == null
                || !user.getPrefPasswordRecoveryAddressStatus().isVerified()) {
            ZimbraLog.account.debug("%s Verified recovery email is not found for %s", LOG_OPERATION, email);
            throw ServiceException.FAILURE("Something went wrong. Please contact your administrator.", null);
        }

        RecoverAccountResponse resp = null;
        SendRecoveryCode sendRecoveryCode = null;
        switch (channel) {
            case EMAIL:
                sendRecoveryCode = new EmailRecoveryCode();
                resp = sendRecoveryCode.handleRecoverAccountRequest(req, user);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid channel received", null);
        }
        return zsc.jaxbToElement(resp);
    }

    // no auth required for this request, so return false for both needsAuth and needsAdminAuth
    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

}
