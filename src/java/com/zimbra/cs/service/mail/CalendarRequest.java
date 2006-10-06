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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;


public abstract class CalendarRequest extends MailDocumentHandler {

    protected static class CalSendData extends ParseMimeMessage.MimeMessageData {
        int mOrigId; // orig id if this is a reply
        String mReplyType; 
        MimeMessage mMm;
        boolean mSaveToSent;
        Invite mInvite;

        public CalSendData(Account fromAcct, Account senderAcct, boolean onBehalfOf) {
            if (onBehalfOf && senderAcct != null)
                mSaveToSent = senderAcct.saveToSent();
            else
                mSaveToSent = fromAcct.saveToSent();
        }
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
    protected static CalSendData handleMsgElement(ZimbraSoapContext lc, Element msgElem, Account acct,
                                                  Mailbox mbox, ParseMimeMessage.InviteParser inviteParser)
    throws ServiceException {
        CalSendData csd = new CalSendData(acct, lc.getAuthtokenAccount(), lc.isDelegatedRequest());

        assert(inviteParser.getResult() == null);

        // check to see if this message is a reply -- if so, then we'll want to note that so 
        // we can more-correctly match the conversations up
        csd.mOrigId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        csd.mReplyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, MailSender.MSGTYPE_REPLY);

        // parse the data
        csd.mMm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, null, inviteParser, csd);
        
        // FIXME FIXME FIXME -- need to figure out a way to get the FRAGMENT data out of the initial
        // message here, so that we can copy it into the DESCRIPTION field in the iCalendar data that
        // goes out...will make for much better interop!

        assert(inviteParser.getResult() != null);

        csd.mInvite = inviteParser.getResult().mInvite;
        
        return csd;
    }
    
    protected static String getOrigHtml(MimeMessage mm) throws ServiceException {
        try {
            List /*<MPartInfo>*/ parts = Mime.getParts(mm);
            for (Iterator it = parts.iterator(); it.hasNext(); ) {
                MPartInfo mpi = (MPartInfo) it.next();
                
                if (mpi.getContentType().equals(Mime.CT_TEXT_HTML))
                    return Mime.getStringContent(mpi.getMimePart());
            }
            return null;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException "+e, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException "+e, e);
        }
    }
        
    protected static void patchCalendarURLs(MimeMessage mm, String htmlStr, String localURL, String orgAddress, String uid, String attendee, String invId) throws ServiceException
    {
        try {
            boolean changed = false;
            
            String accept = buildUrl(localURL, orgAddress, uid, attendee, invId, "ACCEPT");
            String decline = buildUrl(localURL, orgAddress, uid, attendee, invId, "DECLINE");
            String tentative = buildUrl(localURL, orgAddress, uid, attendee, invId, "TENTATIVE");
            
            List /*<MPartInfo>*/ parts = Mime.getParts(mm);
            for (Iterator it = parts.iterator(); it.hasNext(); ) {
                MPartInfo mpi = (MPartInfo) it.next();
                
                if (mpi.getContentType().equals(Mime.CT_TEXT_HTML)) {
                    String str = htmlStr;
                    
                    str = str.replaceFirst("href=\"@@ACCEPT@@\"", accept);
                    str = str.replaceFirst("href=\"@@DECLINE@@\"", decline);
                    str = str.replaceFirst("href=\"@@TENTATIVE@@\"", tentative);
                    
                    System.out.println(str);
                    mpi.getMimePart().setContent(str, Mime.CT_TEXT_HTML);
                    changed = true;
                    
                    break; // only match one part
                }
            }
            
            if (changed) {
                mm.saveChanges();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException "+e, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException "+e, e);
        }
    }
    
    protected static String buildUrl(String localURL, String orgAddress, String uid, String attendee, String invId, String verb)
    {
        StringBuffer toRet = new StringBuffer("href=\"").append(localURL);
        toRet.append("/service/pubcal/reply?org=").append(orgAddress);
        toRet.append("&uid=").append(uid);
        toRet.append("&at=").append(attendee);
        toRet.append("&v=").append(verb);
        toRet.append("&invId=").append(invId);
        toRet.append('\"');
        
        return toRet.toString();
    }

    protected static Element sendCalendarMessage(
        ZimbraSoapContext lc,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        Element response,
        boolean ignoreFailedAddresses)
    throws ServiceException {
        return sendCalendarMessageInternal(lc, apptFolderId,
                                           acct, mbox, csd, response,
                                           ignoreFailedAddresses, true);
    }

    /**
     * Send a cancellation iCalendar email and optionally cancel sender's
     * appointment.
     * @param lc
     * @param apptFolderId
     * @param acct
     * @param mbox
     * @param csd
     * @param cancelOwnAppointment if true, sender's appointment is canceled.
     *                             if false, sender's appointment is not
     *                             canceled. (this may be appropriate when
     *                             sending out cancelleation message to
     *                             removed attendees)
     * @return
     * @throws ServiceException
     */
    protected static Element sendCalendarCancelMessage(
        ZimbraSoapContext lc,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        boolean cancelOwnAppointment)
    throws ServiceException {
    	return sendCalendarMessageInternal(lc, apptFolderId, acct, mbox, csd,
                                           null, true, cancelOwnAppointment);
    }

    /**
     * Send an iCalendar email message and optionally create/update/cancel
     * corresponding appointment/invite in sender's calendar.
     * @param lc
     * @param apptFolderId
     * @param acct
     * @param mbox
     * @param csd
     * @param response
     * @param ignoreFailedAddresses
     * @param updateOwnAppointment if true, corresponding change is made to
     *                             sender's calendar
     * @return
     * @throws ServiceException
     */
    private static Element sendCalendarMessageInternal(
        ZimbraSoapContext lc,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        Element response,
        boolean ignoreFailedAddresses,
        boolean updateOwnAppointment)
    throws ServiceException {
        synchronized (mbox) {
            OperationContext octxt = lc.getOperationContext();

            boolean onBehalfOf = lc.isDelegatedRequest();
            boolean notifyOwner = onBehalfOf && acct.getBooleanAttr(Provisioning.A_zimbraPrefCalendarNotifyDelegatedChanges, false);
            if (notifyOwner) {
                try {
                    InternetAddress addr = AccountUtil.getFriendlyEmailAddress(acct);
                    csd.mMm.addRecipient(javax.mail.Message.RecipientType.TO, addr);
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("count not add calendar owner to recipient list", e);
                } catch (UnsupportedEncodingException e) {
                    throw ServiceException.FAILURE("count not add calendar owner to recipient list", e);
                }
            }

            // Never send a notification to the person making the SOAP request
            // in a non-delegated request.
            if (!onBehalfOf) {
                String[] aliases = acct.getAliases();
                String[] addrs;
                if (aliases != null && aliases.length > 0) {
                    addrs = new String[aliases.length + 1];
                    addrs[0] = acct.getAttr(Provisioning.A_mail);
                    for (int i = 0; i < aliases.length; i++)
                        addrs[i + 1] = aliases[i];
                } else {
                    addrs = new String[1];
                    addrs[0] = acct.getAttr(Provisioning.A_mail);
                }
                try {
                    Mime.removeRecipients(csd.mMm, addrs);
                } catch (MessagingException e) {}
            }

            // DON'T try to do a full text-extraction attachments: if the calendar message doesn't 
            // have useful text in the body, then so be it
            ParsedMessage pm = new ParsedMessage(csd.mMm, false);
            
            if (csd.mInvite.getFragment() == null || csd.mInvite.getFragment().equals("")) {
                csd.mInvite.setFragment(pm.getFragment());
            }

            boolean hasRecipients = true;
            try {
                hasRecipients = csd.mMm.getAllRecipients() != null;
            } catch (MessagingException e) { }

            boolean saveToSent = csd.mSaveToSent && hasRecipients;
            int saveFolderId = saveToSent ? MailSender.getSentFolder(mbox) : 0;
            
            int msgId = 0;

//            String html = getOrigHtml(csd.mMm);
//            if (html != null && html.indexOf("href=\"@@ACCEPT@@\"") >= 0) {
//                try {
//                    String localURL = ZimbraServlet.getURLForServer(Provisioning.getInstance().getLocalServer());
//                    
//                    Address[] addrs = csd.mMm.getAllRecipients();
//                    for (int i = 0; i < addrs.length; i++) {
//                        InternetAddress ia = (InternetAddress)addrs[i];
//                        if (html != null) {
//                            patchCalendarURLs(csd.mMm, html, 
//                                    localURL,
//                                    csd.mInvite.getOrganizer().getCalAddress().getSchemeSpecificPart(),
//                                    csd.mInvite.getUid(),
//                                    ia.getAddress(),
//                                    ids[0]+"-"+ids[1]);
//                        }
//                        
//                        Address[] sendAddrs = new Address[1];
//                        sendAddrs[0] = ia;
//                        csd.mMm.setRecipients(Message.RecipientType.TO, sendAddrs);
//                        
//                        msgId = sendMimeMessage(octxt, mbox, acct, saveFolderId, csd, csd.mMm, csd.mOrigId, csd.mReplyType, ignoreFailedAddresses);                  
//                    }
//                } catch (MessagingException e) { }
//            } else {
            msgId = mbox.getMailSender().sendMimeMessage(octxt, mbox, saveFolderId, csd.mMm, csd.newContacts, csd.uploads,
                                                         csd.mOrigId, csd.mReplyType, ignoreFailedAddresses, true);
//            }

            if (updateOwnAppointment) {
                int[] ids = mbox.addInvite(octxt, csd.mInvite, apptFolderId, false, pm);
    
                if (response != null && ids != null) {
                    response.addAttribute(MailService.A_APPT_ID, lc.formatItemId(ids[0]));
                    response.addAttribute(MailService.A_APPT_INV_ID, lc.formatItemId(ids[0], ids[1]));
                    if (saveToSent) {
                        response.addUniqueElement(MailService.E_MSG).addAttribute(MailService.A_ID, lc.formatItemId(msgId));
                    }
                }
            }
        }
        
        return response;
    }
}
