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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
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
		this(ctxt, getCalendarPath(calItem), calItem);
	}
	
	protected CalendarObject(DavContext ctxt, String path, CalendarItem calItem) throws ServiceException {
		super(ctxt, path, calItem);
		mUid = calItem.getUid();
		mInvites = calItem.getInvites();
		mTzmap = calItem.getTimeZoneMap();
		Invite defInv = calItem.getDefaultInviteOrNull();
		if (defInv != null)
			setProperty(DavElements.P_DISPLAYNAME, defInv.getName());
		setProperty(DavElements.P_GETCONTENTTYPE, Mime.CT_TEXT_CALENDAR);
		setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(calItem.getSize()));
		addProperty(CalDavProperty.getCalendarData(this));
		mAccount = calItem.getAccount();
	}
	
	private String mUid;
	private Invite[] mInvites;
	private TimeZoneMap mTzmap;
	private Account mAccount;
	
	protected static String getCalendarPath(CalendarItem calItem) throws ServiceException {
		return getCalendarPath(calItem.getPath(), calItem.getUid());
	}
	
	protected static String getCalendarPath(String itemPath, String uid) {
		// escape uid
		StringBuilder path = new StringBuilder();
		path.append(itemPath);
		if (path.charAt(path.length()-1) != '/')
			path.append("/");
		path.append(uid);
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
			    boolean allowPrivateAccess = Account.allowPrivateAccess(ctxt.getAuthAccount(), mAccount);
				inv.newToVComponent(false, allowPrivateAccess).toICalendar(wr);
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
	public InputStream getContent(DavContext ctxt) throws IOException {
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
