package com.zimbra.cs.account.auth.twofactor;

import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;

public class TwoFactorAuthUnavailable extends TwoFactorAuth {

    public TwoFactorAuthUnavailable(Account account) {
        super(account, null);
    }

    @Override
    public boolean twoFactorAuthRequired() throws ServiceException {
        return false;
    }

    @Override
    public boolean twoFactorAuthEnabled() throws ServiceException {
        return false;
    }

    @Override
    public void enableTwoFactorAuth() throws ServiceException {}

    @Override
    public void disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException {}

    @Override
    public CredentialConfig getCredentialConfig() throws ServiceException {
        return null;
    }

    @Override
    public AuthenticatorConfig getAuthenticatorConfig() throws ServiceException {
        return null;
    }

    @Override
    public Credentials generateCredentials() throws ServiceException {
        return null;
    }

    @Override
    public void authenticateTOTP(String code) throws ServiceException {}

    @Override
    public void clearData() throws ServiceException {}

    @Override
    public void authenticate(String secondFactor) throws ServiceException {}
}
