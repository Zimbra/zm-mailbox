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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StatsFile;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp
 */
public class MessageCache {

    private static final class CacheNode {
        int mSize;
        byte[] mContent;
        MimeMessage mMessage;

        CacheNode(int size, byte[] content)  { mSize = size;  mContent = content; }
        CacheNode(int size, MimeMessage mm)  { mSize = size;  mMessage = mm; }
    }

    private static final int DEFAULT_CACHE_SIZE = 16 * 1000 * 1000;

    private static Map mCache = new LinkedHashMap(150, (float) 0.75, true);
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

    private static final StatsFile STATS_FILE =
        new StatsFile("perf_message_cache.csv", new String[] { "id", "hit" }, false);

    /** Returns an {@link InputStream} of the raw, uncompressed content of
     *  the item.  For messages, this is the body as received via SMTP; no
     *  postprocessing has been performed to make opaque attachments (e.g.
     *  TNEF) visible.
     * 
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getRawContent() */
    static byte[] getItemContent(MailItem item) throws ServiceException {
        String key = item.getDigest();
        if (key == null || key.equals(""))
            return null;

        boolean cacheHit = false;
        CacheNode cn = null;
        synchronized (mCache) {
            cn = (CacheNode) mCache.get(key);
            cacheHit = cn != null && cn.mContent != null;

            if (!cacheHit && cn != null) {
                // can't use a cached MimeMessage because of TNEF conversion
                mCache.remove(key);  mTotalSize -= cn.mSize;
            }
        }

        if (!cacheHit) {
            try {
                // wasn't cached; fetch, cache, and return it
                int size = (int) item.getSize();
                InputStream is = fetchFromStore(item);
                cn = new CacheNode(size, ByteUtil.getContent(is, size));

                // cache the message content (if it'll fit)
                cacheItem(key, cn);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException while retrieving content for item " + item.getId(), e);
            }
        } else {
            if (ZimbraLog.cache.isDebugEnabled())
                ZimbraLog.cache.debug("msgcache: found raw content in cache: " + item.getDigest());
        }
        
        if (ZimbraLog.perf.isDebugEnabled()) {
            ZimbraPerf.writeStats(STATS_FILE, item.getId(), cacheHit ? 1 : 0);
        }
        
        return cn.mContent;
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
        return new ByteArrayInputStream(getItemContent(item));
    }

    /** Returns a JavaMail {@link javax.mail.internet.MimeMessage}
     *  encapsulating the message content.  If possible, TNEF attachments
     *  are expanded and their components are presented as standard MIME
     *  attachments.  If TNEF decoding fails, the MimeMessage wraps the raw
     *  message content.
     * 
     * @return A MimeMessage wrapping the RFC822 content of the Message.
     * @throws ServiceException when errors occur opening, reading,
     *                          uncompressing, or parsing the message file,
     *                          or when the file does not exist.
     * @see #getRawContent()
     * @see #getItemContent()
     * @see TnefConverter */
    static MimeMessage getMimeMessage(Message msg) throws ServiceException {
        String key = msg.getDigest();
        boolean cacheHit = false;
        CacheNode cn = null, cnOrig = null;
        synchronized (mCache) {
            cn = cnOrig = (CacheNode) mCache.get(key);
            cacheHit = cn != null && cn.mMessage != null;

            if (!cacheHit && cn != null) {
                // replacing the cached byte array with a MimeMessage
                mCache.remove(key);  mTotalSize -= cn.mSize;
            }
        }

        if (!cacheHit) {
            try {
                // wasn't cached; fetch the content and create the MimeMessage
                int size = (int) msg.getSize();
                InputStream is = (cnOrig == null ? fetchFromStore(msg) : new ByteArrayInputStream(cnOrig.mContent));
                cn = new CacheNode(size, new MimeMessage(JMSession.getSession(), is));
                is.close();

                try {
                    // handle TNEF conversion here...
                    Mime.accept(new TnefConverter(), cn.mMessage);
                } catch (Exception e) {
                    // if the conversion bombs for any reason, revert to the original
                    if (ZimbraLog.mailbox.isInfoEnabled())
                        ZimbraLog.mailbox.info("unable to convert TNEF attachment for message " + msg.getId(), e);
                    is = (cnOrig == null ? fetchFromStore(msg) : new ByteArrayInputStream(cnOrig.mContent));
                    cn = new CacheNode(size, new MimeMessage(JMSession.getSession(), is));
                    is.close();
                }

                // cache the MimeMessage (if it'll fit)
                cacheItem(key, cn);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException while retrieving content for item " + msg.getId(), e);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("MessagingException while creating MimeMessage for item " + msg.getId(), e);
            }
        } else {
            if (ZimbraLog.cache.isDebugEnabled())
                ZimbraLog.cache.debug("msgcache: found mime message in cache: " + msg.getDigest());
        }

        if (ZimbraLog.perf.isDebugEnabled()) {
            ZimbraPerf.writeStats(STATS_FILE, msg.getId(), cacheHit ? "1" : "0");
        }
        
        return cn.mMessage;
    }

    private static InputStream fetchFromStore(MailItem item) throws ServiceException, IOException {
        MailboxBlob msgBlob = item.getBlob();
        if (msgBlob == null)
            throw ServiceException.FAILURE("missing blob for id: " + item.getId() + ", change: " + item.getModifiedSequence(), null);
        return StoreManager.getInstance().getContent(msgBlob);
    }

    private static void cacheItem(String key, CacheNode cn) {
        if (cn.mSize >= mMaxCacheSize)
            return;

        synchronized (mCache) {
            if (ZimbraLog.cache.isDebugEnabled())
                ZimbraLog.cache.debug("msgcache: caching " + (cn.mContent != null ? "raw" : "mime") + " message: " + key);
            mCache.put(key, cn);
            mTotalSize += cn.mSize;

            // trim the cache if needed
            if (mTotalSize > mMaxCacheSize)
                for (Iterator it = mCache.values().iterator(); mTotalSize > DEFAULT_CACHE_SIZE && it.hasNext(); ) {
                    CacheNode cnPurge = (CacheNode) it.next();
                    it.remove();
                    mTotalSize -= cnPurge.mSize;
                }
        }
    }
}
