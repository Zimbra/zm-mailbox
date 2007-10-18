/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZMailbox;

public class MailSender {

    public static final String MSGTYPE_REPLY = Flag.getAbbreviation(Flag.ID_FLAG_REPLIED) + "";
    public static final String MSGTYPE_FORWARD = Flag.getAbbreviation(Flag.ID_FLAG_FORWARDED) + "";

    MailSender()  { }

    public static int getSentFolderId(Mailbox mbox, Identity identity) throws ServiceException {
        int folderId = Mailbox.ID_FOLDER_SENT;
        String sentFolder = identity.getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
        if (sentFolder != null) {
            try {
                folderId = mbox.getFolderByPath(null, sentFolder).getId();
            } catch (NoSuchItemException nsie) { }
        }
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

    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm,
                                  List<InternetAddress> newContacts, List<Upload> uploads,
                                  int origMsgId, String replyType, String identityId,
                                  boolean ignoreFailedAddresses, boolean replyToSender)
    throws ServiceException {
        Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
        Identity identity = Provisioning.getInstance().get(authuser, IdentityBy.id, identityId);
        if (identity == null)
            identity = Provisioning.getInstance().getDefaultIdentity(authuser);

        boolean saveToSent = identity.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, true);

        return sendMimeMessage(octxt, mbox, saveToSent, mm, newContacts, uploads, origMsgId, replyType, identity,
                               ignoreFailedAddresses, replyToSender);
    }

    static class RollbackData {
        Mailbox mbox;
        ZMailbox zmbox;
        ItemId msgId;

        RollbackData(Mailbox m, int i)  { mbox = m;  msgId = new ItemId(mbox, i); }
        RollbackData(Message msg)       { mbox = msg.getMailbox();  msgId = new ItemId(msg); }
        RollbackData(ZMailbox z, Account a, String s) throws ServiceException {
            zmbox = z;  msgId = new ItemId(s, a.getId());
        }

        void rollback() {
            try {
                if (mbox != null)
                    mbox.delete(null, msgId.getId(), MailItem.TYPE_MESSAGE);
                else
                    zmbox.deleteMessage("" + msgId);
            } catch (ServiceException e) {
                ZimbraLog.smtp.warn("ignoring error while deleting saved sent message: " + msgId, e);
            }
        }
    }

    /**
     * Send MimeMessage out as an email.
     * Returns the msg-id of the copy in the first saved folder, or 0 if none
     * @param mbox
     * @param mm the MimeMessage to send
     * @param newContacts
     * @param uploads
     * @param origMsgId if replying or forwarding, item ID of original message
     * @param replyType reply/forward type (null if original, which is neither
     *                  reply nor forward)
     * @param identityId TODO
     * @param ignoreFailedAddresses If TRUE, then will attempt to send even if
     *                              some addresses fail (no error is returned!)
     * @param replyToSender if true and if setting Sender header, set Reply-To
     *                      header to the same address, otherwise don't set
     *                      Reply-To, letting the recipient MUA choose to whom
     *                      to send reply
     * 
     * @return
     * @throws ServiceException
     */

    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, boolean saveToSent, MimeMessage mm,
                                  List<InternetAddress> newContacts, List<Upload> uploads,
                                  int origMsgId, String replyType, Identity identity,
                                  boolean ignoreFailedAddresses, boolean replyToSender)
    throws ServiceException {
        try {
            logMessage(mm, origMsgId, uploads, replyType);
            
            Account acct = mbox.getAccount();
            Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
            boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
            if (authuser == null)
                authuser = acct;
            boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());

            // slot the message in the parent's conversation if subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (origMsgId > 0 && !isDelegatedRequest)
                convId = mbox.getConversationIdFromReferent(mm, origMsgId);

            // set the From, Sender, Date, Reply-To, etc. headers
            updateHeaders(mm, acct, authuser, octxt, octxt != null ? octxt.getRequestIP() : null, replyToSender);

            // run any pre-send/pre-save MIME mutators
            try {
                for (Class vclass : MimeVisitor.getMutators())
                    ((MimeVisitor) vclass.newInstance()).accept(mm);
            } catch (Exception e) {
                ZimbraLog.smtp.warn("failure to modify outbound message; aborting send", e);
                throw ServiceException.FAILURE("mutator error; aborting send", e);
            }

            // don't save if the message doesn't actually get *sent*
            boolean hasRecipients = (mm.getAllRecipients() != null);
            saveToSent &= hasRecipients;

            // #0 is the authenticated user's, #1 is the send-as user's
            RollbackData[] rollback = new RollbackData[2];

            // if requested, save a copy of the message to the Sent Mail folder
            ParsedMessage pm = null;
            if (saveToSent) {
                if (identity == null)
                    identity = Provisioning.getInstance().getDefaultIdentity(authuser);

                // figure out where to save the save-to-sent copy
                Mailbox mboxSave = isDelegatedRequest ? null : mbox;
                if (isDelegatedRequest && Provisioning.onLocalServer(authuser))
                    mboxSave = MailboxManager.getInstance().getMailboxByAccount(authuser);

                if (mboxSave != null) {
                    int flags = Flag.BITMASK_FROM_ME;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mboxSave.attachmentsIndexingEnabled());
                    // save it to the requested folder
                    int sentFolderId = getSentFolderId(mboxSave, identity);
                    Message msg = mboxSave.addMessage(octxt, pm, sentFolderId, mboxSave != mbox, flags, null, convId);
                    rollback[0] = new RollbackData(msg);
                } else {
                    // delegated request, not local
                    String uri = AccountUtil.getSoapUri(authuser);
                    if (uri != null) {
                        try {
                            ZMailbox.Options options = new ZMailbox.Options(new AuthToken(authuser, isAdminRequest).getEncoded(), uri);
                            options.setNoSession(true);
                            ZMailbox zmbox = ZMailbox.getMailbox(options);
                            String sentFolder = identity.getAttr(Provisioning.A_zimbraPrefSentMailFolder, "" + Mailbox.ID_FOLDER_SENT);
                            pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());

                            String msgId = zmbox.addMessage(sentFolder, "s", null, mm.getSentDate().getTime(), pm.getRawData(), false);
                            rollback[0] = new RollbackData(zmbox, authuser, msgId);
                        } catch (Exception e) {
                            ZimbraLog.smtp.warn("could not save to remote sent folder (perm denied); continuing", e);
                        }
                    }
                }
            }

            // for delegated sends where the authenticated user is reflected in the Sender header (c.f. updateHeaders),
            //   automatically save a copy to the "From" user's mailbox
            if (hasRecipients && isDelegatedRequest && mm.getSender() != null && acct.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, true)) {
                int flags = Flag.BITMASK_UNREAD | Flag.BITMASK_FROM_ME;
                // save the sent copy using the target's credentials, as the sender doesn't necessarily have write access
                OperationContext octxtTarget = new OperationContext(acct);
                if (pm == null || pm.isAttachmentIndexingEnabled() != mbox.attachmentsIndexingEnabled())
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());
                int sentFolderId = getSentFolderId(mbox, Provisioning.getInstance().getDefaultIdentity(acct));
                Message msg = mbox.addMessage(octxtTarget, pm, sentFolderId, true, flags, null, convId);
                rollback[1] = new RollbackData(msg);
            }

            // actually send the message via SMTP
            sendMessage(mm, ignoreFailedAddresses, rollback);

            // check if this is a reply, and if so flag the msg appropriately
            if (origMsgId > 0) {
                try {
                    if (MSGTYPE_REPLY.equals(replyType))
                        mbox.alterTag(octxt, origMsgId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_REPLIED, true);
                    else if (MSGTYPE_FORWARD.equals(replyType))
                        mbox.alterTag(octxt, origMsgId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FORWARDED, true);
                } catch (ServiceException e) {
                    // this is not an error case:
                    //   - the original message may be gone when accepting/declining an appointment
                    //   - we may have PERM_DENIED on delegated send
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
                        ParsedContact pc = new ParsedContact(addr.getAttributes());
                        mbox.createContact(octxt, pc, Mailbox.ID_FOLDER_AUTO_CONTACTS, null);
                    } catch (ServiceException e) {
                        ZimbraLog.smtp.warn("ignoring error while auto-adding contact", e);
                    }
                }
            }

            return (rollback[0] != null ? rollback[0].msgId : null);

        } catch (SafeSendFailedException sfe) {
            ZimbraLog.smtp.warn("Exception occurred during SendMsg: ", sfe);
            Address[] invalidAddrs = sfe.getInvalidAddresses();
            Address[] validUnsentAddrs = sfe.getValidUnsentAddresses();
            if (invalidAddrs != null && invalidAddrs.length > 0) { 
                StringBuilder msg = new StringBuilder("Invalid address").append(invalidAddrs.length > 1 ? "es: " : ": ");
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
                throw MailServiceException.SEND_FAILURE("SMTP server reported: " + sfe.getMessage(), sfe, invalidAddrs, validUnsentAddrs);
            }
        } catch (IOException ioe) {
            ZimbraLog.smtp.warn("exception occured during send msg", ioe);
            throw ServiceException.FAILURE("IOException", ioe);
        } catch (MessagingException me) {
            ZimbraLog.smtp.warn("exception occurred during SendMsg", me);
            throw ServiceException.FAILURE("MessagingException", me);
        }
    }

    private void logMessage(MimeMessage mm, int origMsgId, List<Upload> uploads, String replyType) {
        // Log sent message info
        if (ZimbraLog.smtp.isInfoEnabled()) {
            StringBuilder msg = new StringBuilder("Sending message: ");
            try {
                msg.append("Message-ID=" + mm.getMessageID());
            } catch (MessagingException e) {
                msg.append(e);
            }
            if (origMsgId > 0)
                msg.append(", origMsgId=" + origMsgId);
            if (uploads != null && uploads.size() > 0)
                msg.append(", uploads=" + uploads);
            if (replyType != null)
                msg.append(", replyType=" + replyType);
            ZimbraLog.smtp.info(msg);
        }
    }

    private static final String X_ORIGINATING_IP = "X-Originating-IP";

    void updateHeaders(MimeMessage mm, Account acct, Account authuser, OperationContext octxt, String originIP, boolean replyToSender)
    throws MessagingException, ServiceException {
        if (originIP != null) {
            Provisioning prov = Provisioning.getInstance();
            boolean addOriginatingIP = prov.getConfig().getBooleanAttr(Provisioning.A_zimbraSmtpSendAddOriginatingIP, true);
            if (addOriginatingIP)
                mm.addHeader(X_ORIGINATING_IP, "[" + originIP + "]");
        }

        boolean overrideFromHeader = true;
        try {
            String fromHdr = mm.getHeader("From", null);
            if (fromHdr != null && !fromHdr.equals("")) {
                InternetAddress from = new InternetAddress(fromHdr);
                if (AccountUtil.allowFromAddress(acct, from.getAddress()))
                    overrideFromHeader = false;
            }
        } catch (Exception e) { }

        // we need to set the Sender to the authenticated user for delegated sends by non-admins
        boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
        boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());
        boolean noSenderRequired = !isDelegatedRequest || AccessManager.getInstance().canAccessAccount(authuser, acct, isAdminRequest);
        InternetAddress sender = noSenderRequired ? null : AccountUtil.getFriendlyEmailAddress(authuser);

        // if the call doesn't require a Sender but the caller supplied one, pass it through if it's acceptable
        if (noSenderRequired) {
            Address addr = mm.getSender();
            if (addr != null && addr instanceof InternetAddress) {
                if (AccountUtil.addressMatchesAccount(authuser, ((InternetAddress) addr).getAddress()))
                    sender = (InternetAddress) addr;
            }
        }

        // set various headers on the outgoing message
        if (overrideFromHeader)
            mm.setFrom(AccountUtil.getFriendlyEmailAddress(acct));
        mm.setSentDate(new Date());
        mm.setSender(sender);
        if (sender == null) {
            Address[] existingReplyTos = mm.getReplyTo();
            if (existingReplyTos == null || existingReplyTos.length == 0) {
                String replyTo = acct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
                if (replyTo != null && !replyTo.trim().equals(""))
                    mm.setHeader("Reply-To", replyTo);
            }
        } else {
            if (replyToSender)
                mm.setReplyTo(new Address[] {sender});
        }

        if (isDelegatedRequest && mm instanceof FixedMimeMessage) {
            // set MAIL FROM to authenticated user for bounce purposes
            String mailfrom = (sender != null ? sender : AccountUtil.getFriendlyEmailAddress(authuser)).getAddress();
            Properties props = new Properties(JMSession.getSession().getProperties());
            props.setProperty("mail.smtp.from", mailfrom);
            ((FixedMimeMessage) mm).setSession(Session.getInstance(props));
        }

        mm.saveChanges();
    }

    private void sendMessage(final MimeMessage mm, final boolean ignoreFailedAddresses, final RollbackData[] rollback)
    throws SafeMessagingException {
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
        } catch (SendFailedException e) {
            for (RollbackData rdata : rollback)
                if (rdata != null)
                    rdata.rollback();
            throw new SafeSendFailedException(e);
        } catch (MessagingException e) {
            for (RollbackData rdata : rollback)
                if (rdata != null)
                    rdata.rollback();
            throw new SafeMessagingException(e);
        } catch (RuntimeException e) {
            for (RollbackData rdata : rollback)
                if (rdata != null)
                    rdata.rollback();
            throw e;
        } 
    }

    /**
     * Class that avoids JavaMail bug that throws OutOfMemoryError when sending
     * a message with many recipients and SMTP server rejects many of them.
     * The bug is in MessagingException.toString().
     */
    public static class SafeMessagingException extends MessagingException {
        private static final long serialVersionUID = -4652297855877992478L;
        private MessagingException mMex;

        public SafeMessagingException(MessagingException mex) {
            mMex = mex;
            setStackTrace(mMex.getStackTrace());
        }

        public String getMessage() {
            String msg = super.getMessage();
            if (msg == null) {
                Exception next = mMex.getNextException();
                if (next != null)
                    msg = next.getLocalizedMessage();
            }
            return msg;
        }

        public synchronized String toString() {
            StringBuffer sb = new StringBuffer();
            appendException(sb, this);
            Exception n = mMex.getNextException();
            int more = 0;
            while (n != null) {
                if (more == 0) {
                    sb.append("; chained exception is:\n\t");
                    appendException(sb, n);
                }
                if (n instanceof MessagingException) {
                    MessagingException mex = (MessagingException) n;
                    n = mex.getNextException();
                    if (n != null)
                        more++;
                } else {
                    // n != null && !(n instanceof MessagingException)
                    break;
                }
            }
            if (more > 0) {
                sb.append("\n\t(").append(more).append(" more chained exception");
                if (more > 1)
                    sb.append('s');
                sb.append(')');
            }
            return sb.toString();
        }

        private static StringBuffer appendException(StringBuffer sb, Exception e) {
            // pretty much a copy of Throwable.toString()
            sb.append(e.getClass().getName());
            String message = e.getLocalizedMessage();
            if (message != null)
                sb.append(": ").append(message);
            return sb;
        }
    }

    public static class SafeSendFailedException extends SafeMessagingException {
        private static final long serialVersionUID = 5625565177360027934L;
        private SendFailedException mSfe;

        public SafeSendFailedException(SendFailedException sfe) {
            super(sfe);
            mSfe = sfe;
        }

        public Address[] getInvalidAddresses() {
            return mSfe.getInvalidAddresses();
        }

        public Address[] getValidSentAddresses() {
            return mSfe.getValidSentAddresses();
        }

        public Address[] getValidUnsentAddresses() {
            return mSfe.getValidUnsentAddresses();
        }
    }
}
