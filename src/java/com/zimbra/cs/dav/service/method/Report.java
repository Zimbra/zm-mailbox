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
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.service.DavMethod;

public class Report extends DavMethod {
	public static final String REPORT = "REPORT";
	public String getName() {
		return REPORT;
	}

	private static HashMap<QName,DavMethod> sReports;
	
	static {
		sReports = new HashMap<QName,DavMethod>();
		sReports.put(DavElements.E_CALENDAR_QUERY, new CalendarQuery());
		sReports.put(DavElements.E_CALENDAR_MULTIGET, new CalendarMultiget());
		sReports.put(DavElements.E_FREE_BUSY_QUERY, new FreeBusyQuery());
	}
	public void handle(DavContext ctxt) throws DavException, IOException {
		if (!ctxt.hasRequestMessage())
			throw new DavException("empty request body", HttpServletResponse.SC_BAD_REQUEST, null);
		
		Document req = ctxt.getRequestMessage();
		Element top = req.getRootElement();
		if (top == null)
			throw new DavException("empty request body", HttpServletResponse.SC_BAD_REQUEST, null);
		QName topName = top.getQName();
		DavMethod report = sReports.get(topName);
		if (report == null)
			throw new DavException("msg "+top.getName()+" not implemented in REPORT", HttpServletResponse.SC_BAD_REQUEST, null);
		
		report.handle(ctxt);
		sendResponse(ctxt);
	}
}
