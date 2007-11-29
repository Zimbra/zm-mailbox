/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

import javax.mail.MessagingException;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.mailbox.Notification;
import com.zimbra.cs.mailbox.QuotaWarning;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.util.Zimbra;

public class ZimbraLmtpBackend implements LmtpBackend {
    
    private static List<LmtpCallback> sCallbacks = new CopyOnWriteArrayList<LmtpCallback>(); 
	private static LRUMap sReceivedMessageIDs = null;

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
	            sReceivedMessageIDs = new LRUMap(cacheSize);
	    } catch (ServiceException e) {
	        ZimbraLog.lmtp.error("could not read zimbraMessageIdDedupeCacheSize; no deduping will be performed", e);
	    }
        addCallback(Notification.getInstance());
        addCallback(QuotaWarning.getInstance());
	}

	public LmtpStatus getAddressStatus(LmtpAddress address) {
        String addr = address.getEmailAddress();

        try {
		    Account acct = Provisioning.getInstance().get(AccountBy.name, addr);
		    if (acct == null) {
		    	ZimbraLog.lmtp.info("rejecting address " + addr + ": no account");
		    	return LmtpStatus.REJECT;
		    }
		    
		    String acctStatus = acct.getAccountStatus();
		    if (acctStatus == null) {
		    	ZimbraLog.lmtp.warn("rejecting address " + addr + ": no account status");
		    	return LmtpStatus.REJECT;
		    }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE)) {
                ZimbraLog.lmtp.info("try again for address " + addr + ": account status maintenance");
                return LmtpStatus.TRYAGAIN;
            }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
                ZimbraLog.lmtp.info("rejecting address " + addr + ": account status closed");
                return LmtpStatus.REJECT;
            }
            
		    if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
                acctStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT) ||
                acctStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKED)) 
            {
		        return LmtpStatus.ACCEPT;
		    }

		    ZimbraLog.lmtp.info("rejecting address " + addr + ": unknown account status " + acctStatus);
		    return LmtpStatus.REJECT;
		
		} catch (ServiceException e) {
            if (e.isReceiversFault()) {
                ZimbraLog.lmtp.warn("try again for address " + addr + ": exception occurred", e);
                return LmtpStatus.TRYAGAIN;
            } else {
                ZimbraLog.lmtp.warn("rejecting address " + addr + ": exception occurred", e);
                return LmtpStatus.REJECT;
            }
		}
	}
	
	public void deliver(LmtpEnvelope env, InputStream in, int sizeHint) {
        try {
            deliverMessageToLocalMailboxes(in, env, sizeHint);
        } catch (MessagingException me) {
            ZimbraLog.lmtp.warn("Exception delivering mail (permanent failure)", me);
            setDeliveryStatuses(env.getRecipients(), LmtpStatus.REJECT);
        } catch (ServiceException e) {
            ZimbraLog.lmtp.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpStatus.TRYAGAIN);
        }
    }

    private boolean dedupe(ParsedMessage pm, Mailbox mbox) {
        if (sReceivedMessageIDs == null || pm == null || mbox == null)
            return false;
        String msgid = pm.getMessageID();
        if (msgid == null || msgid.equals(""))
            return false;

        synchronized (sReceivedMessageIDs) {
            Object hit = sReceivedMessageIDs.get(msgid);
            if (hit instanceof Integer)
                return hit.equals(new Integer(mbox.getId()));
            else if (hit instanceof Set)
                return ((Set) hit).contains(new Integer(mbox.getId()));
            else
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void recordReceipt(String msgid, Mailbox mbox) {
        if (sReceivedMessageIDs == null || msgid == null || mbox == null)
            return;
        if (msgid == null || msgid.equals(""))
            return;
        Integer mboxid = mbox.getId();

        synchronized (sReceivedMessageIDs) {
            Object hit = sReceivedMessageIDs.get(msgid);
            if (hit instanceof Integer) {
                Set<Integer> set = new HashSet<Integer>();
                set.add((Integer) hit);  set.add(mboxid);
                sReceivedMessageIDs.put(msgid, set);
            } else if (hit instanceof Set)
                ((Set) hit).add(mboxid);
            else
                sReceivedMessageIDs.put(msgid, mboxid);
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
    throws MessagingException, ServiceException {

        List<LmtpAddress> recipients = env.getRecipients();
        String envSender = env.getSender().getEmailAddress();
        
        boolean shared = recipients.size() > 1;
        List<Integer> targetMailboxIds = new ArrayList<Integer>(recipients.size());

        Map<LmtpAddress, RecipientDetail> rcptMap = new HashMap<LmtpAddress, RecipientDetail>(recipients.size());

        Volume volume = Volume.getCurrentMessageVolume();
        Blob blob = null;
        int diskThreshold = Provisioning.getInstance().getLocalServer().getIntAttr(
            Provisioning.A_zimbraMailDiskStreamingThreshold, Integer.MAX_VALUE);
        byte[] data = null;
        
        try {
            if (sizeHint <= diskThreshold) {
                ZimbraLog.lmtp.debug("Reading message of size %d into memory.", sizeHint);
                data = ByteUtil.getContent(in, sizeHint);
                if (data.length == 0) {
                    throw new MessagingException("Empty message not allowed");
                }
            } else {
                ZimbraLog.lmtp.debug("Streaming message of size %d to disk.", sizeHint);
                blob = StoreManager.getInstance().storeIncoming(in, sizeHint, null, volume.getId());
                data = blob.getData();
                ZimbraLog.lmtp.debug("Wrote message to %s.", blob.getPath());
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to process incoming message.", e);
        }

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
                    if (attachmentsIndexingEnabled) {
                        if (pmAttachIndex == null) {
                            ZimbraLog.lmtp.debug("Creating ParsedMessage with attachment indexing enabled.");
                            if (data != null) {
                                pmAttachIndex = new ParsedMessage(data, true).analyze();
                            } else {
                                try {
                                    pmAttachIndex = new ParsedMessage(blob.getFile(), null, true);
                                    pmAttachIndex.setRawDigest(blob.getDigest());
                                    pmAttachIndex.analyze();
                                } catch (IOException e) {
                                    throw ServiceException.FAILURE("Unable to parse message.", e);
                                }
                            }
                        }
                        pm = pmAttachIndex;
                    } else {
                        if (pmNoAttachIndex == null) {
                            ZimbraLog.lmtp.debug("Creating ParsedMessage with attachment indexing disabled.");
                            if (data != null) {
                                pmNoAttachIndex = new ParsedMessage(data, false).analyze();
                            } else {
                                try {
                                    pmNoAttachIndex = new ParsedMessage(blob.getFile(), null, false);
                                    pmNoAttachIndex.setRawDigest(blob.getDigest());
                                    pmNoAttachIndex.analyze();
                                } catch (IOException e) {
                                    throw ServiceException.FAILURE("Unable to parse message.", e);
                                }
                            }
                        }
                        pm = pmNoAttachIndex;
                    }
                    assert(pm != null);
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
	                    targetMailboxIds.add(new Integer(mbox.getId()));
                }
            }
            
            logEnvelope(env, msgId);
            ZimbraLog.clearContext();

            SharedDeliveryContext sharedDeliveryCtxt =
            	new SharedDeliveryContext(shared, targetMailboxIds);
            if (blob != null) {
                if ((pmAttachIndex != null && pmAttachIndex.wasMutated()) ||
                    (pmNoAttachIndex != null && pmNoAttachIndex.wasMutated())) {
                    ZimbraLog.lmtp.debug("Incoming message was mutated.  Deleting copy on disk.");
                    // Message was mutated, so the blob on disk is now invalid.
                    try {
                        StoreManager.getInstance().delete(blob);
                        blob = null;
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Unable to delete " + blob, e);
                    }
                } else {
                    // Tell mailbox code that the blob is already in the incoming directory.
                    sharedDeliveryCtxt.setPreexistingBlob(blob);
                }
            }

            // We now know which addresses are valid and which ParsedMessage
            // version each recipient needs.  Deliver!
            try {
                for (LmtpAddress recipient : recipients) {
                    String rcptEmail = recipient.getEmailAddress();
                    LmtpStatus status = LmtpStatus.TRYAGAIN;
                    RecipientDetail rd = rcptMap.get(recipient);
                    if (rd.account != null)
                        ZimbraLog.addAccountNameToContext(rd.account.getName());
                    if (rd.mbox != null)
                        ZimbraLog.addMboxToContext(rd.mbox.getId());

                    try {
                        if (rd != null) {
                            switch (rd.action) {
                            case discard:
                                ZimbraLog.lmtp.info("accepted and discarded message for " + rcptEmail + ": local delivery is disabled");
                                status = LmtpStatus.ACCEPT;
                                break;
                            case deliver:
                                Account account = rd.account;
                                Mailbox mbox = rd.mbox;
                                ParsedMessage pm = rd.pm;
                                Message msg = null;
                                
                                String msgid = null;
                                if (dedupe(pm, mbox)) {
                                    // message was already delivered to this mailbox
                                    ZimbraLog.lmtp.info("Not delivering message with duplicate Message-ID %s", pm.getMessageID());
                                    msg = null;
                                } else if (!DebugConfig.disableFilter) {
                                    // Get msgid first, to avoid having to reopen and reparse the blob
                                    // file if Mailbox.addMessageInternal() closes it.
                                    msgid = pm.getMessageID();
                                    msg = RuleManager.getInstance().applyRules(account, mbox, pm, pm.getRawSize(),
                                                                               rcptEmail, sharedDeliveryCtxt);
                                } else {
                                    msgid = pm.getMessageID();
                                    msg = mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, false, Flag.BITMASK_UNREAD, null,
                                                          rcptEmail, sharedDeliveryCtxt);
                                }
                                if (msg != null) {
                                    recordReceipt(msgid, mbox);
                                    
                                    // Execute callbacks
                                    for (LmtpCallback callback : sCallbacks) {
                                        if (ZimbraLog.lmtp.isDebugEnabled()) {
                                            ZimbraLog.lmtp.debug("Executing callback %s", callback.getClass().getName());
                                        }
                                        try {
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
                                status = LmtpStatus.ACCEPT;
                                break;
                            case defer:
                                // Delivery to mailbox skipped.  Let MTA retry again later.
                                // This case happens for shared delivery to a mailbox in
                                // backup mode.
                                ZimbraLog.lmtp.info("try again for message " + rcptEmail + ": mailbox skipped");
                                status = LmtpStatus.TRYAGAIN;
                                break;
                            }
                        } else {
                            // Account or mailbox not found.
                            ZimbraLog.lmtp.info("rejecting message " + rcptEmail + ": account or mailbox not found");
                        	status = LmtpStatus.REJECT;
                        }
                    } catch (IOException ioe) {
                        status = LmtpStatus.TRYAGAIN;
                        ZimbraLog.lmtp.info("try again for " + rcptEmail + ": exception occurred", ioe);
                    } catch (ServiceException se) {
                        if (se.getCode().equals(MailServiceException.QUOTA_EXCEEDED)) {
                            ZimbraLog.lmtp.info("rejecting message " + rcptEmail + ": overquota");
                            status = LmtpStatus.OVERQUOTA;
                        } else if (se.isReceiversFault()) {
                            ZimbraLog.lmtp.info("try again for message " + rcptEmail + ": exception occurred", se);
                            status = LmtpStatus.TRYAGAIN;
                        } else {
                            ZimbraLog.lmtp.info("rejecting message " + rcptEmail + ": exception occurred", se);
                            status = LmtpStatus.REJECT;
                        }
                    } catch (Exception e) {
                        status = LmtpStatus.TRYAGAIN;
                        ZimbraLog.lmtp.info("try again for message " + rcptEmail + ": exception occurred", e);
                    } finally {
                        ZimbraLog.clearContext();
                        recipient.setDeliveryStatus(status);
                        if (shared && rd != null && rd.esd) {
                            rd.mbox.endSharedDelivery();
                            rd.esd = false;
                        }
                    }
                }
                
                // If this message is being streamed from disk, cache it
                ParsedMessage mimeSource = pmAttachIndex;
                if (mimeSource == null) {
                    mimeSource = pmNoAttachIndex;
                }
                if (mimeSource != null && blob != null && mimeSource.isStreamedFromDisk()) {
                    MessageCache.cacheStreamedMessage(blob.getDigest(), mimeSource.getMimeMessage());
                }
            } finally {
                try {
                    // Close file descriptors
                    if (pmAttachIndex != null) {
                        pmAttachIndex.closeFile();
                    }
                    if (pmNoAttachIndex != null) {
                        pmNoAttachIndex.closeFile();
                    }
                } catch (IOException e) {
                    ZimbraLog.lmtp.warn("Unable to close file descriptors after delivery.", e);
                }

                // Clean up blobs in incoming directory after delivery to all recipients.
                if (shared) {
                    Blob sharedBlob = sharedDeliveryCtxt.getBlob();
                    if (sharedBlob != null) {
                        try {
                            ZimbraLog.lmtp.debug("Deleting shared blob at %s.", sharedBlob.getPath());
                            StoreManager.getInstance().delete(sharedBlob);
                        } catch (IOException e) {
                            ZimbraLog.lmtp.warn("Unable to delete temporary incoming blob after delivery: " + sharedBlob.toString(), e);
                        }
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

    private void setDeliveryStatuses(List<LmtpAddress> recipients, LmtpStatus status) {
        for (LmtpAddress recipient : recipients)
            recipient.setDeliveryStatus(status);
    }
}
