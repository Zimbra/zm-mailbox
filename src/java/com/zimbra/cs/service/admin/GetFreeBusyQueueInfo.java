/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.fb.FreeBusyProvider.FreeBusySyncQueue;
import com.zimbra.soap.ZimbraSoapContext;

public class GetFreeBusyQueueInfo extends AdminDocumentHandler {
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        String name = null;
        Element provider = request.getOptionalElement(AdminConstants.E_PROVIDER);
        if (provider != null)
        	name = provider.getAttribute(AdminConstants.A_NAME);
        
        Element response = lc.createElement(AdminConstants.GET_FREE_BUSY_QUEUE_INFO_RESPONSE);
        if (name != null) {
        	FreeBusyProvider prov = FreeBusyProvider.getProvider(name);
        	if (prov == null)
        		throw ServiceException.INVALID_REQUEST("provider not found: "+name, null);
        	handleProvider(response, prov);
        } else {
        	for (FreeBusyProvider prov : FreeBusyProvider.getProviders()) {
        		handleProvider(response, prov);
        	}
        }
	    return response;
	}
	private void handleProvider(Element response, FreeBusyProvider prov) {
        Element provider = response.addElement(AdminConstants.E_PROVIDER);
        provider.addAttribute(AdminConstants.A_NAME, prov.getName());
        FreeBusySyncQueue queue = prov.getSyncQueue();
        if (queue == null)
        	return;
        synchronized (queue) {
        	for (String id : queue)
        		provider.addElement(AdminConstants.E_ACCOUNT).addAttribute(AdminConstants.A_ID, id);
        }
	}
}
