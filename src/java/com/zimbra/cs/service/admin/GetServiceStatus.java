/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbServiceStatus;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetServiceStatus extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws SoapFaultException, ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // this command can only execute on the monitor host, so proxy if necessary
        Provisioning prov = Provisioning.getInstance();
        String monitorHost = prov.getConfig().getAttr(Provisioning.A_zimbraLogHostname);
        if (monitorHost == null || monitorHost.trim().equals(""))
            throw ServiceException.FAILURE("zimbraLogHostname is not configured", null);
        Server monitorServer = prov.get(ServerBy.name, monitorHost);
        if (monitorServer == null)
            throw ServiceException.FAILURE("could not find zimbraLogHostname server: " + monitorServer, null);
        if (!prov.getLocalServer().getId().equalsIgnoreCase(monitorServer.getId()))
            return proxyRequest(request, context, monitorServer, zsc);

        Element response = zsc.createElement(AdminConstants.GET_SERVICE_STATUS_RESPONSE);
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
            Element s = e.addElement(AdminConstants.E_STATUS);
            s.addAttribute(AdminConstants.A_SERVER, stat.getServer());
            s.addAttribute(AdminConstants.A_SERVICE, stat.getService());
            s.addAttribute(AdminConstants.A_T, stat.getTime());
            s.setText(Integer.toString(stat.getStatus()));
        }
    }
}
