/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.client.ZSearchParams.Cursor;
import org.json.JSONException;

import java.util.List;


public class ZSearchContext implements ToZJSONObject {

    private ZMailbox mMailbox;
    private ZSearchParams mParams;
    private ZSearchResult mResult;
    private int mIndex;
    private int mCount;
    private int mMaxHits = Integer.MAX_VALUE;
    private boolean mByOffset = false;
    private boolean mHasMore;

    public ZSearchContext(ZSearchParams params, ZMailbox mailbox) {
        mParams = params;
        mMailbox = mailbox;
    }

    /**
     *
     * @return next search hit in the results, or null.
     * @throws com.zimbra.common.service.ServiceException
     */
    public ZSearchHit getNextHit() throws ServiceException {
         if (mCount <mMaxHits) {
             if (mResult == null || (mIndex+1 >= mResult.getHits().size() && mResult.hasMore())) {
                 if (mResult != null) {
                     // get next page
                     if (mByOffset) {
                         mParams.setOffset(mParams.getOffset()+mResult.getHits().size());
                     } else {
                         List<ZSearchHit> hits = mResult.getHits();
                         ZSearchHit lastHit = hits.get(hits.size()-1);
                         mParams.setCursor(new Cursor(lastHit.getId(), lastHit.getSortField()));
                     }
                 }

                 // search it
                 if (mParams.getConvId() != null)
                     mResult = mMailbox.searchConversation(mParams.getConvId(), mParams);
                 else
                     mResult = mMailbox.search(mParams);
                 mIndex = 0;
             } else {
                 ++mIndex;
             }
         }

         if (mCount >= mMaxHits || mIndex >= mResult.getHits().size()) {
             mHasMore = false;
             return null;
         } else {
             ++mCount;
             mHasMore = mCount < mMaxHits && ((mIndex+1 < mResult.getHits().size() || mResult.hasMore()));
             return mResult.getHits().get(mIndex);
         }
    }

    public void setByOffset(boolean byOffset) { mByOffset = byOffset; }

    public void setMaxHits(int maxHits) { mMaxHits = maxHits; }

    public boolean isHasMore() { return mHasMore; }

    public boolean isByOffset() { return mByOffset; }

    public int getIndex() { return mIndex; }

    /**
     *
     * @return numbers of hits returned so far
     */
    public int getCount() { return mCount; }

    public ZSearchResult getResult() {
        return mResult;
    }

    public ZSearchParams getParams() {
        return mParams;
    }

    public ZSearchHit getCurrentHit() {
        return (mResult != null && mIndex < mResult.getHits().size()) ?
                mResult.getHits().get(mIndex) : null;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("more", mHasMore);
        zjo.put("index", mIndex);
        zjo.put("count", mCount);
        zjo.put("maxHits", mMaxHits);
        zjo.put("searchResult", mResult);
        zjo.put("searchParams", mParams);
        return zjo;
    }

    public String toString() {
       return String.format("[ZSearchContext count=%d more=%s]", mCount, mHasMore);
    }

    public String dump() {
       return ZJSONObject.toString(this);
    }

}
