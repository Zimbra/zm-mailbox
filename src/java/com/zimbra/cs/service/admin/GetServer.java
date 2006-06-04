/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetServer extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

        boolean applyConfig = request.getAttributeBool(AdminService.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminService.E_SERVER);
	    String method = d.getAttribute(AdminService.A_BY);
        String name = d.getText();

        if (name == null || name.equals(""))
            throw ServiceException.INVALID_REQUEST("must specify a value for a server", null);
        
        Server server = prov.get(ServerBy.fromString(method), name);
        
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(name);
        else
            prov.reload(server);
        
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
        Map<String, Object> attrs = s.getAttrs(applyConfig);
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    server.addElement(AdminService.E_A).addAttribute(AdminService.A_N, name).setText(sv[i]);
            } else if (value instanceof String)
                server.addElement(AdminService.E_A).addAttribute(AdminService.A_N, name).setText((String) value);
        }
    }
}
