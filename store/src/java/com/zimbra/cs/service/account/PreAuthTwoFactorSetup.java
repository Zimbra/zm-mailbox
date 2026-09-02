/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
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
package com.zimbra.cs.service.account;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ZAttrProvisioning.PrefPasswordRecoveryAddressStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.ChannelProvider;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Enrol an already-authenticated user in email two-factor auth <em>without</em> asking for their
 * password.
 * <p>
 * This exists because the stock {@code EnableTwoFactorAuthRequest} calls
 * {@code Account.authAccount(password, ...)} unconditionally on its first leg -- it accepts neither
 * an ENABLE_TWO_FACTOR_AUTH token nor the caller's existing session in place of a password. Users
 * arriving through PreAuth/SSO have been vouched for upstream and have no password to give, so they
 * could never complete enrolment.
 * <p>
 * The two operations mirror what the stock first leg does for the email method, but authenticate by
 * session instead:
 * <ul>
 *   <li>{@code sendCode} -- store the chosen address as pending and email a verification code,
 *       exactly as {@code SetRecoveryAccount} does with {@code isFromEnableTwoFactorAuth}.</li>
 *   <li>{@code validateCode} -- verify the code, mark the address verified, and switch email 2FA on
 *       for the account.</li>
 * </ul>
 *
 * @see com.zimbra.cs.service.PreAuthServlet
 */
public class PreAuthTwoFactorSetup extends AccountDocumentHandler {

    public static final String OP_SEND_CODE = "sendCode";
    public static final String OP_VALIDATE_CODE = "validateCode";

    /**
     * The enrolment page authenticates with an ENABLE_TWO_FACTOR_AUTH token passed in the request,
     * not with a session: SOAP on this deployment rejects cookie-only auth, and the session cookie
     * is HttpOnly so the page cannot read it. The token is validated below.
     */
    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String rawToken = request.getAttribute(AccountConstants.E_AUTH_TOKEN, null);
        if (StringUtil.isNullOrEmpty(rawToken)) {
            throw ServiceException.AUTH_REQUIRED();
        }
        AuthToken setupToken;
        try {
            setupToken = AuthProvider.getAuthToken(rawToken);
        } catch (AuthTokenException e) {
            throw AuthFailedServiceException.AUTH_FAILED("invalid enrolment token", e);
        }
        if (setupToken == null || setupToken.isExpired() || !setupToken.isRegistered()) {
            throw AuthFailedServiceException.AUTH_FAILED("invalid enrolment token");
        }
        if (setupToken.getUsage() != Usage.ENABLE_TWO_FACTOR_AUTH) {
            throw AuthFailedServiceException.AUTH_FAILED("wrong token usage for enrolment");
        }
        Account account = AuthProvider.validateAuthToken(prov, setupToken, false,
                Usage.ENABLE_TWO_FACTOR_AUTH);

        if (!account.isFeatureTwoFactorAuthAvailable()) {
            throw ServiceException.CANNOT_ENABLE_TWO_FACTOR_AUTH();
        }

        String op = request.getAttribute(AccountConstants.E_ACTION);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        OperationContext octxt = new OperationContext(account);
        ChannelProvider provider = ChannelProvider.getProviderForChannel(AccountConstants.E_EMAIL);

        Element response = zsc.createElement(AccountConstants.PRE_AUTH_TWO_FACTOR_SETUP_RESPONSE);

        if (OP_SEND_CODE.equals(op)) {
            String email = request.getAttribute(AccountConstants.E_EMAIL);
            sendCode(email, account, mbox, zsc, octxt, provider);
            response.addUniqueElement(AccountConstants.E_STATUS).setText("sent");
        } else if (OP_VALIDATE_CODE.equals(op)) {
            String code = request.getAttribute(AccountConstants.E_TWO_FACTOR_CODE);
            validateCodeAndEnable(code, account, mbox, zsc, provider);
            response.addUniqueElement(AccountConstants.E_STATUS).setText("enabled");
        } else {
            throw ServiceException.INVALID_REQUEST("unknown op: " + op, null);
        }

        return response;
    }

    /**
     * Store the chosen address as the pending recovery address and email a verification code.
     * Mirrors {@code SetRecoveryAccount.sendCode(..., isFromEnableTwoFactorAuth=true)}.
     */
    private void sendCode(String email, Account account, Mailbox mbox, ZimbraSoapContext zsc,
            OperationContext octxt, ChannelProvider provider) throws ServiceException {
        if (StringUtil.isNullOrEmpty(email)) {
            throw ServiceException.INVALID_REQUEST("recovery email must not be empty", null);
        }
        // Sending the code to the mailbox being protected would defeat the point of the factor.
        if (email.equalsIgnoreCase(account.getName())) {
            throw ServiceException.INVALID_REQUEST(
                    "recovery email must differ from the primary address", null);
        }

        String code = RandomStringUtils.random(8, true, true);
        long expiryTime = new Date().getTime() + account.getRecoveryAccountCodeValidity();

        Map<String, String> recoveryCodeMap = new HashMap<String, String>();
        recoveryCodeMap.put(CodeConstants.EMAIL.toString(), email);
        recoveryCodeMap.put(CodeConstants.CODE.toString(), code);
        recoveryCodeMap.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(expiryTime));
        recoveryCodeMap.put(CodeConstants.RESEND_COUNT.toString(), "0");

        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, email);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
                PrefPasswordRecoveryAddressStatus.pending);

        provider.sendAndStoreTwoFactorAuthAccountCode(account, mbox, recoveryCodeMap, zsc, octxt, prefs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {
                "cmd", "PreAuthTwoFactorSetup", "account", account.getName(),
                "op", OP_SEND_CODE, "recoveryAddress", StringUtil.maskEmail(email) }));
    }

    /**
     * Verify the emailed code, mark the recovery address verified, and turn email 2FA on.
     * <p>
     * {@code validateSetRecoveryAccountCode} throws on mismatch or expiry, so reaching the enable
     * step means the user demonstrated control of the address.
     */
    private void validateCodeAndEnable(String code, Account account, Mailbox mbox,
            ZimbraSoapContext zsc, ChannelProvider provider) throws ServiceException {
        if (StringUtil.isNullOrEmpty(code)) {
            throw ServiceException.INVALID_REQUEST("verification code must not be empty", null);
        }

        provider.validateSetRecoveryAccountCode(code, account, mbox, zsc);

        // Enable through the two-factor manager rather than by writing zimbraTwoFactorAuthEnabled
        // directly: that attribute has a callback which rejects the change unless a shared secret
        // exists, and generateCredentials() is what puts one there. enable() also fires the
        // registered TwoFactorChangeListeners.
        TwoFactorAuth mgr = TwoFactorAuth.getFactory().getTwoFactorAuth(account);
        mgr.generateCredentials();

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+" + Provisioning.A_zimbraTwoFactorAuthMethodEnabled, AccountConstants.E_EMAIL);
        attrs.put(Provisioning.A_zimbraPrefPrimaryTwoFactorAuthMethod, AccountConstants.E_EMAIL);
        Provisioning.getInstance().modifyAttrs(account, attrs, true, null);

        mgr.enable();

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {
                "cmd", "PreAuthTwoFactorSetup", "account", account.getName(),
                "op", OP_VALIDATE_CODE, "info", "email two-factor auth enabled" }));
    }
}
