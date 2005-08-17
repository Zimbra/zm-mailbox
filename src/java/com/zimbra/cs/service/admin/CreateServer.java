/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;
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