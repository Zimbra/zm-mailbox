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
public class ModifyAccount extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
	    Map attrs = AdminService.getAttrs(request);

	    Account account = prov.getAccountById(id);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        // pass in true to checkImmutable
        account.modifyAttrs(attrs, true);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "ModifyAccount","name", account.getName()}, attrs));

        Element response = lc.createElement(AdminService.MODIFY_ACCOUNT_RESPONSE);
        GetAccount.doAccount(response, account);
	    return response;
	}
}