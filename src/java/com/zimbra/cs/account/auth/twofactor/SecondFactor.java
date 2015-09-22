package com.zimbra.cs.account.auth.twofactor;

import com.zimbra.common.service.ServiceException;

public interface SecondFactor {

    public void authenticate(String secondFactor) throws ServiceException;
}
