package com.zimbra.cs.redolog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RScript.ReturnType;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Longs;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.redis.RedisUtils;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

public class RedisRedoBlobStore extends RedoLogBlobStore {

    private RedissonClient client;

    private static final String HKEY_DATA = "d";
    private static final String HKEY_SIZE = "s";
    private static final String BLOB_HASH_SUFFIX = "data";
    private static final String BLOB_SET_SUFFIX = "refs";

    private static final Codec BLOB_CODEC = new CompositeCodec(StringCodec.INSTANCE, ByteArrayCodec.INSTANCE);

    public RedisRedoBlobStore(BlobReferenceManager refManager) {
        super(refManager);
        client = RedissonClientHolder.getInstance().getRedissonClient();
    }

    private RMap<String, byte[]> getBlobMap(String digest) {
        return client.getMap(getBlobMapKey(digest), BLOB_CODEC);
    }

    private static String getIdSetKey(String digest) {
        return RedisUtils.createHashTaggedKey(digest, BLOB_SET_SUFFIX).getKey();
    }

    private static String getBlobMapKey(String digest) {
        return RedisUtils.createHashTaggedKey(digest, BLOB_HASH_SUFFIX).getKey();
    }

    @Override
    public Blob fetchBlob(String digest) throws ServiceException, IOException {
        RMap<String, byte[]> rmap = getBlobMap(digest);
        Map<String, byte[]> map = rmap.readAllMap();
        byte[] payload = map.get(HKEY_DATA);
        long msgSize = Longs.fromByteArray(map.get(HKEY_SIZE));
        boolean compressed = payload.length != msgSize;
        Blob blob = StoreManager.getInstance().storeIncoming(new ByteArrayInputStream(payload), compressed);
        return blob;
    }

    @Override
    protected void storeBlobData(InputStream in, long size, String digest) throws IOException {
        RMap<String, byte[]> rmap = getBlobMap(digest);
        byte[] payload = ByteStreams.toByteArray(in);
        Map<String, byte[]> data = new HashMap<>();
        data.put(HKEY_DATA, payload);
        data.put(HKEY_SIZE, Longs.toByteArray(size));
        rmap.putAll(data);
    }

    @Override
    protected void deleteBlobData(String digest) {
        // not actually needed for this implementation, since the lua script removes blobs automatically
        // when their reference count drops to 0

    }

    public static class RedisReferenceManager extends RedoLogBlobStore.BlobReferenceManager {

        private RScript script;

        private static final String ADD_BLOB_REFS_SCRIPT =
                "redis.call('sadd', KEYS[1], unpack(ARGV)); " +
                "return redis.call('exists', KEYS[2]);";

        private static final String DELETE_BLOB_REFS_SCRIPT =
                "local removed = redis.call('srem', KEYS[1], unpack(ARGV)); " +
                "if (removed > 0 and redis.call('scard', KEYS[1]) == 0) then " +
                "  redis.call('del', KEYS[2]); " +
                "  return 1; " +
                "end; " +
                "return 0;";

        RedisReferenceManager() {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            script = client.getScript(StringCodec.INSTANCE);
        }

        @Override
        public boolean addRefs(String digest, Collection<Integer> mboxIds) {
            List<Object> keys = Arrays.<Object>asList(getIdSetKey(digest), getBlobMapKey(digest));
            Object[] values = mboxIds.toArray();
            return script.eval(Mode.READ_WRITE, ADD_BLOB_REFS_SCRIPT, ReturnType.BOOLEAN, keys, values);
        }

        @Override
        public boolean removeRefs(String digest, Collection<Integer> mboxIds) {
            List<Object> keys = Arrays.<Object>asList(getIdSetKey(digest), getBlobMapKey(digest));
            Object[] values = mboxIds.toArray();
            return script.eval(Mode.READ_WRITE, DELETE_BLOB_REFS_SCRIPT, ReturnType.BOOLEAN, keys, values);
        }
    }

    public static class Factory implements RedoLogBlobStore.Factory {

        @Override
        public RedoLogBlobStore getRedoLogBlobStore() {
            return new RedisRedoBlobStore(new RedisReferenceManager());
        }
    }
}
