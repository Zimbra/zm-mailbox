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
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;

public abstract class CalendarRequest extends SendMsg {

    protected static class CalSendData extends ParseMimeMessage.MimeMessageData {
        int mOrigId; // orig id if this is a reply
        String mReplyType; 
        MimeMessage mMm;
        boolean mSaveToSent;
    }

    protected static CalSendData handleMsgElement(OperationContext octxt, Element msgElem, Account acct,
                                                  Mailbox mbox, ParseMimeMessage.InviteParser inviteParser)
    throws ServiceException {

        CalSendData toRet = new CalSendData();

        // check to see if this message is a reply -- if so, then we'll want to note that so 
        // we can more-correctly match the conversations up
        toRet.mOrigId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        toRet.mReplyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, TYPE_REPLY);

        // parse the data
        toRet.mMm = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, null, inviteParser, toRet);

        toRet.mSaveToSent = shouldSaveToSent(acct);

        return toRet;
    }
    
    protected static Element sendCalendarMessage(OperationContext octxt, Account acct, Mailbox mbox, CalSendData dat, Element response)
    throws ServiceException { 
        synchronized (mbox) {
            int[] folderId;
            if (dat.mSaveToSent)
                folderId = new int[] { Mailbox.ID_FOLDER_CALENDAR, getSentFolder(acct, mbox) };
            else
                folderId = new int[] { Mailbox.ID_FOLDER_CALENDAR };

            int msgId = sendMimeMessage(octxt, mbox, acct, folderId, dat, dat.mMm, dat.mOrigId, dat.mReplyType);

            if (response != null && msgId != 0)
                response.addUniqueElement(MailService.E_MSG).addAttribute(MailService.A_ID, msgId);
        }
        
        return response;
    }
}
