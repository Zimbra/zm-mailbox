/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Handler for sending a verification code to a device.
 */
public class SendVerificationCode extends MailDocumentHandler {

    static Map<String, String> emailToCodeMap = Collections.synchronizedMap(new LruMap<String, String>(1000));

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        // email address corresponding to device
        String emailAddr = request.getAttribute(MailConstants.A_ADDRESS);
        String code = generateVerificationCode();
        try {
            sendVerificationCode(emailAddr,"ZIMBRA_NIC_VERIFICATION_CODE_"+code, getRequestedMailbox(zsc));
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Error in sending verification code to device", e);
        }
        emailToCodeMap.put(emailAddr, code);
        return zsc.createElement(MailConstants.SEND_VERIFICATION_CODE_RESPONSE);
    }

    static void sendVerificationCode(String emailAddr, String code, Mailbox mbox) throws MessagingException, ServiceException {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSmtpSession(mbox.getAccount()));
        mm.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(emailAddr));
        mm.setText(L10nUtil.getMessage(L10nUtil.MsgKey.deviceSendVerificationCodeText,
                                       mbox.getAccount().getLocale(), code),
                   MimeConstants.P_CHARSET_UTF8);
        mm.saveChanges();
        MailSender mailSender = mbox.getMailSender();
        mailSender.setSaveToSent(false);
        mailSender.sendMimeMessage(null, mbox, mm);
    }

    static String generateVerificationCode() {
        ZimbraLog.misc.debug("Generating verification code");
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[3];
        random.nextBytes(bytes);
        return new String(Hex.encodeHex(bytes));
    }
}
