/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.servlet.ETagHeaderFilter;

public class Get extends DavMethod {
    public static final String GET = "GET";

    @Override
    public String getName() {
        return GET;
    }

    protected boolean returnContent() {
        return true;
    }

    @Override
    public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
        DavResource resource = ctxt.getRequestedResource();
        HttpServletResponse resp = ctxt.getResponse();
        String contentType = resource.getContentType(ctxt);
        if (contentType != null) {
            ContentType ct = new ContentType(contentType);
            if (ct.getParameter(MimeConstants.P_CHARSET) == null)
                ct.setParameter(MimeConstants.P_CHARSET, MimeConstants.P_CHARSET_UTF8);
            resp.setContentType(ct.toString());
        }
        if (resource.hasEtag()) {
            ctxt.getResponse().setHeader(DavProtocol.HEADER_ETAG, resource.getEtag());
            ctxt.getResponse().setHeader(ETagHeaderFilter.ZIMBRA_ETAG_HEADER, resource.getEtag());
        }

        // in some cases getContentLength() returns an estimate, and the exact
        // content length is not known until DavResource.getContent() is called.
        // the estimate is good enough for PROPFIND, but not when returning
        // the contents. just leave off setting content length explicitly,
        // and have the servlet deal with it by doing chunking or
        // setting content-length header on its own.

        // resp.setContentLength(resource.getContentLength());
        if (!returnContent() || !resource.hasContent(ctxt))
            return;
        resp.setHeader("Content-Disposition", "attachment");
        ByteUtil.copy(resource.getContent(ctxt), true, ctxt.getResponse().getOutputStream(), false);
        resp.setStatus(ctxt.getStatus());
        ctxt.responseSent();
        if (ZimbraLog.dav.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Response for DAV GET ").append(ctxt.getUri()).append("\n");
            if (contentType != null && contentType.startsWith("text")) {
                DavServlet.addResponseHeaderLoggingInfo(resp, sb);
                if (ZimbraLog.dav.isTraceEnabled()) {
                    sb.append(new String(ByteUtil.getContent(resource.getContent(ctxt), 0), "UTF-8"));
                }
                ZimbraLog.dav.debug(sb);
            }
        }
    }
}
