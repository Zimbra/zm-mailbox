package com.zimbra.cs.service.account;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.twofactor.TOTPCredentials;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.SoapServlet;
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
        String acctNamePassedIn = request.getElement(AccountConstants.E_NAME).getText();
        Account account = prov.get(AccountBy.name, acctNamePassedIn);
        if (account == null) {
            throw AuthFailedServiceException.AUTH_FAILED("no such account");
        }
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            throw ServiceException.CANNOT_ENABLE_TWO_FACTOR_AUTH();
        }
        TwoFactorManager manager = new TwoFactorManager(account, acctNamePassedIn);
        EnableTwoFactorAuthResponse response = new EnableTwoFactorAuthResponse();
        Element passwordEl = request.getOptionalElement(AccountConstants.E_PASSWORD);
        String password = null;
        if (passwordEl != null) {
            password = passwordEl.getText();
        }
        Element twoFactorCode = request.getOptionalElement(AccountConstants.E_TWO_FACTOR_CODE);
        if (twoFactorCode == null) {
            account.authAccount(password, Protocol.soap);
            if (account.isTwoFactorAuthEnabled()) {
                encodeAlreadyEnabled(response);
            } else {
                TOTPCredentials newCredentials = manager.generateCredentials();
                response.setSecret(newCredentials.getSecret());
                try {
                String token = AuthProvider.getAuthToken(account, Usage.ENABLE_TWO_FACTOR_AUTH).getEncoded();
                com.zimbra.soap.account.type.AuthToken at = new com.zimbra.soap.account.type.AuthToken(token, false);
                response.setAuthToken(at);
                } catch (AuthTokenException e) {
                    throw ServiceException.FAILURE("cannot generate auth token", e);
                }
            }
        } else {
            Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
            if (authTokenEl != null) {
                AuthToken at;
                try {
                    at = AuthProvider.getAuthToken(authTokenEl, account);
                } catch (AuthTokenException e) {
                    throw AuthFailedServiceException.AUTH_FAILED("invalid auth token");
                } try {
                    Account authTokenAcct = AuthProvider.validateAuthToken(prov, at, false, Usage.ENABLE_TWO_FACTOR_AUTH);
                    boolean verifyAccount = authTokenEl.getAttributeBool(AccountConstants.A_VERIFY_ACCOUNT, false);
                    if (verifyAccount && !authTokenAcct.getId().equalsIgnoreCase(account.getId())) {
                        throw AuthFailedServiceException.AUTH_FAILED("auth token doesn't match the named account");
                    }
                } finally {
                    if (at != null) {
                        try {
                            at.deRegister();
                        } catch (AuthTokenException e) {
                            ZimbraLog.account.warn("could not de-register two-factor authentication auth token");
                        }
                    }
                }
            } else if (password != null) {
                account.authAccount(password, Protocol.soap);
            } else {
                throw AuthFailedServiceException.AUTH_FAILED("auth token and password missing");
            }
            manager.authenticateTOTP(twoFactorCode.getText());
            manager.enableTwoFactorAuth();
            response.setScratchCodes(manager.getScratchCodes());
            int tokenValidityValue = account.getAuthTokenValidityValue();
            account.setAuthTokenValidityValue(tokenValidityValue == Integer.MAX_VALUE ? 0 : tokenValidityValue + 1);
            HttpServletRequest httpReq = (HttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
            HttpServletResponse httpResp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
            try {
                AuthToken at = AuthProvider.getAuthToken(account);
                response.setAuthToken(new com.zimbra.soap.account.type.AuthToken(at.getEncoded(), false));
                at.encode(httpResp, false, ZimbraCookie.secureCookie(httpReq), false);
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("cannot generate auth token", e);
            }
        }
        return zsc.jaxbToElement(response);
    }

    private void encodeAlreadyEnabled(EnableTwoFactorAuthResponse response) {}

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}