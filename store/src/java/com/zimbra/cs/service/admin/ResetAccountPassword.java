/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ChannelProvider;
import com.zimbra.cs.account.EmailChannel;
import com.zimbra.cs.account.ForgetPasswordException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.mail.RecoverAccount;
import com.zimbra.cs.service.util.ResetPasswordUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ResetAccountPasswordResponse;
import com.zimbra.soap.type.Channel;

public class ResetAccountPassword extends AdminDocumentHandler {
    protected ZimbraSoapContext zsc = null;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Element a = request.getElement(AccountConstants.E_ACCOUNT);
        String key = a.getAttribute(AccountConstants.A_BY);
        String value = a.getText();

        if (Strings.isNullOrEmpty(value)) {
            throw ServiceException.INVALID_REQUEST(
                "no text specified for the " + AccountConstants.E_ACCOUNT + " element", null);
        }
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());

        // prevent directory harvest attack, mask no such account as permission denied
        if (account == null)
            throw ServiceException.PERM_DENIED("can not access account");

        checkAccountRights(zsc, account);

        ResetPasswordUtil.validateFeatureResetPasswordStatus(account);
        ResetPasswordUtil.checkValidRecoveryAccount(account);
        String recoveryAccount = account.getPrefPasswordRecoveryAddress();
        EmailChannel.sendResetPasswordURL(zsc, octxt, account
                , RecoverAccount.fetchAndFormRecoveryCodeParams(account, account.getPasswordRecoveryMaxAttempts(), recoveryAccount, zsc, true));

        ResetAccountPasswordResponse response = new ResetAccountPasswordResponse();
        return zsc.jaxbToElement(response);
    }

    /*
     * returns whether password strength policies should be enforced for the authed user
     *
     * returns false if user can setAccountPassword
     * returns true if user cannot setAccountPassword but can changeAccountPassword
     *
     * throws PERM_DENIED if user doesn't have either right
     */
    private boolean checkAccountRights(ZimbraSoapContext zsc, Account acct)
            throws ServiceException {
        try {
            checkAccountRight(zsc, acct, Admin.R_setAccountPassword);
            return false;
        } catch (ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                checkAccountRight(zsc, acct, Admin.R_changeAccountPassword);
                return true;
            } else {
                throw e;
            }
        }
    }
}
