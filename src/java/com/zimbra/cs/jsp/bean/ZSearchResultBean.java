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
