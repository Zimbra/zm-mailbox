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

public abstract class StoreManager {

    private static StoreManager sInstance;
    private static Integer diskStreamingThreshold;

    public static StoreManager getInstance () {
        if(ImapDaemon.isRunningImapInsideMailboxd()) {
            return getInstance(LC.zimbra_class_store.value());
        } else {
            return getInstance(LC.imapd_class_store.value());
        }
    }

    public static StoreManager getInstance(String className) {
        if (sInstance == null) {
            synchronized (StoreManager.class) {
                if (sInstance != null) {
                    return sInstance;
                }

                try {
                    if (className != null && !className.equals("")) {
                        try {
                            sInstance = (StoreManager) Class.forName(className).newInstance();
                        } catch (ClassNotFoundException e) {
                            sInstance = (StoreManager) ExtensionUtil.findClass(className).newInstance();
                        }
                    } else {
                        sInstance = new FileBlobStore();
                    }
                } catch (Throwable t) {
                    Zimbra.halt("unable to initialize blob store", t);
                }
            }
        }
        return sInstance;
    }

    /**
     * Used for unit testing.
     */
    public static void setInstance(StoreManager instance) {
        ZimbraLog.store.info("Setting StoreManager to " + instance.getClass().getName());
        sInstance = instance;
    }

    public static int getDiskStreamingThreshold() throws ServiceException {
        if (diskStreamingThreshold == null)
            loadSettings();
        return diskStreamingThreshold;
    }

    public static void loadSettings() throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        diskStreamingThreshold = server.getMailDiskStreamingThreshold();
    }

    /**
     * Starts the blob store.
     */
    public abstract void startup() throws IOException, ServiceException;

    /**
     * Shuts down the blob store.
     */
    public abstract void shutdown();

    public enum StoreFeature {
        /** The store has the ability to delete all {@code MailboxBlob}s
         *  associated with a given {@code Mailbox}, <u>without</u> having
         *  a list of those blobs provided to it. */
        BULK_DELETE,
        /** The store is reachable from any {@code mailboxd} host.  When
         *  moving mailboxes between hosts, the store should be left untouched,
         *  as there is no need to move the blobs along with the metadata. */
        CENTRALIZED,
        /**
         * The store supports resumable upload
         */
        RESUMABLE_UPLOAD,
        /**
         * The store supports deduping based on blob content; aka SIS create.
         * If two users upload the same file, only one copy is stored.
         * The remote store must track reference count internally
         * and delete the actual file only when ref-count reaches 0
         */
        SINGLE_INSTANCE_SERVER_CREATE,
        /**
         * The store is a custom store that does not support standard Zimbra actions.
         * It requires to be handled using zxsuite command
         */
        CUSTOM_STORE_API,
    };

    /**
     * Returns whether the store supports a given {@link StoreFeature}.
     */
    public abstract boolean supports(StoreFeature feature);
    public abstract boolean supports(StoreFeature feature, String locator);

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
    public abstract BlobBuilder getBlobBuilder() throws IOException, ServiceException;

    /**
     * Store a blob in incoming directory.  Blob will be compressed if volume supports compression
     * and blob size is over the compression threshold.
     * @param data
     * @param sizeHint used for determining whether data should be compressed
     * @param digest
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public Blob storeIncoming(InputStream data)
    throws IOException, ServiceException {
        return storeIncoming(data, false);
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
    public abstract Blob storeIncoming(InputStream data, boolean storeAsIs)
    throws IOException, ServiceException;

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
    public abstract StagedBlob stage(InputStream data, long actualSize, Mailbox mbox)
    throws IOException, ServiceException;

    /**
     * Stage an incoming <code>InputStream</code> to an
     * appropriate place for subsequent storage in a <code>Mailbox</code> via
     * {@link #link(StagedBlob, Mailbox, int, int)} or {@link #renameTo}.
     *
     * @param data the data stream
     * @param callback callback, or {@code null}
     * @param mbox the mailbox
     */
    public StagedBlob stage(InputStream data, Mailbox mbox)
    throws IOException, ServiceException {
        return stage(data, -1, mbox);
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
    public abstract StagedBlob stage(Blob blob, Mailbox mbox) throws IOException, ServiceException;

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
    public abstract MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, long destRevision)
    throws IOException, ServiceException;

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
    public  MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, long destRevision)
    throws IOException, ServiceException {

        throw ServiceException.UNSUPPORTED();
    }

    public abstract MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
        throws IOException, ServiceException;

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
    public abstract MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, long destRevision)
    throws IOException, ServiceException;

    /**
     * Deletes a blob from incoming directory.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blobFile
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(Blob blob) throws IOException;

    /**
     * Deletes a blob from incoming directory.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blobFile
     * @return true if blob was actually deleted
     */
    public boolean quietDelete(Blob blob) {
        if (blob == null) {
            return true;
        }

        try {
            return delete(blob);
        } catch (IOException ioe) {
            ZimbraLog.store.warn("could not delete blob " + blob.getPath());
            return false;
        }
    }

    /**
     * Deletes a blob staged to the target mailbox.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param staged
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(StagedBlob staged) throws IOException;

    /**
     * Deletes a blob staged to the target mailbox.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param staged
     * @return true if blob was actually deleted
     */
    public boolean quietDelete(StagedBlob staged) {
        if (staged == null) {
            return true;
        }

        try {
            return delete(staged);
        } catch (IOException ioe) {
            ZimbraLog.store.warn("could not delete staged blob " + staged);
            return false;
        }
    }

    /**
     * Deletes a blob from store.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param mblob
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(MailboxBlob mblob) throws IOException;

    /**
     * Deletes a blob from store.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param mblob
     * @return true if blob was actually deleted
     */
    public boolean quietDelete(MailboxBlob mblob) {
        if (mblob == null) {
            return true;
        }

        try {
            return delete(mblob);
        } catch (IOException ioe) {
            ZimbraLog.store.warn("could not delete blob " + mblob);
            return false;
        }
    }

    /**
     * Find the MailboxBlob in mailbox mbox with matching item ID.
     * @param mbox
     * @param itemId mail_item.id value for item
     * @param revision mail_item.mod_content value for item
     * @return the <code>MailboxBlob</code>, or <code>null</code> if the file
     * does not exist
     *
     * @throws ServiceException
     */
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, long revision, String locator)
    throws ServiceException {
        return getMailboxBlob(mbox, itemId, revision, locator, true);
    }

    public abstract  MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator)
        throws ServiceException ;


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
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, long revision, String locator, boolean validate)
    throws ServiceException {

        throw ServiceException.UNSUPPORTED();
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
    public abstract MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, boolean validate)
    throws ServiceException;

    /**
     * Find the stored MailboxBlob for the given Mailitem.
     * @param item An item already deposited in its Mailbox.
     * @return the <code>MailboxBlob</code>, or <code>null</code> if the file
     * does not exist
     *
     * @throws ServiceException
     */
    public MailboxBlob getMailboxBlob(MailItem item) throws ServiceException {
        MailboxBlob mblob = getMailboxBlob(item.getMailbox(), item.getId(), item.getSavedSequenceLong(), item.getLocator());
        if (mblob != null) {
            mblob.setDigest(item.getDigest()).setSize(item.getSize());
        }
        return mblob;
    }

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param mboxBlob
     * @return
     * @throws IOException
     */
    public abstract InputStream getContent(MailboxBlob mboxBlob) throws IOException;

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param blob
     * @return
     * @throws IOException
     */
    public abstract InputStream getContent(Blob blob) throws IOException;

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
    public abstract boolean deleteStore(Mailbox mbox, Iterable<MailboxBlob.MailboxBlobInfo> blobs)
    throws IOException, ServiceException;

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
