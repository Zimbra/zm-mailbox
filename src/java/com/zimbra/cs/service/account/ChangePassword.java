/*
 * Created on Sep 3, 2004
 */
package com.liquidsys.coco.service.account;

import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class ChangePassword extends WriteOpDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);

        String name = request.getAttribute(AccountService.E_ACCOUNT);
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.getAccountByName(name);
        if (acct == null)
            throw AccountServiceException.AUTH_FAILED(name);
		String oldPassword = request.getAttribute(AccountService.E_OLD_PASSWORD);
		String newPassword = request.getAttribute(AccountService.E_PASSWORD);
		prov.changePassword(acct, oldPassword, newPassword);

        Element response = lc.createElement(AccountService.CHANGE_PASSWORD_RESPONSE);
        return response;
	}

    public boolean needsAuth(Map context) {
        // This command can be sent before authenticating, so this method
        // should return false.  The Account.changePassword() method called
        // from handle() will internally make sure the old password provided
        // matches the current password of the account.
        //
        // The user identity in the auth token, if any, is ignored.
        return false;
    }
}
