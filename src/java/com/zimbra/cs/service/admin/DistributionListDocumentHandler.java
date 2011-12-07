/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class DistributionListDocumentHandler extends AdminDocumentHandler {

    private static final String GROUP = "__GROUP__";
    
    abstract protected Group getGroup(Element request) throws ServiceException;
    
    private Group getGroupAndCacheInContext(Element request, Map<String, Object> context) 
    throws ServiceException {
        Group group = getGroup(request);
        if (group != null) {
            context.put(GROUP, group);
        }
        return group;
    }
    
    protected Group getGroupFromContext(Map<String, Object> context) throws ServiceException {
        return (Group) context.get(GROUP);
    }
    
    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        // if we've explicitly been told to execute here, don't proxy
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (zsc.getProxyTarget() != null) {
            return null;
        }
        
        // check whether we need to proxy to the home server of a group
        try {
            Group group = getGroupAndCacheInContext(request, context);
            
            if (group != null && !Provisioning.onLocalServer(group)) {
                Server server = group.getServer();
                if (server == null) {
                    throw ServiceException.PROXY_ERROR(
                            AccountServiceException.NO_SUCH_SERVER(
                            group.getAttr(Provisioning.A_zimbraMailHost)), "");
                }
                return proxyRequest(request, context, server);
            }
            
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                return null;
            }
            // but if it's a real error, it's a real error
            throw e;
        }
    }
}
