/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetAllServers extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();

        List cos = prov.getAllServers();
        
        Element response = lc.createElement(AdminService.GET_ALL_SERVERS_RESPONSE);        
        for (Iterator it = cos.iterator(); it.hasNext(); )
            GetServer.doServer(response, (Server) it.next());

	    return response;
	}
}
