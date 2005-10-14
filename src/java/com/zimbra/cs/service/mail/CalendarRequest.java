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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;


public abstract class CalendarRequest extends SendMsg {

    protected static class CalSendData extends ParseMimeMessage.MimeMessageData {
        int mOrigId; // orig id if this is a reply
        String mReplyType; 
        MimeMessage mMm;
        boolean mSaveToSent;
        Invite mInvite;
    }

    /**
     * 
     * parses an <m> element using the passed-in InviteParser
     * 
     * @param lc
     * @param msgElem
     * @param acct
     * @param mbox
     * @param inviteParser
     * @return
     * @throws ServiceException
     */
    protected static CalSendData handleMsgElement(ZimbraContext lc, Element msgElem, Account acct,
                                                  Mailbox mbox, ParseMimeMessage.InviteParser inviteParser)
    throws ServiceException {
        CalSendData csd = new CalSendData();

        assert(inviteParser.getResult() != null);

        // check to see if this message is a reply -- if so, then we'll want to note that so 
        // we can more-correctly match the conversations up
        csd.mOrigId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        csd.mReplyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, TYPE_REPLY);

        // parse the data
        csd.mMm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, null, inviteParser, csd);
        
        // FIXME FIXME FIXME -- need to figure out a way to get the FRAGMENT data out of the initial
        // message here, so that we can copy it into the DESCRIPTION field in the iCalendar data that
        // goes out...will make for much better interop!

        if (inviteParser.getResult() == null || inviteParser.getResult().mInvite == null) {
            assert(inviteParser.getResult() != null);
        }
        csd.mInvite = inviteParser.getResult().mInvite;

        csd.mSaveToSent = shouldSaveToSent(acct);

        return csd;
    }

    protected static Element sendCalendarMessage(ZimbraContext lc, int apptFolderId, Account acct, Mailbox mbox, CalSendData csd, Element response)
    throws ServiceException { 
        synchronized (mbox) {
            OperationContext octxt = lc.getOperationContext();

            ParsedMessage pm = new ParsedMessage(csd.mMm, mbox.attachmentsIndexingEnabled());
            int[] ids = mbox.addInvite(octxt, apptFolderId, csd.mInvite, false, pm); 

            boolean hasRecipients = true;
            try {
                hasRecipients = csd.mMm.getAllRecipients() != null;
            } catch (MessagingException e) { }
            boolean saveToSent = csd.mSaveToSent && hasRecipients && !lc.isDelegatedRequest();

            int saveFolderId = saveToSent ? getSentFolder(acct, mbox, octxt) : 0;

            int msgId = sendMimeMessage(octxt, mbox, acct, saveFolderId, csd, csd.mMm, csd.mOrigId, csd.mReplyType);

            if (response != null && ids != null) {
                response.addAttribute(MailService.A_APPT_ID, lc.formatItemId(ids[0]));
                response.addAttribute(MailService.A_APPT_INV_ID, lc.formatItemId(ids[0], ids[1]));
                if (saveToSent) {
                    response.addUniqueElement(MailService.E_MSG).addAttribute(MailService.A_ID, lc.formatItemId(msgId));
                }
            }
        }
        
        return response;
    }
}
