package com.zimbra.cs.account.auth.twofactor;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;

public interface ScratchCodes extends SecondFactor {
    public List<String> getCodes();
    public List<String> generateCodes(CredentialConfig config) throws ServiceException;
    public void storeCodes(List<String> codes) throws ServiceException;
}

