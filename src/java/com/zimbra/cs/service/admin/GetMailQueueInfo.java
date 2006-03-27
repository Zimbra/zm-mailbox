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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class GetMailQueueInfo extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

		ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    Element serverElem = request.getElement(AdminService.E_SERVER);
	    String serverName = serverElem.getAttribute(AdminService.A_NAME);
	    
	    Server server = prov.getServerByName(serverName);
	    if (server == null) {
	    	throw ServiceException.INVALID_REQUEST("server with name " + serverName + " could not be found", null);
	    }
	    
        RemoteManager rmgr = RemoteManager.getRemoteManager(server);
        Map<String,String> queueInfo = rmgr.executeWithSimpleMapResult(RemoteCommands.ZMQSTAT_ALL);
        if (queueInfo == null) {
            throw ServiceException.FAILURE("server " + serverName + " returned no result", null);
        }

        Element response = lc.createElement(AdminService.GET_MAIL_QUEUE_INFO_RESPONSE);
        serverElem = response.addElement(AdminService.E_SERVER);
        serverElem.addAttribute(AdminService.A_NAME, serverName);
        for (String k : queueInfo.keySet()) {
            Element queue = serverElem.addElement(AdminService.E_QUEUE);
            queue.addAttribute(AdminService.A_NAME, k);
            queue.addAttribute(AdminService.A_N, queueInfo.get(k));
        }
        return response;
	}

}
