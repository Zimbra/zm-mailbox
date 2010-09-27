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
package com.zimbra.cs.store;

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.util.Zimbra;

public abstract class StoreManager {

    private static StoreManager sInstance;

    public static StoreManager getInstance() {
        if (sInstance == null) {
            synchronized (StoreManager.class) {
                if (sInstance != null)
                    return sInstance;

                String className = LC.zimbra_class_store.value();
                try {
                    if (className != null && !className.equals(""))
                        sInstance = (StoreManager) Class.forName(className).newInstance();
                    else
                        sInstance = new FileBlobStore();
                } catch (Throwable t) {
                    Zimbra.halt("unable to initialize blob store", t);
                }
            }
        }
        return sInstance;
    }

    /**
     * Starts the blob store.
     */
    public abstract void startup() throws IOException, ServiceException;

    /**
     * Shuts down the blob store.
     */
    public abstract void shutdown();

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
    public Blob storeIncoming(InputStream data, StorageCallback callback)
    throws IOException, ServiceException {
        return storeIncoming(data, callback, false);
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
    public abstract Blob storeIncoming(InputStream data, StorageCallback callback, boolean storeAsIs)
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
    public abstract StagedBlob stage(InputStream data, long actualSize, StorageCallback callback, Mailbox mbox)
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
    public StagedBlob stage(InputStream data, StorageCallback callback, Mailbox mbox)
    throws IOException, ServiceException {
        return stage(data, -1, callback, mbox);
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
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the copied blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException;

    /**
     * Create a link in destMbox mailbox with message ID of destMsgId that
     * points to srcBlob.
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the linked blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException;

    /**
     * Create a link in destMbox mailbox with message ID of destMsgId that
     * points to srcBlob.
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the linked blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob link(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException;

    /**
     * Rename a blob to a blob in mailbox directory.
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the renamed blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
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
        if (blob == null)
            return true;

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
        if (staged == null)
            return true;

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
        if (mblob == null)
            return true;

        try {
            return delete(mblob);
        } catch (IOException ioe) {
            ZimbraLog.store.warn("could not delete blob " + mblob);
            return false;
        }
    }

    /**
     * Find the MailboxBlob in mailbox mbox with matching message ID.
     * @param mbox
     * @param msgId mail_item.id value for message
     * @param revision mail_item.mod_content value for message
     * @return the <code>MailboxBlob</code>, or <code>null</code> if the file
     * does not exist
     * 
     * @throws ServiceException
     */
    public abstract MailboxBlob getMailboxBlob(Mailbox mbox, int msgId, int revision, String locator)
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
        MailboxBlob mblob = getMailboxBlob(item.getMailbox(), item.getId(), item.getSavedSequence(), item.getLocator());
        if (mblob != null)
            mblob.setDigest(item.getDigest()).setSize(item.getSize());
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
     * @return true if store was actually deleted
     * @throws IOException
     * @throws ServiceException
     */
    public abstract boolean deleteStore(Mailbox mbox)
    throws IOException, ServiceException;
}
