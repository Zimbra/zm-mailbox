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
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class ModifyServer extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
	    Map attrs = AdminService.getAttrs(request);
	    
	    Server server = prov.getServerById(id);
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(id);

        // pass in true to checkImmutable
        server.modifyAttrs(attrs, true);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyServer","name", server.getName()}, attrs));

        // If updating user service enable flag on local server, we have to 
        // tell Config class about it.
        if (attrs.containsKey(Provisioning.A_zimbraUserServicesEnabled)) {
            Server localServer = Provisioning.getInstance().getLocalServer();
            if (server.equals(localServer)) {
                boolean b = server.getBooleanAttr(Provisioning.A_zimbraUserServicesEnabled, true);
                Config.enableUserServices(b);
            }
        }

	    Element response = lc.createElement(AdminService.MODIFY_SERVER_RESPONSE);
	    GetServer.doServer(response, server);
	    return response;
	}
}
