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

package com.zimbra.cs.service.mail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.RandomStringUtils;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ZAttrProvisioning.PrefPasswordRecoveryAddressStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ChannelProvider;
import com.zimbra.cs.account.ForgetPasswordException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ResetPasswordUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SetRecoveryAccountRequest;
import com.zimbra.soap.mail.message.SetRecoveryAccountRequest.Op;
import com.zimbra.soap.mail.message.SetRecoveryAccountResponse;
import com.zimbra.soap.type.Channel;

public class SetRecoveryAccount extends DocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        boolean isFromEnableTwoFactorAuth = request.getAttributeBool("isFromEnableTwoFactorAuth", false);
        SetRecoveryAccountRequest req = zsc.elementToJaxb(request);
        if (!isFromEnableTwoFactorAuth) {
            ResetPasswordUtil.validateFeatureResetPasswordStatus(account);
        } else if (!(account.isFeatureTwoFactorAuthAvailable() && Arrays.asList(account.getTwoFactorAuthMethodAllowed()).contains(AccountConstants.E_TWO_FACTOR_METHOD_EMAIL))) {
            throw ServiceException.INVALID_REQUEST("email method is not Allowed", null);
        }
        Op op = req.getOp();
        if (op == null) {
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        Channel channel = req.getChannel();
        if (channel == null) {
            ZimbraLog.passwordreset.debug("Channel not received in request, setting default channel \"email\"");
            channel = Channel.EMAIL;
        }
        ChannelProvider provider = ChannelProvider.getProviderForChannel(channel);
        switch (op) {
            case sendCode:
                String recoveryAccount = req.getRecoveryAccount();
                if (StringUtil.isNullOrEmpty(recoveryAccount)) {
                    throw ServiceException.INVALID_REQUEST("Recovery email address not provided.", null);
                }
                validateEmail(recoveryAccount, account);
                sendCode(recoveryAccount, 0, account, mbox, zsc, octxt, provider, isFromEnableTwoFactorAuth);
                break;
            case validateCode:
                String recoveryAccountVerificationCode = req.getRecoveryAccountVerificationCode();
                if (StringUtil.isNullOrEmpty(recoveryAccountVerificationCode)) {
                    throw ServiceException.INVALID_REQUEST("Recovery email address verification code not provided.",
                            null);
                }
                provider.validateSetRecoveryAccountCode(recoveryAccountVerificationCode, account, mbox, zsc);
                break;
            case resendCode:
                resendCode(account, mbox, zsc, octxt, provider, isFromEnableTwoFactorAuth);
                break;
            case reset:
                reset(mbox, zsc);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        SetRecoveryAccountResponse resp = new SetRecoveryAccountResponse();
        return zsc.jaxbToElement(resp);
    }

    protected void sendCode(String email, int resendCount, Account account, Mailbox mbox, ZimbraSoapContext zsc,
            OperationContext octxt, ChannelProvider provider, boolean isFromEnableTwoFactorAuth) throws ServiceException {
        Map<String, String> recoveryDataMap = provider.getSetRecoveryAccountCodeMap(account);
        if (recoveryDataMap != null && !recoveryDataMap.isEmpty()) {
            String recoveryEmail = recoveryDataMap.get(CodeConstants.EMAIL.toString());
            if (ZimbraLog.passwordreset.isDebugEnabled()) {
                ZimbraLog.passwordreset.debug("sendCode: existing recovery email: %s", recoveryEmail);
            }
            if (resendCount == 0 && recoveryEmail.equals(email)) {
                throw ForgetPasswordException.CODE_ALREADY_SENT("Verification code already sent to this recovery email.");
            }
        }
        String code = RandomStringUtils.random(8, true, true);
        Account authAccount = getAuthenticatedAccount(zsc);
        long expiry = account.getRecoveryAccountCodeValidity();
        Date now = new Date();
        long expiryTime = now.getTime() + expiry;
        Map<String, String> recoveryCodeMap = new HashMap<>();
        recoveryCodeMap.put(CodeConstants.EMAIL.toString(), email);
        recoveryCodeMap.put(CodeConstants.CODE.toString(), code);
        recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(expiryTime));
        recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress,
                recoveryCodeMap.get(CodeConstants.EMAIL.toString()));
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, PrefPasswordRecoveryAddressStatus.pending);
        if (isFromEnableTwoFactorAuth) {
            provider.sendAndStoreTwoFactorAuthAccountCode(authAccount, mbox, recoveryCodeMap, zsc, octxt, prefs);
        } else {
            provider.sendAndStoreSetRecoveryAccountCode(authAccount, mbox, recoveryCodeMap, zsc, octxt, prefs);
        }
    }

    protected void resendCode(Account account, Mailbox mbox, ZimbraSoapContext zsc, OperationContext octxt,
            ChannelProvider provider, boolean isFromEnableTwoFactorAuth) throws ServiceException {
        Map<String, String> dataMap = provider.getSetRecoveryAccountCodeMap(account);
        if (dataMap == null || dataMap.isEmpty()) {
            throw ForgetPasswordException.CODE_NOT_FOUND("Verification code for recovery email address not found on server.");
        }
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
            ZimbraLog.passwordreset.debug("resendCode: Last 3 characters of resent code: %s", code.substring(5));
        }
        if (resendCount < account.getPasswordRecoveryMaxAttempts()) {
            // check if code is expired
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                // generate new code and send
                sendCode(email, resendCount + 1, account, mbox, zsc, octxt, provider, isFromEnableTwoFactorAuth);
            } else {
                // send existing code
                Account authAccount = getAuthenticatedAccount(zsc);
                // update resend count
                resendCount = resendCount + 1;
                dataMap.put(CodeConstants.RESEND_COUNT.toString(), String.valueOf(resendCount));
                if (isFromEnableTwoFactorAuth) {
                    provider.sendAndStoreTwoFactorAuthAccountCode(authAccount, mbox, dataMap, zsc, octxt, null);
                } else {
                    provider.sendAndStoreSetRecoveryAccountCode(authAccount, mbox, dataMap, zsc, octxt, null);
                }
            }
        } else {
            throw ForgetPasswordException.MAX_ATTEMPTS_REACHED("Re-send code request has reached maximum limit.");
        }
    }

    protected void reset(Mailbox mbox, ZimbraSoapContext zsc) throws ServiceException {
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, null);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, null);
        prefs.put(Provisioning.A_zimbraRecoveryAccountVerificationData, null);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());
    }

    private static void validateEmail(String email, Account account) throws ServiceException {
        String accountName = account.getName();
        java.util.List<String> aliases = Arrays.asList(account.getAliases());
        if (ZimbraLog.passwordreset.isDebugEnabled()) {
            ZimbraLog.passwordreset.debug("validateEmail: Primary account name: %s", accountName);
            ZimbraLog.passwordreset.debug("validateEmail: Primary account aliases: %s", aliases);
        }
        if (aliases.contains(email) || accountName.equals(email)) {
            throw ForgetPasswordException.RECOVERY_EMAIL_SAME_AS_PRIMARY_OR_ALIAS("Recovery address should not be same as primary/alias email address.");
        }
    }
}
