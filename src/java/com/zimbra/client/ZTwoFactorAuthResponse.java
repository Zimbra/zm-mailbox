package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.account.message.TwoFactorAuthResponse;

public class ZTwoFactorAuthResponse {
    private ZTOTPCredentials credentials; //null if this is the response to disabling two-factor auth
    private boolean sucessfullyEnabled = false;
    private boolean sucessfullyDisabled = false;
    private String info;

    public ZTwoFactorAuthResponse(TwoFactorAuthResponse response) throws ServiceException {
        if (response.getCredentials() != null) {
            credentials = new ZTOTPCredentials(response.getCredentials());
            sucessfullyEnabled = true;
        } else {
            //not really returning anything, for now just set a flag
            sucessfullyDisabled = true;
        }
        info = response.getInfo();
    }

    public ZTOTPCredentials getCredentials() {
        return credentials;
    }

    public boolean twoFactorAuthDisabled() {
        return sucessfullyDisabled;
    }

    public boolean twoFactorAuthEnabled() {
        return sucessfullyEnabled;
    }

    public String getInfo() {
        return info;
    }
}
