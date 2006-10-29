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

import org.dom4j.Element;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;

public class CalendarObject extends DavResource {

	public static final String CAL_EXTENSION = ".ics";
	
	public CalendarObject(Appointment appt) throws ServiceException {
		this(appt.getPath(), appt);
	}

	public CalendarObject(CalendarCollection cal, Appointment appt) throws ServiceException {
		this(cal.mUri, appt);
	}
	
	public CalendarObject(String uri, Appointment appt) throws ServiceException {
		super(uri, appt.getAccount());
		mUid = appt.getUid();
		mInvites = appt.getInvites();
		mTzmap = appt.getTimeZoneMap();
		try {
			mVcalendar = getVcalendar();
		} catch (IOException e) {
			ZimbraLog.dav.error("cannot convert to ICalendar", e);
		}
		setProperty(DavElements.P_DISPLAYNAME, appt.getDefaultInvite().getName());
		setProperty(DavElements.P_GETETAG, Long.toString(appt.getDate()));
		setProperty(DavElements.P_GETCONTENTTYPE, Mime.CT_TEXT_CALENDAR);
		setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(mVcalendar.length()));
	}
	
	public Element addHref(Element parent, boolean nameOnly) throws DavException {
		Element href = super.addHref(parent, true);
		href.setText(generateHref());
		return href;
	}
	
	private String mUid;
	private Invite[] mInvites;
	private TimeZoneMap mTzmap;
	private String mVcalendar;
	
	private String generateHref() {
		// escape
		return mUid + CAL_EXTENSION;
	}

	private String getVcalendar() throws IOException {
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
		buf.append("END:VCALENDAR\r\n");
		return buf.toString();
	}
	
	@Override
	public InputStream getContent() throws IOException, DavException {
		return new ByteArrayInputStream(getVcalendar().getBytes());
	}

	@Override
	public boolean isCollection() {
		return false;
	}

}
