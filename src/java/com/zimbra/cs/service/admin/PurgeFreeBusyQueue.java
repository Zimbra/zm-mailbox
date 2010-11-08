/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.fb.FreeBusyProvider.FreeBusySyncQueue;
import com.zimbra.soap.ZimbraSoapContext;

public class PurgeFreeBusyQueue extends AdminDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // allow only system admin for now
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        
        String name = null;
        Element provider = request.getOptionalElement(AdminConstants.E_PROVIDER);
        if (provider != null)
            name = provider.getAttribute(AdminConstants.A_NAME);
        
        Element response = zsc.createElement(AdminConstants.PURGE_FREE_BUSY_QUEUE_RESPONSE);
        if (name != null) {
            FreeBusyProvider prov = FreeBusyProvider.getProvider(name);
            if (prov == null)
                throw ServiceException.INVALID_REQUEST("provider not found: "+name, null);
            handleProvider(prov);
        } else {
            for (FreeBusyProvider prov : FreeBusyProvider.getProviders()) {
                handleProvider(prov);
            }
        }
        return response;
    }
    private void handleProvider(FreeBusyProvider prov) {
        FreeBusySyncQueue queue = prov.getSyncQueue();
        if (queue == null)
            return;
        synchronized (queue) {
            queue.clear();
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
