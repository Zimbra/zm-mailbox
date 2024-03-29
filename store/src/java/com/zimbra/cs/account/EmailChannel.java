/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
 * (C) 2018, 2023 Synacor, Inc.
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ZAttrProvisioning.PrefPasswordRecoveryAddressStatus;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class EmailChannel extends ChannelProvider {
    /*
     * ##############RecoverAccount API methods - START #####################
     */
    @Override
    public String getRecoveryAccount(Account account) throws ServiceException {
        return account.getPrefPasswordRecoveryAddress();
    }

    @Override
    public void sendAndStoreResetPasswordRecoveryCode(ZimbraSoapContext zsc, Account account,
            Map<String, String> recoveryCodeMap) throws ServiceException {
        ZoneId zoneId = ZoneId.of(Util.getAccountTimeZone(account).getID());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z")
                .withZone(zoneId);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Locale locale = account.getLocale();
        String displayName = account.getDisplayName();
        if (displayName == null) {
            displayName = account.getName();
        }
        Long expiryLong = Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString()));
        ZonedDateTime mailDate = Instant.ofEpochMilli(expiryLong).atZone(zoneId);
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
            ZimbraLog.passwordreset.debug("RecoverAccount: Error occured while sending recovery code in email to %s",
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString()));
            throw ServiceException.FAILURE("Error occured while sending recovery code in email to "
                    + recoveryCodeMap.get(CodeConstants.EMAIL.toString()), me);
        }
        ZimbraLog.passwordreset.debug("RecoverAccount: Recovery code sent in email to %s",
                StringUtil.maskEmail(recoveryCodeMap.get(CodeConstants.EMAIL.toString())));
        // store the same in ldap attribute for user
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraResetPasswordRecoveryCode, JWEUtil.getJWE(recoveryCodeMap));
        Provisioning.getInstance().modifyAttrs(account, prefs, true, null);
    }

    /*
     * ##########SetRecoveryAccount API methods - START ###############
     */
    @Override
    public void validateSetRecoveryAccountCode(String recoveryAccountVerificationCode, Account account, Mailbox mbox,
            ZimbraSoapContext zsc) throws ServiceException {
        Map<String, String> recoveryDataMap = getSetRecoveryAccountCodeMap(account);
        if (recoveryDataMap == null || recoveryDataMap.isEmpty()) {
            throw ForgetPasswordException.CODE_NOT_FOUND("Verification code for recovery email address not found on server.");
        }
        String code = recoveryDataMap.get(CodeConstants.CODE.toString());
        long expiryTime = Long.parseLong(recoveryDataMap.get(CodeConstants.EXPIRY_TIME.toString()));
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone(Util.getAccountTimeZone(account).getID()));
            String dateTime = format.format(expiryTime);
            ZimbraLog.passwordreset.debug("validateCode: expiryTime for code: %s", dateTime);
            ZimbraLog.passwordreset.debug("ValidateCode: last 3 characters of recovery code: %s", code.substring(5));
        }

        if (code.equals(recoveryAccountVerificationCode)) {
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                throw ForgetPasswordException.CODE_EXPIRED("The recovery email address verification code is expired.");
            }
            HashMap<String, Object> prefs = new HashMap<String, Object>();
            prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
                    PrefPasswordRecoveryAddressStatus.verified);
            prefs.put(Provisioning.A_zimbraRecoveryAccountVerificationData, null);
            Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());
        } else {
            throw ForgetPasswordException.CODE_MISMATCH("Verification of recovery email address verification code failed.");
        }
    }

    @Override
    public void sendAndStoreSetRecoveryAccountCode(Account account, Mailbox mbox, Map<String, String> recoveryCodeMap,
            ZimbraSoapContext zsc, OperationContext octxt, HashMap<String, Object> prefs) throws ServiceException {
        Locale locale = account.getLocale();
        String ownerAcctDisplayName = account.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = account.getName();
        }
        String subject = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailSubject, locale, ownerAcctDisplayName);
        String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        try {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone(Util.getAccountTimeZone(account).getID()));
            String dateTime = format.format(Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString())));
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug(
                        "sendRecoveryEmailVerificationCode: Expiry of recovery address verification code sent to %s: %s",
                        recoveryCodeMap.get(CodeConstants.EMAIL.toString()), dateTime);
                ZimbraLog.passwordreset.debug(
                        "sendRecoveryEmailVerificationCode: Last 3 characters of recovery code sent to %s: %s",
                        recoveryCodeMap.get(CodeConstants.EMAIL.toString()),
                        recoveryCodeMap.get(CodeConstants.CODE.toString()).substring(5));
            }
            String mimePartText = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailBodyText, locale,
                    recoveryCodeMap.get(CodeConstants.CODE.toString()), dateTime);
            String mimePartHtml = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailBodyHtml, locale,
                    recoveryCodeMap.get(CodeConstants.CODE.toString()), dateTime);
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(account, account, subject, charset, null, null,
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString()), mmp);
            mbox.getMailSender().sendMimeMessage(octxt, mbox, false, mm, null, null, null, null, false);
        } catch (MessagingException e) {
            ZimbraLog.passwordreset.warn("Failed to send verification code to email ID: '"
                    + recoveryCodeMap.get(CodeConstants.EMAIL.toString()) + "'", e);
            throw ServiceException.FAILURE("Failed to send verification code to email ID: "
                    + recoveryCodeMap.get(CodeConstants.EMAIL.toString()), e);
        }
        // store the recovery code
        if (prefs == null) {
            prefs = new HashMap<String, Object>();
        }
        String verificationDataStr = JWEUtil.getJWE(recoveryCodeMap);
        prefs.put(Provisioning.A_zimbraRecoveryAccountVerificationData, verificationDataStr);
        Provisioning.getInstance().modifyAttrs(account, prefs, true, zsc.getAuthToken());
        account.unsetResetPasswordRecoveryCode();
    }

    public static void sendAndStoreResetPasswordURL(ZimbraSoapContext zsc, Account account,
            Map<String, String> recoveryCodeMap) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Locale locale = account.getLocale();
        String accountName = account.getName();
        String userDisplayName = account.getDisplayName() != null ? String.join("", " ", account.getDisplayName()) : "";
        String subject = L10nUtil.getMessage(MsgKey.sendPasswordResetEmailSubject, locale, userDisplayName);
        String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        ZoneId zoneId = ZoneId.of(Util.getAccountTimeZone(account).getID());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z").withZone(zoneId);
        try {
            Long expiryTimeLong = Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString()));
            ZonedDateTime expiryDate = Instant.ofEpochMilli(expiryTimeLong).atZone(zoneId);
            // add a code to the URL too which can be verified later on
            recoveryCodeMap.put(CodeConstants.ACCOUNT_ID.toString(), account.getId());

            String url = AccountUtil.generateResetPasswordURL(account, expiryTimeLong, recoveryCodeMap.get(CodeConstants.CODE.toString()));

            ZimbraLog.account.debug("Expiry of Password Reset link sent to %s is %s", recoveryCodeMap.get(CodeConstants.EMAIL.toString()), expiryDate.format(formatter));
            ZimbraLog.account.debug("Password Reset verification URL sent to %s is %s", recoveryCodeMap.get(CodeConstants.EMAIL.toString()), url);
            String mimePartText = L10nUtil.getMessage(MsgKey.sendPasswordResetEmailBodyText, locale, userDisplayName,
                    accountName, url, expiryDate.format(formatter));
            String mimePartHtml = L10nUtil.getMessage(MsgKey.sendPasswordResetEmailBodyHtml, locale, userDisplayName,
                    accountName, url, expiryDate.format(formatter));
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(account, account, subject, charset, null, null,
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString()), mmp);
            mbox.getMailSender().sendMimeMessage(null, mbox, false, mm, null, null, null, null, false);

            HashMap<String, Object> prefs = new HashMap<String, Object>();
            prefs.put(Provisioning.A_zimbraResetPasswordRecoveryCode, JWEUtil.getJWE(recoveryCodeMap));
            Provisioning.getInstance().modifyAttrs(account, prefs, true, null);
        } catch (MessagingException e) {
            ZimbraLog.account.warn(String.format("Failed to send verification link to email ID: %s'",
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString())), e);
            throw ServiceException.FAILURE(String.format("Failed to send verification link to email ID: ",
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString())), e);
        }
    }

    @Override
    public void sendAndStoreTwoFactorAuthAccountCode(Account account, Mailbox mbox, Map<String, String> recoveryCodeMap,
            ZimbraSoapContext zsc, OperationContext octxt, HashMap<String, Object> prefs) throws ServiceException {
        Locale locale = account.getLocale();
        String ownerAcctDisplayName = account.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = account.getName();
        }
        String subject = L10nUtil.getMessage(MsgKey.twoFactorAuthEmailSubject, locale, ownerAcctDisplayName);
        String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        try {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone(Util.getAccountTimeZone(account).getID()));
            String dateTime = format.format(Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString())));
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug(
                        "sendTwoFactorAuthEmailVerificationCode: Expiry of two-factor auth email address verification code sent to %s: %s",
                        recoveryCodeMap.get(CodeConstants.EMAIL.toString()), dateTime);
                ZimbraLog.passwordreset.debug(
                        "sendTwoFactorAuthEmailVerificationCode: Last 3 characters of two-factor auth email verification code sent to %s: %s",
                        recoveryCodeMap.get(CodeConstants.EMAIL.toString()),
                        recoveryCodeMap.get(CodeConstants.CODE.toString()).substring(5));
            }
            String mimePartText = L10nUtil.getMessage(MsgKey.twoFactorAuthEmailBodyText, locale,
                    recoveryCodeMap.get(CodeConstants.CODE.toString()), dateTime);
            String mimePartHtml = L10nUtil.getMessage(MsgKey.twoFactorAuthEmailBodyHtml, locale,
                    recoveryCodeMap.get(CodeConstants.CODE.toString()), dateTime);
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(account, account, subject, charset, null, null,
                    recoveryCodeMap.get(CodeConstants.EMAIL.toString()), mmp);
            mbox.getMailSender().sendMimeMessage(octxt, mbox, false, mm, null, null, null, null, false);
        } catch (MessagingException e) {
            ZimbraLog.passwordreset.warn("Failed to send verification code to email ID: '"
                    + recoveryCodeMap.get(CodeConstants.EMAIL.toString()) + "'", e);
            throw ServiceException.FAILURE("Failed to send verification code to email ID: "
                    + recoveryCodeMap.get(CodeConstants.EMAIL.toString()), e);
        }
        // store the recovery code
        if (prefs == null) {
            prefs = new HashMap<String, Object>();
        }
        String verificationDataStr = JWEUtil.getJWE(recoveryCodeMap);
        prefs.put(Provisioning.A_zimbraRecoveryAccountVerificationData, verificationDataStr);
        Provisioning.getInstance().modifyAttrs(account, prefs, true, zsc.getAuthToken());
        account.unsetResetPasswordRecoveryCode();
    }
}
