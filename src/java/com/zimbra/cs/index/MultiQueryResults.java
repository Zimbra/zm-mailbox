/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

/*
 * Created on Mar 17, 2005
 *
 * A QueryResults set which cross multiple mailboxes...
 * 
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.MailboxIndex.SortBy;


/**
 * Take a bunch of QueryResults (each sorted with the same sort-order) and 
 * iterate them in order....kind of like a MergeSort
 * 
 * TODO - this class duplicates functionality in UnionQueryOperation, 
 * they should be combined somehow
 * 
 * The primary use of this class is for cross-mailbox-search, when you need to
 * aggregate search results together from many mailboxes 
 * 
 */
public class MultiQueryResults implements ZimbraQueryResults
{
    SortBy mSortOrder;
    ZimbraQueryResults[] mResults;
    private ZimbraHit mCachedNextHit = null;
    
    public SortBy getSortBy() {
        return mSortOrder;
    }
    public MultiQueryResults(ZimbraQueryResults[] res, SortBy sortOrder)
    {
        mSortOrder = sortOrder;
        
        mResults = new ZimbraQueryResults[res.length];
        for (int i = 0; i < res.length; i++) {
            mResults[i] = HitIdGrouper.Create(res[i], sortOrder); 
        }
    }
    
    public ZimbraHit peekNext() throws ServiceException
    {
        bufferNextHit();
        return mCachedNextHit;
    }
    
    private void internalGetNextHit() throws ServiceException
    {
        if (mCachedNextHit == null) {
            if (mSortOrder == MailboxIndex.SortBy.NONE) {
                for (ZimbraQueryResults res : mResults) {
                    mCachedNextHit = res.getNext();
                    if (mCachedNextHit != null)
                        return;
                }
                // no more results!
                
            } else {
                // mergesort: loop through QueryOperations and find the "best" hit
            
                int currentBestHitOffset = -1;
                ZimbraHit currentBestHit = null;
                for (int i = 0; i < mResults.length; i++) {
                    ZimbraQueryResults op = mResults[i]; 
                    if (op.hasNext()) {
                        if (currentBestHitOffset == -1) {
                            currentBestHitOffset = i;
                            currentBestHit = op.peekNext();
                        } else {
                            ZimbraHit opNext = op.peekNext();
                            int result = opNext.compareBySortField(mSortOrder, currentBestHit);
                            if (result < 0) {
                                // "before"
                                currentBestHitOffset = i;
                                currentBestHit = opNext;
                            }
                        }
                    }
                }
                if (currentBestHitOffset > -1) {
                    mCachedNextHit = mResults[currentBestHitOffset].getNext();
                    assert(mCachedNextHit == currentBestHit);
                }
            }
        }
    }
    
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        resetIterator();
        for (int i = 0; i < hitNo; i++) {
            if (!hasNext()) {
                return null;
            }
            getNext();
        }
        return getNext();
    }
    
    public boolean hasNext() throws ServiceException {
        return bufferNextHit();
    }
    
    public boolean bufferNextHit() throws ServiceException {
        if (mCachedNextHit == null) {
            internalGetNextHit();
        }
        return (mCachedNextHit != null);
    }
    
    public void resetIterator() throws ServiceException {
        for (int i = 0; i < mResults.length; i++) {
            mResults[i].resetIterator();
        }
    }
    
    public ZimbraHit getFirstHit() throws ServiceException
    {
        resetIterator();
        return getNext();
    }
    
    public ZimbraHit getNext() throws ServiceException {
        bufferNextHit();
        ZimbraHit toRet = mCachedNextHit;
        mCachedNextHit = null;
        return toRet;
    }
    
    public void doneWithSearchResults() throws ServiceException {
        for (int i = 0; i < mResults.length; i++) {
            mResults[i].doneWithSearchResults();
        }
    }
    
    public List<QueryInfo> getResultInfo() { 
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        for (ZimbraQueryResults results : mResults) {
            toRet.addAll(results.getResultInfo());
        }
        return toRet;
    }
    
    public int estimateResultSize() throws ServiceException {
        long total = 0;
        for (ZimbraQueryResults results : mResults) {
            total += results.estimateResultSize();
        }
        if (total > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int)total;
    }

    
}