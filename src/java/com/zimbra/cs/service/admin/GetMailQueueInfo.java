/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.cs.rmgmt.RemoteResultParser;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMailQueueInfo extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    Element serverElem = request.getElement(AdminConstants.E_SERVER);
	    String serverName = serverElem.getAttribute(AdminConstants.A_NAME);
	    
	    Server server = prov.get(ServerBy.name, serverName);
	    if (server == null) {
	    	throw ServiceException.INVALID_REQUEST("server with name " + serverName + " could not be found", null);
	    }
	    
        RemoteManager rmgr = RemoteManager.getRemoteManager(server);
        RemoteResult rr = rmgr.execute(RemoteCommands.ZMQSTAT_ALL);
        Map<String,String> queueInfo;
        try { 
            queueInfo = RemoteResultParser.parseSingleMap(rr);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("exception occurred handling command", ioe);
        }
        if (queueInfo == null) {
            throw ServiceException.FAILURE("server " + serverName + " returned no result", null);
        }

        Element response = lc.createElement(AdminConstants.GET_MAIL_QUEUE_INFO_RESPONSE);
        serverElem = response.addElement(AdminConstants.E_SERVER);
        serverElem.addAttribute(AdminConstants.A_NAME, serverName);
        for (String k : queueInfo.keySet()) {
            Element queue = serverElem.addElement(AdminConstants.E_QUEUE);
            queue.addAttribute(AdminConstants.A_NAME, k);
            queue.addAttribute(AdminConstants.A_N, queueInfo.get(k));
        }
        return response;
	}
}
