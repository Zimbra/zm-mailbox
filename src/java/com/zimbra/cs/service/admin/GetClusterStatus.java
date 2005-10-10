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

public class GetClusterStatus extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        StringBuffer resp = new StringBuffer();
        Map status = ClusterUtil.getClusterStatus();
        
        Element response = lc.createElement(AdminService.GET_CLUSTER_STATUS_RESPONSE);
        if (status != null) {
            ClusterUtil.ServerStatus [] servers = (ClusterUtil.ServerStatus []) status.get(ClusterUtil.SERVERS_KEY);
            ClusterUtil.ServiceStatus [] services = (ClusterUtil.ServiceStatus [])status.get(ClusterUtil.SERVICES_KEY);
            
            addServersToResponse(response, servers);
            addServicesToResponse (response, services);
        }
        return response;
    }
    

    private void addServersToResponse (Element response, ClusterUtil.ServerStatus [] servers) {
        if (servers.length > 0 ){
            Element serversEl = response.addElement(AdminService.A_CLUSTER_SERVERS);
            for (int i = 0; i < servers.length ; ++i) {
                Element s = serversEl.addElement(AdminService.A_CLUSTER_SERVER);
                s.addAttribute(AdminService.A_CLUSTER_SERVER_NAME, servers[i].name);
                s.addAttribute(AdminService.A_CLUSTER_SERVER_STATUS, servers[i].status);
            }
        }
    }
    
    private void addServicesToResponse (Element response, ClusterUtil.ServiceStatus [] services) {
        if (services.length > 0 ) {
            Element servicesEl = response.addElement(AdminService.A_CLUSTER_SERVICES);
            for (int i = 0; i < services.length ; ++i) {
                Element s =servicesEl.addElement(AdminService.A_CLUSTER_SERVICE);
                s.addAttribute(AdminService.A_CLUSTER_SERVICE_NAME, services[i].name);
                s.addAttribute(AdminService.A_CLUSTER_SERVICE_STATUS, services[i].status);
                s.addAttribute(AdminService.A_CLUSTER_SERVICE_OWNER, services[i].owner);
                s.addAttribute(AdminService.A_CLUSTER_SERVICE_LAST_OWNER, services[i].lastOwner);
                s.addAttribute(AdminService.A_CLUSTER_SERVICE_RESTARTS, services[i].restarts);
            }
        }
    }
    
}
