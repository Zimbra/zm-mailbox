package com.zimbra.cs.octosync.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.octosync.PatchManifest;
import com.zimbra.cs.octosync.store.BlobStore.IncomingBlob;
import com.zimbra.cs.octosync.store.BlobStore.StoredBlob;

/**
 * Encapsulates functionality for storing and retrieving patches and their manifests.
 *
 * @author grzes
 */
public class PatchStore
{
    private BlobStore blobStore;

    /**
     * Represents incoming patch.
     */
    public class IncomingPatch
    {
        private IncomingBlob patchBlob;
        private String accountId;
        private PatchManifest manifest;

        /**
         * Gets the account id.
         *
         * @return the account id
         */
        public String getAccountId()
        {
            return accountId;
        }

        /**
         * Instantiates a new incoming patch.
         *
         * @param blob the blob
         * @param accountId the account id
         */
        public IncomingPatch(IncomingBlob blob, String accountId)
        {
            this.patchBlob = blob;
            this.accountId = accountId;
            this.manifest = new PatchManifest();
        }

        public PatchManifest getManifest()
        {
            return manifest;
        }

        public OutputStream getOutputStream() throws IOException
        {
            return patchBlob.getAppendingOutputStream();
        }

        public InputStream getInputStream() throws IOException
        {
            return patchBlob.getInputStream();
        }
    }

    /**
     * Represents a stored patch.
     */
    public class StoredPatch
    {

        /** The patch blob. */
        private StoredBlob patchBlob;

        /** The manifest blob. */
        private StoredBlob manifestBlob;

        /** The account id. */
        private String accountId;

        /**
         * Gets the account id.
         *
         * @return the account id
         */
        public String getAccountId()
        {
            return accountId;
        }

        private StoredPatch(StoredBlob patchBlob, StoredBlob manifestBlob, String accountId)
        {
            this.patchBlob = patchBlob;
            this.manifestBlob = manifestBlob;
            this.accountId = accountId;
        }

        /**
         * Gets the input stream.
         *
         * @return the input stream
         * @throws IOException Signals that an I/O exception has occurred.
         */
        public InputStream getInputStream() throws IOException
        {
            return patchBlob.getInputStream();
        }

        /**
         * Gets the manifest input stream.
         *
         * @return the manifest input stream
         * @throws IOException Signals that an I/O exception has occurred.
         */
        public InputStream getManifestInputStream() throws IOException
        {
            return manifestBlob.getInputStream();
        }
    }


    /**
     * Instantiates a new patch store.
     *
     * @param blobStore the blob store
     */
    public PatchStore(BlobStore blobStore)
    {
        this.blobStore = blobStore;
    }

    /**
     * Creates the incoming patch.
     *
     * @param accountId The if of the account to associate the patch with.
     * @param resumeId The user-created resume id to track this patch
     *      (note, we may go with store generated ids TBD)
     *
     * @return IncomingPatch instance.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException the service exception
     */
    public IncomingPatch createIncomingPatch(String accountId, String resumeId) throws IOException, ServiceException
    {
        IncomingBlob ib = blobStore.createIncoming(getIncomingPatchId(accountId, resumeId), null);

        IncomingPatch ip = new IncomingPatch(ib, accountId);
        ib.setContext(ip);

        return ip;
    }

    /**
     * Retrieves the incoming patch.
     *
     * @param accountId The id of the account for the patch.
     * @param resumeId The resume id of the patch.
     *
     * @return IncomingPatch
     */
    public IncomingPatch getIncomingPatch(String accountId, String resumeId)
    {
        return (IncomingPatch)blobStore.getIncoming(getIncomingPatchId(accountId, resumeId)).getContext();
    }


    /**
     * Accepts patch. Turns an IncomingPatch into a StoredPatch.
     *
     * @param ip The IncomingPatch instance to accept.
     * @param fileId The id of the file the patch creates.
     * @param version the version The version number of the file the patch creates.
     * @param manifest The patch manifest.
     *
     * @return StoredPatch instance.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException the service exception
     */
    public StoredPatch acceptPatch(IncomingPatch ip, int fileId, int version)
        throws IOException, ServiceException
    {
        if (version > 1) {
            deletePatch(ip.getAccountId(), fileId);
        }

        // id for incoming manifest blob is temporary, we don't keep
        // incoming blob around since the entire manifest is already in hand
        IncomingBlob manifestBlob = blobStore.createIncoming(ip.patchBlob.getId() + "M", null);

        try {
            ip.getManifest().writeTo(manifestBlob.getAppendingOutputStream());
        } catch (IOException e) {
            blobStore.deleteIncoming(manifestBlob);
            throw e;
        }

        // ok, we got manifest written to a separate incoming blob here, now let's store both

        StoredBlob psb = blobStore.store(ip.patchBlob, getStoredPatchId(ip.getAccountId(), fileId, false), version);

        StoredBlob msb = null;

        try {
            msb = blobStore.store(manifestBlob, getStoredPatchId(ip.getAccountId(), fileId, true), version);
        } catch (IOException e) {
            blobStore.delete(psb, version);
            throw e;
        }

        StoredPatch sp = new StoredPatch(psb, msb, ip.getAccountId());
        psb.setContext(sp);

        return sp;
    }

    /**
     * Reject patch.
     *
     * @param ip the ip
     */
    public void rejectPatch(IncomingPatch ip)
    {
        blobStore.deleteIncoming(ip.patchBlob);
    }

    /**
     * Lookup patch.
     *
     * @param accountId the account id
     * @param fileId the file id
     * @param version the version
     * @return the stored patch
     */
    public StoredPatch lookupPatch(String accountId, int fileId, int version)
    {
        StoredBlob psb = blobStore.get(getStoredPatchId(accountId, fileId, false), version);

        if (psb == null) {
            return null;
        }

        return (StoredPatch)psb.getContext();
    }

    /**
     * Delete patch.
     *
     * @param accountId the account id
     * @param fileId the file id
     */
    public void deletePatch(String accountId, int fileId)
    {
        try {
            StoredBlob psb = blobStore.get(getStoredPatchId(accountId, fileId, false));
            blobStore.delete(psb);
        } finally {
            StoredBlob msb = blobStore.get(getStoredPatchId(accountId, fileId, true));
            blobStore.delete(msb);
        }
    }

    private String getIncomingPatchId(String accountId, String resumeId)
    {
        return accountId + ":" + resumeId;
    }

    private String getStoredPatchId(String accountId, int fileId, boolean manifest)
    {
        return accountId + ':' + fileId + (manifest ? 'M' : 'P');
    }

}
