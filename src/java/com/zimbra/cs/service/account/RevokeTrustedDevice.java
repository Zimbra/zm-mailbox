package com.zimbra.cs.service.account;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.TrustedDeviceToken;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.RevokeTrustedDeviceResponse;

public class RevokeTrustedDevice extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        RevokeTrustedDeviceResponse response = new RevokeTrustedDeviceResponse();
        TwoFactorManager manager = new TwoFactorManager(account);
        TrustedDeviceToken token = TrustedDeviceToken.fromRequest(account, request, context);
        if (token != null) {
            manager.revokeTrustedDevice(token);
            HttpServletResponse resp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
            ZimbraCookie.clearCookie(resp, ZimbraCookie.COOKIE_ZM_TRUST_TOKEN);
        } else {
            ZimbraLog.account.debug("No trusted device token available");
        }
        return zsc.jaxbToElement(response);
    }

}
