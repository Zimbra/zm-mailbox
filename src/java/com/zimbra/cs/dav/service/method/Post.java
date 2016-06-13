/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavServlet;

public class Post extends DavMethod {
    public static final String POST  = "POST";

    @Override
    public String getName() {
        return POST;
    }

    @Override
    public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
        String user = ctxt.getUser();
        String name = ctxt.getItem();

        if (user == null || name == null) {
            throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND);
        }

        DavResource rs = ctxt.getRequestedResource();
        rs.handlePost(ctxt);
        sendResponse(ctxt);
        if (ZimbraLog.dav.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Response for DAV POST ").append(ctxt.getUri()).append("\n");
            DavServlet.addResponseHeaderLoggingInfo(ctxt.getResponse(), sb);
            ZimbraLog.dav.debug(sb);
        }
    }
}
