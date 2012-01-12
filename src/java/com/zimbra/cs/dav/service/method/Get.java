/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

public class Get extends DavMethod {
	public static final String GET  = "GET";
	public String getName() {
		return GET;
	}
	protected boolean returnContent() {
		return true;
	}
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
		if (resource.hasEtag())
			ctxt.getResponse().setHeader(DavProtocol.HEADER_ETAG, resource.getEtag());
		
		// in some cases getContentLength() returns an estimate, and the exact 
		// content length is not known until DavResource.getContent() is called.
		// the estimate is good enough for PROPFIND, but not when returning 
		// the contents.  just leave off setting content length explicitly, 
		// and have the servlet deal with it by doing chunking or 
		// setting content-length header on its own.
		
		//resp.setContentLength(resource.getContentLength());
		if (!returnContent() || !resource.hasContent(ctxt))
			return;
		resp.setHeader("Content-Disposition", "attachment");
		if (ZimbraLog.dav.isDebugEnabled()) {
			ZimbraLog.dav.debug("GET "+ctxt.getUri());
			if (contentType != null && contentType.startsWith("text"))
				ZimbraLog.dav.debug(new String(ByteUtil.getContent(resource.getContent(ctxt), 0), "UTF-8"));
		}
		ByteUtil.copy(resource.getContent(ctxt), true, ctxt.getResponse().getOutputStream(), false);
	}
}
