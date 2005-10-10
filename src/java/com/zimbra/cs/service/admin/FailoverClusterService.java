/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

import com.zimbra.cs.util.ClusterUtil;

public class FailoverClusterService extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        Element serviceEl = request.getElement(AdminService.A_CLUSTER_SERVICE);
        String serviceName = serviceEl.getAttribute(AdminService.A_CLUSTER_SERVICE_NAME);
        String newServer = serviceEl.getAttribute(AdminService.A_FAILOVER_NEW_SERVER);
        System.out.println("**** failover request for " + serviceName + " to go to " + newServer);

        ClusterUtil.failoverService(serviceName, newServer);
        
        Element response = lc.createElement(AdminService.FAILOVER_CLUSTER_SERVICE_RESPONSE);
        return response;
    }

}


