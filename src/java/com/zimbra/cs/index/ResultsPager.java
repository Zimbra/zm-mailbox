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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;

/**
 * @author tim
 * 
 * A helper class which deals with paging through search results (see bug 2937)
 *
 */
public class ResultsPager 
{
    private ZimbraQueryResults mResults;
    private MailboxIndex.SortBy mSortOrder;
    private ItemId mPrevMailItemId;
    
    private String mPrevSortValueStr;
    private long mPrevSortValueLong;
    
    private int mNumResultsRequested;

    private boolean mFixedOffset;
    
    private List<ZimbraHit> mHits;
    
    public MailboxIndex.SortBy getSortOrder() { return mSortOrder; }
    
    static public ResultsPager create(ZimbraQueryResults results, SearchParams params) throws ServiceException
    {
        ResultsPager toRet;
        
        if (!params.hasCursor()) {
            // must use results.getSortBy() because the results might have ignored our sortBy
            // request and used something else...
            toRet = new ResultsPager(results, results.getSortBy(), params.getLimit(), params.getOffset());
        } else {
            // are we paging FORWARD or BACKWARD?  If requested starting-offset is the same or bigger then the cursor's offset, 
            // then we're going FORWARD, otherwise we're going BACKWARD
            boolean forward = true;
            if (params.getOffset() < params.getPrevOffset()) {
                forward = false;
            }
            
            // must use results.getSortBy() because the results might have ignored our sortBy
            // request and used something else...
            toRet = new ResultsPager(results, results.getSortBy(), params.getPrevMailItemId(), params.getPrevSortValueStr(), params.getPrevSortValueLong(), params.getPrevOffset(), forward, params.getLimit());
        }
        return toRet;
    }
    
    public ResultsPager(ZimbraQueryResults results, MailboxIndex.SortBy sortOrder, ItemId prevItemId, String prevSortValueStr, long prevSortValueLong, int prevOffset, 
            boolean forward, int numResultsRequested) throws ServiceException {
        mResults = results;
        mSortOrder = sortOrder;
        mPrevMailItemId = prevItemId;
        mPrevSortValueStr = prevSortValueStr;
        mPrevSortValueLong = prevSortValueLong;
        mNumResultsRequested = numResultsRequested;
        
        mFixedOffset = false;
        
        if (forward) {
            forward();
        } else {
            backward();
        }
    }

    /**
     * Simple offset
     * 
     * @param results
     * @param numResults
     * @param offset
     * @throws ServiceException
     */
    public ResultsPager(ZimbraQueryResults results, MailboxIndex.SortBy sortOrder, int numResults, int offset) throws ServiceException {
        mResults = results;
        mSortOrder = sortOrder;
        mNumResultsRequested = numResults;
        mFixedOffset = true;
        
        if (offset > 0) {
            mResults.skipToHit(offset-1);
        } else {
            mResults.resetIterator();
        }
        forward();
    }
    
    public List getHits() { return mHits; }
    public boolean hasNext() throws ServiceException { return mResults.hasNext(); }
    
    private ZimbraHit getDummyHit() {
        long dateVal = 0;
        String strVal = "";
        strVal = mPrevSortValueStr;
        dateVal = mPrevSortValueLong;
        
        return new DummyHit(strVal, strVal, dateVal, mPrevMailItemId.getId());
    }
    
    private ZimbraHit forwardFindFirst() throws ServiceException {
        int offset = 0;
        
        ZimbraHit prevHit = getDummyHit();
        
        ZimbraHit hit = mResults.getFirstHit();
        while(hit != null) {
            offset++;
            
            if (hit.getItemId() == mPrevMailItemId.getId()) {
                // found it!
                return mResults.getNext();
            }
            
            int comp = hit.compareBySortField(mSortOrder, prevHit); 

            // if (hit at the SAME TIME as prevSortValue) AND the ID is > prevHitId
            //   --> this depends on a secondary sort-order of HitID.  This doesn't
            //  currently hold up with ProxiedHits: we need to convert Hit sorting to
            //  use ItemIds (instead of int's) TODO FIXME
            if ((comp == 0) && 
                        (hit.getItemId() > mPrevMailItemId.getId())) {
                return hit;
            }
            
            // if (hit COMES AFTER prevSortValue) {
            if (comp > 0) {
                return hit;
            }

//            if (offset > mPrevOffset) {
//                throw new NewResultsAtHeadException();
//            }
            
            hit = mResults.getNext();
        }

        // end of line
        return null;
    }
    
    private void forward() throws ServiceException {
        mHits = new ArrayList<ZimbraHit>(mNumResultsRequested);
        
        ZimbraHit hit;

        if (!mFixedOffset) {
            hit = forwardFindFirst();
        } else {
            hit = mResults.getNext();
        }
        if (hit != null) { 
            mHits.add(0, hit);
        }

        for (int i = 1; hit != null && i < mNumResultsRequested; i++) {
            hit = mResults.getNext();
            if (hit != null) {
                mHits.add(i, hit);
            }
        }
    }
    
    private void backward() throws ServiceException {
        LinkedList<ZimbraHit> ll = new LinkedList<ZimbraHit>();
        mHits = ll;

        int offset = 0;
        ZimbraHit hit = mResults.getFirstHit();
        ZimbraHit prevHit = getDummyHit();
        
        
        while(hit != null) {
            offset++;
            
            if (hit.getItemId() == mPrevMailItemId.getId()) {
                // found old one -- DON'T include it in list
                break;
            }
            
            // if (hit COMES AFTER prevSortValue) {
            if (hit.compareBySortField(mSortOrder, prevHit) > 0) {
                break;
            }
            
//            if (offset > mPrevOffset) {
//                throw new NewResultsAtHeadException();
//            }
            
            // okay, so it isn't time to stop yet.
            // add this hit onto our growing list.... and also take
            // existing things off the list if the list is already 
            // as big as we need it..
            if (offset >= mNumResultsRequested) {
                ll.removeFirst();
            }
            ll.addLast(hit);
            
            hit = mResults.getNext();
        }

        // possible that we backed up to the start of the results set....so
        // we do have to check and see if we got enough results...
        while (ll.size() < mNumResultsRequested && hit != null) {
            ll.addLast(hit);
            hit = mResults.getNext();
        }
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
        public int getSize() { return 0; }
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
