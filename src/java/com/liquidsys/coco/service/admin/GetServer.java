/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetServer extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_SERVICE_HOSTNAME = "serviceHostname";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {
	    
        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();
        
        boolean applyConfig = request.getAttributeBool(AdminService.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminService.E_SERVER);
	    String key = d.getAttribute(AdminService.A_BY);
        String value = d.getText();
	    
	    Server server = null;
        
        if (value == null || value.equals(""))
            throw ServiceException.INVALID_REQUEST("must specify a value for a server", null);

        if (key.equals(BY_NAME)) {
            server = prov.getServerByName(value, true);
        } else if (key.equals(BY_ID)) {
            server = prov.getServerById(value, true);
        } else if (key.equals(BY_SERVICE_HOSTNAME)) {
            List servers = prov.getAllServers();
            for (Iterator it = servers.iterator(); it.hasNext(); ) {
                Server s = (Server) it.next();
                // when replication is enabled, should return server representing current master
                if (value.equalsIgnoreCase(s.getAttr(Provisioning.A_liquidServiceHostname, ""))) {
                    server = s;
                    break;
                }
            }
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(value);
        
	    Element response = lc.createElement(AdminService.GET_SERVER_RESPONSE);
        doServer(response, server, applyConfig);

	    return response;
	}

    public static void doServer(Element e, Server s) throws ServiceException {
        doServer(e, s, true);
    }

    public static void doServer(Element e, Server s, boolean applyConfig) throws ServiceException {
        Element server = e.addElement(AdminService.E_SERVER);
        server.addAttribute(AdminService.A_NAME, s.getName());
        server.addAttribute(AdminService.A_ID, s.getId());
        Map attrs = s.getAttrs(applyConfig);
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    server.addAttribute(name, sv[i], Element.DISP_ELEMENT);
            } else if (value instanceof String)
                server.addAttribute(name, (String) value, Element.DISP_ELEMENT);
        }
    }
}
