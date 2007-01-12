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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.MsgKey;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class CancelCalendarItem extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(CancelCalendarItem.class);
    private static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_ITEM_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), lc);
        int compNum = (int) request.getAttributeLong(MailConstants.E_INVITE_COMPONENT);
        
        sLog.info("<CancelCalendarItem id=" + iid + " comp=" + compNum + ">");
        
        synchronized (mbox) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId()); 
            if (calItem == null)
                throw MailServiceException.NO_SUCH_CALITEM(iid.getId(), " for CancelCalendarItemRequest(" + iid + "," + compNum + ")");
            Invite inv = calItem.getInvite(iid.getSubpartId(), compNum);

            Element recurElt = request.getOptionalElement(MailConstants.E_INSTANCE);
            if (recurElt != null) {
                RecurId recurId = CalendarUtils.parseRecurId(recurElt, inv.getTimeZoneMap(), inv);
                cancelInstance(lc, request, acct, mbox, calItem, inv, recurId);
            } else {
                
                // if recur is not set, then we're cancelling the entire calendar item...
                
                // first, pull a list of all the invites and THEN start cancelling them: since cancelling them
                // will remove them from the calendar item's list, we can get really confused if we just directly
                // iterate through the list...
                
                Invite invites[] = new Invite[calItem.numInvites()];
                for (int i = calItem.numInvites()-1; i >= 0; i--) {
                    invites[i] = calItem.getInvite(i);
                }
                
                for (int i = invites.length-1; i >= 0; i--) {
                    if (invites[i] != null && 
                        (invites[i].getMethod().equals(ICalTok.REQUEST.toString()) ||
                            invites[i].getMethod().equals(ICalTok.PUBLISH.toString()))
                    ) {
                        cancelInvite(lc, request, acct, mbox, calItem, invites[i]);
                    }
                }
            }
        } // synchronized on mailbox
        
        Element response = getResponseElement(lc);
        return response;
    }        
    
    void cancelInstance(ZimbraSoapContext lc, Element request, Account acct, Mailbox mbox, CalendarItem calItem, Invite defaultInv, RecurId recurId) 
    throws ServiceException {
        boolean onBehalfOf = lc.isDelegatedRequest();
        Account authAcct = lc.getAuthtokenAccount();
        Locale locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();
        String text = L10nUtil.getMessage(MsgKey.calendarCancelAppointmentInstance, locale);

        if (sLog.isDebugEnabled()) {
            sLog.debug("Sending cancellation message for \"" + defaultInv.getName() + "\" for instance " + recurId + " of invite " + defaultInv);
        }

        Invite cancelInvite = CalendarUtils.buildCancelInstanceCalendar(acct, authAcct.getName(), onBehalfOf, defaultInv, text, recurId);
        CalSendData dat = new CalSendData();
        dat.mOrigId = defaultInv.getMailItemId();
        dat.mReplyType = MailSender.MSGTYPE_REPLY;
        dat.mInvite = cancelInvite;

        ZVCalendar iCal = dat.mInvite.newToICalendar();

        // did they specify a custom <m> message?  If so, then we don't have to build one...
        Element msgElem = request.getOptionalElement(MailConstants.E_MSG);

        if (msgElem != null) {
            String desc = ParseMimeMessage.getTextPlainContent(msgElem);
            iCal.addDescription(desc);

            MimeBodyPart[] mbps = new MimeBodyPart[1];
            mbps[0] = CalendarMailSender.makeICalIntoMimePart(defaultInv.getUid(), iCal);

            // the <inv> element is *NOT* allowed -- we always build it manually
            // based on the params to the <CancelCalendarItem> and stick it in the 
            // mbps (additionalParts) parameter...
            dat.mMm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, mbps, 
                    ParseMimeMessage.NO_INV_ALLOWED_PARSER, dat);
            
        } else {
            List<Address> rcpts =
                CalendarMailSender.toListFromAttendees(defaultInv.getAttendees());
            dat.mMm = CalendarMailSender.createCancelMessage(
                    acct, rcpts, onBehalfOf, authAcct,
                    calItem, cancelInvite, text, iCal);
        }
        
        if (!defaultInv.thisAcctIsOrganizer(acct)) {
            try {
                Address[] rcpts = dat.mMm.getAllRecipients();
                if (rcpts != null && rcpts.length > 0) {
                    throw MailServiceException.MUST_BE_ORGANIZER("CancelCalendarItem");
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }
        
        sendCalendarCancelMessage(lc, calItem.getFolderId(),
                                  acct, mbox, dat, true);
    }

    protected void cancelInvite(ZimbraSoapContext lc, Element request, Account acct, Mailbox mbox, CalendarItem calItem, Invite inv)
    throws ServiceException {
        boolean onBehalfOf = lc.isDelegatedRequest();
        Account authAcct = lc.getAuthtokenAccount();
        Locale locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();
        String text = L10nUtil.getMessage(MsgKey.calendarCancelAppointment, locale);

        if (sLog.isDebugEnabled())
            sLog.debug("Sending cancellation message for \"" + inv.getName() + "\" for " + inv.toString());
        
        CalSendData csd = new CalSendData();
        csd.mOrigId = inv.getMailItemId();
        csd.mReplyType = MailSender.MSGTYPE_REPLY;
        csd.mInvite = CalendarUtils.buildCancelInviteCalendar(acct, authAcct.getName(), onBehalfOf, inv, text);
        
        ZVCalendar iCal = csd.mInvite.newToICalendar();
        
        
        // did they specify a custom <m> message?  If so, then we don't have to build one...
        Element msgElem = request.getOptionalElement(MailConstants.E_MSG);
        
        if (msgElem != null) {
            String desc = ParseMimeMessage.getTextPlainContent(msgElem);
            iCal.addDescription(desc);

            MimeBodyPart[] mbps = new MimeBodyPart[1];
            mbps[0] = CalendarMailSender.makeICalIntoMimePart(inv.getUid(), iCal);
            
            // the <inv> element is *NOT* allowed -- we always build it manually
            // based on the params to the <CancelCalendarItem> and stick it in the 
            // mbps (additionalParts) parameter...
            csd.mMm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, mbps, 
                    ParseMimeMessage.NO_INV_ALLOWED_PARSER, csd);
            
        } else {
            List<Address> rcpts =
                CalendarMailSender.toListFromAttendees(inv.getAttendees());
            csd.mMm = CalendarMailSender.createCancelMessage(
                    acct, rcpts, onBehalfOf, authAcct,
                    calItem, inv, text, iCal);
        }
        
        if (!inv.thisAcctIsOrganizer(acct)) {
            try {
                Address[] rcpts = csd.mMm.getAllRecipients();
                if (rcpts != null && rcpts.length > 0) {
                    throw MailServiceException.MUST_BE_ORGANIZER("CancelCalendarItem");
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }
        
        sendCalendarCancelMessage(lc, calItem.getFolderId(),
                                  acct, mbox, csd, true);
    }
}
