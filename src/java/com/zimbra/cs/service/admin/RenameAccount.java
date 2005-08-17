/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;
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