/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class CheckHostnameResolve extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String host = request.getAttribute(AdminService.E_HOSTNAME).toLowerCase();

        Check.Result r = Check.checkHostnameResolve(host);

	    Element response = lc.createElement(AdminService.CHECK_HOSTNAME_RESOLVE_RESPONSE);
        response.addElement(AdminService.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminService.E_MESSAGE).addText(message);
	    return response;
	}
}