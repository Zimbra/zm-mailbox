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

package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EmailRecoveryCode;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SendRecoveryCode;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest.Op;
import com.zimbra.soap.mail.message.SetRecoveryEmailResponse;
import com.zimbra.soap.type.Channel;

public class SetRecoveryEmail extends DocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        SetRecoveryEmailRequest req = zsc.elementToJaxb(request);
        if (!mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraFeatureResetPasswordEnabled,
            false)) {
            throw ServiceException.PERM_DENIED("password reset feature not enabled.");
        }
        Channel channel = req.getChannel();
        if (channel == null) {
            throw ServiceException.INVALID_REQUEST("Invalid channel received.", null);
        }
        Op op = req.getOp();
        if (op == null) {
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        SetRecoveryEmailResponse resp = null;
        SendRecoveryCode sendRecoveryCode = null;
        switch (channel) {
            case EMAIL:
                sendRecoveryCode = new EmailRecoveryCode(null, mbox, account);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid channel received.", null);
        }
        resp = sendRecoveryCode.handleSetRecoveryEmailRequest(req, zsc, octxt);
        return zsc.jaxbToElement(resp);
    }
}
