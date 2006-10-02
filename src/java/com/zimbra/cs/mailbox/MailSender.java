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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.zclient.ZMailbox;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailSender {

    public static final String MSGTYPE_REPLY = Flag.getAbbreviation(Flag.ID_FLAG_REPLIED) + "";
    public static final String MSGTYPE_FORWARD = Flag.getAbbreviation(Flag.ID_FLAG_FORWARDED) + "";

    static Log mLog = LogFactory.getLog(MailSender.class);

    public static int getSentFolder(Mailbox mbox) throws ServiceException {
        int folderId = Mailbox.ID_FOLDER_SENT;

        Account acct = mbox.getAccount();
        String sentFolder = acct.getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
        if (sentFolder != null)
            try {
                folderId = mbox.getFolderByPath(null, sentFolder).getId();
            } catch (NoSuchItemException nsie) { }
        return folderId;
    }

    private static Address[] removeInvalidAddresses(Address[] orig, Address[] invalidAddrs) {
        if (orig == null || invalidAddrs == null) 
            return orig;

        List<Address> newTo = new ArrayList<Address>();
        for (int i = 0; i < orig.length; i++) {
            boolean invalid = false;
            for (int j = 0; j < invalidAddrs.length; j++) {
                if (invalidAddrs[j].equals(orig[i])) {
                    invalid = true;
                    break;
                }
            }
            if (!invalid)
                newTo.add(orig[i]);
        }
        Address[] valid = newTo.toArray(new Address[newTo.size()]);
        return valid;
    }


    public static enum ReplyForwardType {
        ORIGINAL,  // This is an original message; not a reply or forward.
        REPLY,     // Reply to another message
        FORWARD    // Forwarding another message
    }

    public static int sendMimeMessage(OperationContext octxt, Mailbox mbox, boolean saveToSent, MimeMessage mm,
                                      List<InternetAddress> newContacts, List<Upload> uploads,
                                      int origMsgId, String replyType, boolean ignoreFailedAddresses,
                                      boolean replyToSender)
    throws ServiceException {
        int sentFolderId = saveToSent ? MailSender.getSentFolder(mbox) : 0;
        return sendMimeMessage(octxt, mbox, sentFolderId, mm, newContacts, uploads, origMsgId, replyType, ignoreFailedAddresses, replyToSender);
    }

    static class RollbackData {
        Mailbox mbox;
        ZMailbox zmbox;
        int msgId;

        RollbackData(Mailbox m, int i)      { mbox = m;  msgId = i; }
        RollbackData(Message msg)           { mbox = msg.getMailbox();  msgId = msg.getId(); }
        RollbackData(ZMailbox z, String s)  { zmbox = z;  msgId = Integer.parseInt(s); }

        void rollback() {
            try {
                if (mbox != null)
                    mbox.delete(null, msgId, MailItem.TYPE_MESSAGE);
                else
                    zmbox.deleteMessage("" + msgId);
            } catch (ServiceException e) {
                mLog.warn("ignoring error while deleting saved sent message: " + msgId, e);
            }
        }
    }

    /**
     * Send MimeMessage out as an email.
     * Returns the msg-id of the copy in the first saved folder, or 0 if none
     * 
     * @param mbox
     * @param mm the MimeMessage to send
     * @param newContacts
     * @param uploads
     * @param origMsgId if replying or forwarding, item ID of original message
     * @param replyType reply/forward type (null if original, which is neither
     *                  reply nor forward)
     * @param ignoreFailedAddresses If TRUE, then will attempt to send even if
     *                              some addresses fail (no error is returned!)
     * @param replyToSender if true and if setting Sender header, set Reply-To
     *                      header to the same address, otherwise don't set
     *                      Reply-To, letting the recipient MUA choose to whom
     *                      to send reply
     * @return
     * @throws ServiceException
     */
    public static int sendMimeMessage(OperationContext octxt, Mailbox mbox, int saveToFolder, MimeMessage mm,
                                      List<InternetAddress> newContacts, List<Upload> uploads,
                                      int origMsgId, String replyType, boolean ignoreFailedAddresses,
                                      boolean replyToSender)
    throws ServiceException {
        try {
            // slot the message in the parent's conversation if subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (origMsgId > 0)
                convId = mbox.getConversationIdFromReferent(mm, origMsgId);

            Account acct = mbox.getAccount();
            Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
            if (authuser == null)
                authuser = acct;
            boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());

            boolean overrideFromHeader = true;
            try {
                String fromHdr = mm.getHeader("From", null);
                if (fromHdr != null && !fromHdr.equals("")) {
                    InternetAddress from = new InternetAddress(fromHdr);
                    if (AccountUtil.addressMatchesAccount(acct, from.getAddress()))
                        overrideFromHeader = false;
                }
            } catch (Exception e) { }
            InternetAddress sender = isDelegatedRequest ? AccountUtil.getFriendlyEmailAddress(authuser) : null;

            // set various headers on the outgoing message
            if (overrideFromHeader)
                mm.setFrom(AccountUtil.getFriendlyEmailAddress(acct));
            mm.setSentDate(new Date());
            if (sender == null) {
                String replyTo = acct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
                if (replyTo != null && !replyTo.trim().equals(""))
                    mm.setHeader("Reply-To", replyTo);
            } else {
                mm.setSender(sender);
                if (replyToSender)
                    mm.setReplyTo(new Address[]{sender});
            }
            mm.saveChanges();

            try {
                for (Class vclass : MimeVisitor.getMutators())
                    ((MimeVisitor) vclass.newInstance()).accept(mm);
            } catch (Exception e) {
                ZimbraLog.misc.warn("failure to modify outbound message; aborting send", e);
                throw ServiceException.FAILURE("mutator error; aborting send", e);
            }

            // if requested, save a copy of the message to the Sent Mail folder
            RollbackData rdata = null;
            if (saveToFolder > 0) {
                // figure out where to save the save-to-sent copy
                Mailbox mboxSave = isDelegatedRequest ? null : mbox;
                if (isDelegatedRequest && Provisioning.onLocalServer(authuser)) {
                    mboxSave = MailboxManager.getInstance().getMailboxByAccount(authuser);
                    saveToFolder = getSentFolder(mboxSave);
                }

                if (mboxSave != null) {
                    int flags = Flag.BITMASK_FROM_ME;
                    ParsedMessage pm = new ParsedMessage(mm, mm.getSentDate().getTime(),
                                                         mboxSave.attachmentsIndexingEnabled());
                    // save it to the requested folder
                    Message msg = mboxSave.addMessage(octxt, pm, saveToFolder, true, flags, null, convId);
                    rdata = new RollbackData(msg);
                } else {
                    // delegated request, not local
                    String uri = AccountUtil.getSoapUri(authuser);
                    if (uri != null)
                        try {
                            ZMailbox.Options options = new ZMailbox.Options(new AuthToken(authuser).getEncoded(), uri);
                            options.setNoSession(true);
                            ZMailbox zmbox = ZMailbox.getMailbox(options);
                            String sentFolder = authuser.getAttr(Provisioning.A_zimbraPrefSentMailFolder, "" + Mailbox.ID_FOLDER_SENT);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            mm.writeTo(baos);
    
                            String msgId = zmbox.addMessage(sentFolder, "s", null, mm.getSentDate().getTime(), baos.toByteArray(), true);
                            rdata = new RollbackData(zmbox, msgId);
                        } catch (Exception e) {
                            ZimbraLog.misc.warn("could not save to remote sent folder (perm denied); continuing", e);
                        }
                }
            }

            // send the message via SMTP
            try {
                boolean retry = ignoreFailedAddresses;
                if (mm.getAllRecipients() != null) {
                    do {
                        try {
                            Transport.send(mm);
                            retry = false;
                        } catch (SendFailedException sfe) {
                            Address[] invalidAddrs = sfe.getInvalidAddresses();
                            if (!retry)
                                throw sfe;

                            Address[] to = removeInvalidAddresses(mm.getRecipients(RecipientType.TO), invalidAddrs);
                            Address[] cc = removeInvalidAddresses(mm.getRecipients(RecipientType.CC), invalidAddrs);
                            Address[] bcc = removeInvalidAddresses(mm.getRecipients(RecipientType.BCC), invalidAddrs);

                            // if there are NO valid addrs, then give up!
                            if ((to == null || to.length == 0) &&
                                    (cc == null || cc.length == 0) &&
                                    (bcc == null || bcc.length == 0))
                                retry = false;

                            mm.setRecipients(RecipientType.TO, to);
                            mm.setRecipients(RecipientType.CC, cc);
                            mm.setRecipients(RecipientType.BCC, bcc);
                        }
                    } while(retry);
                }
            } catch (MessagingException e) {
                if (rdata != null)  rdata.rollback();
                throw e;
            } catch (RuntimeException e) {
                if (rdata != null)  rdata.rollback();
                throw e;
            } 

            // check if this is a reply, and if so flag the msg appropriately
            if (origMsgId > 0) {
                try {
                    if (MSGTYPE_REPLY.equals(replyType))
                        mbox.alterTag(octxt, origMsgId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_REPLIED, true);
                    else if (MSGTYPE_FORWARD.equals(replyType))
                        mbox.alterTag(octxt, origMsgId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FORWARDED, true);
                } catch (ServiceException e) {
                    // this is not an error case: when accepting/declining an
                    // appointment, the original message may be gone
                }
            }


            // we can now purge the uploaded attachments
            if (uploads != null)
                FileUploadServlet.deleteUploads(uploads);

            // add any new contacts to the personal address book
            if (newContacts != null) {
                for (InternetAddress inetaddr : newContacts) {
                    ParsedAddress addr = new ParsedAddress(inetaddr);
                    try {
                        mbox.createContact(octxt, addr.getAttributes(), Mailbox.ID_FOLDER_AUTO_CONTACTS, null);
                    } catch (ServiceException e) {
                        mLog.warn("ignoring error while auto-adding contact", e);
                    }
                }
            }

            return (!isDelegatedRequest && rdata != null ? rdata.msgId : 0);

        } catch (SendFailedException sfe) {
            mLog.warn("exception ocurred during SendMsg", sfe);
            Address[] invalidAddrs = sfe.getInvalidAddresses();
            Address[] validUnsentAddrs = sfe.getValidUnsentAddresses();
            if (invalidAddrs != null && invalidAddrs.length > 0) { 
                StringBuffer msg = new StringBuffer("Invalid address").append(invalidAddrs.length > 1 ? "es: " : ": ");
                if (invalidAddrs != null && invalidAddrs.length > 0) {
                    for (int i = 0; i < invalidAddrs.length; i++) {
                        if (i > 0)
                            msg.append(",");
                        msg.append(invalidAddrs[i]);
                    }
                }
                if (JMSession.getSmtpConfig().getSendPartial())
                    throw MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE(msg.toString(), sfe, invalidAddrs, validUnsentAddrs);
                else
                    throw MailServiceException.SEND_ABORTED_ADDRESS_FAILURE(msg.toString(), sfe, invalidAddrs, validUnsentAddrs);
            } else {
                throw MailServiceException.SEND_FAILURE("SMTP server reported: " + sfe.getMessage().trim(), sfe, invalidAddrs, validUnsentAddrs);
            }
        } catch (IOException ioe) {
            mLog.warn("exception occured during send msg", ioe);
            throw ServiceException.FAILURE("IOException", ioe);
        } catch (MessagingException me) {
            mLog.warn("exception occurred during SendMsg", me);
            throw ServiceException.FAILURE("MessagingException", me);
        }
    }
}
