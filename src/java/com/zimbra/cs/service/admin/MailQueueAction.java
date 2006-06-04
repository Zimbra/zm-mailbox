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

import org.apache.lucene.search.Query;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.rmgmt.RemoteMailQueue;
import com.zimbra.cs.rmgmt.RemoteMailQueue.QueueAction;
import com.zimbra.cs.rmgmt.RemoteMailQueue.QueueAttr;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class MailQueueAction extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element serverElem = request.getElement(AdminService.E_SERVER);
        String serverName = serverElem.getAttribute(AdminService.A_NAME);
        
        Server server = prov.get(ServerBy.name, serverName);
        if (server == null) {
            throw ServiceException.INVALID_REQUEST("server with name " + serverName + " could not be found", null);
        }
        
        Element queueElem = serverElem.getElement(AdminService.E_QUEUE);
        String queueName = queueElem.getAttribute(AdminService.A_NAME);

        RemoteMailQueue rmq = RemoteMailQueue.getRemoteMailQueue(server, queueName, false);

        Element actionElem = queueElem.getElement(AdminService.E_ACTION);
        String op = actionElem.getAttribute(AdminService.A_OP);
        QueueAction action = QueueAction.valueOf(op);
        if (action == null) {
        	throw ServiceException.INVALID_REQUEST("bad " + AdminService.A_OP + ":" + op, null);
        }
        String by = actionElem.getAttribute(AdminService.A_BY);
        String[] ids;
        if (by.equals(AdminService.BY_ID)) {
            String idText = actionElem.getText();
            ids = actionElem.getText().split(",");
        } else if (by.equals(AdminService.BY_QUERY)) {
        	Element queryElem = actionElem.getElement(AdminService.E_QUERY);
        	Query query = GetMailQueue.buildLuceneQuery(queryElem);
            RemoteMailQueue.SearchResult sr = rmq.search(query, 0, 0);
            ids = new String[sr.qitems.size()];
            int i = 0;
            for (Map<QueueAttr,String> qitem : sr.qitems) {
            	ids[i++] = qitem.get(QueueAttr.id); 
            }
        } else {
        	throw ServiceException.INVALID_REQUEST("bad " + AdminService.A_BY + ": " + by, null);
        }

        rmq.action(server, action, ids);
        
        Element response = lc.createElement(AdminService.MAIL_QUEUE_ACTION_RESPONSE);
	    return response;
	}

}
