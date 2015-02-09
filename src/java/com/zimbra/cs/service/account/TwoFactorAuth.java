package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.auth.twofactor.TOTPCredentials;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;

/** SOAP handler to enable/disable two-factor auth.
 * If enabling, returns the shared secret.
 * @author iraykin
 *
 */
public class TwoFactorAuth extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        TwoFactorManager manager = new TwoFactorManager(account);
        Element response = zsc.createElement(AccountConstants.TWO_FACTOR_RESPONSE);
        String act = request.getAttribute(AccountConstants.E_ACTION);
        TwoFactorAuthAction action = TwoFactorAuthAction.valueOf(act);
        encodeAction(response, action);
        switch (action) {
        case enable:
            TOTPCredentials newCredentials = manager.enableTwoFactorAuth();
             if (newCredentials != null) {
                 encodeCredentials(response, newCredentials);
             } else {
                 encodeAlreadyEnabled(response);
             }
             break;
        case disable:
            boolean disabled = manager.disableTwoFactorAuth();
            if (disabled) {
                encodeDisabled(response);
            } else {
                encodeAlreadyDisabled(response);
            }
            break;
        }
        return response;
    }

    private void encodeAction(Element response, TwoFactorAuthAction action) {
        response.addElement(AccountConstants.E_ACTION).addText(action.toString());
    }

    private void encodeAlreadyDisabled(Element response) {
        response.addElement(MailConstants.E_INFO).addText("two-factor authentication is already disabled");
    }

    private void encodeDisabled(Element response) {
        response.addElement(MailConstants.E_INFO).addText("two-factor authentication has been disabled");
    }

    private void encodeAlreadyEnabled(Element response) {
        response.addElement(MailConstants.E_INFO).addText("two-factor authentication is already enabled on this account");
    }

    @SuppressWarnings("deprecation")
    private void encodeCredentials(Element response,
            TOTPCredentials credentials) {
        Element container = response.addElement(AccountConstants.E_TWO_FACTOR_CREDENTIALS);
        Element secret = container.addElement(AccountConstants.E_TWO_FACTOR_SECRET);
        secret.addText(credentials.getSecret());
        Element codes = container.addElement(AccountConstants.E_TWO_FACTOR_SCRATCH_CODES);
        for (String code: credentials.getScratchCodes()) {
            Element codeElt = codes.addElement(AccountConstants.E_TWO_FACTOR_SCRATCH_CODE);
            codeElt.setText(code);
        }
    }

    public enum TwoFactorAuthAction {
        enable, disable;
    }
}