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
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class RenameAccount extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
        String newName = request.getAttribute(AdminService.E_NEW_NAME);

	    Account account = prov.getAccountById(id);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        String oldName = account.getName();

        prov.renameAccount(id, newName);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "RenameAccount","name", oldName, "newName", newName})); 
        
        // get again with new name...

        account = prov.getAccountById(id);
        if (account == null)
            throw ServiceException.FAILURE("unabled to get renamed account: "+id, null);
	    Element response = lc.createElement(AdminService.RENAME_ACCOUNT_RESPONSE);
	    GetAccount.doAccount(response, account);
	    return response;
	}

}