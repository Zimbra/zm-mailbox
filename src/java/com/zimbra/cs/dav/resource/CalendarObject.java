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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.Filter;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;

/**
 * CalendarObject is a single instance of iCalendar (RFC 2445) object, such as
 * VEVENT or VTODO.
 * 
 * @author jylee
 *
 */
public class CalendarObject extends MailItemResource {

	public static final String CAL_EXTENSION = ".ics";
	
	public CalendarObject(DavContext ctxt, CalendarItem calItem) throws ServiceException {
		super(ctxt, getCalendarPath(calItem), calItem);
		mUid = calItem.getUid();
		mInvites = calItem.getInvites();
		mTzmap = calItem.getTimeZoneMap();
		Invite defInv = calItem.getDefaultInviteOrNull();
		if (defInv != null)
			setProperty(DavElements.P_DISPLAYNAME, defInv.getName());
		setProperty(DavElements.P_GETCONTENTTYPE, Mime.CT_TEXT_CALENDAR);
		setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(calItem.getSize()));
		addProperty(CalDavProperty.getCalendarData(this));
	}
	
	private String mUid;
	private Invite[] mInvites;
	private TimeZoneMap mTzmap;
	
	private static String getCalendarPath(CalendarItem calItem) throws ServiceException {
		// escape uid
		StringBuilder path = new StringBuilder();
		path.append(calItem.getPath());
		if (path.charAt(path.length()-1) != '/')
			path.append("/");
		path.append(calItem.getUid());
		path.append(CAL_EXTENSION);
		return path.toString();
	}

	/* Returns true if the supplied Filter matches this calendar object. */
	public boolean match(Filter filter) {
		for (Invite inv : mInvites)
			if (filter.match(inv))
				return true;
		
		return false;
	}

	/* Returns iCalendar representation of events that matches
	 * the supplied filter.
	 */
	public String getVcalendar(DavContext ctxt, Filter filter) throws IOException {
		StringBuilder buf = new StringBuilder();
		
		buf.append("BEGIN:VCALENDAR\r\n");
		buf.append("VERSION:").append(ZCalendar.sIcalVersion).append("\r\n");
		buf.append("PRODID:").append(ZCalendar.sZimbraProdID).append("\r\n");
		Iterator iter = mTzmap.tzIterator();
		while (iter.hasNext()) {
			ICalTimeZone tz = (ICalTimeZone) iter.next();
			CharArrayWriter wr = new CharArrayWriter();
			tz.newToVTimeZone().toICalendar(wr);
			wr.flush();
			buf.append(wr.toCharArray());
			wr.close();
		}
		for (Invite inv : mInvites) {
			if (filter != null && !filter.match(inv))
				continue;
			CharArrayWriter wr = new CharArrayWriter();
			try {
				inv.newToVComponent(false).toICalendar(wr);
			} catch (ServiceException se) {
				ZimbraLog.dav.error("cannot convert to ICalendar", se);
			}
			wr.flush();
			buf.append(wr.toCharArray());
			wr.close();
		}
		if (ctxt.isIcalClient()) {
			// XXX hack for early iCal release that puts HTTP url for organizer field.
			int orgPos = buf.indexOf(DavElements.ORGANIZER);
			int xorgPos = buf.indexOf(DavElements.ORGANIZER_HREF+":");
			if (orgPos > 0 && xorgPos > 0) {
				buf.replace(orgPos, orgPos + 9, DavElements.ORGANIZER_MAILTO);
				xorgPos = buf.indexOf(DavElements.ORGANIZER_HREF+":");
				buf.replace(xorgPos, xorgPos+16, DavElements.ORGANIZER);
			}
		}
		buf.append("END:VCALENDAR\r\n");
		return buf.toString();
	}
	
	@Override
	public InputStream getContent(DavContext ctxt) throws IOException, DavException {
		return new ByteArrayInputStream(getVcalendar(ctxt, null).getBytes());
	}

	@Override
	public boolean isCollection() {
		return false;
	}
	
	public String getUid() {
		return mUid;
	}
}
