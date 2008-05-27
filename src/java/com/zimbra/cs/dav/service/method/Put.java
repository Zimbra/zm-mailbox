/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;

public class Put extends DavMethod {
	public static final String PUT  = "PUT";
	public String getName() {
		return PUT;
	}
	public void handle(DavContext ctxt) throws DavException, IOException {
		String user = ctxt.getUser();
		String name = ctxt.getItem();
		
		if (user == null || name == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_ACCEPTABLE, null);
		
		Collection col = UrlNamespace.getCollectionAtUrl(ctxt, ctxt.getPath());
		DavResource rs = col.createItem(ctxt, name);
		if (rs.isNewlyCreated())
			ctxt.setStatus(HttpServletResponse.SC_CREATED);
		else
			ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
		if (rs.hasEtag())
			ctxt.getResponse().setHeader(DavProtocol.HEADER_ETAG, rs.getEtag());
	}
}
