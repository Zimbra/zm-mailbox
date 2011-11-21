package com.zimbra.cs.octosync.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Implementation of a temporary BlobStore. Temporary means that blobs that
 * are "stored" expire after specified amount of time in which they were not
 * accessed.
 *
 * Implementation uses StoreManager's BlobBuilder to create and store blobs.
 * Both incoming and stored blobs are stored the same way, i.e. they
 * live in the incoming directory of StoreManager's. The difference is in
 * the default retention period, which can be set independently for incoming
 * and stored blobs.
 *
 * IncomingBlob and StoredBlob instances (metadata) are kept in memory. This is okay
 * because the intention is to keep them for short periods of time (minutes-hours).
 * The actual data is stored on disk (or wherever StoreManager keeps it).
 *
 */
public class StoreManagerBasedTempBlobStore extends BlobStore
{
    private static final Log log = LogFactory.getLog(StoreManagerBasedTempBlobStore.class);

    private StoreManager storeManager;

    /**
     * Tracks incoming blobs metadata.
     *
     * Not persisted. If server is restarted we rely on StoreManager's
     * implementation to delete any files left behind in the incoming directory
     *
     * (this is true with all current implementations, i.e. FileBlobStore and
     * HttpBlobStore, both of which use IncomingDirectory.
     *
     */
    private Map<String, IncomingBlob> incomingBlobs;

    /**
     * Tracks "stored" blobs metadata.
     *
     * Not persisted. See note about incomingBlobs.
     */
    private Map<String, TreeMap<Integer, StoredBlob>> storedBlobs;

    private long incomingExpiration;
    private long storedExpiration;

    private static final long REAPER_INTERVAL_MSEC = 3 * Constants.MILLIS_PER_MINUTE;

    public final class IncomingBlob extends BlobStore.IncomingBlob
    {
        private String id;
        private BlobBuilder blobBuilder;
        private Object ctx;
        private long expectedSize;
        private boolean expectedSizeSet;
        private long lastAccessTime;

        IncomingBlob(String id, BlobBuilder blobBuilder, Object ctx)
        {
            this.id = id;
            this.blobBuilder = blobBuilder;
            this.ctx = ctx;

            lastAccessTime = System.currentTimeMillis();
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public OutputStream getAppendingOutputStream()
        {
            lastAccessTime = System.currentTimeMillis();

            return new OutputStream() {

                @Override
                public void write(int b) throws IOException
                {
                    // inefficient, but we don't expect this to be used
                    byte[] tmp = new byte[1];
                    tmp[0] = (byte)b;
                    this.write(tmp);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException
                {
                    blobBuilder.append(b, off, len);
                }
            };
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            assert false : "Not yet implemented";
            return null;
            //return new SizeLimitedInputStream(blobBuilder.getBlob().getInputStream(), getCurrentSize());
        }

        @Override
        public Object getContext()
        {
            return ctx;
        }

        @Override
        public void setContext(Object value)
        {
            ctx = value;
        }

        @Override
        public long getCurrentSize()
        {
            return blobBuilder.getTotalBytes();
        }

        @Override
        public boolean hasExpectedSize()
        {
            return expectedSizeSet;
        }

        @Override
        public long getExpectedSize()
        {
            assert expectedSizeSet : "Expected size not set";
            return expectedSize;
        }

        @Override
        public void setExpectedSize(long value)
        {
            assert !expectedSizeSet : "Expected size already set: " + expectedSize;
            expectedSize = value;
            expectedSizeSet = true;
        }

        private long getLastAccessTime()
        {
            return lastAccessTime;
        }
    }

    public final class StoredBlob extends BlobStore.StoredBlob
    {
        private Blob blob;
        private Object ctx;
        private long lastAccessTime;
        private long size;

        protected StoredBlob(String id, Blob blob, Object ctx, long size)
        {
            super(id);
            this.blob = blob;
            this.ctx = ctx;
            this.size = size;

            lastAccessTime = System.currentTimeMillis();
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            lastAccessTime = System.currentTimeMillis();
            return blob.getInputStream();
        }

        @Override
        public Object getContext()
        {
            return ctx;
        }

        @Override
        public void setContext(Object value)
        {
            ctx = value;
        }

        private long getLastAccessTime()
        {
            return lastAccessTime;
        }

        @Override
        public long getSize()
        {
            return size;
        }
    }

    /**
     * Instantiates new StoreManagerBasedTempBlobStore.
     *
     * @param storeManager The store manager to use
     *      (note, it is a singleton currently, but let's not hardcode it)
     * @param incomingExpiration the incoming expiration (in ms)
     * @param storedExpiration the stored expiration (in ms)
     */
    public StoreManagerBasedTempBlobStore(StoreManager storeManager,
            long incomingExpiration,
            long storedExpiration)
    {
        this.storeManager = storeManager;
        this.incomingBlobs = new HashMap<String, IncomingBlob>();
        this.storedBlobs = new HashMap<String, TreeMap<Integer, StoredBlob>>();
        this.incomingExpiration = incomingExpiration;
        this.storedExpiration = storedExpiration;

        Zimbra.sTimer.schedule(new ReaperTask(), REAPER_INTERVAL_MSEC, REAPER_INTERVAL_MSEC);
    }

    // BlobStore API
    @Override
    public IncomingBlob createIncoming(Object ctx) throws IOException, ServiceException
    {
        BlobBuilder bb = storeManager.getBlobBuilder();

        synchronized (incomingBlobs) {

            String id;

            do {
                id = UUID.randomUUID().toString();
            } while (incomingBlobs.containsKey(id));

            IncomingBlob ib = new IncomingBlob(id, bb, ctx);

            incomingBlobs.put(id, ib);
            return ib;
        }
    }

    // BlobStore API
    @Override
    public IncomingBlob getIncoming(String id)
    {
        synchronized (incomingBlobs) {
            return incomingBlobs.get(id);
        }
    }

    // BlobStore API
    @Override
    public StoredBlob store(BlobStore.IncomingBlob ib, String id, int version) throws IOException, ServiceException
    {
        IncomingBlob myIb = (IncomingBlob)ib;
        myIb.blobBuilder.finish();

        synchronized (incomingBlobs) {
            IncomingBlob existingBlob = incomingBlobs.remove(myIb.getId());
            if (existingBlob == null) {
                // theoretically can happen if incoming blob was reaped in meantime so
                // we cannot crash due to  this; let's just fail; note we cannot accept it
                // because the underlying file is gone
                throw ServiceException.INVALID_REQUEST("Cannot accept incoming blob " + myIb.getId(), null);
            }
            assert existingBlob == myIb : "Wrong blob removed: " + myIb.getId();
        }

        Blob blob = myIb.blobBuilder.getBlob();
        StoredBlob sb = new StoredBlob(id, blob, myIb.getContext(), blob.getRawSize());

        synchronized (storedBlobs) {
            TreeMap<Integer, StoredBlob> versionMap = storedBlobs.get(id);

            if (versionMap == null) {
                versionMap = new TreeMap<Integer, StoredBlob>();
                storedBlobs.put(id, versionMap);
            }

            Integer versionInt = new Integer(version);
            assert !versionMap.containsKey(versionInt) : "Version " + version + " already exists";

            versionMap.put(versionInt, sb);
        }

        return sb;
    }

    // BlobStore API
    @Override
    public void deleteIncoming(BlobStore.IncomingBlob ib)
    {
        IncomingBlob myIb = (IncomingBlob)ib;

        synchronized (incomingBlobs) {
            IncomingBlob existingBlob = incomingBlobs.remove(myIb.getId());
            assert existingBlob == null || existingBlob == myIb : "Wrong blob removed: " + myIb.getId();
        }

        myIb.blobBuilder.dispose();
    }

    // BlobStore API
    @Override
    public StoredBlob get(String id)
    {
        synchronized (storedBlobs) {
            TreeMap<Integer, StoredBlob> versionMap = storedBlobs.get(id);

            if (versionMap == null || versionMap.isEmpty()) {
                return null;
            }

            return versionMap.lastEntry().getValue();
        }
    }

    // BlobStore API
    @Override
    public StoredBlob get(String id, int version)
    {
        synchronized (storedBlobs) {
            TreeMap<Integer, StoredBlob> versionMap = storedBlobs.get(id);

            if (versionMap == null || versionMap.isEmpty()) {
                return null;
            }

            return versionMap.get(new Integer(version));
        }
    }

    // BlobStore API
    @Override
    public void delete(BlobStore.StoredBlob sb)
    {
        if (sb != null) {
            TreeMap<Integer, StoredBlob> versionMap;

            synchronized (storedBlobs) {
                versionMap = storedBlobs.remove(sb.getId());
            }

            if (versionMap != null) {
                for (StoredBlob b : versionMap.values()) {
                    storeManager.quietDelete(b.blob);
                }
            }
        }
    }

    @Override
    public void delete(BlobStore.StoredBlob sb, int version)
    {
        if (sb != null) {

            StoredBlob mySb = (StoredBlob)sb;
            String id = mySb.getId();

            synchronized (storedBlobs) {
                TreeMap<Integer, StoredBlob> versionMap = storedBlobs.get(id);

                if (versionMap == null) {
                    return;
                }

                StoredBlob removedSb = versionMap.remove(new Integer(version));

                if (removedSb != null) {

                    assert mySb == removedSb : "Wrong blob removed";

                    storeManager.quietDelete(mySb.blob);

                    if (versionMap.isEmpty()) {
                        storedBlobs.remove(id);
                    }
                }
            }
        }
    }

    private final class ReaperTask extends TimerTask
    {
        @Override
        public void run()
        {
            try {

                ArrayList<Blob> reapedBlobs = new ArrayList<Blob>();

                if (incomingExpiration > 0) {
                    synchronized (incomingBlobs) {

                        long cutoffTime = System.currentTimeMillis() - incomingExpiration;

                        for (IncomingBlob ib : incomingBlobs.values()) {
                            if (ib.getLastAccessTime() <= cutoffTime) {
                                reapedBlobs.add(ib.blobBuilder.finish());
                                incomingBlobs.remove(ib.getId());
                            }
                        }
                    }
                }

                final int numReapedIncoming = reapedBlobs.size();

                if (numReapedIncoming > 0) {
                    log.info("Removed " + numReapedIncoming + " expired incoming incomplete blobs");
                }

                if (storedExpiration > 0) {
                    synchronized (storedBlobs) {

                        long cutoffTime = System.currentTimeMillis() - storedExpiration;

                        for (Map<Integer, StoredBlob> versionMap : storedBlobs.values()) {

                            String sbId = null;

                            for (Map.Entry<Integer, StoredBlob> entry : versionMap.entrySet()) {

                                StoredBlob sb = entry.getValue();
                                if (sb.getLastAccessTime() <= cutoffTime) {
                                    sbId = sb.getId();
                                    reapedBlobs.add(sb.blob);
                                    versionMap.remove(entry.getKey());
                                }
                            }

                            if (sbId != null && versionMap.isEmpty()) {
                                Object removed = storedBlobs.remove(sbId);
                                assert removed == versionMap;
                            }
                        }
                    }
                }

                final int numReapedStored = reapedBlobs.size() - numReapedIncoming;

                if (numReapedStored > 0) {
                    log.info("Removed " +  numReapedStored + " expired stored blobs");
                }

                for (Blob b : reapedBlobs) {
                    storeManager.quietDelete(b);
                }

            } catch (Exception e) {
                log.warn("Exception in " + getClass().getName() + ": " + e);
            }
        }
    }

    /**
     * Bit of a hack, a special function that extracts underlying Blob instance
     * (com.zimbra.cs.store.Blob) and removes specified IncomingBlob.
     * This is a special use to take advantage of "resumability" of PatchStore
     * in places that require the cs.store.Blob (e.g. NativeFormatter).
     *
     * @param ib Incoming blob to extract
     * @return Underlying Blob instance
     *
     * @throws IOException
     * @throws ServiceException
     */
    public Blob extractIncoming(BlobStore.IncomingBlob ib) throws IOException, ServiceException
    {
        IncomingBlob myIb = (IncomingBlob)ib;

        synchronized (incomingBlobs) {
            IncomingBlob existingBlob = incomingBlobs.remove(myIb.getId());
            assert existingBlob == null || existingBlob == myIb : "Wrong blob removed: " + myIb.getId();
        }

        return myIb.blobBuilder.finish();
    }

}
