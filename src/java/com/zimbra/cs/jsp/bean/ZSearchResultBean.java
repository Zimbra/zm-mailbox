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
package com.zimbra.cs.jsp.bean;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.zclient.ZContactHit;
import com.zimbra.cs.zclient.ZConversationHit;
import com.zimbra.cs.zclient.ZMessageHit;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchResult;

public class ZSearchResultBean {
    
    private ZSearchResult mResult;
    private ArrayList<ZSearchHitBean> mHits;
    private int mPrevOffset;
    private int mNextOffset;    
    
    public ZSearchResultBean(ZSearchResult result, int prevOffset, int nextOffset) {
        mResult = result;
        mPrevOffset = prevOffset;
        mNextOffset = nextOffset;
    }

    public int getSize() { return mResult.getHits().size(); }
    
    /**
     * @return ZSearchHit objects from search
     */
    public synchronized List<ZSearchHitBean> getHits() {
        if (mHits == null) {
            mHits = new ArrayList<ZSearchHitBean>();
            for (ZSearchHit hit : mResult.getHits()) {
                if (hit instanceof ZConversationHit) {
                    mHits.add(new ZConversationHitBean((ZConversationHit)hit));
                } else if (hit instanceof ZMessageHit) {
                    mHits.add(new ZMessageHitBean((ZMessageHit)hit));
                } else if (hit instanceof ZContactHit) {
                    mHits.add(new ZContactHitBean((ZContactHit)hit));
                }
            }
        }
        return mHits;
    }

    /**
     * @return true if there are more search results on the server
     */
    public boolean getHasMore() { return mResult.hasMore(); }  
    
    
    /**
     * @return the sort by value
     */
    public String getSortBy() { return mResult.getSortBy(); }

    /**
     * @return offset of the search
     */
    public int getOffset() { return mResult.getOffset(); }

    public int getPreviousOffset() { return mPrevOffset; }
    
    public int getNextOffset() { return mNextOffset; }

}
