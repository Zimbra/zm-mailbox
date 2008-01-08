/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;

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
    private AbstractList<ZimbraHit> mBufferedHits = null;;
    private SearchParams mParams;
    private boolean mForward = true;
    
    public MailboxIndex.SortBy getSortOrder() { return mParams.getSortBy(); }
    
    static public ResultsPager create(ZimbraQueryResults results, SearchParams params) throws ServiceException
    {
        ResultsPager toRet;
        
        // must use results.getSortBy() because the results might have ignored our sortBy
        // request and used something else...
        params.setSortBy(results.getSortBy());
        
        // bug: 23427 -- TASK sorts are incompatible with CURSORS, since cursors require
        //               real (db-visible) sort fields
        boolean dontUseCursor = false;
        switch (params.getSortBy()) {
            case TASK_DUE_ASCENDING:
            case TASK_DUE_DESCENDING:
            case TASK_PERCENT_COMPLETE_ASCENDING:
            case TASK_PERCENT_COMPLETE_DESCENDING:
            case TASK_STATUS_ASCENDING:
            case TASK_STATUS_DESCENDING:
                dontUseCursor = true;
        }
        
        if (dontUseCursor || !params.hasCursor()) {
            toRet = new ResultsPager(results, params, false, true);
        } else {
            // are we paging FORWARD or BACKWARD?  If requested starting-offset is the same or bigger then the cursor's offset, 
            // then we're going FORWARD, otherwise we're going BACKWARD
            boolean forward = true;
            if (params.getOffset() < params.getPrevOffset()) {
                forward = false;
            }
            toRet = new ResultsPager(results, params, true, forward);
        }
        return toRet;
    }
    
    /**
     * @param params SearchParams: if OFFSET-MODE, requires SortBy, offset, limit to be set, 
     *               otherwise requires cursor to be set
     * @throws ServiceException
     */
    private ResultsPager(ZimbraQueryResults results, SearchParams params, boolean useCursor, boolean forward) throws ServiceException {
        mResults = results;
        mParams = params;
        mFixedOffset = !useCursor;
        mForward = forward;
        assert(forward || !mFixedOffset); // only can go backward if using cursor 
        reset();
    }

    public void reset() throws ServiceException {
        if (mFixedOffset) {
            if (mParams.getOffset() > 0) {
                mResults.skipToHit(mParams.getOffset()-1);
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
            
            int comp = hit.compareBySortField(mParams.getSortBy(), prevHit); 

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
    
    private static class DummyHit extends ZimbraHit
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
