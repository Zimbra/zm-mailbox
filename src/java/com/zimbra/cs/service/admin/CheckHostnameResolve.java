/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.ldap.Check;
import com.liquidsys.soap.LiquidContext;

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