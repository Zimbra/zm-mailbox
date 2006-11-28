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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @author tim
 * 
 * A helper class which deals with paging through search results (see bug 2937)
 *
 */
public class ResultsPager 
{
    private ZimbraQueryResults mResults;
    private boolean mFixedOffset;
    private List<ZimbraHit> mHits;
    private SearchParams mParams;
    
    public MailboxIndex.SortBy getSortOrder() { return mParams.getSortBy(); }
    
    static public ResultsPager create(ZimbraQueryResults results, SearchParams params) throws ServiceException
    {
        ResultsPager toRet;
        
        // must use results.getSortBy() because the results might have ignored our sortBy
        // request and used something else...
        params.setSortBy(results.getSortBy());
        
        if (!params.hasCursor()) {
            toRet = new ResultsPager(results, params);
        } else {
            // are we paging FORWARD or BACKWARD?  If requested starting-offset is the same or bigger then the cursor's offset, 
            // then we're going FORWARD, otherwise we're going BACKWARD
            boolean forward = true;
            if (params.getOffset() < params.getPrevOffset()) {
                forward = false;
            }
            toRet = new ResultsPager(results, params, forward);
            
        }
        return toRet;
    }
    
    private ResultsPager(ZimbraQueryResults results, SearchParams params, boolean forward) throws ServiceException {
        mResults = results;
        mParams = params;
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
     * @param params SearchParams, requires SortBy, offset, limit to be set
     * @throws ServiceException
     */
    private ResultsPager(ZimbraQueryResults results, SearchParams params) throws ServiceException {
        mResults = results;
        mParams = params;
        mFixedOffset = true;
        
        if (params.getOffset() > 0) {
            mResults.skipToHit(params.getOffset()-1);
        } else {
            mResults.resetIterator();
        }
        forward();
    }
    
    public List<ZimbraHit> getHits() { return mHits; }
    public boolean hasNext() throws ServiceException { return mResults.hasNext(); }
    
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
            if ((comp == 0) && 
                        (hit.getItemId() > mParams.getPrevMailItemId().getId())) {
                return hit;
            }
            
            // if (hit COMES AFTER prevSortValue) {
            if (comp > 0) {
                return hit;
            }
            hit = mResults.getNext();
        }

        // end of line
        return null;
    }
    
    
    private void forward() throws ServiceException {
        mHits = new ArrayList<ZimbraHit>(mParams.getLimit());
        
        ZimbraHit hit;
        
        ZimbraHit dummyEndHit = null;
        if (mParams.hasEndSortValue())
            dummyEndHit = getDummyEndHit();
        

        if (!mFixedOffset) {
            hit = forwardFindFirst();
        } else {
            hit = mResults.getNext();
        }
        if (hit != null) {
            // if hit BEFORE dummyEndHit
            if (mParams.hasEndSortValue() && (hit.compareBySortField(mParams.getSortBy(), dummyEndHit) > 0))
                return;
            else
                mHits.add(0, hit);
        }
        
        for (int i = 1; hit != null && i < mParams.getLimit(); i++) {
            hit = mResults.getNext();
            if (hit != null) {
                // if hit BEFORE dummyEndHit
                if (mParams.hasEndSortValue() && (hit.compareBySortField(mParams.getSortBy(), dummyEndHit) > 0))
                    break;
                else
                    mHits.add(i, hit);
            }
        }
    }
    
    private void backward() throws ServiceException {
        LinkedList<ZimbraHit> ll = new LinkedList<ZimbraHit>();
        mHits = ll;

        int offset = 0;
        ZimbraHit hit = mResults.getFirstHit();
        ZimbraHit prevHit = getDummyPrevHit();
        
        ZimbraHit dummyEndHit = null;
        if (mParams.hasEndSortValue())
            dummyEndHit = getDummyEndHit();
        
        
        while(hit != null) {
            offset++;
            
            if (hit.getItemId() == mParams.getPrevMailItemId().getId())
                // found old one -- DON'T include it in list
                break;
            
            // if (hit COMES AFTER prevSortValue) {
            if (hit.compareBySortField(mParams.getSortBy(), prevHit) > 0)
                break;
            
            // if (hit COMES BEFORE endSortValue) {
            if (mParams.hasEndSortValue() && (hit.compareBySortField(mParams.getSortBy(), dummyEndHit) <=0)) 
                break;
            
//          if (offset > mPrevOffset) {
//                throw new NewResultsAtHeadException();
//            }
            
            // okay, so it isn't time to stop yet.
            // add this hit onto our growing list.... and also take
            // existing things off the list if the list is already 
            // as big as we need it..
            if (offset >= mParams.getLimit()) {
                ll.removeFirst();
            }
            ll.addLast(hit);
            
            hit = mResults.getNext();
        }

        // possible that we backed up to the start of the results set....so
        // we do have to check and see if we got enough results...
        while (ll.size() < mParams.getLimit() && hit != null) {
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
