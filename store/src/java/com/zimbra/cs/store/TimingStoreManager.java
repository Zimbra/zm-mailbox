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
package com.zimbra.cs.store;

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.imap.ImapDaemon;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.stats.ZimbraPerf;


/**
 * TimingStoreManager is a StoreManager subclass which times the operation
 * of the actual StoreManager that is instantiated within.
 */
public class TimingStoreManager extends StoreManager {

    private StoreManager sInstance;

    public static TimingStoreManager getInstance(String className) {
        StoreManager manager = null;

        if (className != null && !className.equals("")) {
            try {
                try {
                    manager = (StoreManager) Class.forName(className).newInstance();
                } catch (ClassNotFoundException e) {

                    manager = (StoreManager) ExtensionUtil.findClass(className).newInstance();
                }
            }
            catch (Throwable t) {
                Zimbra.halt("unable to initialize blob store", t);
            }
        } else {
            manager = new FileBlobStore();
        }

        return new TimingStoreManager(manager);
    }

    public TimingStoreManager(StoreManager manager) {
        sInstance = (StoreManager) manager;
    }

    /**
     * Starts the blob store.
     */
    public void startup() throws IOException, ServiceException {
        sInstance.startup();
    }

    /**
     * Shuts down the blob store.
     */
    public void shutdown() {
        sInstance.shutdown();
    }

    /**
     * Returns whether the store supports a given {@link StoreFeature}.
     */
    public boolean supports(StoreManager.StoreFeature feature) {
        return sInstance.supports(feature);
    }
    public boolean supports(StoreFeature feature, String locator) { return sInstance.supports(feature, locator); }

    /**
     * Returns a 'BlobBuilder' which can be used to store a blob in incoming
     * directory asynchronously one chunk at a time. Blob will be compressed
     * if volume supports compression and blob size is over the compression
     * threshold.
     *
     * @return the BlobBuilder to use to construct the Blob
     * @throws IOException if an I/O error occurred
     * @throws ServiceException if a service exception occurred
     */
    public BlobBuilder getBlobBuilder() throws IOException, ServiceException {
        return sInstance.getBlobBuilder();
    }

    /**
     * Store a blob in incoming directory.
     * @param data
     * @param callback
     * @param storeAsIs if true, store the blob as is even if volume supports compression
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public Blob storeIncoming(InputStream data, boolean storeAsIs)
    throws IOException, ServiceException {
        ZimbraPerf.COUNTER_STORE_PUT.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_PUT.start();
        try {
            return sInstance.storeIncoming(data, storeAsIs);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_GET.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.storeIncoming - elapsed %d ms", elapsed);
        }
    }

    /**
     * Stage an incoming <code>InputStream</code> to an
     * appropriate place for subsequent storage in a <code>Mailbox</code> via
     * {@link #link(StagedBlob, Mailbox, int, int)} or {@link #renameTo}.
     *
     * @param data the data stream
     * @param actualSize the content size, or {@code -1} if the content size is not available
     * @param callback callback, or {@code null}
     * @param mbox the mailbox
     */
    public StagedBlob stage(InputStream data, long actualSize, Mailbox mbox)
    throws IOException, ServiceException {
        ZimbraPerf.COUNTER_STORE_STAGE.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_STAGE.start();
        try {
            return sInstance.stage(data, actualSize, mbox);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_STAGE.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.stage(data, actualSize, mbox) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Stage an incoming <code>Blob</code> (see {@link #storeIncoming}) to an
     * appropriate place for subsequent storage in a <code>Mailbox</code> via
     * {@link #link(StagedBlob, Mailbox, int, int)} or {@link #renameTo}.
     * @param blob
     * @param mbox
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public StagedBlob stage(Blob blob, Mailbox mbox) throws IOException, ServiceException {
        ZimbraPerf.COUNTER_STORE_STAGE.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_STAGE.start();
        try {
            return sInstance.stage(blob, mbox);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_STAGE.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.stage(blob, mbox) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Create a copy in destMbox mailbox with message ID of destMsgId that
     * points to srcBlob.
     * Implementations may choose to use linking where appropriate (i.e. files on same filesystem)
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the copied blob
     * @throws IOException
     * @throws ServiceException
     */
    public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        ZimbraPerf.COUNTER_STORE_COPY.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_COPY.start();
        try {
            return sInstance.copy(src, destMbox, destMsgId, destRevision);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_COPY.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.copy(src, destMbox, destMsgId, destRevision) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Create a link in destMbox mailbox with message ID of destMsgId that
     * points to srcBlob.
     * If staging creates permanent blobs, this just needs to return mailbox blob with pointer to location of staged blob
     * If staging is done to temporary area such as our incoming directory this operation finishes it
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the linked blob
     * @throws IOException
     * @throws ServiceException
     */
    public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        ZimbraPerf.COUNTER_STORE_LINK.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_LINK.start();
        try {
            return sInstance.link(src, destMbox, destMsgId, destRevision);

        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_LINK.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.link(src, destMbox, destMsgId, destRevision) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Rename a blob to a blob in mailbox directory.
     * This effectively makes the StagedBlob permanent, implementations may not need to do anything if the stage operation creates permanent items
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the renamed blob
     * @throws IOException
     * @throws ServiceException
     */
    public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        ZimbraPerf.COUNTER_STORE_RENAME.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_PUT.start();
        try {
            return sInstance.renameTo(src, destMbox, destMsgId, destRevision);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_PUT.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.renameTo(src, destMbox, destMsgId, destRevision) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Deletes a blob from incoming directory.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blobFile
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public boolean delete(Blob blob) throws IOException {
        ZimbraPerf.COUNTER_STORE_DEL.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_DEL.start();
        try {
            return sInstance.delete(blob);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_DEL.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.delete(blob) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Deletes a blob staged to the target mailbox.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param staged
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public boolean delete(StagedBlob staged) throws IOException {
        ZimbraPerf.COUNTER_STORE_DEL.increment(1);
        long start = ZimbraPerf.STOPWATCH_STORE_DEL.start();
        try {
            return sInstance.delete(staged);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_DEL.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.delete(staged) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Deletes a blob from store.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param mblob
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public boolean delete(MailboxBlob mblob) throws IOException {
        long start = ZimbraPerf.STOPWATCH_STORE_DEL.start();
        try {
            return sInstance.delete(mblob);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_DEL.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.delete(mblob) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Find the MailboxBlob in int mailboxId, String accountId with matching item ID.
     * @param mbox
     * @param itemId mail_item.id value for item
     * @param revision mail_item.mod_content value for item
     * @return the <code>MailboxBlob</code>, or <code>null</code> if the file
     * does not exist
     *
     * @throws ServiceException
     */
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, boolean validate)
    throws ServiceException {
        long start = ZimbraPerf.STOPWATCH_STORE_GET.start();
        try {
            return sInstance.getMailboxBlob(mbox, itemId, revision, locator, validate);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_GET.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.getMailboxBlob(mbox, itemId, revision, locator, validate) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param mboxBlob
     * @return
     * @throws IOException
     */
    public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        long start = ZimbraPerf.STOPWATCH_STORE_GET.start();
        try {
            return sInstance.getContent(mboxBlob);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_GET.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.getContent(mboxBlob) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param blob
     * @return
     * @throws IOException
     */
    public InputStream getContent(Blob blob) throws IOException {
        long start = ZimbraPerf.STOPWATCH_STORE_GET.start();
        try {
            return sInstance.getContent(blob);
        } finally {
            long elapsed = ZimbraPerf.STOPWATCH_STORE_GET.stop(start);
            ZimbraLog.misc.debug("TimingStoreManager.getContent(blob) - elapsed %d ms", elapsed);
        }
    }

    /**
     * Deletes a user's entire store.  SHOULD BE CALLED CAREFULLY.  No going back.
     * @param mbox
     * @param blobs If the store returned {@code true} to {@link #supports(StoreFeature)}
     *              when passed {@link StoreFeature#BULK_DELETE}, {@code blobs} will be
     *              {@code null}.  If it returned {@code false}, {@code blobs} will be
     *              an {@code Iterable} that contains every blob in the {@code Mailbox}.
     * @return true if store was actually deleted
     * @throws IOException
     * @throws ServiceException
     */
    public boolean deleteStore(Mailbox mbox, Iterable<MailboxBlob.MailboxBlobInfo> blobs)
    throws IOException, ServiceException {
        // FIXME: Stats?
        return sInstance.deleteStore(mbox, blobs);
    }

    /**
     * Create an IncomingBlob instance
     * @param id
     * @return
     * @throws ServiceException
     * @throws IOException
     */
    public IncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException, ServiceException {
        return new BufferingIncomingBlob(id, getBlobBuilder(), ctxt);
    }
}
