package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.TrustedDeviceToken;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.RevokeOtherTrustedDevicesResponse;

public class RevokeOtherTrustedDevices extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        RevokeOtherTrustedDevicesResponse response = new RevokeOtherTrustedDevicesResponse();
        TwoFactorManager manager = new TwoFactorManager(account);
        TrustedDeviceToken token = TrustedDeviceToken.fromRequest(account, request, context);
        manager.revokeOtherTrustedDevices(token);
        return zsc.jaxbToElement(response);
    }

}
