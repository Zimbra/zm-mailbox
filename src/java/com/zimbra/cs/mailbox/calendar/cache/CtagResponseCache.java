package com.zimbra.cs.mailbox.calendar.cache;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraMemcachedClient;
import com.zimbra.common.util.ZimbraMemcachedClient.KeyPrefix;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;
import com.zimbra.cs.memcached.MemcachedConnector;

// for CalDAV
// caches responses for PROPFIND-ctag requests
public class CtagResponseCache {

    private static final KeyPrefix MEMCACHED_PREFIX = MemcachedKeyPrefix.CALDAV_CTAG_RESPONSE;
    private ZimbraMemcachedClient mMemcachedClient;

    CtagResponseCache() {
        mMemcachedClient = MemcachedConnector.getClient();
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
            mKeyVal = String.format("%s:%s:%d", mAccountId, mUserAgent, mRootFolderId);
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

        public String getKeyString() { return mKeyVal; }
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

        private static final String FN_RESPONSE_BODY = "b";
        private static final String FN_BODY_LENGTH = "bl";
        private static final String FN_RAW_LENGTH = "rl";
        private static final String FN_IS_GZIPPED = "gz";
        private static final String FN_CALLIST_VERSION = "clv";
        private static final String FN_NUM_CTAGS = "nct";
        private static final String FN_CTAGS_CAL_ID = "ci";
        private static final String FN_CTAGS_CTAG = "ct";

        Metadata encodeMetadata() throws ServiceException {
            Metadata meta = new Metadata();
            String body = null;
            try {
                body = new String(mRespBody, "iso-8859-1");  // must use iso-8859-1 to allow all bytes
            } catch (UnsupportedEncodingException e) {
                throw ServiceException.FAILURE("Unable to encode ctag response body", e);
            } 
            meta.put(FN_BODY_LENGTH, mRespBody.length);
            meta.put(FN_RESPONSE_BODY, body);
            meta.put(FN_RAW_LENGTH, mRawLen);
            if (mGzipped)
                meta.put(FN_IS_GZIPPED, true);
            meta.put(FN_CALLIST_VERSION, mVersion);
            int i = 0;
            for (Map.Entry<Integer, String> entry : mCtags.entrySet()) {
                meta.put(FN_CTAGS_CAL_ID + i, entry.getKey());
                meta.put(FN_CTAGS_CTAG + i, entry.getValue());
                ++i;
            }
            meta.put(FN_NUM_CTAGS, i);
            return meta;
        }

        CtagResponseCacheValue(Metadata meta) throws ServiceException {
            int bodyLen = (int) meta.getLong(FN_BODY_LENGTH, 0);
            String body = meta.get(FN_RESPONSE_BODY, null);
            if (body == null)
                throw ServiceException.FAILURE("Ctag response body not found in cached entry", null);
            if (body.length() != bodyLen)
                throw ServiceException.FAILURE("Ctag response body has wrong length: " + body.length() +
                                               " when expecting " + bodyLen, null);
            try {
                mRespBody = body.getBytes("iso-8859-1");  // must use iso-8859-1 to allow all bytes
            } catch (UnsupportedEncodingException e) {
                throw ServiceException.FAILURE("Unable to decode ctag response body", e);
            }
            mRawLen = (int) meta.getLong(FN_RAW_LENGTH, 0);
            mGzipped = meta.getBool(FN_IS_GZIPPED, false);
            mVersion = meta.get(FN_CALLIST_VERSION, "");
            int numCtags = (int) meta.getLong(FN_NUM_CTAGS, 0);
            mCtags = new HashMap<Integer, String>(Math.min(numCtags, 100));
            if (numCtags > 0) {
                for (int i = 0; i < numCtags; ++i) {
                    int calId = (int) meta.getLong(FN_CTAGS_CAL_ID + i, -1);
                    String ctag = meta.get(FN_CTAGS_CTAG + i, null);
                    if (calId != -1 && ctag != null)
                        mCtags.put(calId, ctag);
                    else
                        break;
                }
            }
        }
    }

    private CtagResponseCacheValue cacheGet(CtagResponseCacheKey key) throws ServiceException {
        Object value = mMemcachedClient.get(MEMCACHED_PREFIX, key.getKeyString());
        if (value == null) return null;

        String encoded = (String) value;
        Metadata meta = new Metadata(encoded);
        return new CtagResponseCacheValue(meta);
    }

    private void cachePut(CtagResponseCacheKey key, CtagResponseCacheValue value) throws ServiceException {
        String encoded = value.encodeMetadata().toString();
        mMemcachedClient.put(MEMCACHED_PREFIX, key.getKeyString(), encoded);
    }

    public CtagResponseCacheValue get(CtagResponseCacheKey key) throws ServiceException {
        return cacheGet(key);
    }

    public void put(CtagResponseCacheKey key, CtagResponseCacheValue value) throws ServiceException {
        cachePut(key, value);
    }
}
