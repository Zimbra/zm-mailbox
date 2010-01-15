/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetServer extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminConstants.E_SERVER);
	    String method = d.getAttribute(AdminConstants.A_BY);
        String name = d.getText();

        if (name == null || name.equals(""))
            throw ServiceException.INVALID_REQUEST("must specify a value for a server", null);
        
        Server server = prov.get(ServerBy.fromString(method), name);
        
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(name);
        else
            prov.reload(server);
        
	    Element response = lc.createElement(AdminConstants.GET_SERVER_RESPONSE);
        doServer(response, server, applyConfig);

	    return response;
	}

    public static void doServer(Element e, Server s) throws ServiceException {
        doServer(e, s, true);
    }

    public static void doServer(Element e, Server s, boolean applyConfig) throws ServiceException {
        Element server = e.addElement(AdminConstants.E_SERVER);
        server.addAttribute(AdminConstants.A_NAME, s.getName());
        server.addAttribute(AdminConstants.A_ID, s.getId());
        Map<String, Object> attrs = s.getUnicodeAttrs(applyConfig);
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    server.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(sv[i]);
            } else if (value instanceof String)
                server.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
        }
    }
}
