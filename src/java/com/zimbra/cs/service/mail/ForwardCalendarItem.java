/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public class ForwardCalendarItem extends CalendarRequest {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // proxy handling

        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);
        if (!iid.belongsTo(acct)) {
            // Proxy it.
            return proxyRequest(request, context, iid.getAccountId());
        }

        Element msgElem = request.getElement(MailConstants.E_MSG);
        ParseMimeMessage.MimeMessageData parsedMessageData = new ParseMimeMessage.MimeMessageData();
        MimeMessage mm =
            ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, 
                null, ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);

        MimeBodyPart[] fwdParts = null;
        synchronized(mbox) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            }

            Account authAcct = getAuthenticatedAccount(zsc);
            boolean hidePrivate =
                !calItem.isPublic() && !calItem.allowPrivateAccess(authAcct, zsc.isUsingAdminPrivileges());
            if (hidePrivate)
                throw ServiceException.PERM_DENIED("Can't forward an unowned private appointment");

            Element exc = request.getOptionalElement(MailConstants.E_CAL_EXCEPTION_ID);
            if (exc == null) {
                // Forwarding entire appointment
                fwdParts = getSeriesFwdParts(octxt, acct, calItem);
            } else {
                // Forwarding an instance
                TimeZoneMap tzmap = calItem.getTimeZoneMap();
                Element tzElem = request.getOptionalElement(MailConstants.E_CAL_TZ);
                ICalTimeZone tz = null;
                if (tzElem != null) {
                    tz = CalendarUtils.parseTzElement(tzElem);
                    tzmap.add(tz);
                }
                ParsedDateTime exceptDt = CalendarUtils.parseDateTime(exc, tzmap);
                RecurId rid = new RecurId(exceptDt, RecurId.RANGE_NONE);

                Invite inv = calItem.getInvite(rid);
                MimeMessage mmInv = null;
                if (inv != null) {
                    mmInv = calItem.getSubpartMessage(inv.getMailItemId());
                } else {
                    if (rid != null) {
                        // No invite found matching the RECURRENCE-ID.  It must be a non-exception instance.
                        // Create an invite based on the series invite.
                        Invite seriesInv = calItem.getDefaultInviteOrNull();
                        if (seriesInv == null)
                            throw ServiceException.INVALID_REQUEST("Instance specified but no recurrence series found", null);
                        Invite exceptInv = seriesInv.newCopy();
                        exceptInv.setRecurrence(null);
                        exceptInv.setRecurId(rid);
                        long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                        exceptInv.setDtStamp(now);
                        ParsedDateTime dtStart = rid.getDt();
                        ParsedDateTime dtEnd = dtStart.add(exceptInv.getEffectiveDuration());
                        exceptInv.setDtStart(dtStart);
                        exceptInv.setDtEnd(dtEnd);
                        inv = exceptInv;

                        // Carry over the MimeMessage/ParsedMessage to preserve any attachments.
                        mmInv = calItem.getSubpartMessage(seriesInv.getMailItemId());
                    } else {
                        // No RECURRENCE-ID given and no invite found.
                        throw ServiceException.INVALID_REQUEST("Invite not found for the requested RECURRENCE-ID", null);
                    }
                }
                MimeBodyPart fwdPart = getInstanceFwdPart(acct, calItem, inv, mmInv);
                fwdParts = new MimeBodyPart[] { fwdPart };
            }
        }

        for (MimeBodyPart fwdMsg : fwdParts) {
            MimeMessage mmAttached = null;
            try {
                mmAttached = attachFwdPart(mm, fwdMsg);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("error creating forward message", e);
            }
            mbox.getMailSender().sendMimeMessage(octxt, mbox, mmAttached, null, null,
                    null, null, null, true, true);
        }

        Element response = getResponseElement(zsc);
        return response;
    }

    private static MimeMessage attachFwdPart(MimeMessage fwdWrapper, MimeBodyPart forwardedMsg)
    throws MessagingException {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        Enumeration e = fwdWrapper.getAllHeaderLines();
        while (e.hasMoreElements()) {
            String hdrLine = (String) e.nextElement();
            mm.addHeaderLine(hdrLine);
        }
        mm.removeHeader("Message-ID");  // clear message-id to force regeneration of unique value during send
        mm.removeHeader("Content-Type");
        MimeMultipart mmp = new MimeMultipart("mixed");
        mm.setContent(mmp);
        MimeBodyPart mbp1 = new MimeBodyPart();
        mbp1.setDataHandler(fwdWrapper.getDataHandler());
        mbp1.setHeader("Content-Type", fwdWrapper.getContentType());
        mmp.addBodyPart(mbp1);
        mmp.addBodyPart(forwardedMsg);
        mm.saveChanges();
        return mm;
    }

    private static MimeBodyPart[] getSeriesFwdParts(
            OperationContext octxt, Account acct, CalendarItem calItem)
    throws ServiceException {
        try {
            List<MimeBodyPart> parts = new ArrayList<MimeBodyPart>();
            long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
            Invite[] invites = calItem.getInvites();
            // Get canceled instances in the future.  These will be included in the series part.
            List<Invite> cancels = new ArrayList<Invite>();
            for (Invite inv : invites) {
                if (inv.isCancel() && inv.hasRecurId() && inviteIsAfterTime(inv, now))
                    cancels.add(inv);
            }
            boolean didCancels = false;
            for (Invite inv : invites) {
                // Ignore exceptions in the past.
                if (inv.hasRecurId() && !inviteIsAfterTime(inv, now))
                    continue;

                if (!inv.isCancel()) {
                    // Make the new iCalendar part.
                    ZVCalendar cal = inv.newToICalendar(true);
                    // For series invite, append the canceled instances.
                    if (inv.isRecurrence() && !didCancels) {
                        didCancels = true;
                        for (Invite cancel : cancels) {
                            ZComponent cancelComp = cancel.newToVComponent(true, true);
                            cal.addComponent(cancelComp);
                        }
                    }

                    MimeMessage mmInv = calItem.getSubpartMessage(inv.getMailItemId());
                    MimeBodyPart mbp = makeFwdPart(acct, calItem, inv, mmInv, cal);
                    parts.add(mbp);
                }
            }
            return parts.toArray(new MimeBodyPart[0]);
        } catch (IOException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        }
    }

    private static MimeBodyPart getInstanceFwdPart(Account acct, CalendarItem calItem, Invite inv, MimeMessage mmInv)
    throws ServiceException {
        try {
            ZVCalendar cal = inv.newToICalendar(true);
            return makeFwdPart(acct, calItem, inv, mmInv, cal);
        } catch (IOException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        }
    }

    private static MimeBodyPart makeFwdPart(Account acct, CalendarItem calItem, Invite inv, MimeMessage mmInv, ZVCalendar cal)
    throws ServiceException, MessagingException, IOException {
        Address from = null;
        Address sender = null;
        List<Address> rcpts = new ArrayList<Address>();
        if (mmInv != null) {
            Address[] fromAddrs = mmInv.getFrom();
            if (fromAddrs != null)
                from = fromAddrs[0];
            sender = mmInv.getSender();
            Address[] toAddrs = mmInv.getAllRecipients();
            if (toAddrs != null) {
                for (Address to : toAddrs)
                    rcpts.add(to);
            }
        } else {
            Address me = AccountUtil.getFriendlyEmailAddress(acct);
            ZOrganizer org = inv.getOrganizer();
            if (org != null) {
                if (org.hasCn())
                    from = new InternetAddress(org.getAddress(), org.getCn(), MimeConstants.P_CHARSET_UTF8);
                else
                    from = new InternetAddress(org.getAddress());
                if (org.hasSentBy())
                    sender = new InternetAddress(org.getSentBy());
            } else {
                from = me;
            }
            rcpts.add(me);
        }
        MimeMessage mm = CalendarMailSender.createCalendarMessage(from, sender, rcpts, mmInv, inv, cal);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            mm.writeTo(out);
        } finally {
            out.close();
        }
        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new ByteArrayDataSource(out.toByteArray(), MimeConstants.CT_MESSAGE_RFC822)));
        mbp.setHeader("Content-Type", MimeConstants.CT_MESSAGE_RFC822);
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        return mbp;
    }
}
