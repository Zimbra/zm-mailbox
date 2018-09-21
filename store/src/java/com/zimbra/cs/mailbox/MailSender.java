/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.PrefDelegatedSendSaveTarget;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.mime.shim.JavaMailInternetHeaders;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Threader.ThreadIndex;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeProcessor;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;

public class MailSender {

    public static final String MSGTYPE_REPLY = String.valueOf(Flag.toChar(Flag.ID_REPLIED));
    public static final String MSGTYPE_FORWARD = String.valueOf(Flag.toChar(Flag.ID_FORWARDED));
    private static Map<String, PreSendMailListener> mPreSendMailListeners = new ConcurrentHashMap<String, PreSendMailListener>();

    private Boolean mSaveToSent;
    private Collection<Upload> mUploads;
    private ItemId mOriginalMessageId;
    private String mReplyType;
    private boolean mIsDataSourceSender;
    private Identity mIdentity;
    private Boolean mSendPartial;
    private boolean mReplyToSender = false;
    private boolean mRedirectMode = false;
    private boolean mCalendarMode = false;
    private boolean mSkipHeaderUpdate = false;
    private final List<String> mSmtpHosts = new ArrayList<String>();
    private Session mSession;
    private boolean mTrackBadHosts = true;
    private int mCurrentHostIndex = 0;
    private final List<String> mRecipients = new ArrayList<String>();
    private String mEnvelopeFrom;
    private String mDsn;
    private MimeProcessor mimeProcessor = null;

    public MailSender()  {
        mSession = JMSession.getSession();
    }

    public static enum DsnNotifyOption {
        NEVER,
        SUCCESS,
        FAILURE,
        DELAY
    }

    public MailSender setDsnNotifyOptions(DsnNotifyOption... dsnNotifyOptions) {
        if (dsnNotifyOptions == null || dsnNotifyOptions.length == 0) {
            mDsn = null;
        } else {
            List<DsnNotifyOption> listOptions = Arrays.asList(dsnNotifyOptions);
            if (listOptions.size() > 1 && listOptions.contains(DsnNotifyOption.NEVER))
                throw new IllegalArgumentException("DSN option 'NEVER' cannot be combined with others");
            mDsn = StringUtil.join(",", dsnNotifyOptions);
        }
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
     * @param if {@code true}, overrides the send partial behavior in the
     * JavaMail session.
     */
    public MailSender setSendPartial(boolean sendPartial) {
        mSendPartial = sendPartial;
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
     * Redirect mode allows any value for From and Sender headers.  This mode should
     * be used only for redirecting/bouncing mail.
     */
    public MailSender setRedirectMode(boolean onoff) {
        mRedirectMode = onoff;
        return this;
    }

    /**
     * Calendar mode allows anyone to send on behalf of anyone else without having
     * the sendOnBehalfOf right granted.  This seemingly contradictory behavior is
     * necessary to remain compatible with Outlook's invite forwarding mechanism.
     * @param onoff
     * @return
     */
    public MailSender setCalendarMode(boolean onoff) {
        mCalendarMode = onoff;
        return this;
    }

    /**
     * @param skip if <tt>true</tt>, leave message headers untouched during
     * the mail send process.  That is, do not update the Date, From, Sender,
     * etc. headers.  The default is <tt>false</tt>.
     */
    public MailSender setSkipHeaderUpdate(boolean skip) {
        mSkipHeaderUpdate = skip;
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
     * Sets an alternate JavaMail <tt>Session</tt> and SMTP hosts
     * that will be used to send the message from the account's
     * domain.  The default behavior is to use SMTP settings from
     * the <tt>Session<tt> on the {@link MimeMessage}.
     * @throws ServiceException
     */
    public MailSender setSession(Account account) throws ServiceException {
        try {
            mSession = JMSession.getSmtpSession(account);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get SMTP session for " + account, e);
        }
        Domain domain = Provisioning.getInstance().getDomain(account);
        mSmtpHosts.clear();
        mSmtpHosts.addAll(JMSession.getSmtpHosts(domain));
        return this;
    }

    /**
     * Sets JavaMail <tt>Session</tt> using the SMTP settings associated with the data source.
     * @throws ServiceException
     */
    public MailSender setSession(DataSource ds) throws ServiceException {
        mSession = JMSession.getSession(ds);
        return this;
    }

    /**
     * Specifies the recipients for the outgoing message.  The default
     * behavior is to use the recipients specified in the headers of
     * the <tt>MimeMessage</tt>.
     */
    public MailSender setRecipients(String ... recipients) {
        mRecipients.clear();
        if (recipients != null) {
            for (String recipient : recipients) {
                mRecipients.add(recipient);
            }
        }
        return this;
    }

    /**
     * Sets the address sent for <tt>MAIL FROM</tt> in the SMTP session.
     * Default behavior: if <tt>zimbraSmtpRestrictEnvelopeFrom</tt> is <tt>true</tt>,
     * <tt>MAIL FROM</tt> will always be set to the account's name.  Otherwise
     * it is set to the value of the <tt>Sender</tt> or <tt>From</tt> header
     * in the outgoing message, in that order.
     */
    public MailSender setEnvelopeFrom(String address) {
        mEnvelopeFrom = address;
        return this;
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

    public static int getSentFolderId(Mailbox mbox) throws ServiceException {
        int folderId = Mailbox.ID_FOLDER_SENT;
        String sentFolder = mbox.getAccount().getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
        if (sentFolder != null) {
            try {
                folderId = mbox.getFolderByPath(null, sentFolder).getId();
            } catch (NoSuchItemException nsie) { }
        }
        return folderId;
    }

    /**
     * Getter save to sent flag - exposed for OfflineMailSender
     */
    protected Boolean getSaveToSent() {
        return mSaveToSent;
    }

    /**
     * Getter for original message Id - exposed for OfflineMailSender
     */
    protected ItemId getOriginalMessageId() {
        return mOriginalMessageId;
    }

    /**
     * Getter for reply type - exposed for OfflineMailSender
     */
    protected String getReplyType() {
        return mReplyType;
    }

    /**
     * Getter for identity - exposed for OfflineMailSender
     */
    protected Identity getIdentity() {
        return mIdentity;
    }

    /**
     * Returns {@code true} if partial sends are allowed.
     */
    protected boolean isSendPartial() {
        if (mSendPartial != null) {
            return mSendPartial;
        }
        if (mSession != null) {
            String val = SystemUtil.coalesce(
                mSession.getProperty(JMSession.SMTP_SEND_PARTIAL_PROPERTY),
                mSession.getProperty(JMSession.SMTPS_SEND_PARTIAL_PROPERTY));
            if (val != null) {
                return Boolean.parseBoolean(val);
            }
        }
        return false;
    }

    /**
     * Getter for reply to sender flag - exposed for OfflineMailSender
     */
    protected boolean isReplyToSender() {
        return mReplyToSender;
    }

    /**
     * Getter for recipient List - exposed for OfflineMailSender
     */
    protected List<String> getRecipients() {
        return mRecipients;
    }

    /**
     * Get recipient addresses from either the member variable or the message.
     * @throws MessagingException
     */
    protected Address[] getRecipients(final MimeMessage mm) throws MessagingException {
        Address[] rcptAddresses = null;
        if (mRecipients.isEmpty()) {
            rcptAddresses = mm.getAllRecipients();
        } else {
            rcptAddresses = new Address[mRecipients.size()];
            for (int i = 0; i < rcptAddresses.length; i++) {
                rcptAddresses[i] = new JavaMailInternetAddress(mRecipients.get(i));
            }
        }
        return rcptAddresses;
    }

    /**
     * Getter for Collection of uploads - exposed for OfflineMailSender
     */
    protected Collection<Upload> getUploads() {
        return mUploads;
    }

    public static enum ReplyForwardType {
        ORIGINAL,  // This is an original message; not a reply or forward.
        REPLY,     // Reply to another message
        FORWARD    // Forwarding another message
    }

    /**
     * Sets member variables and sends the message.
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm, List<Upload> uploads,
            ItemId origMsgId, String replyType, String identityId, boolean replyToSender) throws ServiceException {
        return sendMimeMessage(octxt, mbox, mm, uploads, origMsgId, replyType, identityId, replyToSender, null);
    }

    /**
     * Sets member variables and sends the message.
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm, List<Upload> uploads,
            ItemId origMsgId, String replyType, String identityId, boolean replyToSender, MimeProcessor mimeProc) throws ServiceException {
        Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
        if (authuser == null) {
            authuser = mbox.getAccount();
        }
        Identity identity = null;
        if (identityId != null) {
            identity = Provisioning.getInstance().get(authuser, Key.IdentityBy.id, identityId);
        }
        return sendMimeMessage(octxt, mbox, null, mm, uploads, origMsgId, replyType, identity, replyToSender, mimeProc);
    }

    public ItemId sendDataSourceMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm, List<Upload> uploads,
            ItemId origMsgId, String replyType) throws ServiceException {
        return sendDataSourceMimeMessage(octxt, mbox, mm, uploads, origMsgId, replyType, null);
    }

    public ItemId sendDataSourceMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm, List<Upload> uploads,
            ItemId origMsgId, String replyType, MimeProcessor mimeProc) throws ServiceException {
        mIsDataSourceSender = true;
        ((Mime.FixedMimeMessage) mm).setSession(mSession);
        return sendMimeMessage(octxt, mbox, false, mm, uploads, origMsgId, replyType, null, false, mimeProc);
    }

    protected static class RollbackData {
        Mailbox mbox;
        ZMailbox zmbox;
        ItemId msgId;

        RollbackData(Mailbox m, int i) {
            mbox = m;  msgId = new ItemId(mbox, i);
        }

        RollbackData(Message msg) {
            mbox = msg.getMailbox();  msgId = new ItemId(msg);
        }

        RollbackData(ZMailbox z, Account a, String s) throws ServiceException {
            zmbox = z;  msgId = new ItemId(s, a.getId());
        }

        public void rollback() {
            try {
                if (mbox != null) {
                    mbox.delete(null, msgId.getId(), MailItem.Type.MESSAGE);
                } else {
                    zmbox.deleteMessage("" + msgId);
                }
            } catch (ServiceException e) {
                ZimbraLog.smtp.warn("ignoring error while deleting saved sent message: " + msgId, e);
            }
        }
    }

    /**
     * Sets member variables and sends the message.
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, Boolean saveToSent, MimeMessage mm,
            Collection<Upload> uploads, ItemId origMsgId, String replyType, Identity identity, boolean replyToSender, MimeProcessor mimeProc)
            throws ServiceException {
        mSaveToSent = saveToSent;
        mUploads = uploads;
        mOriginalMessageId = origMsgId;
        mReplyType = replyType;
        mIdentity = identity;
        mReplyToSender = replyToSender;
        mimeProcessor = mimeProc;
        return sendMimeMessage(octxt, mbox, mm);
    }

    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, Boolean saveToSent, MimeMessage mm,
            Collection<Upload> uploads, ItemId origMsgId, String replyType, Identity identity, boolean replyToSender)
            throws ServiceException {
           return sendMimeMessage(octxt, mbox, saveToSent, mm, uploads, origMsgId, replyType, identity, replyToSender, null);
    }

    /**
     * Sends a message.
     */
    public ItemId sendMimeMessage(OperationContext octxt, Mailbox mbox, MimeMessage mm)
    throws ServiceException {
        try {
            long maxSize = Provisioning.getInstance().getConfig().getMtaMaxMessageSize();
            int size = mm.getSize();
            if (size == -1) {
                size = (int) ByteUtil.getDataLength(Mime.getInputStream(mm));
            }

            if ((maxSize != 0 /* 0 means "no limit" */) && (size > maxSize)) {
                throw MailServiceException.MESSAGE_TOO_BIG(maxSize, size);
            }

            Account acct = mbox.getAccount();
            Account authuser = octxt == null ? null : octxt.getAuthenticatedUser();
            boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
            if (authuser == null) {
                authuser = acct;
            }
            boolean isDelegatedRequest = !acct.getId().equalsIgnoreCase(authuser.getId());
            boolean allowSaveToSent = true; // like mSaveToSent but not over-ridden by authuser peference

            if (mSaveToSent == null) {
                mSaveToSent = authuser.isPrefSaveToSent();
            } else {
                allowSaveToSent = mSaveToSent;
            }

            // slot the message in the parent's conversation if subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (mOriginalMessageId != null && !isDelegatedRequest && mOriginalMessageId.belongsTo(mbox)) {
                convId = mbox.getConversationIdFromReferent(mm, mOriginalMessageId.getId());
            }

            // set the From, Sender, Date, Reply-To, etc. headers
            updateHeaders(mm, acct, authuser, octxt, octxt != null ? octxt.getRequestIP() : null, mReplyToSender);

            // Determine envelope sender.
            if (mEnvelopeFrom == null) {
                if (acct.isSmtpRestrictEnvelopeFrom() && !isDataSourceSender()) {
                    mEnvelopeFrom = mbox.getAccount().getName();
                } else {
                    // Set envelope sender to Sender or Reply-To or From, in that order.
                    Address envAddress = mm.getSender();
                    if (envAddress == null) {
                        envAddress =  ArrayUtil.getFirstElement(mm.getReplyTo());
                    }
                    if (envAddress == null) {
                        envAddress = ArrayUtil.getFirstElement(mm.getFrom());
                    }
                    if (envAddress != null) {
                        mEnvelopeFrom = ((InternetAddress) envAddress).getAddress();
                    }
                }
            }

            // run any pre-send/pre-save MIME mutators
            try {
                for (Class<? extends MimeVisitor> vclass : MimeVisitor.getMutators()) {
                    vclass.newInstance().accept(mm);
                }
            } catch (Exception e) {
                ZimbraLog.smtp.warn("failure to modify outbound message; aborting send", e);
                throw ServiceException.FAILURE("mutator error; aborting send", e);
            }

            // don't save if the message doesn't actually get *sent*
            boolean hasRecipients = (mm.getAllRecipients() != null);
            mSaveToSent &= hasRecipients;

            LinkedList<RollbackData> rollbacks = new LinkedList<RollbackData>();
            Object authMailbox = isDelegatedRequest ? null : mbox;

            // Bug: 66823
            // createAutoContact() uses Lucene search to determine whether the address to be added in emailed contact;
            // If the user has SaveInSent preference set, a copy of the message is stored on Sent folder. With
            // zimbraBatchedIndexingSize is set to 1 or, with any search request the item stored in the sent folder
            // gets indexed right away which causes createAutoContact() to skip the sender addresses to be added.
            // To avoid this problem; let's determine a list of new address which we might need to add to emailed
            // contact later before we add the message to sent folder.
            Collection<Address> newAddrs = Collections.emptySet();
            Address[] rcptAddresses = getRecipients(mm);
            if (rcptAddresses != null && rcptAddresses.length > 0)
                newAddrs = mbox.newContactAddrs(Arrays.asList(rcptAddresses));

            if (mimeProcessor != null) {
                try {
                    mimeProcessor.process(mm, mbox);
                } finally {
                    mimeProcessor = null;
                }
            }

            // if requested, save a copy of the message to the Sent Mail folder
            ParsedMessage pm = null;
            ItemId returnItemId = null;
            if (mSaveToSent && !isDataSourceSender() && (!isDelegatedRequest ||
                    (isDelegatedRequest &&
                    (PrefDelegatedSendSaveTarget.sender == acct.getPrefDelegatedSendSaveTarget() ||
                    PrefDelegatedSendSaveTarget.both == acct.getPrefDelegatedSendSaveTarget())))) {
                if (mIdentity == null) {
                    mIdentity = Provisioning.getInstance().getDefaultIdentity(authuser);
                }

                // figure out where to save the save-to-sent copy
                if (authMailbox == null) {
                    authMailbox = getAuthenticatedMailbox(octxt, authuser, isAdminRequest);
                }

                if (authMailbox instanceof Mailbox) {
                    Mailbox mboxSave = (Mailbox) authMailbox;
                    int flags = Flag.BITMASK_FROM_ME;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mboxSave.attachmentsIndexingEnabled());

                    // save it to the requested folder
                    int sentFolderId = getSentFolderId(mboxSave, mIdentity);
                    if (DebugConfig.disableOutgoingFilter) {
                        DeliveryOptions dopt = new DeliveryOptions().setFolderId(sentFolderId).setNoICal(true).setFlags(flags).setConversationId(convId);
                        Message msg = mboxSave.addMessage(octxt, pm, dopt, null);
                        RollbackData rollback = new RollbackData(msg);
                        rollbacks.add(rollback);
                        returnItemId = rollback.msgId;
                    } else {
                        List<ItemId> addedItemIds =
                                RuleManager.applyRulesToOutgoingMessage(octxt, mboxSave, pm, sentFolderId, true, flags, null, convId);
                        // pick one (say first) item id to return
                        for (ItemId itemId : addedItemIds) {
                            rollbacks.add(new RollbackData(mboxSave, itemId.getId()));
                            if (returnItemId == null) {
                                returnItemId = itemId;
                            }
                        }
                    }
                } else if (authMailbox instanceof ZMailbox) {
                    ZMailbox zmbxSave = (ZMailbox) authMailbox;
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());

                    // save it to the requested folder
                    String sentFolder = mIdentity.getAttr(Provisioning.A_zimbraPrefSentMailFolder, "" + Mailbox.ID_FOLDER_SENT);
                    String msgId = zmbxSave.addMessage(sentFolder, "s", null, mm.getSentDate().getTime(), pm.getRawData(), true);
                    RollbackData rollback = new RollbackData(zmbxSave, authuser, msgId);
                    rollbacks.add(rollback);
                    returnItemId = rollback.msgId;
                }
            }

            // for delegated sends automatically save a copy to the "From" user's mailbox, unless we've been
            // specifically requested not to do the save (for instance BES does its own save to Sent, so does'nt
            // want it done here).
            if (allowSaveToSent && hasRecipients && !isDataSourceSender() && isDelegatedRequest &&
                    (PrefDelegatedSendSaveTarget.owner == acct.getPrefDelegatedSendSaveTarget() ||
                    PrefDelegatedSendSaveTarget.both == acct.getPrefDelegatedSendSaveTarget())) {
                int flags = Flag.BITMASK_UNREAD | Flag.BITMASK_FROM_ME;
                // save the sent copy using the target's credentials, as the sender doesn't necessarily have write access
                OperationContext octxtTarget = new OperationContext(acct);
                if (pm == null || pm.isAttachmentIndexingEnabled() != mbox.attachmentsIndexingEnabled()) {
                    pm = new ParsedMessage(mm, mm.getSentDate().getTime(), mbox.attachmentsIndexingEnabled());
                }
                int sentFolderId = getSentFolderId(mbox, Provisioning.getInstance().getDefaultIdentity(acct));
                if (DebugConfig.disableOutgoingFilter) {
                    DeliveryOptions dopt = new DeliveryOptions().setFolderId(sentFolderId).setNoICal(true).setFlags(flags).setConversationId(convId);
                    Message msg = mbox.addMessage(octxt, pm, dopt, null);
                    rollbacks.add(new RollbackData(msg));
                } else {
                    List<ItemId> addedItemIds =
                            RuleManager.applyRulesToOutgoingMessage(octxtTarget, mbox, pm, sentFolderId, true, flags, null, convId);
                    for (ItemId itemId : addedItemIds) {
                        rollbacks.add(new RollbackData(mbox, itemId.getId()));
                    }
                }
            }

            // If send partial behavior was specified, set it in the JavaMail session.
            if (mSendPartial != null && mSession != null) {
                mSession.getProperties().setProperty(JMSession.SMTP_SEND_PARTIAL_PROPERTY, mSendPartial.toString());
                mSession.getProperties().setProperty(JMSession.SMTPS_SEND_PARTIAL_PROPERTY, mSendPartial.toString());
            }

            // If DSN is specified, set it in the JavaMail session.
            if (mDsn != null && mSession != null)
                mSession.getProperties().setProperty("mail.smtp.dsn.notify", mDsn);

            //notify pre-send mail listeners
            String[] customheaders = mm.getHeader(PRE_SEND_HEADER);
            if(customheaders != null && customheaders.length > 0) {
                ZimbraLog.mailbox.debug("Processing pre-send mail listeners");
                for(PreSendMailListener listener : mPreSendMailListeners.values()) {
                    try {
                        listener.handle(mbox, getRecipients(mm), mm);
                    } catch (Exception e) {
                        ZimbraLog.mailbox.error("pre-send mail listener %s failed ", listener.getName(), e);
                    }
                }
                mm.removeHeader(PRE_SEND_HEADER); //no need to keep the header in the message at this point
            }

            // actually send the message via SMTP
            Collection<Address> sentAddresses = sendMessage(mbox, mm, rollbacks);

            // send intercept if save-to-sent didn't do it already
            if (!mSaveToSent) {
                try {
                    Notification.getInstance().interceptIfNecessary(mbox, mm, "send message", null);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.error("Unable to send lawful intercept message.", e);
                }
            }

            // check if this is a reply, and if so flag the msg appropriately
            if (mOriginalMessageId != null) {
                updateRepliedStatus(octxt, authuser, isAdminRequest, mbox);
            }

            // we can now purge the uploaded attachments
            if (mUploads != null) {
                FileUploadServlet.deleteUploads(mUploads);
            }

            // save new contacts and update rankings
            // skip this step if this is a delegate request (bug 44329)
            if (!isDelegatedRequest && !sentAddresses.isEmpty() && octxt != null) {
                assert(authMailbox == mbox);
                try {
                    ContactRankings.increment(octxt.getAuthenticatedUser().getId(), sentAddresses);
                } catch (Exception e) {
                    ZimbraLog.smtp.error("Failed to update contact rankings", e);
                }
                if (authuser.isPrefAutoAddAddressEnabled()) {
                    //intersect the lists;
                    newAddrs.retainAll(sentAddresses);

                    // convert JavaMail Address to Zimbra InternetAddress
                    List<com.zimbra.common.mime.InternetAddress> iaddrs =
                        new ArrayList<com.zimbra.common.mime.InternetAddress>(newAddrs.size());
                    for (Address addr : newAddrs) {
                        if (addr instanceof InternetAddress) {
                            InternetAddress iaddr = (InternetAddress) addr;
                            iaddrs.add(new com.zimbra.common.mime.InternetAddress(
                                    iaddr.getPersonal(), iaddr.getAddress()));
                        }
                    }
                    try {
                        mbox.createAutoContact(octxt, iaddrs);
                    } catch (IOException e) {
                        ZimbraLog.smtp.warn("Failed to auto-add contact addrs=%s", iaddrs, e);
                    }
                }
            }

            return returnItemId;

        } catch (SafeSendFailedException sfe) {
            Address[] invalidAddrs = sfe.getInvalidAddresses();
            Address[] validUnsentAddrs = sfe.getValidUnsentAddresses();
            if (invalidAddrs != null && invalidAddrs.length > 0) {
                StringBuilder msg = new StringBuilder("Invalid address").append(invalidAddrs.length > 1 ? "es: " : ": ");
                msg.append(Joiner.on(",").join(invalidAddrs)).append(".  ").append(sfe.toString());

                if (isSendPartial())
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
            if (chained instanceof SocketException || chained instanceof UnknownHostException) {
                throw MailServiceException.TRY_AGAIN("Unable to connect to the MTA", chained);
            } else {
                throw ServiceException.FAILURE("Unable to send message", me);
            }
        }
    }

    private boolean isDataSourceSender() {
        return mIsDataSourceSender;
    }

    private Object getAuthenticatedMailbox(OperationContext octxt, Account authuser, boolean isAdminRequest) {
        return getTargetMailbox(octxt, authuser, isAdminRequest, authuser);
    }

    private Object getTargetMailbox(OperationContext octxt, Account authuser, boolean isAdminRequest, Account targetUser) {
        if (targetUser == null)
            return null;

        try {
            if (Provisioning.onLocalServer(targetUser)) {
                return MailboxManager.getInstance().getMailboxByAccount(targetUser);
            } else {
                String uri = AccountUtil.getSoapUri(targetUser);
                if (uri == null)
                    return null;

                AuthToken authToken = null;
                if (octxt != null) {
                    authToken = AuthToken.getCsrfUnsecuredAuthToken(octxt.getAuthToken(false));
                }
                if (authToken == null) {
                    authToken = AuthProvider.getAuthToken(authuser, isAdminRequest);
                }

                ZMailbox.Options options = new ZMailbox.Options(authToken.toZAuthToken(), uri);
                options.setNoSession(true);
                if (!targetUser.getId().equalsIgnoreCase(authuser.getId())) {
                    options.setTargetAccount(targetUser.getId());
                    options.setTargetAccountBy(AccountBy.id);
                }
                ZMailbox zmbx = ZMailbox.getMailbox(options);
                if (zmbx != null) {
                    zmbx.setName(targetUser.getName());
                }
                return zmbx;
            }
        } catch (Exception e) {
            ZimbraLog.smtp.info("could not fetch home mailbox for delegated send", e);
            return null;
        }
    }

    /**
     * Print an smtp INFO log when the message is sent via SMTP. If the parameter is set to null,
     * that field will not be included in the log message.
     *
     * @param mm sending MimeMessage object
     * @param rcptAddresses array of envelope To addresses
     * @param envelopeFrom envelope From address
     * @param smtpHost sending SMTP host name
     * @param origMsgId original item ID, UUID or original message ID
     * @param uploads collection of uploads
     * @param replyType reply type
     * @param reason optional text to describe the reason why the message is sent
     */
    public static void logMessage(MimeMessage mm, Address[] rcptAddresses, String envelopeFrom, String smtpHost,
            String origMsgId, Collection<Upload> uploads, String replyType, String reason) {
        if (!ZimbraLog.smtp.isInfoEnabled()) {
            return;
        }
        StringBuilder msg = new StringBuilder("Sending message");
        appendMsgMTA(msg, smtpHost);
        appendMsgMessageID(msg, mm, origMsgId);
        appendReplyType(msg, replyType);
        appendUploads(msg, uploads);
        appendSize(msg, mm);
        appendEnvelopeFrom(msg, envelopeFrom);
        appendEnvelopeTo(msg, rcptAddresses);
        appendReason(msg, reason);
        ZimbraLog.smtp.info(msg);
    }

    private static void appendMsgMTA(StringBuilder msg, String smtpHost) {
        if (smtpHost != null) {
            msg.append(" to MTA at ").append(smtpHost);
        }
    }

    private static void appendMsgMessageID(StringBuilder msg, MimeMessage mm, String origMsgId) {
        try {
            msg.append(": Message-ID=" + mm.getMessageID());
        } catch (MessagingException e) {
            msg.append(e);
        }
        if (null != origMsgId) {
            msg.append(", origMsgId=" + origMsgId);
        }
    }

    private static void appendReplyType(StringBuilder msg, String replyType) {
        if (null != replyType) {
            msg.append(", replyType=" + replyType);
        }
    }

    private static void appendUploads(StringBuilder msg, Collection<Upload> uploads) {
        if (null != uploads && uploads.size() > 0) {
            msg.append(", uploads=" + uploads);
        }
    }

    private static void appendSize(StringBuilder msg, MimeMessage mm) {
        int size;
        try {
            size = mm.getSize();
        } catch (MessagingException e) {
            return;
        }
        if (size > 0) {
            msg.append(", size=" + size);
        }
    }

    private static void appendEnvelopeFrom(StringBuilder msg, String envelope) {
        if (null != envelope) {
            msg.append(", sender=" + envelope);
        }
    }

    private static void appendEnvelopeTo(StringBuilder msg, Address[] rcptAddresses) {
        msg.append(", nrcpts=" + rcptAddresses.length);
        for (int i = 0; i < rcptAddresses.length; i++) {
            String addr = null;
            if (rcptAddresses[i] instanceof InternetAddress) {
                addr = ((InternetAddress) rcptAddresses[i]).getAddress();
            }
            if (null != addr) {
                msg.append(", to=" + addr);
            }
        }
    }

    private static void appendReason(StringBuilder msg, String reason) {
        if (null != reason) {
            msg.append(", reason=").append(reason);
        }
    }

    public static final String PRE_SEND_HEADER = "X-Zimbra-Presend";
    public static final String X_ORIGINATING_IP = "X-Originating-IP";
    private static final String X_MAILER = "X-Mailer";
    public static final String X_AUTHENTICATED_USER = "X-Authenticated-User";

    public static String formatXOrigIpHeader(String origIp) {
        return "[" + origIp + "]";
    }

    void updateHeaders(MimeMessage mm, Account acct, Account authuser, OperationContext octxt, String originIP,
            boolean replyToSender) throws MessagingException, ServiceException {
        if (mSkipHeaderUpdate) {
            return;
        }
        Provisioning prov = Provisioning.getInstance();
        if (originIP != null) {
            boolean addOriginatingIP = prov.getConfig().isSmtpSendAddOriginatingIP();
            if (addOriginatingIP) {
                mm.addHeader(X_ORIGINATING_IP, formatXOrigIpHeader(originIP));
            }
        }

        boolean addMailer = prov.getConfig().isSmtpSendAddMailer();
        if (addMailer) {
            String ua = octxt != null ? octxt.getUserAgent() : null;
            String mailer = "Zimbra " + BuildInfo.VERSION + (ua == null ? "" : " (" + ua + ")");
            mm.addHeader(X_MAILER, mailer);
        }

        if (prov.getConfig().isSmtpSendAddAuthenticatedUser()) {
            mm.addHeader(X_AUTHENTICATED_USER, authuser.getName());
        }

        // set various headers on the outgoing message
        InternetAddress from = (InternetAddress) ArrayUtil.getFirstElement(mm.getFrom());
        InternetAddress sender = (InternetAddress) mm.getSender();
        if (mRedirectMode) {
            // Don't touch the message at all in redirect mode.
        } else {
            Pair<InternetAddress, InternetAddress> fromsender = getSenderHeaders(from, sender, acct, authuser,
                    octxt != null ? octxt.isUsingAdminPrivileges() : false);
            from = fromsender.getFirst();
            sender = fromsender.getSecond();
        }
        mm.setFrom(from);
        mm.setSender(sender);

        mm.setSentDate(new Date());
        if (sender == null) {
            Address[] existingReplyTos = mm.getReplyTo();
            if (existingReplyTos == null || existingReplyTos.length == 0) {
                String replyTo = acct.getPrefReplyToAddress();
                if (replyTo != null && !replyTo.trim().isEmpty()) {
                    mm.setHeader("Reply-To", replyTo);
                }
            }
        } else {
            if (replyToSender) {
                mm.setReplyTo(new Address[] {sender});
            }
        }

        updateReferenceHeaders(mm, octxt, authuser);

        mm.saveChanges();
    }

    /** Determines an acceptable set of {@code From} and {@code Sender} header
     *  values for a message.  The message's existing {@code from} and {@code
     *  sender} values are examined in light of the authenticated account's
     *  settings and are reset to the account's default return address if
     *  they aren't acceptable.
     * @return a {@link Pair} containing the approved {@code from} and {@code
     *         sender} header addresses, in that order. */
    public Pair<InternetAddress, InternetAddress> getSenderHeaders(InternetAddress from, InternetAddress sender,
            Account acct, Account authuser, boolean asAdmin) throws ServiceException {
        if (from != null && authuser.isAllowAnyFromAddress()) {
            return new Pair<InternetAddress, InternetAddress>(from, sender);
        }
        if (from == null && sender == null) {
            return new Pair<InternetAddress, InternetAddress>(AccountUtil.getFriendlyEmailAddress(authuser), null);
        }
        if (Objects.equal(sender, from)) { // no need for matching Sender and From addresses
            sender = null;
        }
        if (from == null && sender != null) {  // if only one value is given, set From and unset Sender
            from = sender;
            sender = null;
        }
        AccessManager amgr = AccessManager.getInstance();
        if (sender == null &&  // send-as requested
            (AccountUtil.addressMatchesAccount(authuser, from.getAddress()) ||  // either it's my address
             amgr.canSendAs(authuser, acct, from.getAddress(), asAdmin))) {           // or I've been granted permission
            return new Pair<InternetAddress, InternetAddress>(from, null);
        }
        if (sender != null) {
            // send-obo requested.
            // Restrict Sender value to owned addresses.  Not even zimbraAllowFromAddress is acceptable.
            AccountAddressMatcher matcher = new AccountAddressMatcher(authuser, true);
            if (!matcher.matches(sender.getAddress())) {
                sender = AccountUtil.getFriendlyEmailAddress(authuser);
            }
        } else if (!isDataSourceSender()) {
            // Downgrade disallowed send-as to send-obo.
            sender = AccountUtil.getFriendlyEmailAddress(authuser);
        }
        if (mCalendarMode) {
            // In calendar mode any user may send on behalf of any other user.
            return new Pair<InternetAddress, InternetAddress>(from, sender);
        } else if (amgr.canSendOnBehalfOf(authuser, acct, from.getAddress(), asAdmin)) {
            // Allow based on rights granted.
            return new Pair<InternetAddress, InternetAddress>(from, sender);
        } else if (AccountUtil.isAllowedDataSourceSendAddress(authuser, from.getAddress())) {
            // Allow send-obo if address is a pop/imap/caldav data source address. (bugs 38813/46378)
            return new Pair<InternetAddress, InternetAddress>(from, sender);
        } else {
            // Not allowed to use the requested From value.  Send as self.
            return new Pair<InternetAddress, InternetAddress>(sender, null);
        }
    }

    protected void updateReferenceHeaders(MimeMessage mm, OperationContext octxt, Account authuser) {
        boolean isReply = mOriginalMessageId != null && (MSGTYPE_REPLY.equals(mReplyType) || MSGTYPE_FORWARD.equals(mReplyType));

        try {
            String irt = mm.getHeader("In-Reply-To", null);
            String refs = mm.getHeader("References", null);
            String tindex = mm.getHeader("Thread-Index", null);
            String ttopic = mm.getHeader("Thread-Topic", null);

            if (!isReply) {
                // generate new Thread-Topic and Thread-Index headers
                if (Strings.isNullOrEmpty(tindex)) {
                    mm.setHeader("Thread-Index", ThreadIndex.newThreadIndex());
                }
                if (Strings.isNullOrEmpty(ttopic)) {
                    mm.setHeader("Thread-Topic", ThreadIndex.newThreadTopic(mm.getSubject()));
                }
                return;
            }

            if (!Strings.isNullOrEmpty(irt) && !Strings.isNullOrEmpty(refs) && !Strings.isNullOrEmpty(tindex) && !Strings.isNullOrEmpty(ttopic))
                return;

            // fetch the parent message's headers (no such item just short circuits the header update)
            JavaMailInternetHeaders hblock;
            if (mOriginalMessageId.isLocal()) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mOriginalMessageId.getAccountId());
                Message msg = mbox.getMessageById(octxt, mOriginalMessageId.getId());
                InputStream is = msg.getContentStream();
                try {
                    hblock = new JavaMailInternetHeaders(is);
                } finally {
                    ByteUtil.closeStream(is);
                }
            } else {
                AuthToken authToken = octxt == null ? null : octxt.getAuthToken(false);
                if (authToken == null) {
                    boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
                    authToken = AuthProvider.getAuthToken(authuser, isAdminRequest);
                }
                // using the sync formatter is suboptimal, but it should work
                Map<String, String> params = new HashMap<String, String>();
                params.put("fmt", "sync");  params.put("body", "0");  params.put("nohdr", "1");
                byte[] content = UserServlet.getRemoteContent(authToken, mOriginalMessageId, params);
                hblock = new JavaMailInternetHeaders(new ByteArrayInputStream(content));
            }

            // set headers appropriately, but don't override headers explicitly set by the sender...
            String parentMsgid = ListUtil.getFirstElement(Mime.getReferences(hblock, "Message-ID"));
            if (Strings.isNullOrEmpty(irt) && !Strings.isNullOrEmpty(parentMsgid)) {
                mm.setHeader("In-Reply-To", "<" + parentMsgid + ">");
            }
            if (Strings.isNullOrEmpty(refs) && !Strings.isNullOrEmpty(parentMsgid)) {
                List<String> parentRefs = Mime.getReferences(hblock, "References");
                // keep the references list from growing too big, but also keep the root intact
                while (parentRefs.size() > 7) {
                    parentRefs.remove(1);
                }
                parentRefs.add(parentMsgid);
                mm.setHeader("References", "<" + Joiner.on("> <").join(parentRefs) + ">");
            }
            if (Strings.isNullOrEmpty(ttopic)) {
                mm.setHeader("Thread-Topic", ThreadIndex.newThreadTopic(mm.getSubject()));
            }
            if (Strings.isNullOrEmpty(tindex)) {
                byte[] parentIndex = ThreadIndex.parseHeader(hblock.getHeader("Thread-Index", null));
                mm.setHeader("Thread-Index", parentIndex == null ? ThreadIndex.newThreadIndex() : ThreadIndex.addChild(parentIndex));
            }
        } catch (Exception e) {
            // if the message goes out with minimal threading headers, so be it
        }
    }

    protected void updateRepliedStatus(OperationContext octxt, Account authuser, boolean isAdminRequest, Mailbox mboxPossible) {
        try {
            Object target = null;
            if (mboxPossible != null && mOriginalMessageId.belongsTo(mboxPossible)) {
                target = mboxPossible;
            } else {
                target = getTargetMailbox(octxt, authuser, isAdminRequest, Provisioning.getInstance().get(AccountBy.id, mOriginalMessageId.getAccountId()));
            }

            if (target instanceof Mailbox) {
                Mailbox mbox = (Mailbox) target;
                if (MSGTYPE_REPLY.equals(mReplyType)) {
                    mbox.alterTag(octxt, mOriginalMessageId.getId(), MailItem.Type.MESSAGE, Flag.FlagInfo.REPLIED, true, null);
                } else if (MSGTYPE_FORWARD.equals(mReplyType)) {
                    mbox.alterTag(octxt, mOriginalMessageId.getId(), MailItem.Type.MESSAGE, Flag.FlagInfo.FORWARDED, true, null);
                }
            } else if (target instanceof ZMailbox) {
                ZMailbox zmbx = (ZMailbox) target;
                if (MSGTYPE_REPLY.equals(mReplyType)) {
                    zmbx.tagMessage(mOriginalMessageId.toString(), "" + Flag.ID_REPLIED, true);
                } else if (MSGTYPE_FORWARD.equals(mReplyType)) {
                    zmbx.tagMessage(mOriginalMessageId.toString(), "" + Flag.ID_FORWARDED, true);
                }
            }
        } catch (ServiceException e) {
            // this is not an error case:
            //   - the original message may be gone when accepting/declining an appointment
            //   - we may have PERM_DENIED on delegated send
        }
    }

    /** @return a Collection of successfully sent recipient Addresses */
    protected Collection<Address> sendMessage(Mailbox mbox, final MimeMessage mm, Collection<RollbackData> rollbacks)
    throws SafeMessagingException, IOException {
        // send the message via SMTP
        HashSet<Address> sentAddresses = new HashSet<Address>();
        mCurrentHostIndex = 0;
        String hostname = getNextHost();

        try {
            // Initialize recipient addresses
            Address[] rcptAddresses = getRecipients(mm);

            if (rcptAddresses == null || rcptAddresses.length == 0)
                throw new SendFailedException("No recipient addresses");

            while (true) {
                try {
                    logMessage(mm, rcptAddresses, mEnvelopeFrom, hostname,
                        null != mOriginalMessageId ? mOriginalMessageId.toString() : null, mUploads, mReplyType, null);
                    if (hostname != null) {
                        sendMessageToHost(hostname, mm, rcptAddresses);
                    } else {
                        Transport.send(mm, rcptAddresses);
                    }
                    Collections.addAll(sentAddresses, rcptAddresses);
                    break;
                } catch (SendFailedException sfe) {
                    throw sfe;
                } catch (MessagingException e) {
                    Exception chained = e.getNextException();
                    if (chained instanceof SocketException || chained instanceof UnknownHostException) {
                        String hostString = (hostname != null ? " " + hostname : "");
                        ZimbraLog.smtp.warn("Unable to connect to SMTP server%s: %s.", hostString, chained.toString());

                        if (mTrackBadHosts) {
                            JMSession.markSmtpHostBad(hostname);
                        }
                        hostname = getNextHost();
                        if (hostname == null) {
                            throw e;
                        }
                        ZimbraLog.smtp.info("Attempting to send to %s.", hostname);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (SendFailedException e) {
            //skip roll backs for partial send failure cases!
            if (isSendPartial())
                throw new SafeSendFailedException(e);

            for (RollbackData rdata : rollbacks) {
                if (rdata != null) {
                    rdata.rollback();
                }
            }

            throw new SafeSendFailedException(e);
        } catch (MessagingException e) {
            for (RollbackData rdata : rollbacks) {
                if (rdata != null) {
                    rdata.rollback();
                }
            }
            throw new SafeMessagingException(e);
        } catch (RuntimeException e) {
            for (RollbackData rdata : rollbacks) {
                if (rdata != null) {
                    rdata.rollback();
                }
            }
            throw e;
        }
        return sentAddresses;
    }

    private String getNextHost() {
        if (mSession != null && mCurrentHostIndex < mSmtpHosts.size()) {
            return mSmtpHosts.get(mCurrentHostIndex++);
        }
        return null;
    }

    private void sendMessageToHost(String hostname, MimeMessage mm, Address[] rcptAddresses)
    throws MessagingException {
        mSession.getProperties().setProperty("mail.smtp.host", hostname);
        if (mEnvelopeFrom != null) {
            mSession.getProperties().setProperty("mail.smtp.from", mEnvelopeFrom);
        }
        ZimbraLog.smtp.debug("Sending message %s to SMTP host %s with properties: %s",
                             mm.getMessageID(), hostname, mSession.getProperties());
        Transport transport = mSession.getTransport("smtp");
        try {
            transport.connect();
            transport.sendMessage(mm, rcptAddresses);
        } finally {
            transport.close();
        }
    }

    private void checkMTAConnectionToHost(String hostname) throws MessagingException {
        mSession.getProperties().setProperty("mail.smtp.host", hostname);
        if (mEnvelopeFrom != null) {
            mSession.getProperties().setProperty("mail.smtp.from", mEnvelopeFrom);
        }
        ZimbraLog.smtp.debug("Testing connection to SMTP host %s with properties: %s",
                             hostname, mSession.getProperties());
        Transport transport = mSession.getTransport("smtp");
        try {
            transport.connect();
        } finally {
            transport.close();
        }
    }

    /**
     * Check connection to the MTA.  An exception is thrown if connection to the MTA cannot be established.
     * Successful return from this method does not guarantee a subsequent send will succeed, but chances are
     * good that the send will work.  Use this method to detect prolonged unavailability of the MTA.
     * @throws ServiceException
     */
    public void checkMTAConnection() throws ServiceException {
        MessagingException connectError = null;
        mCurrentHostIndex = 0;
        String hostname;
        while ((hostname = getNextHost()) != null) {
            try {
                checkMTAConnectionToHost(hostname);
                return;  // Good, we have an MTA that we can connect to.
            } catch (MessagingException e) {
                Exception chained = e.getNextException();
                if (chained instanceof SocketException || chained instanceof UnknownHostException) {
                    if (connectError == null) {
                        connectError = e;
                    }
                    String hostString = (hostname != null ? " " + hostname : "");
                    ZimbraLog.smtp.warn("Unable to connect to SMTP server%s: %s.", hostString, chained.toString());
                    if (mTrackBadHosts) {
                        JMSession.markSmtpHostBad(hostname);
                    }
                } else {
                    throw ServiceException.FAILURE("unexpected error during MTA connection check", e);
                }
            }
        }
        throw ServiceException.FAILURE("unable to connect to MTA", connectError);
    }

    /**
     * Send the MimeMessage via external relay MTA configured on the server.
     * @param mm
     * @throws ServiceException
     */
    public static void relayMessage(MimeMessage mm) throws MessagingException, ServiceException {
        Session session = JMSession.getRelaySession();
        ZimbraLog.smtp.debug("Sending message %s with properties: %s",
                mm.getMessageID(), session.getProperties());
        Transport transport = session.getTransport("smtp");
        try {
            transport.connect();
            transport.sendMessage(mm, mm.getAllRecipients());
        } finally {
            transport.close();
        }
    }

    /**
     * Class that avoids JavaMail bug that throws OutOfMemoryError when sending
     * a message with many recipients and SMTP server rejects many of them.
     * The bug is in MessagingException.toString().
     */
    public static class SafeMessagingException extends MessagingException {
        private static final long serialVersionUID = -4652297855877992478L;
        private final MessagingException mMex;

        public SafeMessagingException(MessagingException mex) {
            mMex = mex;
            setStackTrace(mMex.getStackTrace());
        }

        @Override
        public String getMessage() {
            return mMex.getMessage();
        }

        @Override
        public synchronized Exception getNextException() {
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
                    if (n != null) {
                        more++;
                    }
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
            if (message != null) {
                sb.append(": ").append(message);
            }
            return sb;
        }
    }

    public static class SafeSendFailedException extends SafeMessagingException {
        private static final long serialVersionUID = 5625565177360027934L;
        private final SendFailedException mSfe;

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

    public interface PreSendMailListener {
        void handle(Mailbox mbox, Address[] recipients, MimeMessage mm);
        String getName();
    }

    /**
     * adds a listener to listen on emails being sent
     * @param listener
     */
    public static void registerPreSendMailListener(PreSendMailListener listener) {
        String name = listener.getName();
        if (!mPreSendMailListeners.containsKey(name)) {
            mPreSendMailListeners.put(name, listener);
            ZimbraLog.extensions.info("registered SendMailListener " + name);
        }
    }

    /**
     * removes a listener that listens for emails being sent
     * @param listener
     */
    public static void unregisterPreSendMailListener(PreSendMailListener listener) {
        for (Iterator<String> it = mPreSendMailListeners.keySet().iterator(); it.hasNext(); ) {
            String name = it.next();
            if (name.equalsIgnoreCase(listener.getName())) {
                it.remove();
                ZimbraLog.extensions.info("unregistered SendMailListener " + name);
            }
        }
    }
}
