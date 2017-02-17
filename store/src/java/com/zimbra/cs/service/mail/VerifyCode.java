/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Map;

/**
 * Handler for verifying the device code.
 */
public class VerifyCode extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String emailAddr = request.getAttribute(MailConstants.A_ADDRESS);
        boolean success = false;
        if (SendVerificationCode.emailToCodeMap.containsKey(emailAddr)) {
            String code = request.getAttribute(MailConstants.A_VERIFICATION_CODE);
            if (SendVerificationCode.emailToCodeMap.get(emailAddr).equals(code)) {
                Account account = getRequestedAccount(zsc);
                account.setCalendarReminderDeviceEmail(emailAddr);
                success = true;
                SendVerificationCode.emailToCodeMap.remove(emailAddr);
            } else {
                ZimbraLog.misc.debug("Invalid verification code");
            }
        } else {
            ZimbraLog.misc.debug("Verification code for %s has either not been generated or has expired", emailAddr);
        }
        return zsc.createElement(MailConstants.VERIFY_CODE_RESPONSE).addAttribute(MailConstants.A_VERIFICATION_SUCCESS, success ? "1" : "0");
    }
}
