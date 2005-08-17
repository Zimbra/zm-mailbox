/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.coco.account.Domain;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class CreateDomain extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
	    
        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    String name = request.getAttribute(AdminService.E_NAME).toLowerCase();
	    Map attrs = AdminService.getAttrs(request, true);
	    
	    Domain domain = prov.createDomain(name, attrs);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "CreateDomain","name", name}, attrs));         

	    Element response = lc.createElement(AdminService.CREATE_DOMAIN_RESPONSE);
	    GetDomain.doDomain(response, domain);

	    return response;
	}
}