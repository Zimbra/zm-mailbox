/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class CreateAccount extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
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