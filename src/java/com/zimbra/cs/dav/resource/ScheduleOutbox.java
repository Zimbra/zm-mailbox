/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.caldav.CalDavUtils;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.FriendlyCalendaringDescription;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;

public class ScheduleOutbox extends Collection {
    public ScheduleOutbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
        super(ctxt, f);
        addResourceType(DavElements.E_SCHEDULE_OUTBOX);
    }

    @Override
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

        boolean isVEventOrVTodo = ICalTok.VEVENT.equals(req.getTok()) || ICalTok.VTODO.equals(req.getTok());
        boolean isOrganizerMethod = false, isCancel = false;
        if (isVEventOrVTodo) {
            String method = vcalendar.getPropVal(ICalTok.METHOD, null);
            if (method != null) {
                isOrganizerMethod = Invite.isOrganizerMethod(method);
                isCancel = ICalTok.CANCEL.toString().equalsIgnoreCase(method);;
            }

            CalDavUtils.removeAttendeeForOrganizer(req);  // Apple iCal fixup
        }

        // Get organizer and list of attendees. (mailto:email values)
        ArrayList<String> attendees = new ArrayList<String>();
        String organizer = null;
        for (Iterator<ZProperty> propsIter = req.getPropertyIterator(); propsIter.hasNext(); ) {
            ZProperty prop = propsIter.next();
            ICalTok token = prop.getToken();
            if (ICalTok.ATTENDEE.equals(token)) {
                String val = prop.getValue();
                if (val != null) {
                    attendees.add(val.trim());
                }
            } else if (ICalTok.ORGANIZER.equals(token)) {
                String val = prop.getValue();
                if (val != null) {
                    organizer = val.trim();
                }
            }
        }

        // Keep originator address consistent with the address used in ORGANIZER/ATTENDEE.
        // Apple iCal is very inconsistent about the user's identity when the account has aliases.
        if (isVEventOrVTodo && originator != null && ctxt.getAuthAccount() != null) {
            AccountAddressMatcher acctMatcher = new AccountAddressMatcher(ctxt.getAuthAccount());
            String originatorEmail = stripMailto(originator);
            if (acctMatcher.matches(originatorEmail)) {
                boolean changed = false;
                if (isOrganizerMethod) {
                    if (organizer != null) {
                        String organizerEmail = stripMailto(organizer);
                        if (!organizerEmail.equalsIgnoreCase(originatorEmail) &&
                            acctMatcher.matches(organizerEmail)) {
                            originator = organizer;
                            changed = true;
                        }
                    }
                } else {
                    for (String at : attendees) {
                        String atEmail = stripMailto(at);
                        if (originatorEmail.equalsIgnoreCase(atEmail)) {
                            break;
                        } else if (acctMatcher.matches(atEmail)) {
                            originator = at;
                            changed = true;
                            break;
                        }
                    }
                }
                if (changed) {
                    ZimbraLog.dav.debug("changing originator to " + originator + " to match the address/alias used in ORGANIZER/ATTENDEE");
                }
            }
        }

        // Get the recipients.
        ArrayList<String> rcptArray = new ArrayList<String>();
        while (recipients.hasMoreElements()) {
            String rcptHdr = (String)recipients.nextElement();
            String[] rcpts = null;
            if (rcptHdr.indexOf(',') > 0) {
                rcpts = rcptHdr.split(",");
            } else {
                rcpts = new String[] { rcptHdr };
            }
            for (String rcpt : rcpts) {
                if (rcpt != null) {
                    rcpt = rcpt.trim();
                    if (rcpt.length() != 0) {
                        // Workaround for Apple iCal: Ignore attendees with address "invalid:nomail".
                        if (rcpt.equalsIgnoreCase("invalid:nomail")) {
                            continue;
                        }
                        if (isVEventOrVTodo) {
                            // Workaround for Apple iCal: Never send REQUEST/CANCEL notification to organizer.
                            // iCal can sometimes do that when organizer account has aliases.
                            if (isOrganizerMethod && rcpt.equalsIgnoreCase(organizer)) {
                                continue;
                            }
                            // bug 49987: Workaround for Apple iCal
                            // iCal sends cancel notice to all original attendees when some attendees are removed from the
                            // appointment.  As a result the appointment is cancelled from the calendars of all original
                            // attendees.  Counter this bad behavior by filtering out any recipients who aren't listed
                            // as ATTENDEE in the CANCEL component being sent.  (iCal does that part correctly, at least.)
                            if (isCancel) {
                                boolean isAttendee = false;
                                // Rcpt must be an attendee of the cancel component.
                                for (String at : attendees) {
                                    if (rcpt.equalsIgnoreCase(at)) {
                                        isAttendee = true;
                                        break;
                                    }
                                }
                                if (!isAttendee) {
                                    ZimbraLog.dav.info("Ignoring non-attendee recipient " + rcpt + " of CANCEL request; likely a client bug");
                                    continue;
                                }
                            }
                        }
                        // All checks passed.
                        rcptArray.add(rcpt);
                    }
                }
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
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("2.0;Success");
            resp.addElement(DavElements.E_CALENDAR_DATA).setText(fbMsg);
        } else {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.3;No f/b for the user");
        }
    }

    private void handleEventRequest(DavContext ctxt, ZCalendar.ZVCalendar cal, ZComponent req, String originator, String rcpt, Element resp)
    throws ServiceException, DavException {
        if (!ctxt.isSchedulingEnabled()) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.3;No scheduling for the user");
            return;
        }

        ArrayList<Address> recipients = new java.util.ArrayList<Address>();
        InternetAddress from, sender, to;
        Account target = null;
        try {
            originator = stripMailto(originator);
            sender = new JavaMailInternetAddress(originator);
            Provisioning prov = Provisioning.getInstance();
            if (ctxt.getPathInfo() != null) {
                target = prov.getAccountByName(ctxt.getPathInfo());
            }
            if (target != null) {
                from = AccountUtil.getFriendlyEmailAddress(target);
            } else {
                target = getMailbox(ctxt).getAccount();
                if (AccountUtil.addressMatchesAccount(target, originator)) {
                    // Make sure we don't use two different aliases for From and Sender.
                    // This is a concern with Apple iCal, which picks a random alias as originator.
                    from = sender;
                } else {
                    from = AccountUtil.getFriendlyEmailAddress(target);
                }
            }
            if (sender.getAddress() != null && sender.getAddress().equalsIgnoreCase(from.getAddress())) {
                sender = null;
            }
            to = new JavaMailInternetAddress(stripMailto(rcpt));
            recipients.add(to);
        } catch (AddressException e) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.7;"+rcpt);
            return;
        }
        String subject = "", uid, desc, descHtml, status, method;

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
        } else if (method.equals("REPLY")) {
            ZProperty attendeeProp = req.getProperty(ICalTok.ATTENDEE);
            if (attendeeProp == null)
                throw new DavException("missing property ATTENDEE", HttpServletResponse.SC_BAD_REQUEST);
            ZAttendee attendee = new ZAttendee(attendeeProp);
            String partStat = attendee.getPartStat();
            if (partStat.equals(IcalXmlStrMap.PARTSTAT_ACCEPTED)) {
                subject = "Accept: ";
            } else if (partStat.equals(IcalXmlStrMap.PARTSTAT_TENTATIVE)) {
                subject = "Tentative: ";
            } else if (partStat.equals(IcalXmlStrMap.PARTSTAT_DECLINED)) {
                subject = "Decline: ";
            }
        }

        if (status.equals("CANCELLED"))
            subject = "Cancelled: ";
        subject += req.getPropVal(ICalTok.SUMMARY, "");
        uid = req.getPropVal(ICalTok.UID, null);
        if (uid == null) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.1;UID");
            return;
        }
        try {
            List<Invite> components = Invite.createFromCalendar(ctxt.getAuthAccount(), null, cal, false);
            FriendlyCalendaringDescription friendlyDesc = new FriendlyCalendaringDescription(components, ctxt.getAuthAccount());
            desc = friendlyDesc.getAsPlainText();
            descHtml = req.getDescriptionHtml();
            if ((descHtml == null) || (descHtml.length() == 0))
                descHtml = friendlyDesc.getAsHtml();
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ctxt.getAuthAccount());
            MimeMessage mm = CalendarMailSender.createCalendarMessage(target, from, sender, recipients, subject, desc, descHtml, uid, cal);
            mbox.getMailSender().setSendPartial(true).sendMimeMessage(
                    ctxt.getOperationContext(), mbox, true, mm, null, null, null, null, false);
        } catch (ServiceException e) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.1");
            return;
        }
        resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
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

    private static String stripMailto(String address) {
        if (address != null && address.toLowerCase().startsWith("mailto:")) {
            return address.substring(7);
        } else {
            return address;
        }
    }
}
