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

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.resource.CalendarCollection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;

/*
 * draft-dusseault-caldav section 9.11
 * 
 *     <!ELEMENT free-busy-query (time-range)>
 *                                
 */
public class FreeBusyQuery extends Report {
	public void handle(DavContext ctxt) throws DavException, IOException {
		Element query = ctxt.getRequestMessage().getRootElement();
		if (!query.getQName().equals(DavElements.E_FREE_BUSY_QUERY))
			throw new DavException("msg "+query.getName()+" is not free-busy-query", HttpServletResponse.SC_BAD_REQUEST, null);

		Element trElem = query.element(DavElements.E_TIME_RANGE);
		if (trElem == null)
			throw new DavException("need time-range", HttpServletResponse.SC_BAD_REQUEST, null);
			
		TimeRange timeRange = new TimeRange(trElem);
		DavResource rs = UrlNamespace.getResource(ctxt);
		
		if (!(rs instanceof CalendarCollection))
			throw new DavException("not a calendar collection", HttpServletResponse.SC_BAD_REQUEST, null);

		try {
			String freebusy = ((CalendarCollection)rs).getFreeBusyReport(ctxt, timeRange);
			HttpServletResponse resp = ctxt.getResponse();
			resp.setContentType(Mime.CT_TEXT_CALENDAR);
			resp.getOutputStream().write(freebusy.getBytes());
			ctxt.responseSent();
		} catch (ServiceException se) {
			throw new DavException("can't get freebusy report", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}
}
