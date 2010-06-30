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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.resource.Notebook;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;

public class Move extends DavMethod {
	public static final String MOVE  = "MOVE";
	public String getName() {
		return MOVE;
	}
	
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		DavResource rs = ctxt.getRequestedResource();
        if (!(rs instanceof MailItemResource))
            throw new DavException("cannot copy", HttpServletResponse.SC_BAD_REQUEST, null);
		Collection col = getDestinationCollection(ctxt);
		MailItemResource mir = (MailItemResource) rs;
		mir.move(ctxt, col);

		renameIfNecessary(ctxt, mir, col);
		ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
	
	protected void renameIfNecessary(DavContext ctxt, DavResource rs, MailItemResource destCollection) throws DavException {
	    if (!(rs instanceof Collection) && !(rs instanceof Notebook))
	        return;
		String oldName = ctxt.getItem();
		String dest = getDestination(ctxt);
		int begin, end;
		end = dest.length();
		if (dest.endsWith("/"))
			end--;
		begin = dest.lastIndexOf("/", end-1);
		String newName = dest.substring(begin+1, end);
        try {
            newName = URLDecoder.decode(newName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.dav.warn("can't decode URL ", dest, e);
        }
		if (!oldName.equals(newName))
			rs.rename(ctxt, newName, destCollection);
	}
	
	protected String getDestination(DavContext ctxt) throws DavException {
        String destination = ctxt.getRequest().getHeader(DavProtocol.HEADER_DESTINATION);
        if (destination == null)
            throw new DavException("no destination specified", HttpServletResponse.SC_BAD_REQUEST, null);
        return destination;
	}
    protected Collection getDestinationCollection(DavContext ctxt) throws DavException {
        String destinationUrl = getDestination(ctxt);
        if (!destinationUrl.endsWith("/")) {
            int slash = destinationUrl.lastIndexOf('/');
            destinationUrl = destinationUrl.substring(0, slash+1);
        }
        try {
            DavResource r = UrlNamespace.getResourceAtUrl(ctxt, destinationUrl);
            if (r instanceof Collection)
                return ((Collection)r);
            return UrlNamespace.getCollectionAtUrl(ctxt, destinationUrl);
        } catch (Exception e) {
            throw new DavException("can't get destination collection", DavProtocol.STATUS_FAILED_DEPENDENCY);
        }
    }
}
