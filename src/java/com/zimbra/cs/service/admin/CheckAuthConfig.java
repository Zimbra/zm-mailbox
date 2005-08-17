/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.ldap.Check;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class CheckAuthConfig extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);

	    String name = request.getAttribute(AdminService.E_NAME).toLowerCase();
	    String password = request.getAttribute(AdminService.E_PASSWORD);
	    Map attrs = AdminService.getAttrs(request, true);


        Element response = lc.createElement(AdminService.CHECK_AUTH_CONFIG_RESPONSE);
        Check.Result r = Check.checkAuthConfig(attrs, name, password);
        
        response.addElement(AdminService.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminService.E_MESSAGE).addText(message);
        response.addElement(AdminService.E_BINDDN).addText(r.getComputedDn());

	    return response;
	}
}