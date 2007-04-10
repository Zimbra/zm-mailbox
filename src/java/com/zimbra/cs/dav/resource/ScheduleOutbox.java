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
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;

public class ScheduleOutbox extends CalendarCollection {
	public ScheduleOutbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		addResourceType(DavElements.E_SCHEDULE_OUTBOX);
	}
	
	public void handlePost(DavContext ctxt) throws DavException, IOException {
		byte[] msg = ByteUtil.getContent(ctxt.getRequest().getInputStream(), 0);
		if (ZimbraLog.dav.isDebugEnabled())
			ZimbraLog.dav.debug(new String(msg, "UTF-8"));
		ZCalendar.ZVCalendar vcalendar;
		String      originator = ctxt.getRequest().getHeader(DavProtocol.HEADER_ORIGINATOR);
		Enumeration recipients = ctxt.getRequest().getHeaders(DavProtocol.HEADER_RECIPIENT);
		try {
			vcalendar = ZCalendar.ZCalendarBuilder.build(new InputStreamReader(new ByteArrayInputStream(msg)));
			ZComponent req = vcalendar.getComponentIterator().next();
			if (req == null)
				throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST);
			while (recipients.hasMoreElements()) {
				String rcpt = (String) recipients.nextElement();
				switch (req.getTok()) {
				case VFREEBUSY:
					handleFreebusyRequest(ctxt, req, originator, rcpt);
					break;
				default:
					throw new DavException("unrecognized request: "+req.getTok(), HttpServletResponse.SC_BAD_REQUEST);
				}
			}
		} catch (ServiceException se) {
			throw new DavException("error handling POST request", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
		}
	}
	
	private void handleFreebusyRequest(DavContext ctxt, ZComponent vfreebusy, String originator, String originalRcpt) throws DavException, ServiceException, IOException {
		ZProperty dtstartProp = vfreebusy.getProperty(ICalTok.DTSTART);
		ZProperty dtendProp = vfreebusy.getProperty(ICalTok.DTEND);
		ZProperty durationProp = vfreebusy.getProperty(ICalTok.DURATION);
		if (dtstartProp == null || dtendProp == null && durationProp == null)
			throw new DavException("missing dtstart or dtend/duration in the schedule request", HttpServletResponse.SC_BAD_REQUEST, null);
		long start, end;
		try {
			ParsedDateTime startTime = ParsedDateTime.parseUtcOnly(dtstartProp.getValue());
			start = startTime.getUtcTime();
			if (dtendProp != null) {
				end = ParsedDateTime.parseUtcOnly(dtendProp.getValue()).getUtcTime();
			} else {
				ParsedDuration dur = ParsedDuration.parse(durationProp.getValue());
				end = start + dur.getDurationAsMsecs(new Date(start));
			}
		} catch (ParseException pe) {
			throw new DavException("can't parse date", HttpServletResponse.SC_BAD_REQUEST, pe);
		}

		ZimbraLog.dav.debug("originator: "+originator+", "+"start: "+new Date(start)+", end: "+new Date(end));
		Provisioning prov = Provisioning.getInstance();
		Element resp = ctxt.getDavResponse().getTop(DavElements.E_SCHEDULE_RESPONSE);
		String rcpt = this.getAddressFromPrincipalURL(originalRcpt);
		ZimbraLog.dav.debug("recipient: "+originalRcpt+", email: "+rcpt);
		Element r = resp.addElement(DavElements.E_CALDAV_RESPONSE);
		Account rcptAcct = prov.get(Provisioning.AccountBy.name, rcpt);
		if (Provisioning.onLocalServer(rcptAcct)) {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(rcptAcct);
			FreeBusy fb = mbox.getFreeBusy(start, end);
			String fbMsg = fb.toVCalendar(FreeBusy.Method.REPLY, originator, originalRcpt, null);
			r.addElement(DavElements.E_RECIPIENT).setText(originalRcpt);
			r.addElement(DavElements.E_REQUEST_STATUS).setText("2.0;Success");
			r.addElement(DavElements.E_CALENDAR_DATA).setText(fbMsg);
		} else {
			// XXX get the freebusy information from remote server
		}
	}
}
