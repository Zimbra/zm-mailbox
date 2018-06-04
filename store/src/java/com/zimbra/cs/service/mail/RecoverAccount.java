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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EmailRecoveryCode;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SendRecoveryCode;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecoverAccountRequest;
import com.zimbra.soap.mail.message.RecoverAccountResponse;
import com.zimbra.soap.mail.type.PasswordResetOperation;
import com.zimbra.soap.type.Channel;

public final class RecoverAccount extends MailDocumentHandler {
    public static final String LOG_OPERATION = "RecoverAccount:";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        RecoverAccountRequest req = zsc.elementToJaxb(request);
        req.validateRecoverAccountRequest();
        PasswordResetOperation op = req.getOp();
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

        RecoverAccountResponse resp = new RecoverAccountResponse();
        switch (channel) {
            case EMAIL:
                String recoveryEmail = user.getPrefPasswordRecoveryAddress();
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
                        String storedCodeString = user.getResetPasswordRecoveryCode();
                        ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.systemDefault());
                        Map<String, String> recoveryCodeMap = new HashMap<String, String>();
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
                                recoveryCodeMap.put(CodeConstants.CODE.toString(), RandomStringUtils.random(8, true, true));
                                // add expiry duration in current time.
                                currentDate = currentDate.plus(user.getResetPasswordRecoveryCodeExpiry(), ChronoUnit.MILLIS);
                                Long val = currentDate.toInstant().toEpochMilli();
                                recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(val));
                                recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(0));
                            } else {
                                ZimbraLog.account.debug("%s Recovery code not expired yet, so using the same code.",
                                        LOG_OPERATION);
                                int resendCount = Integer.valueOf(recoveryCodeMap.get(CodeConstants.RESEND_COUNT.toString()));
                                resendCount++;
                                if (user.getPasswordRecoveryMaxAttempts() < resendCount) {
                                    throw ServiceException.INVALID_REQUEST("Max resend attempts reached", null);
                                } else {
                                    recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                                }
                            }
                        } else {
                            ZimbraLog.account.debug("%s Recovery code not found for %s, creating new one", LOG_OPERATION,
                                    email);
                            recoveryCodeMap.put(CodeConstants.EMAIL.toString(), recoveryEmail);
                            recoveryCodeMap.put(CodeConstants.CODE.toString(), RandomStringUtils.random(8, true, true));
                            // add expiry duration in current time.
                            currentDate = currentDate.plus(user.getResetPasswordRecoveryCodeExpiry(), ChronoUnit.MILLIS);
                            Long val = currentDate.toInstant().toEpochMilli();
                            recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(val));
                            recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(0));
                        }
                        sendAndStoreRecoveryCode(user, recoveryCodeMap, channel);
                        break;
                    default:
                        throw ServiceException.INVALID_REQUEST("Invalid op received", null);
                }
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid channel received", null);
        }
        return zsc.jaxbToElement(resp);
    }

    private void sendAndStoreRecoveryCode(Account user, Map<String, String> recoveryCodeMap, Channel channel) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(user);
        SendRecoveryCode sendRecoveryCode = null;
        switch (channel) {
            case EMAIL:
                sendRecoveryCode = new EmailRecoveryCode(recoveryCodeMap, mbox, user);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid channel received", null);
        }
        sendRecoveryCode.sendForgetPasswordCode();
        // store the same in ldap attribute for user
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraResetPasswordRecoveryCode, JWEUtil.getJWE(recoveryCodeMap));
        Provisioning.getInstance().modifyAttrs(user, prefs, true, null);
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
