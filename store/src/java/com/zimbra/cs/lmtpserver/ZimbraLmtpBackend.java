/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.lmtpserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.lmtp.LmtpClient;
import com.zimbra.common.lmtp.LmtpProtocolException;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.Rfc822ValidationInputStream;
import com.zimbra.common.service.DeliveryServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CopyInputStream;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
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

    private static List<LmtpCallback> callbacks = new CopyOnWriteArrayList<LmtpCallback>();
    private static Map<String, Set<Integer>> receivedMessageIDs;
    private static final LoadingCache<Integer, ReentrantLock> mailboxDeliveryLocks = createMailboxDeliveryLocks();

    private final LmtpConfig config;

    public ZimbraLmtpBackend(LmtpConfig lmtpConfig) {
        config = lmtpConfig;
        checkDedupeCacheSize(); // This initializes receivedMessageIDs
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
        callbacks.add(callback);
    }

    static {
        addCallback(Notification.getInstance());
        addCallback(QuotaWarning.getInstance());
    }

    private static LoadingCache<Integer, ReentrantLock> createMailboxDeliveryLocks() {
        Function<Integer, ReentrantLock> lockCreator = new Function<Integer,  ReentrantLock>() {
            @Override
            public ReentrantLock apply(Integer from) {
                return new ReentrantLock();
            }
        };
        LoadingCache<Integer, ReentrantLock> cache = CacheBuilder.newBuilder()
            .build(CacheLoader.from(lockCreator));
        return cache;
    }

    @Override public LmtpReply getAddressStatus(LmtpAddress address) {
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

            if (Provisioning.onLocalServer(acct)) {
                address.setOnLocalServer(true);
            } else  {
                String affinityIp = Provisioning.affinityServer(acct);
                if (affinityIp == null) {
                    ZimbraLog.lmtp.warn("try again for address " + addr + ": mailbox is not on this server");
                    return LmtpReply.MAILBOX_NOT_ON_THIS_SERVER;
                }
                address.setOnLocalServer(false);
                address.setRemoteServer(affinityIp);
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

    private boolean dedupe(ParsedMessage pm, Mailbox mbox)
    throws ServiceException {
        if (pm == null || mbox == null || !mbox.getAccount().isPrefMessageIdDedupingEnabled())
            return false;

        checkDedupeCacheSize();
        String msgid = getMessageID(pm);
        if (msgid == null || msgid.equals(""))
            return false;

        synchronized (ZimbraLmtpBackend.class) {
            Set<Integer> mboxIds = receivedMessageIDs.get(msgid);
            if (mboxIds != null && mboxIds.contains(mbox.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value of the {@code Message-ID} header, or the most
     * recent {@code Resent-Message-ID} header, if set.
     */
    private String getMessageID(ParsedMessage pm) {
        try {
            String id = pm.getMimeMessage().getHeader("Resent-Message-ID", null);
            if (!Strings.isNullOrEmpty(id)) {
                ZimbraLog.lmtp.debug("Resent-Message-ID=%s", id);
                return id;
            }
        } catch (MessagingException e) {
            ZimbraLog.lmtp.warn("Unable to determine Resent-Message-ID header value", e);
        }
        String id = pm.getMessageID();
        ZimbraLog.lmtp.debug("Resent-Message-ID not found.  Message-ID=%s", id);
        return id;
    }

    /**
     * If the configured Message-ID cache size has changed, create a new cache and copy
     * values from the old one.
     */
    private void checkDedupeCacheSize() {
        try {
            Config config = Provisioning.getInstance().getConfig();
            int cacheSize = config.getMessageIdDedupeCacheSize();
            long entryTimeout = config.getMessageIdDedupeCacheTimeout();
            synchronized (ZimbraLmtpBackend.class) {
                Map<String, Set<Integer>> newMap = null;
                if (receivedMessageIDs == null) {
                    // if non-zero entry timeout is specified then use a timeout map, else use an lru map
                    receivedMessageIDs = entryTimeout == 0 ?
                            new LruMap<String, Set<Integer>>(cacheSize) :
                            new TimeoutMap<String, Set<Integer>>(entryTimeout);
                } else if (receivedMessageIDs instanceof LruMap) {
                    if (entryTimeout != 0) {
                        // change to a timeout map
                        newMap = MapUtil.newTimeoutMap(entryTimeout);
                    } else if (((LruMap) receivedMessageIDs).getMaxSize() != cacheSize) {
                        // adjust lru map size
                        newMap = MapUtil.newLruMap(cacheSize);
                    }
                } else if (receivedMessageIDs instanceof TimeoutMap) {
                    if (entryTimeout == 0) {
                        // change to a lru map
                        newMap = MapUtil.newLruMap(cacheSize);
                    } else {
                        ((TimeoutMap) receivedMessageIDs).setTimeout(entryTimeout);
                    }
                }
                if (newMap != null) {
                    // Copy entries from the old map to the new one.  The old map
                    // is iterated in order from least-recently accessed to last accessed.
                    // If the new map size is smaller, we'll get the latest entries.
                    newMap.putAll(receivedMessageIDs);
                    receivedMessageIDs = newMap;
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.lmtp.warn("Unable to update dedupe cache size.", e);
            // create an empty lru map if it doesn't exist already.
            synchronized (ZimbraLmtpBackend.class) {
                if (receivedMessageIDs == null) {
                    receivedMessageIDs = new LruMap<String, Set<Integer>>(0);
                }
            }
        }
    }

    private void addToDedupeCache(ParsedMessage pm, Mailbox mbox) {
        if (pm == null || mbox == null)
            return;
        String msgid = getMessageID(pm);
        if (msgid == null || msgid.equals(""))
            return;

        synchronized (ZimbraLmtpBackend.class) {
            Set<Integer> mboxIds = receivedMessageIDs.get(msgid);
            if (mboxIds == null) {
                mboxIds = new HashSet<Integer>();
                receivedMessageIDs.put(msgid, mboxIds);
            }
            mboxIds.add(mbox.getId());
        }
    }

    private void removeFromDedupeCache(String msgid, Mailbox mbox) {
        if (mbox == null || Strings.isNullOrEmpty(msgid))
            return;

        synchronized (ZimbraLmtpBackend.class) {
            Set<Integer> mboxIds = receivedMessageIDs.get(msgid);
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

    @Override
    public void deliver(LmtpEnvelope env, InputStream in, int sizeHint) throws UnrecoverableLmtpException {
        CopyInputStream cis = null;
        Blob blob = null;
        try {
            int bufLen = Provisioning.getInstance().getLocalServer().getMailDiskStreamingThreshold();
            cis = new CopyInputStream(in, sizeHint, bufLen, bufLen);
            in = cis;

//            MimeParserInputStream mpis = null;
//            if (ZMimeMessage.usingZimbraParser()) {
//                mpis = new MimeParserInputStream(in);
//                in = mpis;
//            }

            Rfc822ValidationInputStream validator = null;
            if (LC.zimbra_lmtp_validate_messages.booleanValue()) {
                validator = new Rfc822ValidationInputStream(in, LC.zimbra_lmtp_max_line_length.longValue());
                in = validator;
            }

            try {
                blob = StoreManager.getInstance().storeIncoming(in);
            } catch (IOException ioe) {
                throw new UnrecoverableLmtpException("Error in storing incoming message", ioe);
            }

            if (validator != null && !validator.isValid()) {
                try {
                    StoreManager.getInstance().delete(blob);
                } catch (IOException e) {
                    ZimbraLog.lmtp.warn("Error in deleting blob %s", blob, e);
                }
                setDeliveryStatuses(env.getRecipients(), LmtpReply.LINE_TOO_LONG);
                return;
            }

            BufferStream bs = cis.getBufferStream();
            byte[] data = bs.isPartial() ? null : bs.getBuffer();

            BlobInputStream bis = null;
            MimeMessage mm = null;
//            if (mpis != null) {
//                try {
//                    if (data == null) {
//                        bis = new BlobInputStream(blob);
//                        mpis.setSource(bis);
//                    } else {
//                        mpis.setSource(data);
//                    }
//                } catch (IOException ioe) {
//                    throw new UnrecoverableLmtpException("Error in accessing incoming message", ioe);
//                }
//
//                mm = new ZMimeMessage(mpis.getMessage(null));
//            }

            try {
                deliverMessageToLocalMailboxes(blob, bis, data, mm, env);
            } catch (Exception e) {
                ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
                setDeliveryStatuses(env.getLocalRecipients(), LmtpReply.TEMPORARY_FAILURE);
            }

            try {
                deliverMessageToRemoteMailboxes(blob, data, env);
            } catch (Exception e) {
                ZimbraLog.lmtp.warn("Exception delivering remote mail", e);
                setDeliveryStatuses(env.getRemoteRecipients(), LmtpReply.TEMPORARY_FAILURE);
            }
        } catch (ServiceException e) {
            ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpReply.TEMPORARY_FAILURE);
        } finally {
            if (cis != null) {
                cis.release();
            }

            if (blob != null) {
                try {
                    // clean up the incoming blob
                    StoreManager.getInstance().delete(blob);
                } catch (IOException e) {
                    ZimbraLog.lmtp.warn("Error in deleting blob %s", blob, e);
                }
            }
        }
    }

    private void deliverMessageToLocalMailboxes(Blob blob, BlobInputStream bis, byte[] data, MimeMessage mm, LmtpEnvelope env)
        throws ServiceException, IOException {

        List<LmtpAddress> recipients = env.getLocalRecipients();
        String envSender = env.getSender().getEmailAddress();

        boolean shared = recipients.size() > 1;
        List<Integer> targetMailboxIds = new ArrayList<Integer>(recipients.size());

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

                Account account;
                Mailbox mbox;
                boolean attachmentsIndexingEnabled;
                try {
                    account = Provisioning.getInstance().get(AccountBy.name, rcptEmail);
                    if (account == null) {
                        ZimbraLog.mailbox.warn("No account found delivering mail to " + rcptEmail);
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
                    ParsedMessageOptions pmo;
                    if (mm != null) {
                        pmo = new ParsedMessageOptions().setContent(mm).setDigest(blob.getDigest()).setSize(blob.getRawSize());
                    } else {
                        pmo = new ParsedMessageOptions(blob, data);
                    }

                    ParsedMessage pm;
                    if (attachmentsIndexingEnabled) {
                        if (pmAttachIndex == null) {
                            pmo.setAttachmentIndexing(true);
                            ZimbraLog.lmtp.debug("Creating ParsedMessage from %s with attachment indexing enabled", data == null ? "file" : "memory");
                            pmAttachIndex = new ParsedMessage(pmo);
                        }
                        pm = pmAttachIndex;
                    } else {
                        if (pmNoAttachIndex == null) {
                            pmo.setAttachmentIndexing(false);
                            ZimbraLog.lmtp.debug("Creating ParsedMessage from %s with attachment indexing disabled", data == null ? "file" : "memory");
                            pmNoAttachIndex = new ParsedMessage(pmo);
                        }
                        pm = pmNoAttachIndex;
                    }

                    msgId = pm.getMessageID();

                    if (account.isPrefMailLocalDeliveryDisabled()) {
                        ZimbraLog.lmtp.debug("Local delivery disabled for account %s", rcptEmail);
                        rcptMap.put(recipient, new RecipientDetail(account, mbox, pm, false, DeliveryAction.discard));
                        continue;
                    }

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
                    if (da == DeliveryAction.deliver) {
                        targetMailboxIds.add(mbox.getId());
                    }
                }
            }

            ZimbraLog.removeAccountFromContext();
            if (ZimbraLog.lmtp.isInfoEnabled()) {
                ZimbraLog.lmtp.info("Delivering message: size=%s, nrcpts=%d, sender=%s, msgid=%s",
                                    env.getSize() == 0 ? "unspecified" : Integer.toString(env.getSize()) + " bytes",
                                    recipients.size(),
                                    env.getSender(),
                                    msgId == null ? "" : msgId);
            }

            DeliveryContext sharedDeliveryCtxt = new DeliveryContext(shared, targetMailboxIds);
            sharedDeliveryCtxt.setIncomingBlob(blob);

            // We now know which addresses are valid and which ParsedMessage
            // version each recipient needs.  Deliver!
            for (LmtpAddress recipient : recipients) {
                String rcptEmail = recipient.getEmailAddress();
                LmtpReply reply = LmtpReply.TEMPORARY_FAILURE;
                RecipientDetail rd = rcptMap.get(recipient);
                if (rd == null) {
                    // Account or mailbox not found.
                    ZimbraLog.lmtp.info("rejecting message from=%s,to=%s: account or mailbox not found",
                            envSender, rcptEmail);
                    recipient.setDeliveryStatus(LmtpReply.PERMANENT_FAILURE);
                    continue;
                }
                if (rd.account != null) {
                    ZimbraLog.addAccountNameToContext(rd.account.getName());
                }
                if (rd.mbox != null) {
                    ZimbraLog.addMboxToContext(rd.mbox.getId());
                }

                boolean success = false;
                try {
                    switch (rd.action) {
                    case discard:
                        ZimbraLog.lmtp.info("accepted and discarded message from=%s,to=%s: local delivery is disabled",
                                envSender, rcptEmail);
                        if (rd.account.getPrefMailForwardingAddress() != null) {
                            // mail forwarding is set up
                            for (LmtpCallback callback : callbacks) {
                                ZimbraLog.lmtp.debug("Executing callback %s", callback.getClass().getName());
                                callback.forwardWithoutDelivery(rd.account, rd.mbox, envSender, rcptEmail, rd.pm);
                            }
                        }
                        reply = LmtpReply.DELIVERY_OK;
                        break;
                    case deliver:
                        Account account = rd.account;
                        Mailbox mbox = rd.mbox;
                        ParsedMessage pm = rd.pm;
                        List<ItemId> addedMessageIds = null;
                        ReentrantLock lock = mailboxDeliveryLocks.get(mbox.getId());
                        boolean acquiredLock;
                        try {
                            // Wait for the lock, up to the timeout
                            acquiredLock = lock.tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            acquiredLock = false;
                        }
                        if (!acquiredLock) {
                            ZimbraLog.lmtp.info("try again for message from=%s,to=%s: another mail delivery in progress.",
                                    envSender, rcptEmail);
                            reply = LmtpReply.TEMPORARY_FAILURE;
                            break;
                        }
                        try {
                            if (dedupe(pm, mbox)) {
                                // message was already delivered to this mailbox
                                ZimbraLog.lmtp.info("Not delivering message with duplicate Message-ID %s", pm.getMessageID());
                            } else if (mbox.dedupeForSelfMsg(pm)) {
                                ZimbraLog.mailbox.info("not delivering message, because it is a duplicate of sent message %s",
                                        pm.getMessageID());

                            } else if (recipient.getSkipFilters()) {
                                msgId = pm.getMessageID();
                                int folderId = Mailbox.ID_FOLDER_INBOX;
                                if (recipient.getFolder() != null) {
                                    try {
                                        Folder folder = mbox.getFolderByPath(null, recipient.getFolder());
                                        folderId = folder.getId();
                                    } catch (ServiceException se) {
                                        if (se.getCode().equals(MailServiceException.NO_SUCH_FOLDER)) {
                                            Folder folder = mbox.createFolder(null, recipient.getFolder(),
                                                    new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
                                            folderId = folder.getId();
                                        } else {
                                            throw se;
                                        }
                                    }
                                }
                                int flags = Flag.BITMASK_UNREAD;
                                if (recipient.getFlags() != null) {
                                    flags = Flag.toBitmask(recipient.getFlags());
                                }
                                DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId);
                                dopt.setFlags(flags).setTags(recipient.getTags()).setRecipientEmail(rcptEmail);
                                Message msg = mbox.addMessage(null, pm, dopt, sharedDeliveryCtxt);
                                addedMessageIds = Lists.newArrayList(new ItemId(msg));
                            } else if (!DebugConfig.disableIncomingFilter) {
                                // Get msgid first, to avoid having to reopen and reparse the blob
                                // file if Mailbox.addMessageInternal() closes it.
                                pm.getMessageID();
                                addedMessageIds = RuleManager.applyRulesToIncomingMessage(
                                        null, mbox, pm, (int) blob.getRawSize(), rcptEmail, env, sharedDeliveryCtxt,
                                        Mailbox.ID_FOLDER_INBOX, false, true);
                            } else {
                                pm.getMessageID();
                                DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
                                dopt.setFlags(Flag.BITMASK_UNREAD).setRecipientEmail(rcptEmail);
                                Message msg = mbox.addMessage(null, pm, dopt, sharedDeliveryCtxt);
                                addedMessageIds = Lists.newArrayList(new ItemId(msg));
                            }
                            success = true;
                            if (addedMessageIds != null && addedMessageIds.size() > 0) {
                                addToDedupeCache(pm, mbox);
                            }
                        } finally {
                            lock.unlock();
                        }

                        if (addedMessageIds != null && addedMessageIds.size() > 0) {
                            // Execute callbacks
                            for (LmtpCallback callback : callbacks) {
                                for (ItemId id : addedMessageIds) {
                                    if (id.belongsTo(mbox)) {
                                        // Message was added to the local mailbox, as opposed to a mountpoint.
                                        ZimbraLog.lmtp.debug("Executing callback %s", callback.getClass().getName());
                                        try {
                                            Message msg = mbox.getMessageById(null, id.getId());
                                            callback.afterDelivery(account, mbox, envSender, rcptEmail, msg);
                                        } catch (OutOfMemoryError oome) {
                                            Zimbra.halt("LMTP callback failed", oome);
                                        } catch (Throwable t) {
                                            ZimbraLog.lmtp.warn("LMTP callback threw an exception", t);
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
                        ZimbraLog.lmtp.info("try again for message from=%s,to=%s: mailbox skipped",
                                envSender, rcptEmail);
                        reply = LmtpReply.TEMPORARY_FAILURE;
                        break;
                    }
                } catch (DeliveryServiceException e) {
                    ZimbraLog.lmtp.info("rejecting message from=%s,to=%s: sieve filter rule", envSender, rcptEmail);
                    reply = LmtpReply.PERMANENT_MESSAGE_REFUSED;
                } catch (ServiceException e) {
                    if (e.getCode().equals(MailServiceException.QUOTA_EXCEEDED)) {
                        ZimbraLog.lmtp.info("rejecting message from=%s,to=%s: overquota", envSender, rcptEmail);
                        if (config.isPermanentFailureWhenOverQuota()) {
                            reply = LmtpReply.PERMANENT_FAILURE_OVER_QUOTA;
                        } else {
                            reply = LmtpReply.TEMPORARY_FAILURE_OVER_QUOTA;
                        }
                    } else if (e.isReceiversFault()) {
                        ZimbraLog.lmtp.info("try again for message from=%s,to=%s", envSender, rcptEmail, e);
                        reply = LmtpReply.TEMPORARY_FAILURE;
                    } else {
                        ZimbraLog.lmtp.info("rejecting message from=%s,to=%s", envSender, rcptEmail, e);
                        reply = LmtpReply.PERMANENT_FAILURE;
                    }
                } catch (Exception e) {
                    reply = LmtpReply.TEMPORARY_FAILURE;
                    ZimbraLog.lmtp.warn("try again for message from=%s,to=%s", envSender, rcptEmail, e);
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
            ParsedMessage mimeSource = pmAttachIndex != null ? pmAttachIndex : pmNoAttachIndex;
            MailboxBlob mblob = sharedDeliveryCtxt.getMailboxBlob();
            if (mblob != null && mimeSource != null) {
                if (bis == null) {
                    bis = mimeSource.getBlobInputStream();
                }
                if (bis != null) {
                    try {
                        // Update the MimeMessage with the blob that's stored inside the mailbox,
                        // since the incoming blob will be deleted.
                            Blob storedBlob = mblob.getLocalBlob();
                            bis.fileMoved(storedBlob.getFile());
                            MessageCache.cacheMessage(mblob.getDigest(), mimeSource.getOriginalMessage(), mimeSource.getMimeMessage());
                    } catch (IOException e) {
                        ZimbraLog.lmtp.warn("Unable to cache message for " + mblob, e);
                    }
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
        }
    }

    private void deliverMessageToRemoteMailboxes(Blob blob, byte[] data, LmtpEnvelope env) {
        Multimap<String, LmtpAddress> serverToRecipientsMap = env.getRemoteServerToRecipientsMap();
        for (String server : serverToRecipientsMap.keySet()) {
            LmtpClient lmtpClient = null;
            InputStream in = null;
            Collection<LmtpAddress> serverRecipients = serverToRecipientsMap.get(server);
            try {
                int lmtpPort = Provisioning.getInstance().getLocalServer().getLmtpBindPort();
                lmtpClient = new LmtpClient(server, lmtpPort);
                in = data == null ? blob.getInputStream() : new ByteArrayInputStream(data);
                boolean success = lmtpClient.sendMessage(in,
                                                         getRecipientsEmailAddress(serverRecipients),
                                                         env.getSender().getEmailAddress(),
                                                         blob.getFile().getName(),
                                                         blob.getRawSize());
                if (success) {
                    setDeliveryStatuses(serverRecipients, LmtpReply.DELIVERY_OK);
                } else {
                    ZimbraLog.lmtp.warn("Unsuccessful remote mail delivery - LMTP response: %s", lmtpClient.getResponse());
                    setDeliveryStatuses(serverRecipients, LmtpReply.TEMPORARY_FAILURE);
                }
            } catch (LmtpProtocolException e) {
                ZimbraLog.lmtp.warn("Unsuccessful remote mail delivery - LMTP response: %s", e.getMessage());
                setDeliveryStatuses(serverRecipients, LmtpReply.TEMPORARY_FAILURE);
            } catch (Exception e) {
                ZimbraLog.lmtp.warn("Exception delivering remote mail", e);
                setDeliveryStatuses(serverRecipients, LmtpReply.TEMPORARY_FAILURE);
            } finally {
                ByteUtil.closeStream(in);
                if (lmtpClient != null) {
                    lmtpClient.close();
                }
            }
        }
    }

    private static String[] getRecipientsEmailAddress(Collection<LmtpAddress> recipients) {
        LmtpAddress[] recipientsArray = recipients.toArray(new LmtpAddress[recipients.size()]);
        String[] recipientEmailAddrs = new String[recipientsArray.length];
        for (int i = 0; i < recipientsArray.length; i ++) {
            recipientEmailAddrs[i] = recipientsArray[i].getEmailAddress();
        }
        return recipientEmailAddrs;
    }

    private void setDeliveryStatuses(Collection<LmtpAddress> recipients, LmtpReply reply) {
        for (LmtpAddress recipient : recipients)
            recipient.setDeliveryStatus(reply);
    }
}
