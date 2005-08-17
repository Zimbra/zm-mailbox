/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class CreateAccount extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String name = request.getAttribute(AdminService.E_NAME).toLowerCase();
	    String password = request.getAttribute(AdminService.E_PASSWORD, null);
	    Map attrs = AdminService.getAttrs(request, true);

	    Account account = prov.createAccount(name, password, attrs);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "CreateAccount","name", name}, attrs));         

	    Element response = lc.createElement(AdminService.CREATE_ACCOUNT_RESPONSE);

	    GetAccount.doAccount(response, account);

	    return response;
	}
}