/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;

public class Delete extends DavMethod {
	public static final String DELETE  = "DELETE";
	public String getName() {
		return DELETE;
	}
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		DavResource rsc = ctxt.getRequestedResource();
		if (rsc == null)
			throw new DavException("cannot find the resource", HttpServletResponse.SC_NOT_FOUND, null);
		rsc.delete(ctxt);
		ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}
