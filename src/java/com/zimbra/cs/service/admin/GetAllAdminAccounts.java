/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetAllAdminAccounts extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();

        List accounts = prov.getAllAdminAccounts();

        Element response = lc.createElement(AdminService.GET_ALL_ADMIN_ACCOUNTS_RESPONSE);
        for (Iterator it=accounts.iterator(); it.hasNext(); ) {
            GetAccount.doAccount(response, (Account) it.next());
        }
	    return response;
	}
}