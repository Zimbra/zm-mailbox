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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.RandomStringUtils;

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
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecoverAccountRequest;
import com.zimbra.soap.mail.message.RecoverAccountResponse;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest.Op;
import com.zimbra.soap.mail.message.SetRecoveryEmailResponse;
import com.zimbra.soap.mail.type.PasswordResetOperation;

public class EmailRecoveryCode implements SendRecoveryCode {
    public static final String LOG_OPERATION = "RecoverAccount:";
    private static final int CODE_LENGTH = 8;
    private Map<String, String> recoveryCodeMap;
    private Account account;
    private Mailbox mbox;

    public EmailRecoveryCode() {
        recoveryCodeMap = new HashMap<String, String>();
    }

    public EmailRecoveryCode(Map<String, String> recoveryCodeMap, Mailbox mbox, Account account) {
        if (recoveryCodeMap ==  null) {
            recoveryCodeMap = new HashMap<String, String>();
        }
        this.recoveryCodeMap = recoveryCodeMap;
        this.mbox = mbox;
        this.account = account;
    }

    @Override
    public RecoverAccountResponse handleRecoverAccountRequest(RecoverAccountRequest request, Account account)
            throws ServiceException {
        RecoverAccountResponse resp = new RecoverAccountResponse();
        this.account = account;
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        PasswordResetOperation op = request.getOp();
        String email = request.getEmail();
        String recoveryEmail = account.getPrefPasswordRecoveryAddress();
        if (StringUtil.isNullOrEmpty(recoveryEmail)) {
            ZimbraLog.account.debug("%s Recovery email missing or unverified for %s", LOG_OPERATION, email);
            throw ServiceException.FAILURE("Something went wrong. Please contact your administrator.", null);
        }
        switch (op) {
            case GET_RECOVERY_ACCOUNT:
                recoveryEmail = StringUtil.maskEmail(recoveryEmail);
                ZimbraLog.account.debug("%s Recovery email: %s", LOG_OPERATION, recoveryEmail);
                resp.setRecoveryAccount(recoveryEmail);
                break;
            case SEND_RECOVERY_CODE:
                String storedCodeString = account.getResetPasswordRecoveryCode();
                ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.systemDefault());
                if (!StringUtil.isNullOrEmpty(storedCodeString)) {
                    ZimbraLog.account.debug("%s Recovery code found for %s", LOG_OPERATION, email);
                    recoveryCodeMap = JWEUtil.getDecodedJWE(storedCodeString);
                    ZonedDateTime storedDate = Instant
                            .ofEpochMilli(Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString())))
                            .atZone(ZoneId.systemDefault());
                    if (ChronoUnit.MILLIS.between(currentDate, storedDate) <= 0L) {
                        ZimbraLog.account.debug(
                                "%s Recovery code expired, so generating new one and reseting resend count.",
                                LOG_OPERATION);
                        recoveryCodeMap.put(CodeConstants.CODE.toString(), RandomStringUtils.random(CODE_LENGTH, true, true));
                        // add expiry duration in current time.
                        currentDate = currentDate.plus(account.getResetPasswordRecoveryCodeExpiry(), ChronoUnit.MILLIS);
                        Long val = currentDate.toInstant().toEpochMilli();
                        recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(val));
                        recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(0));
                    } else {
                        ZimbraLog.account.debug("%s Recovery code not expired yet, so using the same code.",
                                LOG_OPERATION);
                        int resendCount = Integer.valueOf(recoveryCodeMap.get(CodeConstants.RESEND_COUNT.toString()));
                        resendCount++;
                        if (account.getPasswordRecoveryMaxAttempts() < resendCount) {
                            throw ServiceException.INVALID_REQUEST("Max resend attempts reached", null);
                        } else {
                            recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                        }
                    }
                } else {
                    ZimbraLog.account.debug("%s Recovery code not found for %s, creating new one", LOG_OPERATION,
                            email);
                    recoveryCodeMap.put(CodeConstants.EMAIL.toString(), recoveryEmail);
                    recoveryCodeMap.put(CodeConstants.CODE.toString(), RandomStringUtils.random(CODE_LENGTH, true, true));
                    // add expiry duration in current time.
                    currentDate = currentDate.plus(account.getResetPasswordRecoveryCodeExpiry(), ChronoUnit.MILLIS);
                    Long val = currentDate.toInstant().toEpochMilli();
                    recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(val));
                    recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(0));
                }
                sendResetPasswordRecoveryCode();
                saveResetPasswordRecoveryCode(null, null);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid op received", null);
        }
        return resp;
    }

    @Override
    public void sendResetPasswordRecoveryCode() throws ServiceException {
        if (recoveryCodeMap.isEmpty() || account == null) {
            throw ServiceException.INVALID_REQUEST("Invalid request received in sendForgetPasswordCode()", null);
        }
        if (mbox == null) {
            mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        }
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
    public void saveResetPasswordRecoveryCode(AuthToken authToken, HashMap<String, Object> prefs) throws ServiceException {
        if (recoveryCodeMap.isEmpty() || account == null) {
            throw ServiceException.INVALID_REQUEST("Invalid request received in saveRecoveryCode()", null);
        }

        if (prefs == null) {
            prefs = new HashMap<String, Object>();
        }
        prefs.put(Provisioning.A_zimbraResetPasswordRecoveryCode, JWEUtil.getJWE(recoveryCodeMap));
        Provisioning.getInstance().modifyAttrs(account, prefs, true, authToken);
    }

    @Override
    public SetRecoveryEmailResponse handleSetRecoveryEmailRequest(SetRecoveryEmailRequest request,
            ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        SetRecoveryEmailResponse resp = new SetRecoveryEmailResponse();
        Op op = request.getOp();
        if (op == null) {
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        switch (op) {
            case sendCode:
                String recoveryEmailAddr = request.getRecoveryAccount();
                if (StringUtil.isNullOrEmpty(recoveryEmailAddr)) {
                    throw ServiceException.INVALID_REQUEST("Recovery email address not provided.",
                        null);
                }
                validateEmail(recoveryEmailAddr);
                sendCode(recoveryEmailAddr, 0, zsc, octxt);
                break;
            case validateCode:
                String recoveryAccountVerificationCode = request
                    .getRecoveryAccountVerificationCode();
                if (StringUtil.isNullOrEmpty(recoveryAccountVerificationCode)) {
                    throw ServiceException.INVALID_REQUEST(
                        "Recovery email address verification code not provided.", null);
                }
                validateSetRecoveryEmailCode(recoveryAccountVerificationCode, zsc, octxt);
                break;
            case resendCode:
                resendCode(zsc, octxt);
                break;
            case reset:
                resetSetRecoveryEmailCode(zsc);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
            }
        return resp;
    }

    @Override
    public void sendSetRecoveryAccountValidationCode(OperationContext octxt) throws ServiceException {
        String emailIdToVerify = recoveryCodeMap.get(CodeConstants.EMAIL.toString());
        String code = recoveryCodeMap.get(CodeConstants.CODE.toString());
        String expiryTime = recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString());
        Locale locale = account.getLocale();
        String acctDisplayName = account.getDisplayName();
        if (StringUtil.isNullOrEmpty(acctDisplayName)) {
            acctDisplayName = account.getMail();
        }
        String subject = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailSubject, locale,
            acctDisplayName);
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
            MimeMessage mm = AccountUtil.generateMimeMessage(account, account, subject,
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

    private void sendCode(String email, int resendCount, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        if (verificationData != null) {
            Map<String, String> recoveryDataMap = JWEUtil.getDecodedJWE(verificationData);
            String recoveryEmail = recoveryDataMap.get(CodeConstants.EMAIL.toString());
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug("sendCode: existing recovery email: %s", recoveryEmail);
            }
            if (resendCount == 0 && recoveryEmail.equals(email)) {
                throw ServiceException.INVALID_REQUEST(
                    "Verification code already sent to this recovery email.", null);
            }
        }
        String code = RandomStringUtils.random(8, true, true);
        long expiry = account.getRecoveryEmailCodeValidity();
        Date now = new Date();
        long expiryTime = now.getTime() + expiry;
        recoveryCodeMap.put(CodeConstants.EMAIL.toString(), email);
        recoveryCodeMap.put(CodeConstants.CODE.toString(), code);
        recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(expiryTime));
        recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
        sendSetRecoveryAccountValidationCode(octxt);

        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, email);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
            PrefPasswordRecoveryAddressStatus.pending);
        saveSetRecoveryAccountValidationCode(zsc.getAuthToken(), prefs);
    }

    private void resendCode(ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        if (verificationData == null) {
            throw ServiceException
            .FAILURE("The recovery email address verification data is missing.", null);
        }
        Map<String, String> dataMap = JWEUtil.getDecodedJWE(verificationData);
        String email = dataMap.get(CodeConstants.EMAIL.toString());
        String code = dataMap.get(CodeConstants.CODE.toString());
        long expiryTime = Long.parseLong(dataMap.get(CodeConstants.EXPIRY_TIME.toString()));
        int resendCount = Integer.parseInt(dataMap.get(CodeConstants.RESEND_COUNT.toString()));
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String gmtDate = format.format(expiryTime);
            ZimbraLog.passwordreset.debug("resendCode: Current resend count: %d", resendCount);
            ZimbraLog.passwordreset.debug("resendCode: Resending code to: %s", email);
            ZimbraLog.passwordreset.debug("resendCode: Code expiry time: %s", gmtDate);
            ZimbraLog.passwordreset.debug("resendCode: Last 3 characters of resent code: %s",
                code.substring(5));
        }
        if (resendCount < account.getPasswordRecoveryMaxAttempts()) {
            // check if code is expired
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                // generate new code and send
                sendCode(email, resendCount + 1, zsc, octxt);
            } else {
                // send existing code
                // update resend count
                resendCount = resendCount + 1;
                HashMap<String, Object> prefs = new HashMap<String, Object>();
                dataMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, JWEUtil.getJWE(dataMap));
                Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());
                sendSetRecoveryAccountValidationCode(octxt);
            }
        } else {
            throw ServiceException.FAILURE("Resend code request has reached maximum limit.", null);
        }
    }

    @Override
    public void saveSetRecoveryAccountValidationCode(AuthToken authToken, HashMap<String, Object> prefs) throws ServiceException {
        if (recoveryCodeMap.isEmpty() || account == null) {
            throw ServiceException.INVALID_REQUEST("Invalid request received in saveRecoveryCode()", null);
        }
        if (prefs == null) {
            prefs = new HashMap<String, Object>();
        }
        String verificationDataStr = JWEUtil.getJWE(recoveryCodeMap);
        prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, verificationDataStr);
        Provisioning.getInstance().modifyAttrs(account, prefs, true, authToken);
    }

    @Override
    public void validateSetRecoveryEmailCode(String recoveryAccountVerificationCode, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
            String verificationData = account.getRecoveryEmailVerificationData();
            if (verificationData == null) {
                throw ServiceException
                .FAILURE("The recovery email address verification data is missing.", null);
            }
            Map<String, String> recoveryDataMap = JWEUtil.getDecodedJWE(verificationData);
            String code = recoveryDataMap.get(CodeConstants.CODE.toString());
            long expiryTime = Long.parseLong(recoveryDataMap.get(CodeConstants.EXPIRY_TIME.toString()));
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                String gmtDate = format.format(expiryTime);
                ZimbraLog.passwordreset.debug("validateCode: expiryTime for code: %s", gmtDate);
                ZimbraLog.passwordreset.debug("ValidateCode: last 3 characters of recovery code: %s",
                    code.substring(5));
            }
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                throw ServiceException
                    .FAILURE("The recovery email address verification code is expired.", null);
            }
            if (code.equals(recoveryAccountVerificationCode)) {
                HashMap<String, Object> prefs = new HashMap<String, Object>();
                prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
                    PrefPasswordRecoveryAddressStatus.verified);
                prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, null);
                Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
                    zsc.getAuthToken());
            } else {
                throw ServiceException
                    .FAILURE("Verification of recovery email address verification code failed.", null);
            }
        }

    protected void resetSetRecoveryEmailCode(ZimbraSoapContext zsc) throws ServiceException {
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, null);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, null);
        prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, null);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());
    }

    private void validateEmail(String email) throws ServiceException {
        String accountName = account.getName();
        List<String> aliases = Arrays.asList(account.getAliases());
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            ZimbraLog.passwordreset.debug("validateEmail: Primary account name: %s", accountName);
            ZimbraLog.passwordreset.debug("validateEmail: Primary account aliases: %s", aliases);
        }
        if (aliases.contains(email) || accountName.equals(email)) {
            throw ServiceException
                .FAILURE("Recovery address should not be same as primary/alias email address.", null);
        }
    }
}
