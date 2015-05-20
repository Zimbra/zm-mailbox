package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.twofactor.TOTPCredentials;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.TwoFactorCredentials;

/** SOAP handler to enable/disable two-factor auth.
 * If enabling, returns the shared secret.
 * @author iraykin
 *
 */
public class EnableTwoFactorAuth extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String acctName = request.getAttribute(AccountConstants.A_NAME);
        Account account = prov.get(AccountBy.name, acctName);
        TwoFactorManager manager = new TwoFactorManager(account);
        EnableTwoFactorAuthResponse response = new EnableTwoFactorAuthResponse();
        String password = request.getAttribute(AccountConstants.A_PASSWORD);
        account.authAccount(password, Protocol.soap);
        if (account.isPrefTwoFactorAuthEnabled()) {
            encodeAlreadyEnabled(response);
        } else {
            TOTPCredentials newCredentials = manager.enableTwoFactorAuth();
            int tokenValidityValue = account.getAuthTokenValidityValue();
            account.setAuthTokenValidityValue(tokenValidityValue == Integer.MAX_VALUE ? 0 : tokenValidityValue + 1);
            encodeCredentials(response, newCredentials);
        }
        return zsc.jaxbToElement(response);
    }

    private void encodeAlreadyEnabled(EnableTwoFactorAuthResponse response) {
        //what to return if two-factor auth is already enabled?
    }

    private void encodeCredentials(EnableTwoFactorAuthResponse response,
            TOTPCredentials credentials) {
        TwoFactorCredentials credsResponse = new TwoFactorCredentials();
        credsResponse.setSharedSecret(credentials.getSecret());
        credsResponse.setScratchCodes(credentials.getScratchCodes());
        response.setCredentials(credsResponse);
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}