package com.zimbra.cs.store.external;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbBlobConsistency;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.BlobConsistencyChecker;

/**
 * Consistency checker for ExternalStoreManager implementations. The StoreManager may optionally override getAllBlobPaths(Mailbox mbox)
 * which will be used to locate unexpected blobs/orphaned blobs. In cases where the StoreManager has no direct associated between
 * Mailbox and stored blobs a list of all used blobs can be obtained and used for manual/scripted consistency checking
 *
 */
public class ExternalBlobConsistencyChecker extends BlobConsistencyChecker {
    private static final int CHUNK_SIZE = 500;

    private List<String> unexpectedBlobPaths = new ArrayList<String>();

    @Override
    public Results check(Collection<Short> volumeIds, int mboxId, boolean checkSize, boolean reportUsedBlobs) throws ServiceException {
        mailboxId = mboxId;
        this.checkSize = checkSize;
        this.reportUsedBlobs = reportUsedBlobs;
        results = new Results();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
        DbConnection conn = null;
        assert(StoreManager.getInstance() instanceof ExternalStoreManager);
        ExternalStoreManager sm = (ExternalStoreManager) StoreManager.getInstance();
        try {
            unexpectedBlobPaths = sm.getAllBlobPaths(mbox);
        } catch (IOException ioe) {
            log.error("IOException getting remote blob list", ioe);
        }
        try {
            conn = DbPool.getConnection();
            int mailboxMaxId = DbBlobConsistency.getMaxId(conn, mbox);
            int minId = 0;
            int maxId = CHUNK_SIZE;
            while (minId <= mailboxMaxId) {
                for (BlobInfo blobInfo : DbBlobConsistency.getExternalMailItemBlobInfo(conn, mbox, minId, maxId)) {
                    checkExternalBlob(mbox, checkSize, blobInfo, sm);
                }
                for (BlobInfo blobInfo : DbBlobConsistency.getExternalMailItemDumpsterBlobInfo(conn, mbox, minId, maxId)) {
                    checkExternalBlob(mbox, checkSize, blobInfo, sm);
                }
                for (BlobInfo blobInfo : DbBlobConsistency.getExternalRevisionBlobInfo(conn, mbox, minId, maxId)) {
                    checkExternalBlob(mbox, checkSize, blobInfo, sm);
                }
                for (BlobInfo blobInfo : DbBlobConsistency.getExternalRevisionDumpsterBlobInfo(conn, mbox, minId, maxId)) {
                    checkExternalBlob(mbox, checkSize, blobInfo, sm);
                }
                minId = maxId + 1;
                maxId += CHUNK_SIZE;
            }
        } finally {
            DbPool.quietClose(conn);
        }
        for (String unexpected : unexpectedBlobPaths) {
            BlobInfo bi = new BlobInfo();
            bi.external = true;
            bi.locator = unexpected;
            bi.path = unexpected;
            results.unexpectedBlobs.put(0, bi);
            try {
                Blob blob = sm.getLocalBlob(mbox, unexpected, false);
                bi.fileSize = blob.getFile().length();
            } catch (IOException ioe) {
                //log this?
                bi.fileSize = 0L;
                bi.fetchException = ioe;
            }
        }
        return results;
    }

    private void checkExternalBlob(Mailbox mbox, boolean checkSize, BlobInfo blobInfo, ExternalStoreManager sm) throws ServiceException {
        MailboxBlob mblob = sm.getMailboxBlob(mbox, blobInfo.itemId, blobInfo.version, blobInfo.path);
        if (mblob == null) {
            results.missingBlobs.put(blobInfo.itemId, blobInfo);
        } else {
            try {
                unexpectedBlobPaths.remove(mblob.getLocator());
                Blob blob = sm.getLocalBlob(mbox, mblob.getLocator(), false);
                if (blob == null) {
                    results.missingBlobs.put(blobInfo.itemId, blobInfo);
                } else {
                    //blob exists for the locator
                    blobInfo.fileModContent = blobInfo.modContent;
                    if (reportUsedBlobs) {
                        results.usedBlobs.put(blobInfo.itemId, blobInfo);
                    }
                    if (checkSize) {
                        blobInfo.fileSize = blob.getFile().length();
                        blobInfo.fileDataSize = getDataSize(blob.getFile(), blobInfo.dbSize);
                        if (blobInfo.dbSize != blobInfo.fileDataSize) {
                            results.incorrectSize.put(blobInfo.itemId, blobInfo);
                        }
                    }
                }
            } catch (IOException ioe) {
                blobInfo.fetchException = ioe;
                results.missingBlobs.put(blobInfo.itemId, blobInfo);
            }
        }
    }
}
