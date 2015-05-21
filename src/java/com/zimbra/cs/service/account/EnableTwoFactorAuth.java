package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.twofactor.TOTPCredentials;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;

/** SOAP handler to enable two-factor auth.
 * @author iraykin
 *
 */
public class EnableTwoFactorAuth extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String acctName = request.getElement(AccountConstants.E_NAME).getText();
        Account account = prov.get(AccountBy.name, acctName);
        if (account == null) {
            throw AuthFailedServiceException.AUTH_FAILED("no such account");
        }
        TwoFactorManager manager = new TwoFactorManager(account);
        EnableTwoFactorAuthResponse response = new EnableTwoFactorAuthResponse();
        String password = request.getElement(AccountConstants.E_PASSWORD).getText();
        account.authAccount(password, Protocol.soap);
        Element twoFactorCode = request.getOptionalElement(AccountConstants.E_TWO_FACTOR_CODE);
        if (twoFactorCode == null) {
            if (account.isPrefTwoFactorAuthEnabled()) {
                encodeAlreadyEnabled(response);
            } else {
                TOTPCredentials newCredentials = manager.generateCredentials();
                response.setSecret(newCredentials.getSecret());
            }
        } else {
            manager.authenticateTOTP(twoFactorCode.getText());
            manager.enableTwoFactorAuth();
            response.setScratchCodes(manager.getScratchCodes());
            int tokenValidityValue = account.getAuthTokenValidityValue();
            account.setAuthTokenValidityValue(tokenValidityValue == Integer.MAX_VALUE ? 0 : tokenValidityValue + 1);
        }
        return zsc.jaxbToElement(response);
    }

    private void encodeAlreadyEnabled(EnableTwoFactorAuthResponse response) {}

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}