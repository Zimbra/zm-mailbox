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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
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
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailclient.smtp.InvalidRecipientException;
import com.zimbra.cs.mailclient.smtp.SmtpConfig;
import com.zimbra.cs.mailclient.smtp.SmtpConnection;
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

    private Boolean mSaveToSent;
    private Collection<InternetAddress> mSaveContacts;
    private Collection<Upload> mUploads;
    private ItemId mOriginalMessageId;
    private String mReplyType;
    private Identity mIdentity;
    private Boolean mIgnoreFailedAddresses;
    private boolean mReplyToSender = false;
    private boolean mSkipSendAsCheck = false;
    private boolean mTrackBadHosts = true;
    private List<InternetAddress> mInvalidRecipients = new ArrayList<InternetAddress>();
    private boolean mWasConnectionFailure = false;
    
    public MailSender()  { }
    
    /**
     * Specifies the contacts to save in the <tt>Emailed Contacts</tt> folder.
     * @param savedContacts contacts or <tt>null</tt> 
     */
    public MailSender setSaveContacts(Collection<InternetAddress> saveContacts) {
        mSaveContacts = saveContacts;
        return this;
    }
    
    /**
     * Specifies the uploads to attach to the outgoing message.
     * @param uploads the uploads or <tt>null</tt>
     */
    public MailSender setUploads(Collection<Upload> uploads) {
        mUploads = uploads;
        return this;
    }

    /**
     * Specifies the original message id when replying or forwarding.
     */
    public MailSender setOriginalMessageId(ItemId id) {
        mOriginalMessageId = id;
        return this;
    }

    /**
     * Specifies the reply type: {@link #MSGTYPE_REPLY}, {@link #MSGTYPE_FORWARD}
     * or <tt>null</tt> if this is not a reply.  If the reply type is specified,
     * we set the {@link Flag#ID_FLAG_FORWARDED} or {@link Flag#ID_FLAG_REPLIED}
     * flag on the original message.
     */
    public MailSender setReplyType(String replyType) {
        mReplyType = replyType;
        return this;
    }

    /**
     * Specifies the identity to use, or <tt>null</tt> for the default identity.
     */
    public MailSender setIdentity(Identity identity) {
        mIdentity = identity;
        return this;
    }

    /**
     * @param ignore if <tt>true</tt>, don't throw a {@link SendFailedException}
     * when a message send fails.  The default is <tt>false</tt>.
     */
    public MailSender setIgnoreFailedAddresses(boolean ignore) {
        mIgnoreFailedAddresses = ignore;
        return this;
    }
    
    /**
     * @param replyToSender if <tt>true</tt>, set the <tt>Reply-To<tt>
     * header to the same value as the <tt>Sender</tt> header.  The
     * default is <tt>false</tt>.
     */
    public MailSender setReplyToSender(boolean replyToSender) {
        mReplyToSender = replyToSender;
        return this;
    }
    
    /**
     * @param skip if <tt>true</tt>, don't confirm that the user can send
     * the message from the specified address.  The default is <tt>false</tt>.
     */
    public MailSender setSkipSendAsCheck(boolean skip) {
        mSkipSendAsCheck = skip;
        return this;
    }

    /**
     * @param saveToSent if <tt>true</tt>, save the message to the user's
     * <tt>Sent</tt> folder after sending.  The default is the account's
     * <tt>zimbraPrefSaveToSent</tt> attribute value.
     */
    public MailSender setSaveToSent(boolean saveToSent) {
        mSaveToSent = saveToSent;
        return this;
    }

    /**
     * If <tt>true</tt>, calls {@link JMSession#markSmtpHostBad(String)} when
     * a connection to an SMTP host fails.  The default is <tt>true</tt>.
     */
    public MailSender setTrackBadHosts(boolean track) {
        mTrackBadHosts = track;
        return this;
    }

    /**
     * Returns <tt>true</tt> if an error occurred while connecting to the
     * SMTP server. 
     */
    public boolean wasConnectionFailure() {
        return mWasConnectionFailure;
    }

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
     * Sets member variables and sends the message.
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

        return sendMimeMessage(octxt, mbox, null, mm, newContacts, uploads, origMsgId, replyType, identity,
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

    /**
     * Sets member variables and sends the message.
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, Boolean saveToSent, MimeMessage mm,
                                  Collection<InternetAddress> saveContacts, Collection<Upload> uploads,
                                  ItemId origMsgId, String replyType, Identity identity,
                                  boolean ignoreFailedAddresses, boolean replyToSender)
    throws ServiceException {
        mSaveToSent = saveToSent;
        mSaveContacts = saveContacts;
        mUploads = uploads;
        mOriginalMessageId = origMsgId;
        mReplyType = replyType;
        mIdentity = identity;
        mIgnoreFailedAddresses = ignoreFailedAddresses;
        mReplyToSender = replyToSender;
        return sendMimeMessage(octxt, mbox, mm);
    }
    
    /**
     * Sends a message.
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm)
    throws ServiceException {
        Address[] allRecipients = null;
        
        try {
            Account acct = mbox.getAccount();
            Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
            boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
            if (authuser == null)
                authuser = acct;
            boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());

            if (mSaveToSent == null)
                mSaveToSent = authuser.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, true);

            // slot the message in the parent's conversation if subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (mOriginalMessageId != null && !isDelegatedRequest && mOriginalMessageId.belongsTo(mbox))
                convId = mbox.getConversationIdFromReferent(mm, mOriginalMessageId.getId());

            // set the From, Sender, Date, Reply-To, etc. headers
            updateHeaders(mm, acct, authuser, octxt, octxt != null ? octxt.getRequestIP() : null, mReplyToSender, mSkipSendAsCheck);

            // run any pre-send/pre-save MIME mutators
            try {
                for (Class<? extends MimeVisitor> vclass : MimeVisitor.getMutators())
                    vclass.newInstance().accept(mm);
            } catch (Exception e) {
                ZimbraLog.smtp.warn("failure to modify outbound message; aborting send", e);
                throw ServiceException.FAILURE("mutator error; aborting send", e);
            }

            // don't save if the message doesn't actually get *sent*
            allRecipients = mm.getAllRecipients();
            boolean hasRecipients = (allRecipients != null);
            mSaveToSent &= hasRecipients;

            // #0 is the authenticated user's, #1 is the send-as user's
            RollbackData[] rollback = new RollbackData[2];
            Object authMailbox = isDelegatedRequest ? null : mbox;

            // if requested, save a copy of the message to the Sent Mail folder
            ParsedMessage pm = null;
            if (mSaveToSent) {
                if (mIdentity == null)
                    mIdentity = Provisioning.getInstance().getDefaultIdentity(authuser);

                // figure out where to save the save-to-sent copy
                if (authMailbox == null)
                    authMailbox = getAuthenticatedMailbox(octxt, authuser, isAdminRequest);

                if (authMailbox instanceof Mailbox) {
                    Mailbox mboxSave = (Mailbox) authMailbox;
                    int flags = Flag.BITMASK_FROM_ME;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mboxSave.attachmentsIndexingEnabled());

                    // save it to the requested folder
                    int sentFolderId = getSentFolderId(mboxSave, mIdentity);
                    Message msg = mboxSave.addMessage(octxt, pm, sentFolderId, true, flags, null, convId);
                    rollback[0] = new RollbackData(msg);
                } else if (authMailbox instanceof ZMailbox) {
                    ZMailbox zmbxSave = (ZMailbox) authMailbox;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());

                    // save it to the requested folder
                    String sentFolder = mIdentity.getAttr(Provisioning.A_zimbraPrefSentMailFolder, "" + Mailbox.ID_FOLDER_SENT);
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

            // actually send the message via SMTP
            Collection<Address> sentAddresses = sendMessage(mbox, mm, rollback);
            if (!mInvalidRecipients.isEmpty()) {
                // Partial was allowed, some recipients were rejected.
                throw MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE(null, mInvalidRecipients);
            }

            if (!sentAddresses.isEmpty() && octxt != null) {
                try {
                    ContactRankings.increment(octxt.getAuthenticatedUser().getId(), sentAddresses);
                } catch (Exception e) {
                    ZimbraLog.smtp.error("unable to update contact rankings", e);
                }
                if (authuser.getBooleanAttr(Provisioning.A_zimbraPrefAutoAddAddressEnabled, false)) {
                    Collection<InternetAddress> newContacts = getNewContacts(sentAddresses, authuser, octxt, authMailbox);
                    saveNewContacts(newContacts, octxt, authMailbox);
                }
            }
            
            // Send intercept if save-to-sent didn't do it already.
            if (!mSaveToSent) {
                try {
                    Notification.getInstance().interceptIfNecessary(mbox, mm, "send message", null);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.error("Unable to send legal intercept message.", e);
                }
            }

            // check if this is a reply, and if so flag the msg appropriately
            if (mOriginalMessageId != null && !isDelegatedRequest && mOriginalMessageId.belongsTo(mbox)) {
                try {
                    if (MSGTYPE_REPLY.equals(mReplyType))
                        mbox.alterTag(octxt, mOriginalMessageId.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_REPLIED, true);
                    else if (MSGTYPE_FORWARD.equals(mReplyType))
                        mbox.alterTag(octxt, mOriginalMessageId.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FORWARDED, true);
                } catch (ServiceException e) {
                    // this is not an error case:
                    //   - the original message may be gone when accepting/declining an appointment
                    //   - we may have PERM_DENIED on delegated send
                }
            }


            // we can now purge the uploaded attachments
            if (mUploads != null)
                FileUploadServlet.deleteUploads(mUploads);

            // save contacts explicitly requested by the client to the personal address book
            if (false && mSaveContacts != null && !mSaveContacts.isEmpty()) {
                if (authMailbox == null)
                    authMailbox = getAuthenticatedMailbox(octxt, authuser, isAdminRequest);
                saveNewContacts(mSaveContacts, octxt, authMailbox);
            }

            return (rollback[0] != null ? rollback[0].msgId : null);
        } catch (InvalidRecipientException e) {
            throw MailServiceException.SEND_ABORTED_ADDRESS_FAILURE(e.getMessage(), e, getAddress(allRecipients, e.getRecipient()));
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Unable to send message", ioe);
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("Unable to send message", me);
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

    public void logMessage(MimeMessage mm, SmtpConfig config, ItemId origMsgId, Collection<Upload> uploads, String replyType) {
        // Log sent message info
        if (ZimbraLog.smtp.isInfoEnabled()) {
            StringBuilder msg = new StringBuilder(
                String.format("Sending message to MTA at %s, port %d",
                    config.getHost(), config.getPort()));
            try {
                msg.append(": Message-ID=" + mm.getMessageID());
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
        boolean canSendAs = !isDelegatedRequest || AccessManager.getInstance().canDo(authuser, acct, User.R_sendAs, isAdminRequest);

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

        if (mm instanceof FixedMimeMessage) {
            FixedMimeMessage fmm = (FixedMimeMessage) mm;
            Session session = fmm.getSession() != null ? fmm.getSession() : JMSession.getSession();
            // set MAIL FROM to authenticated user for bounce purposes
            String mailfrom = (sender != null ? sender : AccountUtil.getFriendlyEmailAddress(authuser)).getAddress();
            session.getProperties().setProperty("mail.smtp.from", mailfrom);
            fmm.setSession(session);
        }

        mm.saveChanges();
    }

    /*
     * returns a Collection of successfully sent recipient Addresses
     */
    protected Collection<Address> sendMessage(Mailbox mbox, final MimeMessage mm, final RollbackData[] rollback)
    throws MessagingException, IOException, ServiceException  {
        Address[] rcptAddresses = mm.getAllRecipients();
        if (rcptAddresses == null || rcptAddresses.length == 0) {
            // Calendar code can call sendMessage() with a message that has no recipients. 
            ZimbraLog.smtp.debug("Not sending message that has no recipients.");
            return Collections.emptyList();
        }
        
        // Get SMTP hosts.
        List<String> hosts = new ArrayList<String>();
        hosts.addAll(JMSession.getSmtpHosts(mbox.getAccount()));
        if (hosts.isEmpty()) {
            throw ServiceException.FAILURE("No available SMTP hosts.", null);
        }
        if (hosts.size() > 1) {
            Collections.shuffle(hosts);
        }
        
        // Connect to the SMTP server.
        SmtpConnection smtp = null;
        SmtpConfig config = null;
        for (int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);
            config = JMSession.getSmtpConfig(mbox.getAccount(), host);
            smtp = new SmtpConnection(config);
            try {
                smtp.connect();
                break; // Successful connection
            } catch (IOException e) {
                if (i == hosts.size() - 1) {
                    // Last host.
                    ZimbraLog.smtp.error("Unable to connect to SMTP server: %s", host);
                    mWasConnectionFailure = true;
                    throw MailServiceException.TRY_AGAIN("Unable to connect to the MTA", e);
                } else {
                    // Retry.
                    if (mTrackBadHosts) {
                        JMSession.markSmtpHostBad(host);
                    }
                    ZimbraLog.smtp.warn("Unable to connect to SMTP server: %s.  Retrying.", host, e);
                }
            }
        }

        // Send the message.
        logMessage(mm, config, mOriginalMessageId, mUploads, mReplyType);
        
    	String[] rcptStrings = new String[rcptAddresses.length];
    	for (int i = 0; i < rcptAddresses.length; i++) {
    	    rcptStrings[i] = rcptAddresses[i].toString();
    	}
    	if (mIgnoreFailedAddresses != null) {
    	    config.setAllowPartialSend(mIgnoreFailedAddresses);
    	} else {
    	    mIgnoreFailedAddresses = config.isPartialSendAllowed();
    	}
        Set<Address> sentAddresses = new HashSet<Address>();
    	
        try {
            smtp.sendMessage(mbox.getAccount().getName(), rcptStrings, mm);
            for (String invalid : smtp.getInvalidRecipients()) {
                InternetAddress address = getAddress(rcptAddresses, invalid);
                if (address != null) {
                    mInvalidRecipients.add(address);
                }
            }
                
            return sentAddresses;
        } catch (IOException e) {
            for (RollbackData rdata : rollback)
                if (rdata != null)
                    rdata.rollback();
            throw e;
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
    
    private InternetAddress getAddress(Address[] addresses, String addressString) {
        if (addresses == null) {
            return null;
        }
        for (Address address : addresses) {
            if (address != null && ((InternetAddress) address).getAddress().equalsIgnoreCase(addressString)) {
                return (InternetAddress) address;
            }
        }
        return null;
    }
    
    public List<InternetAddress> getNewContacts(Collection<Address> contacts, Account authuser, OperationContext octxt, Object authmbox) {
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
    
    public void saveNewContacts(Collection<InternetAddress> newContacts, OperationContext octxt, Object authMailbox) {
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
}
