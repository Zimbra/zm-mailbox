/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.ExpandMimeMessage;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.smime.SmimeHandler;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;

public class MessageCache {

    private static final Log sLog = LogFactory.getLog(MessageCache.class);

    private static final class CacheNode {
        CacheNode()  { }
        MimeMessage message;
        MimeMessage expanded;
        Map<Integer, String> smimeAccessInfo = new HashMap<Integer, String>();
        long size = 0;
    }

    /** Cache mapping message digest to the corresponding message structure. */
    private static Map<String, CacheNode> sCache = new LinkedHashMap<String, CacheNode>(150, (float) 0.75, true);
    /** Maximum number of items in {@link #sCache}. */
    private static int sMaxCacheSize;
    /** Number of bytes of message data stored in the cache.  This value includes only
     * messages that are read into memory, not streamed from disk. */
    private volatile static long sDataSize = 0;

    static {
        try {
            loadSettings();
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadSettings() throws ServiceException {
        sMaxCacheSize = Provisioning.getInstance().getLocalServer().getMessageCacheSize();
        ZimbraLog.cache.info("setting message cache size to " + sMaxCacheSize);
    }

    /** Returns the number of messages in the cache. */
    public static int getSize() {
        synchronized (sCache) {
            return sCache.size();
        }
    }

    public static boolean contains(String digest) {
        synchronized (sCache) {
            return sCache.containsKey(digest);
        }
    }

    public static long getDataSize() {
        return sDataSize;
    }

    /** Uncaches any data associated with the given item.  This must be done
     *  before you change the item's content; otherwise, the cache will return
     *  stale data. */
    static void purge(MailItem item) {
        String digest = item.getDigest();
        if (digest != null) {
            sLog.debug("Purging item %d from the message cache.", item.getId());
            purge(item.getDigest());
        }
    }

    /** Uncaches any data associated with the given item.  This must be done
     *  before you change the item's content; otherwise, the cache will return
     *  stale data. */
    static void purge(Mailbox mbox, int itemId) {
        String digest = null;
        try {
            digest = DbMailItem.getBlobDigest(mbox, itemId);
        } catch (ServiceException e) {
            sLog.warn("Unable to uncache message for item %d.", itemId, e);
        }
        if (digest != null) {
            sLog.debug("Purging item %d from the message cache.", itemId);
            purge(digest);
        }
    }

    /** Uncaches any data associated with the given item.  This must be done
     *  before you change the item's content; otherwise, the cache will return
     *  stale data. */
    public static void purge(String digest) {
        if (digest != null) {
            synchronized (sCache) {
                CacheNode node = sCache.remove(digest);
                if (node != null) {
                    sLog.debug("Purged digest %s from the message cache.", digest);
                    sDataSize -= node.size;
                }
            }
        }
    }

    /** Returns a JavaMail {@link javax.mail.internet.MimeMessage}
     *  encapsulating the message content.  If possible, TNEF and uuencoded
     *  attachments are expanded and their components are presented as
     *  standard MIME attachments.  If TNEF or uuencode decoding fails, the
     *  MimeMessage wraps the raw message content.
     *
     * @return A MimeMessage wrapping the RFC822 content of the Message.
     * @throws ServiceException when errors occur opening, reading,
     *                          uncompressing, or parsing the message file,
     *                          or when the file does not exist.
     * @see #getRawContent()
     * @see #getItemContent()
     * @see com.zimbra.cs.mime.TnefConverter
     * @see com.zimbra.cs.mime.UUEncodeConverter */
    static MimeMessage getMimeMessage(MailItem item, boolean expand) throws ServiceException {
        String digest = item.getDigest();
        CacheNode cnode = null;
        boolean cacheHit = true;
        boolean newNode = false;
        InputStream in = null;
        int mboxId = item.getMailboxId();
        boolean isEncrypted = false;

        synchronized (sCache) {
            cnode = sCache.get(digest);
            if (cnode == null) {
                newNode = true;
                cnode = new CacheNode();
            }
        }

        try {
            if (cnode.message == null) {
                sLog.debug("Loading MimeMessage for item %d.", item.getId());
                cacheHit = false;
                try {
                    in = fetchFromStore(item);
                    cnode.message = new Mime.FixedMimeMessage(JMSession.getSession(), in);
                    if (item.getSize() < MESSAGE_CACHE_DISK_STREAMING_THRESHOLD) {
                        cnode.size = item.getSize();
                        // Not the best place to increment the data size, but cacheItem()
                        // won't get called if we're expanding a message for an existing
                        // node.
                        sDataSize += cnode.size;
                    }
                } finally {
                    ByteUtil.closeStream(in);
                }
            }

            if (expand) {
                sLog.debug("Expanding MimeMessage for item %d.", item.getId());
                try {
                    MimeMessage decryptedMimeMessage = null;
                    if (item instanceof Message) {
                        // if the mime is encrypted; decrypt it first
                        if (cnode.message != null) {
                            isEncrypted = Mime.isEncrypted(cnode.message.getContentType());
                        }
                        if (isEncrypted) {
                            if (isSmimeFeatureToggled(item.getMailbox(), cnode)) {
                                sLog.debug(
                                    "Smime feature is toggled. So remove old entry from smimeAccessInfo for mailboxId=%d and itemDigest=%s",
                                    mboxId, item.getDigest());
                                cnode.smimeAccessInfo.remove(mboxId);
                            }
                            if (cnode.expanded == null || !cnode.smimeAccessInfo.containsKey(mboxId)) {
                                cacheHit = false;
                                decryptedMimeMessage = doDecryption(item, cnode, mboxId);
                            }
                        }
                    }
                    //expand if the message has not yet been expanded or if the message is decrypted successfully
                    if (cnode.expanded == null || (decryptedMimeMessage != null && cnode.expanded != decryptedMimeMessage)) {
                        cacheHit = false;
                        expandMessage(item, cnode, decryptedMimeMessage);
                    }
                } catch (Exception e) {
                    // if the conversion bombs for any reason, revert to the original
                    sLog.warn("MIME converter failed for message %d.  Reverting to original.", item.getId(), e);
                    cnode.expanded = cnode.message;
                }
            }

            if (newNode) {
                cacheItem(digest, cnode);
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while retrieving content for item " + item.getId(), e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while creating MimeMessage for item " + item.getId(), e);
        } finally {
            ByteUtil.closeStream(in);
        }

        if (cacheHit) {
            sLog.debug("Cache hit for item %d: digest=%s, expand=%b.", item.getId(), item.getDigest(), expand);
            ZimbraPerf.COUNTER_MBOX_MSG_CACHE.increment(100);
        } else {
            sLog.debug("Cache miss for item %d: digest=%s, expand=%b.", item.getId(), item.getDigest(), expand);
            ZimbraPerf.COUNTER_MBOX_MSG_CACHE.increment(0);
        }

        if (expand) {
            if (isEncrypted && (!cnode.smimeAccessInfo.containsKey(mboxId)
                || cnode.smimeAccessInfo.get(mboxId) != null)) {
                return cnode.message;
            }
            return cnode.expanded;
        } else {
            return cnode.message;
        }
    }

    private static boolean isSmimeFeatureToggled(Mailbox mailbox, CacheNode cnode) {
        if (cnode.smimeAccessInfo.containsKey(mailbox.getId())) {
            try {
                String errorCode = cnode.smimeAccessInfo.get(mailbox.getId());
                boolean isSmimeFeatureEnabled = mailbox.getAccount().isFeatureSMIMEEnabled();
                if (isSmimeFeatureEnabled && MimeConstants.ERR_FEATURE_SMIME_DISABLED.equals(errorCode)) {
                    return true;
                } else if (!isSmimeFeatureEnabled && errorCode == null) {
                    return true;
                }
            } catch (ServiceException e) {
                sLog.warn("Unable to get account for mailbox with id=%d", mailbox.getId());
                return false;
            }
        }
        return false;
    }

    private static void expandMessage(MailItem item, CacheNode cnode, MimeMessage decryptedMimeMessage)
        throws MessagingException, ServiceException {
        MimeMessage mimeToExpand = cnode.message;
        if (decryptedMimeMessage != null) {
            mimeToExpand = decryptedMimeMessage;
        }
        MimeMessage decodedMimeMessage = null;
        if (Mime.isPKCS7Signed(mimeToExpand.getContentType())) {
            if (SmimeHandler.getHandler() != null) {
                ZimbraLog.mailbox
                    .debug("The message is PKCS7 signed. Forwarding it to SmimeHandler for decoding.");
                decodedMimeMessage = SmimeHandler.getHandler().decodePKCS7Message(item.getAccount(),
                    mimeToExpand);
            }
        }
        ExpandMimeMessage expander = new ExpandMimeMessage(
            decodedMimeMessage != null ? decodedMimeMessage : mimeToExpand);
        expander.expand();
        cnode.expanded = expander.getExpanded();
        if (cnode.expanded != cnode.message) {
            sDataSize += cnode.size;
            cnode.size *= 2;
        }
    }

    private static MimeMessage doDecryption(MailItem item, CacheNode cnode, int mboxId) {
        MimeMessage decryptedMimeMessage = null;
        if (SmimeHandler.getHandler() != null) {
            sLog.debug(
                "The message %d is encrypted. Forwarding it to SmimeHandler for decryption.",
                item.getId());
            String decryptionError = null;
            try {
                decryptedMimeMessage = SmimeHandler.getHandler().decryptMessage(
                    ((Message) item).getMailbox(), cnode.message, item.getId());
                if (decryptedMimeMessage == null) {
                    decryptionError = MimeConstants.ERR_DECRYPTION_FAILED;
                }
            } catch (ServiceException e) {
                switch (e.getCode()) {
                case ServiceException.FEATURE_SMIME_DISABLED:
                    decryptionError = MimeConstants.ERR_FEATURE_SMIME_DISABLED;
                    break;
                case ServiceException.LOAD_CERTIFICATE_FAILED:
                    decryptionError = MimeConstants.ERR_LOAD_CERTIFICATE_FAILED;
                    break;
                case ServiceException.LOAD_PRIVATE_KEY_FAILED:
                    decryptionError = MimeConstants.ERR_LOAD_PRIVATE_KEY_FAILED;
                    break;
                case ServiceException.USER_CERT_MISMATCH:
                    decryptionError = MimeConstants.ERR_USER_CERT_MISMATCH;
                    break;
                case ServiceException.DECRYPTION_FAILED:
                    decryptionError = MimeConstants.ERR_DECRYPTION_FAILED;
                    break;
                }
            } finally {
                cnode.smimeAccessInfo.put(mboxId, decryptionError);
            }
        }
        return decryptedMimeMessage;
    }

    /** For remote stores, we allow JavaMail to stream message content to
     *  a memory buffer when the messages are sufficiently small.  (For
     *  local stores, all cached messages are backed by disk.) */
    private static final int MESSAGE_CACHE_DISK_STREAMING_THRESHOLD = 4096;

    static InputStream fetchFromStore(MailItem item) throws ServiceException, IOException {
        MailboxBlob mblob = item.getBlob();
        if (mblob == null)
            throw ServiceException.FAILURE("missing blob for id: " + item.getId() + ", change: " + item.getModifiedSequence(), null);

        if (item.getSize() < MESSAGE_CACHE_DISK_STREAMING_THRESHOLD) {
            return StoreManager.getReaderSMInstance(mblob.getLocator()).getContent(mblob);
        } else {
            return StoreManager.getReaderSMInstance(mblob.getLocator()).getContent(mblob.getLocalBlob());
        }
    }

    /**
     * Public API that adds an existing <tt>MimeMessage</tt> to the cache.
     * @param digest the message digest
     * @param original the original message
     * @param expanded the expanded message
     */
    public static void cacheMessage(String digest, MimeMessage original, MimeMessage expanded) {
        sLog.debug("Caching existing MimeMessage, digest=%s.", digest);
        CacheNode cnode = new CacheNode();
        cnode.message = original;
        cnode.expanded = expanded;
        cacheItem(digest, cnode);
    }

    private static void cacheItem(String digest, CacheNode cnode) {
        sLog.debug("Caching MimeMessage for digest %s.", digest);
        synchronized (sCache) {
            sCache.put(digest, cnode);
            // Cache data size was incremented in getMimeMessage().

            // trim the cache if needed
            if (sCache.size() > sMaxCacheSize) {
                Iterator<Map.Entry<String, CacheNode>> it = sCache.entrySet().iterator();
                while (sCache.size() > sMaxCacheSize && it.hasNext()) {
                    Map.Entry<String, CacheNode> entry = it.next();
                    sLog.debug("Pruning digest %s from the cache.", entry.getKey());
                    it.remove();
                    sDataSize -= entry.getValue().size;
                }
            }
        }
    }

    public static void removeDecryptedMessages(int mboxId) {
        sLog.debug("Start removing decrypted messages for mboxId=%d", mboxId);
        synchronized (sCache) {
            Iterator<Map.Entry<String, CacheNode>> it = sCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CacheNode> entry = it.next();
                CacheNode cacheNode = entry.getValue();
                try {
                    if (Mime.isEncrypted(cacheNode.message.getContentType())
                        && cacheNode.smimeAccessInfo.containsKey(mboxId)) {
                        cacheNode.smimeAccessInfo.remove(mboxId);
                    }
                } catch (MessagingException e) {
                    sLog.warn("MessagingException while checking content type for cache node with digest = %s",
                        entry.getKey(), e);
                }
            }
        }
        sLog.debug("Removed decrypted messages for mboxId=%d", mboxId);
    }

    public static String getDecryptionError(int id, String digest) {
        if (digest != null) {
            synchronized (sCache) {
                CacheNode node = sCache.get(digest);
                if (node != null) {
                    return node.smimeAccessInfo.get(id);
                }
            }
        }
        return null;
    }
}
