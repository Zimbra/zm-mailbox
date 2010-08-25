/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.io.InputStream;
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

import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.util.AccountUtil;

public class ScheduleOutbox extends Collection {
	public ScheduleOutbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		addResourceType(DavElements.E_SCHEDULE_OUTBOX);
	}
	
	public void handlePost(DavContext ctxt) throws DavException, IOException, ServiceException {
		String      originator = ctxt.getRequest().getHeader(DavProtocol.HEADER_ORIGINATOR);
		Enumeration recipients = ctxt.getRequest().getHeaders(DavProtocol.HEADER_RECIPIENT);

		InputStream in = ctxt.getUpload().getInputStream();

		ZCalendar.ZVCalendar vcalendar = ZCalendar.ZCalendarBuilder.build(in, MimeConstants.P_CHARSET_UTF8);
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
        // bug 49987: Workaround for an Apple iCal bug
        // Don't send the CANCEL notification to anyone who is not listed as an ATTENDEE of the cancel
        // component.  This attendee list is the inverse of the remaining attendees in the appointment.  Also
        // make sure the cancel is not sent to the organizer.
        boolean isCancel = ICalTok.CANCEL.toString().equalsIgnoreCase(vcalendar.getPropVal(ICalTok.METHOD, null));
        if (isCancel) {
            // Get list of attendees. (mailto:email values)
            ArrayList<String> attendees = new ArrayList<String>();
            String organizer = null;
            for (Iterator<ZProperty> propsIter = req.getPropertyIterator(); propsIter.hasNext(); ) {
                ZProperty prop = propsIter.next();
                ICalTok token = prop.getToken();
                if (ICalTok.ATTENDEE.equals(token))
                    attendees.add(prop.getValue());
                else if (ICalTok.ORGANIZER.equals(token))
                    organizer = prop.getValue();
            }
            // Validate rcptArray against attendee list.
            for (Iterator<String> rcptIter = rcptArray.iterator(); rcptIter.hasNext(); ) {
                String rcpt = rcptIter.next();
                boolean isAttendee = false;
                if (rcpt != null) {
                    // Rcpt must be an attendee of the cancel component.
                    for (String at : attendees) {
                        if (rcpt.equalsIgnoreCase(at)) {
                            isAttendee = true;
                            break;
                        }
                    }
                    // But it can't be the organizer.
                    if (isAttendee && rcpt.equalsIgnoreCase(organizer))
                        isAttendee = false;
                }
                // Remove the invalid recipient.
                if (!isAttendee) {
                    ZimbraLog.dav.info("Ignoring recipient " + rcpt + " of CANCEL request; likely a client bug");
                    rcptIter.remove();
                }
            }
        }
		Element scheduleResponse = ctxt.getDavResponse().getTop(DavElements.E_SCHEDULE_RESPONSE);
		for (String rcpt : rcptArray) {
		    rcpt = rcpt.trim();
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
				ParsedDateTime endTime = startTime.add(dur);
				end = endTime.getUtcTime();
			}
		} catch (ParseException pe) {
			throw new DavException("can't parse date", HttpServletResponse.SC_BAD_REQUEST, pe);
		}

		ZimbraLog.dav.debug("rcpt: "+rcpt+", start: "+new Date(start)+", end: "+new Date(end));
		
		FreeBusy fb = null;
        if (ctxt.isFreebusyEnabled()) {
        	FreeBusyQuery fbQuery = new FreeBusyQuery(ctxt.getRequest(), ctxt.getAuthAccount(), start, end, null);
        	fbQuery.addEmailAddress(getAddressFromPrincipalURL(rcpt), FreeBusyQuery.CALENDAR_FOLDER_ALL);
        	java.util.Collection<FreeBusy> fbResult = fbQuery.getResults();
            if (fbResult.size() > 0)
            	fb = fbResult.iterator().next();
        }
        if (fb != null) {
        	String fbMsg = fb.toVCalendar(FreeBusy.Method.REPLY, originator, rcpt, null);
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("2.0;Success");
            resp.addElement(DavElements.E_CALENDAR_DATA).setText(fbMsg);
        } else {
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.3;No f/b for the user");
        }
	}

    private void handleEventRequest(DavContext ctxt, ZCalendar.ZVCalendar cal, ZComponent req, String originator, String rcpt, Element resp) throws ServiceException,DavException {
        if (!ctxt.isSchedulingEnabled()) {
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.3;No scheduling for the user");
            return;
        }
        ArrayList<Address> recipients = new java.util.ArrayList<Address>();
        InternetAddress from, sender, to;
        try {
            Account target = null;
            Provisioning prov = Provisioning.getInstance();
            if (ctxt.getPathInfo() != null)
                target = prov.getAccountByName(ctxt.getPathInfo());
            if (target == null)
                target = getMailbox(ctxt).getAccount();
            from = AccountUtil.getFriendlyEmailAddress(target);
            if (originator.toLowerCase().startsWith("mailto:"))
                originator = originator.substring(7);
            sender = new InternetAddress(originator);
            if (rcpt.toLowerCase().startsWith("mailto:"))
                rcpt = rcpt.substring(7);
            to = new InternetAddress(rcpt);
            recipients.add(to);
        } catch (AddressException e) {
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.7;"+rcpt);
            return;
        }
        String subject, uid, desc, descHtml, status, method;

        status = req.getPropVal(ICalTok.STATUS, "");
        method = cal.getPropVal(ICalTok.METHOD, "REQUEST");
        
        if (method.equals("REQUEST")) {
            ZProperty organizerProp = req.getProperty(ICalTok.ORGANIZER);
            if (organizerProp != null) {
                String organizerStr = this.getAddressFromPrincipalURL(new ZOrganizer(organizerProp).getAddress());
                if (!AccountUtil.addressMatchesAccount(getMailbox(ctxt).getAccount(), organizerStr)) {
                	ZimbraLog.dav.debug("scheduling appointment on behalf of %s", organizerStr);
                }
            }
            subject = "Meeting Request: ";
        } else if (method.equals("REPLY")) {
            ZProperty attendeeProp = req.getProperty(ICalTok.ATTENDEE);
            if (attendeeProp == null)
                throw new DavException("missing property ATTENDEE", HttpServletResponse.SC_BAD_REQUEST);
            ZAttendee attendee = new ZAttendee(attendeeProp);
            String partStat = attendee.getPartStat();
            if (partStat.equals(IcalXmlStrMap.PARTSTAT_ACCEPTED)) {
                subject = "Accepted: ";
            } else if (partStat.equals(IcalXmlStrMap.PARTSTAT_TENTATIVE)) {
                subject = "Tentative: ";
            } else if (partStat.equals(IcalXmlStrMap.PARTSTAT_DECLINED)) {
                subject = "Declined: ";
            } else {
                subject = "Meeting Reply: ";
            }
        } else {
            subject = "Meeting: ";
        }
        
        if (status.equals("CANCELLED"))
            subject = "Meeting Cancelled: ";
        subject += req.getPropVal(ICalTok.SUMMARY, "");
        desc = req.getPropVal(ICalTok.DESCRIPTION, "");
        descHtml = req.getDescriptionHtml();
        uid = req.getPropVal(ICalTok.UID, null);
        if (uid == null) {
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.1;UID");
            return;
        }
        try {
            MimeMessage mm = CalendarMailSender.createCalendarMessage(from, sender, recipients, subject, desc, descHtml, uid, cal);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ctxt.getAuthAccount());
            mbox.getMailSender().sendMimeMessage(ctxt.getOperationContext(), mbox, true, mm, null, null, null, null, null, true, false);
        } catch (ServiceException e) {
            resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.1");
            return;
        }
        resp.addElement(DavElements.E_RECIPIENT).setText(rcpt);
        resp.addElement(DavElements.E_REQUEST_STATUS).setText("2.0;Success");
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
