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

/*
 * Created on Mar 2, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender.Verb;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author tim
 */
public class SendInviteReply extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(SendInviteReply.class);
    
    private static final String[] TARGET_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Account acct = getRequestedAccount(lc);
        Account authAcct = lc.getAuthtokenAccount();
        OperationContext octxt = lc.getOperationContext();

        boolean onBehalfOf = lc.isDelegatedRequest();

        ItemId iid = new ItemId(request.getAttribute(MailService.A_ID), lc);
        int compNum = (int) request.getAttributeLong(MailService.A_CAL_COMPONENT_NUM);
        
        String verbStr = request.getAttribute(MailService.A_VERB);
        Verb verb = CalendarMailSender.parseVerb(verbStr);
        
        boolean updateOrg = request.getAttributeBool(MailService.A_CAL_UPDATE_ORGANIZER, true);
        
        if (sLog.isInfoEnabled()) {
            sLog.info("<SendInviteReply id=" + lc.formatItemId(iid) + " verb=" + verb + " updateOrg=" + updateOrg + "> " + lc.toString());
        }
        
        Element response = getResponseElement(lc);
        
        synchronized (mbox) {
            
            Invite oldInv = null;
            int calItemId; 
            int inviteMsgId;
            CalendarItem calItem;
            
            // the user could be accepting EITHER the original-mail-item (id="nnn") OR the
            // calendar item (id="aaaa-nnnn") --- work in both cases
            if (iid.hasSubpart()) {
                // directly accepting the calendar item
                calItemId = iid.getId();
                inviteMsgId = iid.getSubpartId();
                calItem = mbox.getCalendarItemById(octxt, calItemId); 
                if (calItem == null)
                	throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
                oldInv = calItem.getInvite(inviteMsgId, compNum);
            } else {
                // accepting the message: go find the calendar item and then the invite
                inviteMsgId = iid.getId();
                Message msg = mbox.getMessageById(octxt, inviteMsgId);
                Message.CalendarItemInfo info = msg.getCalendarItemInfo(compNum);
                if (info == null)
                	throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
                calItemId = info.getCalendarItemId();
                calItem = mbox.getCalendarItemById(octxt, calItemId);
                if (calItem == null)
                	throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
                oldInv = calItem.getInvite(inviteMsgId, compNum);  
            }
            if (oldInv == null)
            	throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            
            if ((mbox.getEffectivePermissions(octxt, calItemId, MailItem.TYPE_UNKNOWN) & ACL.RIGHT_ACTION) == 0)
                throw ServiceException.PERM_DENIED("You do not have ACTION rights for CalendarItem "+calItemId);
            
            
            // see if there is a specific Exception being referenced by this reply...
            Element exc = request.getOptionalElement(MailService.E_CAL_EXCEPTION_ID);
            ParsedDateTime exceptDt = null;
            if (exc != null) {
                exceptDt = CalendarUtils.parseDateTime(exc,
                                                       oldInv.getTimeZoneMap(),
                                                       oldInv);
            } else if (oldInv.hasRecurId()) {
                exceptDt = oldInv.getRecurId().getDt();
            }

            if (updateOrg && oldInv.hasOrganizer()) {
                Locale locale;
                Account organizer = oldInv.getOrganizerAccount();
                if (organizer != null)
                    locale = organizer.getLocale();
                else
                    locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();
                String replySubject =
                    CalendarMailSender.getReplySubject(verb, oldInv, locale);

                CalSendData csd = new CalSendData(acct, authAcct, onBehalfOf);
                csd.mOrigId = oldInv.getMailItemId();
                csd.mReplyType = MailSender.MSGTYPE_REPLY;
                csd.mInvite = CalendarMailSender.replyToInvite(acct, authAcct, onBehalfOf, oldInv, verb, replySubject, exceptDt);
                
                ZVCalendar iCal = csd.mInvite.newToICalendar();
                
                ParseMimeMessage.MimeMessageData parsedMessageData = new ParseMimeMessage.MimeMessageData();
                
                // did they specify a custom <m> message?  If so, then we don't have to build one...
                Element msgElem = request.getOptionalElement(MailService.E_MSG);
                if (msgElem != null) {
                    String text = ParseMimeMessage.getTextPlainContent(msgElem);
                    iCal.addDescription(text);

                    MimeBodyPart[] mbps = new MimeBodyPart[1];
                    mbps[0] = CalendarMailSender.makeICalIntoMimePart(oldInv.getUid(), iCal);

                    // the <inv> element is *NOT* allowed -- we always build it manually
                    // based on the params to the <SendInviteReply> and stick it in the 
                    // mbps (additionalParts) parameter...
                    csd.mMm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, mbps, 
                        ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);
                } else {
                    // build a default "Accepted" response
                    csd.mMm = CalendarMailSender.createDefaultReply(
                            acct, authAcct, onBehalfOf, calItem, oldInv, null, replySubject,
                            verb, null, iCal);
                }

                sendCalendarMessage(lc, calItem.getFolderId(), acct, mbox, csd, response, false);
            }

            RecurId recurId = null;
            if (exceptDt != null) {
                recurId = new RecurId(exceptDt, RecurId.RANGE_NONE);
            }
            ZAttendee me = oldInv.getMatchingAttendee(acct);
            String cnStr = null;
            String addressStr = acct.getName();
            String role = IcalXmlStrMap.ROLE_OPT_PARTICIPANT;
            int seqNo = oldInv.getSeqNo();
            long dtStamp = oldInv.getDTStamp();
            if (me != null) { 
                if (me.hasCn()) {
                    cnStr = me.getCn();
                }
                addressStr = me.getAddress();
                if (me.hasRole()) {
                    role = me.getRole();
                }
            }
            
            mbox.modifyPartStat(octxt, calItemId, recurId, cnStr, addressStr, null, role, verb.getXmlPartStat(), Boolean.FALSE, seqNo, dtStamp);
            
            // move the invite to the Trash if the user wants it
            if (acct.getBooleanAttr(Provisioning.A_zimbraPrefDeleteInviteOnReply, true)) {
                try {
                    if (onBehalfOf) {
                        // HACK: Run the move in the context of the organizer
                        // mailbox because the authenticated account doesn't
                        // have rights on Inbox and Trash folders.
                        octxt = new OperationContext(mbox);
                    }
                    mbox.move(octxt, inviteMsgId, MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_TRASH);
                } catch (MailServiceException.NoSuchItemException nsie) {
                    sLog.debug("can't move nonexistent invite to Trash: " + inviteMsgId);
                }
            }
        }
        
        return response;
    }
}
