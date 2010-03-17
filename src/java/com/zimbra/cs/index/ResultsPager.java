/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Helper class -- deals with finding the "first result" to be returned, which
 * is dealt with in one of several ways depending on if this is an offset/limit
 * request, a forward cursor request, or a backward cursor request. 
 * (see bug 2937)
 */
public final class ResultsPager 
{
    private ZimbraQueryResults mResults;
    private boolean mFixedOffset;
    
    // in cases where ReSortingQueryResults is simulating the cursor for us, we need to skip
    // the passed-in cursor AND offset....otherwise pages will be skipped b/c we will end up
    // skipping OFFSET entries into the cursor-narrowed return set.  Note that we can't just
    // change the requested offset in SearchParams because that would cause the offset returned
    // in <SearchResponse> to be incorrect.
    private boolean mIgnoreOffsetHack = false;
    
    private AbstractList<ZimbraHit> mBufferedHits = null;;
    private SearchParams mParams;
    private boolean mForward = true;
    
    private Comparator mComparator; 
    
    public SortBy getSortOrder() { return mParams.getSortBy(); }
    
    static public ResultsPager create(ZimbraQueryResults results, SearchParams params) throws ServiceException
    {
        ResultsPager toRet;
        
        // must use results.getSortBy() because the results might have ignored our sortBy
        // request and used something else...
        params.setSortBy(results.getSortBy());
        
        // bug: 23427 -- TASK sorts are incompatible with cursors here so don't use the cursor
        //               at all
        boolean dontUseCursor = false;
        boolean skipOffsetHack = false;
        switch (params.getSortBy().getType()) {
            case TASK_DUE_ASCENDING:
            case TASK_DUE_DESCENDING:
            case TASK_PERCENT_COMPLETE_ASCENDING:
            case TASK_PERCENT_COMPLETE_DESCENDING:
            case TASK_STATUS_ASCENDING:
            case TASK_STATUS_DESCENDING:
                dontUseCursor = true;
                break;
            case NAME_LOCALIZED_ASCENDING:
            case NAME_LOCALIZED_DESCENDING:
                if (DebugConfig.enableContactLocalizedSort)
                    dontUseCursor = false;
                else
                    dontUseCursor = true;
                
                // for localized sorts, the cursor is actually simulated by the 
                // ReSortingQueryResults....so we need to zero out the offset here
                if (DebugConfig.enableContactLocalizedSort)
                    skipOffsetHack = false;
                else
                    skipOffsetHack = true;
        }
        
        if (dontUseCursor || !params.hasCursor()) {
            toRet = new ResultsPager(results, params, false, true, skipOffsetHack);
        } else {
            // are we paging FORWARD or BACKWARD?  If requested starting-offset is the same or bigger then the cursor's offset, 
            // then we're going FORWARD, otherwise we're going BACKWARD
            boolean forward = true;
            if (params.getOffset() < params.getPrevOffset()) {
                forward = false;
            }
            toRet = new ResultsPager(results, params, true, forward, false);
        }
        return toRet;
    }
    
    /**
     * @param params SearchParams: if OFFSET-MODE, requires SortBy, offset, limit to be set, 
     *               otherwise requires cursor to be set
     * @throws ServiceException
     */
    private ResultsPager(ZimbraQueryResults results, SearchParams params, boolean useCursor, boolean forward, boolean skipOffset) throws ServiceException {
        mResults = results;
        mParams = params;
        mFixedOffset = !useCursor;
        mIgnoreOffsetHack = skipOffset;
        mForward = forward;
        
        if (DebugConfig.enableContactLocalizedSort) {
            SortBy desiredSort = mParams.getSortBy();
            if (desiredSort instanceof LocalizedSortBy) {
                mComparator = ((LocalizedSortBy)desiredSort).getZimbraHitComparator();
            }
        }
            
        assert(forward || !mFixedOffset); // only can go backward if using cursor 
        reset();
    }

    public void reset() throws ServiceException {
        if (mFixedOffset) {
            int offsetToUse = mParams.getOffset();
            if (mIgnoreOffsetHack)
                offsetToUse = 0;
            if (offsetToUse > 0) {
                mResults.skipToHit(offsetToUse-1);
            } else {
                mResults.resetIterator();
            }
        } else {
            if (mForward) {
                mBufferedHits = new ArrayList<ZimbraHit>(1);
                ZimbraHit current = forwardFindFirst();
                if (current != null)
                    mBufferedHits.add(current);
            } else {
                mBufferedHits = backward();
            }
        }       
    }
    
    public boolean hasNext() throws ServiceException {
        if (mBufferedHits != null && !mBufferedHits.isEmpty())
            return true;
        else
            return mResults.hasNext();
    }
    
    public ZimbraHit getNextHit() throws ServiceException {
        if (mBufferedHits != null && !mBufferedHits.isEmpty())
            return mBufferedHits.remove(0);
        else
            return mResults.getNext();
    }
    
    private ZimbraHit forwardFindFirst() throws ServiceException {
        int offset = 0;
        
        ZimbraHit prevHit = getDummyPrevHit();
        
        ZimbraHit hit = mResults.getFirstHit();
        while(hit != null) {
            offset++;
            
            if (hit.getItemId() == mParams.getPrevMailItemId().getId()) {
                // found it!
                return mResults.getNext();
            }
            
            int comp;
            if (DebugConfig.enableContactLocalizedSort) {
                if (mComparator != null)
                    comp = mComparator.compare(hit, prevHit);
                else
                    comp = hit.compareBySortField(mParams.getSortBy(), prevHit); 
            } else
                comp = hit.compareBySortField(mParams.getSortBy(), prevHit); 

            // if (hit at the SAME TIME as prevSortValue) AND the ID is > prevHitId
            //   --> this depends on a secondary sort-order of HitID.  This doesn't
            //  currently hold up with ProxiedHits: we need to convert Hit sorting to
            //  use ItemIds (instead of int's) TODO FIXME
            if (comp == 0) {
                // special case prevId of 0 
                if (mParams.getPrevMailItemId().getId() == 0)
                    return hit;
                
                if (mParams.getSortBy().isDescending()) {
                    if (hit.getItemId() < mParams.getPrevMailItemId().getId())
                        return hit;
                } else {
                    if (hit.getItemId() > mParams.getPrevMailItemId().getId())
                        return hit;
                }
                // keep looking...
                hit = mResults.getNext();
            } else if (comp < 0) {
                // oops, we haven't gotten to the cursor-specified sort field yet...this happens
                // when we use a cursor without doing adding a range constraint to specify
                // the sort ranges....e.g. when using a Cursor with Conversation search
                // we skip the range b/c we need to force the search code to iterate over all
                // results (to build the conversations and hit them into the right spot
                // in the results)
                hit = mResults.getNext();
            } else {
                return hit;
            }
        }

        // end of line
        return null;
    }
    
    /**
     * @return A list (in reverse order) of all hits between start and current-cursor
     *         position 
     * @throws ServiceException
     */
    private AbstractList<ZimbraHit> backward() throws ServiceException {
        LinkedList<ZimbraHit> ll = new LinkedList<ZimbraHit>();
    
        int offset = 0;
        ZimbraHit hit = mResults.getFirstHit();
        ZimbraHit prevHit = getDummyPrevHit();
        
        ZimbraHit dummyEndHit = null;
        if (mParams.hasEndSortValue())
            dummyEndHit = getDummyEndHit();
        
        
        while(hit != null) {
            offset++;

            ll.addLast(hit);
            
            if (hit.getItemId() == mParams.getPrevMailItemId().getId())
                // found old one 
                break;
            
            // if (hit COMES AFTER prevSortValue) {
            if (hit.compareBySortField(mParams.getSortBy(), prevHit) > 0)
                break;
            
            // if (hit COMES BEFORE endSortValue) {
            if (mParams.hasEndSortValue() && (hit.compareBySortField(mParams.getSortBy(), dummyEndHit) <=0)) 
                break;
            
            hit = mResults.getNext();
        }
        return ll;
    }
    
    /**
     * @return a dummy hit which is immediately before the first hit we want to return
     */
    private ZimbraHit getDummyPrevHit() {
        long dateVal = 0;
        String strVal = "";
        strVal = mParams.getPrevSortValueStr();
        dateVal = mParams.getPrevSortValueLong();
        
        return new DummyHit(strVal, strVal, dateVal, mParams.getPrevMailItemId().getId());
    }
    
    /**
     * @return a dummy hit which is immediately after the last hit we want to return
     */
    private ZimbraHit getDummyEndHit() {
        long dateVal = 0;
        String strVal = "";
        strVal = mParams.getEndSortValueStr();
        dateVal = mParams.getEndSortValueLong();
        
        return new DummyHit(strVal, strVal, dateVal, 0);
    }
    
    static class DummyHit extends ZimbraHit
    {
        private int mItemId;
        private long mDate;
        private String mName;
        private String mSubject;
        
        DummyHit(String name, String subject, long date, int itemId)
        {
            super(null, null, 0);
            mName = name;
            mSubject = subject;
            mDate = date;
            mItemId = itemId;
        }
        
        public String toString() {
            return "DummyHit("+mName+","+mSubject+","+mDate+","+mItemId+")";
        }
        
        public long getDate() { return mDate; } 
        public long getSize() { return 0; }
        public int getConversationId() { return 0; }
        public int getItemId() { return mItemId; }
        public byte getItemType() { return 0; }
        void setItem(MailItem item) {}
        boolean itemIsLoaded() { return false; }
        public String getSubject() { return mSubject; }
        public String getName() { return mName; }
        public MailItem getMailItem() throws ServiceException { return  null; }
    }
    
}
