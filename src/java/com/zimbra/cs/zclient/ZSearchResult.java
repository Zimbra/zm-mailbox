/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import java.util.List;

public class ZSearchResult {
 
    private List<ZSearchHit> mHits;
    private boolean mHasMore;
    private String mSortBy;
    private int mOffset;

    public ZSearchResult(List<ZSearchHit> hits, boolean hasMore, String sortBy, int offset) {
        mHits = hits;
        mHasMore = hasMore;
        mSortBy = sortBy;
        mOffset = offset;
    }

    /**
     * @return ZSearchHit objects from search
     */
    public List<ZSearchHit> getHits() {
        return mHits;
    }
    
    /**
     * @return true if there are more search results on the server
     */
    public boolean hasMore() {
        return mHasMore;
    }
    
    /**
     * @return the sort by value
     */
    public String getSortBy() {
        return mSortBy;
    }
    
    /**
     * @return offset of the search
     */
    public int getOffset() {
        return mOffset;
    }

    public String toString() {
        StringBuilder hits = new StringBuilder();
        hits.append("{");
        for (ZSearchHit hit : mHits) {
            hits.append(hit).append("\n");
        }
        hits.append("}");        
        return String.format("searchresult: { more: %s, sortBy: %s, offset: %d, hits: %s }",
                mHasMore, mSortBy, mOffset, hits.toString());
    }
    
}
