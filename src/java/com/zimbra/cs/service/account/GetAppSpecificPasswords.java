package com.zimbra.cs.service.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AppSpecificPassword.PasswordData;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.AppSpecificPasswordData;
import com.zimbra.soap.account.message.GetAppSpecificPasswordsResponse;

public class GetAppSpecificPasswords extends AccountDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        GetAppSpecificPasswordsResponse response = new GetAppSpecificPasswordsResponse();
        TwoFactorManager manager = new TwoFactorManager(account);
        Set<PasswordData> names = manager.getAppSpecificPasswords();
        encodeResponse(account, response, names);
        return zsc.jaxbToElement(response);
	}

	private void encodeResponse(Account acccount, GetAppSpecificPasswordsResponse response, Set<PasswordData> appPasswords) {
		for (PasswordData passwordData: appPasswords) {
		    AppSpecificPasswordData password = new AppSpecificPasswordData();
		    password.setAppName(passwordData.getName());
		    password.setDateCreated(passwordData.getDateCreated());
		    password.setDateLastUsed(passwordData.getDateLastUsed());
		    response.addAppSpecificPassword(password);
		}
		response.setMaxAppPasswords(acccount.getMaxAppSpecificPasswords());
	}
}
