/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.Config;
import com.liquidsys.coco.util.LiquidLog;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class ModifyServer extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
	    Map attrs = AdminService.getAttrs(request);
	    
	    Server server = prov.getServerById(id);
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(id);

        // pass in true to checkImmutable
        server.modifyAttrs(attrs, true);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "ModifyServer","name", server.getName()}, attrs));

        // If updating user service enable flag on local server, we have to 
        // tell Config class about it.
        if (attrs.containsKey(Provisioning.A_liquidUserServicesEnabled)) {
            Server localServer = Provisioning.getInstance().getLocalServer();
            if (server.equals(localServer)) {
                boolean b = server.getBooleanAttr(Provisioning.A_liquidUserServicesEnabled, true);
                Config.enableUserServices(b);
            }
        }

	    Element response = lc.createElement(AdminService.MODIFY_SERVER_RESPONSE);
	    GetServer.doServer(response, server);
	    return response;
	}
}
