/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.Range.TimeRange;
import com.zimbra.cs.dav.resource.CalendarCollection;
import com.zimbra.cs.dav.resource.DavResource;

/*
 * draft-dusseault-caldav section 9.11
 * 
 *     <!ELEMENT free-busy-query (time-range)>
 *                                
 */
public class FreeBusyQuery extends Report {
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		Element query = ctxt.getRequestMessage().getRootElement();
		if (!query.getQName().equals(DavElements.E_FREE_BUSY_QUERY))
			throw new DavException("msg "+query.getName()+" is not free-busy-query", HttpServletResponse.SC_BAD_REQUEST, null);

		Element trElem = query.element(DavElements.E_TIME_RANGE);
		if (trElem == null)
			throw new DavException("need time-range", HttpServletResponse.SC_BAD_REQUEST, null);
			
		TimeRange timeRange = new TimeRange(trElem);
		DavResource rs = ctxt.getRequestedResource();
		
		if (!(rs instanceof CalendarCollection))
			throw new DavException("not a calendar collection", HttpServletResponse.SC_BAD_REQUEST, null);

		try {
			String freebusy = ((CalendarCollection)rs).getFreeBusyReport(ctxt, timeRange);
			HttpServletResponse resp = ctxt.getResponse();
            resp.setContentType(MimeConstants.CT_TEXT_CALENDAR);
			resp.getOutputStream().write(freebusy.getBytes("UTF-8"));
			ctxt.responseSent();
		} catch (ServiceException se) {
			throw new DavException("can't get freebusy report", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}
}
