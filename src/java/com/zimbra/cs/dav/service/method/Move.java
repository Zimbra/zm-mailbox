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
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;

public class Move extends DavMethod {
	public static final String MOVE  = "MOVE";
	public String getName() {
		return MOVE;
	}
	
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		String destination = ctxt.getRequest().getHeader(DavProtocol.HEADER_DESTINATION);
		if (destination == null)
			throw new DavException("no destination specified", HttpServletResponse.SC_BAD_REQUEST, null);
		DavResource rs = ctxt.getRequestedResource();
		Collection col = UrlNamespace.getCollectionAtUrl(ctxt, destination);
		if (!(col instanceof MailItemResource))
			throw new DavException("cannot move", HttpServletResponse.SC_BAD_REQUEST, null);
		MailItemResource mir = (MailItemResource) rs;
		mir.move(ctxt, col);

		renameIfNecessary(ctxt, mir, destination, col);
		ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
	
	protected void renameIfNecessary(DavContext ctxt, MailItemResource rs, String dest, MailItemResource destCollection) throws DavException {
		String oldName = ctxt.getItem();
		int begin, end;
		end = dest.length();
		if (dest.endsWith("/"))
			end--;
		begin = dest.lastIndexOf("/", end-1);
		String newName = dest.substring(begin+1, end);
		if (!oldName.equals(newName))
			rs.rename(ctxt, newName, destCollection);
	}
}
