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

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;

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
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.MsgKey;

public class CalendarCollection extends Collection {

	public CalendarCollection(Folder f) throws DavException, ServiceException {
		super(f);
		Account acct = f.getAccount();
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		mDavCompliance.add(Compliance.three);
		mDavCompliance.add(Compliance.access_control);
		mDavCompliance.add(Compliance.calendar_access);

		ResourceProperty rtype = getProperty(DavElements.E_RESOURCETYPE);
		rtype.addChild(DavElements.E_CALENDAR);
		rtype.addChild(DavElements.E_PRINCIPAL);
		
		ResourceProperty desc = new ResourceProperty(DavElements.E_CALENDAR_DESCRIPTION);
		Locale lc = acct.getLocale();
		desc.setMessageLocale(lc);
		desc.setStringValue(L10nUtil.getMessage(MsgKey.caldavCalendarDescription, lc));
		addProperty(desc);
		addProperty(CalDavProperty.getSupportedCalendarComponentSet());
		addProperty(CalDavProperty.getSupportedCalendarData());
		addProperty(CalDavProperty.getSupportedCollationSet());
		
		setProperty(DavElements.E_DISPLAYNAME, acct.getAttr(Provisioning.A_displayName));
		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getResourceUrl(this), true);
		setProperty(DavElements.E_ALTERNATE_URI_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);

		//setProperty(DavElements.E_GETETAG, "", true);
	
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

	private static TimeRange sAllAppts;
	
	static {
		sAllAppts = new TimeRange(null);
	}
	
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		try {
			java.util.Collection<Appointment> appts = get(ctxt, sAllAppts);
			ArrayList<DavResource> children = new ArrayList<DavResource>();
			for (Appointment appt : appts)
				children.add(new CalendarObject(appt));
			return children;
		} catch (ServiceException se) {
			ZimbraLog.dav.error("can't get appointments", se);
		}
		return Collections.emptyList();
	}
	
	public Element addPropertyElement(DavContext ctxt, Element parent, QName propName, boolean putValue) {
		Element e = super.addPropertyElement(ctxt, parent, propName, putValue);

		if (e != null)
			return e;
		
		return null;
	}
	
	public java.util.Collection<Appointment> get(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
		Mailbox mbox = getMailbox();
		return mbox.getAppointmentsForRange(ctxt.getOperationContext(), range.getStart(), range.getEnd(), mId, null);
	}
	
	public ParsedMessage createParsedMessage(byte[] item) throws DavException, IOException {
		String method = "REQUEST";
		String boundary = "--caldav-"+System.currentTimeMillis();
		StringBuilder msgBuf = new StringBuilder();
		msgBuf.append("Subject: calendar item from caldav\r\n");
		msgBuf.append("Content-Type: multipart/mixed");
		msgBuf.append("; boundary=\"").append(boundary).append("\"\r\n");
		msgBuf.append("\r\n");
		msgBuf.append("this is a msg in multipart format\r\n");
		msgBuf.append("\r\n");
		msgBuf.append("--").append(boundary).append("\r\n");
		msgBuf.append("Content-Type: ").append(Mime.CT_TEXT_CALENDAR);
		msgBuf.append("; method=").append(method);
		msgBuf.append("; name=meeting.ics").append("\r\n");
		msgBuf.append("Content-Transfer-Encoding: 8bit\r\n");
		msgBuf.append("\r\n");
		msgBuf.append(new String(item, "UTF-8"));
		msgBuf.append("\r\n");
		msgBuf.append("--").append(boundary).append("--").append("\r\n");
		try {
			return new ParsedMessage(msgBuf.toString().getBytes(), false);
		} catch (MessagingException e) {
			throw new DavException("cannot create ParsedMessage", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
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
	
	public void createItem(DavContext ctxt, String user, String name) throws DavException, IOException {
		HttpServletRequest req = ctxt.getRequest();
		if (req.getContentLength() <= 0)
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST, null);
		if (!req.getContentType().equalsIgnoreCase(Mime.CT_TEXT_CALENDAR))
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST, null);
			
		String etag = req.getHeader(DavProtocol.HEADER_IF_MATCH);

		/*
		 * chandler doesn't send the If-None-Match or If-Match headers yet.
		String noneMatch = req.getHeader(DavProtocol.HEADER_IF_NONE_MATCH);
		if (etag != null && noneMatch != null ||
				etag == null && noneMatch == null ||
				name == null)
			throw new DavException("bad request", HttpServletResponse.SC_BAD_REQUEST, null);
			*/
		
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
			
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			Appointment appt = mbox.getAppointmentByUid(ctxt.getOperationContext(), name);
			if (appt == null && isUpdate)
				throw new DavException("event not found", HttpServletResponse.SC_NOT_FOUND, null);
			/*
			 * chandler doesn't use If-Match
			if (appt != null && !isUpdate)
				throw new DavException("event already exists", HttpServletResponse.SC_CONFLICT, null);
			if (isUpdate) {
				CalendarObject calObj = new CalendarObject(appt);
				ResourceProperty etagProp = calObj.getProperty(DavElements.P_GETETAG);
				if (!etagProp.getStringValue().equals(etag))
					throw new DavException("event not found", HttpServletResponse.SC_BAD_REQUEST, null);
			}
			 */
			for (Invite i : invites) {
	            if (i.getUid() == null)
	                i.setUid(LdapUtil.generateUUID());
				mbox.addInvite(ctxt.getOperationContext(), i, mId, false, null);
			}
			/*
			Mailbox.SetAppointmentData data = new SetAppointmentData();
			data.mPm = createParsedMessage(item);
			data.mInv = invites.get(0);
			mbox.setAppointment(ctxt.getOperationContext(), this.mId, data, null);
			*/
		} catch (ServiceException e) {
			throw new DavException("cannot create icalendar item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
		
	}
}
