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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ZAttrProvisioning.FeatureResetPasswordStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ChannelProvider;
import com.zimbra.cs.account.ForgetPasswordException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.cs.service.util.ResetPasswordUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecoverAccountRequest;
import com.zimbra.soap.mail.message.RecoverAccountResponse;
import com.zimbra.soap.mail.type.RecoverAccountOperation;
import com.zimbra.soap.type.Channel;

public final class RecoverAccount extends MailDocumentHandler {
    public static final String LOG_OPERATION = "RecoverAccount:";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        RecoverAccountRequest req = zsc.elementToJaxb(request);
        req.validateRecoverAccountRequest();
        RecoverAccountOperation op = req.getOp();
        String email = req.getEmail();
        Account user = Provisioning.getInstance().getAccountByName(email);
        if (user == null) {
            ZimbraLog.passwordreset.debug("%s Account not found for %s", LOG_OPERATION, email);
            throw ForgetPasswordException.CONTACT_ADMIN("Something went wrong. Please contact your administrator.");
        }
        ResetPasswordUtil.validateFeatureResetPasswordStatus(user);
        if (user.getPrefPasswordRecoveryAddressStatus() == null
                || !user.getPrefPasswordRecoveryAddressStatus().isVerified()) {
            ZimbraLog.passwordreset.debug("%s Verified recovery email is not found for %s", LOG_OPERATION, email);
            throw ForgetPasswordException.CONTACT_ADMIN("Something went wrong. Please contact your administrator.");
        }
        Channel channel = req.getChannel();
        ChannelProvider provider = ChannelProvider.getProviderForChannel(channel);
        String recoveryAccount = provider.getRecoveryAccount(user);
        if (StringUtil.isNullOrEmpty(recoveryAccount)) {
            ZimbraLog.passwordreset.debug("%s Recovery account missing or unverified for %s", LOG_OPERATION, email);
            throw ForgetPasswordException.CONTACT_ADMIN("Something went wrong. Please contact your administrator.");
        }

        Element response = proxyIfNecessary(request, context, user);
        if (response == null) {
            RecoverAccountResponse resp = new RecoverAccountResponse();
            switch (op) {
            case GET_RECOVERY_ACCOUNT:
                recoveryAccount = StringUtil.maskEmail(recoveryAccount);
                ZimbraLog.passwordreset.debug("%s Recovery account: %s", LOG_OPERATION, recoveryAccount);
                resp.setRecoveryAccount(recoveryAccount);
                break;
            case SEND_RECOVERY_CODE:
                ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.systemDefault());
                int maxAttempts = user.getPasswordRecoveryMaxAttempts();
                int resendCount = 0;
                Map<String, String> recoveryCodeMap = provider.getResetPasswordRecoveryCodeMap(user);
                if (recoveryCodeMap != null && !recoveryCodeMap.isEmpty()) {
                    resendCount = Integer.valueOf(recoveryCodeMap.get(CodeConstants.RESEND_COUNT.toString()));
                    if (resendCount >= maxAttempts) {
                        user.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.suspended);
                        long suspension = user.getFeatureResetPasswordSuspensionTime();
                        Date now = new Date();
                        long suspensionTime = now.getTime() + suspension;
                        recoveryCodeMap.put(CodeConstants.SUSPENSION_TIME.toString(), String.valueOf(suspensionTime));
                        HashMap<String, Object> prefs = new HashMap<String, Object>();
                        prefs.put(Provisioning.A_zimbraResetPasswordRecoveryCode, JWEUtil.getJWE(recoveryCodeMap));
                        Provisioning.getInstance().modifyAttrs(user, prefs, true, zsc.getAuthToken());
                        throw ForgetPasswordException.MAX_ATTEMPTS_REACHED_SUSPEND_FEATURE("Max re-send attempts reached, feature is suspended.");
                    } else {
                        ZonedDateTime storedDate = Instant
                                .ofEpochMilli(Long.valueOf(recoveryCodeMap.get(CodeConstants.EXPIRY_TIME.toString())))
                                .atZone(ZoneId.systemDefault());
                        if (ChronoUnit.MILLIS.between(currentDate, storedDate) <= 0L) {
                            ZimbraLog.passwordreset.debug("%s Recovery code expired, so generating new one.", LOG_OPERATION);
                            recoveryCodeMap.put(CodeConstants.CODE.toString(), RandomStringUtils.random(8, true, true));
                            // add expiry duration in current time.
                            currentDate = currentDate.plus(user.getResetPasswordRecoveryCodeExpiry(), ChronoUnit.MILLIS);
                            Long val = currentDate.toInstant().toEpochMilli();
                            recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(val));
                        } else {
                            ZimbraLog.passwordreset.debug("%s Recovery code not expired yet, so using the same code.",
                                    LOG_OPERATION);
                        }
                        resendCount++;
                        recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                    }
                } else {
                    ZimbraLog.passwordreset.debug("%s Recovery code not found for %s, creating new one", LOG_OPERATION, email);
                    recoveryCodeMap = new HashMap<String, String>();
                    recoveryCodeMap.put(CodeConstants.EMAIL.toString(), recoveryAccount);
                    recoveryCodeMap.put(CodeConstants.CODE.toString(), RandomStringUtils.random(8, true, true));
                    // add expiry duration in current time.
                    currentDate = currentDate.plus(user.getResetPasswordRecoveryCodeExpiry(), ChronoUnit.MILLIS);
                    Long val = currentDate.toInstant().toEpochMilli();
                    recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(val));
                    recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                }
                resp.setRecoveryAttemptsLeft(maxAttempts - resendCount);
                provider.sendAndStoreResetPasswordRecoveryCode(zsc, user, recoveryCodeMap);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid op received", null);
            }
            response = zsc.jaxbToElement(resp);
        }
        return response;
    }

    private Element proxyIfNecessary(Element request, Map<String, Object> context, Account acct)
            throws ServiceException {
        Provisioning.Reasons reasons = new Provisioning.Reasons();
        if (acct != null && !Provisioning.onLocalServer(acct, reasons)) {
            ZimbraLog.passwordreset.debug("Proxying RecoverAccount request: requestedAccountId=%s", acct.getId());
            return proxyRequest(request, context, acct.getId());
        }
        return null;
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
