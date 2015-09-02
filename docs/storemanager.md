# StoreManager
The Zimbra `StoreManager` provides persistent object storage for `MailItem` content; for example full `Message` MIME structure. By default, Zimbra uses a `FileBlobStore` implementation which writes files to local disk. It is also possible to create Java extensions that write files to a cloud data storage provider, a remote document store, or any other storage technology.


*Before we dive in, a few notes and disclaimers.*

The source code examples are provided for illustration purposes only. They are not meant to be used directly in a production environment.
Code examples have been trimmed for readability. Imports, package declarations, and other details not directly related to `StoreManager` have been omitted. The complete code is available in Zimbra source control.

##StoreManager workflows

###Writing a blob

1. When an item or content revision is created it is placed into the incoming directory by calling `storeIncoming()` and passing the raw input stream (e.g. the request body from a POST request or MIME message body from LTMP). This method returns a reference to the `Blob` object. The blob is **not** persisted at this point.

2. The `Blob` is then passed into the `stage()` method. This copies the content from the incoming directory to persistent storage if necessary, and returns a `StagedBlob`.

3. The `StagedBlob` is then finalized and associated with a `Mailbox` by calling either `link()` or `renameTo()`. This returns a `MailboxBlob` and the content is finally persisted. However the blob is **still not** fully associated with a `MailItem` in the metadata store (MariaDB) at this point.  A crash at this moment could result in an orphaned blob that may or may not be cleaned up by redolog replay during the post-crash restart.

4. The `Mailbox` continues the transaction and eventually commits the MariaDB transaction. At that point the content is fully associated with a `MailItem`.

####Deduplication during delivery

When a message is added to the system, it may be delivered to more than one user. To avoid storing multiple copies of the same data, the `Blob` is stored once and then referenced by other recipients. This is implemented by calling `link()` multiple times on the same `StagedBlob`. No other deduplication occurs automatically; for example if two users upload the same briefcase item they are treated as two different objects.

The `StoreManager` may or may not support deduplication internally; more specifically the `FileBlobStore` does support deduplication and `ExternalStoreManager` generally does not unless it supports remote reference counting and extends `SisStore`.

###Reading a blob

1. Obtain a `Mailbox` object.

2. Call `Mailbox.getItemById()` or similar to get a `MailItem` which has raw content; for example a `Message` or `Document`.

3. Call one of the following methods from MailItem
    * `MailItem.getContentStream()` - returns an `InputStream`. Preferred access method. As with all `InputStreams` it is vital to close the stream when done; ideally in a `finally` block.
    * `MailItem.getContent()` - returns a byte[].  Should only be used with small data. For example smaller than `ToXML.MAX_INLINE_MSG_SIZE`.
    * `MailItem.getBlob()` - Returns a `MailboxBlob` which references the content via `getLocalBlob().getInputStream()` and similar methods. Used in code that needs to pass along a blob but does not need to reference it directly; e.g. when forwarding an existing message.

###Deleting a blob

* When a `MailItem` is deleted, the underlying `Blob` is deleted by calling `StoreManager.quietDelete()`. However, note that this typically does not occur until the item is fully purged from the dumpster. The delete operation should not be confused with moving an item to trash or emptying the trash.

##Volumes

In the `FileBlobStore` implementation; one or more `Volume` entries are created representing various paths on the filesystem. When a new `Blob` is created, it occurs on the current primary message volume. If that volume becomes full an admin may add a new volume and cause messages received after that time to be written. Once a blob is written to a volume it stays there unless moved by other means, such as the Zimbra HSM extension.

At this time, `Volume` entries have no effect on `ExternalStoreManager` implementations.

##Utilities

*Run the command for current usage*

`zmblobchk` - Checks for missing blobs and orphaned blobs. Can be used to cleanup or generate output for consumption by other CLI utilities.

`zmdedupe` - Used to deduplicate blobs which reside on the same `Volume` and have the same SHA-256 content hash. Since blobs are normally only deduplicated during shared delivery, this tool is useful after a migration or mailbox move.

`zmvolume` - Used to add/edit/delete volumes.


##Configuration

*Note that these values have recently been migrated into LDAP in the main branch, so older branches have similar values specified in LC. The values are listed as <new LDAP attr name>/<old LC key>*

####FileBlobStore options

`zimbraBlobStoreUncompressedCacheMinLifetime` / `uncompressed_cache_min_lifetime` - Minimum TTL in millis for uncompressed file cache used by `FileBlobStore`. Default 60000 ms.

`zimbraBlobStoreInputStreamBufferSize` / `zimbra_blob_input_stream_buffer_size_kb` - Blob store input stream buffer size in kilobytes. Default 1.

`zimbraBlobStoreSweeperMaxAge` / `zimbra_store_sweeper_max_age` - Files older than this many minutes are auto-deleted from store incoming directory. Default is 8 hours.

####StoreManager class override

`zimbra_class_store`
The `zimbra_class_store` zmlocalconfig option configures the class used for the `StoreManager`. The default `StoreManager` class is the standard filesystem-based blob store.

Default:

`zimbra_class_store = com.zimbra.cs.store.file.FileBlobStore`

This class can be replaced with a custom class to write the message blobs to the Store Manager of choice.

Example:

`zmlocalconfig -e zimbra_class_store=com.zimbra.examples.extns.storemanager.MyStoreManager`

####External StoreManager options

`zimbraStoreExternalMaxIOExceptionsForDelete` / `external_store_delete_max_ioexceptions` - Maximum number of consecutive IOExceptions before aborting during mailbox deletion. Default 25.

The `ExternalBlobStore` maintains a local cache to avoid downloading the same content from the remote store if it is accessed concurrently (e.g. by mobile device and web browser). These values can be increased to alleviate traffic to the remote store, or decreased to reduce local resource utilization.

 `zimbraStoreExternalLocalCacheMaxSize` /`external_store_local_cache_max_bytes` - Maximum number of bytes to keep in ExternalStoreManager's local file cache. Default value is 1GB. 
 
  `zimbraStoreExternalLocalCacheMaxFiles` / `external_store_local_cache_max_files` - Maximum number of files to keep in ExternalStoreManager's local file cache. Default 10000.

 `zimbraStoreExternalLocalCacheMinLifetime` / `external_store_local_cache_min_lifetime` - Minimum time in millis to keep idle entries in ExternalStoreManager's local file cache. Default 60000.


####Configuration example

1. Copy zimbra-extns-storemanager.jar to /opt/zimbra/lib/ext/storemanager dir

2. Execute:

   `zmlocalconfig -e zimbra_class_store=com.zimbra.examples.extns.storemanager.ExampleStoreManager`
   
3. Restart server

4. Perform any write operations such as sending mail, uploading files, etc. Blobs should be written to /tmp/examplestore/blobs

##Basic External Integration
The minimum code for integration requires overriding `ExternalStoreManager` and implementing `ExternalBlobIO`. This interface contains methods for writing data, reading data, and deleting data.

```java
     /**
     * Write data to blob store
     * @param in: InputStream containing data to be written
     * @param actualSize: size of data in stream, or -1 if size is unknown. To be used by implementor for optimization where possible
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @return locator string for the stored blob, unique identifier created by storage protocol
     * @throws IOException
     * @throws ServiceException
     */
    String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox) throws IOException, ServiceException;


    /**
     * Write data to blob store
     * @param in: InputStream containing data to be written
     * @param actualSize: size of data in stream, or -1 if size is unknown. To be used by implementor for optimization where possible
     * @param mailboxData: {@link Mailbox.MailboxData} object which contains information about the mailbox that contains the blob.
     * Can optionally be used by store for partitioning
     * @return locator string for the stored blob, unique identifier created by storage protocol
     * @throws IOException
     * @throws ServiceException
     */
    String writeStreamToStore(InputStream in, long actualSize, Mailbox.MailboxData mailboxData) throws IOException, ServiceException;

    /**
     * Create an input stream for reading data from blob store
     * @param locator: identifier string for the blob as returned from write operation
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @return InputStream containing the data
     * @throws IOException
     */

    InputStream readStreamFromStore(String locator, Mailbox mbox) throws IOException;
    /**
     * Create an input stream for reading data from blob store
     * @param locator: identifier string for the blob as returned from write operation
     * @param mailboxData: {@link Mailbox.MailboxData} object which contains information about the mailbox that contains the blob.
     * Can optionally be used by store for partitioning
     * @return InputStream containing the data
     * @throws IOException
     */
    InputStream readStreamFromStore(String locator, Mailbox.MailboxData mailboxData) throws IOException;

    /**
     * Delete a blob from the store
     * @param locator: identifier string for the blob
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @return true on success false on failure
     * @throws IOException
     */
    boolean deleteFromStore(String locator, Mailbox mbox) throws IOException;

    /**
     * Delete a blob from the store
     * @param locator: identifier string for the blob
     * @param mailboxData: {@link Mailbox.MailboxData} object which contains information about the mailbox that contains the blob.
     * Can optionally be used by store for partitioning
     * @return true on success false on failure
     * @throws IOException
     */
    boolean deleteFromStore(String locator, Mailbox.MailboxData mailboxData) throws IOException;
```

Here is an example of a minimalist `StoreManager` implementation which writes to local disk using java.io.File and related classes. The details of `getNewFile()` are omitted for brevity and would typically involve creating a new empty file in a predefined directory.

This class also overrides three methods from `ExternalStoreManager`

`startup()` - called during initialization, can be used to setup any paths, background threads, or other resources needed by the store implementation. Must call `super.startup()` to initialize parent resources

`shutdown()`- called during application shutdown, can be used to cleanup any temporary resources and terminate background threads. Must call `super.shutdown()` to cleanup parent resources

`isCentralized()` - A boolean value which is used in multi-server configurations. If true then the store is global and locators from one Zimbra server can be accessed from another Zimbra server, otherwise locators are only valid within the server where they are created. An example of a centralized store would be a cloud file storage system, and an example of a non-centralized store would be a local filesystem

```java
public class SimpleStoreManager extends ExternalStoreManager {

    @Override
    public void startup() throws IOException, ServiceException {
        super.startup();
        //initialize any local resources such as storage directory
    }

    @Override
    public void shutdown() {
        super.shutdown();
        //cleanup any resources and background threads
    }

    @Override
    protected boolean isCentralized() {
        //this store writes to local disk, so blobs cannot be accessed from other Zimbra servers
        return false;
    }

    private File getNewFile(Mailbox.MailboxData mailboxData) throws IOException {
        //generate a new file on disk 
    }

    @Override
    public String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox) throws IOException {
        return writeStreamToStore(in, actualSize, mbox.getData());
    }

    @Override
    public String writeStreamToStore(InputStream in, long actualSize, Mailbox.MailboxData mailboxData) throws IOException {
        File destFile = getNewFile(mailboxData);
        FileUtil.copy(in, false, destFile);
        return destFile.getCanonicalPath();
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox) throws IOException {
        return readStreamFromStore(locator, mbox.getData());
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox.MailboxData mailboxData) throws IOException {
        return new FileInputStream(locator);
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox mbox) throws IOException {
        return deleteFromStore(locator, mbox.getData());
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox.MailboxData mailboxData) throws IOException {
        File deleteFile = new File(locator);
        return deleteFile.delete();
    }
```

##External HTTP Storage
Another common use case is writing to an external HTTP endpoint, i.e. a cloud file store. If the HTTP endpoint allows unnamed stream uploads which return a unique identifier, this can be accomplished by extending `HttpStoreManager`. The implementor need only provide the code for the methods which define the URL which is used to POST new content, the process of extracting the HTTP server's unique identifier from the POST response, and the URLs for getting and deleting previously stored content. The `Mailbox.MailboxData` object is provided for optional usage; depending on the HTTP endpoint semantics it can be used to construct part of the URL, or it can be ignored. The size, SHA-256 digest, and `Mailbox.MailboxData` object are provided in the `getLocator()` method, as is the Apache Commons `HttpClient` `PostMethod` which can be used to extract response headers and the response body. For complete details on `HttpClient` see [HttpClient 3.x Website](http://hc.apache.org/httpclient-3.x/)

```java
    protected abstract String getPostUrl(Mailbox.MailboxData mboxData);
    protected abstract String getGetUrl(Mailbox.MailboxData mboxData, String locator);
    protected abstract String getDeleteUrl(Mailbox.MailboxData mboxData, String locator);
    protected abstract String getLocator(PostMethod post, String postDigest, long postSize, Mailbox.MailboxData mboxData) throws ServiceException, IOException;
```
 
 
The full listing of `HttpStoreManager` is below. Note that `isCentralized()` returns true since there will typically be a single HTTP store which all Zimbra servers connect to. The store must generate globally unique locators which can be accessed from any Zimbra server.

```java

public abstract class HttpStoreManager extends ExternalStoreManager {

    protected abstract String getPostUrl(Mailbox.MailboxData mboxData);
    protected abstract String getGetUrl(Mailbox.MailboxData mboxData, String locator);
    protected abstract String getDeleteUrl(Mailbox.MailboxData mboxData, String locator);
    protected abstract String getLocator(PostMethod post, String postDigest, long postSize, Mailbox.MailboxData mboxData) throws ServiceException, IOException;

    @Override
    public String writeStreamToStore(InputStream in, long actualSize, Mailbox.MailboxData mboxData) throws IOException,
                    ServiceException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("SHA-256 digest not found", e);
        }
        ByteUtil.PositionInputStream pin = new ByteUtil.PositionInputStream(new DigestInputStream(in, digest));

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(getPostUrl(mboxData));
        try {
            HttpClientUtil.addInputStreamToHttpMethod(post, pin, actualSize, "application/octet-stream");
            int statusCode = HttpClientUtil.executeMethod(client, post);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
                return getLocator(post, ByteUtil.encodeFSSafeBase64(digest.digest()), pin.getPosition(), mboxData);
            } else {
                throw ServiceException.FAILURE("error POSTing blob: " + post.getStatusText(), null);
            }
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox.MailboxData mboxData)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(getGetUrl(mboxData,  locator));
        int statusCode = HttpClientUtil.executeMethod(client, get);
        if (statusCode == HttpStatus.SC_OK) {
            return new UserServlet.HttpInputStream(get);
        } else {
            get.releaseConnection();
            throw new IOException("unexpected return code during blob GET: " + get.getStatusText());
        }
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox.MailboxData mboxData)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        DeleteMethod delete = new DeleteMethod(getDeleteUrl(mboxData,  locator));
        try {
            int statusCode = HttpClientUtil.executeMethod(client, delete);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NO_CONTENT) {
                return true;
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return false;
            } else {
                throw new IOException("unexpected return code during blob DELETE: " + delete.getStatusText());
            }
        } finally {
            delete.releaseConnection();
        }
    }
}
```

##External Content Based Storage
Some external storage systems may maintain identifiers based on data content. For example, a store may use SHA-256 or another hash as the primary key for stored objects. The `ContentAddressableStoreManager` abstract class may be used as a starting point for integration with this type of store.

```java

The implementer must provide code to generate a byte[] hash and a String locator.

    /**
     * Generate content hash for the blob using the hash algorithm from the remote store
     * @param blob - Blob which has been constructed locally
     * @return byte[] representing the blob content
     * @throws ServiceException
     * @throws IOException
     */
    public abstract byte[] getHash(Blob blob) throws ServiceException, IOException;

    /**
     * Generate a locator String based on the content of blob
     * @param blob - Blob which has been constructed locally
     * @return String representing the blob content, e.g. hex encoded hash
     * @throws ServiceException
     * @throws IOException
     */
    protected abstract String getLocator(Blob blob) throws ServiceException, IOException;

    /**
     * Return the locator string for the content hash by hex encoding or other similar encoding required by the store
     * @param hash: byte[] containing the content hash
     * @return the locator String
     */
    public abstract String getLocator(byte[] hash);
Here is an example implementation which uses SHA-256 as the hash and appends .blob to generate the locator.


    @Override
    protected String getLocator(Blob blob) throws ServiceException, IOException {
        return getLocator(getHash(blob));
    }

    @Override
    public String getLocator(byte[] hash) {
        return Hex.encodeHexString(hash).toUpperCase() + ".blob";
    }


    @Override
    public byte[] getHash(Blob blob) throws ServiceException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        DigestInputStream dis = new DigestInputStream(blob.getInputStream(), digest);
        while (dis.read() >= 0) {
        }
        return digest.digest();
    }
```

The full listing of `ContentAddressableStoreManager` is below. Several methods from `ExternalStoreManager` are overridden so the content locator can be included in the upstream write requests.

```java

/**
 * Abstract framework for StoreManager implementations which require content hash or other content-based locator
 * The base implementation here handles the more common cases where blob is cached locally by storeIncoming and then pushed to remote store during stage operation
 */
public abstract class ContentAddressableStoreManager extends ExternalStoreManager {

    @Override
    public String writeStreamToStore(InputStream in, long actualSize,
                    Mailbox.MailboxData mboxData) throws IOException, ServiceException {
        //the override of stage below should never allow this code to be reached
        throw ServiceException.FAILURE("anonymous write is not permitted, something went wrong", null);
    }

    /**
     * Generate content hash for the blob using the hash algorithm from the remote store
     * @param blob - Blob which has been constructed locally
     * @return byte[] representing the blob content
     * @throws ServiceException
     * @throws IOException
     */
    public abstract byte[] getHash(Blob blob) throws ServiceException, IOException;

    /**
     * Generate a locator String based on the content of blob
     * @param blob - Blob which has been constructed locally
     * @return String representing the blob content, e.g. hex encoded hash
     * @throws ServiceException
     * @throws IOException
     */
    protected abstract String getLocator(Blob blob) throws ServiceException, IOException;

    /**
     * Return the locator string for the content hash by hex encoding or other similar encoding required by the store
     * @param hash: byte[] containing the content hash
     * @return the locator String
     */
    public abstract String getLocator(byte[] hash);

    /**
     * Write data to blob store using previously generated blob locator
     * @param in: InputStream containing data to be written
     * @param actualSize: size of data in stream, or -1 if size is unknown. To be used by implementor for optimization where possible
     * @param locator string for the blob as returned by getLocator()
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @throws IOException
     * @throws ServiceException
     */
    protected abstract void writeStreamToStore(InputStream in, long actualSize, Mailbox.MailboxData mboxData, String locator) throws IOException, ServiceException;

    @Override
    public StagedBlob stage(Blob blob, Mailbox.MailboxData mboxData) throws IOException, ServiceException {
        if (supports(StoreFeature.RESUMABLE_UPLOAD) && blob instanceof ExternalUploadedBlob && blob.getRawSize() > 0) {
            ZimbraLog.store.debug("blob already uploaded, just need to commit");
            String locator = ((ExternalResumableUpload) this).finishUpload((ExternalUploadedBlob) blob);
            ZimbraLog.store.debug("staged to locator %s", locator);
            localCache.put(locator, getContent(blob));
            return new ExternalStagedBlob(mboxData,  blob.getDigest(), blob.getRawSize(), locator);
        } else {
            InputStream is = getContent(blob);
            String locator = getLocator(blob);
            try {
                StagedBlob staged = stage(is, blob.getRawSize(), mboxData,  locator);
                if (staged != null) {
                    ZimbraLog.store.debug("staged to locator %s", staged.getLocator());
                    localCache.put(staged.getLocator(), getContent(blob));
                }
                return staged;
            } finally {
                ByteUtil.closeStream(is);
            }
        }
    }

    @Override
    public StagedBlob stage(InputStream in, long actualSize, Mailbox.MailboxData mboxData) throws ServiceException, IOException {
        Blob blob = storeIncoming(in);
        try {
            return stage(blob, mboxData);
        } finally {
            quietDelete(blob);
        }
    }

    protected StagedBlob stage(InputStream in, long actualSize, Mailbox.MailboxData mboxData, String locator) throws ServiceException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("SHA-256 digest not found", e);
        }
        ByteUtil.PositionInputStream pin = new ByteUtil.PositionInputStream(new DigestInputStream(in, digest));

        try {
            writeStreamToStore(pin, actualSize, mboxData,  locator);
            return new ExternalStagedBlob(mboxData,  ByteUtil.encodeFSSafeBase64(digest.digest()), pin.getPosition(), locator);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to stage blob", e);
        }
    }
```

##Resumable Upload (Not Used - Future)
*Note that the functionality described in this section is not used by current code. It may be used for an API developed in a future release*

While not currently supported by any client-facing APIs, the ability to resume a partially completed upload exists within the blob store. With a default `ExternalStoreManager` implementation, the blobs are staged within the Zimbra server and only sent to the external store once fully uploaded. This can lead to undesirable delay for the client upon completion of an upload. In order to optimize this process, integrators may implement the `ExternalResumableUpload` interface, then provide `ExternalResumableIncomingBlob` and `ExternalResumableOutputStream` implementation which interacts with the store in a resumable manner.

```java
public interface ExternalResumableUpload {
    /**
     * Create a new ExternalResumableIncomingBlob instance to handle the upload
     * of a single object. The implementation should compute all remote metadata
     * such as remote id, size, and content hash inline with the upload process
     * so that finishUpload() does not need to traverse the data again
     *
     * @param id: local upload ID. Used internally; must be passed to super constructor
     * @param ctxt: local upload context. Used internally; must be passed to super constructor
     * @return initialized ExternalResumableIncomingBlob instance ready to accept a new data upload
     * @throws IOException
     * @throws ServiceException
     */
    public ExternalResumableIncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException, ServiceException;

    /**
     * Finalize an upload. Depending on store semantics this may involve a
     * commit, checksum, or other similar operation.
     *
     * @param blob: The ExternalUploadedBlob which data has been written into
     * @return String identifier (locator) for the permanent storage location for the uploaded content
     * @throws IOException
     * @throws ServiceException
     */
    public String finishUpload(ExternalUploadedBlob blob) throws IOException, ServiceException;
}
```

```java

/**
 * IncomingBlob implementation which streams data directly to external store during upload
 * The store must support resumable upload, otherwise it should use the default BufferingIncomingBlob implementation
 *
 */
public abstract class ExternalResumableIncomingBlob extends BufferingIncomingBlob {

    public ExternalResumableIncomingBlob(String id, BlobBuilder blobBuilder, Object ctx) throws ServiceException, IOException {
        super(id, blobBuilder, ctx);
    }

    @Override
    public OutputStream getAppendingOutputStream() throws IOException {
        lastAccessTime = System.currentTimeMillis();
        return getAppendingOutputStream(blobBuilder);
    }

    @Override
    public long getCurrentSize() throws IOException {
        long internalSize = super.getCurrentSize();
        long remoteSize = getRemoteSize();
        if (remoteSize != internalSize) {
            throw new IOException("mismatch between local (" + internalSize + ") and remote (" + remoteSize + ") " +
                "content sizes. Client must restart upload", null);
        } else {
            return internalSize;
        }
    }

    @Override
    public Blob getBlob() throws IOException, ServiceException {
        return new ExternalUploadedBlob(blobBuilder.finish(), id);
    }

    /**
     * Retrieve an OutputStream which can be used to write data to the remote upload location
     * @param blobBuilder: Used to create local Blob instance inline with upload. Must be passed to super constructor
     * @return ExternalResumableOutputStream instance which can write data to the upload session/location encapsulated by this IncomingBlob instance
     * @throws IOException
     */
    protected abstract ExternalResumableOutputStream getAppendingOutputStream(BlobBuilder blobBuilder) throws IOException;

    /**
     * Query the remote store for the size of the upload received so far. Used for consistency checking during resume
     * @return: The number of bytes which have been stored remotely.
     * @throws IOException
     * @throws ServiceException
     */
    protected abstract long getRemoteSize() throws IOException;
}
```

```java

/**
 * OutputStream used to write to an external store during resumable upload.
 *
 */
public abstract class ExternalResumableOutputStream extends BlobBuilderOutputStream {

    protected ExternalResumableOutputStream(BlobBuilder blobBuilder) {
        super(blobBuilder);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeToExternal(b, off, len);
        super.write(b, off, len);
    }

    /**
     * Append data to remote upload location
     * @param b: byte array holding the data to upload
     * @param off: offset to start the upload from
     * @param len: length of the data to copy from the byte array
     * @throws IOException
     */
    protected abstract void writeToExternal(byte[] b, int off, int len) throws IOException;
}
```

The following example illustrates the key functionality involved in resumable upload. The example is intentionally arbitrary and uses local disk storage.

```java

/**
 * Example implementation of ExternalResumableUpload which writes to a flat directory structure
 * This is intended for illustration purposes only; it should *never* be used in a production environment
 *
 */
public class SimpleStreamingStoreManager extends SimpleStoreManager implements ExternalResumableUpload {

    String uploadDirectory = "/tmp/simplestore/uploads";

    @Override
    public void startup() throws IOException, ServiceException {
        super.startup();
        FileUtil.mkdirs(new File(uploadDirectory));
    }

    @Override
    public String finishUpload(ExternalUploadedBlob blob) throws IOException,
                    ServiceException {
        ZimbraLog.store.info("finishing upload to "+blob.getUploadId());
        return blob.getUploadId();
    }

    @Override
    public ExternalResumableIncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException, ServiceException {
        return new SimpleStreamingIncomingBlob(id, getBlobBuilder(), ctxt);
    }

    private class SimpleStreamingIncomingBlob extends ExternalResumableIncomingBlob {

        private final File file;

        public SimpleStreamingIncomingBlob(String id, BlobBuilder blobBuilder,
                        Object ctx) throws ServiceException, IOException {
            super(id, blobBuilder, ctx);
            String baseName = uploadDirectory+"/upload-"+id;
            String name = baseName;

            synchronized (this) {
                int count = 1;
                File upFile = new File(name+".upl");
                while (upFile.exists()) {
                    name = baseName+"_"+count++;
                    upFile = new File(name+".upl");
                }
                if (upFile.createNewFile()) {
                    ZimbraLog.store.debug("writing to new file %s",upFile.getName());
                    file = upFile;
                } else {
                    throw new IOException("unable to create new file");
                }
            }
        }

        @Override
        protected ExternalResumableOutputStream getAppendingOutputStream(BlobBuilder blobBuilder) throws IOException {
            return new SimpleStreamingOutputStream(blobBuilder, file);
        }

        @Override
        protected long getRemoteSize() throws IOException {
            return file.length();
        }

        @Override
        public Blob getBlob() throws IOException, ServiceException {
            return new ExternalUploadedBlob(blobBuilder.finish(), file.getCanonicalPath());
        }
    }

    private class SimpleStreamingOutputStream extends ExternalResumableOutputStream {

        private final FileOutputStream fos;

        public SimpleStreamingOutputStream(BlobBuilder blobBuilder, File file) throws IOException {
            super(blobBuilder);
            this.fos = new FileOutputStream(file, true);
        }

        @Override
        protected void writeToExternal(byte[] b, int off, int len)
                        throws IOException {
            fos.write(b, off, len);
        }
    }
```
