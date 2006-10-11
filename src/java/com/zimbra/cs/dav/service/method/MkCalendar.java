/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.service.DavMethod;

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
