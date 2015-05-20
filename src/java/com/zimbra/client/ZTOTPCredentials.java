package com.zimbra.client;

import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.account.message.TwoFactorCredentials;

public class ZTOTPCredentials {
    private String secret;
    private Set<String> scratchCodes;

    public ZTOTPCredentials(TwoFactorCredentials twoFactorCredentials) throws ServiceException {
        secret = twoFactorCredentials.getSharedSecret();
        scratchCodes = new HashSet<String>();
        for (String code: twoFactorCredentials.getScratchCodes()) {
            scratchCodes.add(code);
        }
    }

    public String getSecret() {
        return secret;
    }

    public Set<String> getScratchCodes() {
        return scratchCodes;
    }
}
