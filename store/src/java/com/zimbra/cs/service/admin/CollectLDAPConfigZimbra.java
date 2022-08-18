/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.mail.Part;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.cs.servlet.ZimbraServlet;

public class CollectLDAPConfigZimbra extends ZimbraServlet {
	private static final String DOWNLOAD_CONTENT_TYPE = "application/x-compressed";
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		try {
			//check the auth token
			AuthToken authToken = getAdminAuthTokenFromCookie(req, resp);
            if (authToken == null) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if(!authToken.isAdmin()) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
			//find the LDAP master
			Provisioning prov = Provisioning.getInstance();
			String ldapHost = LC.ldap_host.value();
			if(ldapHost == null) {
				throw ServiceException.INVALID_REQUEST("Cannot find value for ldap_host in local config", null);
			}
			Server server = prov.get(Key.ServerBy.name, ldapHost);
			if (server == null) {
				throw ServiceException.INVALID_REQUEST("Cannot find server record for LDAP master host: " + ldapHost, null);
			}
			//call RemoteManager
			RemoteManager rmgr = RemoteManager.getRemoteManager(server);
			RemoteResult rr = rmgr.execute(RemoteCommands.COLLECT_LDAP_ZIMBRA);
			//stream the data
			resp.setContentType(DOWNLOAD_CONTENT_TYPE);
            ContentDisposition cd = new ContentDisposition(Part.INLINE).setParameter("filename", ldapHost+".ldif.gz");
            resp.addHeader("Content-Disposition", cd.toString());
			ByteUtil.copy(new ByteArrayInputStream(rr.getMStdout()), true, resp.getOutputStream(), false);
		} catch (ServiceException e) {
			returnError(resp, e);
        	return;
		}

	}

	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
}
