/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class CreateServer extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
	    
        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    String name = request.getAttribute(AdminService.E_NAME).toLowerCase();
	    Map attrs = AdminService.getAttrs(request, true);
	    
	    Server server = prov.createServer(name, attrs);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "CreateServer","name", name}, attrs));

	    Element response = lc.createElement(AdminService.CREATE_SERVER_RESPONSE);
	    GetServer.doServer(response, server);

	    return response;
	}

}