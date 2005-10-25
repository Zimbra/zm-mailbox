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

import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SetAppointmentData;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class SetAppointment extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(SetAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("SetAppointment");

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.A_FOLDER };
    protected String[] getProxiedIdPath()     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy()  { return true; }

    protected static class SetAppointmentInviteParser extends ParseMimeMessage.InviteParser {
        
        private boolean mExceptOk = false;
        
        SetAppointmentInviteParser(boolean exceptOk) { mExceptOk = exceptOk; };

        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraContext lc, Account account, Element inviteElem) throws ServiceException 
        {
            Element content = inviteElem.getOptionalElement("content");
            if (content != null) {
                ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteRaw(account, inviteElem);
                return toRet;
            } else {
                return CalendarUtils.parseInviteForCreate(account, inviteElem, null, null, mExceptOk, CalendarUtils.RECUR_ALLOWED);
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
            
            ItemId iidFolder = new ItemId(request.getAttribute(MailService.A_FOLDER, CreateAppointment.DEFAULT_FOLDER), lc);
            
            sLog.info("<SetAppointment> " + lc.toString());
            
            SetAppointmentData defaultData;
            ArrayList /* SetAppointmentData */ exceptions = new ArrayList();
            
            synchronized (mbox) {
                // First, the <default>
                {
                    Element e = request.getElement(MailService.A_DEFAULT);
                    defaultData = getSetAppointmentData(lc, acct, mbox, e, new SetAppointmentInviteParser(false));
                }
                
                // for each <exception>
                for (Iterator iter = request.elementIterator(MailService.A_EXCEPT); iter.hasNext(); ) {
                    Element e = (Element) iter.next();
                    SetAppointmentData exDat = getSetAppointmentData(lc, acct, mbox, e, new SetAppointmentInviteParser(true));
                    exceptions.add(exDat);
                }
                
                
                SetAppointmentData[] exceptArray = null;
                if (exceptions.size() > 0) {
                    exceptArray = new SetAppointmentData[exceptions.size()];
                    exceptions.toArray(exceptArray);
                }
                
                int apptId = mbox.setAppointment(octxt, iidFolder.getId(), defaultData, exceptArray);
                
                Element response = lc.createElement(MailService.SET_APPOINTMENT_RESPONSE);

                response.addElement(MailService.A_DEFAULT)
                        .addAttribute(MailService.A_ID, lc.formatItemId(defaultData.mInv.getMailItemId()));
                
                for (Iterator iter = exceptions.iterator(); iter.hasNext();) {
                    SetAppointmentData cur = (SetAppointmentData) iter.next();
                    Element e = response.addElement(MailService.A_EXCEPT);
                    e.addAttribute(MailService.A_APPT_RECURRENCE_ID, cur.mInv.getRecurId().toString());
                    e.addAttribute(MailService.A_ID, lc.formatItemId(cur.mInv.getMailItemId()));
                }
                response.addAttribute(MailService.A_APPT_ID, lc.formatItemId(apptId));
                
                return response;
            } // synchronized(mbox)
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    static private SetAppointmentData getSetAppointmentData(ZimbraContext lc, Account acct, Mailbox mbox, 
                                                            Element e, ParseMimeMessage.InviteParser parser)
    throws ServiceException {
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
            mm = SendMsg.parseUploadedMessage(lc, attachmentId, mimeData);
        } else if (contentElement != null) {
            mm = ParseMimeMessage.importMsgSoap(msgElem);
        } else {
            CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);
            mm = dat.mMm;
        }
        
        ParsedMessage pm = new ParsedMessage(mm, mbox.attachmentsIndexingEnabled());
        
        pm.analyze();
        net.fortuna.ical4j.model.Calendar cal = pm.getiCalendar();
        if (cal == null) {
            throw ServiceException.FAILURE("SetAppointment could not find an iCalendar part for <default>", null);
        }
        
        boolean sentByMe = false; // not applicable in the SetAppointment case
        
        Invite inv = (Invite)(Invite.createFromICalendar(acct, pm.getFragment(), cal, sentByMe).get(0));
        
        inv.modifyPartStatInMemory(needsReply, partStatStr);
        
        SetAppointmentData sadata = new SetAppointmentData();
        sadata.mInv = inv;
        sadata.mPm = pm;
        sadata.mForce = true;
        return sadata;
    }
}
