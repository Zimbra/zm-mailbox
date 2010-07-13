/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.zimbra.common.util.MapUtil;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.Rfc822ValidationInputStream;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.CopyInputStream;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.mailbox.Notification;
import com.zimbra.cs.mailbox.QuotaWarning;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.Zimbra;

public class ZimbraLmtpBackend implements LmtpBackend {

    private static List<LmtpCallback> sCallbacks = new CopyOnWriteArrayList<LmtpCallback>(); 
    private static Map sReceivedMessageIDs = null;

    private LmtpConfig mConfig;

    public ZimbraLmtpBackend(LmtpConfig lmtpConfig) {
        mConfig = lmtpConfig;
    }

    /**
     * Adds an instance of an LMTP callback class that will be triggered
     * before and after a message is added to a user mailbox. 
     */
    public static void addCallback(LmtpCallback callback) {
        if (callback == null) {
            ZimbraLog.lmtp.error("", new IllegalStateException("LmtpCallback cannot be null"));
            return;
        }
        ZimbraLog.lmtp.info("Adding LMTP callback: %s", callback.getClass().getName());
        sCallbacks.add(callback);
    }

    static {
        try {
            int cacheSize = Provisioning.getInstance().getConfig().getIntAttr(Provisioning.A_zimbraMessageIdDedupeCacheSize, 0);
            if (cacheSize > 0)
                sReceivedMessageIDs = MapUtil.newLruMap(cacheSize);
        } catch (ServiceException e) {
            ZimbraLog.lmtp.error("could not read zimbraMessageIdDedupeCacheSize; no deduping will be performed", e);
        }
        addCallback(Notification.getInstance());
        addCallback(QuotaWarning.getInstance());
    }

    public LmtpReply getAddressStatus(LmtpAddress address) {
        String addr = address.getEmailAddress();

        try {
            Provisioning prov = Provisioning.getInstance();
            Account acct = prov.get(AccountBy.name, addr);
            if (acct == null) {
                ZimbraLog.lmtp.info("rejecting address " + addr + ": no account");
                return LmtpReply.NO_SUCH_USER;
            }

            String acctStatus = acct.getAccountStatus(prov);
            if (acctStatus == null) {
                ZimbraLog.lmtp.warn("rejecting address " + addr + ": no account status");
                return LmtpReply.NO_SUCH_USER;
            }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE)) {
                ZimbraLog.lmtp.info("try again for address " + addr + ": account status maintenance");
                return LmtpReply.MAILBOX_DISABLED;
            }
            
            if (!Provisioning.onLocalServer(acct)) {
                ZimbraLog.lmtp.warn("try again for address " + addr + ": mailbox is not on this server");
                return LmtpReply.MAILBOX_NOT_ON_THIS_SERVER;
            }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_PENDING)) {
                ZimbraLog.lmtp.info("rejecting address " + addr + ": account status pending");
                return LmtpReply.NO_SUCH_USER;
            }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
                ZimbraLog.lmtp.info("rejecting address " + addr + ": account status closed");
                return LmtpReply.NO_SUCH_USER;
            }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
                acctStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT) ||
                acctStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKED)) 
            {
                return LmtpReply.RECIPIENT_OK;
            }

            ZimbraLog.lmtp.info("rejecting address " + addr + ": unknown account status " + acctStatus);
            return LmtpReply.NO_SUCH_USER;

        } catch (ServiceException e) {
            if (e.isReceiversFault()) {
                ZimbraLog.lmtp.warn("try again for address " + addr + ": exception occurred", e);
                return LmtpReply.MAILBOX_DISABLED;
            } else {
                ZimbraLog.lmtp.warn("rejecting address " + addr + ": exception occurred", e);
                return LmtpReply.NO_SUCH_USER;
            }
        }
    }

    public void deliver(LmtpEnvelope env, Blob blob) {
        try {
            deliverMessageToLocalMailboxes(blob, null, env);
        } catch (ServiceException e) {
            ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpReply.TEMPORARY_FAILURE);
        } catch (IOException e) {
            ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpReply.TEMPORARY_FAILURE);
        }
    }
    
    public void deliver(LmtpEnvelope env, InputStream in, int sizeHint) {
        try {
            deliverMessageToLocalMailboxes(in, env, sizeHint);
        } catch (ServiceException e) {
            ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpReply.TEMPORARY_FAILURE);
        } catch (IOException e) {
            ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpReply.TEMPORARY_FAILURE);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean dedupe(ParsedMessage pm, Mailbox mbox) {
        if (sReceivedMessageIDs == null || pm == null || mbox == null)
            return false;
        String msgid = pm.getMessageID();
        if (msgid == null || msgid.equals(""))
            return false;

        synchronized (sReceivedMessageIDs) {
            Set<Long> mboxIds = (Set<Long>) sReceivedMessageIDs.get(msgid);
            if (mboxIds == null) {
                mboxIds = new HashSet<Long>();
                sReceivedMessageIDs.put(msgid, mboxIds);
            } else {
                if (mboxIds.contains(mbox.getId())) {
                    return true;
                }
            }
            mboxIds.add(mbox.getId());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void removeFromDedupeCache(String msgid, Mailbox mbox) {
        if (sReceivedMessageIDs == null || msgid == null || mbox == null)
            return;
        if (msgid == null || msgid.equals(""))
            return;

        synchronized (sReceivedMessageIDs) {
            Set<Long> mboxIds = (Set<Long>) sReceivedMessageIDs.get(msgid);
            if (mboxIds != null) {
                mboxIds.remove(mbox.getId());
            }
        }
    }

    private enum DeliveryAction {
        discard, // local delivery is disabled
        defer,   // can not deliver to mailbox right now - backup maybe in progress
        deliver  // OK to deliver
    }

    private static class RecipientDetail {
        public Account account;
        public Mailbox mbox;
        public ParsedMessage pm;
        public boolean esd; // whether endSharedDelivery should be called
        public DeliveryAction action;

        public RecipientDetail(Account a, Mailbox m, ParsedMessage p, boolean endSharedDelivery, DeliveryAction da) {
            account = a;
            mbox = m;
            pm = p;
            esd = endSharedDelivery;
            action = da;
        }
    }

    private void logEnvelope(LmtpEnvelope env, String msgId) {

        // Log envelope information
        if (ZimbraLog.lmtp.isInfoEnabled()) {
            String size = (env.getSize() == 0 ?
                "unspecified" : Integer.toString(env.getSize()) + " bytes");

            ZimbraLog.lmtp.info("Delivering message: size=%s, nrcpts=%d, sender=%s, msgid=%s",
                size, 
                env.getRecipients().size(), 
                env.getSender(),
                msgId==null?"":msgId);
        }
    }

    private void deliverMessageToLocalMailboxes(InputStream in,
                                                LmtpEnvelope env, 
                                                int sizeHint)
        throws ServiceException, IOException {
        int bufLen = Provisioning.getInstance().getLocalServer().getMailDiskStreamingThreshold();
        CopyInputStream cis = new CopyInputStream(in, sizeHint, bufLen, bufLen);
        in = cis;
        
        Rfc822ValidationInputStream validator = null;
        if (LC.zimbra_lmtp_validate_messages.booleanValue()) {
            validator = new Rfc822ValidationInputStream(cis, LC.zimbra_lmtp_max_line_length.longValue());
            in = validator;
        }
        
        Blob blob = StoreManager.getInstance().storeIncoming(in, sizeHint, null);
        
        if (validator != null && !validator.isValid()) {
            StoreManager.getInstance().delete(blob);
            setDeliveryStatuses(env.getRecipients(), LmtpReply.INVALID_BODY_PARAMETER);
            return;
        }
        
        BufferStream bs = cis.getBufferStream();

        try {
            deliverMessageToLocalMailboxes(blob, bs.isPartial() ? null :
                bs.getBuffer(), env);
        } finally {
            cis.release();
            StoreManager.getInstance().delete(blob);
        }
    }

    private void deliverMessageToLocalMailboxes(Blob blob, byte[] data, LmtpEnvelope env)
        throws ServiceException, IOException {

        List<LmtpAddress> recipients = env.getRecipients();
        String envSender = env.getSender().getEmailAddress();

        boolean shared = recipients.size() > 1;
        List<Long> targetMailboxIds = new ArrayList<Long>(recipients.size());

        Map<LmtpAddress, RecipientDetail> rcptMap = new HashMap<LmtpAddress, RecipientDetail>(recipients.size());
        try {
            // Examine attachments indexing option for all recipients and
            // prepare ParsedMessage versions needed.  Parsing is done before
            // attempting delivery to any recipient.  Therefore, parse error
            // will result in non-delivery to all recipients.

            // ParsedMessage for users with attachments indexing
            ParsedMessage pmAttachIndex = null;
            // ParsedMessage for users without attachments indexing
            ParsedMessage pmNoAttachIndex = null;

            // message id for logging
            String msgId = null;

            for (LmtpAddress recipient : recipients) {
                String rcptEmail = recipient.getEmailAddress();

                Account account = null;
                Mailbox mbox = null;
                boolean attachmentsIndexingEnabled = true;
                try {
                    account = Provisioning.getInstance().get(AccountBy.name, rcptEmail);
                    if (account == null) {
                        ZimbraLog.mailbox.warn("No account found delivering mail to " + rcptEmail);
                        continue;
                    }
                    if (account.getBooleanAttr(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled, false)) {
                        ZimbraLog.lmtp.debug("Local delivery disabled for account %s", rcptEmail);
                        rcptMap.put(recipient, new RecipientDetail(null, null, null, false, DeliveryAction.discard));
                        continue;
                    }
                    mbox = MailboxManager.getInstance().getMailboxByAccount(account);
                    if (mbox == null) {
                        ZimbraLog.mailbox.warn("No mailbox found delivering mail to " + rcptEmail);
                        continue;
                    }
                    attachmentsIndexingEnabled = mbox.attachmentsIndexingEnabled();
                } catch (ServiceException se) {
                    if (se.isReceiversFault()) {
                        ZimbraLog.mailbox.info("Recoverable exception getting mailbox for " + rcptEmail, se);
                        rcptMap.put(recipient, new RecipientDetail(null, null, null, false, DeliveryAction.defer));
                    } else {
                        ZimbraLog.mailbox.warn("Unrecoverable exception getting mailbox for " + rcptEmail, se);
                    }
                    continue;
                }

                if (account != null && mbox != null) {
                    ParsedMessage pm;
                    ParsedMessageOptions pmo = new ParsedMessageOptions(blob, data);

                    if (attachmentsIndexingEnabled) {
                        if (pmAttachIndex == null) {
                            pmo.setAttachmentIndexing(true);
                            ZimbraLog.lmtp.debug("Creating ParsedMessage from " +
                                (data == null ? "file" : "memory") + " with attachment indexing enabled");
                            pmAttachIndex = new ParsedMessage(pmo);
                        }
                        pm = pmAttachIndex;
                    } else {
                        if (pmNoAttachIndex == null) {
                            pmo.setAttachmentIndexing(false);
                            ZimbraLog.lmtp.debug("Creating ParsedMessage from " +
                                (data == null ? "file" : "memory") + " with attachment indexing disabled");
                            pmNoAttachIndex = new ParsedMessage(pmo);
                        }
                        pm = pmNoAttachIndex;
                    }

                    msgId = pm.getMessageID();

                    // For non-shared delivery (i.e. only one recipient),
                    // always deliver regardless of backup mode.
                    DeliveryAction da = DeliveryAction.deliver;
                    boolean endSharedDelivery = false;
                    if (shared) {
                        if (mbox.beginSharedDelivery()) {
                            endSharedDelivery = true;
                        } else {
                            // Skip delivery to mailboxes in backup mode.
                            da = DeliveryAction.defer;
                        }
                    }
                    rcptMap.put(recipient, new RecipientDetail(account, mbox, pm, endSharedDelivery, da));
                    if (da == DeliveryAction.deliver)
                        targetMailboxIds.add(mbox.getId());
                }
            }

            ZimbraLog.removeAccountFromContext();
            logEnvelope(env, msgId);

            DeliveryContext sharedDeliveryCtxt =
                new DeliveryContext(shared, targetMailboxIds);
            sharedDeliveryCtxt.setIncomingBlob(blob);

            // We now know which addresses are valid and which ParsedMessage
            // version each recipient needs.  Deliver!
            for (LmtpAddress recipient : recipients) {
                String rcptEmail = recipient.getEmailAddress();
                LmtpReply reply = LmtpReply.TEMPORARY_FAILURE;
                RecipientDetail rd = rcptMap.get(recipient);
                if (rd.account != null)
                    ZimbraLog.addAccountNameToContext(rd.account.getName());
                if (rd.mbox != null)
                    ZimbraLog.addMboxToContext(rd.mbox.getId());

                boolean success = false;
                try {
                    if (rd != null) {
                        switch (rd.action) {
                        case discard:
                            ZimbraLog.lmtp.info("accepted and discarded message for " + rcptEmail + ": local delivery is disabled");
                            reply = LmtpReply.DELIVERY_OK;
                            break;
                        case deliver:
                            Account account = rd.account;
                            Mailbox mbox = rd.mbox;
                            ParsedMessage pm = rd.pm;
                            List<ItemId> addedMessageIds = null; 
                            if (dedupe(pm, mbox)) {
                                // message was already delivered to this mailbox
                                ZimbraLog.lmtp.info("Not delivering message with duplicate Message-ID %s", pm.getMessageID());
                            } else if (recipient.getSkipFilters()) {
                                msgId = pm.getMessageID();
                                int folderId = Mailbox.ID_FOLDER_INBOX;
                                if (recipient.getFolder() != null) {
                                    try {
                                        Folder folder = mbox.getFolderByPath(null, recipient.getFolder());
                                        folderId = folder.getId();
                                    } catch (ServiceException se) {
                                        if (se.getCode().equals(MailServiceException.NO_SUCH_FOLDER)) {
                                            Folder folder = mbox.createFolder(null, recipient.getFolder(), (byte) 0, MailItem.TYPE_MESSAGE);
                                            folderId = folder.getId();
                                        } else {
                                            throw se;
                                        }
                                    }
                                }
                                int flags = Flag.BITMASK_UNREAD;
                                if (recipient.getFlags() != null) {
                                    flags = Flag.flagsToBitmask(recipient.getFlags());
                                }
                                Message msg = mbox.addMessage(null, pm, folderId, false, flags, recipient.getTags(), rcptEmail, sharedDeliveryCtxt);
                                addedMessageIds = new ArrayList<ItemId>(1);
                                addedMessageIds.add(new ItemId(msg));
                            } else if (!DebugConfig.disableFilter) {
                                // Get msgid first, to avoid having to reopen and reparse the blob
                                // file if Mailbox.addMessageInternal() closes it.
                                pm.getMessageID();
                                addedMessageIds = RuleManager.applyRulesToIncomingMessage(mbox, pm,
                                    rcptEmail, sharedDeliveryCtxt, Mailbox.ID_FOLDER_INBOX);
                            } else {
                                pm.getMessageID();
                                Message msg = mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, false, Flag.BITMASK_UNREAD, null,
                                    rcptEmail, sharedDeliveryCtxt);
                                addedMessageIds = new ArrayList<ItemId>(1);
                                addedMessageIds.add(new ItemId(msg));
                            }
                            success = true;
                            
                            if (addedMessageIds != null && addedMessageIds.size() > 0) {
                                // Execute callbacks
                                for (LmtpCallback callback : sCallbacks) {
                                    for (ItemId id : addedMessageIds) {
                                        if (id.belongsTo(mbox)) {
                                            // Message was added to the local mailbox, as opposed to a mountpoint.
                                            ZimbraLog.lmtp.debug("Executing callback %s", callback.getClass().getName());
                                            try {
                                                Message msg = mbox.getMessageById(null, id.getId());
                                                callback.afterDelivery(account, mbox, envSender, rcptEmail, msg);
                                            } catch (Throwable t) {
                                                if (t instanceof OutOfMemoryError) {
                                                    Zimbra.halt("LMTP callback failed", t);
                                                } else {
                                                    ZimbraLog.lmtp.warn("LMTP callback threw an exception", t);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            reply = LmtpReply.DELIVERY_OK;
                            break;
                        case defer:
                            // Delivery to mailbox skipped.  Let MTA retry again later.
                            // This case happens for shared delivery to a mailbox in
                            // backup mode.
                            ZimbraLog.lmtp.info("try again for message " + rcptEmail + ": mailbox skipped");
                            reply = LmtpReply.TEMPORARY_FAILURE;
                            break;
                        }
                    } else {
                        // Account or mailbox not found.
                        ZimbraLog.lmtp.info("rejecting message " + rcptEmail + ": account or mailbox not found");
                        reply = LmtpReply.PERMANENT_FAILURE;
                    }
                } catch (IOException ioe) {
                    reply = LmtpReply.TEMPORARY_FAILURE;
                    ZimbraLog.lmtp.warn("try again for " + rcptEmail + ": exception occurred", ioe);
                } catch (ServiceException se) {
                    if (se.getCode().equals(MailServiceException.QUOTA_EXCEEDED)) {
                        ZimbraLog.lmtp.info("rejecting message " + rcptEmail + ": overquota");
                        if (mConfig.isPermanentFailureWhenOverQuota()) {
                            reply = LmtpReply.PERMANENT_FAILURE_OVER_QUOTA;
                        } else {
                            reply = LmtpReply.TEMPORARY_FAILURE_OVER_QUOTA;
                        }
                    } else if (se.isReceiversFault()) {
                        ZimbraLog.lmtp.info("try again for message " + rcptEmail + ": exception occurred", se);
                        reply = LmtpReply.TEMPORARY_FAILURE;
                    } else {
                        ZimbraLog.lmtp.info("rejecting message " + rcptEmail + ": exception occurred", se);
                        reply = LmtpReply.PERMANENT_FAILURE;
                    }
                } catch (Exception e) {
                    reply = LmtpReply.TEMPORARY_FAILURE;
                    ZimbraLog.lmtp.warn("try again for message " + rcptEmail + ": exception occurred", e);
                } finally {
                    if (rd.action == DeliveryAction.deliver && !success) {
                        // Message was not delivered.  Remove it from the dedupe
                        // cache so we don't dedupe it on LMTP retry.
                        removeFromDedupeCache(msgId, rd.mbox);
                    }
                    recipient.setDeliveryStatus(reply);
                    if (shared && rd != null && rd.esd) {
                        rd.mbox.endSharedDelivery();
                        rd.esd = false;
                    }
                }
            }

            // If this message is being streamed from disk, cache it
            ParsedMessage mimeSource = pmAttachIndex;
            if (mimeSource == null)
                mimeSource = pmNoAttachIndex;
            MailboxBlob mblob = sharedDeliveryCtxt.getMailboxBlob();
            if (mblob != null && mimeSource != null && mimeSource.isStreamedFromDisk()) {
                try {
                    // Update the MimeMessage with the blob that's stored inside the mailbox,
                    // since the incoming blob will be deleted.
                    BlobInputStream bis = mimeSource.getBlobInputStream();
                    Blob storedBlob = mblob.getLocalBlob();
                    bis.fileMoved(storedBlob.getFile());
                    MessageCache.cacheMessage(mblob.getDigest(), mimeSource.getOriginalMessage(), mimeSource.getMimeMessage());
                } catch (IOException e) {
                    ZimbraLog.lmtp.warn("Unable to cache message for " + mblob, e);
                }
            }
        } finally {
            // If there were any stray exceptions after the call to
            // beginSharedDelivery that caused endSharedDelivery to be not
            // called, we check and fix those here.
            if (shared) {
                for (RecipientDetail rd : rcptMap.values()) {
                    if (rd.esd && rd.mbox != null)
                        rd.mbox.endSharedDelivery();
                }
            }
            if (blob != null) {
                // Clean up the incoming blob.
                StoreManager.getInstance().delete(blob);
            }
        }
    }

    private void setDeliveryStatuses(List<LmtpAddress> recipients, LmtpReply reply) {
        for (LmtpAddress recipient : recipients)
            recipient.setDeliveryStatus(reply);
    }
}
