/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.*;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraContext;

public class SetAppointment extends CalendarRequest {
    private static Log sLog = LogFactory.getLog(SetAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("SetAppointment");
    
    protected static class SetAppointmentInviteParser implements ParseMimeMessage.InviteParser { 
        SetAppointmentInviteParser() { };

        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            Element content = inviteElem.getOptionalElement("content");
            if (content != null) {
                ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteRaw(account, inviteElem);
                return toRet;
            } else {
                return CalendarUtils.parseInviteForCreate(account, inviteElem, null, null, false);
            }
        }
    };
    
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();
            
            sLog.info("<SetAppointment> " + lc.toString());
            
            synchronized (mbox) {
                
                Appointment appt;
                
                // First, the <default>
                {
                    Element e = request.getElement(MailService.A_DEFAULT);
                    
                    int[] ids = getParsedMessage(octxt, acct, mbox, e, new SetAppointmentInviteParser());
                    
                    appt = mbox.getAppointmentById(ids[0]);
                }
                
                Invite inv = appt.getDefaultInvite();
                
                if (inv.hasRecurId()) {
                    throw ServiceException.FAILURE("Invite id="+appt.getId()+"-"+inv.getMailItemId()+" comp="+inv.getComponentNum()+" is not the a default invite", null);
                }
                if (appt == null) {
                    throw ServiceException.FAILURE("Could not find Appointment for id="+appt.getId()+"-"+inv.getMailItemId()+" comp="+inv.getComponentNum()+">", null);
                }
                // for each <exception>
                for (Iterator iter = request.elementIterator(MailService.A_EXCEPT); iter.hasNext();) {
                    if (!appt.isRecurring()) {
                        throw ServiceException.FAILURE("Appointment "+appt.getId()+" is not a recurring appointment", null);
                    }
                    
                    Element e = (Element)iter.next();
                    
                    int ids[] = getParsedMessage(octxt, acct, mbox, e, new CreateAppointmentException.CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap()));
                    assert(ids[0] == appt.getId());
                }
                
                Element response = lc.createElement(MailService.SET_APPOINTMENT_RESPONSE);
                response.addAttribute(MailService.A_APPT_ID, appt.getId());
                return response;
            } // synchronized(mbox)
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    static private int[] getParsedMessage(OperationContext octxt, Account acct, Mailbox mbox, 
            Element e, ParseMimeMessage.InviteParser parser) throws ServiceException {
        
        boolean needsReply = e.getAttributeBool(MailService.A_APPT_NEEDS_REPLY, true);
        String partStatStr = e.getAttribute(MailService.A_APPT_PARTSTAT, "TE");
                                
        // <M>
        Element msgElem = e.getElement(MailService.E_MSG);

        
        // check to see whether the entire message has been uploaded under separate cover
        String attachmentId = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);
        
        Element contentElement = msgElem.getOptionalElement(MailService.E_CONTENT);
        
        MimeMessage mm = null;
        
        if (attachmentId != null) {
            ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
            mm = SendMsg.parseUploadedMessage(mbox, attachmentId, mimeData);
        } else if (contentElement != null) {
            mm = ParseMimeMessage.importMsgSoap(msgElem);
        } else {
            CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbox, parser);
            mm = dat.mMm;
        }
        
        ParsedMessage pm = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());
        
        pm.analyze();
        net.fortuna.ical4j.model.Calendar cal = pm.getiCalendar();
        if (cal == null) {
            throw ServiceException.FAILURE("SetAppointment could not find an iCalendar part for <default>", null);
        }
        
        boolean sentByMe = false; // not applicable in the SetAppointment case
        
        Invite except = Invite.createFromICalendar(acct, pm.getFragment(), cal, sentByMe);
        
        int[] ids = mbox.addInvite(null, except, true, pm);
        
        mbox.modifyInvitePartStat(octxt, ids[0], ids[1], 0, needsReply, partStatStr);
        
        return ids;
    }
    
    protected static int sendThenDeleteCalendarMessage(OperationContext octxt, Account acct, Mailbox mbox, CalSendData dat) throws ServiceException
    {
        int msgId = sendMimeMessage(octxt, mbox, acct, Mailbox.ID_FOLDER_CALENDAR, dat, dat.mMm, dat.mOrigId, dat.mReplyType);
        
//        mbox.delete(octxt, msgId, MailItem.TYPE_MESSAGE);
        
        return msgId;
    }
    
}
