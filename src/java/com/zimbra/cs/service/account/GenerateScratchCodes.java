package com.zimbra.cs.service.account;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GenerateScratchCodesResponse;

public class GenerateScratchCodes extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        TwoFactorManager manager = new TwoFactorManager(account);
        if (!manager.twoFactorAuthEnabled()) {
            throw ServiceException.FAILURE("two-factor authentication is not enabled", new Throwable());
        }
        List<String> scratchCodes = manager.generateNewScratchCodes();
        GenerateScratchCodesResponse response = new GenerateScratchCodesResponse();
        response.setScratchCodes(scratchCodes);
        return zsc.jaxbToElement(response);
    }
}
