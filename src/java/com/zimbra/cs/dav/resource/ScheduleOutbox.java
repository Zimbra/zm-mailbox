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
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

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
import com.zimbra.cs.dav.property.ResourceProperty;
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

public class ScheduleOutbox extends Collection {
	public ScheduleOutbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		ResourceProperty rtype = getProperty(DavElements.E_RESOURCETYPE);
		rtype.addChild(DavElements.E_SCHEDULE_OUTBOX);
	}
	
	public void handleScheduleRequest(DavContext ctxt) throws DavException, ServiceException, java.io.IOException {
		byte[] msg = ByteUtil.getContent(ctxt.getRequest().getInputStream(), 0);
		if (ZimbraLog.dav.isDebugEnabled())
			ZimbraLog.dav.debug(new String(msg, "UTF-8"));
		ZCalendar.ZVCalendar vcalendar = ZCalendar.ZCalendarBuilder.build(new InputStreamReader(new ByteArrayInputStream(msg)));
		Iterator<ZComponent> iter = vcalendar.getComponentIterator();
		while (iter.hasNext()) {
			ZComponent comp = iter.next();
			if (comp.getTok().equals(ICalTok.VFREEBUSY))
				handleFreebusyRequest(ctxt, comp);
		}
	}
	
	private void handleFreebusyRequest(DavContext ctxt, ZComponent vfreebusy) throws DavException, ServiceException, java.io.IOException {
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
				end = ParsedDateTime.parseUtcOnly(dtstartProp.getValue()).getUtcTime();
			} else {
				ParsedDuration dur = ParsedDuration.parse(durationProp.getValue());
				end = start + dur.getDurationAsMsecs(new Date(start));
			}
		} catch (ParseException pe) {
			throw new DavException("can't parse date", HttpServletResponse.SC_BAD_REQUEST, pe);
		}
		String originator = ctxt.getRequest().getHeader("Originator");
		Enumeration recipients = ctxt.getRequest().getHeaders("Recipient");
		ZimbraLog.dav.debug("originator: "+originator);
		Provisioning prov = Provisioning.getInstance();
		Element resp = ctxt.getDavResponse().getTop(DavElements.E_SCHEDULE_RESPONSE);
		while (recipients.hasMoreElements()) {
			String rcpt = (String)recipients.nextElement();
			ZimbraLog.dav.debug("recipient: "+rcpt);
			Element r = resp.addElement(DavElements.E_CALDAV_RESPONSE);
			try {
				Account rcptAcct = prov.get(Provisioning.AccountBy.name, rcpt);
				if (Provisioning.onLocalServer(rcptAcct)) {
					Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(rcptAcct);
					FreeBusy fb = mbox.getFreeBusy(start, end);
					String fbMsg = fb.toVCalendar(FreeBusy.Method.REPLY, originator, rcpt, null);
					r.addElement(DavElements.E_RECIPIENT).setText("mailto:"+rcpt);
					r.addElement(DavElements.E_REQUEST_STATUS).setText("2.0;Success");
					r.addElement(DavElements.E_CALENDAR_DATA).setText(fbMsg);
				} else {
					// XXX get the freebusy information from remote server
				}
			} catch (ServiceException se) {
				ZimbraLog.dav.error("can't get account", se);
			}
		}
	}
}
