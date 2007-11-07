/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
/*
 * Created on Nov 13, 2005
 */
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author dkarp
 */
public class MessageCache {

    private static enum ConvertedState { RAW, EXPANDED, BOTH };

    private static final class CacheNode {
        int mSize;
        byte[] mContent;
        MimeMessage mMessage;
        ConvertedState mConvertersRun;

        CacheNode(int size, byte[] content)                            { mSize = size;  mContent = content; }
        CacheNode(int size, MimeMessage mm, ConvertedState converted)  { mSize = size;  mMessage = mm;  mConvertersRun = converted; }
    }

    private static final int DEFAULT_CACHE_SIZE = 16 * 1000 * 1000;
    
    private static Map<String, CacheNode> mCache = new LinkedHashMap<String, CacheNode>(150, (float) 0.75, true);
    private static int mTotalSize = 0;
    private static int mMaxCacheSize;
    static {
        try {
            mMaxCacheSize = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMessageCacheSize, DEFAULT_CACHE_SIZE);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    /** Uncaches any data associated with the given item.  This must be done
     *  before you change the item's content; otherwise, the cache will return
     *  stale data. */
    static void purge(MailItem item) {
        String key = item.getDigest();
        synchronized (mCache) {
            mCache.remove(key);
        }
    }

    /** Returns the raw, uncompressed content of the item.  For messages,
     *  this is the body as received via SMTP; no postprocessing has been
     *  performed to make opaque attachments (e.g. TNEF) visible.
     * 
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getRawContent() */
    static byte[] getItemContent(MailItem item) throws ServiceException {
        String key = item.getDigest();
        if (key == null || key.equals(""))
            return null;

        boolean cacheHit = false;
        CacheNode cnode = null;
        synchronized (mCache) {
            cnode = mCache.get(key);
            cacheHit = cnode != null && cnode.mContent != null;

            if (!cacheHit && cnode != null) {
                // can't use a cached MimeMessage because of TNEF conversion
                mCache.remove(key);  mTotalSize -= cnode.mSize;
            }
        }

        if (!cacheHit) {
            try {
                // wasn't cached; fetch, cache, and return it
                int size = item.getSize();
                InputStream is = fetchFromStore(item);
                cnode = new CacheNode(size, ByteUtil.getContent(is, size));

                // cache the message content (if it'll fit)
                cacheItem(key, cnode);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException while retrieving content for item " + item.getId(), e);
            }
        } else {
            if (ZimbraLog.cache.isDebugEnabled())
                ZimbraLog.cache.debug("msgcache: found raw content in cache: " + item.getDigest());
        }
        
        ZimbraPerf.COUNTER_MBOX_MSG_CACHE.increment(cacheHit ? 1 : 0);
        
        return cnode.mContent;
    }

    /** Returns an {@link InputStream} of the raw, uncompressed content of
     *  the item.  For messages, this is the body as received via SMTP; no
     *  postprocessing has been performed to make opaque attachments (e.g.
     *  TNEF) visible.
     * 
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getItemContent() */
    static InputStream getRawContent(MailItem item) throws ServiceException {
        String key = item.getDigest();
        if (key == null || key.equals(""))
            return null;
        if (item.getSize() < mMaxCacheSize) {
            // Content is small enough to be cached in memory
            return new SharedByteArrayInputStream(getItemContent(item));
        }
        try {
            return fetchFromStore(item);
        } catch (IOException e) {
            String msg = String.format("Unable to get content for %s %d", item.getClass().getSimpleName(), item.getId());
            throw ServiceException.FAILURE(msg, e);
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
        String key = item.getDigest();
        boolean cacheHit = false;
        CacheNode cnode = null, cnOrig = null;
        synchronized (mCache) {
            cnode = cnOrig = mCache.get(key);
            if (cnode != null && cnode.mMessage != null)
                cacheHit = cnode.mConvertersRun == ConvertedState.BOTH || cnode.mConvertersRun == (expand ? ConvertedState.EXPANDED : ConvertedState.RAW);

            if (!cacheHit && cnode != null) {
                // replacing the cached byte array with a MimeMessage
                mCache.remove(key);  mTotalSize -= cnode.mSize;
            }
        }

        if (!cacheHit) {
        	InputStream is = null;
            try {
                // wasn't cached; fetch the content and create the MimeMessage
                int size = item.getSize();
                if (expand && cnOrig != null && cnOrig.mMessage != null && cnOrig.mConvertersRun == ConvertedState.RAW) {
                    // switching from RAW to EXPANDED -- can reuse an existing raw MimeMessage
                    cnode = new CacheNode(size, cnOrig.mMessage, ConvertedState.BOTH);
                } else {
                    // use the raw byte array to construct the MimeMessage if possible, else read from disk
                    if (cnOrig == null || cnOrig.mContent == null) {
                        is = fetchFromStore(item);
                    } else {
                        is = new SharedByteArrayInputStream(cnOrig.mContent);
                    }
                    
                    cnode = new CacheNode(size, new Mime.FixedMimeMessage(JMSession.getSession(), is), expand ? ConvertedState.BOTH : ConvertedState.RAW);
                }

                if (expand) {
                    try {
                        // handle UUENCODE and TNEF conversion here...
                        for (Class visitor : MimeVisitor.getConverters())
                            if (((MimeVisitor) visitor.newInstance()).accept(cnode.mMessage))
                                cnode.mConvertersRun = ConvertedState.EXPANDED;
                    } catch (Exception e) {
                        // if the conversion bombs for any reason, revert to the original
                        ZimbraLog.mailbox.warn("MIME converter failed for message " + item.getId(), e);

                        if (cnOrig == null || cnOrig.mContent == null) {
                            is = fetchFromStore(item);
                        } else {
                            is = new SharedByteArrayInputStream(cnOrig.mContent);
                        }
                        
                        cnode = new CacheNode(size, new MimeMessage(JMSession.getSession(), is), ConvertedState.BOTH);
                    }
                }

                // cache the MimeMessage (if it'll fit)
                // cacheItem(key, cnode);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException while retrieving content for item " + item.getId(), e);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("MessagingException while creating MimeMessage for item " + item.getId(), e);
            }
        } else {
            if (ZimbraLog.cache.isDebugEnabled())
                ZimbraLog.cache.debug("msgcache: found mime message in cache: " + item.getDigest());
        }

        ZimbraPerf.COUNTER_MBOX_MSG_CACHE.increment(cacheHit ? 1 : 0);
        
        return cnode.mMessage;
    }

    private static InputStream fetchFromStore(MailItem item) throws ServiceException, IOException {
        MailboxBlob msgBlob = item.getBlob();
        if (msgBlob == null)
            throw ServiceException.FAILURE("missing blob for id: " + item.getId() + ", change: " + item.getModifiedSequence(), null);
        return StoreManager.getInstance().getContent(msgBlob);
    }

    private static void cacheItem(String key, CacheNode cnode) {
        if (cnode.mSize >= mMaxCacheSize)
            return;

        synchronized (mCache) {
            if (ZimbraLog.cache.isDebugEnabled())
                ZimbraLog.cache.debug("msgcache: caching " + (cnode.mContent != null ? "raw" : "mime") + " message: " + key);
            mCache.put(key, cnode);
            mTotalSize += cnode.mSize;

            // trim the cache if needed
            if (mTotalSize > mMaxCacheSize) {
                for (Iterator it = mCache.values().iterator(); mTotalSize > DEFAULT_CACHE_SIZE && it.hasNext(); ) {
                    CacheNode cnPurge = (CacheNode) it.next();
                    it.remove();
                    mTotalSize -= cnPurge.mSize;
                }
            }
        }
    }
}
