/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class SetPassword extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
        String newPassword = request.getAttribute(AdminService.E_NEW_PASSWORD);

	    Account account = prov.getAccountById(id);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        prov.setPassword(account, newPassword);
        
        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "SetPassword","name", account.getName()}));

	    Element response = lc.createElement(AdminService.SET_PASSWORD_RESPONSE);
	    return response;
	}

}