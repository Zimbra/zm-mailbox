/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZContactHit;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZSearchResult;

public class MailSender {

    public static final String MSGTYPE_REPLY = Flag.getAbbreviation(Flag.ID_FLAG_REPLIED) + "";
    public static final String MSGTYPE_FORWARD = Flag.getAbbreviation(Flag.ID_FLAG_FORWARDED) + "";
    
    public MailSender()  { }

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

    public static enum ReplyForwardType {
        ORIGINAL,  // This is an original message; not a reply or forward.
        REPLY,     // Reply to another message
        FORWARD    // Forwarding another message
    }

    /**
     * Send MimeMessage out as an email.
     * @param octxt operation context
     * @param mbox mailbox
     * @param mm the outgoing message
     * @param newContacts contacts to add, or <tt>null</tt>
     * @param uploads uploads to attach, or <tt>null</tt>
     * @param origMsgId if replying or forwarding, item ID of original message
     * @param replyType {@link #MSGTYPE_REPLY} if this is a reply, {@link #MSGTYPE_FORWARD}
     *     if this is a forward, or <tt>null</tt>
     * @param identityId the id of the identity to send as, or <tt>null</tt> to use the default
     * @param ignoreFailedAddresses If TRUE, then will attempt to send even if
     *                              some addresses fail (no error is returned!)
     * @param replyToSender if true and if setting Sender header, set Reply-To
     *                      header to the same address, otherwise don't set
     *                      Reply-To, letting the recipient MUA choose to whom
     *                      to send reply
     * 
     * @return the id of the copy in the first saved folder, or <tt>null</tt>
     * @throws ServiceException
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm,
                                  List<InternetAddress> newContacts, List<Upload> uploads,
                                  ItemId origMsgId, String replyType, String identityId,
                                  boolean ignoreFailedAddresses, boolean replyToSender)
    throws ServiceException {
        Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
        if (authuser == null)
            authuser = mbox.getAccount();
        Identity identity = null;
        if (identityId != null)
            identity = Provisioning.getInstance().get(authuser, IdentityBy.id, identityId);
        if (identity == null)
            identity = Provisioning.getInstance().getDefaultIdentity(authuser); 

        boolean saveToSent = authuser.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, true);

        return sendMimeMessage(octxt, mbox, saveToSent, mm, newContacts, uploads, origMsgId, replyType, identity,
                               ignoreFailedAddresses, replyToSender);
    }

    protected static class RollbackData {
        Mailbox mbox;
        ZMailbox zmbox;
        ItemId msgId;

        RollbackData(Mailbox m, int i)  { mbox = m;  msgId = new ItemId(mbox, i); }
        RollbackData(Message msg)       { mbox = msg.getMailbox();  msgId = new ItemId(msg); }
        RollbackData(ZMailbox z, Account a, String s) throws ServiceException {
            zmbox = z;  msgId = new ItemId(s, a.getId());
        }

        public void rollback() {
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

    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, boolean saveToSent, MimeMessage mm,
                                  List<InternetAddress> saveContacts, List<Upload> uploads,
                                  ItemId origMsgId, String replyType, Identity identity,
                                  boolean ignoreFailedAddresses, boolean replyToSender)
    throws ServiceException {
        return sendMimeMessage(octxt, mbox, saveToSent, mm, saveContacts, uploads, origMsgId, replyType,
                               identity, ignoreFailedAddresses, replyToSender, false);
    }
    /**
     * Send MimeMessage out as an email.
     * @param octxt operation context
     * @param mbox mailbox
     * @param mm the outgoing message
     * @param saveContacts contacts to save in Emailed Contacts folder, or <tt>null</tt>
     * @param uploads uploads to attach, or <tt>null</tt>
     * @param origMsgId if replying or forwarding, item ID of original message
     * @param replyType {@link #MSGTYPE_REPLY} if this is a reply, {@link #MSGTYPE_FORWARD}
     *     if this is a forward, or <tt>null</tt>
     * @param identity the identity to send as, or <tt>null</tt> to use the default
     * @param ignoreFailedAddresses If TRUE, then will attempt to send even if
     *                              some addresses fail (no error is returned!)
     * @param replyToSender if true and if setting Sender header, set Reply-To
     *                      header to the same address, otherwise don't set
     *                      Reply-To, letting the recipient MUA choose to whom
     *                      to send reply
     * @param skipSendAsCheck if true, skip the check that disallows From address that's not explicitly allowed
     * 
     * @return the id of the copy in the first saved folder, or <tt>null</tt>
     * @throws ServiceException
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, boolean saveToSent, MimeMessage mm,
                                  List<InternetAddress> saveContacts, List<Upload> uploads,
                                  ItemId origMsgId, String replyType, Identity identity,
                                  boolean ignoreFailedAddresses, boolean replyToSender,
                                  boolean skipSendAsCheck)
    throws ServiceException {
        try {
            Account acct = mbox.getAccount();
            Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
            boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
            if (authuser == null)
                authuser = acct;
            boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());

            // slot the message in the parent's conversation if subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (origMsgId != null && !isDelegatedRequest && origMsgId.belongsTo(mbox))
                convId = mbox.getConversationIdFromReferent(mm, origMsgId.getId());

            // set the From, Sender, Date, Reply-To, etc. headers
            updateHeaders(mm, acct, authuser, octxt, octxt != null ? octxt.getRequestIP() : null, replyToSender, skipSendAsCheck);

            // run any pre-send/pre-save MIME mutators
            try {
                for (Class<? extends MimeVisitor> vclass : MimeVisitor.getMutators())
                    vclass.newInstance().accept(mm);
            } catch (Exception e) {
                ZimbraLog.smtp.warn("failure to modify outbound message; aborting send", e);
                throw ServiceException.FAILURE("mutator error; aborting send", e);
            }

            // don't save if the message doesn't actually get *sent*
            boolean hasRecipients = (mm.getAllRecipients() != null);
            saveToSent &= hasRecipients;

            // #0 is the authenticated user's, #1 is the send-as user's
            RollbackData[] rollback = new RollbackData[2];
            Object authMailbox = isDelegatedRequest ? null : mbox;

            // if requested, save a copy of the message to the Sent Mail folder
            ParsedMessage pm = null;
            if (saveToSent) {
                if (identity == null)
                    identity = Provisioning.getInstance().getDefaultIdentity(authuser);

                // figure out where to save the save-to-sent copy
                if (authMailbox == null)
                    authMailbox = getAuthenticatedMailbox(octxt, authuser, isAdminRequest);

                if (authMailbox instanceof Mailbox) {
                    Mailbox mboxSave = (Mailbox) authMailbox;
                    int flags = Flag.BITMASK_FROM_ME;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mboxSave.attachmentsIndexingEnabled());

                    // save it to the requested folder
                    int sentFolderId = getSentFolderId(mboxSave, identity);
                    Message msg = mboxSave.addMessage(octxt, pm, sentFolderId, true, flags, null, convId);
                    rollback[0] = new RollbackData(msg);
                } else if (authMailbox instanceof ZMailbox) {
                    ZMailbox zmbxSave = (ZMailbox) authMailbox;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());

                    // save it to the requested folder
                    String sentFolder = identity.getAttr(Provisioning.A_zimbraPrefSentMailFolder, "" + Mailbox.ID_FOLDER_SENT);
                    String msgId = zmbxSave.addMessage(sentFolder, "s", null, mm.getSentDate().getTime(), pm.getRawData(), true);
                    rollback[0] = new RollbackData(zmbxSave, authuser, msgId);
                }
            }

            // for delegated sends where the authenticated user is reflected in the Sender header (c.f. updateHeaders),
            //   automatically save a copy to the "From" user's mailbox
            Message msg = null;
            if (hasRecipients && isDelegatedRequest && mm.getSender() != null && acct.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, true)) { 
                int flags = Flag.BITMASK_UNREAD | Flag.BITMASK_FROM_ME;
                // save the sent copy using the target's credentials, as the sender doesn't necessarily have write access
                OperationContext octxtTarget = new OperationContext(acct);
                if (pm == null || pm.isAttachmentIndexingEnabled() != mbox.attachmentsIndexingEnabled())
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());
                int sentFolderId = getSentFolderId(mbox, Provisioning.getInstance().getDefaultIdentity(acct));
                msg = mbox.addMessage(octxtTarget, pm, sentFolderId, true, flags, null, convId);
                rollback[1] = new RollbackData(msg);
            }

            // Apply SMTP session settings from the account and domain.  We do this here
            // because we can't assume that the callsite passed in the domain when creating
            // the FixedMimeMessage.
            if (mm instanceof FixedMimeMessage) {
                ((FixedMimeMessage) mm).setSession(JMSession.getSmtpSession(acct));
            }
            logMessage(mm, origMsgId, uploads, replyType);

            // actually send the message via SMTP
            Collection<Address> sentAddresses = sendMessage(mbox, mm, ignoreFailedAddresses, rollback);
        	Collection<InternetAddress> newContacts = null;

            if (!sentAddresses.isEmpty() && octxt != null) {
            	try {
                	ContactRankings.increment(octxt.getAuthenticatedUser().getId(), sentAddresses);
            	} catch (Exception e) {
            		ZimbraLog.smtp.error("unable to update contact rankings", e);
            	}
            	newContacts = getNewContacts(sentAddresses, authuser, octxt, authMailbox);
            	if (authuser.getBooleanAttr(Provisioning.A_zimbraPrefAutoAddAddressEnabled, false))
            		saveNewContacts(newContacts, octxt, authMailbox);
            }
            
            // Send intercept if save-to-sent didn't do it already.
            if (!saveToSent) {
                try {
                    Notification.getInstance().interceptIfNecessary(mbox, mm, "send message", null);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.error("Unable to send lawful intercept message.", e);
                }
            }

            // check if this is a reply, and if so flag the msg appropriately
            if (origMsgId != null && !isDelegatedRequest && origMsgId.belongsTo(mbox)) {
                try {
                    if (MSGTYPE_REPLY.equals(replyType))
                        mbox.alterTag(octxt, origMsgId.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_REPLIED, true);
                    else if (MSGTYPE_FORWARD.equals(replyType))
                        mbox.alterTag(octxt, origMsgId.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FORWARDED, true);
                } catch (ServiceException e) {
                    // this is not an error case:
                    //   - the original message may be gone when accepting/declining an appointment
                    //   - we may have PERM_DENIED on delegated send
                }
            }


            // we can now purge the uploaded attachments
            if (uploads != null)
                FileUploadServlet.deleteUploads(uploads);

            // save contacts explicitly requested by the client to the personal address book
            if (false && saveContacts != null && !saveContacts.isEmpty()) {
                if (authMailbox == null)
                    authMailbox = getAuthenticatedMailbox(octxt, authuser, isAdminRequest);
                saveNewContacts(saveContacts, octxt, authMailbox);
            }

            return (rollback[0] != null ? rollback[0].msgId : null);

        } catch (SafeSendFailedException sfe) {
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
                msg.append(".  ").append(sfe.toString());

                if (Provisioning.getInstance().getLocalServer().isSmtpSendPartial())
                    throw MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE(msg.toString(), sfe, invalidAddrs, validUnsentAddrs);
                else
                    throw MailServiceException.SEND_ABORTED_ADDRESS_FAILURE(msg.toString(), sfe, invalidAddrs, validUnsentAddrs);
            } else {
                throw MailServiceException.SEND_FAILURE("SMTP server reported: " + sfe.getMessage(), sfe, invalidAddrs, validUnsentAddrs);
            }
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Unable to send message", ioe);
        } catch (MessagingException me) {
            Exception chained = me.getNextException();
            if (chained instanceof ConnectException || chained instanceof UnknownHostException) {
                throw MailServiceException.TRY_AGAIN("Unable to connect to the MTA", chained);
            } else {
                throw ServiceException.FAILURE("Unable to send message", me);
            }
        }
    }
    
    private Object getAuthenticatedMailbox(OperationContext octxt, Account authuser, boolean isAdminRequest) {
        try {
            if (Provisioning.onLocalServer(authuser)) {
                return MailboxManager.getInstance().getMailboxByAccount(authuser);
            } else {
                String uri = AccountUtil.getSoapUri(authuser);
                if (uri == null)
                    return null;
                
                AuthToken authToken = null;
                if (octxt != null)
                    authToken = octxt.getAuthToken(false);
                if (authToken == null)
                    authToken = AuthProvider.getAuthToken(authuser, isAdminRequest);
                    
                ZMailbox.Options options = new ZMailbox.Options(authToken.toZAuthToken(), uri);
                options.setNoSession(true);
                return ZMailbox.getMailbox(options);
            }
        } catch (Exception e) {
            ZimbraLog.smtp.info("could not fetch home mailbox for delegated send", e);
            return null;
        }
    }

    private void logMessage(MimeMessage mm, ItemId origMsgId, List<Upload> uploads, String replyType) {
        // Log sent message info
        if (ZimbraLog.smtp.isInfoEnabled()) {
            StringBuilder msg = new StringBuilder("Sending message to MTA at ")
                .append(getSmtpHost(mm)).append(", port ").append(getSmtpPort(mm)).append(": ");
            try {
                msg.append("Message-ID=" + mm.getMessageID());
            } catch (MessagingException e) {
                msg.append(e);
            }
            if (origMsgId != null)
                msg.append(", origMsgId=" + origMsgId);
            if (uploads != null && uploads.size() > 0)
                msg.append(", uploads=" + uploads);
            if (replyType != null)
                msg.append(", replyType=" + replyType);
            ZimbraLog.smtp.info(msg);
        }
    }
    
    private String getSmtpHost(MimeMessage mm) {
        String host = "<unknown>";
        if (mm instanceof FixedMimeMessage) {
            host = ((FixedMimeMessage) mm).getSession().getProperty("mail.smtp.host");
        }
        return host;
    }
    
    private String getSmtpPort(MimeMessage mm) {
        String port = "<unknown>";
        if (mm instanceof FixedMimeMessage) {
            port = ((FixedMimeMessage) mm).getSession().getProperty("mail.smtp.port");
        }
        return port;
    }

    public static final String X_ORIGINATING_IP = "X-Originating-IP";
    private static final String X_MAILER = "X-Mailer";
    public static final String X_AUTHENTICATED_USER = "X-Authenticated-User"; 
    
    public static String formatXOrigIpHeader(String origIp) {
        return "[" + origIp + "]";
    }

    void updateHeaders(MimeMessage mm, Account acct, Account authuser, OperationContext octxt, String originIP,
                       boolean replyToSender, boolean skipSendAsCheck)
    throws MessagingException, ServiceException {
	    Provisioning prov = Provisioning.getInstance();
        if (originIP != null) {
            boolean addOriginatingIP = prov.getConfig().getBooleanAttr(Provisioning.A_zimbraSmtpSendAddOriginatingIP, true);
            if (addOriginatingIP)
                mm.addHeader(X_ORIGINATING_IP, formatXOrigIpHeader(originIP));
        }
        
        boolean addMailer = prov.getConfig().getBooleanAttr(Provisioning.A_zimbraSmtpSendAddMailer, true);
        if (addMailer) {
            String ua = octxt != null ? octxt.getUserAgent() : null;
            String mailer = "Zimbra " + BuildInfo.VERSION + (ua == null ? "" : " (" + ua + ")");
            mm.addHeader(X_MAILER, mailer);
        }

        if (prov.getConfig().getBooleanAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, false))
            mm.addHeader(X_AUTHENTICATED_USER, authuser.getName());

        boolean overrideFromHeader;
        if (skipSendAsCheck) {
            overrideFromHeader = false;
        } else {
            overrideFromHeader = true;
            try {
                String fromHdr = mm.getHeader("From", null);
                if (fromHdr != null && !fromHdr.equals("")) {
                    InternetAddress from = new InternetAddress(fromHdr);
                    if (AccountUtil.allowFromAddress(acct, from.getAddress()))
                        overrideFromHeader = false;
                }
            } catch (Exception e) { }
        }

        // we need to set the Sender to the authenticated user for delegated sends by non-admins
        boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
        boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());
        boolean canSendAs = !isDelegatedRequest || AccessManager.getInstance().canDo(authuser, acct, User.R_sendAs, isAdminRequest, false);

        InternetAddress sender = null;
        if (skipSendAsCheck) {
            Address addr = mm.getSender();
            if (addr != null && addr instanceof InternetAddress)
                sender = (InternetAddress) addr;
        } else {
            sender = canSendAs ? null : AccountUtil.getFriendlyEmailAddress(authuser);
            if (canSendAs) {
                // if the call doesn't require a Sender but the caller supplied one, pass it through if it's acceptable
                Address addr = mm.getSender();
                if (addr != null && addr instanceof InternetAddress) {
                    if (AccountUtil.addressMatchesAccount(authuser, ((InternetAddress) addr).getAddress()))
                        sender = (InternetAddress) addr;
                }
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

    /*
     * returns a Collection of successfully sent recipient Addresses
     */
    @SuppressWarnings("unused")
    protected Collection<Address> sendMessage(Mailbox mbox, final MimeMessage mm, final boolean ignoreFailedAddresses, final RollbackData[] rollback)
    throws SafeMessagingException, IOException  {
        // send the message via SMTP
    	HashSet<Address> sentAddresses = new HashSet<Address>();
        try {
            Address[] rcptAddresses = mm.getAllRecipients();
            while (rcptAddresses != null && rcptAddresses.length > 0) {
                try {
                    Transport.send(mm, rcptAddresses);
                    sentAddresses.addAll(Arrays.asList(rcptAddresses));
                    break;
                } catch (SendFailedException sfe) {
                    if (!ignoreFailedAddresses)
                        throw sfe;
                    rcptAddresses = sfe.getValidUnsentAddresses();
                    Address[] sent = sfe.getValidSentAddresses();
                    if (sent != null && sent.length > 0)
                        sentAddresses.addAll(Arrays.asList(sent));
                } catch (MessagingException e) {
                    Exception chained = e.getNextException(); 
                    if (chained instanceof ConnectException || chained instanceof UnknownHostException) {
                        ZimbraLog.smtp.warn("Unable to connect to SMTP server: %s.", chained.toString());
                        if (!(mm instanceof FixedMimeMessage)) {
                            ZimbraLog.smtp.warn("SMTP retry not supported for %s.", mm.getClass().getName());
                            throw e;
                        }
                        String newHost = updateSmtpHost((FixedMimeMessage) mm, mbox, e);
                        ZimbraLog.smtp.info("Attempting to send to %s.", newHost);
                    } else {
                        throw e;
                    }
                }
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
        return sentAddresses;
    }
    
    /**
     * Updates the <tt>mail.smtp.host</tt> property on the given message with a new
     * SMTP host.  Removes the bad host from the SMTP hostname cache, so that the
     * host is not retried for 60 seconds.
     * 
     * @return the new hostname
     */
    private String updateSmtpHost(FixedMimeMessage mm, Mailbox mbox, MessagingException originalException)
    throws MessagingException {
        Session session = mm.getSession();
        String badHost = session.getProperty("mail.smtp.host");
        JMSession.markSmtpHostBad(badHost);
        String smtpHost = null;
        
        try {
            Domain domain = Provisioning.getInstance().getDomain(mbox.getAccount());
            smtpHost = JMSession.getRandomSmtpHost(domain);
        } catch (ServiceException e) {
            ZimbraLog.smtp.error("Unable to update SMTP host.", e);
            throw originalException;
        }
        
        if (smtpHost == null) {
            ZimbraLog.smtp.error("No alternate SMTP host available.");
            throw originalException;
        }
        
        session.getProperties().setProperty("mail.smtp.host", smtpHost);
        return smtpHost;
    }


    private List<InternetAddress> getNewContacts(Collection<Address> contacts, Account authuser, OperationContext octxt, Object authmbox) {
        if (contacts.isEmpty())
            return Collections.emptyList();

        HashMap<String,InternetAddress> newContacts = new HashMap<String,InternetAddress>();
        Collection<String> emailKeys = new ContactAutoComplete(authuser.getId()).getEmailKeys();
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Address addr : contacts) {
            if (addr instanceof InternetAddress) {
                InternetAddress iaddr = (InternetAddress)addr;
                String email = iaddr.getAddress();
                newContacts.put(email, iaddr);
                for (String emailKey : emailKeys) {
                    if (first)
                        first = false;
                    else
                        buf.append(" OR ");
                    buf.append("#").append(emailKey).append(":").append(email);
                }
            }
        }
        
        byte[] types = { MailItem.TYPE_CONTACT };
        String query = buf.toString();
        try {
            if (authmbox instanceof Mailbox) {
                Mailbox mbox = (Mailbox) authmbox;
                ZimbraQueryResults qres = null;
                try {
                    qres = mbox.search(octxt, query, types, SortBy.NONE, contacts.size());
                    while (qres.hasNext()) {
                        ZimbraHit hit = qres.getNext();
                        if (hit instanceof ContactHit) {
                            Contact c = ((ContactHit) hit).getContact();
                            for (String emailKey : emailKeys) {
                                String v = c.get(emailKey);
                                if (v != null)
                                    newContacts.remove(v);
                            }
                        }
                    }
                } finally {
                    if (qres != null)
                        try { qres.doneWithSearchResults(); } catch (Exception e) {}
                }
            } else if (authmbox instanceof ZMailbox) {
                ZMailbox zmbox = (ZMailbox) authmbox;
                ZSearchResult res = zmbox.search(new ZSearchParams(query));
                for (ZSearchHit hit: res.getHits()) {
                    if (hit instanceof ZContactHit) {
                        ZContactHit c = (ZContactHit)hit;
                        String v = c.getEmail();
                        if (v != null)
                            newContacts.remove(v);
                        v = c.getEmail2();
                        if (v != null)
                            newContacts.remove(v);
                        v = c.getEmail3();
                        if (v != null)
                            newContacts.remove(v);
                        v = c.getWorkEmail1();
                        if (v != null)
                            newContacts.remove(v);
                        v = c.getWorkEmail2();
                        if (v != null)
                            newContacts.remove(v);
                        v = c.getWorkEmail3();
                        if (v != null)
                            newContacts.remove(v);
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.smtp.warn("ignoring error while auto-adding contact", e);
            newContacts.clear();
        } catch (IOException e) {
            ZimbraLog.smtp.warn("ignoring error while auto-adding contact", e);
            newContacts.clear();
        } catch (ParseException e) {
            ZimbraLog.smtp.warn("ignoring error while auto-adding contact", e);
            newContacts.clear();
        }
        
        List<InternetAddress> ret = new ArrayList<InternetAddress>();
        if (!newContacts.isEmpty())
            ret.addAll(newContacts.values());
        return ret;
    }
    
    private void saveNewContacts(Collection<InternetAddress> newContacts, OperationContext octxt, Object authMailbox) {
        for (InternetAddress inetaddr : newContacts) {
            ZimbraLog.smtp.debug("adding new contact: " + inetaddr);
            Map<String, String> fields = new ParsedAddress(inetaddr).getAttributes();
            try {
                if (authMailbox instanceof Mailbox) {
                    ParsedContact pc = new ParsedContact(fields);
                    ((Mailbox) authMailbox).createContact(octxt, pc, Mailbox.ID_FOLDER_AUTO_CONTACTS, null);
                } else if (authMailbox instanceof ZMailbox) {
                    ((ZMailbox) authMailbox).createContact("" + Mailbox.ID_FOLDER_AUTO_CONTACTS, null, fields);
                }
            } catch (ServiceException e) {
                ZimbraLog.smtp.warn("ignoring error while auto-adding contact", e);
            }
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

        @Override
        public String getMessage() {
            return mMex.getMessage();
        }
        
        @Override
        public Exception getNextException() {
            return mMex.getNextException();
        }

        @Override
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
