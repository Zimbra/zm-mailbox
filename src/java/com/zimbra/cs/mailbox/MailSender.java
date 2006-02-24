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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

public class MailSender {

    public static final String MSGTYPE_REPLY =
        Flag.getAbbreviation(Flag.ID_FLAG_REPLIED) + "";
    public static final String MSGTYPE_FORWARD =
        Flag.getAbbreviation(Flag.ID_FLAG_FORWARDED) + "";

    private static Log mLog = LogFactory.getLog(MailSender.class);

    public static int getSentFolder(OperationContext octxt, Mailbox mbox)
    throws ServiceException{
        int folderId = Mailbox.ID_FOLDER_SENT;

        Account acct = mbox.getAccount();
        String sentFolder = acct.getAttr(
                Provisioning.A_zimbraPrefSentMailFolder, null);
        if (sentFolder != null)
            try {
                folderId = mbox.getFolderByPath(octxt, sentFolder).getId();
            } catch (NoSuchItemException nsie) { }
        return folderId;
    }

    private static Address[] removeInvalidAddresses(Address[] orig,
                                                    Address[] invalidAddrs) {
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
        Address[] toRet = new Address[newTo.size()];
        toRet = newTo.toArray(toRet);
        return toRet;
    }


    public static enum ReplyForwardType {
        ORIGINAL,  // This is an original message; not a reply or forward.
        REPLY,     // Reply to another message
        FORWARD    // Forwarding another message
    }

    public static int sendMimeMessage(OperationContext octxt,
                                      Mailbox mbox,
                                      boolean saveToSent,
                                      MimeMessage mm,
                                      List<InternetAddress> newContacts,
                                      List<Upload> uploads,
                                      int origMsgId,
                                      String replyType,
                                      boolean ignoreFailedAddresses)
    throws ServiceException {
        int sentFolderId =
            saveToSent ? MailSender.getSentFolder(octxt, mbox) : 0;
        return sendMimeMessage(octxt, mbox, sentFolderId,
                               mm, newContacts, uploads,
                               origMsgId, replyType,
                               ignoreFailedAddresses);
    }

    /**
     * Send MimeMessage out as an email.
     * Returns the msg-id of the copy in the first saved folder, or 0 if none
     * 
     * @param mbox
     * @param saveToFolders[] list of folderIds to save a copy of this msg
     *                        into.  This is a list primarily so that we can
     *                        do special things with INVITE messages (save
     *                        copy to calendar folder).  Messages are
     *                        guaranteed to be inserted in the specified-order
     *                        (this matters for calendaring!)
     * @param mm the MimeMessage to send
     * @param newContacts
     * @param uploads
     * @param origMsgId if replying or forwarding, item ID of original message
     * @param replyType reply/forward type (null if original, which is neither
     *                  reply nor forward)
     * @param ignoreFailedAddresses If TRUE, then will attempt to send even if
     *                              some addresses fail (no error is returned!)
     * @return
     * @throws ServiceException
     */
    public static int sendMimeMessage(OperationContext octxt,
                                      Mailbox mbox,
                                      int saveToFolder,
                                      MimeMessage mm,
                                      List<InternetAddress> newContacts,
                                      List<Upload> uploads,
                                      int origMsgId,
                                      String replyType,
                                      boolean ignoreFailedAddresses)
    throws ServiceException {
        try {
            // slot the message in the parent's conversation if subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (origMsgId > 0)
                convId = mbox.getConversationIdFromReferent(mm, origMsgId);

            Account acct = mbox.getAccount();
            String replyTo =
                acct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
            mm.setFrom(AccountUtil.getOutgoingFromAddress(acct));
            mm.setSentDate(new Date());
            if (replyTo != null && !replyTo.trim().equals(""))
                mm.setHeader("Reply-To", replyTo);
            mm.saveChanges();

            // save a copy of the message to the Sent Mail folder
            Message  msg = null;
            if (saveToFolder != 0) {
                int flags = Flag.FLAG_FROM_ME;
                ParsedMessage pm =
                    new ParsedMessage(mm,
                                      mm.getSentDate().getTime(),
                                      mbox.attachmentsIndexingEnabled());

                pm.analyze();

                // save it to the requested folder
                msg = mbox.addMessage(octxt, pm, saveToFolder, true,
                                      flags, null, convId);
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

                            Address[] to = removeInvalidAddresses(
                                    mm.getRecipients(RecipientType.TO),
                                    invalidAddrs);
                            Address[] cc = removeInvalidAddresses(
                                    mm.getRecipients(RecipientType.CC),
                                    invalidAddrs);
                            Address[] bcc = removeInvalidAddresses(
                                    mm.getRecipients(RecipientType.BCC),
                                    invalidAddrs);

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
                rollbackMessage(octxt, mbox, msg);
                throw e;
            } catch (RuntimeException e) {
                rollbackMessage(octxt, mbox, msg);
                throw e;
            } 

            // check if this is a reply, and if so flag the msg appropriately
            if (origMsgId > 0) {
                try {
                    if (MSGTYPE_REPLY.equals(replyType))
                        mbox.alterTag(octxt, origMsgId, MailItem.TYPE_MESSAGE,
                                      Flag.ID_FLAG_REPLIED, true);
                    else if (MSGTYPE_FORWARD.equals(replyType))
                        mbox.alterTag(octxt, origMsgId, MailItem.TYPE_MESSAGE,
                                      Flag.ID_FLAG_FORWARDED, true);
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
                        mbox.createContact(octxt, addr.getAttributes(),
                                           Mailbox.ID_FOLDER_CONTACTS, null);
                    } catch (ServiceException e) {
                        mLog.warn("ignoring error while auto-adding contact",
                                  e);
                    }
                }
            }

            return (msg != null ? msg.getId() : 0);

        } catch (SendFailedException sfe) {
            mLog.warn("exception ocurred during SendMsg", sfe);
            Address[] invalidAddrs = sfe.getInvalidAddresses();
            Address[] validUnsentAddrs = sfe.getValidUnsentAddresses();
            if (invalidAddrs != null && invalidAddrs.length > 0) { 
                StringBuffer msg =
                    new StringBuffer("Invalid address").
                    append(invalidAddrs.length > 1 ? "es: " : ": ");
                if (invalidAddrs != null && invalidAddrs.length > 0) {
                    for (int i = 0; i < invalidAddrs.length; i++) {
                        if (i > 0) {
                            msg.append(",");
                        }
                        msg.append(invalidAddrs[i]);
                    }
                }
                if (JMSession.getSmtpConfig().getSendPartial()) {
                    throw MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE(
                            msg.toString(), sfe,
                            invalidAddrs, validUnsentAddrs);
                } else {
                    throw MailServiceException.SEND_ABORTED_ADDRESS_FAILURE(
                            msg.toString(), sfe,
                            invalidAddrs, validUnsentAddrs);
                }
            } else {
                throw MailServiceException.SEND_FAILURE(
                        "SMTP server reported: " + sfe.getMessage().trim(),
                        sfe, invalidAddrs, validUnsentAddrs);
            }
        } catch (IOException ioe) {
            mLog.warn("exception occured during send msg", ioe);
            throw ServiceException.FAILURE("IOException", ioe);
        } catch (MessagingException me) {
            mLog.warn("exception occurred during SendMsg", me);
            throw ServiceException.FAILURE("MessagingException", me);
        }
    }

    private static void rollbackMessage(OperationContext octxt,
                                        Mailbox mbox,
                                        Message msg) {
        // clean up save-to-sent if needed
        if (msg == null)
            return;
        try {
            mbox.delete(octxt, msg.getId(), msg.getType());
        } catch (Exception e) {
            mLog.warn("ignoring error while deleting saved sent message: " +
                      msg.getId(), e);
        }
    }
}
