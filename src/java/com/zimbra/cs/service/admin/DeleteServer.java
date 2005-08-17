/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class DeleteServer extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
	    Map attrs = AdminService.getAttrs(request);
	    
	    Server server = prov.getServerById(id);
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(id);
        
        prov.deleteServer(server.getId());
        
        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "DeleteServer","name", server.getName(), "id", server.getId()}));

	    Element response = lc.createElement(AdminService.DELETE_SERVER_RESPONSE);
	    return response;
	}
}