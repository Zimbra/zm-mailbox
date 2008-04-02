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

import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.CalendarCollection;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.mailbox.MailItem;

public class MkCalendar extends DavMethod {
	public static final String MKCALENDAR = "MKCALENDAR";
	public String getName() {
		return MKCALENDAR;
	}

	// valid return codes:
	// 201 Created, 207 Multi-Status (403, 409, 423, 424, 507),
	// 403 Forbidden, 409 Conflict, 415 Unsupported Media Type,
	// 507 Insufficient Storage
	public void handle(DavContext ctxt) throws DavException, IOException {
		String user = ctxt.getUser();
		String name = ctxt.getItem();
		
		if (user == null || name == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_FORBIDDEN, null);
		Element top = null;
		if (ctxt.hasRequestMessage()) {
			Document doc = ctxt.getRequestMessage();
			top = doc.getRootElement();
			if (!top.getName().equals(DavElements.P_MKCALENDAR))
				throw new DavException("msg "+top.getName()+" not allowed in MKCALENDAR", HttpServletResponse.SC_BAD_REQUEST, null);
		}
		
		Collection col = UrlNamespace.getCollectionAtUrl(ctxt, ctxt.getPath());
		if (col instanceof CalendarCollection)
			throw new DavException("can't create calendar under another calendar", HttpServletResponse.SC_FORBIDDEN, null);
		
		Collection newone = col.mkCol(ctxt, name, MailItem.TYPE_APPOINTMENT);
		boolean success = false;
		try {
			PropPatch.handlePropertyUpdate(ctxt, top, newone);
			success = true;
		} finally {
			if (!success)
				newone.delete(ctxt);
		}
		ctxt.setStatus(HttpServletResponse.SC_CREATED);
		ctxt.getResponse().addHeader(DavProtocol.HEADER_CACHE_CONTROL, DavProtocol.NO_CACHE);
	}
	
	public void checkPrecondition(DavContext ctxt) throws DavException {
		// DAV:resource-must-be-null
		// CALDAV:calendar-collection-location-ok
		// CALDAV:valid-calendar-data
		// DAV:need-privilege
	}
	
	public void checkPostcondition(DavContext ctxt) throws DavException {
		// DAV:initialize-calendar-collection
	}
}
