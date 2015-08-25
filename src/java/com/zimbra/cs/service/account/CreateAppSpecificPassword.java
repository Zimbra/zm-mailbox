package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AppSpecificPassword;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.CreateAppSpecificPasswordResponse;

public class CreateAppSpecificPassword extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        String appName = request.getAttribute(AccountConstants.A_APP_NAME);
        TwoFactorManager manager = new TwoFactorManager(account);
        if (!manager.twoFactorAuthEnabled()) {
            throw AuthFailedServiceException.AUTH_FAILED("two-factor authentication must be enabled");
        }
        AppSpecificPassword password = manager.generateAppSpecificPassword(appName);
        CreateAppSpecificPasswordResponse response = new CreateAppSpecificPasswordResponse();
        response.setPassword(password.getPassword());
        return zsc.jaxbToElement(response);
    }
}
