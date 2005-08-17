/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetAllAdminAccounts extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

        List accounts = prov.getAllAdminAccounts();

        Element response = lc.createElement(AdminService.GET_ALL_ADMIN_ACCOUNTS_RESPONSE);
        for (Iterator it=accounts.iterator(); it.hasNext(); ) {
            GetAccount.doAccount(response, (Account) it.next());
        }
	    return response;
	}
}