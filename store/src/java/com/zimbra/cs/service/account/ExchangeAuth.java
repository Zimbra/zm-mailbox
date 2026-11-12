package com.zimbra.cs.service.account;

import java.util.Map;
import javax.servlet.ServletRequest;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.zco.ZcoExchangeCredentialService;
import com.zimbra.cs.listeners.AuthListener;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class ExchangeAuth extends Auth {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        final ZimbraSoapContext zsc = getZimbraSoapContext(context);
        final Provisioning prov = Provisioning.getInstance();
        final ZcoExchangeCredentialService service = ZcoExchangeCredentialService.getInstance();

        if (!checkPasswordSecurity(context)) {
            throw ServiceException.INVALID_REQUEST("clear text password is not allowed", null);
        }

        final String credential = request.getAttribute(AccountConstants.E_CREDENTIAL, null);
        final String verifier = request.getAttribute(AccountConstants.E_VERIFIER, null);
        final Account account;

        try {
            service.checkFormat(credential, verifier);
            final Account requested = resolveRequestedAccount(prov, request);
            account = service.redeem(credential, verifier, requested);
        } catch (AuthFailedServiceException e) {
            AuthListener.invokeOnException(e);
            throw e;
        }
        AccountUtil.addAccountToLogContext(prov, account.getId(), ZimbraLog.C_NAME, ZimbraLog.C_ID, null);

        try {
            prov.accountAuthed(account);
        } catch (ServiceException e) {
            ZimbraLog.security.warn("ZCO exchange: unable to record last logon for account=%s", account.getId(), e);
        }

        // Session token is minted only now, after the credential has been redeemed
        final AuthToken at = AuthProvider.getAuthToken(account);

        final ServletRequest httpReq = (ServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        httpReq.setAttribute(CsrfFilter.AUTH_TOKEN, at);

        AuthListener.invokeOnSuccess(account);

        String src = (String) context.get(SoapEngine.ORIG_REQUEST_IP);
        if (src == null || src.isEmpty()) {
            src = (String) context.get(SoapEngine.REQUEST_IP);
        }

        ZimbraLog.security.info("ZCO exchange succeeded: account=%s key=%s src=%s", account.getId(),
                ZcoExchangeCredentialService.auditKey(credential), src);

        return doResponse(request, at, zsc, context, account, false, null, null);
    }

    private static Account resolveRequestedAccount(Provisioning prov, Element request) {
        try {
            final Element acctEl = request.getOptionalElement(AccountConstants.E_ACCOUNT);
            if (acctEl == null) {
                return null;
            }
            String value = acctEl.getText();
            final AccountBy by = AccountBy.fromString(acctEl.getAttribute(AccountConstants.A_BY, AccountBy.name.name()));
            if (value == null || value.isEmpty()) {
                return null;
            }
            if (by == AccountBy.name) {
                final Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
                final String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
                if (virtualHost != null && value.indexOf('@') == -1) {
                    final Domain d = prov.get(Key.DomainBy.virtualHostname, virtualHost);
                    if (d != null) {
                        value = value + "@" + d.getName();
                    }
                }
            }
            return prov.get(by, value);
        } catch (ServiceException e) {
            ZimbraLog.security.warn("ZCO exchange: account element did not resolve");
            return null;
        }
    }
}