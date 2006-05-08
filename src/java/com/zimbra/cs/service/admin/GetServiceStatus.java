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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbServiceStatus;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetServiceStatus extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws SoapFaultException, ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        // this command can only execute on the monitor host, so proxy if necessary
        Provisioning prov = Provisioning.getInstance();
        String monitorHost = prov.getConfig().getAttr(Provisioning.A_zimbraLogHostname);
        if (monitorHost == null || monitorHost.trim().equals(""))
            throw ServiceException.FAILURE("zimbraLogHostname is not configured", null);
        Server monitorServer = prov.getServerByName(monitorHost);
        if (monitorServer == null)
            throw ServiceException.FAILURE("could not find zimbraLogHostname server: " + monitorServer, null);
        if (!prov.getLocalServer().getId().equalsIgnoreCase(monitorServer.getId()))
            return proxyRequest(request, context, monitorServer, new ZimbraSoapContext(lc, lc.getRequestedAccountId()));

        Element response = lc.createElement(AdminService.GET_SERVICE_STATUS_RESPONSE);
        boolean loggerEnabled = false;
        Server local = prov.getLocalServer();
        String[] services = local.getMultiAttr(Provisioning.A_zimbraServiceEnabled);
        if (services != null) {
            for (int i = 0; i < services.length; i++) {
                if ("logger".equals(services[i])) {
                    loggerEnabled = true;
                    break;
                }
            }
        }
        if (loggerEnabled) {
    	    Connection conn = null;
            try { 
                conn = DbPool.getLoggerConnection();
                List stats = DbServiceStatus.getStatus(conn.getConnection());
                doServiceStatus(response, stats);            
            } finally {
                DbPool.quietClose(conn);
            }
        }
	    return response;
	}

	// <status server="..." service="..." t="...">{status}<status/>
    public static void doServiceStatus(Element e, List stats) {
        for (Iterator it = stats.iterator(); it.hasNext(); ) {
            DbServiceStatus stat = (DbServiceStatus) it.next();
            Element s = e.addElement(AdminService.E_STATUS);
            s.addAttribute(AdminService.A_SERVER, stat.getServer());
            s.addAttribute(AdminService.A_SERVICE, stat.getService());
            s.addAttribute(AdminService.A_T, stat.getTime());
            s.setText(Integer.toString(stat.getStatus()));
        }
    }
}
