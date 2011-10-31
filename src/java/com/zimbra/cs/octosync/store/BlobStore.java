package com.zimbra.cs.octosync.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.zimbra.common.service.ServiceException;


/**
 * Abstract interface representing generic storage of blobs.
 *
 * This interface is currently used for storing Octopus patches only. It provides
 * simplest layer of abstraction with the functionality required for storing, retrieving and
 * deleting patch files. It is to be implemented using existing blob storage
 * facilities such as StoreManager. In the future, as we modernize the storage back-end
 * this API may evolve into widely used general purpose API. For the time being, it
 * just remains the glue between patch storage and the existing storage facilities.
 *
 * The key idea in this API is that incoming blobs are distinguished from stored blobs.
 * Incoming blob is a blob that is being received, and is possibly not yet complete.
 * It can be appended to as the data is being received. This supports resumable uploads.
 * Once complete, it can be turned into stored blob. An incoming blob that never got stored
 * should be automatically purged.
 *
 * Both incoming and stored blobs are identified by user supplied identifiers (strings).
 * Namespaces for both types are separate. Additionally stored blobs support versioning.
 *
 * @author grzes
 *
 */
public abstract class BlobStore
{
    /**
     * Represents incoming blob, i.e. blob that may not be yet completely received.
     */
    public abstract class IncomingBlob
    {
        /**
         * Returns the incoming blob id.
         *
         * @return the id
         */
        public abstract String getId();

        /**
         * Returns the current size of the incoming blob.
         *
         * @return the size
         */
        //public abstract long getCurrentSize();

        /**
         * Gets the expected size, if set. 0 if not set/unknown.
         *
         * @return the expected size
         */
        //public abstract long getExpectedSize();

        /**
         * Sets the expected size.
         *
         * @param value The expected size. Must be greater than 0.
         */

        //public abstract void setExpectedSize(long value);

        /**
         * Gets the output stream for the incoming blob. The
         * stream is used to write to the end of the incoming blob.
         *
         * @return the output stream
         */
        public abstract OutputStream getAppendingOutputStream() throws IOException;

        /**
         * Gets the input stream. The return stream can be used
         * to read the already written data.
         *
         * The stream must not return data that was written to the incoming blob
         * after the InputStream instance was obtained via this call. EOF
         * should be reported upon attempt to read byte past the current
         * size of the blob at the time of this call.
         *
         * @return the input stream
         */
        public abstract InputStream getInputStream() throws IOException;

        /**
         * Returns the user settable context data. Must not be used or
         * otherwise interpreted by the implementations.
         *
         * Future note: should instances of IncomingBlob be serialized, the Object
         * stored here must be serializable.
         *
         * @return the context
         */
        public abstract Object getContext();

        /**
         * Returns the previously set user context data.
         *
         * @param value the new context
         */
        public abstract void setContext(Object value);
    }

    /**
     * Represents blob that was stored after all data
     * for the incoming blob were received.
     *
     * The details of how the blob is stored, possible aspects such as
     * retention period are implementation dependent.
     *
     */
    public abstract class StoredBlob
    {
        private String id;

        /**
         * Gets the stored blob identifier.
         *
         * @return the id
         */
        public String getId()
        {
            return id;
        }

        /**
         * Gets the input stream.
         *
         * @return the input stream
         * @throws IOException Signals that an I/O exception has occurred.
         */
        public abstract InputStream getInputStream() throws IOException;

        /**
         * Gets the user context. If the context was set for the
         * incoming blob used to create stored blob, the same object must be
         * preserved and returned here.
         *
         * @return the context
         */
        public abstract Object getContext();

        /**
         * Sets the user context.
         *
         * @param value the new context
         */
        public abstract void setContext(Object value);

        protected StoredBlob(String id)
        {
            this.id = id;
        }
    }

    /**
     * Creates an incoming blob.
     *
     * @param id User assigned id. The name space for incoming blobs is separate from
     *      stored blob id namespace.
     * @param ctx User context. Can be null.
     *
     * @return Instance of IncomingBlob
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException the service exception
     */
    public abstract IncomingBlob createIncoming(String id, Object ctx) throws IOException, ServiceException;

    /**
     * Retrieves the incoming blob by the id.
     *
     * @param id The id passed to createIncoming() when the blob was created.
     *
     * @return The IncomingBlob instance or null if not found.
     */
    public abstract IncomingBlob getIncoming(String id);

    /**
     * Rejects incoming blob. Called when an incoming blob cannot be accepted, e.g.
     * if complete data were never received.
     *
     * @param ib The IncomingBlob instance
     */
    public abstract void deleteIncoming(IncomingBlob ib);

    /**
     * Stores an incoming blob.
     *
     * @param ib The IncomingBlob instance to accept.
     * @param id The id for the stored blob.
     * @param version the version The version of the blob. Must be 1 or greater. Must
     *      not be equal to a version that already exists.
     *
     * @return StoredBlob instance
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException
     */
    public abstract StoredBlob store(IncomingBlob ib, String id, int version) throws IOException, ServiceException;

    /**
     * Gets the specified version of a StoredBlob.
     *
     * @param id Stored blob identifier.
     * @param version the version
     * @return the stored blob
     */
    public abstract StoredBlob get(String id, int version);

    /**
     * Retrieves the latest version of a StoredBlob.
     *
     * @param id Stored blob identifier.
     * @return the stored blob
     */
    public abstract StoredBlob get(String id);

    /**
     * Delete all version of a specified stored blob.
     *
     * @param sb Stored blob to delete. Can be null in which case it is no-op.
     */
    public abstract void delete(StoredBlob sb);

    /**
     * Delete specific version of a stored blob.
     *
     * @param sb Stored blob to delete. Can be null in which case it is no-op.
     * @param version The version to delete. No-op if the version does not exist.
     */
    public abstract void delete(StoredBlob sb, int version);
}
