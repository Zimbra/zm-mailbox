/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Triple;
import com.zimbra.cs.mailbox.Metadata;

// Cache of responses to PROPFIND-ctag requests. Used for CalDAV.
public interface CtagResponseCache {

    Value get(Key key) throws ServiceException;

    void put(Key key, Value value) throws ServiceException;


    public static class Key extends Triple<String, String, Integer> {
        public Key(String accountId, String userAgent, int rootFolderId) {
            super(accountId, userAgent, rootFolderId);
        }
    }


    public static class Value {
        private byte[] mRespBody;
        private int mRawLen;
        private boolean mGzipped;
        private String mVersion;  // calendar list's version at response cache time
        private Map<Integer /* folder id */, String /* ctag */> mCtags;  // snapshot of ctags at response cache time

        public Value(byte[] respBody, int rawLen, boolean gzipped, String calListVer, Map<Integer, String> ctags) {
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

        Value(Metadata meta) throws ServiceException {
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
}
