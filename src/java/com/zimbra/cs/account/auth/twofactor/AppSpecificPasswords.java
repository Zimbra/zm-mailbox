package com.zimbra.cs.account.auth.twofactor;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AppSpecificPassword;

public interface AppSpecificPasswords extends SecondFactor {
    public boolean isEnabled() throws ServiceException;
    public AppSpecificPassword generatePassword(String name) throws ServiceException;
    public Set<AppSpecificPasswordData> getPasswords() throws ServiceException;
    public void revoke(String appName) throws ServiceException;
    public void revokeAll() throws ServiceException;
}
