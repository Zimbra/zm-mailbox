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

package com.zimbra.cs.account;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.util.AccountUtil;

public class EmailRecoveryCode implements SendRecoveryCode {
    public static final String LOG_OPERATION = "RecoverAccount:";
    private Map<String, String> recoveryCodeMap;
    private Mailbox mbox;
    private Account account;

    @SuppressWarnings("unused")
    private EmailRecoveryCode() {
        // private default constructor
    }
    /**
     * @param recoveryCodeMap
     */
    public EmailRecoveryCode(Map<String, String> recoveryCodeMap, Mailbox mbox, Account account) {
        this.recoveryCodeMap = recoveryCodeMap;
        this.mbox = mbox;
        this.account = account;
    }

    @Override
    public void sendForgetPasswordCode() throws ServiceException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z")
                .withZone(ZoneId.of("GMT"));
        Locale locale = account.getLocale();
        String displayName = account.getDisplayName();
        if (displayName == null) {
            displayName = account.getName();
        }
        Long expiryLong = Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString()));
        ZonedDateTime mailDate = Instant.ofEpochMilli(expiryLong).atZone(ZoneId.of("GMT"));
        String subject = L10nUtil.getMessage(MsgKey.sendPasswordRecoveryEmailSubject, locale, account.getDomainName());
        String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        String mimePartText = L10nUtil.getMessage(MsgKey.sendPasswordRecoveryEmailBodyText, locale, displayName,
                recoveryCodeMap.get(CodeConstants.CODE.toString()), mailDate.format(formatter));
        String mimePartHtml = L10nUtil.getMessage(MsgKey.sendPasswordRecoveryEmailBodyHtml, locale, displayName,
                recoveryCodeMap.get(CodeConstants.CODE.toString()), mailDate.format(formatter));
        try {
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(account, account, subject, charset, null, null,
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString()), mmp);
            mbox.getMailSender().sendMimeMessage(null, mbox, false, mm, null, null, null, null, false);
        } catch (MessagingException me) {
            ZimbraLog.account.debug("%s Error occured while sending recovery code in email to ", LOG_OPERATION,
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString()));
            throw ServiceException.FAILURE("Error occured while sending recovery code in email to "
                    + recoveryCodeMap.get(CodeConstants.EMAIL.toString()), me);
        }
        ZimbraLog.account.debug("%s Recovery code sent in email to %s", LOG_OPERATION,
                StringUtil.maskEmail(recoveryCodeMap.get(CodeConstants.EMAIL.toString())));
    }

    @Override
    public void sendRecoveryAccountValidationCode(Account ownerAccount, OperationContext octxt) throws ServiceException {
        String emailIdToVerify = recoveryCodeMap.get(CodeConstants.EMAIL.toString());
        String code = recoveryCodeMap.get(CodeConstants.CODE.toString());
        String expiryTime = recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString());
        Locale locale = account.getLocale();
        String ownerAcctDisplayName = ownerAccount.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = ownerAccount.getName();
        }
        String subject = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailSubject, locale,
            ownerAcctDisplayName);
        String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset,
            MimeConstants.P_CHARSET_UTF8);
        try {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date expiryDate = new Date(Long.valueOf(expiryTime));
            String gmtDate = format.format(expiryDate);
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug(
                    "sendRecoveryEmailVerificationCode: Expiry of recovery address verification code sent to %s: %s",
                    emailIdToVerify, gmtDate);
                ZimbraLog.passwordreset.debug(
                    "sendRecoveryEmailVerificationCode: Last 3 characters of recovery code sent to %s: %s",
                    emailIdToVerify, code.substring(5));
            }
            String mimePartText = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailBodyText, locale, code,
                gmtDate);
            String mimePartHtml = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailBodyHtml, locale, code,
                gmtDate);
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(account, ownerAccount, subject,
                charset, null, null, emailIdToVerify, mmp);
            mbox.getMailSender().sendMimeMessage(octxt, mbox, false, mm, null, null, null, null,
                false);
        } catch (MessagingException e) {
            ZimbraLog.account
                .warn("Failed to send verification code to email ID: '" + emailIdToVerify + "'", e);
            throw ServiceException
                .FAILURE("Failed to send verification code to email ID: " + emailIdToVerify, e);
        }
    }

}
