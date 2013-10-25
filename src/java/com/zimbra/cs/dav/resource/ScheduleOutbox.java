/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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
import java.util.Set;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
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
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.CalendarItem;
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
    private String eventOrganizer;

    public ScheduleOutbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
        super(ctxt, f);
        addResourceType(DavElements.E_SCHEDULE_OUTBOX);
    }

    @Override
    public void handlePost(DavContext ctxt) throws DavException, IOException, ServiceException {
        DelegationInfo delegationInfo = new DelegationInfo(ctxt.getRequest().getHeader(DavProtocol.HEADER_ORIGINATOR));
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
        if (req == null) {
            throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST);
        }
        ZimbraLog.dav.debug("originator: %s", delegationInfo.getOriginator());

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
                    String addr = CalDavUtils.stripMailto(organizer);
                    // Rewrite the alias to primary address
                    Account acct = Provisioning.getInstance().get(AccountBy.name, addr);
                    if (acct != null) {
                        String newAddr = acct.getName();
                        if (!addr.equals(newAddr)) {
                            organizer = "mailto:" + newAddr;
                            prop.setValue(organizer);
                        }
                    }
                }
            }
        }

        // Keep originator address consistent with the address used in ORGANIZER/ATTENDEE.
        // Apple iCal is very inconsistent about the user's identity when the account has aliases.
        if (isVEventOrVTodo && delegationInfo.getOriginator() != null && ctxt.getAuthAccount() != null) {
            AccountAddressMatcher acctMatcher = new AccountAddressMatcher(ctxt.getAuthAccount());
            if (acctMatcher.matches(delegationInfo.getOriginatorEmail())) {
                boolean changed = false;
                if (isOrganizerMethod) {
                    if (organizer != null) {
                        String organizerEmail = CalDavUtils.stripMailto(organizer);
                        if (!organizerEmail.equalsIgnoreCase(delegationInfo.getOriginatorEmail()) &&
                            acctMatcher.matches(organizerEmail)) {
                            delegationInfo.setOriginator(organizer);
                            changed = true;
                        }
                    }
                } else {
                    for (String at : attendees) {
                        String atEmail = CalDavUtils.stripMailto(at);
                        if (delegationInfo.getOriginatorEmail().equalsIgnoreCase(atEmail)) {
                            break;
                        } else if (acctMatcher.matches(atEmail)) {
                            delegationInfo.setOriginator(at);
                            changed = true;
                            break;
                        }
                    }
                }
                if (changed) {
                    ZimbraLog.dav.debug("changing originator to %s to match address/alias used in ORGANIZER/ATTENDEE",
                            delegationInfo.getOriginator());
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
                handleFreebusyRequest(ctxt, req, delegationInfo.getOriginator(), rcpt, resp);
                break;
            case VEVENT:
                // adjustOrganizer works around issues where we don't have delegation enabled but clients write
                // to the shared calendar as if it was the delegate's calendar instead of the owner.
                // Note assumption that clients won't generate replies on behalf of owner as they won't be aware
                // that they should be working on behalf of the owner and will only look for their own attendee
                // records.
                if (isOrganizerMethod) {
                    adjustOrganizer(ctxt, vcalendar, req, delegationInfo);
                }
                handleEventRequest(ctxt, vcalendar, req, delegationInfo, rcpt, resp);
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

    private void handleEventRequest(DavContext ctxt, ZCalendar.ZVCalendar cal, ZComponent req,
            DelegationInfo delegationInfo, String rcpt, Element resp)
    throws ServiceException, DavException {
        if (!DavResource.isSchedulingEnabled()) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("5.3;No scheduling for the user");
            return;
        }

        ArrayList<Address> recipients = new java.util.ArrayList<Address>();
        InternetAddress from, sender, to;
        Account target = null;
        try {
            sender = new JavaMailInternetAddress(delegationInfo.getOriginatorEmail());
            Provisioning prov = Provisioning.getInstance();
            if (ctxt.getPathInfo() != null) {
                target = prov.getAccountByName(ctxt.getPathInfo());
            }
            if (target != null) {
                from = AccountUtil.getFriendlyEmailAddress(target);
            } else {
                if (delegationInfo.getOwnerEmail() != null) {
                    from = new JavaMailInternetAddress(delegationInfo.getOwnerEmail());
                } else {
                    target = getMailbox(ctxt).getAccount();
                    if (AccountUtil.addressMatchesAccount(target, delegationInfo.getOriginatorEmail())) {
                        // Make sure we don't use two different aliases for From and Sender.
                        // This is a concern with Apple iCal, which picks a random alias as originator.
                        from = sender;
                    } else {
                        from = AccountUtil.getFriendlyEmailAddress(target);
                    }
                }
            }
            if (sender.getAddress() != null && sender.getAddress().equalsIgnoreCase(from.getAddress())) {
                sender = null;
            }
            to = new JavaMailInternetAddress(CalDavUtils.stripMailto(rcpt));
            recipients.add(to);
        } catch (AddressException e) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.7;"+rcpt);
            return;
        }
        String status = req.getPropVal(ICalTok.STATUS, "");
        String method = cal.getPropVal(ICalTok.METHOD, "REQUEST");
        String subject = "";
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
        String uid = req.getPropVal(ICalTok.UID, null);
        if (uid == null) {
            resp.addElement(DavElements.E_RECIPIENT).addElement(DavElements.E_HREF).setText(rcpt);
            resp.addElement(DavElements.E_REQUEST_STATUS).setText("3.1;UID");
            return;
        }
        try {
            List<Invite> components = Invite.createFromCalendar(ctxt.getAuthAccount(), null, cal, false);
            FriendlyCalendaringDescription friendlyDesc = new FriendlyCalendaringDescription(components, ctxt.getAuthAccount());
            String desc = friendlyDesc.getAsPlainText();
            String descHtml = req.getDescriptionHtml();
            if ((descHtml == null) || (descHtml.length() == 0))
                descHtml = friendlyDesc.getAsHtml();
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ctxt.getAuthAccount());
            MimeMessage mm = CalendarMailSender.createCalendarMessage(target, from, sender,
                    recipients, subject, desc, descHtml, uid, cal);
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

    /**
     * For Vanilla CalDAV access where Apple style delegation has not been enabled, attempts by the delegate
     * to use a shared calendar acting as themselves are translated to appear as if acting as a delegate,
     * otherwise the experience can be very poor.
     * @throws ServiceException
     */
    private void adjustOrganizer(DavContext ctxt, ZCalendar.ZVCalendar cal, ZComponent req,
            DelegationInfo delegationInfo)
    throws ServiceException {
        // BusyCal 2.5.3 seems to post with the wrong organizer even if ical delegation is switched on - even though
        // it uses the right ORGANIZER in the calendar entry.
        // if (ctxt.useIcalDelegation()) { return; }
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ctxt.getAuthAccount());
        String uid = req.getPropVal(ICalTok.UID, null);
        CalendarItem matchingCalendarEntry = mbox.getCalendarItemByUid(ctxt.getOperationContext(), uid);
        if (matchingCalendarEntry == null) {
            List<com.zimbra.cs.mailbox.Mountpoint> sharedCalendars =
                    mbox.getCalendarMountpoints(ctxt.getOperationContext(), SortBy.NONE);
            if (sharedCalendars == null) {
                return;  // Can't work out anything useful
            }
            Set<Account> accts = Sets.newHashSet();
            for (com.zimbra.cs.mailbox.Mountpoint sharedCalendar : sharedCalendars) {
                accts.add(Provisioning.getInstance().get(AccountBy.id, sharedCalendar.getOwnerId()));
            }
            for (Account acct : accts) {
                Mailbox sbox = MailboxManager.getInstance().getMailboxByAccount(acct);
                matchingCalendarEntry = sbox.getCalendarItemByUid(ctxt.getOperationContext(), uid);
                if (matchingCalendarEntry != null) {
                    break;
                }
            }
        }
        if (matchingCalendarEntry == null) {
            return;
        }
        Invite[] invites = matchingCalendarEntry.getInvites();
        if (invites == null) {
            return;
        }
        for (Invite inv : invites) {
            ZOrganizer org = inv.getOrganizer();
            if (org != null) {
                delegationInfo.setOwner(org.getAddress());
                if (Strings.isNullOrEmpty(org.getCn())) {
                    Account ownerAcct = Provisioning.getInstance().get(AccountBy.name, org.getAddress());
                    if (! Strings.isNullOrEmpty(ownerAcct.getDisplayName())) {
                        delegationInfo.setOwnerCn(ownerAcct.getDisplayName());
                    }
                } else {
                    delegationInfo.setOwnerCn(org.getCn());
                }
                break;
            }
        }
        if (delegationInfo.getOwner() == null) {
            return;
        }
        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(ctxt.getAuthAccount());
        boolean originatorIsCalEntryOrganizer = acctMatcher.matches(delegationInfo.getOwnerEmail());
        if (originatorIsCalEntryOrganizer) {
            return;
        }
        for (ZComponent component : cal.getComponents()) {
            ZProperty organizerProp = component.getProperty(ICalTok.ORGANIZER);
            if (organizerProp != null) {
                organizerProp.setValue(delegationInfo.getOwner());
                ZParameter cn = organizerProp.getParameter(ICalTok.CN);
                if (cn == null) {
                    organizerProp.addParameter(new ZParameter(ICalTok.CN, delegationInfo.getOwnerCn()));
                } else {
                    cn.setValue(delegationInfo.getOwnerCn());
                }
                ZParameter sentBy = organizerProp.getParameter(ICalTok.SENT_BY);
                if (sentBy == null) {
                    organizerProp.addParameter(new ZParameter(ICalTok.SENT_BY, delegationInfo.getOriginator()));
                } else {
                    sentBy.setValue(delegationInfo.getOriginator());
                }
            }
        }
    }

    private class DelegationInfo {
        private String originator;
        private String originatorEmail;
        private String owner;
        private String ownerEmail;
        private String ownerCn;
        DelegationInfo(String originator) {
            this.setOriginator(originator);
        }
        String getOriginator() { return originator; }
        void setOriginator(String originator) {
            this.originator = originator;
            this.originatorEmail = CalDavUtils.stripMailto(originator);
        }
        String getOriginatorEmail() { return originatorEmail; }
        String getOwner() { return owner; }
        void setOwner(String owner) {
            this.owner = owner;
            this.ownerEmail = CalDavUtils.stripMailto(owner);
        }
        String getOwnerEmail() {
            return ownerEmail;
        }
        String getOwnerCn() { return Strings.isNullOrEmpty(ownerCn) ? owner : ownerCn; }
        void setOwnerCn(String ownerCn) { this.ownerCn = ownerCn; }
    }

}
