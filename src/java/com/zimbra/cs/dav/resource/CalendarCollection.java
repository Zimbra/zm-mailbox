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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
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
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
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
		mDavCompliance.add(Compliance.calendar_schedule);

		addResourceType(DavElements.E_CALENDAR);
		
		// the display name can be a user friendly string like "John Smith's Calendar".
		// but the problem is the name may be too long to fit into the field in UI.
		Locale lc = acct.getLocale();
		String description = L10nUtil.getMessage(MsgKey.caldavCalendarDescription, lc, acct.getAttr(Provisioning.A_displayName), f.getName());
		ResourceProperty desc = new ResourceProperty(DavElements.E_CALENDAR_DESCRIPTION);
		desc.setMessageLocale(lc);
		desc.setStringValue(description);
		desc.setProtected(true);
		addProperty(desc);
		addProperty(CalDavProperty.getSupportedCalendarComponentSet());
		addProperty(CalDavProperty.getSupportedCalendarData());
		addProperty(CalDavProperty.getSupportedCollationSet());
		
		setProperty(DavElements.E_DISPLAYNAME, f.getName());
		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getResourceUrl(this), true);
		setProperty(DavElements.E_ALTERNATE_URI_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);

		// remaining recommented attributes: calendar-timezone, max-resource-size,
		// min-date-time, max-date-time, max-instances, max-attendees-per-instance,
		//
	}
	
	private static TimeRange sAllCalItems;
	
	static {
		sAllCalItems = new TimeRange(null);
	}
	
	/* Returns all the appoinments stored in the calendar as DavResource. */
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		try {
			return get(ctxt, sAllCalItems);
		} catch (ServiceException se) {
			ZimbraLog.dav.error("can't get calendar items", se);
		}
		return Collections.emptyList();
	}

	/* Returns the appoinments in the specified time range. */
	public java.util.Collection<DavResource> get(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
		Mailbox mbox = getMailbox(ctxt);
		java.util.Collection<CalendarItem> calItems = mbox.getCalendarItemsForRange(ctxt.getOperationContext(), range.getStart(), range.getEnd(), mId, null);
        ArrayList<DavResource> children = new ArrayList<DavResource>();
        for (CalendarItem calItem : calItems)
            children.add(new CalendarObject.LocalCalendarObject(ctxt, calItem));
        return children;
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
				if (uid != null && uid.compareTo(i.getUid()) != 0)
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
		ContentType ct = new ContentType(req.getContentType());
		if (req.getContentLength() <= 0 ||
                ct.getValue() == null || 
                !ct.getValue().equalsIgnoreCase(Mime.CT_TEXT_CALENDAR))
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST, null);
		
		/*
		 * some of the CalDAV clients do not behave very well when it comes to
		 * etags.
		 * 
		 * chandler doesn't set User-Agent header, doesn't understand 
		 * If-None-Match or If-Match headers.
		 * 
		 * evolution 2.8 always sets If-None-Match although we return etag in REPORT.
		 * 
		 * ical correctly understands etag and sets If-Match for existing etags, but
		 * does not use If-None-Match for new resource creation.
		 */
		String etag = req.getHeader(DavProtocol.HEADER_IF_MATCH);
		boolean useEtag = (etag != null);

		//String noneMatch = req.getHeader(DavProtocol.HEADER_IF_NONE_MATCH);
		
		if (name.endsWith(CalendarObject.CAL_EXTENSION))
			name = name.substring(0, name.length()-CalendarObject.CAL_EXTENSION.length());
		
		Provisioning prov = Provisioning.getInstance();
		try {
			byte[] item = ctxt.getRequestData();
            String user = ctxt.getUser();
            Account account = prov.get(AccountBy.name, user);
            if (account == null)
                throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			ZCalendar.ZVCalendar vcalendar = ZCalendar.ZCalendarBuilder.build(new InputStreamReader(new ByteArrayInputStream(item)));
			List<Invite> invites = Invite.createFromCalendar(account,
					findSummary(vcalendar), 
					vcalendar, 
					true);

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
			if (calItem == null && useEtag)
				throw new DavException("event not found", HttpServletResponse.SC_NOT_FOUND, null);
			
			if (useEtag) {
				String itemEtag = MailItemResource.getEtag(calItem);
				if (!itemEtag.equals(etag))
					throw new DavException("event has different etag ("+itemEtag+") vs "+etag, HttpServletResponse.SC_CONFLICT, null);
			}
			for (Invite i : invites) {
				// check for valid uid.
				if (i.getUid() == null)
					i.setUid(LdapUtil.generateUUID());
				// check for valid organizer field.
				if (i.hasOrganizer() || i.hasOtherAttendees()) {
					ZOrganizer org = i.getOrganizer();
					// if ORGANIZER field is unset, set the field value with authUser's email addr.
					if (org == null) {
						org = new ZOrganizer(ctxt.getAuthAccount().getName(), null);
						i.setOrganizer(org);
					}
					/*
					 * this hack was to work around iCal setting ORGANIZER field
					 * with principalURL.  iCal seemed to have fixed that bug.
					 * 
					String addr = i.getOrganizer().getAddress();
					String newAddr = getAddressFromPrincipalURL(addr);
					if (!addr.equals(newAddr)) {
						i.setOrganizer(new ZOrganizer(newAddr, null));
						ZProperty href = null;
						Iterator<ZProperty> xprops = i.xpropsIterator();
						while (xprops.hasNext()) {
							href = xprops.next();
							if (href.getName().equals(DavElements.ORGANIZER_HREF))
								break;
							href = null;
						}
						if (href == null) {
							href = new ZProperty(DavElements.ORGANIZER_HREF);
							i.addXProp(href);
						}
						href.setValue(addr);
					}
					*/
				}
				mbox.addInvite(ctxt.getOperationContext(), i, mId, false, null);
			}
			calItem = mbox.getCalendarItemByUid(ctxt.getOperationContext(), uid);
			return new CalendarObject.LocalCalendarObject(ctxt, calItem);
		} catch (ServiceException e) {
			throw new DavException("cannot create icalendar item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/* Returns iCalalendar (RFC 2445) representation of freebusy report for specified time range. */
	public String getFreeBusyReport(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
		Mailbox mbox = getMailbox(ctxt);
		FreeBusy fb = mbox.getFreeBusy(range.getStart(), range.getEnd());
		return fb.toVCalendar(FreeBusy.Method.REPLY, ctxt.getAuthAccount().getName(), mbox.getAccount().getName(), null);
	}
}
