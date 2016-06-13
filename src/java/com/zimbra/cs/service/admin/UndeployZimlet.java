/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.cs.zimlet.ZimletUtil.ZimletSoapUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class UndeployZimlet extends AdminDocumentHandler {

	private static class UndeployThread implements Runnable {
	    final Server server;
		String name;
		ZAuthToken auth;

		public UndeployThread(Server s, String na, ZAuthToken au) {
		    server = s;
			name = na;
			auth = au;
		}

		@Override
        public void run() {
			try {
                ZimletSoapUtil soapUtil = new ZimletSoapUtil(auth);
                soapUtil.undeployZimletRemotely(server, name);
			} catch (Exception e) {
				ZimbraLog.zimlet.info("undeploy", e);
			}
		}
	}

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    String name = request.getAttribute(AdminConstants.A_NAME);
		String action = request.getAttribute(AdminConstants.A_ACTION, null);
		ZAuthToken auth = null;
		if (action == null) {
            auth = zsc.getRawAuthToken();
        }

	    Element response = zsc.createElement(AdminConstants.UNDEPLOY_ZIMLET_RESPONSE);
        //undeploy on local server and LDAP anyway
        ZimletUtil.undeployZimletLocally(name);
        if (AdminConstants.A_DEPLOYALL.equals(action)) {
	        //undeploy on remote servers
	        for (Server server : Provisioning.getInstance().getAllServers()) {
	            if (!server.isLocalServer()) {
	                checkRight(zsc, context, server, Admin.R_deployZimlet);
                    new Thread(new UndeployThread(server, name, auth)).start();
	            }
	        }
	    }

		return response;
	}

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_deployZimlet);
        notes.add("Need the " + Admin.R_deployZimlet.getName() + " right on all servers.");
    }

}
