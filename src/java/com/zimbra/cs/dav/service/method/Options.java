/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavServlet;

public class Options extends DavMethod {
	public static final String OPTIONS  = "OPTIONS";
	public String getName() {
		return OPTIONS;
	}
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		HttpServletResponse resp = ctxt.getResponse();
		DavServlet.setAllowHeader(resp);
		if (ctxt.isMsft())
		    resp.addHeader(DavProtocol.HEADER_MS_AUTHOR_VIA, "DAV");
		resp.setContentLength(0);
		try {
	        DavResource rs = ctxt.getRequestedResource();
	        ctxt.setDavCompliance(DavProtocol.getComplianceString(rs.getComplianceList()));
		} catch (Exception e) {}
		sendResponse(ctxt);
	}
}
