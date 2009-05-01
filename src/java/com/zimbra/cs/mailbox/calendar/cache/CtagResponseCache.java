package com.zimbra.cs.mailbox.calendar.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;

// for CalDAV
// caches responses for PROPFIND-ctag requests
public class CtagResponseCache {

    private CtagResponseLRU mLRU;

    CtagResponseCache() {
        // TODO: Use memcached instead of LRU on heap.
        int lruSize = 0;
        if (LC.calendar_cache_enabled.booleanValue())
            lruSize = LC.calendar_cache_lru_size.intValue();
        mLRU = new CtagResponseLRU(lruSize);
    }

    // CTAG response cache key is account + client (User-Agent) + root folder
    public static class CtagResponseCacheKey {
        private String mAccountId;
        private String mUserAgent;
        private int mRootFolderId;
        private String mKeyVal;

        public CtagResponseCacheKey(String accountId, String userAgent, int rootFolderId) {
            mAccountId = accountId;
            mUserAgent = userAgent;
            mRootFolderId = rootFolderId;
            mKeyVal = String.format("%s-%s-%d", mAccountId, mUserAgent, mRootFolderId);
        }

        public String getAccountId() { return mAccountId; }
        public String getUserAgent() { return mUserAgent; }
        public int getRootFolderId() { return mRootFolderId; }

        public boolean equals(Object other) {
            if (other instanceof CtagResponseCacheKey) {
                CtagResponseCacheKey otherKey = (CtagResponseCacheKey) other;
                return mKeyVal.equals(otherKey.mKeyVal);
            }
            return false;
        }

        public int hashCode() {
            return mKeyVal.hashCode();
        }
    }

    public static class CtagResponseCacheValue {
        private byte[] mRespBody;
        private int mRawLen;
        private boolean mGzipped;
        private String mVersion;  // calendar list's version at response cache time
        private Map<Integer /* folder id */, String /* ctag */> mCtags;  // snapshot of ctags at response cache time

        public CtagResponseCacheValue(byte[] respBody, int rawLen, boolean gzipped, String calListVer, Map<Integer, String> ctags) {
            mRespBody = respBody;
            if (gzipped) {
                mRawLen = rawLen;
                mGzipped = gzipped;
            } else {
                mRawLen = respBody.length;
                mGzipped = false;
            }
            mVersion = calListVer;
            mCtags = ctags;
        }

        public byte[] getResponseBody() { return mRespBody; }
        public int getRawLength() { return mRawLen; }
        public boolean isGzipped() { return mGzipped; }
        public String getVersion() { return mVersion; }
        public Map<Integer, String> getCtags() { return mCtags; }
    }

    @SuppressWarnings("serial")
    private static class CtagResponseLRU extends LinkedHashMap<CtagResponseCacheKey, CtagResponseCacheValue> {
        private int mMaxAllowed;

        private CtagResponseLRU(int capacity) {
            super(capacity + 1, 1.0f, true);
            mMaxAllowed = Math.max(capacity, 1);
        }

        @Override
        public void clear() {
            super.clear();
        }

        @Override
        public CtagResponseCacheValue get(Object key) {
            return super.get(key);
        }

        @Override
        public CtagResponseCacheValue put(CtagResponseCacheKey key, CtagResponseCacheValue value) {
            CtagResponseCacheValue prevVal = super.put(key, value);
            return prevVal;
        }

        @Override
        public void putAll(Map<? extends CtagResponseCacheKey, ? extends CtagResponseCacheValue> t) {
            super.putAll(t);
        }

        @Override
        public CtagResponseCacheValue remove(Object key) {
            CtagResponseCacheValue prevVal = super.remove(key);
            return prevVal;
        }        

        @Override
        protected boolean removeEldestEntry(Map.Entry<CtagResponseCacheKey, CtagResponseCacheValue> eldest) {
            boolean remove = size() > mMaxAllowed;
            return remove;
        }
    }

    public CtagResponseCacheValue get(CtagResponseCacheKey key) {
        synchronized (mLRU) {
            return mLRU.get(key);
        }
    }

    public void put(CtagResponseCacheKey key, CtagResponseCacheValue value) {
        synchronized (mLRU) {
            mLRU.put(key, value);
        }
    }
}
