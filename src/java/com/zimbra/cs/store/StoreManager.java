/*
 * Created on 2004. 10. 12.
 */
package com.zimbra.cs.store;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Liquid;

/**
 * @author jhahm
 */
public abstract class StoreManager {

    private static Log mLog = LogFactory.getLog(StoreManager.class);

	private static StoreManager sInstance;
	static {
        try {
            sInstance = new FileBlobStore();
        } catch (Throwable t) {
        	Liquid.halt("Unable to initialize blob store", t);
        }
	}

	public static StoreManager getInstance() {
		return sInstance;
	}

    /**
     * Shutdown the blob store.
     */
    public abstract void shutdown();

    /**
     * Store a message in incoming directory.
     * @param data
     * @param digest
     * @param path If null, blob store assigns one.
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public abstract Blob storeIncoming(byte[] data, String digest,
                                       String path, short volumeId)
    throws IOException, ServiceException;

    /**
	 * Create a link in destMbox mailbox with message ID of destMsgId that
	 * points to srcBlob.
	 * @param src
	 * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @param destVolumeId volume for destination blob
	 * @return MailboxBlob object representing the linked blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob link(Blob src, Mailbox destMbox,
                                     int destMsgId, int destRevision,
                                     short destVolumeId)
	throws IOException, ServiceException;

    /**
     * Rename a blob to a blob in mailbox directory.
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @param destVolumeId volume for destination blob
     * @return MailboxBlob object representing the renamed blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob renameTo(Blob src, Mailbox destMbox,
                                         int destMsgId, int destRevision,
                                         short destVolumeId)
    throws IOException, ServiceException;

    /**
     * Deletes a blob from store.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blob
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(MailboxBlob blob) throws IOException;

    /**
     * Deletes a blob from incoming directory.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blobFile
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(Blob blob) throws IOException;

    /**
     * Find the MailboxBlob in mailbox mbox with matching message ID.
     * @param mbox
     * @param msgId mail_item.id value for message
     * @param revision mail_item.mod_content value for message
     * @param volumeId the volume the blob is on
     * @return
     * @throws ServiceException
     */
    public abstract MailboxBlob getMailboxBlob(Mailbox mbox,
                                               int msgId, int revision,
                                               short volumeId)
    throws ServiceException;

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param mboxBlob
     * @return
     * @throws IOException
     */
    public abstract InputStream getContent(MailboxBlob mboxBlob)
    throws IOException;

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
     * @param volume 
     * @return true if store was actually deleted
     * @throws IOException
     * @throws ServiceException
     */
    public abstract boolean deleteStore(Mailbox mbox, int volume)
    throws IOException, ServiceException;
}
