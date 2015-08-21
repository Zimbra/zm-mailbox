package com.zimbra.cs.service.account;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.TrustedDevice;
import com.zimbra.cs.account.TrustedDeviceToken;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetTrustedDevicesResponse;

public class GetTrustedDevices extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        GetTrustedDevicesResponse response = new GetTrustedDevicesResponse();
        TwoFactorManager manager = new TwoFactorManager(account);
        if (!manager.twoFactorAuthEnabled()) {
            throw AccountServiceException.TWO_FACTOR_AUTH_REQUIRED();
        }
        List<TrustedDevice> devices = manager.getTrustedDevices();
        TrustedDeviceToken token = TrustedDeviceToken.fromRequest(account, request, context);
        boolean thisDeviceTrusted = false;
        int numOtherTrustedDevices = 0;
        if (token == null) {
            numOtherTrustedDevices = devices.size();
        } else {
            for (TrustedDevice td: devices) {
                if (token.getId().equals(td.getTokenId())) {
                    thisDeviceTrusted = true;
                } else {
                    numOtherTrustedDevices++;
                }
            }
        }
        response.setThisDeviceTrusted(thisDeviceTrusted);
        response.setNumOtherTrustedDevices(numOtherTrustedDevices);
        return zsc.jaxbToElement(response);
    }

}
