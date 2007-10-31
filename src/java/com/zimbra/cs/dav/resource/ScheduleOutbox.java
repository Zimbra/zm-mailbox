/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.NetUtil;

public class ScheduleOutbox extends Collection {
	public ScheduleOutbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		addResourceType(DavElements.E_SCHEDULE_OUTBOX);
	}
	
	public void handlePost(DavContext ctxt) throws DavException, IOException, ServiceException {
		String      originator = ctxt.getRequest().getHeader(DavProtocol.HEADER_ORIGINATOR);
		Enumeration recipients = ctxt.getRequest().getHeaders(DavProtocol.HEADER_RECIPIENT);

		byte[] msg = ByteUtil.getContent(ctxt.getRequest().getInputStream(), 0);
		if (ZimbraLog.dav.isDebugEnabled())
			ZimbraLog.dav.debug(new String(msg, "UTF-8"));

		ZCalendar.ZVCalendar vcalendar = ZCalendar.ZCalendarBuilder.build(new InputStreamReader(new ByteArrayInputStream(msg)));
		Iterator<ZComponent> iter = vcalendar.getComponentIterator();
		ZComponent req = null;
		while (iter.hasNext()) {
			req = iter.next();
			if (req.getTok() != ICalTok.VTIMEZONE)
				break;
			req = null;
		}
		if (req == null)
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST);
		ZProperty organizerProp = req.getProperty(ICalTok.ORGANIZER);
		if (organizerProp != null) {
			ZOrganizer organizer = new ZOrganizer(organizerProp);
			String organizerStr = this.getAddressFromPrincipalURL(organizer.getAddress());
			if (!organizerStr.equals(ctxt.getAuthAccount().getName()))
				throw new DavException("the requestor is not the organizer", HttpServletResponse.SC_FORBIDDEN);
		}
		ZimbraLog.dav.debug("originator: "+originator);
        ArrayList<String> rcptArray = new ArrayList<String>();
        while (recipients.hasMoreElements()) {
            String rcpt = (String)recipients.nextElement();
            if (rcpt.indexOf(',') > 0) {
                String[] rcpts = rcpt.split(",");
                Collections.addAll(rcptArray, rcpts);
            } else {
                rcptArray.add(rcpt);
            }
        }
		Element scheduleResponse = ctxt.getDavResponse().getTop(DavElements.E_SCHEDULE_RESPONSE);
		for (String rcpt : rcptArray) {
			ZimbraLog.dav.debug("recipient email: "+rcpt);
			Element resp = scheduleResponse.addElement(DavElements.E_CALDAV_RESPONSE);
			switch (req.getTok()) {
			case VFREEBUSY:
				handleFreebusyRequest(ctxt, req, originator, rcpt, resp);
				break;
			case VEVENT:
				handleEventRequest(ctxt, vcalendar, req, originator, rcpt, resp);
				break;
			default:
				throw new DavException("unrecognized request: "+req.getTok(), HttpServletResponse.SC_BAD_REQUEST);
			}
		}
	}
	
	private void handleFreebusyRequest(DavContext ctxt, ZComponent vfreebusy, String originator, String rcpt, Element resp) throws DavException, ServiceException {
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

		ZimbraLog.dav.debug("start: "+new Date(start)+", end: "+new Date(end));
		Provisioning prov = Provisioning.getInstance();
		rcpt = rcpt.trim();
		String rcptAddr = this.getAddressFromPrincipalURL(rcpt);
		Account rcptAcct = prov.get(Provisioning.AccountBy.name, rcptAddr);
        String fbMsg = null;
		if (rcptAcct == null)
			throw new DavException("account not found "+rcptAddr, HttpServletResponse.SC_BAD_REQUEST);
		if (Provisioning.onLocalServer(rcptAcct)) {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(rcptAcct);
			FreeBusy fb = mbox.getFreeBusy(start, end);
			fbMsg = fb.toVCalendar(FreeBusy.Method.REPLY, originator, rcpt, null);
		} else {
            HttpMethod method = null;
            try {
                StringBuilder targetUrl = new StringBuilder();
                targetUrl.append(UserServlet.getRestUrl(rcptAcct));
                targetUrl.append("/Calendar?fmt=ifb");
                targetUrl.append("&start=");
                targetUrl.append(start);
                targetUrl.append("&end=");
                targetUrl.append(end);
                HttpClient client = new HttpClient();
                NetUtil.configureProxy(client);
                method = new GetMethod(targetUrl.toString());
                try {
                    client.executeMethod(method);
                    byte[] buf = ByteUtil.getContent(method.getResponseBodyAsStream(), 0);
                    fbMsg = new String(buf, "UTF-8");
                    fbMsg = fbMsg.replaceAll("METHOD:PUBLISH", "METHOD:REPLY");
                    fbMsg = fbMsg.replaceAll("ORGANIZER:.*", "ORGANIZER:"+originator+"\nATTENDEE:"+rcpt);
                } catch (IOException ex) {
                    // ignore this recipient and go on
                    fbMsg = null;
                }
            } finally {
                if (method != null)
                    method.releaseConnection();
            }
		}
        if (fbMsg != null) {
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("2.0;Success");
            resp.addElement(DavElements.E_CALENDAR_DATA).setText(fbMsg);
        }
	}

	private void handleEventRequest(DavContext ctxt, ZCalendar.ZVCalendar cal, ZComponent req, String originator, String rcpt, Element resp) {
        Address from, to;
        try {
            from = AccountUtil.getFriendlyEmailAddress(ctxt.getAuthAccount());
            if (rcpt.toLowerCase().startsWith("mailto:"))
            	rcpt = rcpt.substring(7);
            to = new InternetAddress(rcpt);
        } catch (AddressException e) {
			resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
			resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.7;"+rcpt);
			return;
        }
        ArrayList<Address> recipients = new java.util.ArrayList<Address>();
        recipients.add(to);
        String subject, uid, text, status;

        subject = "Meeting Request: ";
        status = req.getPropVal(ICalTok.STATUS, "");
        if (status.equals("CANCELLED"))
            subject = "Meeting Cancelled: ";
        subject += req.getPropVal(ICalTok.SUMMARY, "");
        text = req.getPropVal(ICalTok.DESCRIPTION, "");
        uid = req.getPropVal(ICalTok.UID, null);
        if (uid == null) {
			resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
			resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.1;UID");
			return;
        }
        try {
        	MimeMessage mm = CalendarMailSender.createCalendarMessage(from, from, recipients, subject, text, uid, cal);
        	Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ctxt.getAuthAccount());
        	mbox.getMailSender().sendMimeMessage(ctxt.getOperationContext(), mbox, true, mm, null, null, 0, null, null, true, false);
        } catch (ServiceException e) {
			resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
			resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.1");
        }
	}
    
    /*
     * to workaround the pre release iCal bugs
     */
    protected String getAddressFromPrincipalURL(String url) throws ServiceException, DavException {
        url = url.trim();
        if (url.startsWith("http://")) {
            // iCal sets the organizer field to be the URL of
            // CalDAV account.
            //     ORGANIZER:http://jylee-macbook:7070/service/dav/user1
            int pos = url.indexOf("/service/dav/");
            if (pos != -1) {
                int start = pos + 13;
                int end = url.indexOf("/", start);
                String userId = (end == -1) ? url.substring(start) : url.substring(start, end);
                Account organizer = Provisioning.getInstance().get(AccountBy.name, userId);
                if (organizer == null)
                    throw new DavException("user not found: "+userId, HttpServletResponse.SC_BAD_REQUEST, null);
                return organizer.getName();
            }
        } else if (url.toLowerCase().startsWith("mailto:")) {
            // iCal sometimes prefixes the email addr with more than one mailto:
            while (url.toLowerCase().startsWith("mailto:")) {
                url = url.substring(7);
            }
        }
        return url;
    }
}
