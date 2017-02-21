/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.servlet.ZimbraServlet;

@SuppressWarnings("serial")
public class DavWellKnownServlet extends ZimbraServlet {
    
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        ZimbraLog.addUserAgentToContext(req.getHeader(DavProtocol.HEADER_USER_AGENT));
        String path = req.getPathInfo();
        if (path.equalsIgnoreCase("/caldav") || path.equalsIgnoreCase("/carddav")) {
            resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            resp.setHeader("Location", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + DavServlet.DAV_PATH);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }    
    
}
