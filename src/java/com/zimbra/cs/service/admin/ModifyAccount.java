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
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class ModifyAccount extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
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