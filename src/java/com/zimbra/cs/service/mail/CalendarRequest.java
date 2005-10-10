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

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message.ApptInfo;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public abstract class CalendarRequest extends SendMsg {

    protected static class CalSendData extends ParseMimeMessage.MimeMessageData {
        int mOrigId; // orig id if this is a reply
        String mReplyType; 
        MimeMessage mMm;
        boolean mSaveToSent;
        Invite mInvite;
    }

    protected static CalSendData handleMsgElement(OperationContext octxt, Element msgElem, Account acct,
                                                  Mailbox mbox, ParseMimeMessage.InviteParser inviteParser)
    throws ServiceException {

        CalSendData toRet = new CalSendData();
        
        if (inviteParser.getResult() != null) {
            assert(inviteParser.getResult() == null);
        }
        
        // check to see if this message is a reply -- if so, then we'll want to note that so 
        // we can more-correctly match the conversations up
        toRet.mOrigId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        toRet.mReplyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, TYPE_REPLY);

        // parse the data
        toRet.mMm = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, null, inviteParser, toRet);
        
        if (inviteParser.getResult() == null || inviteParser.getResult().mInvite == null) {
            assert(inviteParser.getResult() != null);
        }
        toRet.mInvite = inviteParser.getResult().mInvite;

        toRet.mSaveToSent = shouldSaveToSent(acct);

        return toRet;
    }
    
    protected static Element sendCalendarMessage(OperationContext octxt, int apptFolderId, Account acct, Mailbox mbox, CalSendData dat, Element response)
    throws ServiceException { 
        synchronized (mbox) {
            int  folderId = 0;
            
            ParsedMessage pm = new ParsedMessage(dat.mMm, mbox.attachmentsIndexingEnabled());
            
            mbox.addInvite(octxt, apptFolderId, dat.mInvite, false, pm); 
            
            if (dat.mSaveToSent) {
                folderId = getSentFolder(acct, mbox, octxt);
            }

            int msgId = sendMimeMessage(octxt, mbox, acct, folderId, dat, dat.mMm, dat.mOrigId, dat.mReplyType);

            if (response != null && msgId != 0) {
                if (dat.mSaveToSent) {
                    response.addUniqueElement(MailService.E_MSG).addAttribute(MailService.A_ID, msgId);
                } else {
                    Message msg = mbox.getMessageById(octxt, msgId);
                    ApptInfo inf = msg.getApptInfo(0); // OK for now b/c client never creates >0 components, but FIXME
                    String inviteId = inf.getAppointmentId() + "-" + msgId;
                    response.addUniqueElement(MailService.E_MSG).addAttribute(MailService.A_ID, inviteId);
                }
            }
        }
        
        return response;
    }
}
