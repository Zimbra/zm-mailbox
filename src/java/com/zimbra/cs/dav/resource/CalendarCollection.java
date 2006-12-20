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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.MsgKey;

/**
 * draft-dusseault-caldav-15 section 4.2
 * 
 * @author jylee
 *
 */
public class CalendarCollection extends Collection {

	public CalendarCollection(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		Account acct = f.getAccount();
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		mDavCompliance.add(Compliance.three);
		mDavCompliance.add(Compliance.access_control);
		mDavCompliance.add(Compliance.calendar_access);

		ResourceProperty rtype = getProperty(DavElements.E_RESOURCETYPE);
		rtype.addChild(DavElements.E_CALENDAR);
		rtype.addChild(DavElements.E_PRINCIPAL);
		
		String displayName = acct.getAttr(Provisioning.A_displayName);
		ResourceProperty desc = new ResourceProperty(DavElements.E_CALENDAR_DESCRIPTION);
		Locale lc = acct.getLocale();
		desc.setMessageLocale(lc);
		desc.setStringValue(L10nUtil.getMessage(MsgKey.caldavCalendarDescription, lc, displayName));
		desc.setProtected(true);
		addProperty(desc);
		addProperty(CalDavProperty.getSupportedCalendarComponentSet());
		addProperty(CalDavProperty.getSupportedCalendarData());
		addProperty(CalDavProperty.getSupportedCollationSet());
		
		setProperty(DavElements.E_DISPLAYNAME, displayName);
		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getResourceUrl(this), true);
		setProperty(DavElements.E_ALTERNATE_URI_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);

		// remaining recommented attributes: calendar-timezone, max-resource-size,
		// min-date-time, max-date-time, max-instances, max-attendees-per-instance,
		//
	}
	
	@Override
	public InputStream getContent() throws IOException, DavException {
		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	private static TimeRange sAllCalItems;
	
	static {
		sAllCalItems = new TimeRange(null);
	}
	
	/* Returns all the appoinments stored in the calendar as DavResource. */
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		try {
			java.util.Collection<CalendarItem> calItems = get(ctxt, sAllCalItems);
			ArrayList<DavResource> children = new ArrayList<DavResource>();
			for (CalendarItem calItem : calItems)
				children.add(new CalendarObject(ctxt, calItem));
			return children;
		} catch (ServiceException se) {
			ZimbraLog.dav.error("can't get calendar items", se);
		}
		return Collections.emptyList();
	}

	/* Returns the appoinments in the specified time range. */
	public java.util.Collection<CalendarItem> get(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
		Mailbox mbox = getMailbox(ctxt);
		return mbox.getCalendarItemsForRange(ctxt.getOperationContext(), range.getStart(), range.getEnd(), mId, null);
	}
	
	private String findSummary(ZVCalendar cal) {
		Iterator<ZComponent> iter = cal.getComponentIterator();
		while (iter.hasNext()) {
			ZComponent comp = iter.next();
			String summary = comp.getPropVal(ICalTok.SUMMARY, null);
			if (summary != null)
				return summary;
		}
		return "calendar event";
	}
	
	private String findEventUid(List<Invite> invites) throws DavException {
		String uid = null;
		for (Invite i : invites)
            if (i.getItemType() == MailItem.TYPE_APPOINTMENT) {
				if (uid != null)
					throw new DavException("too many events", HttpServletResponse.SC_BAD_REQUEST, null);
				uid = i.getUid();
			}
		if (uid == null)
			throw new DavException("no event in the request", HttpServletResponse.SC_BAD_REQUEST, null);
		return uid;
	}

	/* creates an appointment sent in PUT request in this calendar. */
	public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
		HttpServletRequest req = ctxt.getRequest();
		if (req.getContentLength() <= 0)
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST, null);
		if (!req.getContentType().equalsIgnoreCase(Mime.CT_TEXT_CALENDAR))
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST, null);
		
		/*
		 * chandler doesn't set User-Agent header, doesn't understand 
		 * If-None-Match or If-Match headers.  assume the worst, and don't expect
		 * If-None-Match or If-Match headers unless the client is verified
		 * to work correctly with those headers, like Evolution 2.8.
		 */
		String userAgent = req.getHeader(DavProtocol.HEADER_USER_AGENT);
		boolean beLessRestrictive = (userAgent == null) || !userAgent.startsWith("Evolution");
		
		String etag = req.getHeader(DavProtocol.HEADER_IF_MATCH);

		String noneMatch = req.getHeader(DavProtocol.HEADER_IF_NONE_MATCH);
		if (!beLessRestrictive &&
				(etag != null && noneMatch != null ||
				 etag == null && noneMatch == null ||
				 name == null))
			throw new DavException("bad request", HttpServletResponse.SC_BAD_REQUEST, null);
		
		boolean isUpdate = (etag != null);
		if (name.endsWith(CalendarObject.CAL_EXTENSION))
			name = name.substring(0, name.length()-CalendarObject.CAL_EXTENSION.length());
		
		Provisioning prov = Provisioning.getInstance();
		try {
			byte[] item = ByteUtil.getContent(req.getInputStream(), req.getContentLength());
			ZCalendar.ZVCalendar vcalendar = ZCalendar.ZCalendarBuilder.build(new InputStreamReader(new ByteArrayInputStream(item)));
			List<Invite> invites = Invite.createFromCalendar(ctxt.getAuthAccount(), 
					findSummary(vcalendar), 
					vcalendar, 
					true);
			String user = ctxt.getUser();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			String uid = findEventUid(invites);
			if (!uid.equals(name)) {
				// because we are keying off the URI, we don't have
				// much choice except to use the UID of VEVENT for calendar item URI.
				// Evolution doesn't use UID as the URI, so we'll force it
				// by issuing redirect to the URI we want it to be at.
				StringBuilder url = new StringBuilder();
				url.append(DavServlet.getDavUrl(user));
				url.append(getUri());
				url.append(uid);
				url.append(CalendarObject.CAL_EXTENSION);
				ctxt.getResponse().sendRedirect(url.toString());
				throw new DavException("wrong url", HttpServletResponse.SC_MOVED_PERMANENTLY, null);
			}
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			CalendarItem calItem = mbox.getCalendarItemByUid(ctxt.getOperationContext(), name);
			if (calItem == null && isUpdate)
				throw new DavException("event not found", HttpServletResponse.SC_NOT_FOUND, null);
			
			if (!beLessRestrictive && calItem != null && !isUpdate)
				throw new DavException("event already exists", HttpServletResponse.SC_CONFLICT, null);
			
			if (isUpdate) {
				if (etag.charAt(0) == '"' && etag.charAt(0) == '"')
					etag = etag.substring(1, etag.length()-1);
				CalendarObject calObj = new CalendarObject(ctxt, calItem);
				ResourceProperty etagProp = calObj.getProperty(DavElements.P_GETETAG);
				if (!etagProp.getStringValue().equals(etag))
					throw new DavException("event has different etag ("+etagProp.getStringValue()+") vs "+etag, HttpServletResponse.SC_CONFLICT, null);
			}
			for (Invite i : invites) {
	            if (i.getUid() == null)
	                i.setUid(LdapUtil.generateUUID());
				mbox.addInvite(ctxt.getOperationContext(), i, mId, false, null);
			}
			calItem = mbox.getCalendarItemByUid(ctxt.getOperationContext(), uid);
			return new CalendarObject(ctxt, calItem);
		} catch (ServiceException e) {
			throw new DavException("cannot create icalendar item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/* Returns iCalalendar (RFC 2445) representation of freebusy report for specified time range. */
	public String getFreeBusyReport(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
		Mailbox mbox = getMailbox(ctxt);
		FreeBusy fb = mbox.getFreeBusy(range.getStart(), range.getEnd());
		ParsedDateTime now = ParsedDateTime.fromUTCTime(System.currentTimeMillis());
		ParsedDateTime startTime = ParsedDateTime.fromUTCTime(range.getStart());
		ParsedDateTime endTime = ParsedDateTime.fromUTCTime(range.getEnd());
		
		StringBuilder buf = new StringBuilder();
		buf.append("BEGIN:VCALENDAR\r\n");
		buf.append("VERSION:").append(ZCalendar.sIcalVersion).append("\r\n");
		buf.append("PRODID:").append(ZCalendar.sZimbraProdID).append("\r\n");
		buf.append("BEGIN:VFREEBUSY\r\n");
		buf.append("ORGANIZER:").append(ctxt.getAuthAccount().getName()).append("\r\n");
		buf.append("ATENDEE:").append(mbox.getAccount().getName()).append("\r\n");
		buf.append("DTSTART:").append(startTime.toString()).append("\r\n");
		buf.append("DTEND:").append(endTime.toString()).append("\r\n");
		buf.append("DTSTAMP:").append(now.toString()).append("\r\n");

		Iterator iter = fb.iterator();
		while (iter.hasNext()) {
			FreeBusy.Interval cur = (FreeBusy.Interval)iter.next();
			String status = cur.getStatus();

			if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
				continue;
			} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
				buf.append("FREEBUSY:");
			} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
				buf.append("FREEBUSY;FBTYPE=BUSY-TENTATIVE:");
			} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
				buf.append("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:");
			} else {
				continue;
			}

			ParsedDateTime curStart = ParsedDateTime.fromUTCTime(cur.getStart());
			ParsedDateTime curEnd = ParsedDateTime.fromUTCTime(cur.getEnd());

			buf.append(curStart.toString()).append('/').append(curEnd.toString()).append("\r\n");
		}
        
		buf.append("END:VFREEBUSY\r\n");
		buf.append("END:VCALENDAR\r\n");
		return buf.toString();
	}
}
